package io.github.spring.middleware.jms.rabbitmq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExchangeData {

    private String name;
    private String error;
    private String reason;

}
