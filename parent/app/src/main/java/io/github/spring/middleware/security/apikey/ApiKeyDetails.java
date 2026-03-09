package io.github.spring.middleware.security.apikey;

import java.util.List;

public record ApiKeyDetails(String key, boolean enabled, List<String> roles) {
}
