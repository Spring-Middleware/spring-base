package io.github.spring.middleware.kafka.core.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "middleware.kafka")
public class KafkaProperties {

    private String bootstrapServers = "localhost:9092";
    private boolean createMissingTopics = true;
    private Logging logging = new Logging();
    private Map<String, TopicConfiguration> topics = new HashMap<>();
    private Map<String, Publisher> publishers = new HashMap<>();
    private Map<String, Subscriber> subscribers = new HashMap<>();

    @Getter
    @Setter
    public static class TopicConfiguration {
        @NotBlank
        private Integer partitions = 1;
        @NotBlank
        private Short replicationFactor = 1;
    }

    @Getter
    @Setter
    public static class Publisher {

        @NotBlank
        private String topic;
    }

    @Getter
    @Setter
    public static class Subscriber {

        @NotBlank
        private String topic;
        @NotBlank
        private String groupId;
        private int concurrency = 1;
    }

    @Getter
    @Setter
    public static class Logging {
        private boolean enabled = false;
        private boolean logPayload = false;
        private boolean logHeaders = false;
    }

}
