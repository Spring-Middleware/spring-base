package io.github.spring.middleware.jpa.types;

public enum CompareOperator {

    EQUAL("="),
    GREATER_THAN(">"),
    LESS_THAN("<"),
    GREATER_OR_EQUAL_THAN(">="),
    LESS_OR_EQUAL_THAN("<=");

    private String value;

    CompareOperator(String value) {

        this.value = value;
    }

    public String getValue() {

        return value;
    }
}
