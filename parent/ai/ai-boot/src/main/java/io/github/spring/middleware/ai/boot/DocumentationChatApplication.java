package io.github.spring.middleware.ai.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "io.github.spring.middleware")
public class DocumentationChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentationChatApplication.class, args);
    }


}
