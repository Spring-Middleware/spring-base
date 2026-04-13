package io.github.spring.middleware.client.proxy.security.oauth2;

import io.github.spring.middleware.client.proxy.ProxyClientException;
import io.github.spring.middleware.error.ErrorCodes;
import io.github.spring.middleware.error.SecurityErrorCodes;

import java.util.Map;

public class OAuth2TokenAcquisitionException extends ProxyClientException {

    public OAuth2TokenAcquisitionException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public ErrorCodes getCode() {
        return SecurityErrorCodes.OAUTH2_TOKEN_ACQUISITION_ERROR;
    }

    public Map<String, Object> getExtensions() {
        return Map.of("auth.phase", "token_acquisition");
    }
}
