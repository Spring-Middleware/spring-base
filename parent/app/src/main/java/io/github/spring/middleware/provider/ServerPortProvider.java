package io.github.spring.middleware.provider;

import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ServerPortProvider implements ApplicationListener<WebServerInitializedEvent> {

    private volatile int port = -1;

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        // solo root context
        if (event.getApplicationContext().getParent() != null) return;
        this.port = event.getWebServer().getPort();
    }

    public int getPort() {
        if (port <= 0) throw new IllegalStateException("Server port not initialized yet");
        return port;
    }
}