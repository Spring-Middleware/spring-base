package io.github.spring.middleware.rabbitmq.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JmsConnectionCredentials {

    @JsonProperty("username")
    private String username;
    @JsonProperty("password")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
