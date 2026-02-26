package io.github.spring.middleware.component;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class DateTimeAndServer {

    public LocalDateTime getCurrentTime() {

        return LocalDateTime.now();
    }

    public String getServerName() throws Exception {

        return Optional.ofNullable(System.getenv("MY_POD_NAME"))
                .orElse(InetAddress.getLocalHost().getHostName());
    }

}
