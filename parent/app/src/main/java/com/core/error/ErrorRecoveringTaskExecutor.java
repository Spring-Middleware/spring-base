package com.core.error;

import com.core.error.api.ErrorView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class ErrorRecoveringTaskExecutor {

    @Autowired
    private ErrorClient errorClient;
    @Autowired
    private ErrorRecoveryService errorRecoveryService;

    public void execute(ErrorRecoveringTask errorRecoveringTask) throws Exception {

        if (log.isDebugEnabled()) {
            log.debug("Executing recovering task " + errorRecoveringTask);
        }
        Flux<ErrorView> errors = errorClient.searchErrors(errorRecoveringTask.getErrorRecoveringTaskDefinition()
                .getErrorSearch());
        Optional.ofNullable(errors).ifPresent(err -> {
            errorRecoveryService.recoveryErrors(err).subscribe(tuple -> {
                if (log.isInfoEnabled()) {
                    UUID uuid = tuple.getT1();
                    log.info("Error " + uuid + " Recovered attempt " + tuple.getT2());
                }
            });
        });
    }

}
