package io.github.spring.middleware.register.resource;

import io.github.spring.middleware.annotation.Register;
import io.github.spring.middleware.client.RegistryClient;
import io.github.spring.middleware.component.NodeInfoRetriever;
import io.github.spring.middleware.registry.model.PublicServer;
import io.github.spring.middleware.registry.params.ResourceRegisterParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceRegisterTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ResourceRegisterTask.class);

    private final RegistryClient registryClient;
    private final Class<?> clazz;
    private final ResourceRegister resourceRegister;
    private final NodeInfoRetriever nodeInfoRetriever;

    public ResourceRegisterTask(RegistryClient registryClient, Class<?> clazz, ResourceRegister resourceRegister, NodeInfoRetriever nodeInfoRetriever) {
        this.registryClient = registryClient;
        this.clazz = clazz;
        this.resourceRegister = resourceRegister;
        this.nodeInfoRetriever = nodeInfoRetriever;
    }

    @Override
    public void run() {
        try {
            Register register = clazz.getAnnotation(Register.class);
            ResourceRegisterParameters params = buildParameters(register);

            // Debug: log parameters that will be sent
            if (logger.isDebugEnabled()) {
                logger.debug("Registering resource with params: resource={}, cluster={}, node={}, port={}, path={}, contextPath={}, publicServer={}",
                        params.getResourceName(), params.getCluster(), params.getNode(), params.getPort(), params.getPath(), resourceRegister.getContextPath(), params.getPublicServer());
            }

            registryClient.registerResource(params); // llama al client proxy
            logger.info("Registered resource {} at {}:{}{} node: {}",
                    register.name(),
                    resourceRegister.getClusterName(),
                    resourceRegister.getPort(),
                    params.getPath(),
                    nodeInfoRetriever.getAddress()
            );
        } catch (Exception ex) {
            logger.error("Error registering resource: {}", clazz.getSimpleName(), ex);
        } finally {
            resourceRegister.remove(this);
        }
    }

    private ResourceRegisterParameters buildParameters(Register register) throws Exception {
        ResourceRegisterParameters params = new ResourceRegisterParameters();
        params.setResourceName(register.name());
        params.setCluster(resourceRegister.getClusterName());
        params.setPort(resourceRegister.getPort());
        params.setNode(nodeInfoRetriever.getAddress());
        params.setNodeId(nodeInfoRetriever.getNodeId());
        String path = register.path();
        if (path == null) path = "/";
        if (!path.startsWith("/")) path = STR."/\{path}";
        params.setPath(path);
        params.setContextPath(resourceRegister.getContextPath());
        // Avoid sending the Spring-managed bean instance (may be proxied). Copy values into a plain PublicServer.
        PublicServer configured = resourceRegister.getPublicServer();
        if (configured != null) {
            PublicServer plain = new PublicServer(configured.host(), configured.port(), configured.ssl());
            params.setPublicServer(plain);
        }
        return params;
    }

    public String getResourceName() {
        return clazz.getAnnotation(Register.class).name();
    }
}
