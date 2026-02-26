package io.github.spring.middleware.rabbitmq.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.jms.JMSException;


import java.util.Optional;

public class JmsConnectionConfiguration {

    @JsonProperty("tcpHost")
    private String tcpHost;
    @JsonProperty("jmsConnectionCredentials")
    private JmsConnectionCredentials jmsConnectionCredentials;
    @JsonProperty("jmsConnnectionRetryConfiguration")
    private JmsConnectionPoolConfiguration jmsConnectionPoolConfiguration;

    public String getTcpHost() {
        return tcpHost;
    }

    public void setTcpHost(String tcpHost) {
        this.tcpHost = tcpHost;
    }

    public JmsConnectionCredentials getJmsConnectionCredentials() {
        return jmsConnectionCredentials;
    }

    public void setJmsConnectionCredentials(JmsConnectionCredentials jmsConnectionCredentials) {
        this.jmsConnectionCredentials = jmsConnectionCredentials;
    }

    public JmsConnectionPoolConfiguration getJmsConnectionPoolConfiguration() {
        return Optional.ofNullable(jmsConnectionPoolConfiguration).orElse(new JmsConnectionPoolConfiguration());
    }

    public void setJmsConnectionPoolConfiguration(JmsConnectionPoolConfiguration jmsConnectionPoolConfiguration) {
        this.jmsConnectionPoolConfiguration = jmsConnectionPoolConfiguration;
    }

    public String getUrlConnection() throws JMSException {
        JmsConnectionCredentials jmsConnectionCredentials = Optional.ofNullable(getJmsConnectionCredentials()).orElseThrow(() -> new JMSException("Missing jmsConnectionCredentials"));
        StringBuffer urlConnectionStringBuffer = new StringBuffer("amqp://");
        urlConnectionStringBuffer.append(Optional.ofNullable(jmsConnectionCredentials.getUsername()).orElseThrow(() -> new JMSException("Missing username"))).append(":");
        urlConnectionStringBuffer.append(Optional.ofNullable(jmsConnectionCredentials.getPassword()).orElseThrow(() -> new JMSException("Missing password"))).append("@clientid/?brokerlist='");
        urlConnectionStringBuffer.append(Optional.ofNullable(getTcpHost()).orElseThrow(() -> new JMSException("Missing tcp host"))).append("'");
        return urlConnectionStringBuffer.toString();
    }
}
