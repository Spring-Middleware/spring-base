package io.github.spring.middleware.kafka.api.interf;

import io.github.spring.middleware.kafka.api.data.PublishResult;

import java.util.concurrent.CompletableFuture;

public interface KafkaPublisher<T, K> {

    CompletableFuture<PublishResult<T, K>> publishWithKey(T event, K key);

    CompletableFuture<PublishResult<T, K>> publish(T event);
}
