package io.github.spring.middleware;

import io.github.spring.middleware.annotations.MiddlewareClient;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class RegistryClientScanTest {

    @Test
    public void shouldFindRegistryClientAnnotatedWithMiddlewareClient() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
        scanner.addIncludeFilter(new AnnotationTypeFilter(MiddlewareClient.class));
        scanner.setResourceLoader(new DefaultResourceLoader());

        var components = scanner.findCandidateComponents("io.github.spring.middleware.client");
        System.out.println("Found components: " + components);

        // assert that at least one candidate is found
        assertFalse(components.isEmpty(), "No components found in package io.github.spring.middleware.client; check classpath and compilation");
    }
}

