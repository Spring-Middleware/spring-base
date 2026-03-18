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

    public JmsResources createJmsResources(List<String> packages,
                                           JmsConnectionConfiguration jmsConnectionConfiguration) throws Exception {

        Reflections reflections = new Reflections(packages);
        JmsResources jmsResources = new JmsResources();
        Properties jmsProperties = null;  //discoverJMS(reflections, jmsConnectionConfiguration);
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

        return jmsResources;
    }

}