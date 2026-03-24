package io.github.spring.middleware.kafka.core.registrar;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(EnableMiddlewareKafkaListenersRegistrar.class)
public @interface EnableMiddlewareKafkaListeners {

    String[] basePackages() default {};
}
