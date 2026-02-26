package io.github.spring.middleware.rabbitmq.core.resource.consumer;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.spring.middleware.rabbitmq.annotations.JmsConsumer;
import io.github.spring.middleware.rabbitmq.connection.JmsConnection;
import io.github.spring.middleware.rabbitmq.converter.Converter;
import io.github.spring.middleware.rabbitmq.core.JmsAcknowledgeListener;
import io.github.spring.middleware.rabbitmq.core.JmsResourceDestination;
import io.github.spring.middleware.rabbitmq.core.JmsSelector;
import io.github.spring.middleware.rabbitmq.core.JmsSessionParameters;
import io.github.spring.middleware.rabbitmq.core.destination.type.DestinationTypeFunctionExecutor;
import io.github.spring.middleware.rabbitmq.core.resource.JmsResource;
import io.github.spring.middleware.rabbitmq.core.resource.JmsResourceType;
import io.github.spring.middleware.rabbitmq.core.resource.consumer.creator.MessageConsumerFactory;
import io.github.spring.middleware.rabbitmq.core.resource.handler.HandlerParameters;
import jakarta.jms.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.pool2.ObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.util.Comparator;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Properties;

public abstract class JmsConsumerResource<T> extends JmsResource<T> implements MessageListener {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private MessageConsumer messageConsumer;
    private JmsAcknowledgeListener jmsAcknowledgeListener;
    private MessageConsumerFactory messageConsumerFactory;
    private Session session;
    private JmsConnection jmsConnection;
    private boolean started = false;
    private Integer id;

    public JmsConsumerResource() {

    }

    public JmsConsumerResource(ObjectPool<JmsConnection> connectionPool, JmsSessionParameters jmsSessionParameters,
                               JmsResourceDestination jmsResourceDestination, Class<T> clazz) {

        super(connectionPool, jmsSessionParameters, jmsResourceDestination, clazz);
    }

    public void setDurabilityFunctionExecutor(DestinationTypeFunctionExecutor destinationTypeFunctionExecutor) {

        this.messageConsumerFactory = MessageConsumerFactory.getInstance(destinationTypeFunctionExecutor);
    }

    public Integer getId() {

        return id;
    }

    public void setId(Integer id) {

        this.id = id;
    }

    public void restart(boolean force) {

        stop(force);
        start(force);
    }

    public void start(boolean force) {

        try {
            if (!started || force) {
                jmsConnection = getConnection();
                ExceptionConnectionListener connectionListener = new ExceptionConnectionListener(this);
                jmsConnection.getConnection().setExceptionListener(connectionListener);
                session = getSession(jmsConnection);
                messageConsumer = messageConsumerFactory
                        .createMesssageConsumer(session, jmsResourceDestination, createMessageSelector());
                logger.info("Started jms: " + this.toString());
                messageConsumer.setMessageListener(this);
                started = true;
            }
        } catch (Exception e) {
            logger.error("Error creating a consumer ", e);
        }
    }

    public boolean isStarted() {

        return Optional.ofNullable(jmsConnection).map(JmsConnection::isStarted).orElse(Boolean.FALSE);
    }

    private String createMessageSelector() throws Exception {

        JmsConsumer jmsConsumer = this.getClass().getAnnotation(JmsConsumer.class);
        JmsSelector jmsSelector = jmsConsumer.selector().newInstance();
        StringBuffer messageSelector = new StringBuffer();
        if (jmsSelector.properties() != null) {
            Properties properties = jmsSelector.properties();
            for (String property : properties.stringPropertyNames()) {
                messageSelector.append(property).append(" = ").append("'").append(properties.getProperty(property))
                        .append("'").append(" and ");
            }
            messageSelector.setLength(messageSelector.length() - 5);
            logger.debug("Created message selector " + messageSelector.toString());
        }
        return messageSelector.toString();
    }

    public void stop(boolean force) {

        try {
            if (started || force) {
                logger.info("Stopping consumer " + this.toString());
                messageConsumer.close();
                close(session);
                invalidate(jmsConnection);
                started = false;
            }
        } catch (RuntimeException rex) {
            logger.error("Error stopping consumer " + this.toString(), rex);
        } catch (Exception ex) {
            logger.error("Error stopping consumer " + this.toString(), ex);
        }
    }

    public void setJmsAcknowledgeListener(JmsAcknowledgeListener jmsAcknowledgeListener) {

        this.jmsAcknowledgeListener = jmsAcknowledgeListener;
    }

    public abstract void process(T t, Properties properties) throws Exception;

    public JmsResourceType getJmsResourceType() {

        return JmsResourceType.CONSUMER;
    }

    public void onMessage(Message message) {

        Properties properties = null;
        String bodyMessage = null;
        try {
            properties = readPropertiesFromMessage(message);
            bodyMessage = getBodyMessage(message);

            final String rawType = properties.getProperty("Content-Type");
            final String contentType = (rawType != null && !rawType.trim().isEmpty())
                    ? rawType
                    : MediaType.APPLICATION_JSON_VALUE;

            logger.debug("Mensaje extraído con éxito. Content-Type: {}", contentType);

            logPropertiesAndMessage(properties, bodyMessage);
            T t = getT(contentType, bodyMessage);
            if (jmsAcknowledgeListener != null) {
                jmsAcknowledgeListener.acknowledge(message);
            }
            HandlerParameters handlerParameters = new HandlerParameters();
            handlerParameters.setMessage(t);
            handlerParameters.setProperties(properties);
            handlerParameters.setHandlerError(true);
            handleMessage(handlerParameters);
        } catch (Exception e) {
            logger.error("Error consuming message ", e);
            handleError(e, (T) bodyMessage, properties);
        }
    }


    public void onMessageRecovery(String bodyMessage, Properties properties) throws Exception {

        final String rawType = properties.getProperty("Content-Type");
        final String contentType = (rawType != null && !rawType.trim().isEmpty())
                ? rawType
                : MediaType.APPLICATION_JSON_VALUE;
        HandlerParameters handlerParameters = new HandlerParameters();
        handlerParameters.setProperties(properties);
        handlerParameters.setMessage(getT(contentType, bodyMessage));
        handlerParameters.setHandlerError(false);
        handleMessage(handlerParameters);
    }

    private T getT(String contentType, String bodyMessage) throws Exception {

        Converter converter = getConverter(contentType);
        T t = (T) converter.toObject(bodyMessage, new JavaTimeModule(), new Jdk8Module());
        return t;
    }

    private String getBodyMessage(Message message) throws Exception {
        String plainMessage = null;

        if (message instanceof TextMessage) {
            plainMessage = ((TextMessage) message).getText();
        } else if (message instanceof BytesMessage) {
            BytesMessage bytesMessage = (BytesMessage) message;
            // Método estándar de la interfaz BytesMessage de Jakarta
            byte[] b = new byte[(int) bytesMessage.getBodyLength()];
            bytesMessage.readBytes(b);
            plainMessage = new String(b);
        } else {
            // En Altia aman el tipado fuerte: lanza una excepción descriptiva
            throw new IllegalArgumentException("Unsupported JMS message type: " + message.getJMSType());
        }

        return plainMessage;
    }

    private void handleMessage(HandlerParameters handlerParameters) {

        if (jmsHandlerResource == null) {
            processWithListeners(handlerParameters);
        } else {
            jmsHandlerResource.handle(this::processWithListeners, handlerParameters);
        }
    }

    public void processWithListeners(HandlerParameters handlerParameters) {

        CollectionUtils.emptyIfNull(jmsResourceListeners).stream()
                .sorted(Comparator.comparingInt(rl -> getPriorityFromListener(rl)))
                .forEach(l -> l.onBeforeProcessingMessage(handlerParameters.getProperties()));
        try {
            process((T) handlerParameters.getMessage(), handlerParameters.getProperties());
        } catch (Throwable exception) {
            logger.error("Error processing message " + this.getClass().getSimpleName(), exception);
            if (handlerParameters.isHandlerError()) {
                handleError(exception, (T) handlerParameters.getMessage(), handlerParameters.getProperties());
            } else {
                throw new RuntimeException(exception);
            }
        }
    }

    private Properties readPropertiesFromMessage(Message message) throws Exception {

        Properties properties = new Properties();
        Enumeration e = message.getPropertyNames();
        while (e.hasMoreElements()) {
            String property = e.nextElement().toString();
            properties.setProperty(property, message.getStringProperty(property));
        }
        return properties;
    }

}
