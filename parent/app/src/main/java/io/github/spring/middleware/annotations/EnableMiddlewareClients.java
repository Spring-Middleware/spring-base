package io.github.spring.middleware.annotations;

import io.github.spring.middleware.client.registar.MiddlewareClientRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(MiddlewareClientRegistrar.class)
public @interface EnableMiddlewareClients {
    String[] basePackages() default {};
}
