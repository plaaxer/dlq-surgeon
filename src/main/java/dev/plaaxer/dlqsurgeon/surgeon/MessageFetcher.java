package dev.plaaxer.dlqsurgeon.surgeon;

import dev.plaaxer.dlqsurgeon.cli.ConnectOptions;
import dev.plaaxer.dlqsurgeon.client.ManagementClient;
import dev.plaaxer.dlqsurgeon.model.RabbitMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Fetches messages from a DLQ and holds them in memory for the duration of a session.
 *
 * Design contract:
 *   - Messages are fetched via the Management HTTP API with requeue=true.
 *     They are NOT consumed in the AMQP sense; they return to the queue immediately.
 *   - The in-memory list is the only mutable state in this class.
 *   - Messages are never written to disk — only to the tmp file opened by PayloadEditor,
 *     which is cleaned up after the editor exits.
 *
 * Thread safety: Not thread-safe. Use one instance per command invocation.
 */
public class MessageFetcher {

    private static final Logger log = LoggerFactory.getLogger(MessageFetcher.class);

    private final ManagementClient managementClient;

    public MessageFetcher(ConnectOptions opts) {
        this.managementClient = new ManagementClient(opts);
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
}
