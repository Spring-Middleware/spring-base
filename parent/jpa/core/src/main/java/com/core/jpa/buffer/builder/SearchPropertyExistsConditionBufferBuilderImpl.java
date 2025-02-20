package com.core.jpa.buffer.builder;

import com.core.jpa.annotations.SearchPropertyExists;
import com.core.jpa.buffer.ConditionBuffer;
import com.core.jpa.buffer.JoinBuffer;
import com.core.jpa.buffer.SearchPropertyExistsConditionBufferBuilder;
import com.core.jpa.query.ParameterCounter;
import com.core.jpa.search.Search;

import java.beans.BeanInfo;
import java.lang.reflect.Field;

public class SearchPropertyExistsConditionBufferBuilderImpl<S extends Search> extends CommonConditionBufferBuilderImpl<S>
        implements SearchPropertyExistsConditionBufferBuilder<S> {

    public SearchPropertyExistsConditionBufferBuilderImpl(){

    }

    public SearchPropertyExistsConditionBufferBuilderImpl(JoinBuffer joinBuffer, ParameterCounter parameterCounter) {

        super(joinBuffer, parameterCounter);
    }

    public ConditionBuffer build(S search, Field field, BeanInfo beanInfo) {

        SearchPropertyExists searchPropertyExists = field.getAnnotation(SearchPropertyExists.class);
        Boolean exists = (Boolean) invokeReadMethod(beanInfo, search, field);
        ConditionBuffer conditionBuffer = new ConditionBuffer(joinBuffer, parameterCounter,
                searchPropertyExists.conditionType());
        conditionBuffer.buldSearchPropertyExists(searchPropertyExists, exists);
        return conditionBuffer;
    }

}
