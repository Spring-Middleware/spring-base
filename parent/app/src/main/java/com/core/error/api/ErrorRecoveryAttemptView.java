package com.core.error.api;

import com.core.view.View;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@Data
public class ErrorRecoveryAttemptView implements View {

    private UUID uuid;
    private LocalDateTime dateTime;
    private boolean recovered;
    private String errorRecoveryMessage;

}
