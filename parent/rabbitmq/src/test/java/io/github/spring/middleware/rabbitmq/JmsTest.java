package io.github.spring.middleware.rabbitmq;

import io.github.spring.middleware.rabbitmq.configuration.JmsConnectionConfiguration;
import io.github.spring.middleware.rabbitmq.configuration.JmsConnectionCredentials;
import io.github.spring.middleware.rabbitmq.configuration.JmsConnectionPoolConfiguration;
import io.github.spring.middleware.rabbitmq.core.JmsResources;
import io.github.spring.middleware.rabbitmq.message.TestingMessage;
import io.github.spring.middleware.rabbitmq.resources.queue.durable.JmsConsumerQueueDurable;
import io.github.spring.middleware.rabbitmq.resources.queue.durable.JmsProducerQueueDurable;
import io.github.spring.middleware.rabbitmq.resources.queue.transients.JmsConsumerQueueTransient;
import io.github.spring.middleware.rabbitmq.resources.queue.transients.JmsProducerQueueTransient;
import io.github.spring.middleware.rabbitmq.resources.topic.JmsConsumerES;
import io.github.spring.middleware.rabbitmq.resources.topic.JmsConsumerNews;
import io.github.spring.middleware.rabbitmq.resources.topic.JmsConsumerUK;
import io.github.spring.middleware.rabbitmq.resources.topic.JmsProducerNewsWorld;
import jakarta.jms.JMSException;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.fail;

public class JmsTest {

    private Logger logger = LoggerFactory.getLogger(JmsTest.class);

    private JmsResources jmsResources;
    private JmsProducerQueueTransient jmsProducerQueueTransient;
    private JmsConsumerQueueTransient jmsConsumerQueueTransient;

    private JmsProducerQueueDurable jmsProducerQueueDurable;
    private JmsConsumerQueueDurable jmsConsumerQueueDurable;

    // Esto levanta RabbitMQ automáticamente antes de los tests
    @ClassRule
    public static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("definitions.json"),
                    "/etc/rabbitmq/definitions.json"
            )
            // Le decimos a Rabbit que use este archivo de definiciones al arrancar
            .withEnv("RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS", "-rabbitmq_management load_definitions \"/etc/rabbitmq/definitions.json\"")
            .withAdminPassword("admin")
            .withUser("admin", "admin", Set.of("administrator"))
            .waitingFor(Wait.forLogMessage(".*Server startup complete.*\\n", 1));




    @Before
    public void init() throws Exception {
        // 1. EXTRAER HOST Y PUERTO DINÁMICO DEL CONTENEDOR
        String host = rabbit.getHost();
        Integer port = rabbit.getAmqpPort(); // El puerto que Docker ha asignado al 5672

        String rabbitUrl = "amqp://" + host + ":" + port;
        logger.info("Conectando Middleware a RabbitMQ en: {}", rabbitUrl);

        JmsConnectionConfiguration jmsConnectionConfiguration = new JmsConnectionConfiguration();
        jmsConnectionConfiguration.setTcpHost(rabbitUrl);
        JmsConnectionCredentials jmsConnectionCredentials = new JmsConnectionCredentials();
        jmsConnectionCredentials.setUsername("admin");
        jmsConnectionCredentials.setPassword("admin");

        JmsConnectionPoolConfiguration jmsConnectionPoolConfiguration = new JmsConnectionPoolConfiguration();
        jmsConnectionPoolConfiguration.setMinIdle(1);
        jmsConnectionPoolConfiguration.setMaxIdle(5);
        jmsConnectionPoolConfiguration.setMaxTotal(10);
        jmsConnectionConfiguration.setJmsConnectionPoolConfiguration(jmsConnectionPoolConfiguration);
        jmsConnectionConfiguration.setJmsConnectionCredentials(jmsConnectionCredentials);

        JmsFactory jmsFactory = JmsFactory.newInstance();
        jmsResources = jmsFactory.createJmsResources(Arrays.asList("io.github.spring.middleware.rabbitmq.resources"), jmsConnectionConfiguration);
        jmsProducerQueueTransient = jmsResources.getJmsProducer(JmsProducerQueueTransient.class);
        jmsConsumerQueueTransient = jmsResources.getJmsConsumer(JmsConsumerQueueTransient.class);
        jmsProducerQueueDurable = jmsResources.getJmsProducer(JmsProducerQueueDurable.class);
        jmsConsumerQueueDurable = jmsResources.getJmsConsumer(JmsConsumerQueueDurable.class);

        // El puerto 15672 es el estándar del Management Plugin
        String dashboardUrl = "http://" + rabbit.getHost() + ":" + rabbit.getMappedPort(15672);
        logger.info("DASHBOARD RABBITMQ: " + dashboardUrl);
    }

    @After
    public void shutdown() throws JMSException {
        jmsResources.close();
    }

    @Test
    public void sendAndReceiveMessageToTestQueueTransient() throws Exception {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        jmsResources.getJmsConsumers(JmsConsumerQueueTransient.class).stream().forEach(c -> c.setAtomicInteger(atomicInteger));
        jmsResources.start(JmsConsumerQueueTransient.class);
        for (int i = 0; i < 4; i++) {
            final int iMessage = i;
            CompletableFuture.runAsync(() -> {
                TestingMessage testMessaage = new TestingMessage();
                try {
                    testMessaage.setMessage("Hola Mundo! (Queue Transient)");
                    testMessaage.setId(iMessage);
                    jmsProducerQueueTransient.send(testMessaage);
                } catch (Exception ex) {
                    logger.error("Error sending message " + testMessaage.getId());
                }
            }).get();
        }
        jmsResources.getJmsConsumers(JmsConsumerQueueTransient.class).stream().forEach(c -> {
            try {
                c.waitUntilMessageReceived(4);
            } catch (Exception ex) {
                fail(ex.getMessage());
            }
        });
    }

    @Test
    public void sendAndReceiveMessageToTestQueueDurable() throws Exception {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        jmsResources.getJmsConsumers(JmsConsumerQueueDurable.class).stream().forEach(c -> c.setAtomicInteger(atomicInteger));
        jmsResources.start(JmsConsumerQueueDurable.class);
        for (int i = 0; i < 3; i++) {
            final int iMessage = i;
            CompletableFuture.runAsync(() -> {
                TestingMessage testMessaage = new TestingMessage();
                try {
                    testMessaage.setMessage("Hola Mundo! (Queue Durable)");
                    testMessaage.setId(iMessage);
                    jmsProducerQueueDurable.send(testMessaage);
                } catch (Exception ex) {
                    logger.error("Error sending message " + testMessaage.getId());
                }
            }).get();
        }
        jmsResources.getJmsConsumers(JmsConsumerQueueDurable.class).stream().forEach(c -> {
            try {
                c.waitUntilMessageReceived(3);
            } catch (Exception ex) {
                fail(ex.getMessage());
            }
        });
    }


    @Test
    public void sendAndReceiveMessageToTestTopic() throws Exception {
        TestingMessage news = new TestingMessage();
        news.setMessage("News");

        jmsResources.getJmsConsumer(JmsConsumerES.class).start(false);
        jmsResources.getJmsConsumer(JmsConsumerUK.class).start(false);
        jmsResources.getJmsConsumer(JmsConsumerNews.class).start(false);
        logger.info("Sending message " + news.getMessage());
        jmsResources.getJmsProducers(JmsProducerNewsWorld.class).stream().forEach(p -> {
            try {
                p.send(news);
            } catch (Exception ex) {
                logger.error("Error sending message", ex);
            }
        });

        jmsResources.getJmsConsumer(JmsConsumerES.class).waitUntilMessageReceived(1);
        jmsResources.getJmsConsumer(JmsConsumerUK.class).waitUntilMessageReceived(1);
        jmsResources.getJmsConsumer(JmsConsumerNews.class).waitUntilMessageReceived(2);
    }


}
