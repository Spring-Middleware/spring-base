package com.core.filter;

import com.core.config.PropertyNames;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.log4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class RequestTrackerFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
        String requestId = Optional.ofNullable(httpServletRequest.getHeader(PropertyNames.REQUEST_ID))
                .orElseGet(() -> UUID.randomUUID().toString().replaceAll("-", "").toUpperCase());
        MDC.put(PropertyNames.REQUEST_ID, requestId);
        httpServletResponse.addHeader(PropertyNames.REQUEST_ID, requestId);
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }
}
