package com.core.jpa.pagination;


import com.core.data.Pagination;

import java.util.ArrayList;
import java.util.List;

public abstract class Paginable<T> {

    public List<T> paginate(Pagination pagination) {

        List<T> allData = getData();
        if (pagination != null) {
            int init = (pagination.getPageNumber() - 1) * pagination.getPageSize();
            int fin = init + pagination.getPageSize();
            if (init < allData.size()) {
                return allData.subList(init, Math.min(fin, allData.size()));
            } else {
                return new ArrayList<>();
            }
        } else {
            return allData;
        }
    }

    protected abstract List<T> getData();
}
