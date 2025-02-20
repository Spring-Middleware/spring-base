package com.core.jpa.annotations;


import com.core.jpa.buffer.ConditionBufferBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionBufferBuilderClass {

    Class<? extends ConditionBufferBuilder> value();
}
