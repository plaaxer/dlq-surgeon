package dev.plaaxer.dlqsurgeon.surgeon;

import dev.plaaxer.dlqsurgeon.cli.ConnectOptions;
import dev.plaaxer.dlqsurgeon.client.ManagementClient;
import dev.plaaxer.dlqsurgeon.model.DeadLetteredMessage;

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

    private final ManagementClient managementClient;

    public MessageFetcher(ConnectOptions opts) {
        this.managementClient = new ManagementClient(opts);
    }

    /**
     * Fetches up to {@code count} messages from {@code queueName}.
     *
     * @return An ordered list of dead-lettered messages; empty if the queue is empty.
     * @throws Exception on network or parsing errors.
     *
     * TODO: Implement by delegating to managementClient.fetchMessages().
     *       Number the returned messages sequentially (1-based) for display.
     *       Log a warning if count > 100 — large fetches with requeue=true can
     *       briefly starve other consumers.
     */
    public List<DeadLetteredMessage> fetch(String queueName, int count) throws Exception {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
