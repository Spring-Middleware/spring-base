package io.github.spring.middleware.filter;

import io.github.spring.middleware.config.PropertyNames;
import io.github.spring.middleware.log.LogRequestResponse;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StopWatch;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
@Order(value = Ordered.HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final LogRequestResponse requestResponseLog;
    private final MultipartResolver multipartResolver;
    private final MiddlewareLogProperties middlewareLogProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {


        StopWatch stopWatch = null;
        if (!multipartResolver.isMultipart(request)) {
            request = new CustomHttpServletRequestWrapper(request);
            if (isNotActuator(request) && requestResponseLog.isInfoEnabled()) {
                try {
                    if (middlewareLogProperties.getRequest().isEnabled()) {
                        requestResponseLog.info(getMessageLog(request));
                    }
                } catch (Exception ex) {
                    log.error("Error retrieving request log ", ex);
                }
            }
            if (isResponseTimeLog() && middlewareLogProperties.getResponse().isEnabled()) {
                stopWatch = new StopWatch();
                stopWatch.start();
            }
        }
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, responseWrapper);
        Optional.ofNullable(stopWatch).ifPresent(StopWatch::stop);
        if (isNotActuator(request) && requestResponseLog.isInfoEnabled() && middlewareLogProperties.getResponse().isEnabled()) {
            requestResponseLog.info(STR."\{responseWrapper.getStatus()} \{IOUtils.toString(responseWrapper.getContentInputStream(), Charset.defaultCharset())}\{getResponseTime(stopWatch)}");
        }
        responseWrapper.copyBodyToResponse();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());

        return middlewareLogProperties.getExclude().getUrlPatterns().stream()
                .filter(StringUtils::isNotBlank)
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean isNotActuator(HttpServletRequest request) {

        return !request.getRequestURI().contains("actuator");
    }

    private boolean isResponseTimeLog() {

        return middlewareLogProperties.getResponseTime().isEnabled() ||
                Optional.ofNullable((Boolean) Context.get(PropertyNames.RESPONSE_TIME_LOG)).orElse(Boolean.FALSE);
    }

    private String getResponseTime(StopWatch stopWatch) {

        return Optional.ofNullable(stopWatch).map(s -> {
            return STR." \{stopWatch.getLastTaskInfo().getTimeMillis()}";
        }).orElse(StringUtils.EMPTY);
    }

    private String getMessageLog(HttpServletRequest request) throws Exception {

        StringBuilder logMessage = new StringBuilder();
        logMessage.append(request.getMethod()).append(" ");
        logMessage.append(request.getRequestURI());
        if (request.getQueryString() != null) {
            logMessage.append("?").append(request.getQueryString());
        }
        request = new CustomHttpServletRequestWrapper(request);
        logMessage.append(STR." \{Optional.ofNullable(request.getInputStream())
                .map(is -> {
                    try {
                        return IOUtils.toString(is, Charset.defaultCharset());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElse(
                        StringUtils.EMPTY)}");
        return logMessage.toString();
    }

}
