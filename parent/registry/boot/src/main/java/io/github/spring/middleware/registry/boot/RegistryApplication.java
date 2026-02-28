package io.github.spring.middleware.registry.boot;

import io.github.spring.middleware.annotations.EnableMiddlewareClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication(scanBasePackages = "io.github.spring.middleware")
@EnableMiddlewareClients(basePackages = "io.github.spring.middleware.client")
public class RegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegistryApplication.class, args);
    }
}
