package io.github.spring.middleware.error;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.UUID;

@Slf4j
@Getter
public class ErrorRecoveringTask implements Runnable {

    private final UUID uuid;
    private final ErrorRecoveringTaskDefinition errorRecoveringTaskDefinition;
    private final ErrorRecoveringTaskExecutor errorRecoveringTaskExecutor;

    public ErrorRecoveringTask(ErrorRecoveringTaskDefinition errorRecoveringTaskDefinition,
                               ErrorRecoveringTaskExecutor errorRecoveringTaskExecutor) {

        this.errorRecoveringTaskExecutor = errorRecoveringTaskExecutor;
        this.errorRecoveringTaskDefinition = errorRecoveringTaskDefinition;
        this.uuid = UUID.randomUUID();
    }

    @Override
    public void run() {

        try {
            errorRecoveringTaskExecutor.execute(this);
        } catch (Exception ex) {
            log.error("Error execution errorRecoeringTask " + uuid);
        }
    }

    @Override
    public String toString() {

        return new ToStringBuilder(this)
                .append("uuid", uuid)
                .append("errorRecoveringTaskDefinition", errorRecoveringTaskDefinition)
                .toString();
    }
}
