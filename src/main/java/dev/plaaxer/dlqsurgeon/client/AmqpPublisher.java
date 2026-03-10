package dev.plaaxer.dlqsurgeon.client;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import dev.plaaxer.dlqsurgeon.cli.ConnectOptions;
import dev.plaaxer.dlqsurgeon.model.RabbitMessage;
import dev.plaaxer.dlqsurgeon.model.RepairPlan;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

        // enable publisher confirms. waitForConfirmsOrDie() in publish() will block
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
     */
    public void publish(RepairPlan plan) throws IOException, InterruptedException, TimeoutException {
        byte[] body = plan.editedPayload().getBytes(StandardCharsets.UTF_8);
        channel.basicPublish(plan.targetExchange(), plan.targetRoutingKey(), plan.properties(), body);
        channel.waitForConfirmsOrDie(5_000);

    }

    /**
     * Fetches the message at the head of {@code queue}, verifies it matches {@code source}
     * by message-id (or payload if message-id is absent), then acks it to remove it from
     * the queue. If the head message does not match, it is nacked back (requeued) and an
     * exception is thrown — nothing is deleted.
     *
     * Must be called only after a successful publish confirm. Uses the same connection
     * and channel as the publisher to avoid opening a second AMQP connection.
     */
    public void deleteFromDlq(String queue, RabbitMessage source) throws IOException {
        GetResponse response = channel.basicGet(queue, false); // false = manual ack
        if (response == null) {
            throw new IOException(
                    "Queue '" + queue + "' was empty when attempting to delete source message. " +
                    "The re-injected message is in the broker, so manual cleanup of the DLQ may be needed.");
        }

        String fetchedMessageId = response.getProps().getMessageId();
        String fetchedBody = new String(response.getBody(), StandardCharsets.UTF_8);

        boolean idMatch = source.messageId() != null && source.messageId().equals(fetchedMessageId);
        boolean bodyMatch = fetchedBody.equals(source.payload());

        if (!idMatch && !bodyMatch) {
            channel.basicNack(response.getEnvelope().getDeliveryTag(), false, true); // requeue
            throw new IOException(
                    "Message at head of '" + queue + "' does not match the source (different message-id/payload). " +
                    "A new message may have arrived in the DLQ. The source message was NOT deleted - manual cleanup required.");
        }

        channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
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
