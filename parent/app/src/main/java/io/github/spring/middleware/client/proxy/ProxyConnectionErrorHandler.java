package io.github.spring.middleware.client.proxy;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.spring.middleware.config.PropertyNames;
import io.github.spring.middleware.error.ErrorMessage;
import io.github.spring.middleware.error.ErrorSpanStep;
import io.github.spring.middleware.error.FrameworkErrorCodes;
import io.github.spring.middleware.register.resource.ResourceRegisterConfiguration;
import io.github.spring.middleware.utils.ErrorSpanUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProxyConnectionErrorHandler {

    private final ResourceRegisterConfiguration resourceRegisterConfiguration;
    private final ObjectMapper objectMapper;

    public void processError(String content,
                             int statusCode,
                             String url,
                             String method,
                             Map<String, Object> context) throws RemoteServerException {

        String requestId = (String) Optional.ofNullable(context)
                .map(c -> c.get(PropertyNames.REQUEST_ID))
                .orElse(StringUtils.EMPTY);

        String spanId = (String) Optional.ofNullable(context)
                .map(c -> c.get(PropertyNames.SPAN_ID))
                .orElse(StringUtils.EMPTY);

        try {
            ErrorMessage remoteBody = objectMapper.readValue(content, ErrorMessage.class);

            ErrorSpanUtils.ensureMutableExtensions(remoteBody);
            remoteBody.getExtensions().putIfAbsent("remote.url", url);
            remoteBody.getExtensions().putIfAbsent("remote.method", method);
            remoteBody.getExtensions().putIfAbsent("remote.httpStatus", statusCode);
            remoteBody.getExtensions().putIfAbsent("remote.service", resourceRegisterConfiguration.getClusterName());

            if (StringUtils.isNotBlank(requestId)) {
                remoteBody.getExtensions().putIfAbsent("requestId", requestId);
                remoteBody.getExtensions().putIfAbsent("remote.requestId", requestId);
            }

            if (StringUtils.isNotBlank(spanId)) {
                remoteBody.getExtensions().putIfAbsent("spanId", spanId);
                remoteBody.getExtensions().putIfAbsent("remote.spanId", spanId);
            }

            ErrorSpanUtils.appendTrace(remoteBody, new ErrorSpanStep(
                    resourceRegisterConfiguration.getClusterName(),
                    method,
                    url,
                    statusCode,
                    requestId
            ));

            throw new RemoteServerException(remoteBody, statusCode, requestId);

        } catch (JacksonException parseEx) {
            ErrorMessage fallback = new ErrorMessage();
            fallback.setStatusCode(statusCode > 0 ? statusCode : 500);

            HttpStatus httpStatus = HttpStatus.resolve(statusCode);
            fallback.setStatusMessage(httpStatus != null ? httpStatus.getReasonPhrase() : "Remote Error");

            fallback.setCode(FrameworkErrorCodes.REMOTE_SERVICE_ERROR);
            fallback.setMessage("Error calling remote service");
            fallback.setExtensions(new HashMap<>());

            fallback.getExtensions().put("remote.url", url);
            fallback.getExtensions().put("remote.method", method);
            fallback.getExtensions().put("remote.httpStatus", statusCode);
            fallback.getExtensions().put("remote.service", resourceRegisterConfiguration.getClusterName());
            fallback.getExtensions().put("remote.body", safeTruncate(content, 2000));
            fallback.getExtensions().put("remote.parseError", safeTruncate(parseEx.getMessage(), 1000));

            if (StringUtils.isNotBlank(requestId)) {
                fallback.getExtensions().put("requestId", requestId);
                fallback.getExtensions().put("remote.requestId", requestId);
            }

            if (StringUtils.isNotBlank(spanId)) {
                fallback.getExtensions().put("spanId", spanId);
                fallback.getExtensions().put("remote.spanId", spanId);
            }

            ErrorSpanUtils.appendTrace(fallback, new ErrorSpanStep(
                    resourceRegisterConfiguration.getClusterName(),
                    method,
                    url,
                    statusCode,
                    requestId
            ));

            throw new RemoteServerException(
                    fallback,
                    statusCode > 0 ? statusCode : 500,
                    requestId
            );
        }
    }

    private String safeTruncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}