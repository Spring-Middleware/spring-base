package io.github.spring.middleware.controller;

import io.github.spring.middleware.manager.RegistrationManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
public class ResourcesController {

    private final ObjectProvider<RegistrationManager> registrationManagerObjectProvider;

    @GetMapping("/register")
    public ResponseEntity<String> register() throws Exception {
        RegistrationManager registrationManager = registrationManagerObjectProvider.getIfAvailable();
        if (registrationManager == null) {
            return ok("No registration manager found");
        }
        registrationManager.registerResources();
        return ok("Resources and schema registered");
    }
}
