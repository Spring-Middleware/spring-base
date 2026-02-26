package io.github.spring.middleware.jpa.buffer.builder;

import io.github.spring.middleware.jpa.annotations.SearchPropertyExists;
import io.github.spring.middleware.jpa.buffer.ConditionBuffer;
import io.github.spring.middleware.jpa.buffer.JoinBuffer;
import io.github.spring.middleware.jpa.buffer.SearchPropertyExistsConditionBufferBuilder;
import io.github.spring.middleware.jpa.query.ParameterCounter;
import io.github.spring.middleware.jpa.search.Search;

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
