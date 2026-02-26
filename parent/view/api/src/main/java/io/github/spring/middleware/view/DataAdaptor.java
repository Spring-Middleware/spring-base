package io.github.spring.middleware.view;

public interface DataAdaptor<E, S> {

    S adapt(E e);

}
