package io.github.spring.middleware.jpa.buffer.builder;

import io.github.spring.middleware.jpa.annotations.SearchProperties;
import io.github.spring.middleware.jpa.buffer.ConditionBuffer;
import io.github.spring.middleware.jpa.buffer.JoinBuffer;
import io.github.spring.middleware.jpa.buffer.SearchPropertiesConditionBufferBuilder;
import io.github.spring.middleware.jpa.query.ParameterCounter;
import io.github.spring.middleware.jpa.search.Search;
import org.springframework.stereotype.Service;

import java.beans.BeanInfo;
import java.lang.reflect.Field;

@Service
public class SearchPropertiesConditionBufferBuilderImpl<S extends Search> extends CommonConditionBufferBuilderImpl<S>
        implements SearchPropertiesConditionBufferBuilder<S> {

    public SearchPropertiesConditionBufferBuilderImpl() {

    }

    public SearchPropertiesConditionBufferBuilderImpl(JoinBuffer joinBuffer, ParameterCounter parameterCounter) {

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
