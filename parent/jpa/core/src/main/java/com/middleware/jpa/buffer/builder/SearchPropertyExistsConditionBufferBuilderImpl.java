package com.middleware.jpa.buffer.builder;

import com.middleware.jpa.annotations.SearchPropertyExists;
import com.middleware.jpa.buffer.ConditionBuffer;
import com.middleware.jpa.buffer.JoinBuffer;
import com.middleware.jpa.buffer.SearchPropertyExistsConditionBufferBuilder;
import com.middleware.jpa.query.ParameterCounter;
import com.middleware.jpa.search.Search;

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
