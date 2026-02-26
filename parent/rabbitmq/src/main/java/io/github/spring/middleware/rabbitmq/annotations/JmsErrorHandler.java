package io.github.spring.middleware.rabbitmq.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JmsErrorHandler {

    Class<? extends Annotation> value();

    int priority() default 0;

}
