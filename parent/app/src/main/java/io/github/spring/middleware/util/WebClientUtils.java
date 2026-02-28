package io.github.spring.middleware.util;

import io.netty.channel.ChannelOption;
import org.apache.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class WebClientUtils {

    private static final Logger logger = Logger.getLogger(WebClientUtils.class);

    private static final List<String> VALID_HEADERS = Arrays.asList(
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.CONTENT_LANGUAGE
    );

    /**
     * Crea un WebClient nuevo con timeout.
     */
    public static WebClient createWebClient(int timeoutMillis) {
        // Crear TCPClient con socket timeout
        TcpClient tcpClient = TcpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMillis)
                .doOnConnected(conn ->
                        conn.addHandlerLast(new io.netty.handler.timeout.ReadTimeoutHandler(timeoutMillis / 1000))
                                .addHandlerLast(new io.netty.handler.timeout.WriteTimeoutHandler(timeoutMillis / 1000))
                );

        // Luego crear HttpClient a partir del TcpClient
        HttpClient httpClient = HttpClient.from(tcpClient)
                .responseTimeout(Duration.ofMillis(timeoutMillis));

        WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                        .build())
                .build();

        logger.info("WebClient created with timeout: " + timeoutMillis + " ms");
        return webClient;
    }

    /**
     * Copia solo headers válidos al WebClient.RequestHeadersSpec
     */
    public static WebClient.RequestHeadersSpec<?> copyHeaders(WebClient.RequestBodySpec requestSpec,
                                                              Map<String, List<String>> headers) {
        if (headers != null) {
            headers.forEach((key, values) -> {
                if (VALID_HEADERS.contains(key) && values != null) {
                    String headerValue = String.join(",", values);
                    requestSpec.header(key, headerValue);
                }
            });
        }
        return requestSpec;
    }

    /**
     * Reconfigura WebClient con nuevo timeout.
     * Nota: WebClient es inmutable, siempre devuelve uno nuevo.
     */
    public static WebClient reconfigureWebClient(WebClient oldClient, int newTimeout) {
        logger.info("Reconfiguring WebClient to timeout: " + newTimeout + " ms");
        return createWebClient(newTimeout);
    }
}
