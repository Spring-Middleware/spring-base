package com.middleware.jms.rabbitmq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RabbitBindingData {

    private String source;
    private String vhost;
    private String destination;
    private String destination_type;
    @JsonProperty("routing_key")
    private String routingKey;

}
