package com.core.jpa.buffer;

public class FromBuffer {

    private StringBuffer fromBuffer = new StringBuffer();

    public <T> void buildFrom(Class<T> entityClass) {

        fromBuffer.append(" FROM ").append(entityClass.getName()).append(" c ");
    }

    @Override
    public String toString() {

        return fromBuffer.toString();
    }
}
