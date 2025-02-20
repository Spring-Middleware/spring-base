package com.core.jpa.adaptor;

public interface DataAdaptor<E, S> {

    S adapt(E e);

}
