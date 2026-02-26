package com.middleware.jpa.buffer;

import com.middleware.jpa.query.ParameterCounter;
import com.middleware.jpa.order.OrderBy;
import com.middleware.jpa.search.Search;

import java.util.Optional;

public class QueryBuffer<T, S extends Search> {

    private SelectBuffer selectBuffer = new SelectBuffer();
    private FromBuffer fromBuffer = new FromBuffer();
    private JoinBuffer joinBuffer = new JoinBuffer();
    private OrderByBuffer orderByBuffer = new OrderByBuffer();
    private ParameterCounter parameterCounter = new ParameterCounter();
    private WhereBuffer whereBuffer = new WhereBuffer(joinBuffer, parameterCounter, true);

    public QueryBuffer(QueryBufferParameters<T, S> queryBufferParameters) {

        buildSelect(queryBufferParameters.getSearch(), queryBufferParameters.isCount());
        buildFrom(queryBufferParameters.getEntityClazz());
        Optional.ofNullable(queryBufferParameters.getSearch())
                .ifPresent(search -> {
                    try {
                        buildWhere(search);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
        if (!queryBufferParameters.isCount()) {
            buildOrderBy(queryBufferParameters.getOrderBy());
        }
    }

    private void buildSelect(S search, boolean isCount) {

        selectBuffer.buildSelect(search, isCount);
    }

    private void buildFrom(Class<T> entityClass) {

        fromBuffer.buildFrom(entityClass);
    }

    private void buildWhere(S search) throws Exception {

        whereBuffer.buildWhere(search);
    }

    private void buildOrderBy(OrderBy orderBy) {

        orderByBuffer = new OrderByBuffer();
        orderByBuffer.buildOrderBy(orderBy);
    }

    public String nextParameter() {

        return parameterCounter.next();
    }

    public String toString() {

        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append(selectBuffer).append(fromBuffer).append(joinBuffer).append(whereBuffer)
                .append(orderByBuffer);
        return queryBuffer.toString();
    }
}
