package io.github.spring.middleware.jpa.annotations;

import io.github.spring.middleware.jpa.buffer.SubSearchConditionBufferBuilder;
import io.github.spring.middleware.jpa.types.ConditionType;

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
