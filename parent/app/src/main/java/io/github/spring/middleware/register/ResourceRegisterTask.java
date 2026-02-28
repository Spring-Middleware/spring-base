package io.github.spring.middleware.register;

import io.github.spring.middleware.annotations.Register;
import io.github.spring.middleware.client.RegistryClient;
import io.github.spring.middleware.registry.model.PublicServer;
import io.github.spring.middleware.registry.params.ResourceRegisterParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

public class ResourceRegisterTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ResourceRegisterTask.class);

    private final RegistryClient registryClient;
    private final Class<?> clazz;
    private final ResourceRegister resourceRegister;

    public ResourceRegisterTask(RegistryClient registryClient, Class<?> clazz, ResourceRegister resourceRegister) {
        this.registryClient = registryClient;
        this.clazz = clazz;
        this.resourceRegister = resourceRegister;
    }

    @Override
    public void run() {
        try {
            Register register = clazz.getAnnotation(Register.class);
            ResourceRegisterParameters params = buildParameters(register);

            // Debug: log parameters that will be sent
            if (logger.isDebugEnabled()) {
                logger.debug("Registering resource with params: resource={}, cluster={}, node={}, port={}, path={}, publicServer={}",
                        params.getResourceName(), params.getCluster(), params.getNode(), params.getPort(), params.getPath(), params.getPublicServer());
            }

            registryClient.registerResource(params); // llama al client proxy
            logger.info("Registered resource {} at {}:{}{} node: {}",
                    register.name(),
                    resourceRegister.getClusterName(),
                    resourceRegister.getPort(),
                    params.getPath(),
                    InetAddress.getLocalHost().getHostName()
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
        params.setNode(InetAddress.getLocalHost().getHostAddress());
        String path = register.name();
        if (path == null) path = "/";
        if (!path.startsWith("/")) path = "/" + path;
        params.setPath(path);
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
