package io.github.spring.middleware.client.error;

import java.util.Map;

public class ConstraintMessage {

    private String constraintErrorCode;
    private String systemConstraintMessage;
    private String constraintMessage;
    private Map<String, ?> details;

    public String getConstraintErrorCode() {

        return constraintErrorCode;
    }

    public void setConstraintErrorCode(String constraintErrorCode) {

        this.constraintErrorCode = constraintErrorCode;
    }

    public String getSystemConstraintMessage() {

        return systemConstraintMessage;
    }

    public void setSystemConstraintMessage(String systemConstraintMessage) {

        this.systemConstraintMessage = systemConstraintMessage;
    }

    public String getConstraintMessage() {

        return constraintMessage;
    }

    public void setConstraintMessage(String constraintMessage) {

        this.constraintMessage = constraintMessage;
    }

    public Map<String, ?> getDetails() {

        return details;
    }

    public void setDetails(Map<String, ?> details) {

        this.details = details;
    }
}
