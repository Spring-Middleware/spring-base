package com.middleware.cache.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisCacheConfiguration {

    int ttl() default 0;

    ChronoUnit chronoUnit() default ChronoUnit.MINUTES;
    String ttlString() default "";

    String chronoUnitString() default "";
}
