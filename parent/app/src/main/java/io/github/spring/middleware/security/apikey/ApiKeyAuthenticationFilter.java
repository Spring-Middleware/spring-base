package io.github.spring.middleware.security.apikey;

import io.github.spring.middleware.security.ProtectedPathRuleResolver;
import io.github.spring.middleware.security.SecurityConfigProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final SecurityConfigProperties properties;
    private final ApiKeyRetriever apiKeyRetriever;
    private final ProtectedPathRuleResolver protectedPathRuleResolver;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    private final static String AUTHORITY_PREFIX = "ROLE_";
    private final static String PRINCIPAL = "api-key";

    public ApiKeyAuthenticationFilter(SecurityConfigProperties properties,
                                      ApiKeyRetriever apiKeyRetriever,
                                      ProtectedPathRuleResolver protectedPathRuleResolver,
                                      AuthenticationEntryPoint authenticationEntryPoint) {
        this.properties = properties;
        this.apiKeyRetriever = apiKeyRetriever;
        this.protectedPathRuleResolver = protectedPathRuleResolver;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String headerName = properties.getApiKey().getHeaderName();
        String apiKey = request.getHeader(headerName);
        Optional<SecurityConfigProperties.ProtectedPathRule> protectedPathRule =
                protectedPathRuleResolver.resolve(request);

        if (!StringUtils.hasText(apiKey)) {
            if (protectedPathRule.isPresent()) {
                authenticationEntryPoint.commence(request, response, new InsufficientAuthenticationException(
                        STR."Missing API key in header: \{headerName}"));
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        SecurityConfigProperties.ApiKey.ApiKeyDetails details = apiKeyRetriever.findByKey(apiKey)
                .filter(SecurityConfigProperties.ApiKey.ApiKeyDetails::isEnabled).orElse(null);

        if (details == null) {
            authenticationEntryPoint.commence(request, response, new BadCredentialsException(
                    STR."Invalid API key provided in header: \{headerName}"));
            return;
        }

        List<? extends GrantedAuthority> authorities = details.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(AUTHORITY_PREFIX + role))
                .toList();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                PRINCIPAL,
                apiKey,
                authorities
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(request, response);
    }
}