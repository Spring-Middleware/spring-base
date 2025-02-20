package com.core.mongo.annotations;

import com.core.mongo.types.ConditionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MongoSearchProperties {

    MongoSearchProperty[] value();

    ConditionType conditionType() default ConditionType.AND;

}

