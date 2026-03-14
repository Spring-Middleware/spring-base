package io.github.spring.middleware.security;

import io.github.spring.middleware.security.jwt.JwtHmacAlgorithm;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties("middleware.security")
public class SecurityConfigProperties {

    private SecurityType type;
    private List<String> publicPaths = new ArrayList<>();
    private List<ProtectedPathRule> protectedPaths = new ArrayList<>();
    private BasicAuth basicAuth = new BasicAuth();
    private Jwt jwt = new Jwt();
    private Oauth2 oauth2 = new Oauth2();
    private ApiKey apiKey = new ApiKey();

    @Data
    public static class BasicAuth {

        private List<Credentials> credentials = new ArrayList<>();
        private UserApi userApi = new UserApi();

        @Data
        public static class UserApi {
            private boolean enabled = false;
        }

        @Data
        public static class Credentials {
            private String username;
            private String password;
            private List<String> roles = new ArrayList<>();
        }
    }

    @Data
    public static class Jwt {
        private String secret;
        JwtHmacAlgorithm algorithm = JwtHmacAlgorithm.HS256;
        private String authorityClaimName = "roles";
    }

    @Data
    public static class Oauth2 {
        private String issuerUri;
        private String jwkSetUri;
        private String authoritiesClaimPath;
    }

    @Data
    public static class ProtectedPathRule {
        private SecurityPathType type = SecurityPathType.NONE;
        private String path;
        private List<HttpMethod> methods = new ArrayList<>();
        private List<String> allowedRoles = new ArrayList<>();
        private List<QueryParamRule> queryParams = new ArrayList<>();

        @Data
        public static class QueryParamRule {
            private String name;
            private List<String> values = new ArrayList<>();
            private boolean required = true;
        }
    }

    @Data
    public static class ApiKey {
        private String headerName = "X-Api-Key";
        private List<ApiKeyDetails> credentials = new ArrayList<>();

        @Data
        public static class ApiKeyDetails {
            private String key;
            private boolean enabled;
            private List<String> roles = new ArrayList<>();
        }
    }

}
