package com.core.controller;

import com.core.jms.JmsConsumerResourceStatus;
import com.core.jms.rabbitmq.RabbitMQChecker;
import com.middleware.jms.core.JmsResources;
import com.middleware.jms.core.resource.consumer.JmsConsumerResource;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.stream.Collectors;

@RestController
@RequestMapping(JmsController.BASE_MAPPING)
@OpenAPIDefinition(info = @Info(title = "JMS Controller"))
public class JmsController extends CommonsController {

    public static final String BASE_MAPPING = "/jms";

    @Autowired(required = false)
    private JmsResources jmsResources;
    @Autowired
    private RabbitMQChecker rabbitMQChecker;

    @GetMapping("/start/{jmsResourceClazz}")
    public ResponseEntity<JmsConsumerResourceStatus> startJMS(
            @PathVariable(name = "jmsResourceClazz") String jmsResourceClazz,
            @RequestParam(value = "force", defaultValue = "false") boolean force) throws Exception {

        JmsConsumerResourceStatus consumerResourceStatus = null;
        if (jmsResources != null) {
            JmsConsumerResource jmsConsumerResource = (JmsConsumerResource) jmsResources
                    .getJmsConsumer(Class.forName(jmsResourceClazz));
            if (jmsConsumerResource != null) {
                jmsConsumerResource.restart(force);
                consumerResourceStatus = JmsConsumerResourceStatus.builder()
                        .clazzName(jmsConsumerResource.getClass().getName())
                        .started(jmsConsumerResource.isStarted()).build();
            }
        }
        return new ResponseEntity(consumerResourceStatus, HttpStatus.OK);
    }

    @GetMapping("/startAll")
    public ResponseEntity<Collection<JmsConsumerResourceStatus>> startAllJMS(
            @RequestParam(value = "force", defaultValue = "false") boolean force) throws Exception {

        if (jmsResources != null) {
            return new ResponseEntity<>(jmsResources.getAllConsumers().stream().map(jmsConsumerResource -> {
                jmsConsumerResource.restart(force);
                return JmsConsumerResourceStatus.builder()
                        .clazzName(jmsConsumerResource.getClass().getName())
                        .started(jmsConsumerResource.isStarted()).build();
            }).collect(Collectors.toSet()), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/stop/{jmsResourceClazz}")
    public ResponseEntity<JmsConsumerResourceStatus> stopJMS(
            @PathVariable(name = "jmsResourceClazz") String jmsResourceClazz,
            @RequestParam(value = "force", defaultValue = "false") boolean force) throws Exception {

        JmsConsumerResourceStatus consumerResourceStatus = null;
        if (jmsResources != null) {
            JmsConsumerResource jmsConsumerResource = (JmsConsumerResource) jmsResources
                    .getJmsConsumer(Class.forName(jmsResourceClazz));
            if (jmsConsumerResource != null) {
                jmsConsumerResource.stop(force);
                consumerResourceStatus = JmsConsumerResourceStatus.builder()
                        .clazzName(jmsConsumerResource.getClass().getName())
                        .started(jmsConsumerResource.isStarted()).build();
            }
        }
        return new ResponseEntity<>(consumerResourceStatus, HttpStatus.OK);
    }

    @GetMapping("/list")
    public ResponseEntity<Collection<JmsConsumerResourceStatus>> listJMSConsumersResourceStatus() throws Exception {

        Collection collectionConsumers = null;
        if (jmsResources != null) {
            collectionConsumers = jmsResources.getAllConsumers().stream().map(jmsConsumerResource -> {
                return JmsConsumerResourceStatus.builder()
                        .clazzName(jmsConsumerResource.getClass().getName())
                        .started(jmsConsumerResource.isStarted()).build();
            }).collect(Collectors.toSet());
        }
        return new ResponseEntity<>(collectionConsumers, HttpStatus.OK);
    }

    @GetMapping("/rabbitmq/consumers")
    public void checkRabbitMQConsumers() throws Exception {

        rabbitMQChecker.checkConsumers();
    }

}
