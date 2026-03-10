package io.github.spring.middleware.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MiddlewareCircuitBreaker {

    String enabled() default "true";

    String failureRateThreshold() default "50";

    String minimumNumberOfCalls() default "10";

    String slidingWindowSize() default "20";

    String permittedNumberOfCallsInHalfOpenState() default "3";

    String waitDurationInOpenStateMs() default "10000";

    String[] statusShouldOpenBreaker() default {"5xx"};

    String[] statusShouldIgnoreBreaker() default {"4xx"};

}
