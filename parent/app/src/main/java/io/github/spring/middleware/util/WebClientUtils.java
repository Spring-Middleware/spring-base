package io.github.spring.middleware.util;

import io.netty.channel.ChannelOption;
import org.apache.log4j.Logger;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.tcp.TcpClient;

import java.time.Duration;

public class WebClientUtils {

    private static final Logger logger = Logger.getLogger(WebClientUtils.class);

    /**
     * Crea un WebClient nuevo con timeout.
     */
    public static WebClient createWebClient(int timeoutMillis, int maxConnections) {

        int safeTimeout = Math.max(1, timeoutMillis);
        int safeMaxConnections = Math.max(1, maxConnections);

        // Connection pool (per WebClient instance)
        ConnectionProvider provider = ConnectionProvider.builder(STR."middleware-pool-\{safeMaxConnections}")
                .maxConnections(safeMaxConnections)
                // Avoid unbounded waiting for a connection from the pool
                .pendingAcquireTimeout(Duration.ofMillis(safeTimeout))
                .pendingAcquireMaxCount(1000)
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofMinutes(5))
                .build();

        // TCP settings + timeouts
        TcpClient tcpClient = TcpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, safeTimeout)
                .doOnConnected(conn ->
                        conn.addHandlerLast(new io.netty.handler.timeout.ReadTimeoutHandler(Math.max(1, safeTimeout / 1000)))
                                .addHandlerLast(new io.netty.handler.timeout.WriteTimeoutHandler(Math.max(1, safeTimeout / 1000)))
                );

        // HTTP client on top of TCP client
        HttpClient httpClient = HttpClient.from(tcpClient)
                .responseTimeout(Duration.ofMillis(safeTimeout));

        WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                        .build())
                .build();

        logger.info(STR."WebClient created with timeout: \{safeTimeout} ms, maxConnections: \{safeMaxConnections}");
        return webClient;
    }


    /**
     * Reconfigura WebClient con nuevo timeout.
     * Nota: WebClient es inmutable, siempre devuelve uno nuevo.
     */
    public static WebClient reconfigureWebClient(WebClient oldClient, int newTimeout, int maxConnections) {
        logger.info(STR."Reconfiguring WebClient to timeout: \{newTimeout} ms");
        return createWebClient(newTimeout, maxConnections);
    }
}
