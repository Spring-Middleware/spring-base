package io.github.spring.middleware.client.proxy;

import io.github.spring.middleware.annotation.MiddlewareCircuitBreaker;
import io.github.spring.middleware.annotation.NoCacheSession;
import io.github.spring.middleware.annotation.security.MiddlewareApiKeyValue;
import io.github.spring.middleware.annotation.security.MiddlewareRequiredScopes;
import io.github.spring.middleware.client.proxy.security.method.ApiKeyMethodSecurityConfiguration;
import io.github.spring.middleware.client.proxy.security.method.ClientCredentialsMethodSecurityConfiguration;
import io.github.spring.middleware.client.proxy.security.method.VoidMethodSecurityConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

@Component
public class MethodMetaDataExtractor {

    private final Environment environment;

    public MethodMetaDataExtractor(Environment environment) {
        this.environment = environment;
    }

    public MethodMetaData extractMetaData(Method method) {

        MethodMetaData md = new MethodMetaData();

        md.setMethod(method);
        md.setHttpMethod(resolveHttpMethod(method));
        md.setPath(resolvePath(method));
        md.setCacheable(resolveCacheable(method));
        md.setCircuitBreakerParameters(createCircuitBreakerParameters(method));

        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter p = parameters[i];
            if (p.isAnnotationPresent(PathVariable.class)) {
                PathVariable ann = p.getAnnotation(PathVariable.class);
                if (ann.value() == null || ann.value().isEmpty()) {
                    throw new IllegalArgumentException("@PathVariable parameters must have a non-empty value");
                }
                md.addBinding(MethodMetaData.ParameterType.PATH_VARIABLE, i, ann);
            } else if (p.isAnnotationPresent(RequestParam.class)) {
                RequestParam ann = p.getAnnotation(RequestParam.class);
                if (ann.value() == null || ann.value().isEmpty()) {
                    throw new IllegalArgumentException("@RequestParam parameters must have a non-empty value");
                }
                md.addBinding(MethodMetaData.ParameterType.REQUEST_PARAM, i, ann);
            } else if (p.isAnnotationPresent(RequestBody.class)) {
                if (md.getBodyParamIndex() != null) {
                    throw new IllegalArgumentException("Multiple @RequestBody parameters are not allowed");
                }
                md.setBodyParamIndex(i);
            }
        }

        List<String> requiredScopes = resolveRequiredScopes(method);
        String apiKeyValue = resolveApiKey(method);

        if (!requiredScopes.isEmpty() && apiKeyValue != null) {
            throw new IllegalArgumentException(
                    "Method '%s' cannot declare both @MiddlewareRequiredScopes and @MiddlewareApiKeyValue"
                            .formatted(method.getName())
            );
        }

        if (!requiredScopes.isEmpty()) {
            md.setMethodSecurityConfiguration(new ClientCredentialsMethodSecurityConfiguration(requiredScopes));
        } else if (apiKeyValue != null) {
            md.setMethodSecurityConfiguration(new ApiKeyMethodSecurityConfiguration(apiKeyValue));
        } else {
            md.setMethodSecurityConfiguration(new VoidMethodSecurityConfiguration());
        }

        return md;
    }


    private List<String> resolveRequiredScopes(Method method) {
        if (!method.isAnnotationPresent(MiddlewareRequiredScopes.class)) {
            return List.of();
        }

        MiddlewareRequiredScopes requiredScopes = method.getAnnotation(MiddlewareRequiredScopes.class);

        return Arrays.stream(requiredScopes.value())
                .map(environment::resolvePlaceholders)
                .map(String::trim)
                .filter(scope -> !scope.isEmpty())
                .distinct()
                .toList();
    }

    private String resolveApiKey(Method method) {
        if (!method.isAnnotationPresent(MiddlewareApiKeyValue.class)) {
            return null;
        }

        MiddlewareApiKeyValue apiKeyAnnotation = method.getAnnotation(MiddlewareApiKeyValue.class);
        String apiKeyValue = environment.resolvePlaceholders(apiKeyAnnotation.value()).trim();

        if (apiKeyValue.isEmpty()) {
            throw new IllegalArgumentException("@MiddlewareApiKeyValue value cannot be empty");
        }
        return apiKeyValue;
    }


    private MiddlewareCircuitBreakerParameters createCircuitBreakerParameters(Method method) {
        if (method.isAnnotationPresent(MiddlewareCircuitBreaker.class)) {
            final MiddlewareCircuitBreakerParameters circuitBreakerParameters = new MiddlewareCircuitBreakerParameters();
            MiddlewareCircuitBreaker circuitBreaker = method.getAnnotation(MiddlewareCircuitBreaker.class);
            circuitBreakerParameters.setEnanbled(Boolean.valueOf(environment.resolvePlaceholders(circuitBreaker.enabled())));
            circuitBreakerParameters.setFailureRateThreshold(Float.valueOf(environment.resolvePlaceholders(circuitBreaker.failureRateThreshold())));
            circuitBreakerParameters.setMinimumNumberOfCalls(Integer.valueOf(environment.resolvePlaceholders(circuitBreaker.minimumNumberOfCalls())));
            circuitBreakerParameters.setSlidingWindowSize(Integer.valueOf(environment.resolvePlaceholders(circuitBreaker.slidingWindowSize())));
            circuitBreakerParameters.setWaitDurationInOpenStateMs(Long.valueOf(environment.resolvePlaceholders(circuitBreaker.waitDurationInOpenStateMs())));
            circuitBreakerParameters.setPermittedNumberOfCallsInHalfOpenState(Integer.valueOf(environment.resolvePlaceholders(circuitBreaker.permittedNumberOfCallsInHalfOpenState())));
            Arrays.stream(circuitBreaker.statusShouldOpenBreaker()).forEach(expresion -> {
                circuitBreakerParameters.getOpenCircuitBreakerStatusExpressions().add(environment.resolvePlaceholders(expresion));
            });
            Arrays.stream(circuitBreaker.statusShouldIgnoreBreaker()).forEach(expresion -> {
                circuitBreakerParameters.getIgnoreCircuitBreakerStatusExpressions().add(environment.resolvePlaceholders(expresion));
            });
            return circuitBreakerParameters;
        }
        return null;
    }


    private boolean resolveCacheable(Method method) {
        // regla equivalente a la que tenías:
        // cacheable si no es delete y no tiene NoCacheSession (y además body == null lo decides en runtime)
        return !method.isAnnotationPresent(DeleteMapping.class)
                && !method.isAnnotationPresent(NoCacheSession.class);
    }


    private HttpMethod resolveHttpMethod(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) return HttpMethod.GET;
        if (method.isAnnotationPresent(PostMapping.class)) return HttpMethod.POST;
        if (method.isAnnotationPresent(PutMapping.class)) return HttpMethod.PUT;
        if (method.isAnnotationPresent(DeleteMapping.class)) return HttpMethod.DELETE;
        if (method.isAnnotationPresent(PatchMapping.class)) return HttpMethod.PATCH;
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMethod[] rm = method.getAnnotation(RequestMapping.class).method();
            if (rm.length > 0) return HttpMethod.valueOf(rm[0].name());
        }
        throw new IllegalArgumentException("Method must be annotated with a mapping annotation");
    }

    private String resolveClassLevelPath(Method method) {
        RequestMapping classRequestMapping = method.getDeclaringClass().getAnnotation(RequestMapping.class);
        if (classRequestMapping == null) {
            return "";
        }
        return firstNonEmpty(classRequestMapping.path(), classRequestMapping.value());
    }

    private String resolveMethodLevelPath(Method method) {

        // value y path son aliases: cubrimos ambos y el caso vacío.
        if (method.isAnnotationPresent(GetMapping.class)) {
            GetMapping a = method.getAnnotation(GetMapping.class);
            return firstNonEmpty(a.path(), a.value());
        }
        if (method.isAnnotationPresent(PostMapping.class)) {
            PostMapping a = method.getAnnotation(PostMapping.class);
            return firstNonEmpty(a.path(), a.value());
        }
        if (method.isAnnotationPresent(PutMapping.class)) {
            PutMapping a = method.getAnnotation(PutMapping.class);
            return firstNonEmpty(a.path(), a.value());
        }
        if (method.isAnnotationPresent(DeleteMapping.class)) {
            DeleteMapping a = method.getAnnotation(DeleteMapping.class);
            return firstNonEmpty(a.path(), a.value());
        }
        if (method.isAnnotationPresent(PatchMapping.class)) {
            PatchMapping a = method.getAnnotation(PatchMapping.class);
            return firstNonEmpty(a.path(), a.value());
        }
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping a = method.getAnnotation(RequestMapping.class);
            return firstNonEmpty(a.path(), a.value());
        }

        throw new IllegalArgumentException("Method must be annotated with a mapping annotation");
    }

    private String resolvePath(Method method) {
        String classPath = resolveClassLevelPath(method);
        String methodPath = resolveMethodLevelPath(method);
        return joinPaths(classPath, methodPath);
    }

    private static String joinPaths(String classPath, String methodPath) {
        String left = classPath == null ? "" : classPath.trim();
        String right = methodPath == null ? "" : methodPath.trim();

        if (left.isEmpty()) {
            return normalizePath(right);
        }
        if (right.isEmpty()) {
            return normalizePath(left);
        }

        return normalizePath(
                left.endsWith("/") ? left.substring(0, left.length() - 1) : left,
                right.startsWith("/") ? right : "/" + right
        );
    }

    private static String normalizePath(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null) {
                sb.append(part);
            }
        }
        String path = sb.toString().replaceAll("//+", "/");
        return path.startsWith("/") ? path : "/" + path;
    }

    private static String firstNonEmpty(String[] a, String[] b) {
        if (a != null && a.length > 0 && a[0] != null) return a[0];
        if (b != null && b.length > 0 && b[0] != null) return b[0];
        return "";
    }

}