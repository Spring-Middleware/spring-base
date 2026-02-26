package io.github.spring.middleware.jpa.buffer.builder;

import io.github.spring.middleware.jpa.annotations.SearchProperty;
import io.github.spring.middleware.jpa.buffer.ConditionBuffer;
import io.github.spring.middleware.jpa.buffer.JoinBuffer;
import io.github.spring.middleware.jpa.buffer.SearchPropertyConditionBufferBuilder;
import io.github.spring.middleware.jpa.query.ParameterCounter;
import io.github.spring.middleware.jpa.search.Search;
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
