package com.core.mongo.annotations;

import com.core.mongo.types.ConditionType;
import com.core.mongo.types.OperationType;

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
