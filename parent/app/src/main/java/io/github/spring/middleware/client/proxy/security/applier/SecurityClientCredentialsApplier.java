package io.github.spring.middleware.client.proxy.security.applier;

import io.github.spring.middleware.client.proxy.security.config.SecurityClientCredentialsConfiguration;
import io.github.spring.middleware.client.proxy.security.method.ClientCredentialsMethodSecurityConfiguration;
import io.github.spring.middleware.client.proxy.security.method.MethodSecurityConfiguration;
import io.github.spring.middleware.client.proxy.security.oauth2.OAuth2ClientCredentialsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Qualifier("CLIENT_CREDENTIALS")
@RequiredArgsConstructor
public class SecurityClientCredentialsApplier implements SecurityApplier<SecurityClientCredentialsConfiguration, ClientCredentialsMethodSecurityConfiguration> {

    private final OAuth2ClientCredentialsClient oAuth2ClientCredentialsClient;

    @Override
    public WebClient.RequestHeadersSpec<?> applySecurity(
            SecurityClientCredentialsConfiguration securityConfiguration,
            ClientCredentialsMethodSecurityConfiguration methodSecurityConfiguration,
            Map<String, String> currentHeaders,
            WebClient.RequestHeadersSpec<?> specHeaders) {

        String accessToken = oAuth2ClientCredentialsClient.getAccessToken(
                securityConfiguration.getTokenUri(),
                securityConfiguration.getClientId(),
                securityConfiguration.getClientSecret(),
                methodSecurityConfiguration.requiredScopes()
        );

        return specHeaders.header("Authorization", STR."Bearer \{accessToken}");
    }

    @Override
    public boolean supports(MethodSecurityConfiguration methodSecurityConfiguration) {
        return methodSecurityConfiguration instanceof ClientCredentialsMethodSecurityConfiguration;
    }


}
