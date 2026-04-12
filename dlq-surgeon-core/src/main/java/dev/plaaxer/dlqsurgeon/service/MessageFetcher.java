package dev.plaaxer.dlqsurgeon.service;

import dev.plaaxer.dlqsurgeon.client.ManagementClient;
import dev.plaaxer.dlqsurgeon.config.ConnectionConfig;
import dev.plaaxer.dlqsurgeon.model.QueueInfo;
import dev.plaaxer.dlqsurgeon.model.RabbitMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Facade over {@link ManagementClient} for fetching queue and message data.
 *
 * Pure data retrieval — no display logic. Presentation is handled by
 * {@code MessagePresenter} in the CLI module.
 *
 * Messages are fetched via the Management HTTP API with requeue=true (that's the intended design).
 * They are NOT consumed in the AMQP sense; they return to the queue immediately.
 */
public class MessageFetcher {

    private static final Logger log = LoggerFactory.getLogger(MessageFetcher.class);

    private final ManagementClient managementClient;

    public MessageFetcher(ConnectionConfig config) throws Exception {
        this.managementClient = new ManagementClient(config);
    }

    /**
     * Fetches up to {@code count} messages from {@code queueName}.
     *
     * @return An ordered list of messages; empty if the queue is empty.
     * @throws Exception on network or parsing errors.
     */
    public List<RabbitMessage> fetch(String queueName, int count) throws Exception {
        if (count > 100) {
            log.warn("Fetching {} messages with requeue=true — this may briefly starve other consumers", count);
        }
        return managementClient.fetchMessages(queueName, count);
    }

    public List<QueueInfo> listQueues() throws Exception {
        return managementClient.listQueues();
    }

    public QueueInfo getQueue(String queueName) throws Exception {
        return managementClient.getQueue(queueName);
    }
}
