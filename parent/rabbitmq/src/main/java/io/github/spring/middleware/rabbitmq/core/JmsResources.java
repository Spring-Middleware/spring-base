package io.github.spring.middleware.rabbitmq.core;

import io.github.spring.middleware.rabbitmq.core.resource.JmsResource;
import io.github.spring.middleware.rabbitmq.core.resource.consumer.JmsConsumerResource;
import io.github.spring.middleware.rabbitmq.core.resource.handler.JmsResourceErrorHandler;
import io.github.spring.middleware.rabbitmq.core.resource.listener.JmsResourceListener;
import io.github.spring.middleware.rabbitmq.core.resource.producer.JmsProducerResource;
import jakarta.jms.JMSException;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class JmsResources {

    private static Logger logger = LoggerFactory.getLogger(JmsResources.class);
    private Map<Class<JmsConsumerResource>, List<JmsConsumerResource>> consumerResourceMap = new HashMap<>();
    private Map<Class<JmsProducerResource>, List<JmsProducerResource>> producerResourceMap = new HashMap<>();
    private Collection<JmsResourceListener> jmsResourceListeners = new HashSet<>();
    private Collection<JmsResourceErrorHandler> jmsResourceErrorHandlers = new HashSet<>();


    public JmsResources() {
    }

    public void addConsumers(Collection<JmsConsumerResource> jmsConsumerResources) {
        jmsConsumerResources.stream().forEach(jmsConsumerResource -> {
            List<JmsConsumerResource> consumerResources = consumerResourceMap.get((Class) jmsConsumerResource.getClass());
            if (consumerResources == null) {
                consumerResources = new ArrayList<>();
            }
            consumerResources.add(jmsConsumerResource);
            consumerResourceMap.put((Class) jmsConsumerResource.getClass(), consumerResources);
        });
    }

    public void addProducers(List<JmsProducerResource> jmsProducerResources) {
        jmsProducerResources.stream().forEach(jmsProducerResource -> {
            List<JmsProducerResource> producerResources = producerResourceMap.get((Class) jmsProducerResource.getClass());
            if (producerResources == null) {
                producerResources = new ArrayList<>();
            }
            producerResources.add(jmsProducerResource);
            producerResourceMap.put((Class) jmsProducerResource.getClass(), producerResources);
        });
    }

    public Set<JmsProducerResource> getAllProducers() {
        Set<JmsProducerResource> producers = new HashSet<>();
        producerResourceMap.entrySet().stream().forEach(e -> producers.addAll(e.getValue()));
        return producers;
    }

    public Set<JmsConsumerResource> getAllConsumers() {
        Set<JmsConsumerResource> consumers = new HashSet<>();
        consumerResourceMap.entrySet().stream().forEach(e -> consumers.addAll(e.getValue()));
        return consumers;
    }

    public <T> T getJmsConsumer(Class<T> clazz) {
        return getJmsConsumer(clazz, 0);
    }


    public <T extends JmsConsumerResource> List<T> getJmsConsumers(Class<T> clazz) {
        return (List) consumerResourceMap.get(clazz);
    }

    public <T> T getJmsConsumer(Class<T> clazz, int id) {
        List<JmsConsumerResource> consumerResources = consumerResourceMap.get(clazz);
        return (T) CollectionUtils.emptyIfNull(consumerResources).stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

    public <T> T getJmsProducer(Class<T> clazz) {
        return getJmsProducer(clazz, null);
    }

    public <T> T getJmsProducer(Class<T> clazz, String routingKey) {
        List<JmsProducerResource> producerResources = producerResourceMap.get(clazz);
        return (T) CollectionUtils.emptyIfNull(producerResources).stream()
                .filter(p -> p.getRoutingKey().equals(Optional.ofNullable(routingKey).orElse(p.getJmsResourceDestination().getDestinationName())))
                .findFirst().orElse(null);
    }

    public <T> List<T> getJmsProducers(Class<T> clazz) {
        return (List) producerResourceMap.get(clazz);
    }


    public <T extends JmsResource> Collection<T> getJmsResources() {
        Collection<JmsResource> jmsResources = new HashSet<>();
        jmsResources.addAll(getAllProducers());
        jmsResources.addAll(getAllConsumers());
        return (Collection) jmsResources;
    }


    public <T extends JmsConsumerResource> void start(Class<T> clazz) {
        List<JmsConsumerResource> consumerResources = consumerResourceMap.get(clazz);
        CollectionUtils.emptyIfNull(consumerResources).stream().forEach(c -> c.start(false));
    }

    public <T extends JmsConsumerResource> void start() {
        CollectionUtils.emptyIfNull(getAllConsumers()).stream().forEach(c -> c.start(false));
    }

    public void close() throws JMSException {
        for (JmsConsumerResource jmsConsumerResource : getAllConsumers()) {
            jmsConsumerResource.stop(false);
        }
    }

    public Map<Class<JmsConsumerResource>, List<JmsConsumerResource>> getConsumerResources() {
        return consumerResourceMap;
    }

    public Map<Class<JmsProducerResource>, List<JmsProducerResource>> getProducerResources() {
        return producerResourceMap;
    }

    public Collection<JmsResourceListener> getJmsResourceListeners() {
        return jmsResourceListeners;
    }

    public void setJmsResourceListeners(Collection<JmsResourceListener> jmsResourceListeners) {
        this.jmsResourceListeners = jmsResourceListeners;
    }

    public Collection<JmsResourceErrorHandler> getJmsResourceErrorHandlers() {

        return jmsResourceErrorHandlers;
    }

    public void setJmsResourceErrorHandlers(
            Collection<JmsResourceErrorHandler> jmsResourceErrorHandlers) {

        this.jmsResourceErrorHandlers = jmsResourceErrorHandlers;
    }
}
