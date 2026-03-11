package io.github.spring.middleware.jms.rabbitmq;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RabbitQueueData {

    @JsonProperty("name")
    private String name;

    @JsonProperty("vhost")
    private String vhost;

}
