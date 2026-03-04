package io.github.spring.middleware.controller;

import io.github.spring.middleware.manager.RegistrationManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
public class ResourcesController {

    private final RegistrationManager registrationManager;

    @GetMapping("/register")
    public ResponseEntity<String> register() throws Exception {
        registrationManager.registerResources();
        return ok("Resources and schema registered");
    }
}
