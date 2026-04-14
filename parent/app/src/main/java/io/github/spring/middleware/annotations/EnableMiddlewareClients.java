package io.github.spring.middleware.annotations;


import io.github.spring.middleware.client.registar.MiddlewareClientRegistrar;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnProperty(prefix = "middleware.clients", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(MiddlewareClientRegistrar.class)
public @interface EnableMiddlewareClients {
    String[] basePackages() default {};
}
