package io.github.spring.middleware.register.graphql;

import io.github.spring.middleware.component.NodeInfoRetriever;
import io.github.spring.middleware.graphql.annotations.GraphQLEndpoint;
import io.github.spring.middleware.provider.ServerPortProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.UnknownHostException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class GraphQLAutoRegistrar implements ApplicationListener<ApplicationReadyEvent> {

    private final ObjectProvider<GraphQLSchemaRegister> graphQLSchemaRegisterProvider;
    private final ServerPortProvider serverPortProvider;
    private final NodeInfoRetriever nodeInfoRetriever;

    private volatile Set<Class<?>> schemasToRegister = Set.of();
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public GraphQLAutoRegistrar(
            ObjectProvider<GraphQLSchemaRegister> graphQLSchemaRegisterProvider,
            ServerPortProvider serverPortProvider,
            NodeInfoRetriever nodeInfoRetriever) {
        this.graphQLSchemaRegisterProvider = graphQLSchemaRegisterProvider;
        this.serverPortProvider = serverPortProvider;
        this.nodeInfoRetriever = nodeInfoRetriever;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        GraphQLSchemaRegister graphQLSchemaRegister = graphQLSchemaRegisterProvider.getIfAvailable();
        if (graphQLSchemaRegister == null) {
            return;
        }

        if (event.getApplicationContext().getParent() != null) return;
        if (!initialized.compareAndSet(false, true)) return;

        this.schemasToRegister = event.getApplicationContext()
                .getBeansWithAnnotation(GraphQLEndpoint.class)
                .values().stream()
                .map(AopUtils::getTargetClass)
                .collect(Collectors.toUnmodifiableSet());

        if (schemasToRegister.isEmpty()) {
            log.info("No GraphQL schemas annotated with @GraphQLEndpoint were found to register");
            return;
        }

        log.info("Discovered {} GraphQL schema endpoints: {}",
                schemasToRegister.size(),
                schemasToRegister.stream().map(Class::getSimpleName).sorted().collect(Collectors.joining(", ")));

        graphQLSchemaRegister.register(schemasToRegister);
    }

    public String getSchemaLocationNodeName() throws UnknownHostException {
        return STR."\{nodeInfoRetriever.getAddress()}:\{this.serverPortProvider.getPort()}";
    }

    public void reRegister() {
        GraphQLSchemaRegister graphQLSchemaRegister = graphQLSchemaRegisterProvider.getIfAvailable();
        if (graphQLSchemaRegister == null) {
            return;
        }

        if (schemasToRegister.isEmpty()) return;
        graphQLSchemaRegister.register(schemasToRegister);
    }

    public Set<Class<?>> getSchemasToRegister() {
        return schemasToRegister;
    }
}