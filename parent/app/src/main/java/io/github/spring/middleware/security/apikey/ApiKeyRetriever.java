package io.github.spring.middleware.security.apikey;

import java.util.Optional;

public interface ApiKeyRetriever {

    Optional<ApiKeyDetails> findByKey(String apiKey);
}
