package dev.plaaxer.dlqsurgeon.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Represents a single message fetched from a RabbitMQ queue via the Management API.
 *
 * This is an in-memory snapshot; the original message remains in the queue
 * (requeued by ManagementClient.fetchMessages) until the user commits a repair.
 *
 * Fields mirror the shape returned by the Management API /get endpoint.
 *
 * TODO: If you add binary (non-JSON) payload support, store rawPayloadBytes
 *       separately and decode lazily only when the editor is opened.
 *
 * @param messageNumber    1-based index within the current fetch batch (for display).
 * @param exchange         Exchange the message was originally published to.
 * @param routingKey       Routing key the message was originally published with.
 * @param payload          Message body as a UTF-8 string (decoded from base64 if needed).
 * @param contentType      MIME type from properties (may be null).
 * @param headers          Raw AMQP headers map, including x-death entries.
 * @param xDeathEntries    Parsed x-death header entries in chronological order; empty for non-DLQ messages.
 * @param deliveryMode     1 = non-persistent, 2 = persistent.
 * @param correlationId    Correlation ID from properties (may be null).
 * @param messageId        Message ID from properties (may be null).
 * @param redelivered      Whether this message has been redelivered before.
 */
public record RabbitMessage(
        int messageNumber,
        String exchange,
        String routingKey,
        String payload,
        String contentType,
        Map<String, Object> headers,
        List<XDeathEntry> xDeathEntries,
        int deliveryMode,
        String correlationId,
        String messageId,
        boolean redelivered
) {

    private static final DateTimeFormatter LABEL_FMT =
            DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /**
     * Returns a short display label for use in the message picker list.
     * Example: "#3  orders.created  → orders.dlx  rejected  03-23 16:49:08"
     */
    public String label() {
        String reason = "";
        String time = "";
        if (!xDeathEntries.isEmpty()) {
            XDeathEntry last = xDeathEntries.getLast();
            reason = last.reason();
            if (last.time() > 0) {
                time = LABEL_FMT.format(Instant.ofEpochSecond(last.time()));
            }
        }
        return String.format("#%-3d %-30s → %-25s %-10s %s",
                messageNumber, routingKey, exchange, reason, time);
    }

    /**
     * Returns the original exchange and routing key from the most recent x-death entry.
     * Falls back to the message's own exchange/routingKey if x-death is absent.
     *
     * This is the target for re-injection unless the user overrides with --target-* flags.
     */
    public String originalExchange() {
        if (xDeathEntries.isEmpty()) return exchange;
        return xDeathEntries.getLast().exchange();
    }

    public String originalRoutingKey() {
        if (xDeathEntries.isEmpty()) return routingKey;
        // x-death stores routing-keys as a List<String>; take the first.
        List<String> keys = xDeathEntries.getLast().routingKeys();
        return keys.isEmpty() ? routingKey : keys.getFirst();
    }

    @SuppressWarnings("unchecked")
    public static RabbitMessage from(Map<String, Object> raw, int messageNumber) {
        String exchange = (String) raw.get("exchange");
        String routingKey = (String) raw.get("routing_key");
        boolean redelivered = Boolean.TRUE.equals(raw.get("redelivered"));

        String payloadRaw = (String) raw.get("payload");
        String payload = "base64".equals(raw.get("payload_encoding"))
                ? new String(Base64.getDecoder().decode(payloadRaw))
                : payloadRaw;

        Map<String, Object> properties = (Map<String, Object>) raw.getOrDefault("properties", Map.of());
        String contentType = (String) properties.get("content_type");
        int deliveryMode = ((Number) properties.getOrDefault("delivery_mode", 1)).intValue();
        String correlationId = (String) properties.get("correlation_id");
        String messageId = (String) properties.get("message_id");
        Map<String, Object> headers = (Map<String, Object>) properties.getOrDefault("headers", Map.of());

        List<XDeathEntry> xDeathEntries = headers.get("x-death") instanceof List<?> xDeath
                ? ((List<Map<String, Object>>) xDeath).stream().map(XDeathEntry::from).toList()
                : List.of();

        return new RabbitMessage(messageNumber, exchange, routingKey, payload, contentType,
                headers, xDeathEntries, deliveryMode, correlationId, messageId, redelivered);
    }
}