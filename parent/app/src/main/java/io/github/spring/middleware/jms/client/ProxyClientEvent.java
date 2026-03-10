package io.github.spring.middleware.jms.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProxyClientEvent {

    private String clientName;
    private ProxyClientEventType eventType;

}
