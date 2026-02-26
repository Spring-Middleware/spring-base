package io.github.spring.middleware.rabbitmq;


import com.rabbitmq.jms.admin.RMQConnectionFactory;
import io.github.spring.middleware.rabbitmq.annotations.JmsConsumer;
import io.github.spring.middleware.rabbitmq.annotations.JmsDestination;
import io.github.spring.middleware.rabbitmq.annotations.JmsProducer;
import io.github.spring.middleware.rabbitmq.configuration.JmsConnectionConfiguration;
import io.github.spring.middleware.rabbitmq.core.JmsResourceFactory;
import io.github.spring.middleware.rabbitmq.core.JmsResources;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationNamer;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationTypeFunctionExecutor;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationTypeFunctionType;
import io.github.spring.middleware.rabbitmq.core.destination.type.ValidateJmsDestinationParameters;
import io.github.spring.middleware.rabbitmq.core.resource.consumer.creator.ValidTopicID;
import jakarta.jms.JMSException;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class JmsFactory {

    private static final Logger logger = LoggerFactory.getLogger(JmsFactory.class);
    private DestinationTypeFunctionExecutor destinationTypeFunctionExecutor;
    private JmsResourceFactory jmsResourceFactory;

    private JmsFactory() {

        System.setProperty("IMMEDIATE_PREFETCH", "true");
        destinationTypeFunctionExecutor = new DestinationTypeFunctionExecutor();
        this.jmsResourceFactory = new JmsResourceFactory();
        jmsResourceFactory.setDestinationTypeFunctionExecutor(destinationTypeFunctionExecutor);
    }

    public static JmsFactory newInstance() {

        return new JmsFactory();
    }

    private Properties discoverConsumerAndProducers(Reflections reflections,
                                                    Class<? extends Annotation> annotation) throws JMSException {

        Properties destinationProperties = new Properties();
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(annotation);
        if (!classes.isEmpty()) {
            for (Class clazz : classes) {
                if (clazz.isAnnotationPresent(JmsDestination.class)) {
                    JmsDestination jmsDestination = (JmsDestination) clazz.getAnnotation(JmsDestination.class);
                    if (annotation.isAssignableFrom(JmsConsumer.class)) {
                        ValidateJmsDestinationParameters validateJmsDestinationParameters = new ValidateJmsDestinationParameters(
                                jmsDestination);
                        ValidTopicID validTopicID = (ValidTopicID) destinationTypeFunctionExecutor
                                .execute(DestinationTypeFunctionType.VALIDATE_JMS_DESTINATION,
                                        jmsDestination.destinationType(), validateJmsDestinationParameters);
                        if (!validTopicID.isValid()) {
                            throw new JMSException("Missing topicID in @JmsDestination for @JmsConsumer DURABLE");
                        }
                        destinationProperties.put(jmsDestination.destinationType().getReference() + "." +
                                DestinationNamer.getDestinationSuffixName(jmsDestination), getBindingUrlForConsumer(jmsDestination));
                    } else if (annotation.isAssignableFrom(JmsProducer.class)) {
                        JmsProducer jmsProducer = (JmsProducer) clazz.getAnnotation(JmsProducer.class);
                        Arrays.stream(jmsProducer.bindings()).forEach(binding -> {
                            try {
                                destinationProperties.put(jmsDestination.destinationType().getReference() + "." +
                                                getDestinationName(jmsDestination, binding.routingKey()),
                                        getBindingUrlForProducer(jmsDestination, binding.routingKey()));
                            } catch (JMSException ex) {
                                logger.error("Can't create producer destination for " +
                                        getRoutingKey(jmsDestination, binding.routingKey()));
                            }
                        });
                    }
                }
            }
        }
        return destinationProperties;
    }

    private String getDestinationName(JmsDestination jmsDestination, String routingKey) {

        String destinationName = DestinationNamer.getDestinationSuffixName(jmsDestination);
        if (!routingKey.isEmpty()) {
            destinationName = destinationName + "_" + routingKey;
        }
        return destinationName;
    }

    private String getRoutingKey(JmsDestination jmsDestination, String routingKey) {

        return routingKey.isEmpty() ? DestinationNamer.getDestinationSuffixName(jmsDestination) : routingKey;
    }

    private String getBindingUrlForProducer(JmsDestination jmsDestination,
                                            String routingKey) throws JMSException {
        // Formato estándar de RabbitMQ JMS: exchange/routingKey?queue=queueName
        try {
            return String.format("%s/%s?queue=%s",
                    getExchangeName(jmsDestination),
                    getRoutingKey(jmsDestination, routingKey),
                    DestinationNamer.getDestinationSuffixName(jmsDestination)
            );
        } catch (Exception ex) {
            throw new JMSException("Error construyendo el Address String del productor: " + ex.getMessage());
        }
    }


    private String getBindingUrlForConsumer(JmsDestination jmsDestination) throws JMSException {

        try {
            // En RabbitMQ JMS, el formato para consumidores suele ser:
            // [exchange]/[routingKey]?queue=[queueName]&durable=[true/false]
            return String.format("%s/%s?queue=%s&durable=%b",
                    getExchangeName(jmsDestination),
                    DestinationNamer.getDestinationSuffixName(jmsDestination), // Routing key para el binding
                    DestinationNamer.getDestinationSuffixName(jmsDestination), // Nombre de la cola
                    jmsDestination.durable()
            );
        } catch (Exception ex) {
            throw new JMSException("Error construyendo el Address String del consumidor: " + ex.getMessage());
        }
    }

    private String getExchangeName(JmsDestination jmsDestination) {
        String exchangeName = jmsDestination.exchange();
        if (exchangeName.equals("amq.direct")) {
            return exchangeName;
        } else {
            return DestinationNamer.getExchangeSuffixName(jmsDestination);
        }
    }


    private Properties discoverJMS(Reflections reflections,
                                   JmsConnectionConfiguration jmsConnectionConfiguration) throws JMSException {

        Properties jmsProperties = new Properties();
        // 1. Usar el Factory de RabbitMQ para Jakarta
        jmsProperties.put("java.naming.factory.initial", "com.rabbitmq.jms.admin.RMQObjectFactory");

        // 2. Cambiar la propiedad de la conexión (Ya no es 'connectionfactory.qpid...')
        // En RabbitMQ-JMS se suele inyectar el RMQConnectionFactory directamente,
        // pero si usas JNDI, la propiedad estándar es:
        jmsProperties.put("connectionFactory.ConnectionFactory", "ConnectionFactory");

        jmsProperties.putAll(discoverConsumerAndProducers(reflections, JmsProducer.class));
        jmsProperties.putAll(discoverConsumerAndProducers(reflections, JmsConsumer.class));
        return jmsProperties;
    }

    public JmsResources createJmsResources(List<String> packages,
                                           JmsConnectionConfiguration jmsConnectionConfiguration) throws Exception {

        Reflections reflections = new Reflections(packages);
        JmsResources jmsResources = new JmsResources();
        Properties jmsProperties = discoverJMS(reflections, jmsConnectionConfiguration);
        if (jmsProperties != null) {

            try {
                RMQConnectionFactory connectionFactory = new RMQConnectionFactory();

                // Configuramos la conexión con los datos del jmsConnectionConfiguration
                // El host tcp://localhost:XXXX lo parseamos para sacar host y puerto
                connectionFactory.setUri(jmsConnectionConfiguration.getTcpHost());
                connectionFactory.setUsername(jmsConnectionConfiguration.getJmsConnectionCredentials().getUsername());
                connectionFactory.setPassword(jmsConnectionConfiguration.getJmsConnectionCredentials().getPassword());

                // 4. Pasamos la factoría y las propiedades (destinos) al ResourceFactory
                jmsResourceFactory.configure(connectionFactory, jmsConnectionConfiguration);

            } catch (Exception ex) {
                logger.error("Error configurando la factoría de conexiones de RabbitMQ-JMS", ex);
                throw ex;
            }

            for (Class clazz : reflections.getTypesAnnotatedWith(JmsProducer.class)) {
                JmsProducer jmsProducer = (JmsProducer) clazz.getAnnotation(JmsProducer.class);
                jmsResources.addProducers(jmsResourceFactory.createProducers(clazz, jmsProducer.bindings()));
            }
            for (Class clazz : reflections.getTypesAnnotatedWith(JmsConsumer.class)) {
                jmsResources.addConsumers(jmsResourceFactory.createConsumers(clazz));
            }
            jmsResources.setJmsResourceListeners(
                    jmsResourceFactory.createListeners(reflections, jmsResources.getJmsResources()));
            jmsResources.setJmsResourceErrorHandlers(
                    jmsResourceFactory.createErrorHandlers(reflections, jmsResources.getJmsResources()));
            jmsResourceFactory.createHandlers(reflections, jmsResources.getJmsResources());
        }
        return jmsResources;
    }

}