package io.github.spring.middleware.kafka.core.registrar;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.github.spring.middleware.kafka.api.annotations.MiddlewareKafkaListener;
import io.github.spring.middleware.kafka.api.data.EventEnvelope;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.reflect.Method;
import java.util.Map;

public class EnableMiddlewareKafkaListenersRegistrar
        implements ImportBeanDefinitionRegistrar {

    private static final Logger log = LoggerFactory.getLogger(EnableMiddlewareKafkaListenersRegistrar.class);

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        Map<String, Object> attrs =
                metadata.getAnnotationAttributes(EnableMiddlewareKafkaListeners.class.getName());

        String[] basePackages = (String[]) attrs.get("basePackages");

        log.info("Scanning for @MiddlewareClient in packages: {}", (Object) basePackages);

        KafkaListenerMetadataRegistry metadataRegistry = new KafkaListenerMetadataRegistry();

        for (String basePackage : basePackages) {
            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .forPackages(basePackage)
                    .addScanners(new MethodAnnotationsScanner()));
            reflections.getMethodsAnnotatedWith(MiddlewareKafkaListener.class).stream().forEach(method -> {
                MiddlewareKafkaListener annotation = method.getAnnotation(MiddlewareKafkaListener.class);
                log.info("Found @MiddlewareKafkaListener on method: {}.{}",
                        method.getDeclaringClass().getName(),
                        method.getName());

                metadataRegistry.register(
                        new KafkaListenerMethodMetadata(
                                method.getDeclaringClass(),
                                resolveEnvelopeJavaType(method),
                                method,
                                annotation.value()
                        )
                );
            });
        }

        BeanDefinitionBuilder builder =
                BeanDefinitionBuilder.genericBeanDefinition(KafkaListenerMetadataRegistry.class,
                        () -> metadataRegistry);

        registry.registerBeanDefinition("middlewareKafkaListenerMetadataRegistry", builder.getBeanDefinition());
    }


    private JavaType resolveEnvelopeJavaType(Method method) {
        return TypeFactory.defaultInstance()
                .constructType(method.getGenericParameterTypes()[0]);
    }

}
