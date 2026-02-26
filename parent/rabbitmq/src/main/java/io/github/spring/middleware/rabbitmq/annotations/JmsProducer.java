package io.github.spring.middleware.rabbitmq.annotations;


import jakarta.jms.Session;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JmsProducer {

    boolean transacted() default false;

    int acknoledgement() default Session.AUTO_ACKNOWLEDGE;

    JmsBinding[] bindings() default {@JmsBinding};


}
