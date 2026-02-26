package com.middleware.error;

import com.middleware.error.api.ErrorSearch;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Data
public class ErrorRecoveringTaskDefinition {

    private ErrorSearch errorSearch;
    private long fixedDelayMilis;

    @Override
    public String toString() {

        return new ToStringBuilder(this)
                .append("errorSearch", errorSearch)
                .append("fixedDelayMilis", fixedDelayMilis)
                .toString();
    }
}
