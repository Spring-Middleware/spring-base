package io.github.spring.middleware.jpa.annotations;

import io.github.spring.middleware.jpa.types.ConditionType;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PreCondition {

    String condition() default StringUtils.EMPTY;

    ConditionType conditionType() default ConditionType.AND;

}
