package io.github.spring.middleware.error;

import io.github.spring.middleware.error.api.ErrorRecoveryAttemptView;
import io.github.spring.middleware.error.api.ErrorView;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

import java.util.UUID;

public interface ErrorRecoveryService {

    Flux<Tuple2<UUID, ErrorRecoveryAttemptView>> recoveryErrors(Flux<ErrorView> errorViews);

}
