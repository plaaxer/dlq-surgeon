package dev.plaaxer.dlqsurgeon.surgeon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
 * Messages are fetched via the Management HTTP API with requeue=true (that's the intended design).
 * They are NOT consumed in the AMQP sense; they return to the queue immediately.
 */
public class MessageFetcher {

    private static final Logger log = LoggerFactory.getLogger(MessageFetcher.class);
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final String HR = "─".repeat(70);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ManagementClient managementClient;

    public MessageFetcher(ConnectOptions opts) throws Exception {
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
        Console.dim(String.format("%-50s  %5s  %3s  %s", "QUEUE", "MSGS", "CON", "TYPE"));
        Console.dim(HR);
        for (QueueInfo q : queues) {
            printQueue(q);
        }
    }

    public void printQueuesJson(List<QueueInfo> queues) {
        ArrayNode array = MAPPER.createArrayNode();
        for (QueueInfo q : queues) {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("name", q.name());
            node.put("messages", q.messageCount());
            node.put("consumers", q.consumers());
            node.put("hasDlx", q.hasDlx());
            array.add(node);
        }
        try {
            Console.info(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(array));
        } catch (Exception e) {
            Console.error("Failed to serialize queues to JSON: " + e.getMessage());
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

    public void printMessagesJson(List<RabbitMessage> messages) {
        ArrayNode array = MAPPER.createArrayNode();
        for (RabbitMessage msg : messages) {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("messageNumber", msg.messageNumber());
            node.put("exchange", msg.exchange());
            node.put("routingKey", msg.routingKey());
            node.put("sourceQueue", msg.sourceQueue());
            if (msg.contentType() != null) node.put("contentType", msg.contentType());
            if (msg.messageId() != null)   node.put("messageId", msg.messageId());
            if (msg.correlationId() != null) node.put("correlationId", msg.correlationId());
            node.put("deliveryMode", msg.deliveryMode());
            node.put("redelivered", msg.redelivered());
            // Embed payload as JSON if valid, otherwise as a plain string
            try {
                node.set("payload", MAPPER.readTree(msg.payload()));
            } catch (Exception e) {
                node.put("payload", msg.payload());
            }
            if (!msg.xDeathEntries().isEmpty()) {
                ArrayNode xdeath = MAPPER.createArrayNode();
                for (XDeathEntry e : msg.xDeathEntries()) {
                    ObjectNode ed = MAPPER.createObjectNode();
                    ed.put("queue", e.queue());
                    ed.put("exchange", e.exchange());
                    ed.put("reason", e.reason());
                    ed.put("count", e.count());
                    if (e.time() > 0) ed.put("time", TS_FMT.format(Instant.ofEpochSecond(e.time())));
                    xdeath.add(ed);
                }
                node.set("xDeath", xdeath);
            }
            array.add(node);
        }
        try {
            Console.info(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(array));
        } catch (Exception e) {
            Console.error("Failed to serialize messages to JSON: " + e.getMessage());
        }
    }

    public void printMessagesTable(List<RabbitMessage> messages) {
        String fmt = "%-4s  %-30s  %-25s  %-10s  %-19s  %s";
        Console.dim(String.format(fmt, "#", "ROUTING-KEY", "EXCHANGE", "REASON", "TIME", "PAYLOAD"));
        Console.dim(HR);
        for (RabbitMessage msg : messages) {
            String reason = "";
            String time = "";
            if (!msg.xDeathEntries().isEmpty()) {
                XDeathEntry latest = msg.xDeathEntries().getFirst();
                reason = latest.reason();
                if (latest.time() > 0) time = TS_FMT.format(Instant.ofEpochSecond(latest.time()));
            }
            String payloadSnippet = msg.payload().replace('\n', ' ').replace('\r', ' ');
            if (payloadSnippet.length() > 40) payloadSnippet = payloadSnippet.substring(0, 37) + "...";
            Console.info(String.format(fmt,
                    "#" + msg.messageNumber(),
                    msg.routingKey(),
                    msg.exchange(),
                    reason,
                    time,
                    payloadSnippet));
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
