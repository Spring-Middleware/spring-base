package com.core.jpa.buffer;


import com.core.jpa.buffer.factory.ConditionBufferBuilderFactory;
import com.core.jpa.query.ParameterCounter;
import com.core.jpa.search.Search;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class WhereBuffer<S extends Search> {

    private StringBuffer whereBuffer = new StringBuffer();
    private Set<ConditionBuffer> conditions = new HashSet<>();
    private JoinBuffer joinBuffer;
    private ParameterCounter parameterCounter;
    private boolean appendWhere;

    public WhereBuffer(JoinBuffer joinBuffer, ParameterCounter parameterCounter, boolean appendWhere) {

        this.joinBuffer = joinBuffer;
        this.parameterCounter = parameterCounter;
        this.appendWhere = appendWhere;
    }

    public <S extends Search> void buildWhere(S search) throws Exception {

        BeanInfo beanInfo = null;
        if (Optional.ofNullable(search).isPresent()) {
            Class<? super Search> clazzSearch = (Class) search.getClass();
            while (clazzSearch != null) {
                beanInfo = Introspector.getBeanInfo(clazzSearch);
                joinBuffer.processJoinClass(clazzSearch);
                for (Field field : clazzSearch.getDeclaredFields()) {
                    ConditionBufferBuilder conditionBufferBuilder = ConditionBufferBuilderFactory
                            .getConditionBufferBuilder(field, joinBuffer, parameterCounter);
                    if (conditionBufferBuilder != null) {
                        ConditionBuffer conditionBuffer = conditionBufferBuilder.build(search, field, beanInfo);
                        Optional.ofNullable(conditionBuffer).filter(buff -> !buff.isEmpty())
                                .ifPresent(buff -> conditions.add(buff));
                    }
                }
                clazzSearch = clazzSearch.getSuperclass();
            }
        }
        buildWhere();
    }

    private void buildWhere() {

        if (!conditions.isEmpty()) {
            if (appendWhere)
                whereBuffer.append(" WHERE ");
            boolean firstCondition = true;
            for (ConditionBuffer conditionBuffer : conditions) {
                if (!firstCondition) {
                    conditionBuffer.prependAndOr();
                }
                whereBuffer.append(conditionBuffer);
                firstCondition = false;
            }
        }
    }

    @Override
    public String toString() {

        return whereBuffer.toString();
    }
}
