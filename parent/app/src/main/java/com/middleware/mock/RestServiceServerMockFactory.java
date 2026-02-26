package com.middleware.mock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestTemplate;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@Slf4j
public class RestServiceServerMockFactory {

    private static Map<UUID, MockRestServiceServer> mockServers = new HashMap<>();

    public static void init() {

        mockServers.clear();
    }

    public static UUID createRestServer(RestTemplate restTemplate, String... paths) throws Exception {

        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        UUID serverUUID = UUID.randomUUID();
        mockServers.put(serverUUID, mockServer);
        Stream.of(paths).forEach(path -> {
            try {
                Properties properties = new Properties();
                properties.load(mockServer.getClass().getClassLoader().getResourceAsStream(path));
                ResponseActions responseActions = mockServer
                        .expect(ExpectedCount.once(), requestTo(properties.getProperty("URL")));
                String content = properties.getProperty("CONTENT");
                String acceptContent = properties.getProperty("ACCEPT-CONTENT");
                if (properties.getProperty("CONTENT") != null) {
                    String fieldsToIgnore = properties.getProperty("CONTENT-IGNORE-FIELDS");
                    if (fieldsToIgnore == null) {
                        if (acceptContent == null || acceptContent.equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
                            responseActions.andExpect(content().json(content));
                        } else if (acceptContent.equalsIgnoreCase(MediaType.APPLICATION_XML_VALUE)) {
                            responseActions.andExpect(content().xml(content));
                        }

                    } else {
                        if (acceptContent == null || acceptContent.equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
                            responseActions
                                    .andExpect(new JsonIgnoreContentRequestMatcher()
                                            .json(content, fieldsToIgnore.split("\\;")));
                        } else {
                            throw new NoSuchAlgorithmException("Only JSON con use CONTENT-IGNORE-FIELDS");
                        }
                    }
                }
                responseActions.andExpect(method(HttpMethod.valueOf(properties.getProperty("METHOD"))));
                responseActions.andRespond(
                        withStatus(HttpStatus.resolve(Integer.valueOf(properties.getProperty("HTTP-STATUS"))))
                                .contentType(MediaType.valueOf(properties.getProperty("CONTENT-TYPE")))
                                .body(properties.getProperty("RESPONSE")));
            } catch (Exception ex) {
                log.error("Error loading file " + path, ex);
            }
        });
        return serverUUID;
    }
}
