package io.github.spring.middleware.error.api;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ErrorView {

    private UUID uuid;
    private LocalDateTime dateTime;
    private LocalDateTime lastRecoveryDateTime;
    private String hostname;
    private String serviceName;
    private String clazzName;
    private String operationName;
    private String errorMessage;
    private Map<String, String> data;
    private ErrorRecoverStatus recoverStatus;
    private Collection<ErrorRecoveryAttemptView> recoveryAttempts;

}
