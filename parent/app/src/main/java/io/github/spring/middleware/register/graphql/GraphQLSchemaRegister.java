package io.github.spring.middleware.register.graphql;

import io.github.spring.middleware.client.RegistryClient;
import io.github.spring.middleware.provider.ServerPortProvider;
import io.github.spring.middleware.registry.params.SchemaRegisterParameters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class GraphQLSchemaRegister {

    private final RegistryClient registryClient;
    private final GraphQLRegisterProperties props;
    private final ServerPortProvider serverPortProvider;

    public GraphQLSchemaRegister(final RegistryClient registryClient,
                                 final ServerPortProvider serverPortProvider,
                                 final GraphQLRegisterProperties props) {
        this.registryClient = registryClient;
        this.props = props;
        this.serverPortProvider = serverPortProvider;
    }

    public void register(Set<Class<?>> endpoints) {
        if (!props.isEnabled()) {
            log.info("GraphQL schema register disabled");
            return;
        }

        if (endpoints.isEmpty()) {
            log.info("No @GraphQLEndpoint beans found");
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(endpoints.size(), 4));

        endpoints.forEach(bean ->
                executor.submit(() -> doRegister(bean.getClass()))
        );

        executor.shutdown();
    }

    private void doRegister(Class<?> beanClass) {
        try {
            String path = resolvePath(beanClass); // e.g. "/graphql"
            SchemaRegisterParameters params = buildParameters(path);
            registryClient.registerGraphQLSchemaLocation(params);

            log.info("Schema {} bound to {}:{}{}",
                    props.getNamespace(), props.getClusterName(), this.serverPortProvider.getPort(), path);

        } catch (Exception ex) {
            log.error("Error registering GraphQL endpoint for {}", beanClass.getName(), ex);
        }
    }

    private String resolvePath(Class<?> beanClass) {
        RequestMapping rm = beanClass.getAnnotation(RequestMapping.class);
        if (rm == null || rm.value().length == 0) {
            // fallback: si no hay @RequestMapping en clase, asume /graphql
            return "/graphql";
        }
        return rm.value()[0];
    }

    private SchemaRegisterParameters buildParameters(String path) throws Exception {
        SchemaRegisterParameters p = new SchemaRegisterParameters();
        p.setNamespace(props.getNamespace());
        p.setLocation(STR."\{props.getClusterName()}:\{this.serverPortProvider.getPort()}");
        p.setNodeLocation(STR."\{InetAddress.getLocalHost().getHostAddress()}:\{this.serverPortProvider.getPort()}");
        p.setPathApi(path);
        return p;
    }
}