package dev.plaaxer.dlqsurgeon.model;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * An immutable value object describing the full re-injection plan for a repaired message.
 *
 * Built in FixCommand after the user has edited the payload and before Reinjector acts.
 * Printed to the console for user confirmation before any write operation occurs.
 *
 * @param targetExchange     Exchange to publish to (from x-death or user override).
 * @param targetRoutingKey   Routing key to use (from x-death or user override).
 * @param editedPayload      The new message body, post-editor and post-validation.
 * @param properties         Full AMQP basic properties to preserve (content-type,
 *                           delivery-mode, correlation-id, message-id, headers, etc.).
 * @param sourceQueue        The DLQ the message will be deleted from after successful publish.
 * @param stripDeathHeaders  Whether to remove x-death and x-first-death-* before publishing.
 */
public record RepairPlan(
        String targetExchange,
        String targetRoutingKey,
        String editedPayload,
        Map<String, Object> properties,
        String sourceQueue,
        boolean stripDeathHeaders
) {

    /**
     * Factory method that constructs a RepairPlan from a fetched message and user overrides.
     */
    public static RepairPlan from(
            RabbitMessage message,
            String editedPayload,
            String targetExchangeOverride,
            String targetRoutingKeyOverride,
            boolean stripDeathHeaders
    ) {
        Map<String, Object> props = new LinkedHashMap<>(message.headers());
        if (stripDeathHeaders) {
            props.keySet().removeIf(k -> k.equals("x-death") || k.startsWith("x-first-death-"));
        }

        return new RepairPlan(
                targetExchangeOverride != null ? targetExchangeOverride : message.originalExchange(),
                targetRoutingKeyOverride != null ? targetRoutingKeyOverride : message.originalRoutingKey(),
                editedPayload,
                props,
                message.sourceQueue(),
                stripDeathHeaders
        );
    }

    /**
     * Returns a human-readable summary for the confirmation prompt.
     *
     * Example output:
     *   ┌─ Repair Plan ─────────────────────────────────┐
     *   │ Source queue   : orders.dead                   │
     *   │ Target exchange: orders                        │
     *   │ Routing key    : orders.created                │
     *   │ Strip x-death  : false                         │
     *   │ Payload diff   : (shown by PayloadEditor)      │
     *   └────────────────────────────────────────────────┘
     */
    public String summary() {
        return String.format("""
                ┌─ Repair Plan ──────────────────────────────────────┐
                │ Source queue    : %-32s │
                │ Target exchange : %-32s │
                │ Routing key     : %-32s │
                │ Strip x-death   : %-32s │
                └────────────────────────────────────────────────────┘
                """,
                truncate(sourceQueue, 32),
                truncate(targetExchange, 32),
                truncate(targetRoutingKey, 32),
                stripDeathHeaders);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "(null)";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
