package com.middleware.jpa.buffer.builder;


import com.middleware.jpa.buffer.ConditionBufferBuilder;
import com.middleware.jpa.buffer.JoinBuffer;
import com.middleware.jpa.query.ParameterCounter;
import com.middleware.jpa.search.Search;
import com.middleware.jpa.utils.MethodInvoker;

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
