package io.github.spring.middleware.client.proxy;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProxyClientAnalyzer {

    private final MethodMetaDataExtractor methodMetaDataExtractor;

    public Map<Method, MethodMetaData> analyze(Class<?> proxyClientInterface) {
        Map<Method, MethodMetaData> methodMethodMetaDataMap = new HashMap<>();
        for (Method method : proxyClientInterface.getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            methodMethodMetaDataMap.putIfAbsent(method, methodMetaDataExtractor.extractMetaData(method));
        }
        return methodMethodMetaDataMap;
    }
}
