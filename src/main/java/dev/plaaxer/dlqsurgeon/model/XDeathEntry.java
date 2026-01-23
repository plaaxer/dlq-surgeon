package dev.plaaxer.dlqsurgeon.model;

import java.util.List;

/**
 * Represents one entry in the x-death AMQP header array.
 *
 * RabbitMQ prepends a new entry to x-death each time a message is dead-lettered,
 * so the list is reverse-chronological (index 0 = most recent death).
 *
 * Documented at: https://www.rabbitmq.com/docs/dlx#effects-on-messages
 *
 * @param exchange    The exchange the message was dead-lettered from.
 * @param routingKeys The routing keys the message was dead-lettered with.
 * @param queue       The queue the message was dead-lettered from.
 * @param reason      Why the message was dead-lettered: "rejected", "expired", "maxlen",
 *                    "delivery-limit" (quorum queues), or "expired" (TTL).
 * @param count       How many times this exchange/queue combination has dead-lettered
 *                    this message.
 * @param time        Epoch milliseconds when the message was dead-lettered
 *                    (stored as long from the AMQP timestamp).
 *
 * TODO: Parse these from the raw headers map in ManagementClient.
 *       The x-death value is a List of Maps; each map has string keys and mixed values.
 *       Be careful: "time" comes as a java.util.Date from the AMQP client library,
 *       so convert it: ((Date) raw.get("time")).getTime().
 */
public record XDeathEntry(
        String exchange,
        List<String> routingKeys,
        String queue,
        String reason,
        long count,
        long time
) {}
