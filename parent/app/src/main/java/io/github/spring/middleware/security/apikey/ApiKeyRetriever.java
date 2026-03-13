package io.github.spring.middleware.security.apikey;

import io.github.spring.middleware.security.SecurityConfigProperties;

import java.util.Optional;

public interface ApiKeyRetriever {

    Optional<SecurityConfigProperties.ApiKey.ApiKeyDetails> findByKey(String apiKey);
}
