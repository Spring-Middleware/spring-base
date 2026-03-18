package io.github.spring.middleware.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public class PageRequestUtils {


    public static PageRequest buildPageRequest(Integer page, Integer size, String sort) {
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size);
        }

        // Supported formats:
        //   "name"                    -> ORDER BY name ASC
        //   "name,desc"               -> ORDER BY name DESC
        //   "name,asc;status,desc"    -> ORDER BY name ASC, status DESC
        String[] sortSpecs = sort.split(";");
        List<Sort.Order> orders = new ArrayList<>();

        for (String spec : sortSpecs) {
            if (spec == null || spec.isBlank()) {
                continue;
            }
            String[] parts = spec.split(",");
            String property = parts[0].trim();
            if (property.isEmpty()) {
                continue;
            }

            String directionToken = parts.length > 1 ? parts[1].trim().toLowerCase() : "asc";
            Sort.Direction direction = "desc".equals(directionToken)
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;

            orders.add(new Sort.Order(direction, property));
        }

        if (orders.isEmpty()) {
            return PageRequest.of(page, size);
        }

        return PageRequest.of(page, size, Sort.by(orders));
    }
}
