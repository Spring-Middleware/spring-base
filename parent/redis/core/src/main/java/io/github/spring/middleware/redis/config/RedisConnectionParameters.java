package io.github.spring.middleware.redis.config;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "redis")
public class RedisConnectionParameters {

    private String host;
    private int port;
    private int database;
    private int timeout;
    private int maxReconnectiongTime;
    private int waitTimeBetweenRetries;
    private boolean isCluster;
    private int maxPoolConn;
    private int maxIdlePoolConn;
    private int minIdlePoolConn;
    private int maxWaitMillis;


    public int getMaxReconnectiongTime() {

        return maxReconnectiongTime;
    }

    public void setMaxReconnectiongTime(int maxReconnectiongTime) {

        this.maxReconnectiongTime = maxReconnectiongTime;
    }

    public int getWaitTimeBetweenRetries() {

        return waitTimeBetweenRetries;
    }

    public void setWaitTimeBetweenRetries(int waitTimeBetweenRetries) {

        this.waitTimeBetweenRetries = waitTimeBetweenRetries;
    }

    public String getHost() {

        return host;
    }

    public void setHost(String host) {

        this.host = host;
    }

    public int getPort() {

        return port;
    }

    public void setPort(int port) {

        this.port = port;
    }

    public int getDatabase() {

        return database;
    }

    public void setDatabase(int database) {

        this.database = database;
    }

    public int getTimeout() {

        return timeout;
    }

    public void setTimeout(int timeout) {

        this.timeout = timeout;
    }

    public GenericObjectPoolConfig getGenericObjectPoolConfig() {

        GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
        genericObjectPoolConfig.setMaxTotal(getMaxPoolConn());
        genericObjectPoolConfig.setMaxIdle(getMaxIdlePoolConn());
        genericObjectPoolConfig.setMinIdle(getMinIdlePoolConn());
        genericObjectPoolConfig.setMaxWaitMillis(getMaxWaitMillis());
        return genericObjectPoolConfig;
    }

    public boolean isCluster() {

        return isCluster;
    }

    public void setCluster(boolean cluster) {

        isCluster = cluster;
    }

    public int getMaxPoolConn() {

        return maxPoolConn;
    }

    public void setMaxPoolConn(int maxPoolConn) {

        this.maxPoolConn = maxPoolConn;
    }

    public int getMaxIdlePoolConn() {

        return maxIdlePoolConn;
    }

    public void setMaxIdlePoolConn(int maxIdlePoolConn) {

        this.maxIdlePoolConn = maxIdlePoolConn;
    }

    public int getMinIdlePoolConn() {

        return minIdlePoolConn;
    }

    public void setMinIdlePoolConn(int minIdlePoolConn) {

        this.minIdlePoolConn = minIdlePoolConn;
    }

    public int getMaxWaitMillis() {

        return maxWaitMillis;
    }

    public void setMaxWaitMillis(int maxWaitMillis) {

        this.maxWaitMillis = maxWaitMillis;
    }
}
