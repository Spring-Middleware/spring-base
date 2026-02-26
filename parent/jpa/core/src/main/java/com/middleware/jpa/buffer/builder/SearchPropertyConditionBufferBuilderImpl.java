package com.middleware.jpa.buffer.builder;

import com.middleware.jpa.annotations.SearchProperty;
import com.middleware.jpa.buffer.ConditionBuffer;
import com.middleware.jpa.buffer.JoinBuffer;
import com.middleware.jpa.buffer.SearchPropertyConditionBufferBuilder;
import com.middleware.jpa.query.ParameterCounter;
import com.middleware.jpa.search.Search;
import org.springframework.stereotype.Service;

import java.beans.BeanInfo;
import java.lang.reflect.Field;

@Service
public class SearchPropertyConditionBufferBuilderImpl<S extends Search> extends CommonConditionBufferBuilderImpl<S>
        implements SearchPropertyConditionBufferBuilder<S> {

    public SearchPropertyConditionBufferBuilderImpl(){
    }

    public SearchPropertyConditionBufferBuilderImpl(JoinBuffer joinBuffer, ParameterCounter parameterCounter) {

        super(joinBuffer, parameterCounter);
    }

    public ConditionBuffer build(S search, Field field, BeanInfo beanInfo) {

        SearchProperty searchProperty = field.getAnnotation(SearchProperty.class);
        Object value = invokeReadMethod(beanInfo, search, field);
        ConditionBuffer conditionBuffer = new ConditionBuffer(joinBuffer, parameterCounter,
                searchProperty.conditionType());
        conditionBuffer.buildSearchPropertyCondition(searchProperty, value);
        return conditionBuffer;
    }

}
