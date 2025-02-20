package com.core.filter;

import com.core.config.HttpHeaderNames;
import com.core.config.PropertyNames;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.core.config.PropertyNames.REQUEST_ID;

@Component
@Order(value = Ordered.HIGHEST_PRECEDENCE + 1)
public class InitContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
            FilterChain filterChain) throws ServletException, IOException {

        initContext(getContextProperties(httpServletRequest,null));
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    public static void initContext() {

        initContext(null);
    }

    public static void initContext(Map<String, Object> propertyNamesObjectMap) {

        Map<String, Object> myContext = new HashMap<>();
        propertyNamesObjectMap = Optional.ofNullable(propertyNamesObjectMap).orElse(new HashMap<>());
        myContext.put(REQUEST_ID,
                Optional.ofNullable(propertyNamesObjectMap.get(REQUEST_ID))
                        .orElse(StringUtils.EMPTY));
        myContext.put(PropertyNames.REQUEST_LOG_ENABLED,
                Optional.ofNullable(propertyNamesObjectMap.get(PropertyNames.REQUEST_LOG_ENABLED))
                        .orElse(Boolean.FALSE));
        myContext.put(PropertyNames.CONTENT_LANGUAGE,
                Optional.ofNullable(propertyNamesObjectMap.get(PropertyNames.CONTENT_LANGUAGE))
                        .orElse("en-GB"));
        Context.set(myContext);
    }

    public static Map<String, Object> getContextProperties(HttpServletRequest httpServletRequest, String locale) {

        HashMap<String, Object> contextProperties = new HashMap<>();
        Optional.ofNullable(MDC.get(REQUEST_ID))
                .ifPresent(reqId -> contextProperties.put(REQUEST_ID, reqId));

        contextProperties.put(PropertyNames.REQUEST_LOG_ENABLED, isRequestLogEnabled(httpServletRequest));
        contextProperties.put(PropertyNames.RESPONSE_TIME_LOG, isResponseTimeLogEnabled(httpServletRequest));
        contextProperties.put(PropertyNames.CONTENT_LANGUAGE,
                Optional.ofNullable(locale).orElse(getContentLanguage(httpServletRequest)));
        return contextProperties;
    }

    private static boolean isRequestLogEnabled(HttpServletRequest request) {

        return Optional.ofNullable((String) request.getHeader(HttpHeaderNames.LogRequestEnabled))
                .map(s -> s.equalsIgnoreCase(Boolean.TRUE.toString())).orElse(Boolean.FALSE);
    }

    private static boolean isResponseTimeLogEnabled(HttpServletRequest request) {

        return Optional.ofNullable((String) request.getHeader(HttpHeaderNames.LogResponseTime))
                .map(s -> s.equalsIgnoreCase(Boolean.TRUE.toString())).orElse(Boolean.FALSE);
    }

    private static String getContentLanguage(HttpServletRequest request) {

        return Optional.ofNullable(request.getHeader(HttpHeaderNames.ContentLanguage))
                .orElse(request.getLocale().toLanguageTag());
    }

}
