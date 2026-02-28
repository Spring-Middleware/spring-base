package io.github.spring.middleware.client.error;

import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@XmlRootElement(name = "Response")
public class ErrorResponse<T> {

    private boolean successful = Boolean.FALSE;
    private String idErrorCode;
    private String errorCode;
    private String errorMessage;
    private String errorSystemMessage;
    private String server;
    private T data;

    public ErrorResponse() {

        super();
    }

    public boolean isSuccessful() {

        return successful;
    }

    public void setSuccessful(boolean successful) {

        this.successful = successful;
    }

    public String getIdErrorCode() {

        return idErrorCode;
    }

    public void setIdErrorCode(String idErrorCode) {

        this.idErrorCode = idErrorCode;
    }

    public String getErrorSystemMessage() {

        return errorSystemMessage;
    }

    public void setErrorSystemMessage(String errorSystemMessage) {

        this.errorSystemMessage = errorSystemMessage;
    }

    public String getErrorMessage() {

        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {

        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {

        return errorCode;
    }

    public void setErrorCode(String errorCode) {

        this.errorCode = errorCode;
    }

    public T getData() {

        return data;
    }

    public void setData(T data) {

        this.data = data;
    }

    public List<Map<String, ?>> getDetails() {

        return new ArrayList<>();
    }

    public String getServer() {

        return server;
    }

    public void setServer(String server) {

        this.server = server;
    }

    public static ErrorResponse nok(String errorSystemMessage) {

        ErrorResponse basicResponse = new ErrorResponse();
        basicResponse.setSuccessful(Boolean.FALSE);
        basicResponse.setErrorSystemMessage(errorSystemMessage);
        return basicResponse;
    }

    public static ErrorResponse ok() {

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setSuccessful(Boolean.TRUE);
        return errorResponse;
    }

    public static <T> ErrorResponse ok(T data) {

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setSuccessful(Boolean.TRUE);
        errorResponse.setData(data);
        return errorResponse;
    }

}
