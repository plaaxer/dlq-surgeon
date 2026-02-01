package dev.plaaxer.dlqsurgeon.client;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import dev.plaaxer.dlqsurgeon.cli.ConnectOptions;
import dev.plaaxer.dlqsurgeon.model.RepairPlan;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Publishes a repaired message back to RabbitMQ via AMQP with publisher confirms.
 *
 * Why AMQP (not the Management HTTP API) for publishing?
 *   The HTTP API's /publish endpoint explicitly states it cannot guarantee delivery
 *   and is "not recommended for production". AMQP with confirms gives us
 *   exactly-once delivery semantics before we delete the source message.
 *
 * Lifecycle: create → publish → close. Do not share across threads.
 *
 * TODO: Add support for mTLS by injecting an SSLContext into ConnectionFactory.
 * TODO: Consider a connection pool if bulk-repair of many messages is needed.
 */
public class AmqpPublisher implements Closeable {

    private final Connection connection;
    private final Channel channel;

    public AmqpPublisher(ConnectOptions opts) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(opts.host);
        factory.setPort(opts.amqpPort);
        factory.setVirtualHost(opts.vhost);
        factory.setUsername(opts.user);
        factory.setPassword(new String(opts.password));

        // TODO: factory.useSslProtocol(sslContext)  when TLS opts are added.

        this.connection = factory.newConnection("dlq-surgeon");
        this.channel = connection.createChannel();

        // Enable publisher confirms. waitForConfirmsOrDie() in publish() will block
        // until the broker acks the message. Only then do we allow deletion from DLQ.
        channel.confirmSelect();
    }

    /**
     * Publishes the repaired message according to the given RepairPlan and waits
     * for a publisher confirm from the broker.
     *
     * @param plan the repaired payload, target exchange, routing key, and properties.
     * @throws IOException      on AMQP channel errors.
     * @throws TimeoutException if the broker does not confirm within the timeout.
     *
     * TODO: Implement this method.
     *   1. Build AMQP.BasicProperties from plan.properties() (preserve original headers,
     *      content-type, delivery-mode, correlation-id, message-id, etc.).
     *      If plan.stripDeathHeaders() is true, remove x-death and x-first-death-* keys.
     *   2. Call channel.basicPublish(exchange, routingKey, properties, bodyBytes).
     *   3. Call channel.waitForConfirmsOrDie(5_000) — throws if broker nacks or times out.
     *      This is the safety gate: if it throws, the caller must NOT delete from DLQ.
     */
    public void publish(RepairPlan plan) throws IOException, InterruptedException, TimeoutException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void close() throws IOException {
        try {
            if (channel != null && channel.isOpen()) channel.close();
        } catch (TimeoutException ignored) {
            // Suppress — we're tearing down.
        } finally {
            if (connection != null && connection.isOpen()) connection.close();
        }
    }
}
