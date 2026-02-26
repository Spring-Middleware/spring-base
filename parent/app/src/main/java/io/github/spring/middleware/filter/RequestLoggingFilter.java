package io.github.spring.middleware.filter;

import io.github.spring.middleware.config.PropertyNames;
import io.github.spring.middleware.log.LogRequestResponse;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
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
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Autowired
    private LogRequestResponse requestResponseLog;
    @Autowired
    private MultipartResolver multipartResolver;

    @Value("${logging.logResponseTime:false}")
    private boolean logResponseTime;

    @Value("#{'${logging.exclude.urlPattern:}'.split(',')}")
    private Collection<String> excludeUrlPatterns;
    private Collection<Pattern> excludePatterns;

    @PostConstruct
    public void initPatterns() {
        excludePatterns = excludeUrlPatterns.stream().filter(s -> !s.isEmpty()).map(Pattern::compile)
                .collect(Collectors.toSet());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        StopWatch stopWatch = null;
        if (!multipartResolver.isMultipart(request)) {
            request = new CustomHttpServletRequestWrapper(request);
            if (isNotActuator(request) && requestResponseLog.isInfoEnabled()) {
                try {
                    requestResponseLog.info(getMessageLog(request));
                } catch (Exception ex) {
                    log.error("Error retrieving request log ", ex);
                }
            }
            if (isReponseTimeLog()) {
                stopWatch = new StopWatch();
                stopWatch.start();
            }
        }
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, responseWrapper);
        Optional.ofNullable(stopWatch).ifPresent(StopWatch::stop);
        if (isNotActuator(request) && requestResponseLog.isInfoEnabled()) {
            requestResponseLog.info(responseWrapper.getStatus() + " " +
                    IOUtils.toString(responseWrapper.getContentInputStream(), Charset.defaultCharset()) +
                    getResponseTime(stopWatch));
        }
        responseWrapper.copyBodyToResponse();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {

        return excludePatterns.stream().anyMatch(p -> p.matcher(request.getRequestURI()).matches());
    }

    private boolean isNotActuator(HttpServletRequest request) {

        return !request.getRequestURI().contains("actuator");
    }

    private boolean isReponseTimeLog() {

        return logResponseTime ||
                Optional.ofNullable((Boolean) Context.get(PropertyNames.RESPONSE_TIME_LOG)).orElse(Boolean.FALSE);
    }

    private String getResponseTime(StopWatch stopWatch) {

        return Optional.ofNullable(stopWatch).map(s -> {
            return " " + stopWatch.getLastTaskInfo().getTimeMillis();
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
        logMessage.append(" " + Optional.ofNullable(request.getInputStream())
                .map(is -> {
                    try {
                        return IOUtils.toString(is, Charset.defaultCharset());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElse(
                        StringUtils.EMPTY));
        return logMessage.toString();
    }

}
