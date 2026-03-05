package io.github.spring.middleware.error;

import java.util.HashMap;
import java.util.Map;

public interface ErrorDescriptor extends HasExtensions {

    default String getMessage() {
        return "Error occurred";
    }

    String getCode();          // string (no enum) para no acoplar

    default Map<String, Object> getExtensions() {
        return new HashMap<>();
    }

}

