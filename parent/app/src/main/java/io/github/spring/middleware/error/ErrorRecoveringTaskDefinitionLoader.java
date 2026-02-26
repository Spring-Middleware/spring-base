package io.github.spring.middleware.error;

import io.github.spring.middleware.converter.JsonConverter;
import io.github.spring.middleware.error.api.ErrorRecoveryAttemptSearch;
import io.github.spring.middleware.error.api.ErrorSearch;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringValueResolver;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ErrorRecoveringTaskDefinitionLoader implements EmbeddedValueResolverAware {

    @Value("${com.commons.error.recovering.tasks.resource:#{null}}")
    private String resourceName;
    @Autowired
    private ApplicationContext applicationContext;
    private StringValueResolver resolver;

    private JsonConverter<List> errorRecoveringTaskDefinitionJsonConverter = new JsonConverter<>(
            List.class, ErrorRecoveringTaskDefinition.class);

    public Flux<ErrorRecoveringTaskDefinition> loadErrorRecoveringTaskDefinitionFromFile() {

        List<ErrorRecoveringTaskDefinition> errorRecoveringTaskDefinitions = new ArrayList<>();
        if (resourceName != null) {
            try {
                File errorRecoveryTaskFile = new File(
                        this.getClass().getClassLoader().getResource(resourceName).toURI());
                String errorRecoveringTasksJson = IOUtils
                        .toString(new FileInputStream(errorRecoveryTaskFile), Charset.defaultCharset());
                errorRecoveringTaskDefinitions = errorRecoveringTaskDefinitionJsonConverter
                        .toObject(errorRecoveringTasksJson);
            } catch (Exception ex) {
                log.error("Error loading receory taks definitions " + resourceName);
            }
        }
        return Flux.fromIterable(errorRecoveringTaskDefinitions);
    }

    public Flux<ErrorRecoveringTaskDefinition> loadErrorRecoveringTaskDefinitionFromClassPath() {

        Reflections reflections = new Reflections("com.triggle");
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(ErrorRecovery.class);
        return Flux.fromIterable(classes.stream().map(clazz -> {
            ErrorRecovery errorRecovery = clazz.getAnnotation(ErrorRecovery.class);
            ErrorRecoveringTaskDefinition errorRecoveringTaskDefinition = new ErrorRecoveringTaskDefinition();
            ErrorSearch errorSearch = new ErrorSearch();
            errorSearch.setClazzName(clazz.getName());
            errorSearch.setServiceName(applicationContext.getId());
            errorSearch.setMaxAttempts(Integer.valueOf(resolver.resolveStringValue(errorRecovery.maxRetries())));
            ErrorRecoveryAttemptSearch errorRecoveryAttemptSearch = new ErrorRecoveryAttemptSearch();
            errorRecoveryAttemptSearch.setRecovered(true);
            errorSearch.setErrorRecoveryAttemptSearch(errorRecoveryAttemptSearch);
            errorRecoveringTaskDefinition.setErrorSearch(errorSearch);
            errorRecoveringTaskDefinition.setFixedDelayMilis(
                    Long.valueOf(resolver.resolveStringValue(errorRecovery.fixedDelayMilis())));
            return errorRecoveringTaskDefinition;
        }).collect(Collectors.toList()));
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver stringValueResolver) {

        this.resolver = stringValueResolver;
    }
}
