package io.github.spring.middleware.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorSpanStep {
    private String service;
    private String method;
    private String url;
    private Integer httpStatus;
    private String requestId;
}
