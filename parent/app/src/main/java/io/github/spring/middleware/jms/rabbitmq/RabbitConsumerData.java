package io.github.spring.middleware.jms.rabbitmq;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RabbitConsumerData {

    @JsonProperty("ack_required")
    private boolean ackRequired;

    @JsonProperty("active")
    private boolean active;

    @JsonProperty("activity_status")
    private String activityStatus;

    @JsonProperty("queue")
    private RabbitQueueData queue;

}

