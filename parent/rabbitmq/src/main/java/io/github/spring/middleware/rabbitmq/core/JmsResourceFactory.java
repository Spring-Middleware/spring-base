package io.github.spring.middleware.rabbitmq.core;

import com.rabbitmq.jms.admin.RMQConnectionFactory;
import com.rabbitmq.jms.admin.RMQDestination;
import io.github.spring.middleware.rabbitmq.annotations.*;
import io.github.spring.middleware.rabbitmq.annotations.listener.JmsAll;
import io.github.spring.middleware.rabbitmq.annotations.listener.JmsAllConsumers;
import io.github.spring.middleware.rabbitmq.annotations.listener.JmsAllProducers;
import io.github.spring.middleware.rabbitmq.configuration.JmsConnectionConfiguration;
import io.github.spring.middleware.rabbitmq.connection.JmsConnection;
import io.github.spring.middleware.rabbitmq.connection.JmsConnectionManager;
import io.github.spring.middleware.rabbitmq.connection.JmsConnectionPoolFactory;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationTypeFunctionExecutor;
import io.github.spring.middleware.rabbitmq.core.resource.JmsResource;
import io.github.spring.middleware.rabbitmq.core.resource.consumer.JmsConsumerResource;
import io.github.spring.middleware.rabbitmq.core.resource.handler.JmsHandlerResource;
import io.github.spring.middleware.rabbitmq.core.resource.handler.JmsResourceErrorHandler;
import io.github.spring.middleware.rabbitmq.core.resource.listener.JmsResourceListener;
import io.github.spring.middleware.rabbitmq.core.resource.producer.JmsProducerResource;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.github.spring.middleware.rabbitmq.core.destination.type.DestinationNamer.getDestinationSuffixName;

@Component(value = "JmsResourceFactory")
public class JmsResourceFactory implements ApplicationContextAware {

    private JmsConnectionManager jmsConnectionManager;
    private DestinationTypeFunctionExecutor destinationTypeFunctionExecutor;
    private Logger logger = LoggerFactory.getLogger(JmsResourceFactory.class);
    private GenericObjectPoolConfig genericObjectPoolConfig;
    private GenericObjectPool<JmsConnection> connectionPool;
    private static ApplicationContext applicationContext;

    public JmsResourceFactory() {

    }

    public void setDestinationTypeFunctionExecutor(
            DestinationTypeFunctionExecutor destinationTypeFunctionExecutor) {

        this.destinationTypeFunctionExecutor = destinationTypeFunctionExecutor;
    }

    public void configure(RMQConnectionFactory connectionFactory, JmsConnectionConfiguration jmsConnectionConfiguration) {
        try {

            jmsConnectionManager = new JmsConnectionManager(connectionFactory, jmsConnectionConfiguration);

            genericObjectPoolConfig = new GenericObjectPoolConfig<JmsConnection>();
            genericObjectPoolConfig.setMinIdle(jmsConnectionConfiguration.getJmsConnectionPoolConfiguration().getMinIdle());
            genericObjectPoolConfig.setMaxIdle(jmsConnectionConfiguration.getJmsConnectionPoolConfiguration().getMaxIdle());
            genericObjectPoolConfig.setMaxTotal(jmsConnectionConfiguration.getJmsConnectionPoolConfiguration().getMaxTotal());

            JmsConnectionPoolFactory jmsConnectionPoolFactory = JmsConnectionPoolFactory.getInstance(jmsConnectionManager);

            // Tipamos el pool para evitar warnings de Raw Types
            connectionPool = new GenericObjectPool<JmsConnection>(jmsConnectionPoolFactory, genericObjectPoolConfig);

            logger.info("JmsResourceFactory configured successfully for RabbitMQ-JMS (Jakarta)");
        } catch (Exception ex) {
            logger.error("Error configuring resource factory for RabbitMQ: ", ex);
        }
    }

    public <L extends JmsResourceListener, R extends JmsResource> Set<L> createListeners(Reflections reflections,
                                                                                         Collection<R> resources) {

        Set<Class<L>> clazzListeners = (Set) reflections.getTypesAnnotatedWith(JmsListener.class);
        return clazzListeners.stream().map(clazzListener -> {
            try {
                JmsListener jmsListener = clazzListener.getAnnotation(JmsListener.class);
                L jmsRsourceListener = getInstance(clazzListener);
                resources.stream().filter(r -> jmsListener.value().isAssignableFrom(JmsAll.class) ||
                                mathClazzAnnotationCorresponding(r, jmsListener.value())
                                || r.getClass().isAnnotationPresent(jmsListener.value()))
                        .forEach(r -> r.addJmsResourceListeners(jmsRsourceListener));
                return jmsRsourceListener;
            } catch (Exception ex) {
                logger.error("Can not instatiate listener for class " + clazzListener.getSimpleName());
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public <E extends JmsResourceErrorHandler, R extends JmsResource> Set<E> createErrorHandlers(
            Reflections reflections, Collection<R> resources) {

        Set<Class<E>> clazzErrorHandlers = (Set) reflections.getTypesAnnotatedWith(JmsErrorHandler.class);
        return clazzErrorHandlers.stream().map(clazzErrorHandler -> {
            try {
                JmsErrorHandler jmsErrorHandler = clazzErrorHandler.getAnnotation(JmsErrorHandler.class);
                E jmsErrorHandlerResource = getInstance(clazzErrorHandler);
                resources.stream().filter(r -> jmsErrorHandler.value().isAssignableFrom(JmsAll.class) ||
                                mathClazzAnnotationCorresponding(r, jmsErrorHandler.value())
                                || r.getClass().isAnnotationPresent(jmsErrorHandler.value()))
                        .forEach(r -> r.addJmsErrorHandlerResource(jmsErrorHandlerResource));
                return jmsErrorHandlerResource;
            } catch (Exception ex) {
                logger.error("Can not instatiate listener for class " + clazzErrorHandler.getSimpleName());
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private <R extends JmsResource> boolean mathClazzAnnotationCorresponding(R resorce,
                                                                             Class<? extends Annotation> annotation) {

        boolean match = false;
        if (JmsConsumerResource.class.isAssignableFrom(resorce.getClass())) {
            match = annotation.isAssignableFrom(JmsAllConsumers.class);
        }
        if (JmsProducerResource.class.isAssignableFrom(resorce.getClass())) {
            match = annotation.isAssignableFrom(JmsAllProducers.class);
        }
        return match;
    }

    public <H extends JmsHandlerResource, R extends JmsResource> void createHandlers(Reflections reflections,
                                                                                     Collection<R> resources) {

        Set<Class<H>> clazzHandlers = (Set) reflections.getTypesAnnotatedWith(JmsHandler.class);
        clazzHandlers.stream().forEach(clazzHandler -> {
            JmsHandler jmsHandler = clazzHandler.getAnnotation(JmsHandler.class);
            resources.stream().filter(r -> r.getClass().isAnnotationPresent(jmsHandler.value()))
                    .forEach(r -> {
                        try {
                            if (r.getJmsHandlerResource() == null) {
                                r.setJmsHandlerResource(getInstance(clazzHandler));
                            } else {
                                logger.error("Only allowed one handler for a consumer");
                            }
                        } catch (Exception ex) {
                            logger.error("Error configuring handler " + clazzHandler.getSimpleName() + " in consumer " +
                                    r.getClass());
                        }
                    });
        });
    }

    public <T extends JmsResource> List<T> createProducers(Class<T> clazz, JmsBinding[] bindings) {

        return (List) Arrays.stream(bindings).map(binding -> {
            JmsProducer jmsProducer = clazz.getAnnotation(JmsProducer.class);
            JmsResourceDestination jmsResourceDestination = null;
            try {
                jmsResourceDestination = dicoverJmsResourceDestination(clazz, JmsProducer.class, binding.routingKey());
            } catch (Exception ex) {
                logger.error("Error discovering detination jms for: " + clazz.getSimpleName(), ex);
            }
            JmsSessionParameters jmsSessionParameters = new JmsSessionParameters(jmsProducer.transacted(),
                    jmsProducer.acknoledgement());
            JmsProducerResource jmsProducerResource = null;
            try {
                Class<?> genericType = (Class) ((ParameterizedType) getJmsResourceClazz(clazz).getGenericSuperclass())
                        .getActualTypeArguments()[0];
                if (applicationContext == null) {
                    jmsProducerResource = (JmsProducerResource) clazz
                            .getConstructor(String.class, ObjectPool.class, JmsSessionParameters.class,
                                    JmsResourceDestination.class, Class.class)
                            .newInstance(
                                    getRoutingKey(jmsResourceDestination.getDestinationName(), binding.routingKey()),
                                    connectionPool, jmsSessionParameters, jmsResourceDestination, genericType);
                } else {
                    jmsProducerResource = (JmsProducerResource) applicationContext.getBean(clazz);
                    jmsProducerResource.setConnectionPool(connectionPool);
                    jmsProducerResource.setJmsSessionParameters(jmsSessionParameters);
                    jmsProducerResource.setJmsResourceDestination(jmsResourceDestination);
                    jmsProducerResource.setClazz(genericType);
                    jmsProducerResource
                            .setRoutingKey(getRoutingKey(jmsResourceDestination.getDestinationName(),
                                    binding.routingKey()));
                }
                return jmsProducerResource;
            } catch (Exception ex) {
                logger.error(
                        "Can't create producer for class " + clazz.getSimpleName() + " and routingKey " +
                                binding.routingKey());
            }
            return jmsProducerResource;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private Class<?> getJmsResourceClazz(Class clazz) throws Exception {

        if (clazz.getSuperclass() != null) {
            if (clazz.getSuperclass().isAssignableFrom(JmsProducerResource.class) ||
                    clazz.getSuperclass().isAssignableFrom(JmsConsumerResource.class)) {
                return clazz;
            } else {
                return getJmsResourceClazz(clazz.getSuperclass());
            }
        } else {
            throw new Exception("No found valid clazz " + clazz.getSimpleName());
        }
    }

    private String getRoutingKey(String jmsDestinationName, String routingKey) {

        return routingKey.isEmpty() ? jmsDestinationName : routingKey;
    }

    public <T extends JmsResource> Collection<JmsConsumerResource> createConsumers(Class<T> clazz) throws Exception {

        JmsConsumer jmsConsumer = clazz.getAnnotation(JmsConsumer.class);
        return IntStream.range(0, jmsConsumer.instances()).mapToObj(id -> {
            JmsResourceDestination jmsResourceDestination = null;
            JmsConsumerResource jmsConsumerResource = null;
            try {
                jmsResourceDestination = dicoverJmsResourceDestination(clazz, JmsConsumer.class, null);
            } catch (Exception ex) {
                logger.error("Error discovering detination jms for: " + clazz.getSimpleName(), ex);
            }
            try {
                JmsSessionParameters jmsSessionParameters = new JmsSessionParameters(jmsConsumer.transacted(),
                        jmsConsumer.acknoledgement());
                Class<?> genericType = (Class) ((ParameterizedType) getJmsResourceClazz(clazz).getGenericSuperclass())
                        .getActualTypeArguments()[0];

                if (applicationContext == null) {
                    jmsConsumerResource = (JmsConsumerResource) clazz
                            .getConstructor(ObjectPool.class, JmsSessionParameters.class, JmsResourceDestination.class,
                                    Class.class)
                            .newInstance(connectionPool, jmsSessionParameters, jmsResourceDestination, genericType);
                } else {
                    jmsConsumerResource = (JmsConsumerResource) applicationContext.getBean(clazz);
                    jmsConsumerResource.setConnectionPool(connectionPool);
                    jmsConsumerResource.setJmsSessionParameters(jmsSessionParameters);
                    jmsConsumerResource.setJmsResourceDestination(jmsResourceDestination);
                    jmsConsumerResource.setClazz(genericType);
                }

                jmsConsumerResource.setDurabilityFunctionExecutor(destinationTypeFunctionExecutor);
                jmsConsumerResource.setId(id);
            } catch (Exception ex) {
                logger.error("Error creating consumer for class " + clazz.getSimpleName());
            }
            return jmsConsumerResource;
        }).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private <T extends JmsResource> JmsResourceDestination dicoverJmsResourceDestination(Class<T> clazzJMS,
                                                                                         Class<? extends Annotation> annotationClazz,
                                                                                         String routingKey) throws Exception {

        Destination destination = null;
        JmsDestination jmsDestination = null;
        if (clazzJMS.isAnnotationPresent(JmsDestination.class)) {
            jmsDestination = clazzJMS.getAnnotation(JmsDestination.class);
            if (annotationClazz.isAssignableFrom(JmsConsumer.class)) {
                destination = getDestinationQueue(jmsDestination);
            }
            if (annotationClazz.isAssignableFrom(JmsProducer.class)) {
                destination = routingKey.isEmpty() ? getDestinationQueue(jmsDestination) : getDestinationTopic(
                        getDestinationSuffixName(jmsDestination), routingKey);
            }
        } else {
            throw new JMSException("Missing @JmsDestination in " + clazzJMS.getSimpleName());
        }
        return new JmsResourceDestination(destination, jmsDestination);
    }

    private <T extends JmsResource> Destination getDestinationQueue(
            JmsDestination jmsDestination)  {

        RMQDestination destination = new RMQDestination();
        destination.setQueue(true);

        // 1. Normalizamos el Exchange (Default Exchange si es vacío/null)
        String exchange = (jmsDestination.exchange() == null) ? "" : jmsDestination.exchange();
        String queueName = getDestinationSuffixName(jmsDestination);
        String routingKey = getDestinationSuffixName(jmsDestination);

        // 2. FORZAMOS LOS SETTERS INTERNOS (Esto soluciona el 'exchange must be non-null')
        destination.setAmqpExchangeName(exchange);
        destination.setAmqpRoutingKey(routingKey);
        destination.setAmqpQueueName(queueName);

        // 3. Montamos la Address String por compatibilidad con el motor de JMS
        String address = String.format("%s/%s?queue=%s&durable=%b",
                exchange, routingKey, queueName, jmsDestination.durable());

        destination.setDestinationName(queueName);

        return destination;
    }

    private Destination getDestinationTopic(String topicName, String routingKey) throws JMSException {
        RMQDestination destination = new RMQDestination();

        // Normalizamos nombres (amq.topic, etc)
        String exchange = (topicName == null || topicName.trim().isEmpty()) ? "" : topicName;
        String rKey = (routingKey == null) ? "" : routingKey;

        destination.setAmqpExchangeName(exchange);
        destination.setAmqpRoutingKey(rKey);
        // --- LÓGICA PRODUCTOR ---
        // El productor NUNCA debe declarar una cola
        destination.setDestinationName(exchange + "/" + rKey);
        destination.setQueue(false);
        return destination;
    }

    private <T> T getInstance(Class<T> clazz) {

        return Optional.ofNullable(applicationContext).map(ctx -> {
            return ctx.getBean(clazz);
        }).orElseGet(() -> {
            T t = null;
            try {
                t = clazz.newInstance();
            } catch (Exception ex) {
                logger.error("Can't get instance for " + clazz);
            }
            return t;
        });
    }

    @Override
    public void setApplicationContext(ApplicationContext myApplicationContext) throws BeansException {

        applicationContext = myApplicationContext;
    }
}