package com.middleware.error;

import com.middleware.error.api.ErrorRecoveryAttemptView;
import com.middleware.error.api.ErrorView;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

import java.util.UUID;

public interface ErrorRecoveryService {

    Flux<Tuple2<UUID, ErrorRecoveryAttemptView>> recoveryErrors(Flux<ErrorView> errorViews);

}
