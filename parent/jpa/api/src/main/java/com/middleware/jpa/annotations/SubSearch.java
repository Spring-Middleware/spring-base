package com.middleware.jpa.annotations;

import com.middleware.jpa.buffer.SubSearchConditionBufferBuilder;
import com.middleware.jpa.types.ConditionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@ConditionBufferBuilderClass(SubSearchConditionBufferBuilder.class)
public @interface SubSearch {

    ConditionType conditionType() default ConditionType.AND;

}
