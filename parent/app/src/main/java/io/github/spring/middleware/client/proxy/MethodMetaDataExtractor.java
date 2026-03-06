package io.github.spring.middleware.client.proxy;

import io.github.spring.middleware.annotation.NoCacheSession;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class MethodMetaDataExtractor {

    private MethodMetaDataExtractor() {
    }

    public static MethodMetaData extractMetaData(Method method) {

        MethodMetaData md = new MethodMetaData();

        md.setMethod(method);
        md.setHttpMethod(resolveHttpMethod(method));
        md.setPath(resolvePath(method));
        md.setCacheable(resolveCacheable(method));

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
        return md;
    }

    private static boolean resolveCacheable(Method method) {
        // regla equivalente a la que tenías:
        // cacheable si no es delete y no tiene NoCacheSession (y además body == null lo decides en runtime)
        return !method.isAnnotationPresent(DeleteMapping.class)
                && !method.isAnnotationPresent(NoCacheSession.class);
    }


    private static HttpMethod resolveHttpMethod(Method method) {
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

    private static String resolveClassLevelPath(Method method) {
        RequestMapping classRequestMapping = method.getDeclaringClass().getAnnotation(RequestMapping.class);
        if (classRequestMapping == null) {
            return "";
        }
        return firstNonEmpty(classRequestMapping.path(), classRequestMapping.value());
    }

    private static String resolveMethodLevelPath(Method method) {

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

    private static String resolvePath(Method method) {
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