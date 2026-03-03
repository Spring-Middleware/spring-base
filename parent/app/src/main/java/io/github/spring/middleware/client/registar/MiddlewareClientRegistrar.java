package io.github.spring.middleware.client.registar;

import io.github.spring.middleware.annotation.MiddlewareContract;
import io.github.spring.middleware.annotations.EnableMiddlewareClients;
import io.github.spring.middleware.client.proxy.ProxyClient;
import io.github.spring.middleware.client.proxy.ProxyClientRegistry;
import org.jspecify.annotations.NonNull;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;

import java.beans.Introspector;
import java.util.Map;
import java.util.Set;

public class MiddlewareClientRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    private ResourceLoader resourceLoader;

    private static final Logger log = LoggerFactory.getLogger(MiddlewareClientRegistrar.class);

    @Override
    public void registerBeanDefinitions(
            AnnotationMetadata metadata,
            BeanDefinitionRegistry registry) {

        Map<String, Object> attrs =
                metadata.getAnnotationAttributes(EnableMiddlewareClients.class.getName());

        String[] basePackages = (String[]) attrs.get("basePackages");

        log.info("Scanning for @MiddlewareClient in packages: {}", (Object) basePackages);

        for (String basePackage : basePackages) {
            Reflections reflections = new Reflections(basePackage);
            log.debug("Scanning base package: {}", basePackage);
            for (Class<?> candidate : reflections.getTypesAnnotatedWith(MiddlewareContract.class)) {
                String className = candidate.getName();
                log.debug("Found candidate: {}", className);
                try {
                    Class<?> clazz = Class.forName(className);
                    if (!clazz.isInterface()) {
                        log.info("Skipping {} because it's not an interface", className);
                        continue;
                    }

                    // 2) Si ya existe una implementación (server-side), NO registrar proxy
                    //    (Esto cubre el caso: Controller implements CatalogsApi)
                    Set<?> implementations = reflections.getSubTypesOf((Class<Object>) clazz);
                    if (implementations != null && !implementations.isEmpty()) {
                        // Puedes afinar el filtro si quieres (ej: solo si alguna es @RestController)
                        log.info(
                                "Skipping proxy registration for {} because implementations were found: {}",
                                clazz.getName(),
                                implementations.stream().limit(5).toList()
                        );
                        continue;
                    }

                    // 3) Evita duplicados por nombre
                    String beanName = Introspector.decapitalize(clazz.getSimpleName());
                    if (registry.containsBeanDefinition(beanName)) {
                        log.info(
                                "Skipping proxy registration for {} because bean name '{}' is already registered",
                                clazz.getName(),
                                beanName
                        );
                        continue;
                    }

                    MiddlewareContract clientAnnotation = clazz.getAnnotation(MiddlewareContract.class);
                    BeanDefinitionBuilder builder =
                            BeanDefinitionBuilder.genericBeanDefinition(clazz);
                    AbstractBeanDefinition beanDefinition = getAbstractBeanDefinition(builder, clazz, clientAnnotation);
                    log.info("Registering MiddlewareClient bean: {} -> {}", beanName, className);
                    registry.registerBeanDefinition(beanName, beanDefinition);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create proxy for " + className, e);
                }
            }
        }
    }

    private static @NonNull AbstractBeanDefinition getAbstractBeanDefinition(BeanDefinitionBuilder builder, Class<?> clazz, MiddlewareContract clientAnnotation) {
        AbstractBeanDefinition beanDefinition =
                builder.getBeanDefinition();
        beanDefinition.setInstanceSupplier(() -> {
            ProxyClient<?> proxyClient = new ProxyClient<>(clazz, clientAnnotation.timeout());
            ProxyClientRegistry.add(proxyClient);
            try {
                return proxyClient.wrappedInstance();
            } catch (Exception e) {
                throw new RegistarClientException("Error registering client " + Introspector.decapitalize(clazz.getSimpleName()), e);
            }
        });
        return beanDefinition;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
