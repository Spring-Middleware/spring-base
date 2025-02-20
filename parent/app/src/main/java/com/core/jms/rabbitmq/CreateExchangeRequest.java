package com.core.jms.rabbitmq;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExchangeRequest {

    private String type;
    @JsonProperty("auto_delete")
    private boolean autoDelete;
    @JsonProperty("durable")
    private boolean durable;
    @JsonProperty("internal")
    private boolean internal;

}
