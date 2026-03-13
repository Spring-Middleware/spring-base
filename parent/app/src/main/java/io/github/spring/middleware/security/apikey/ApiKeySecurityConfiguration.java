package io.github.spring.middleware.security.apikey;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.spring.middleware.error.ErrorMessageFactory;
import io.github.spring.middleware.security.MiddlewareAuthenticationEntryPoint;
import io.github.spring.middleware.security.ProtectedPathRuleResolver;
import io.github.spring.middleware.security.SecurityConfigProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Objects;

@Configuration
@ConditionalOnProperty(prefix = "middleware.security", name = "type", havingValue = "API_KEY")
public class ApiKeySecurityConfiguration {

    @Bean
    @ConditionalOnMissingBean(ApiKeyRetriever.class)
    public ApiKeyRetriever propertiesApiKeyRetriever(SecurityConfigProperties properties) {
        return apiKey -> properties.getApiKey().getCredentials().stream()
                .filter(SecurityConfigProperties.ApiKey.ApiKeyDetails::isEnabled)
                .filter(c -> Objects.equals(c.getKey(), apiKey))
                .findFirst()
                .map(c -> {
                    var newApiKeyDetails = new SecurityConfigProperties.ApiKey.ApiKeyDetails();
                    newApiKeyDetails.setKey(c.getKey());
                    newApiKeyDetails.setRoles(c.getRoles());
                    newApiKeyDetails.setEnabled(c.isEnabled());
                    return newApiKeyDetails;
                });
    }

    @Bean
    public OncePerRequestFilter apiKeyAuthenticationFilter(
            SecurityConfigProperties properties,
            ApiKeyRetriever apiKeyRetriever,
            ProtectedPathRuleResolver protectedPathRuleResolver,
            AuthenticationEntryPoint authenticationEntryPoint) {
        return new ApiKeyAuthenticationFilter(properties, apiKeyRetriever, protectedPathRuleResolver, authenticationEntryPoint);
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(final ObjectMapper objectMapper,
                                                             final ErrorMessageFactory errorMessageFactory) {
        return new MiddlewareAuthenticationEntryPoint(objectMapper, errorMessageFactory);
    }
}