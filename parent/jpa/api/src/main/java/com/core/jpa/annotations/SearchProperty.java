package com.core.jpa.annotations;

import com.core.jpa.adaptor.DataAdaptor;
import com.core.jpa.buffer.SearchPropertyConditionBufferBuilder;
import com.core.jpa.types.CompareOperator;
import com.core.jpa.types.ConditionType;
import com.core.jpa.types.IncusionOperator;

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
