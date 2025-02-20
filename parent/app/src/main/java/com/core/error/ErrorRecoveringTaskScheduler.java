package com.core.error;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ErrorRecoveringTaskScheduler {

    private Map<UUID, ErrorRecoveringTask> errorRecoveringTasks = new ConcurrentHashMap<>();
    private Flux<ErrorRecoveringTaskDefinition> errorRecoveringTaskDefinitions = null;

    @Autowired
    private ErrorRecoveringTaskDefinitionLoader errorRecoveringTaskDefinitionLoader;
    @Autowired
    private ErrorRecoveringTaskFactory errorRecoveringTaskFactory;
    @Autowired
    private ErrorRecoveringTaskExecutor errorRecoveringTaskExecutor;
    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @PostConstruct
    public void scheduleTasks() {

        errorRecoveringTaskDefinitions = errorRecoveringTaskDefinitionLoader.loadErrorRecoveringTaskDefinitionFromClassPath();
        errorRecoveringTaskDefinitions.map(errorRecoveringTaskDefinition -> {
            log.info("Creating errorRecoveringTask with definition " + errorRecoveringTaskDefinition);
            return errorRecoveringTaskFactory
                    .createErrorRecoveringTask(errorRecoveringTaskDefinition, errorRecoveringTaskExecutor);
        }).map(errorRecoveringTask -> {
            addTaskToScheduledTasks(errorRecoveringTask);
            return errorRecoveringTask;
        }).doOnNext(errorRecoveringTask -> {
            threadPoolTaskScheduler.scheduleAtFixedRate(errorRecoveringTask,
                    errorRecoveringTask.getErrorRecoveringTaskDefinition().getFixedDelayMilis());
        }).switchIfEmpty(e -> {
            if (log.isInfoEnabled()) {
                log.debug("No task definitions configured");
            }
        }).subscribe(errorRecoveringTask -> {
            if (log.isDebugEnabled()) {
                log.debug("Scheduled task " + errorRecoveringTask);
            }
        });
    }

    private void addTaskToScheduledTasks(ErrorRecoveringTask errorRecoveringTask) {

        Optional.ofNullable(errorRecoveringTask).ifPresent(t -> errorRecoveringTasks.put(t.getUuid(), t));
    }

}
