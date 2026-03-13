package io.github.spring.middleware.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/_alive")
    public Map<String,String> alive() {
        return Map.of("status", "UP");
    }

}
