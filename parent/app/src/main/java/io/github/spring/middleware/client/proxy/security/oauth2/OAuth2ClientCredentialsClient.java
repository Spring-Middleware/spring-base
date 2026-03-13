package io.github.spring.middleware.client.proxy.security.oauth2;

import io.github.spring.middleware.client.proxy.ProxyClientException;
import io.github.spring.middleware.util.WebClientUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OAuth2ClientCredentialsClient {

    private final WebClient webClient;
    private final Map<String, CachedAccessToken> tokenCache = new ConcurrentHashMap<>();

    public OAuth2ClientCredentialsClient() {
        this.webClient = WebClientUtils.createWebClient(3000, 5);
    }

    public String getAccessToken(String tokenUri,
                                 String clientId,
                                 String clientSecret,
                                 List<String> scopes) {

        validate(tokenUri, clientId, clientSecret);

        String cacheKey = buildCacheKey(tokenUri, clientId, scopes);
        CachedAccessToken cached = tokenCache.get(cacheKey);

        if (cached != null && !cached.isExpired()) {
            return cached.getAccessToken();
        }

        synchronized (cacheKey.intern()) {
            cached = tokenCache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                return cached.getAccessToken();
            }

            OAuth2TokenResponse response = requestToken(tokenUri, clientId, clientSecret, scopes);

            if (!StringUtils.hasText(response.getAccessToken())) {
                throw new ProxyClientException("OAuth2 token endpoint returned an empty access token.");
            }

            long expiresIn = response.getExpiresIn() != null ? response.getExpiresIn() : 300L;
            CachedAccessToken newToken = new CachedAccessToken(
                    response.getAccessToken(),
                    Instant.now().plusSeconds(expiresIn)
            );

            tokenCache.put(cacheKey, newToken);
            return newToken.getAccessToken();
        }
    }

    private OAuth2TokenResponse requestToken(String tokenUri,
                                             String clientId,
                                             String clientSecret,
                                             List<String> scopes) {

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");

        String scopeValue = String.join(" ", scopes);
        if (StringUtils.hasText(scopeValue)) {
            formData.add("scope", scopeValue);
        }

        try {
            return webClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                    .bodyValue(formData)
                    .retrieve()
                    .bodyToMono(OAuth2TokenResponse.class)
                    .blockOptional()
                    .orElseThrow(() -> new ProxyClientException("OAuth2 token endpoint returned no response."));
        } catch (WebClientResponseException ex) {
            throw new OAuth2TokenAcquisitionException(
                    STR."OAuth2 token request failed. status=\{ex.getStatusCode().value()}, tokenUri=\{tokenUri}, clientId=\{clientId}, response=\{ex.getResponseBodyAsString()}",
                    ex
            );
        }
    }

    private void validate(String tokenUri, String clientId, String clientSecret) {
        if (!StringUtils.hasText(tokenUri)) {
            throw new ProxyClientException("tokenUri is required for OAuth2 client credentials.");
        }
        if (!StringUtils.hasText(clientId)) {
            throw new ProxyClientException("clientId is required for OAuth2 client credentials.");
        }
        if (!StringUtils.hasText(clientSecret)) {
            throw new ProxyClientException("clientSecret is required for OAuth2 client credentials.");
        }
    }

    private String buildCacheKey(String tokenUri, String clientId, List<String> scopes) {

        String scopeKey = scopes == null
                ? ""
                : scopes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .sorted()
                .collect(Collectors.joining(" "));

        return STR."\{tokenUri}::\{clientId}::\{scopeKey}";
    }
}
