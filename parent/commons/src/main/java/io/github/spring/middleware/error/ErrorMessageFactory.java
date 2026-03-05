package io.github.spring.middleware.error;

import io.github.spring.middleware.resolver.HttpStatusCodeResolver;
import io.github.spring.middleware.resolver.ThrowableErrorResolver;
import io.github.spring.middleware.utils.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ErrorMessageFactory {

    private final List<HttpStatusCodeResolver> statusResolvers;
    private final List<ThrowableErrorResolver> throwableResolvers;

    public ErrorMessageFactory(List<ThrowableErrorResolver> throwableResolvers,
                               List<HttpStatusCodeResolver> statusResolvers) {
        this.throwableResolvers = throwableResolvers;
        this.statusResolvers = statusResolvers;
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
        for (ThrowableErrorResolver r : throwableResolvers) {
            Optional<ErrorDescriptor> out = r.resolve(t);
            if (out.isPresent()) return out.get();
        }
        // En teoría nunca llegas aquí si tienes UnknownErrorResolver
        return FrameworkErrorCodes.UNKNOWN_ERROR;
    }

    private int resolveStatusCode(ErrorDescriptor error) {

        for (HttpStatusCodeResolver resolver : statusResolvers) {
            Optional<Integer> resolved = resolver.resolve(error);
            if (resolved.isPresent()) {
                return resolved.get();
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private String safeMessage(ErrorDescriptor d) {
        // aquí luego puedes meter política de “no filtrar detalles internos”
        return d.getMessage();
    }
}
