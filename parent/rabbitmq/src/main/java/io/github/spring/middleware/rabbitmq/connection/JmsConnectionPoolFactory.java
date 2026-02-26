package io.github.spring.middleware.rabbitmq.connection;

import jakarta.jms.JMSException;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class JmsConnectionPoolFactory extends BasePooledObjectFactory<JmsConnection> {

    private JmsConnectionManager jmsConnectionManager;

    private JmsConnectionPoolFactory(JmsConnectionManager jmsConnectionManager) {
        this.jmsConnectionManager = jmsConnectionManager;
    }

    public static JmsConnectionPoolFactory getInstance(JmsConnectionManager jmsConnectionManager) {
        return new JmsConnectionPoolFactory(jmsConnectionManager);
    }

    @Override
    public JmsConnection create() throws JMSException {
        return jmsConnectionManager.createConnection();
    }

    public void destroyObject(PooledObject<JmsConnection> pooledCJmsConnection) throws Exception {
        jmsConnectionManager.close(pooledCJmsConnection.getObject());
    }

    public void activateObject(PooledObject<JmsConnection> pooledCJmsConnection) throws Exception {
        jmsConnectionManager.start(pooledCJmsConnection.getObject());
    }

    @Override
    public PooledObject<JmsConnection> wrap(JmsConnection jmsConnection) {
        return new DefaultPooledObject<JmsConnection>(jmsConnection);
    }
}
