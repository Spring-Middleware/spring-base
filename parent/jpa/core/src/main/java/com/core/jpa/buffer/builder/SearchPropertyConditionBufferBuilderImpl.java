package com.core.jpa.buffer.builder;

import com.core.jpa.annotations.SearchProperty;
import com.core.jpa.buffer.ConditionBuffer;
import com.core.jpa.buffer.JoinBuffer;
import com.core.jpa.buffer.SearchPropertyConditionBufferBuilder;
import com.core.jpa.query.ParameterCounter;
import com.core.jpa.search.Search;
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
