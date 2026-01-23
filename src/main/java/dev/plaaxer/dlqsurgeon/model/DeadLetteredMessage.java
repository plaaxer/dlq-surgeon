package dev.plaaxer.dlqsurgeon.model;

import java.util.List;
import java.util.Map;

/**
 * Represents a single message fetched from a Dead Letter Queue.
 *
 * This is an in-memory snapshot; the original message remains in the DLQ
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
 * @param xDeathEntries    Parsed x-death header entries in chronological order.
 * @param deliveryMode     1 = non-persistent, 2 = persistent.
 * @param correlationId    Correlation ID from properties (may be null).
 * @param messageId        Message ID from properties (may be null).
 * @param redelivered      Whether this message has been redelivered before.
 */
public record DeadLetteredMessage(
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

    /**
     * Returns a short display label for use in the message picker list.
     * Example: "#3  orders.created  → dead.letter.exchange  (died 4×)"
     */
    public String label() {
        int deathCount = Math.toIntExact(xDeathEntries.stream()
                .mapToLong(XDeathEntry::count)
                .sum());
        return String.format("#%-3d %-40s → %-30s (died %d×)",
                messageNumber, routingKey, exchange, deathCount);
    }

    /**
     * Returns the original exchange and routing key from the most recent x-death entry.
     * Falls back to the message's own exchange/routingKey if x-death is absent.
     *
     * This is the target for re-injection unless the user overrides with --target-* flags.
     *
     * TODO: Implement. The most recent x-death entry is the last element in the list
     *       (x-death is prepended, not appended, by RabbitMQ).
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
}
