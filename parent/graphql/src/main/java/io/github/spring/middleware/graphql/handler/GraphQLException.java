package io.github.spring.middleware.graphql.handler;

import io.github.spring.middleware.error.ErrorDescriptor;
import io.github.spring.middleware.error.RemoteError;
import io.github.spring.middleware.utils.ExceptionUtils;

import java.util.*;
import java.util.regex.Pattern;

public class GraphQLException extends RuntimeException {

    private final Map<String, Object> extensions = new HashMap<>();
    private final String code;
    private String path;
    private Map<String, String> parameters = new HashMap<>();

    // ---------- Constructors ----------

    public GraphQLException(String code, String message) {
        super(message, null, false, false);
        this.code = code;
        this.extensions.put("code", code);
        addServerExtensionIfPresent();
    }

    public GraphQLException(ErrorDescriptor error) {
        super(error != null ? error.getMessage() : null, null, false, false);
        String resolvedCode = error != null ? error.getCode() : null;
        this.code = resolvedCode;
        if (resolvedCode != null) this.extensions.put("code", resolvedCode);

        if (error != null && error.getExtensions() != null && !error.getExtensions().isEmpty()) {
            this.extensions.putAll(error.getExtensions());
        }
        addServerExtensionIfPresent();
    }

    public GraphQLException(String message, Throwable cause) {
        super(message, ExceptionUtils.getExceptionFromRuntimeException(cause), false, false);

        Throwable root = ExceptionUtils.getExceptionFromRuntimeException(cause);

        // Defaults: if no remote info, we keep code null and only message + stack via cause
        String resolvedCode = null;

        if (root instanceof RemoteError remote) {

            resolvedCode = remote.getCode();
            if (resolvedCode != null) {
                this.extensions.put("code", resolvedCode);
            }

            // Remote namespace (stable contract)
            this.extensions.put("remote.code", remote.getCode());
            this.extensions.put("remote.message", remote.getMessage());
            this.extensions.put("remote.httpStatus", remote.getHttpStatusCode());
            this.extensions.put("remote.requestId", remote.getRequestId());

            Map<String, Object> remoteExt = remote.getExtensions();
            if (remoteExt != null && !remoteExt.isEmpty()) {
                this.extensions.put("remote.extensions", remoteExt);
            }

            Object payload = remote.getPayload();
            if (payload != null) {
                this.extensions.put("remote.payload", payload);
            }
        }

        this.code = resolvedCode;
        addServerExtensionIfPresent();
    }

    public GraphQLException(Throwable cause) {
        this(null, cause);
    }

    // ---------- Server metadata hook ----------

    private void addServerExtensionIfPresent() {
        // Hook for adding server info (hostname, instance id, etc).
        // Keep it empty by default to avoid leaking sensitive data.
    }

    // ---------- Message templating ----------

    @Override
    public String getMessage() {
        String msg = Optional.ofNullable(getCause())
                .map(c -> ExceptionUtils.getExceptionMessage(c, 2))
                .orElse(super.getMessage());

        if (msg != null && parameters != null && !parameters.isEmpty()) {
            for (Map.Entry<String, String> p : parameters.entrySet()) {
                msg = msg.replace(p.getKey(), p.getValue());
            }
        }
        return msg;
    }

    // ---------- Public API ----------

    public String getCode() {
        return code;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public void addExtension(String name, Object value) {
        if (name != null) {
            extensions.put(name, value);
        }
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getPathSegments() {
        return Optional.ofNullable(path)
                .map(p -> Arrays.asList(p.split(Pattern.quote("."))))
                .orElseGet(ArrayList::new);
    }

    public void setParameters(String... kvPairs) {
        if (kvPairs == null) return;
        this.parameters = toMap(Arrays.asList(kvPairs));
    }

    private Map<String, String> toMap(List<String> parameters) {
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < parameters.size() - 1; i += 2) {
            params.put(parameters.get(i), parameters.get(i + 1));
        }
        return params;
    }
}