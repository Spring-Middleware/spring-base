package io.github.spring.middleware.client.proxy;

import io.github.spring.middleware.client.params.PathVariableValue;
import io.github.spring.middleware.client.params.RequestParamValue;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ResourceMetadaURLResolver {

    public static String resolvePath(
            String path,
            List<PathVariableValue<?>> pathParamsValued,
            List<RequestParamValue<?>> queryParamsValued) {

        // 1) Replace path variables (raw replacement) and then URI-encode the final URI
        // NOTE: This expects @PathVariable("name") to match the {name} placeholders in the path.
        for (PathVariableValue<?> pv : pathParamsValued) {
            String key = pv.getPathParam().value();
            String value = Optional.ofNullable(pv.getValue()).map(Object::toString).orElse("");
            path = path.replace("{" + key + "}", value);
        }

        // 2) Build URI with proper query encoding
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(path);

        for (RequestParamValue<?> qp : queryParamsValued) {
            String key = qp.getQueryParam().value();
            Object val = qp.getValue();
            if (val == null) continue;

            if (val instanceof Collection<?> c) {
                for (Object v : c) {
                    if (v != null) b.queryParam(key, v);
                }
            } else {
                b.queryParam(key, val);
            }
        }

        // encode() will percent-encode query params and any illegal URI chars
        // build(true) keeps any already-encoded sequences; encode() handles the rest safely
        return b.build(true).encode().toUriString();
    }

}
