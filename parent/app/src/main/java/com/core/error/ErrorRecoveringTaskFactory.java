package com.core.error;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ErrorRecoveringTaskFactory {

    @Autowired
    private ErrorRecoveringTaskExecutor errorRecoveringTaskExecutor;

    public ErrorRecoveringTask createErrorRecoveringTask(ErrorRecoveringTaskDefinition errorRecoveringTaskDefinition,
                                                         ErrorRecoveringTaskExecutor errorRecoveringTaskExecutor) {

        return new ErrorRecoveringTask(errorRecoveringTaskDefinition, errorRecoveringTaskExecutor);

    }

}
