package dev.plaaxer.dlqsurgeon.surgeon;

import dev.plaaxer.dlqsurgeon.cli.ConnectOptions;
import dev.plaaxer.dlqsurgeon.client.ManagementClient;
import dev.plaaxer.dlqsurgeon.model.QueueInfo;
import dev.plaaxer.dlqsurgeon.model.RabbitMessage;
import dev.plaaxer.dlqsurgeon.model.XDeathEntry;
import dev.plaaxer.dlqsurgeon.tui.Console;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Facade over ManagementClient for fetching queue and message data,
 * and formatting results to the console.
 *
 * Design contract:
 *   - Messages are fetched via the Management HTTP API with requeue=true.
 *     They are NOT consumed in the AMQP sense; they return to the queue immediately.
 *   - Messages are never written to disk — only to the tmp file opened by PayloadEditor,
 *     which is cleaned up after the editor exits.
 *
 * Thread safety: Not thread-safe. Use one instance per command invocation.
 */
public class MessageFetcher {

    private static final Logger log = LoggerFactory.getLogger(MessageFetcher.class);
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final String HR = "─".repeat(70);

    private final ManagementClient managementClient;

    public MessageFetcher(ConnectOptions opts) {
        this.managementClient = new ManagementClient(opts);
    }

    // ── Fetching ─────────────────────────────────────────────────────────────

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

    // ── Printing ─────────────────────────────────────────────────────────────

    public void printQueueList(List<QueueInfo> queues) {
        Console.dim(String.format("%-50s  %5s  %s", "QUEUE", "MSGS", "TYPE"));
        Console.dim(HR);
        for (QueueInfo q : queues) {
            printQueue(q);
        }
    }

    public void printQueue(QueueInfo queue) {
        String line = queue.label();
        if (queue.messageCount() > 0) {
            Console.error(line);
        } else {
            Console.success(line);
        }
    }

    public void printMessage(RabbitMessage msg) {
        Console.dim(HR);
        Console.info(String.format("  Message    #%d", msg.messageNumber()));
        Console.info(String.format("  Exchange    : %s", msg.exchange()));
        Console.info(String.format("  Routing key : %s", msg.routingKey()));
        if (msg.contentType() != null)   Console.info(String.format("  Content-Type: %s", msg.contentType()));
        if (msg.messageId() != null)     Console.info(String.format("  Message-ID  : %s", msg.messageId()));
        if (msg.correlationId() != null) Console.info(String.format("  Correlation : %s", msg.correlationId()));
        Console.info(String.format("  Redelivered : %s", msg.redelivered()));

        if (!msg.xDeathEntries().isEmpty()) {
            Console.dim("");
            Console.dim(String.format("  x-death (%d):", msg.xDeathEntries().size()));
            for (XDeathEntry e : msg.xDeathEntries()) {
                String time = e.time() > 0
                        ? TS_FMT.format(Instant.ofEpochSecond(e.time()))
                        : "unknown";
                Console.info(String.format("    %-30s  reason=%-15s  count=%d  %s",
                        e.queue(), e.reason(), e.count(), time));
            }
        }

        Map<String, Object> userHeaders = new java.util.LinkedHashMap<>(msg.headers());
        userHeaders.keySet().removeIf(k -> k.startsWith("x-death") || k.startsWith("x-first-death"));
        if (!userHeaders.isEmpty()) {
            Console.dim("");
            Console.dim("  Headers:");
            userHeaders.forEach((k, v) -> Console.info(String.format("    %s: %s", k, v)));
        }

        Console.dim("");
        Console.dim("  Payload:");
        Console.info(msg.payload());
        Console.dim(HR);
    }
}
