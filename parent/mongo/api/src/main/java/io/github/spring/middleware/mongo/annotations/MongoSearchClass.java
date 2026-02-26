package io.github.spring.middleware.mongo.annotations;

import io.github.spring.middleware.mongo.types.ConditionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MongoSearchClass {

    String value() default "";

    ConditionType conditionType() default ConditionType.AND;

    boolean isCollection() default false;

    boolean not() default false;

}
