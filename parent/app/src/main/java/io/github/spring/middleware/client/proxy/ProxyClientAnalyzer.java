package io.github.spring.middleware.client.proxy;


import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Component
public class ProxyClientAnalyzer {

    public Map<Method, MethodMetaData> analize(Class<?> proxyClientInterface) {
        Map<Method, MethodMetaData> methodMethodMetaDataMap = new HashMap<>();
        for (Method method : proxyClientInterface.getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            methodMethodMetaDataMap.putIfAbsent(method, MethodMetaDataExtractor.extractMetaData(method));
        }
        return methodMethodMetaDataMap;
    }
}
