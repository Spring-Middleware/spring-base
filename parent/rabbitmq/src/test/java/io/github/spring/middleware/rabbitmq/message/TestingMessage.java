package io.github.spring.middleware.rabbitmq.message;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestingMessage {

    @JsonProperty("message")
    private String message;
    private Integer id;

    public TestingMessage() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
