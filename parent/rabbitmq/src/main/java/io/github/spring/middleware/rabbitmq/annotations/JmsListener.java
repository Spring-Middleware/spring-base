package io.github.spring.middleware.rabbitmq.annotations;

import io.github.spring.middleware.rabbitmq.annotations.listener.JmsAll;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JmsListener {

    Class<? extends Annotation> value() default JmsAll.class;

    int priority() default 0;

}
