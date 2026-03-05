package io.github.spring.middleware.graphql.handler;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ResultPath;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;
import io.github.spring.middleware.error.ConstraintErrorResolver;
import io.github.spring.middleware.error.ErrorDescriptor;
import io.github.spring.middleware.error.FrameworkErrorCodes;
import io.github.spring.middleware.exception.ServiceException;
import io.github.spring.middleware.utils.ExceptionUtils;
import jakarta.persistence.PersistenceException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.hibernate.LazyInitializationException;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class GraphQLValidationExceptionHandler implements DataFetcherExceptionHandler {

    private final ConstraintErrorResolver errorResolver;

    public GraphQLValidationExceptionHandler(ConstraintErrorResolver errorResolver) {
        this.errorResolver = errorResolver;
    }

    @Override
    public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(
            DataFetcherExceptionHandlerParameters parameters) {

        DataFetchingEnvironment env = parameters.getDataFetchingEnvironment();

        Throwable exception = ExceptionUtils.getExceptionFromRuntimeException(parameters.getException());

        ResultPath path = env.getExecutionStepInfo().getPath();
        SourceLocation sourceLocation = env.getField().getSourceLocation();

        List<GraphQLError> errors = new ArrayList<>();

        // 0) Si ya es una GraphQLException nuestra, la renderizamos tal cual
        if (exception instanceof GraphQLException gqlEx) {
            errors.add(buildError(gqlEx, path, sourceLocation));
            return completed(errors);
        }

        if (exception instanceof ErrorDescriptor ed) {
            GraphQLException gqlEx = new GraphQLException(ed);
            errors.add(buildError(gqlEx, path, sourceLocation));
            return completed(errors);
        }

        // 1) Hibernate DB constraint violation (unique, fk, etc.)
        if (exception instanceof PersistenceException
                && exception.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {

            org.hibernate.exception.ConstraintViolationException dbViolation =
                    (org.hibernate.exception.ConstraintViolationException) exception.getCause();

            ErrorDescriptor descriptor =
                    errorResolver.resolveFromDbConstraintName(dbViolation.getConstraintName());

            GraphQLException gqlEx = new GraphQLException(descriptor);
            errors.add(buildError(gqlEx, path, sourceLocation));
            return completed(errors);
        }

        // 2) Bean Validation (jakarta.validation)
        if (exception instanceof ConstraintViolationException validationEx) {

            for (ConstraintViolation<?> violation : validationEx.getConstraintViolations()) {

                Annotation annotation = violation.getConstraintDescriptor().getAnnotation();
                Class<? extends Annotation> annoType = annotation.annotationType();

                ErrorDescriptor descriptor = errorResolver.resolveFromAnnotation(annoType);

                GraphQLException gqlEx = new GraphQLException(descriptor);
                // si quieres meter params (min/max/etc) esto es el sitio
                // gqlEx.setParameters(...)

                ResultPath validationPath = buildPath(path, violation.getPropertyPath());

                errors.add(buildError(gqlEx, validationPath, sourceLocation));
            }

            return completed(errors);
        }

        // 3) LazyInitializationException -> ignorar (como hacías antes)
        if (exception instanceof LazyInitializationException) {
            return completed(errors);
        }

        // 4) Fallback UNKNOWN (framework default)
        GraphQLException unknown = new GraphQLException(
                FrameworkErrorCodes.UNKNOWN_ERROR.getCode(),
                Optional.ofNullable(ExceptionUtils.getExceptionMessage(exception, 2))
                        .orElse(FrameworkErrorCodes.UNKNOWN_ERROR.getMessage())
        );
        errors.add(buildError(unknown, path, sourceLocation));
        return completed(errors);
    }

    private GraphQLError buildError(GraphQLException ex, ResultPath path, SourceLocation location) {

        // Si la excepción trae pathSegments adicionales (por ejemplo "a.b.c"),
        // las mergeamos al path actual.
        ResultPath mergedPath = mergePath(path, ex);

        return GraphqlErrorBuilder.newError()
                .message(ex.getMessage())
                .path(mergedPath)
                .location(location)
                .extensions(ex.getExtensions())
                .build();
    }

    private ResultPath mergePath(ResultPath base, GraphQLException ex) {
        List<String> extra = ex.getPathSegments();
        if (extra == null || extra.isEmpty()) {
            return base;
        }
        List<Object> nodes = new ArrayList<>(base.toList());
        nodes.addAll(extra);
        return ResultPath.fromList(nodes);
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

    private CompletableFuture<DataFetcherExceptionHandlerResult> completed(List<GraphQLError> errors) {

        return CompletableFuture.completedFuture(
                DataFetcherExceptionHandlerResult.newResult()
                        .errors(errors)
                        .build()
        );
    }
}