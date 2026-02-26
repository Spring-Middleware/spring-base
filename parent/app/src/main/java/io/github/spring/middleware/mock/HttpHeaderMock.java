package io.github.spring.middleware.mock;

import org.springframework.web.client.RestTemplate;

import java.util.Collections;

public class HttpHeaderMock {

    public static void mockRequestID(RestTemplate restTemplate, String uuid) {

        restTemplate.setInterceptors(
                Collections.singletonList((request, body, execution) -> {
                    request.getHeaders()
                            .add("REQUEST-ID", uuid);
                    return execution.execute(request, body);
                }));
    }

}
