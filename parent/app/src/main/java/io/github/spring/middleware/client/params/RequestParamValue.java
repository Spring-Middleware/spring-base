package io.github.spring.middleware.client.params;

import org.springframework.web.bind.annotation.RequestParam;


public class RequestParamValue<T> implements ParamValue {

    private RequestParam queryParam;
    private T value;

    public RequestParamValue(RequestParam queryParam, T value) {
        this.queryParam = queryParam;
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public RequestParam getQueryParam() {
        return queryParam;
    }

    public void setQueryParam(RequestParam queryParam) {
        this.queryParam = queryParam;
    }
}
