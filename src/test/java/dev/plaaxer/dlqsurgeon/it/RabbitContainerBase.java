package dev.plaaxer.dlqsurgeon.it;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import dev.plaaxer.dlqsurgeon.cli.ConnectOptions;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared RabbitMQ container for all integration tests.
 *
 * The container is started once (static) and reused across all subclasses,
 * keeping the test suite fast. Each test is responsible for cleaning up the
 * queues and exchanges it creates — use {@link #channel()} and close it in
 * a @AfterEach when you need a fresh AMQP channel.
 *
 * Usage: extend this class (no extra annotations needed — the container is
 * managed manually, not via @Testcontainers, to avoid one-container-per-class).
 */
public abstract class RabbitContainerBase {

    static final RabbitMQContainer RABBIT =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"));

    static {
        RABBIT.start();
    }

    /**
     * Builds a {@link ConnectOptions} wired to the running container.
     */
    protected static ConnectOptions containerOpts() {
        ConnectOptions opts = new ConnectOptions();
        opts.host = RABBIT.getHost();
        opts.managementPort = RABBIT.getMappedPort(15672);
        opts.amqpPort = RABBIT.getMappedPort(5672);
        opts.vhost = "/";
        opts.user = RABBIT.getAdminUsername();
        opts.password = RABBIT.getAdminPassword().toCharArray();
        opts.readOnly = false;
        return opts;
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