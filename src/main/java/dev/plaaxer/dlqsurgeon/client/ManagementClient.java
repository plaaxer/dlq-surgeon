package dev.plaaxer.dlqsurgeon.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.plaaxer.dlqsurgeon.cli.ConnectOptions;
import dev.plaaxer.dlqsurgeon.model.QueueInfo;
import dev.plaaxer.dlqsurgeon.model.RabbitMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around the RabbitMQ Management HTTP API.
 *
 * Key endpoints used:
 *   GET  /api/queues/{vhost}/{queue}              → queue metadata + message count
 *   GET  /api/queues                               → list all queues
 *   POST /api/queues/{vhost}/{queue}/get           → fetch messages (requeue=true)
 *   DELETE /api/queues/{vhost}/{queue}/contents    → purge (not used in normal flow)
 *
 * Reference: <a href="https://www.rabbitmq.com/docs/http-api-reference">...</a>
 *
 * TODO: Add TLS support by injecting an SSLContext into HttpClient.newBuilder().
 * TODO: Implement pagination for very large queue lists.
 */
public class ManagementClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ApiHttpClient http;
    private final String vhost;

    public ManagementClient(ConnectOptions opts) {
        this.http = new ApiHttpClient(opts);
        this.vhost = opts.vhost;
    }

    /**
     * Returns metadata for every queue in the vhost.
     * Queues with {@code x-dead-letter-exchange} in their arguments are flagged with {@code hasDlx=true},
     * meaning dead messages from those queues are forwarded to another exchange — not that they are DLQs themselves.
     */
    public List<QueueInfo> listQueues() throws Exception {
        String body = http.get("/queues/" + encodedVhost());

        List<Map<String, Object>> raw = MAPPER.readValue(body, new TypeReference<>() {});
        return raw.stream().map(this::toQueueInfo).toList();
    }

    /**
     * Returns metadata for a single queue by name.
     * GET /api/queues/{vhost}/{queue}
     */
    public QueueInfo getQueue(String queueName) throws Exception {
        String body = http.get("/queues/" + encodedVhost() + "/" + queueName);

        Map<String, Object> raw = MAPPER.readValue(body, new TypeReference<>() {});
        return toQueueInfo(raw);
    }

    /**
     * TODO: Implement.
     * Fetches up to {@code count} messages from {@code queueName}.
     * Always sets requeue=true so messages are not permanently removed.
     *
     * POST /api/queues/{vhost}/{queue}/get
     * Body: { "count": N, "ackmode": "ack_requeue_true", "encoding": "auto", "truncate": 50000 }
     *
     * The response is a JSON array; each element contains:
     *   - payload        (string, possibly base64 if binary)
     *   - payload_encoding ("string" | "base64")
     *   - properties     (headers, content-type, delivery-mode, etc.)
     *   - routing_key
     *   - exchange
     *   - redelivered
     *   - message_count  (messages remaining after this fetch)
     *
     * Map each element to a DeadLetteredMessage, parsing x-death from properties.headers.
     */
    public List<RabbitMessage> fetchMessages(String queueName, int count) throws Exception {
        ObjectNode bodyNode = MAPPER.createObjectNode();
        bodyNode.put("count", count);
        bodyNode.put("ackmode", "ack_requeue_true");
        bodyNode.put("encoding", "auto");
        bodyNode.put("truncate", 50000);

        String jsonBody = MAPPER.writeValueAsString(bodyNode);
        String response = http.post("/queues/" + encodedVhost() + "/" + queueName + "/get", jsonBody);

        List<Map<String, Object>> raw = MAPPER.readValue(response, new TypeReference<>() {});

        List<RabbitMessage> result = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            result.add(RabbitMessage.from(raw.get(i), i + 1, queueName));
        }
        return result;
    }

    // ──────────────────────────────────────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private QueueInfo toQueueInfo(Map<String, Object> raw) {
        String name = (String) raw.get("name");
        int messages = ((Number) raw.getOrDefault("messages", 0)).intValue();
        Map<String, Object> args = (Map<String, Object>) raw.getOrDefault("arguments", Map.of());
        boolean hasDlx = args.containsKey("x-dead-letter-exchange");
        return new QueueInfo(name, messages, hasDlx);
    }

    /**
     * URL-encodes the vhost for use in API paths.
     * "/" must become "%2F" in the URL path segment.
     */
    private String encodedVhost() {
        return vhost.equals("/") ? "%2F" : vhost;
    }
}
