package io.github.spring.middleware.security;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
public class QueryParamPathRequestMatcher implements RequestMatcher {

    private final HttpMethod method;
    private final String path;
    private final List<SecurityConfigProperties.ProtectedPathRule.QueryParamRule> queryParams;

    @Override
    public boolean matches(HttpServletRequest request) {
        AntPathRequestMatcher pathMatcher = method == null
                ? new AntPathRequestMatcher(path)
                : new AntPathRequestMatcher(path, method.name());

        if (!pathMatcher.matches(request)) {
            return false;
        }

        if (CollectionUtils.isEmpty(queryParams)) {
            return true;
        }

        for (SecurityConfigProperties.ProtectedPathRule.QueryParamRule rule : queryParams) {
            String requestValue = request.getParameter(rule.getName());

            if (rule.isRequired() && !StringUtils.hasText(requestValue)) {
                return false;
            }

            if (StringUtils.hasText(requestValue)
                    && !CollectionUtils.isEmpty(rule.getValues())
                    && !rule.getValues().contains(requestValue)) {
                return false;
            }
        }

        return true;
    }
}
