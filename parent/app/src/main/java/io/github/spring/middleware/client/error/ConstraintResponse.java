package io.github.spring.middleware.client.error;

import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@XmlRootElement(name = "Response")
public class ConstraintResponse extends ErrorResponse {

    private List<ConstraintMessage> constraintMessages;

    public List<ConstraintMessage> getConstraintMessages() {

        return constraintMessages;
    }

    public void setConstraintMessages(List<ConstraintMessage> constraintMessages) {

        this.constraintMessages = constraintMessages;
    }

    public String getErrorSystemMessage() {

        StringBuffer systemMessage = new StringBuffer(super.getErrorSystemMessage() + " ");
        CollectionUtils.emptyIfNull(constraintMessages).forEach(
                constraintMessage -> systemMessage.append(constraintMessage.getSystemConstraintMessage()).append(" "));
        return systemMessage.toString();
    }

    public List<Map<String, ?>> getDetails() {

        return CollectionUtils.emptyIfNull(constraintMessages).stream().map(msg -> msg.getDetails())
                .filter(msg -> msg != null).collect(Collectors.toList());
    }

}
