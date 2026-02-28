package io.github.spring.middleware.client.params;

import org.springframework.web.bind.annotation.PathVariable;

public class PathVariableValue<T> implements ParamValue {

    private PathVariable pathVariable;
    private T value;

    public PathVariableValue(PathVariable pathVariable, T value) {
        this.pathVariable = pathVariable;
        this.value = value;
    }

    public PathVariable getPathParam() {
        return pathVariable;
    }

    public void setPathVariable(PathVariable pathVariable) {
        this.pathVariable = pathVariable;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
