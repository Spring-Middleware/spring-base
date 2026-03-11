package io.github.spring.middleware.client.proxy.security.oauth2;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class CachedAccessToken {
    private String accessToken;
    private Instant expiresAt;

    public boolean isExpired() {
        return expiresAt == null || Instant.now().isAfter(expiresAt.minusSeconds(30));
    }
}
