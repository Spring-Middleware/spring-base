package io.github.spring.middleware.jpa.buffer;

import java.util.Optional;
import io.github.spring.middleware.jpa.order.OrderBy;

public class OrderByBuffer {

    private StringBuffer orderByBuffer = new StringBuffer();

    public StringBuffer buildOrderBy(OrderBy orderBy) {

        if (Optional.ofNullable(orderBy.getOrderBy()).map(list -> !list.isEmpty()).orElse(Boolean.FALSE)) {
            orderByBuffer.append(" ORDER BY ");
            for (String order : orderBy.getOrderBy()) {
                orderByBuffer.append("c.").append(order).append(",");
            }
            orderByBuffer.setLength(orderByBuffer.length() - 1);
            if (orderBy.getOrderType() != null) {
                orderByBuffer.append(" ").append(orderBy.getOrderType().toString());
            }
        }
        return orderByBuffer;
    }

    @Override
    public String toString() {

        return orderByBuffer.toString();
    }
}
