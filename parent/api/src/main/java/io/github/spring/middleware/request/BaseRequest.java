package io.github.spring.middleware.request;

import io.github.spring.middleware.annotation.MappedContextParam;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseRequest {

    @MappedContextParam("REQUEST-ID")
    private String requestUUid;

}
