package io.github.spring.middleware.client.proxy;

import com.fasterxml.jackson.databind.type.TypeFactory;
import org.springframework.core.ParameterizedTypeReference;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Construye un {@code ParameterizedTypeReference} para un método dado,
 * considerando tipos genéricos (ej. {@code List<Foo>}, {@code Map<String,Bar>}).
 */
public class MethodReturnTypeResolver {

    /**
     * Construye un ParameterizedTypeReference para un método dado,
     * considerando tipos genéricos (ej. {@code List<Foo>}, {@code Map<String,Bar>}).
     *
     * @param method Método cuyo tipo de retorno se quiere analizar
     * @return {@code ParameterizedTypeReference<?>} adecuado para {@code bodyToMono()} / {@code bodyToFlux()}
     */
    public static ParameterizedTypeReference<?> getTypeReference(Method method) {
        Type returnType = method.getGenericReturnType();

        if (returnType instanceof ParameterizedType pType) {
            Class<?> rawType = (Class<?>) pType.getRawType();
            Type[] genericArgs = pType.getActualTypeArguments();

            // Convertimos los Type a Class<?>
            Class<?>[] genericClasses = new Class<?>[genericArgs.length];
            for (int i = 0; i < genericArgs.length; i++) {
                if (genericArgs[i] instanceof Class<?> cls) {
                    genericClasses[i] = cls;
                } else {
                    // fallback: si es un tipo complejo (ej. ? extends Foo), usamos Object
                    genericClasses[i] = Object.class;
                }
            }

            // Construimos tipo paramétrico con Jackson
            return ParameterizedTypeReference.forType(
                    TypeFactory.defaultInstance().constructParametricType(rawType, genericClasses)
            );
        } else if (returnType instanceof Class<?>) {
            // No genérico
            return ParameterizedTypeReference.forType(returnType);
        } else {
            // fallback conservador
            return ParameterizedTypeReference.forType(Object.class);
        }
    }
}
