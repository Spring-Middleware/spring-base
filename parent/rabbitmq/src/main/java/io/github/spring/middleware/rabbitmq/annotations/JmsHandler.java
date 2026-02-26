package io.github.spring.middleware.rabbitmq.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JmsHandler {

    Class<? extends Annotation> value();


}
