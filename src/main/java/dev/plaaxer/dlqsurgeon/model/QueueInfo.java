package dev.plaaxer.dlqsurgeon.model;

/**
 * Lightweight snapshot of a RabbitMQ queue, as returned by
 * {@code GET /api/queues/{vhost}}.
 *
 * @param name         Queue name.
 * @param messageCount Total messages currently in the queue.
 * @param hasDlx       True when the queue has {@code x-dead-letter-exchange} set in its
 *                     arguments. This means dead messages from this queue are routed to
 *                     another exchange (and ultimately a dead-letter queue). It does NOT
 *                     mean this queue is itself a dead-letter queue.
 */
public record QueueInfo(
        String name,
        int messageCount,
        boolean hasDlx
) {
    public String label() {
        return String.format("%-50s  %5d msg%s", name, messageCount, hasDlx ? "  [DLX]" : "");
    }
}