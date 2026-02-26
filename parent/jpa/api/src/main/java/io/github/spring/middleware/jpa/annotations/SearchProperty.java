package io.github.spring.middleware.jpa.annotations;

import io.github.spring.middleware.jpa.adaptor.DataAdaptor;
import io.github.spring.middleware.jpa.buffer.SearchPropertyConditionBufferBuilder;
import io.github.spring.middleware.jpa.types.CompareOperator;
import io.github.spring.middleware.jpa.types.ConditionType;
import io.github.spring.middleware.jpa.types.IncusionOperator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@ConditionBufferBuilderClass(SearchPropertyConditionBufferBuilder.class)
public @interface SearchProperty {

    ConditionType conditionType() default ConditionType.AND;

    PreCondition preCondition() default @PreCondition;

    String value() default "";

    boolean isLike() default false;

    IncusionOperator inclusionOperator() default IncusionOperator.IN;

    Join join() default @Join;

    Concat concat() default @Concat;

    SearchPropertyParameter[] parameters() default {};

    Class<? extends DataAdaptor> adaptor() default DefaultAdaptor.class;

    CompareOperator compareOperator() default CompareOperator.EQUAL;

    boolean searchForNull() default false;

    boolean isEnum() default false;

    class DefaultAdaptor implements DataAdaptor<Object, Object> {

        public Object adapt(Object e) {

            return e;
        }
    }
}
