package io.github.spring.middleware.filter;

import io.github.spring.middleware.config.HttpHeaderNames;
import io.github.spring.middleware.config.PropertyNames;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.github.spring.middleware.config.PropertyNames.CONTENT_LANGUAGE;
import static io.github.spring.middleware.config.PropertyNames.LOGGING_KEY;
import static io.github.spring.middleware.config.PropertyNames.REQUEST_HEADERS;
import static io.github.spring.middleware.config.PropertyNames.REQUEST_ID;
import static io.github.spring.middleware.config.PropertyNames.RESPONSE_TIME_LOG;

@Component
@Order(value = Ordered.HIGHEST_PRECEDENCE + 1)
public class InitContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            initContext(getContextProperties(httpServletRequest, null));
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } finally {
            Context.clear();
        }
    }

    public static void initContext() {
        initContext(null);
    }

    public static void initContext(Map<String, Object> propertyNamesObjectMap) {
        Map<String, Object> myContext = new HashMap<>(
                Optional.ofNullable(propertyNamesObjectMap).orElseGet(HashMap::new)
        );

        myContext.putIfAbsent(REQUEST_ID, StringUtils.EMPTY);
        myContext.putIfAbsent(PropertyNames.LOGGING_KEY, Boolean.FALSE);
        myContext.putIfAbsent(PropertyNames.RESPONSE_TIME_LOG, Boolean.FALSE);
        myContext.putIfAbsent(PropertyNames.CONTENT_LANGUAGE, "en-GB");
        myContext.putIfAbsent(PropertyNames.HEADERS_TO_COPY,
                List.of(LOGGING_KEY, RESPONSE_TIME_LOG, REQUEST_ID, CONTENT_LANGUAGE));
        myContext.putIfAbsent(REQUEST_HEADERS, new HashMap<String, String>());

        Context.set(myContext);
    }

    public static Map<String, Object> getContextProperties(HttpServletRequest httpServletRequest, String locale) {
        Map<String, Object> contextProperties = new HashMap<>();

        Optional.ofNullable(MDC.get(REQUEST_ID))
                .ifPresent(reqId -> contextProperties.put(REQUEST_ID, reqId));

        contextProperties.put(PropertyNames.LOGGING_KEY, isRequestLogEnabled(httpServletRequest));
        contextProperties.put(PropertyNames.RESPONSE_TIME_LOG, isResponseTimeLogEnabled(httpServletRequest));
        contextProperties.put(PropertyNames.CONTENT_LANGUAGE,
                Optional.ofNullable(locale).orElse(getContentLanguage(httpServletRequest)));
        contextProperties.put(PropertyNames.HEADERS_TO_COPY,
                List.of(LOGGING_KEY, RESPONSE_TIME_LOG, REQUEST_ID, CONTENT_LANGUAGE));

        final Map<String, String> currentHeaders = new HashMap<>();
        httpServletRequest.getHeaderNames().asIterator().forEachRemaining(headerName ->
                currentHeaders.put(headerName.toLowerCase(), httpServletRequest.getHeader(headerName))
        );

        contextProperties.put(REQUEST_HEADERS, currentHeaders);

        return contextProperties;
    }

    private static boolean isRequestLogEnabled(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(HttpHeaderNames.LogRequestEnabled))
                .map(s -> s.equalsIgnoreCase(Boolean.TRUE.toString()))
                .orElse(Boolean.FALSE);
    }

    private static boolean isResponseTimeLogEnabled(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(HttpHeaderNames.LogResponseTime))
                .map(s -> s.equalsIgnoreCase(Boolean.TRUE.toString()))
                .orElse(Boolean.FALSE);
    }

    private static String getContentLanguage(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(HttpHeaderNames.ContentLanguage))
                .orElse(request.getLocale().toLanguageTag());
    }
}