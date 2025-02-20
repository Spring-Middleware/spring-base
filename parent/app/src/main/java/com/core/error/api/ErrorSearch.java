package com.core.error.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorSearch {

    private UUID uuid;
    private ZonedDateTime dateTimeFrom;
    private ZonedDateTime dateTimeTo;
    private String serviceName;
    private String operationName;
    private String clazzName;
    private String hostname;

    private ErrorRecoveryAttemptSearch errorRecoveryAttemptSearch;
    private Integer maxAttempts;


}
