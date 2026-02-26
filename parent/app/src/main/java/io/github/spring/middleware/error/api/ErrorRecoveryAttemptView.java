package io.github.spring.middleware.error.api;

import io.github.spring.middleware.view.View;
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
