package io.github.spring.middleware.ai.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan(basePackages = "io.github.spring.middleware")
@SpringBootApplication(scanBasePackages = "io.github.spring.middleware")
public class DocumentationChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentationChatApplication.class, args);
    }
}
