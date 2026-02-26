package com.middleware.mock;

import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.JsonExpectationsHelper;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.match.ContentRequestMatchers;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JsonIgnoreContentRequestMatcher extends ContentRequestMatchers {

    private JsonExpectationsHelper jsonHelper;

    public RequestMatcher json(String expectedJsonContent, String... ignoreFields) {

        return this.jsonIgnoringFields(expectedJsonContent, ignoreFields);
    }

    public RequestMatcher jsonIgnoringFields(String expectedJsonContent, String... ignoreFields) {

        return (request) -> {
            try {
                MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
                Collection<Customization> customizations = Stream.of(ignoreFields).map(field -> {
                    return new Customization(field, (o1, o2) -> true);
                }).collect(Collectors.toSet());
                CustomComparator customComparator =
                        new CustomComparator(JSONCompareMode.LENIENT,
                                customizations.toArray(new Customization[customizations.size()]));
                JSONAssert.assertEquals(expectedJsonContent, mockRequest.getBodyAsString(), customComparator);

            } catch (Exception var5) {
                throw new AssertionError("Failed to parse expected or actual JSON request content", var5);
            }
        };
    }

}
