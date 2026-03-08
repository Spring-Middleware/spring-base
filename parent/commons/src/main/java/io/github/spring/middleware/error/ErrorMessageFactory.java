package io.github.spring.middleware.error;

import io.github.spring.middleware.resolver.CompositeHttpStatusCodeResolver;
import io.github.spring.middleware.resolver.CompositeThrowableErrorResolver;
import io.github.spring.middleware.utils.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class ErrorMessageFactory {

    private final CompositeHttpStatusCodeResolver statusCodeResolver;
    private final CompositeThrowableErrorResolver throwableResolvers;

    public ErrorMessageFactory(CompositeHttpStatusCodeResolver statusCodeResolver,
                               CompositeThrowableErrorResolver throwableResolvers) {
        this.throwableResolvers = throwableResolvers;
        this.statusCodeResolver = statusCodeResolver;
    }

    public ErrorMessage from(Throwable t) {

        // Si tienes tu helper, úsalo; si no, puedes quitar esta línea.
        Throwable root = ExceptionUtils.getExceptionFromRuntimeException(t);

        ErrorDescriptor descriptor = resolveDescriptor(root);

        int statusCode = resolveStatusCode(descriptor);

        HttpStatus status = HttpStatus.resolve(statusCode);
        String statusMessage = status != null ? status.getReasonPhrase() : "Unknown";

        Map<String, Object> extensions =
                Optional.ofNullable(descriptor.getExtensions()).orElse(Map.of());

        return new ErrorMessage(
                statusCode,
                statusMessage,
                descriptor.getCode(),
                safeMessage(descriptor),
                extensions
        );
    }

    public ErrorMessage from(ErrorDescriptor error) {

        int statusCode = resolveStatusCode(error);

        HttpStatus status = HttpStatus.resolve(statusCode);
        String statusMessage = status != null ? status.getReasonPhrase() : "Unknown";

        Map<String, Object> extensions =
                Optional.ofNullable(error.getExtensions()).orElse(Map.of());

        return new ErrorMessage(
                statusCode,
                statusMessage,
                error.getCode(),
                error.getMessage(),
                extensions
        );
    }

    private ErrorDescriptor resolveDescriptor(Throwable t) {
        return throwableResolvers.resolve(t).orElse(FrameworkErrorCodes.UNKNOWN_ERROR);
    }

    private int resolveStatusCode(ErrorDescriptor error) {
        return statusCodeResolver.resolve(error).orElse(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    private String safeMessage(ErrorDescriptor d) {
        // aquí luego puedes meter política de “no filtrar detalles internos”
        return d.getMessage();
    }
}
