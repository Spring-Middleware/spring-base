package com.middleware.error.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ErrorRequest {

    private UUID uuid;
    private LocalDateTime dateTime;
    private String hostname;
    private String serviceName;
    private String clazzName;
    private String operationName;
    private String errorMessage;
    private String stackTrace;
    private Map<String, String> data;
    private Boolean recoverable;
}
