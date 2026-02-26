package com.middleware.jpa.adaptor;

public interface DataAdaptor<E, S> {

    S adapt(E e);

}
