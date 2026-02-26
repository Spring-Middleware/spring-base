package com.middleware.error.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorRecoveryAttemptRequest {

    private UUID errorId;
    private boolean recovered;
    private String errorRecoveryMessage;
    private LocalDateTime dateTime;

}
