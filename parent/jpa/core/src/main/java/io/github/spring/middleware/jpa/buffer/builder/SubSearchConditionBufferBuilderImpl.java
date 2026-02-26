package io.github.spring.middleware.jpa.buffer.builder;

import io.github.spring.middleware.jpa.annotations.SubSearch;
import io.github.spring.middleware.jpa.buffer.ConditionBuffer;
import io.github.spring.middleware.jpa.buffer.JoinBuffer;
import io.github.spring.middleware.jpa.buffer.SubSearchConditionBufferBuilder;
import io.github.spring.middleware.jpa.query.ParameterCounter;
import io.github.spring.middleware.jpa.search.Search;

import java.beans.BeanInfo;
import java.lang.reflect.Field;

public class SubSearchConditionBufferBuilderImpl<S extends Search> extends CommonConditionBufferBuilderImpl<S>
        implements SubSearchConditionBufferBuilder<S> {

    public SubSearchConditionBufferBuilderImpl() {

    }

    public SubSearchConditionBufferBuilderImpl(JoinBuffer joinBuffer, ParameterCounter parameterCounter) {

        super(joinBuffer, parameterCounter);
    }

    public ConditionBuffer build(S search, Field field, BeanInfo beanInfo) throws Exception {

        SubSearch subSearch = field.getAnnotation(SubSearch.class);
        S searchOr = (S) invokeReadMethod(beanInfo, search, field);
        ConditionBuffer conditionBuffer = new ConditionBuffer(joinBuffer, parameterCounter, subSearch.conditionType());
        conditionBuffer.buildSubSearch(subSearch, searchOr);
        return conditionBuffer;
    }
}
