package io.github.spring.middleware.jpa.buffer.builder;


import io.github.spring.middleware.jpa.buffer.ConditionBufferBuilder;
import io.github.spring.middleware.jpa.buffer.JoinBuffer;
import io.github.spring.middleware.jpa.query.ParameterCounter;
import io.github.spring.middleware.jpa.search.Search;
import io.github.spring.middleware.jpa.utils.MethodInvoker;

import java.beans.BeanInfo;
import java.lang.reflect.Field;

public abstract class CommonConditionBufferBuilderImpl<S extends Search> implements ConditionBufferBuilder<S> {

    protected JoinBuffer joinBuffer;
    protected ParameterCounter parameterCounter;

    public CommonConditionBufferBuilderImpl() {
    }

    public CommonConditionBufferBuilderImpl(JoinBuffer joinBuffer, ParameterCounter parameterCounter) {

        this.joinBuffer = joinBuffer;
        this.parameterCounter = parameterCounter;
    }

    protected <R> R invokeReadMethod(BeanInfo beanInfo, S search, Field field) {

        return MethodInvoker.invokeReadMethod(beanInfo, search, field);
    }

    public void setJoinBuffer(JoinBuffer joinBuffer) {

        this.joinBuffer = joinBuffer;
    }

    public void setParameterCounter(ParameterCounter parameterCounter) {

        this.parameterCounter = parameterCounter;
    }
}
