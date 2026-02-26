package io.github.spring.middleware.jpa.adaptor;

public interface DataAdaptor<E, S> {

    S adapt(E e);

}
