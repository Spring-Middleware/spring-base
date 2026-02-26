package com.middleware.jpa.annotations;


import com.middleware.jpa.buffer.SearchPropertyExistsConditionBufferBuilder;
import com.middleware.jpa.types.ConditionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@ConditionBufferBuilderClass(SearchPropertyExistsConditionBufferBuilder.class)
public @interface SearchPropertyExists {

    String value() default "";

    ConditionType conditionType() default ConditionType.AND;
}
