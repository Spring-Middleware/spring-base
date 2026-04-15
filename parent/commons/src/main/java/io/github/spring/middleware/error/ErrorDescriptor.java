package io.github.spring.middleware.error;

import java.util.HashMap;
import java.util.Map;

public interface ErrorDescriptor extends HasExtensions {

    default String getMessage() {
        return "Error occurred";
    }

    ErrorCodes getErrorCode();          // string (no enum) para no acoplar

    default Map<String, Object> getExtensions() {
        return new HashMap<>();
    }

    static DefaultErrorDescriptor fromErrorCodes(ErrorCodes errorCodes) {

        return new DefaultErrorDescriptor(errorCodes) {

            @Override
            public String getMessage() {
                return errorCodes.getMessage();
            }

            @Override
            public ErrorCodes getErrorCode() {
                return errorCodes;
            }
        };
    }

}

