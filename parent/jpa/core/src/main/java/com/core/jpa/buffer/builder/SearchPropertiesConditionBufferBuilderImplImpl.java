package com.core.jpa.buffer.builder;

import com.core.jpa.annotations.SearchProperties;
import com.core.jpa.buffer.ConditionBuffer;
import com.core.jpa.buffer.JoinBuffer;
import com.core.jpa.buffer.SearchPropertiesConditionBufferBuilder;
import com.core.jpa.query.ParameterCounter;
import com.core.jpa.search.Search;
import org.springframework.stereotype.Service;

import java.beans.BeanInfo;
import java.lang.reflect.Field;

@Service
public class SearchPropertiesConditionBufferBuilderImplImpl<S extends Search> extends CommonConditionBufferBuilderImpl<S>
        implements SearchPropertiesConditionBufferBuilder<S> {

    public SearchPropertiesConditionBufferBuilderImplImpl() {

    }

    public SearchPropertiesConditionBufferBuilderImplImpl(JoinBuffer joinBuffer, ParameterCounter parameterCounter) {

        super(joinBuffer, parameterCounter);
    }

    public ConditionBuffer build(S search, Field field, BeanInfo beanInfo) {

        SearchProperties searchProperties = field.getAnnotation(SearchProperties.class);
        Object value = invokeReadMethod(beanInfo, search, field);
        ConditionBuffer conditionBuffer = new ConditionBuffer(joinBuffer, parameterCounter,
                searchProperties.conditionType());
        conditionBuffer.buildSearchPropertiesCondition(searchProperties, value);
        return conditionBuffer;
    }
}
