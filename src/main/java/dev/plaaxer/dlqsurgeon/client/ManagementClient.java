package dev.plaaxer.dlqsurgeon.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.plaaxer.dlqsurgeon.cli.ConnectOptions;
import dev.plaaxer.dlqsurgeon.model.DeadLetteredMessage;
import dev.plaaxer.dlqsurgeon.model.QueueInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around the RabbitMQ Management HTTP API.
 *
 * Uses Java 21's built-in HttpClient (no extra dependency) with Virtual Threads
 * for non-blocking I/O. All calls are synchronous from the caller's perspective;
 * the VT scheduler handles the actual blocking.
 *
 * Key endpoints used:
 *   GET  /api/queues/{vhost}/{queue}              → queue metadata + message count
 *   GET  /api/queues                               → list all queues
 *   POST /api/queues/{vhost}/{queue}/get           → fetch messages (requeue=true)
 *   DELETE /api/queues/{vhost}/{queue}/contents    → purge (not used in normal flow)
 *
 * Important: The /get endpoint *consumes* messages. We always pass requeue=true
 * to put them back immediately. This means messages are briefly unavailable to
 * other consumers during the fetch — negligible for DLQs which have no active
 * consumers.
 *
 * TODO: Add TLS support by injecting an SSLContext into HttpClient.newBuilder().
 * TODO: Cache the authentication header (it doesn't change per session).
 * TODO: Implement pagination for very large queue lists.
 */
public class ManagementClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http;
    private final String baseUrl;
    private final String authHeader;
    private final String vhost;

    public ManagementClient(ConnectOptions opts) {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        this.vhost = opts.vhost;
        this.baseUrl = "http://" + opts.host + ":" + opts.managementPort + "/api";

        // Basic auth header — encode once, reuse on every request.
        String credentials = opts.user + ":" + new String(opts.password);
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Returns metadata for every queue in the vhost.
     * Queues with {@code x-dead-letter-exchange} in their arguments are flagged as DLQs.
     */
    public List<QueueInfo> listQueues() throws Exception {
        String body = get("/queues/" + encodedVhost());
        List<Map<String, Object>> raw = MAPPER.readValue(body, new TypeReference<>() {});
        return raw.stream().map(this::toQueueInfo).toList();
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private QueueInfo toQueueInfo(Map<String, Object> raw) {
        String name = (String) raw.get("name");
        int messages = ((Number) raw.getOrDefault("messages", 0)).intValue();
        Map<String, Object> args = (Map<String, Object>) raw.getOrDefault("arguments", Map.of());
        boolean isDlq = args.containsKey("x-dead-letter-exchange");
        return new QueueInfo(name, messages, isDlq);
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
    public List<DeadLetteredMessage> fetchMessages(String queueName, int count) throws Exception {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private String get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new RuntimeException("Management API error " + status + " for " + path + ": " + response.body());
        }
        return response.body();
    }

    /**
     * TODO: Implement.
     * Builds a POST request with a JSON body. Used for /get and future endpoints.
     */
    private String post(String path, String jsonBody) throws Exception {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * URL-encodes the vhost for use in API paths.
     * "/" must become "%2F" in the URL path segment.
     */
    private String encodedVhost() {
        return vhost.equals("/") ? "%2F" : vhost;
    }
}
