package io.github.spring.middleware.common.view;

import java.util.HashSet;
import java.util.Set;

public class VisitedEntities<E> {

    private Set<E> visited = new HashSet<>();

    public void add(E e) {

        visited.add(e);
    }

    public boolean isVisited(E e) {

        return visited.contains(e);
    }

}
