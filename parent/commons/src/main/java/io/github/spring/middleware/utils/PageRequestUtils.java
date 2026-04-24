package io.github.spring.middleware.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Arrays;
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

    public static Sort parseSort(String sort) {

        if (sort == null || sort.isBlank()) {
            return Sort.unsorted();
        }

        String[] parts = sort.split(",");

        if (parts.length == 0) {
            return Sort.unsorted();
        }

        // Último elemento puede ser dirección
        String last = parts[parts.length - 1].trim();

        Sort.Direction direction;
        List<String> fields;

        if (last.equalsIgnoreCase("asc") || last.equalsIgnoreCase("desc")) {
            direction = Sort.Direction.fromString(last);
            fields = Arrays.stream(parts, 0, parts.length - 1)
                    .map(String::trim)
                    .toList();
        } else {
            // No direction → default ASC
            direction = Sort.Direction.ASC;
            fields = Arrays.stream(parts)
                    .map(String::trim)
                    .toList();
        }

        return Sort.by(direction, fields.toArray(new String[0]));
    }


}
