package io.github.spring.middleware.utils;

import java.util.ArrayList;
import java.util.List;

public class PaginationUtils {

    public static <E, P> List<E> findAllPages(final Finder<E, P> finder, final P params, final int initialPage, final int pageSize) {
        int page = initialPage;
        int size = pageSize;
        List<E> result = new ArrayList<>();
        do {
            result.addAll(finder.findPage(params, page, size));
            if (result != null && !result.isEmpty()) {
                page++;
            }
        } while (result != null && !result.isEmpty());
        return result;
    }

    public static interface Finder<E, P> {
        List<E> findPage(P params, int page, int size);
    }
}
