package io.github.spring.middleware.rabbitmq.connection;

import io.github.spring.middleware.rabbitmq.configuration.JmsConnectionConfiguration;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JmsConnectionManager {

    private Logger logger = LoggerFactory.getLogger(JmsConnectionManager.class);
    private ConnectionFactory connectionFactory;
    private JmsConnectionConfiguration jmsConnectionConfiguration;

    public JmsConnectionManager(ConnectionFactory connectionFactory,
                                JmsConnectionConfiguration jmsConnectionConfiguration) {

        this.connectionFactory = connectionFactory;
        this.jmsConnectionConfiguration = jmsConnectionConfiguration;
    }

    public void close(JmsConnection jmsConnection) throws JMSException {

        try {
            jmsConnection.getConnection().close();
            jmsConnection.setStarted(false);
            logger.debug("Close connection " + jmsConnection + " closed");
        } catch (Exception ex) {
            logger.error("Can't close connection " + jmsConnection, ex);
            throw new JMSException("Can't close connection " + jmsConnection);
        }
    }

    public JmsConnection createConnection() throws JMSException {

        JmsConnection jmsConnection = null;
        int numberTries = 0;
        JMSException jmsException = null;
        while (jmsConnection == null && numberTries < 3) {
            try {
                jmsConnection = new JmsConnection(this.connectionFactory.createConnection());
                logger.debug("Created new connection " + jmsConnection + " with " +
                        jmsConnectionConfiguration.getUrlConnection());
            } catch (Exception ex) {
                logger.error("Can't created a new connection with " + jmsConnectionConfiguration.getUrlConnection(),
                        ex);
                jmsException = new JMSException(
                        "Can't created a new connection with " + jmsConnectionConfiguration.getUrlConnection());
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException iex) {
                    throw new JMSException(iex.getMessage());
                }

            } finally {
                numberTries++;
            }
        }
        if (jmsConnection == null) {
            throw jmsException;
        }
        return jmsConnection;
    }

    public void start(JmsConnection jmsConnection) throws JMSException {

        try {
            if (!jmsConnection.isStarted()) {
                jmsConnection.getConnection().start();
                jmsConnection.setStarted(true);
                logger.debug("Started connection " + jmsConnection);
            }
        } catch (Exception ex) {
            logger.error("Can't start connection " + jmsConnection, ex);
            throw new JMSException("Can't start connection " + jmsConnection);
        }
    }

}
