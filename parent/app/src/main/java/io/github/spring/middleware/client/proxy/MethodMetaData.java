package io.github.spring.middleware.client.proxy;

import io.github.spring.middleware.client.params.PathVariableValue;
import io.github.spring.middleware.client.params.RequestParamValue;
import lombok.Data;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
public class MethodMetaData {

    private Method method;
    private HttpMethod httpMethod;
    private String path;
    private List<ParameterBindingName> parameterNames = new ArrayList<>();
    private Integer bodyParamIndex;
    private boolean cacheable;

    public void addBinding(final ParameterType parameterType, int order, Annotation annotation) {
        parameterNames.add(new ParameterBindingName(parameterType, order, annotation));
    }

    public ExtractedParams extractedParams(Object[] args) {
        ExtractedParams extractedParams = new ExtractedParams();
        extractedParams.setPath(path);
        if (bodyParamIndex != null && bodyParamIndex != -1) {
            extractedParams.setBody(args[bodyParamIndex]);
        }
        parameterNames.stream().map(
                binding -> {
                    switch (binding.parameterType) {
                        case PATH_VARIABLE -> {
                            PathVariable pathVariable = (PathVariable) binding.annotation;
                            if (pathVariable != null) {
                                return new PathVariableValue<>(pathVariable, args[binding.order]);
                            }
                        }
                        case REQUEST_PARAM -> {
                            RequestParam requestParam = (RequestParam) binding.annotation;
                            if (requestParam != null) {
                                return new RequestParamValue<>(requestParam, args[binding.order]);
                            }
                        }
                        default ->
                                throw new IllegalArgumentException(STR."Unsupported parameter type: \{binding.parameterType}");
                    }
                    return null;
                }
        ).filter(Objects::nonNull).forEach(pathOrRequestParam -> {
            if (pathOrRequestParam instanceof PathVariableValue) {
                extractedParams.getPathVariables().add((PathVariableValue<?>) pathOrRequestParam);
            } else if (pathOrRequestParam instanceof RequestParamValue) {
                extractedParams.getRequestParams().add((RequestParamValue<?>) pathOrRequestParam);
            }
        });
        return extractedParams;
    }

    public static class ExtractedParams {
        private List<PathVariableValue<?>> pathVariables = new ArrayList<>();
        private List<RequestParamValue<?>> requestParams = new ArrayList<>();
        private String path;
        private Object body;

        public List<PathVariableValue<?>> getPathVariables() {
            return pathVariables;
        }

        public List<RequestParamValue<?>> getRequestParams() {
            return requestParams;
        }

        public Object getBody() {
            return body;
        }

        public void setBody(Object body) {
            this.body = body;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public record ParameterBindingName(ParameterType parameterType, int order, Annotation annotation) {
    }

    public enum ParameterType {
        PATH_VARIABLE, REQUEST_PARAM, BODY
    }

}
