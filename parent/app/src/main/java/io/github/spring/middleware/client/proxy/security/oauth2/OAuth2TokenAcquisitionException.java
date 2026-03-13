package io.github.spring.middleware.client.proxy.security.oauth2;

import io.github.spring.middleware.client.proxy.ProxyClientException;

import java.util.Map;

public class OAuth2TokenAcquisitionException extends ProxyClientException {

    private static final String ERROR_CODE = "OAUTH2_TOKEN_ACQUISITION_ERROR";

    public OAuth2TokenAcquisitionException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getCode() {
        return ERROR_CODE;
    }

    public Map<String, Object> getExtensions() {
        return Map.of("auth.phase", "token_acquisition");
    }
}
