package com.middleware.jpa.types;

public enum CompareOperator {

    EQUAL("="),
    GREATER(">"),
    LESS("<"),
    GREATER_OR_EQUAL(">="),
    LESS_OR_EQUAL("<=");

    private String value;

    CompareOperator(String value) {

        this.value = value;
    }

    public String getValue() {

        return value;
    }
}
