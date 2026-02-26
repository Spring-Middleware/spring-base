package com.middleware.controller;

import com.middleware.error.ErrorClient;
import com.middleware.error.ErrorRecoveryService;
import com.middleware.error.api.ErrorRecoveryAttemptView;
import com.middleware.error.api.ErrorSearch;
import com.middleware.error.api.ErrorView;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(ErrorRecoveryController.BASE_MAPPING)
@OpenAPIDefinition(info = @Info(title = "Error Recovery Controller"))
public class ErrorRecoveryController extends CommonsController {

    public static final String BASE_MAPPING = "/errorRecovery";

    @Autowired
    private ErrorClient errorClient;
    @Autowired
    private ErrorRecoveryService errorRecoveryService;

    @PostMapping
    public Flux<Tuple2<UUID, ErrorRecoveryAttemptView>> recovery(ErrorSearch errorSearch) throws Exception {

        Flux<ErrorView> errors = errorClient.searchErrors(errorSearch);
        return Optional.ofNullable(errors).map(err -> errorRecoveryService.recoveryErrors(err)).orElse(null);
    }

}
