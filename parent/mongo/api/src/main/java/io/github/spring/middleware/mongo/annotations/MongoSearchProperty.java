package io.github.spring.middleware.mongo.annotations;

import io.github.spring.middleware.mongo.types.ConditionType;
import io.github.spring.middleware.mongo.types.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MongoSearchProperty {

    String value() default "";

    OperationType operationType() default OperationType.IS;

    ConditionType conditionType() default ConditionType.AND;

}
