package io.github.spring.middleware.error;

import java.util.Collections;
import java.util.Map;

public interface ErrorDescriptor extends HasExtensions {

    String getMessage();

    String getCode();          // string (no enum) para no acoplar

    default Map<String, Object> getExtensions() {
        return Collections.emptyMap();
    }

}
