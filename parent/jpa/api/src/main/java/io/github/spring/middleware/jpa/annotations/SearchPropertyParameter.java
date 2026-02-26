package io.github.spring.middleware.jpa.annotations;

import io.github.spring.middleware.jpa.search.Search;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SearchPropertyParameter {

    Class<? extends Function<? extends Search, ?>> function();

    String name();

}
