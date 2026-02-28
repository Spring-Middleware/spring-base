package io.github.spring.middleware.client.params;

import io.github.spring.middleware.client.proxy.ProxyClientException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class MethodParamExtractor {

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

    public static ExtractedParams extract(Method method, Object[] args) {
        ExtractedParams result = new ExtractedParams();

        // Ensure args is non-null to avoid NPE for methods without parameters
        if (args == null) {
            args = new Object[0];
        }

        // Resolve class-level @RequestMapping prefix if present
        String classPrefix = "";
        Class<?> declaringClass = method.getDeclaringClass();
        if (declaringClass.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping classRequest = declaringClass.getAnnotation(RequestMapping.class);
            if (classRequest.value() != null && classRequest.value().length > 0 && classRequest.value()[0] != null && !classRequest.value()[0].isBlank()) {
                classPrefix = classRequest.value()[0];
            } else if (classRequest.path() != null && classRequest.path().length > 0 && classRequest.path()[0] != null && !classRequest.path()[0].isBlank()) {
                classPrefix = classRequest.path()[0];
            }
        }

        // Resolve method-level mapping (@RequestMapping, @GetMapping, @PostMapping, ...)
        String methodPath = "";
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping rm = method.getAnnotation(RequestMapping.class);
            if (rm.value() != null && rm.value().length > 0 && rm.value()[0] != null && !rm.value()[0].isBlank()) {
                methodPath = rm.value()[0];
            } else if (rm.path() != null && rm.path().length > 0 && rm.path()[0] != null && !rm.path()[0].isBlank()) {
                methodPath = rm.path()[0];
            }
        } else if (method.isAnnotationPresent(GetMapping.class)) {
            GetMapping gm = method.getAnnotation(GetMapping.class);
            if (gm.value() != null && gm.value().length > 0 && gm.value()[0] != null && !gm.value()[0].isBlank()) {
                methodPath = gm.value()[0];
            } else if (gm.path() != null && gm.path().length > 0 && gm.path()[0] != null && !gm.path()[0].isBlank()) {
                methodPath = gm.path()[0];
            }
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            PostMapping pm = method.getAnnotation(PostMapping.class);
            if (pm.value() != null && pm.value().length > 0 && pm.value()[0] != null && !pm.value()[0].isBlank()) {
                methodPath = pm.value()[0];
            } else if (pm.path() != null && pm.path().length > 0 && pm.path()[0] != null && !pm.path()[0].isBlank()) {
                methodPath = pm.path()[0];
            }
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            DeleteMapping dm = method.getAnnotation(DeleteMapping.class);
            if (dm.value() != null && dm.value().length > 0 && dm.value()[0] != null && !dm.value()[0].isBlank()) {
                methodPath = dm.value()[0];
            } else if (dm.path() != null && dm.path().length > 0 && dm.path()[0] != null && !dm.path()[0].isBlank()) {
                methodPath = dm.path()[0];
            }
        } else if (method.isAnnotationPresent(PutMapping.class)) {
            PutMapping pm2 = method.getAnnotation(PutMapping.class);
            if (pm2.value() != null && pm2.value().length > 0 && pm2.value()[0] != null && !pm2.value()[0].isBlank()) {
                methodPath = pm2.value()[0];
            } else if (pm2.path() != null && pm2.path().length > 0 && pm2.path()[0] != null && !pm2.path()[0].isBlank()) {
                methodPath = pm2.path()[0];
            }
        } else if (method.isAnnotationPresent(PatchMapping.class)) {
            PatchMapping pch = method.getAnnotation(PatchMapping.class);
            if (pch.value() != null && pch.value().length > 0 && pch.value()[0] != null && !pch.value()[0].isBlank()) {
                methodPath = pch.value()[0];
            } else if (pch.path() != null && pch.path().length > 0 && pch.path()[0] != null && !pch.path()[0].isBlank()) {
                methodPath = pch.path()[0];
            }
        }

        // Combine class prefix and method path carefully
        String combinedPath = "";
        if (!classPrefix.isBlank() && !methodPath.isBlank()) {
            // ensure single slash between
            combinedPath = classPrefix.endsWith("/") || methodPath.startsWith("/") ? classPrefix + methodPath : classPrefix + "/" + methodPath;
        } else if (!classPrefix.isBlank()) {
            combinedPath = classPrefix;
        } else if (!methodPath.isBlank()) {
            combinedPath = methodPath;
        }

        // Path de la anotación @RequestMapping o @GetMapping etc.
        String path = combinedPath;

        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object argValue = i < args.length ? args[i] : null;

            if (parameter.isAnnotationPresent(PathVariable.class)) {
                PathVariable pathVar = parameter.getAnnotation(PathVariable.class);
                result.getPathVariables().add(new PathVariableValue<>(pathVar, argValue));
            } else if (parameter.isAnnotationPresent(RequestParam.class)) {
                RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
                result.getRequestParams().add(new RequestParamValue<>(requestParam, argValue));
            } else if (parameter.isAnnotationPresent(RequestBody.class)) {
                if (result.getBody() != null) {
                    throw new ProxyClientException("Multiple @RequestBody parameters are not allowed");
                }
                result.setBody(argValue);
            } else {
                throw new ProxyClientException(
                        "Unsupported parameter type in method: " + method.getName() +
                                " -> parameter: " + parameter.getName()
                );
            }
        }
        result.setPath(path);
        return result;
    }
}