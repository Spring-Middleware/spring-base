package com.middleware.jms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Properties;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JmsErrorMessage<T> {

    private T message;
    private Properties properties;


}
