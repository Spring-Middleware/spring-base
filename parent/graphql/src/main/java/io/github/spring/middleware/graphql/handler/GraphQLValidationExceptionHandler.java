package io.github.spring.middleware.graphql.handler;

import graphql.GraphQLError;
import graphql.GraphQLException;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ResultPath;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;
import io.github.spring.middleware.error.ConstraintErrorResolver;
import io.github.spring.middleware.error.ErrorDescriptor;
import io.github.spring.middleware.error.ErrorMessage;
import io.github.spring.middleware.error.ErrorMessageFactory;
import io.github.spring.middleware.error.RemoteError;
import io.github.spring.middleware.utils.ExceptionUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.hibernate.LazyInitializationException;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class GraphQLValidationExceptionHandler implements DataFetcherExceptionHandler {

    private final ConstraintErrorResolver errorResolver;
    private final ErrorMessageFactory errorMessageFactory;

    public GraphQLValidationExceptionHandler(ConstraintErrorResolver errorResolver, ErrorMessageFactory errorMessageFactory) {
        this.errorResolver = errorResolver;
        this.errorMessageFactory = errorMessageFactory;
    }

    @Override
    public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(
            DataFetcherExceptionHandlerParameters parameters) {

        DataFetchingEnvironment env = parameters.getDataFetchingEnvironment();

        Throwable exception = ExceptionUtils.getExceptionFromRuntimeException(parameters.getException());

        ResultPath path = env.getExecutionStepInfo().getPath();
        SourceLocation sourceLocation = env.getField().getSourceLocation();

        List<GraphQLError> errors = new ArrayList<>();

        if (exception instanceof ConstraintViolationException validationEx) {
            for (ConstraintViolation<?> violation : validationEx.getConstraintViolations()) {

                Annotation annotation = violation.getConstraintDescriptor().getAnnotation();
                Class<? extends Annotation> annoType = annotation.annotationType();

                ErrorDescriptor descriptor = errorResolver.resolveFromAnnotation(annoType);
                ErrorMessage errorMessage = errorMessageFactory.from(descriptor);

                errorMessage.getExtensions().putIfAbsent("field", extractFieldName(violation.getPropertyPath()));
                errorMessage.getExtensions().putIfAbsent("validationMessage", violation.getMessage());
                errorMessage.getExtensions().putIfAbsent("constraint", annoType.getSimpleName());

                ResultPath validationPath = buildPath(path, violation.getPropertyPath());

                errors.add(buildError(errorMessage, validationPath, sourceLocation));
            }

            return completed(errors);
        }

        if (exception instanceof RemoteError remoteEx && remoteEx.getErrorMessage() != null) {
            errors.add(buildError(remoteEx.getErrorMessage(), path, sourceLocation));
            return completed(errors);
        }

        if (exception instanceof LazyInitializationException) {
            return completed(errors);
        }

        ErrorMessage error = errorMessageFactory.from(exception);
        errors.add(buildError(error, path, sourceLocation));
        return completed(errors);
    }

    private GraphQLError buildError(ErrorMessage errorMessage, ResultPath resultPath, SourceLocation location) {
        Map<String, Object> extensions = new LinkedHashMap<>();

        extensions.put("code", errorMessage.getCode());
        extensions.put("statusCode", errorMessage.getStatusCode());
        extensions.put("statusMessage", errorMessage.getStatusMessage());

        if (errorMessage.getExtensions() != null) {
            extensions.putAll(errorMessage.getExtensions());
        }

        return GraphqlErrorBuilder.newError()
                .message(errorMessage.getMessage())
                .path(resultPath)
                .location(location)
                .extensions(extensions)
                .build();
    }


    private ResultPath buildPath(ResultPath basePath, Path validationPath) {

        List<Object> nodes = new ArrayList<>(basePath.toList());

        validationPath.forEach(node -> {
            String name = node.getName();
            if (name != null && !name.isEmpty()
                    && nodes.stream().noneMatch(n -> n != null && n.toString().startsWith(name))) {
                nodes.add(name);
            }
        });

        return ResultPath.fromList(nodes);
    }

    private String extractFieldName(Path path) {
        String field = null;
        for (Path.Node node : path) {
            field = node.getName();
        }
        return field;
    }

    private CompletableFuture<DataFetcherExceptionHandlerResult> completed(List<GraphQLError> errors) {

        return CompletableFuture.completedFuture(
                DataFetcherExceptionHandlerResult.newResult()
                        .errors(errors)
                        .build()
        );
    }
}