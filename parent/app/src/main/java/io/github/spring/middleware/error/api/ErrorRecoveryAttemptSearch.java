package io.github.spring.middleware.error.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorRecoveryAttemptSearch {

    private Boolean recovered;
}
