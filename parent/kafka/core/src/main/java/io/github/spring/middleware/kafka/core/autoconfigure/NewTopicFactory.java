package io.github.spring.middleware.kafka.core.autoconfigure;

import io.github.spring.middleware.kafka.core.properties.KafkaProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class NewTopicFactory {

    public List<NewTopic> buildTopics(KafkaProperties kafkaProperties) {
        return kafkaProperties.getTopics().entrySet().stream()
                .flatMap(entry -> buildTopicAndDeadLetterTopics(entry, kafkaProperties.getErrorHandling().getDeadLetter()).stream())
                .toList();
    }

    private List<NewTopic> buildTopicAndDeadLetterTopics(
            Map.Entry<String, KafkaProperties.TopicConfiguration> entry,
            KafkaProperties.ErrorHandling.DeadLetter deadLetter
    ) {
        List<NewTopic> topics = new ArrayList<>();
        topics.add(buildTopic(entry));

        if (deadLetter.isEnabled()) {
            topics.add(buildDeadLetterTopic(entry, deadLetter));
        }

        return topics;
    }

    private NewTopic buildTopic(Map.Entry<String, KafkaProperties.TopicConfiguration> entry) {
        return TopicBuilder.name(entry.getKey())
                .partitions(entry.getValue().getPartitions())
                .replicas(entry.getValue().getReplicationFactor())
                .build();
    }

    private NewTopic buildDeadLetterTopic(Map.Entry<String, KafkaProperties.TopicConfiguration> entry, KafkaProperties.ErrorHandling.DeadLetter deadLetter) {
        if (!deadLetter.isEnabled()) {
            return null;
        }
        return TopicBuilder.name(entry.getKey() + deadLetter.getSuffix())
                .partitions(entry.getValue().getPartitions())
                .replicas(entry.getValue().getReplicationFactor())
                .build();
    }

}
