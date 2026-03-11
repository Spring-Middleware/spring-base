package io.github.spring.middleware.client.proxy.security.resolver;

import io.github.spring.middleware.client.proxy.security.config.SecurityClientCredentialsConfiguration;
import io.github.spring.middleware.client.proxy.security.oauth2.OAuth2ClientCredentialsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Qualifier("CLIENT_CREDENTIALS")
@RequiredArgsConstructor
public class SecurityClientCredentialsHeaderResolver implements SecurityHeaderResolver<SecurityClientCredentialsConfiguration> {

    private final OAuth2ClientCredentialsClient oAuth2ClientCredentialsClient;

    @Override
    public WebClient.RequestHeadersSpec<?> resolveSecurityHeader(
            SecurityClientCredentialsConfiguration securityConfiguration,
            Map<String, String> currentHeaders,
            WebClient.RequestHeadersSpec<?> specHeaders) {

        String accessToken = oAuth2ClientCredentialsClient.getAccessToken(
                securityConfiguration.getTokenUri(),
                securityConfiguration.getClientId(),
                securityConfiguration.getClientSecret(),
                securityConfiguration.getScopes()
        );

        return specHeaders.header("Authorization", STR."Bearer \{accessToken}");
    }


}
