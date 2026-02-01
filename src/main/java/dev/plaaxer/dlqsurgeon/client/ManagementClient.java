package dev.plaaxer.dlqsurgeon.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.plaaxer.dlqsurgeon.cli.ConnectOptions;
import dev.plaaxer.dlqsurgeon.model.DeadLetteredMessage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

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
     * TODO: Implement.
     * Returns metadata for every queue in the vhost.
     * Filter by x-dead-letter-exchange presence to identify true DLQs.
     */
    public List<?> listQueues() throws Exception {
        // GET /api/queues/{vhost}
        // Parse response as List<Map<String,Object>> and map to a light QueueInfo record.
        throw new UnsupportedOperationException("Not yet implemented");
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

    // ── Private helpers ─────────────────────────────────────────────────────

    /**
     * TODO: Implement.
     * Builds a GET request with Basic auth and sends it, returning the body string.
     * Throws a descriptive exception on non-2xx responses (include status + body).
     */
    private String get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        // TODO: check response.statusCode(), throw on error with message from body
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
