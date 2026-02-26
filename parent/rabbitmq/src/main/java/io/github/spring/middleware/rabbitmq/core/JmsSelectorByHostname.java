package io.github.spring.middleware.rabbitmq.core;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Properties;

public class JmsSelectorByHostname implements JmsSelector {

    private final Logger logger = LoggerFactory.getLogger(JmsSelectorByHostname.class);

    public Properties properties() {
        Properties properties = null;
        try {
            properties = new Properties();
            properties.put("Host", InetAddress.getLocalHost().getHostName());
        } catch (Exception e) {
            logger.warn("Cant't create properties ", e);
        }
        return properties;
    }

}
