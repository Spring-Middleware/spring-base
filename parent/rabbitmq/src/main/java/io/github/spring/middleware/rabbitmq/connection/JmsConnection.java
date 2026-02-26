package io.github.spring.middleware.rabbitmq.connection;


import jakarta.jms.Connection;

import java.util.UUID;

public class JmsConnection {

    private final Connection connection;
    private final String uuid;
    private boolean started = false;

    public JmsConnection(Connection connection) {
        this.connection = connection;
        this.uuid = UUID.randomUUID().toString().toUpperCase();
    }

    public String getUuid() {
        return uuid;
    }

    public Connection getConnection() {
        return connection;
    }

    public String toString() {
        return uuid;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }
}
