package io.github.spring.middleware.client.proxy;

import com.fasterxml.jackson.databind.type.TypeFactory;
import org.springframework.core.ParameterizedTypeReference;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class MethodReturnTypeResolver {

    /**
     * Construye un ParameterizedTypeReference para un método dado,
     * considerando tipos genéricos (ej. List<Foo>, Map<String,Bar>).
     *
     * @param method Método cuyo tipo de retorno se quiere analizar
     * @return ParameterizedTypeReference<?> adecuado para bodyToMono() / bodyToFlux()
     */
    public static ParameterizedTypeReference<?> getTypeReference(Method method) {
        Type returnType = method.getGenericReturnType();

        if (returnType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) returnType;
            Class<?> rawType = (Class<?>) pType.getRawType();
            Type[] genericArgs = pType.getActualTypeArguments();

            // Convertimos los Type a Class<?>
            Class<?>[] genericClasses = new Class<?>[genericArgs.length];
            for (int i = 0; i < genericArgs.length; i++) {
                if (genericArgs[i] instanceof Class<?>) {
                    genericClasses[i] = (Class<?>) genericArgs[i];
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
            return ParameterizedTypeReference.forType((Class<?>) returnType);
        } else {
            // fallback conservador
            return ParameterizedTypeReference.forType(Object.class);
        }
    }
}
