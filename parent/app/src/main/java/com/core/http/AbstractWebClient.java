package com.core.http;

import jakarta.annotation.PostConstruct;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;


import java.util.Optional;

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

            webClient = builder.baseUrl(getBaseUrl()).build();
        }
    }

    protected abstract String getBaseUrl();

    protected ExchangeFilterFunction getExchangeFilterFunction() {

        return null;
    }

}
