package io.github.spring.middleware.error;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorMessage implements ErrorDescriptor {

    private int statusCode;
    private String statusMessage;
    private String code;
    private String message;
    private Map<String, Object> extensions = new HashMap<>();


    @Override
    @JsonIgnore
    public ErrorCodes getErrorCode() {
        return ErrorCodes.fromCode(code, message);
    }
}
