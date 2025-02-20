package com.core.jpa.buffer.builder;

import com.core.jpa.annotations.SubSearch;
import com.core.jpa.buffer.ConditionBuffer;
import com.core.jpa.buffer.JoinBuffer;
import com.core.jpa.buffer.SubSearchConditionBufferBuilder;
import com.core.jpa.query.ParameterCounter;
import com.core.jpa.search.Search;

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
