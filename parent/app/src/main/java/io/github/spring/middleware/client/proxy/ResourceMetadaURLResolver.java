package io.github.spring.middleware.client.proxy;

import io.github.spring.middleware.client.params.PathVariableValue;
import io.github.spring.middleware.client.params.RequestParamValue;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ResourceMetadaURLResolver {

    public static <T> String resolvePath(String path, List<PathVariableValue<?>> pathParamsValued,
                                         List<RequestParamValue<?>> queryParamsValued) {

        for (PathVariableValue pathVariableValued : pathParamsValued) {
            path = path.replace("{" + pathVariableValued.getPathParam().value() + "}",
                    Optional.ofNullable(pathVariableValued.getValue()).map(Object::toString).orElse(""));
        }

        if (!queryParamsValued.isEmpty()) {
            StringBuilder query = new StringBuilder("?");
            for (RequestParamValue requestParamValued : queryParamsValued) {
                Object val = requestParamValued.getValue();
                if (val instanceof Collection<?> collection) {
                    for (Object v : collection) {
                        query.append(requestParamValued.getQueryParam().value())
                                .append("=")
                                .append(v)
                                .append("&");
                    }
                } else if (val != null) {
                    query.append(requestParamValued.getQueryParam().value())
                            .append("=")
                            .append(val)
                            .append("&");
                }
            }
            // quitar el último "&"
            query.setLength(query.length() - 1);
            path += query.toString();
        }

        return path;
    }
}
