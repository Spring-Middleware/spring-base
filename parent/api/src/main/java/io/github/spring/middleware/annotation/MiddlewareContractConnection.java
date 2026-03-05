package io.github.spring.middleware.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface MiddlewareContractConnection {

    String timeout() default "30000";

    String maxConnections() default "50";

    String maxConcurrentCalls() default "200";

    String maxRetries() default "3";

    String retryBackoffMillis() default "1000";

}
