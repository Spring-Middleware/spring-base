package io.github.spring.middleware.jpa.pagination;

import java.util.List;

public class PaginableResultDB<T> extends Paginable {

    public List<T> entities;

    public PaginableResultDB(List<T> entities) {

        this.entities = entities;
    }

    protected List<T> getData() {

        return entities;
    }

}
