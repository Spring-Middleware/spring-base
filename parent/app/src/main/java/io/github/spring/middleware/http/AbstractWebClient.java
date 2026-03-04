package io.github.spring.middleware.http;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;


import java.util.Optional;

@Slf4j
public abstract class AbstractWebClient {

    protected WebClient webClient;

    @PostConstruct
    public void createWebClient() {

        final int size = 16 * 1024 * 1024;
        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
                .build();

        String baseUrl = getBaseUrl();

        if (baseUrl != null) {

            WebClient.Builder builder = WebClient.builder().exchangeStrategies(strategies);
            Optional.ofNullable(getExchangeFilterFunction()).ifPresent(exchangeFilterFunction -> {
                builder.filter(exchangeFilterFunction);
            });

            webClient = builder.baseUrl(baseUrl).build();
        }else{
            log.warn("Base URL is null for WebClient in {}, WebClient will not be created", this.getClass().getSimpleName());
        }
    }

    protected WebClient client() {
        if (webClient == null) {
            throw new IllegalStateException(
                    STR."\{getClass().getSimpleName()} WebClient not initialized (baseUrl missing?)"
            );
        }
        return webClient;
    }

    protected abstract String getBaseUrl();

    protected ExchangeFilterFunction getExchangeFilterFunction() {
        return null;
    }

}
