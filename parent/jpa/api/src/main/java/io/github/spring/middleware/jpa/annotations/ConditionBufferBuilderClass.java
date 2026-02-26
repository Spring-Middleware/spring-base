package io.github.spring.middleware.jpa.annotations;


import io.github.spring.middleware.jpa.buffer.ConditionBufferBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionBufferBuilderClass {

    Class<? extends ConditionBufferBuilder> value();
}
