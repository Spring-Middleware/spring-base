package com.core.jpa.order;

import com.core.jpa.types.OrderType;

import java.util.List;

public class OrderBy {

    private List<String> orderBy;
    private OrderType orderType;

    public OrderBy(List<String> orderBy, OrderType orderType) {

        this.orderBy = orderBy;
        this.orderType = orderType;
    }

    public OrderBy() {

    }

    public List<String> getOrderBy() {

        return orderBy;
    }

    public void setOrderBy(List<String> orderBy) {

        this.orderBy = orderBy;
    }

    public OrderType getOrderType() {

        return orderType;
    }

    public void setOrderType(OrderType orderType) {

        this.orderType = orderType;
    }

    public static OrderBy from(List<String> orderByList, OrderType orderType) {

        return new OrderBy(orderByList, orderType);
    }
}
