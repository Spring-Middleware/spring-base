package io.github.spring.middleware.security.basic;

import io.github.spring.middleware.annotation.MiddlewareContract;
import org.springframework.security.core.userdetails.UserDetailsService;

@MiddlewareContract(name = "user", enabled = "${middleware.security.basic.user-api.enabled:false}" )
public interface UserApi extends UserDetailsService {
}
