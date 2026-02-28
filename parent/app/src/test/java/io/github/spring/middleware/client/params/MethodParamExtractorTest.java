package io.github.spring.middleware.client.params;

import io.github.spring.middleware.client.RegistryClient;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class MethodParamExtractorTest {

    @Test
    public void extract_getRegistryEntry_shouldReturnRequestParam() throws NoSuchMethodException {
        Method m = RegistryClient.class.getMethod("getRegistryEntry", String.class);
        MethodParamExtractor.ExtractedParams p = MethodParamExtractor.extract(m, new Object[]{"my-resource"});

        assertEquals("", p.getPath());
        assertEquals(1, p.getRequestParams().size());
        assertEquals(0, p.getPathVariables().size());
        assertNull(p.getBody());
    }

    @Test
    public void extract_getRegistryMap_shouldReturnMapPath() throws NoSuchMethodException {
        Method m = RegistryClient.class.getMethod("getRegistryMap");
        MethodParamExtractor.ExtractedParams p = MethodParamExtractor.extract(m, new Object[]{});

        assertEquals("/map", p.getPath());
        assertTrue(p.getRequestParams().isEmpty());
        assertTrue(p.getPathVariables().isEmpty());
    }

    @Test
    public void extract_getSchemaLocation_shouldReturnPathVariable() throws NoSuchMethodException {
        Method m = RegistryClient.class.getMethod("getSchemaLocation", String.class);
        MethodParamExtractor.ExtractedParams p = MethodParamExtractor.extract(m, new Object[]{"ns"});

        assertEquals("/schema/{namespace}", p.getPath());
        assertEquals(1, p.getPathVariables().size());
    }

    @Test
    public void extract_registerResource_shouldReturnBodyAndPath() throws NoSuchMethodException {
        Method m = RegistryClient.class.getMethod("registerResource", Object.class);
        MethodParamExtractor.ExtractedParams p = MethodParamExtractor.extract(m, new Object[]{new Object()});

        assertEquals("/resource", p.getPath());
        assertNotNull(p.getBody());
    }
}

