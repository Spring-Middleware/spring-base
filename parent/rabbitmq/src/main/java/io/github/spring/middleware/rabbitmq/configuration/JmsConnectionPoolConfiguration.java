package io.github.spring.middleware.rabbitmq.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JmsConnectionPoolConfiguration {

    @JsonProperty("minIdle")
    private int minIdle = 1;
    @JsonProperty("maxIdle")
    private int maxIdle = 3;
    @JsonProperty("maxTotal")
    private int maxTotal = 10;

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

    public int getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }
}
