package io.github.spring.middleware.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.spring.middleware.error.DefaultErrorDescriptor;
import io.github.spring.middleware.error.ErrorDescriptor;
import io.github.spring.middleware.error.ErrorMessage;
import io.github.spring.middleware.error.ErrorMessageFactory;
import io.github.spring.middleware.error.SecurityErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public class MiddlewareAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final ErrorMessageFactory errorMessageFactory;

    public MiddlewareAuthenticationEntryPoint(
            ObjectMapper objectMapper,
            ErrorMessageFactory errorMessageFactory
    ) {
        this.objectMapper = objectMapper;
        this.errorMessageFactory = errorMessageFactory;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        ErrorDescriptor descriptor = mapAuthenticationException(authException);
        ErrorMessage errorMessage = errorMessageFactory.from(descriptor);

        response.setStatus(errorMessage.getStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getOutputStream(), errorMessage);
    }

    private ErrorDescriptor mapAuthenticationException(AuthenticationException ex) {
        if (ex instanceof BadCredentialsException) {
            return new DefaultErrorDescriptor(SecurityErrorCodes.INVALID_CREDENTIALS);
        }

        if (ex instanceof InsufficientAuthenticationException) {
            return new DefaultErrorDescriptor(SecurityErrorCodes.MISSING_CREDENTIALS);
        }

        return new DefaultErrorDescriptor(SecurityErrorCodes.AUTHENTICATION_FAILED);
    }
}