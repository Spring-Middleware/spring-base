package com.middleware.jpa.buffer.builder;

import com.middleware.jpa.annotations.SubSearch;
import com.middleware.jpa.buffer.ConditionBuffer;
import com.middleware.jpa.buffer.JoinBuffer;
import com.middleware.jpa.buffer.SubSearchConditionBufferBuilder;
import com.middleware.jpa.query.ParameterCounter;
import com.middleware.jpa.search.Search;

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
