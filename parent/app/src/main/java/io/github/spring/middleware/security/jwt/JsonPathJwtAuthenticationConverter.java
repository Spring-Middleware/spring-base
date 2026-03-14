package io.github.spring.middleware.security.jwt;

import com.jayway.jsonpath.JsonPath;
import io.github.spring.middleware.security.SecurityConfigProperties;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class JsonPathJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final String authoritiesClaimPath;
    private final static String AUTHORITY_PREFIX = "ROLE_";

    public JsonPathJwtAuthenticationConverter(SecurityConfigProperties properties) {
        SecurityConfigProperties.Oauth2 oauth2 = Objects.requireNonNull(
                properties.getOauth2(),
                "OAUTH2 configuration is missing"
        );

        this.authoritiesClaimPath = oauth2.getAuthoritiesClaimPath();
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        Object result;
        try {
            result = JsonPath.read(jwt.getClaims(), authoritiesClaimPath);
        } catch (Exception e) {
            return authorities;
        }

        if (result instanceof Collection<?> values) {
            for (Object value : values) {
                if (value != null) {
                    authorities.add(new SimpleGrantedAuthority(AUTHORITY_PREFIX + value));
                }
            }
        } else if (result != null) {
            authorities.add(new SimpleGrantedAuthority(AUTHORITY_PREFIX + result));
        }

        return authorities;
    }
}