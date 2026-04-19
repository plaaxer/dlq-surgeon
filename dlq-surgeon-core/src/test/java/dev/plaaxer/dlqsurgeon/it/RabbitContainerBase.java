package dev.plaaxer.dlqsurgeon.it;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import dev.plaaxer.dlqsurgeon.config.ConnectionConfig;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared RabbitMQ container for all integration tests.
 *
 * The container is started once (static) and reused across all subclasses,
 * keeping the test suite fast. Each test is responsible for cleaning up the
 * queues and exchanges it creates — use {@link #channel()} and close it in
 * a @AfterEach when you need a fresh AMQP channel.
 */
public abstract class RabbitContainerBase {

    static final RabbitMQContainer RABBIT =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"));

    static {
        RABBIT.start();
    }

    /**
     * Builds a {@link ConnectionConfig} wired to the running container.
     */
    protected static ConnectionConfig containerOpts() {
        return new ConnectionConfig(
                RABBIT.getHost(),
                RABBIT.getMappedPort(15672),
                RABBIT.getMappedPort(5672),
                "/",
                RABBIT.getAdminUsername(),
                RABBIT.getAdminPassword().toCharArray(),
                false, null, null, null
        );
    }

    /**
     * Opens a new AMQP {@link Channel} against the container.
     * TODO: maybe caching connections so that each new test doesn't need to open a new one?
     */
    protected static Channel channel() throws Exception {
        ConnectionFactory cf = new ConnectionFactory();
        cf.setHost(RABBIT.getHost());
        cf.setPort(RABBIT.getMappedPort(5672));
        cf.setUsername(RABBIT.getAdminUsername());
        cf.setPassword(RABBIT.getAdminPassword());
        Connection conn = cf.newConnection();
        return conn.createChannel();
    }
}