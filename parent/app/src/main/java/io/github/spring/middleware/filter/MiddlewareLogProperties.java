package io.github.spring.middleware.filter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "middleware.log")
public class MiddlewareLogProperties {

    private Request request = new Request();
    private Response response = new Response();
    private ResponseTime responseTime = new ResponseTime();
    private Exclude exclude = new Exclude();
    private String apiKey;

    @Data
    public static class Request {
        private boolean enabled = true;
    }

    @Data
    public static class Response {
        private boolean enabled = true;
    }

    @Data
    public static class ResponseTime {
        private boolean enabled = false;
    }

    @Data
    public static class Exclude {
        private List<String> urlPatterns = new ArrayList<>();
    }
}