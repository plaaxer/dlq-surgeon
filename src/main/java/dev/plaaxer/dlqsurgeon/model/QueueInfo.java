package dev.plaaxer.dlqsurgeon.model;

/**
 * Lightweight snapshot of a RabbitMQ queue, as returned by
 * {@code GET /api/queues/{vhost}}.
 *
 * @param name         Queue name.
 * @param messageCount Total messages currently in the queue.
 * @param isDlq        True when the queue has {@code x-dead-letter-exchange} set in its
 *                     arguments — i.e. it is itself a dead-letter queue.
 */
public record QueueInfo(
        String name,
        int messageCount,
        boolean isDlq
) {
    /** One-line label for the interactive queue picker. */
    public String label() {
        return String.format("%-50s  %5d msg%s", name, messageCount, isDlq ? "  [DLQ]" : "");
    }
}