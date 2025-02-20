package com.core.error;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorMessage {

    private Integer statusCode;
    private String statusMessage;
    private String errorCode;
    private String errorMessage;
    private ErrorLevel errorLevel;

    @Override
    public String toString() {

        return "ErrorMessage{" +
                "statusCode=" + statusCode +
                ", statusMessage='" + statusMessage + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorLevel=" + errorLevel +
                '}';
    }
}
