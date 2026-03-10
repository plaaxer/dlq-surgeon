package dev.plaaxer.dlqsurgeon.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RepairPlan.
 *
 * TODO: Add tests once RepairPlan.from() is implemented.
 *
 * Suggested test cases:
 *  - from() defaults to empty exchange and x-death queue as routing key.
 *  - from() applies targetExchangeOverride when supplied.
 *  - from() applies targetRoutingKeyOverride when supplied.
 *  - from() removes x-death headers when stripDeathHeaders=true.
 *  - from() preserves all other headers intact.
 *  - from() falls back to message routing key when x-death is empty.
 *  - summary() output contains the correct queue, exchange, and routing-key values.
 */
class RepairPlanTest {

    private RabbitMessage sampleMessage() {
        XDeathEntry death = new XDeathEntry(
                "orders",               // original exchange
                List.of("orders.created"), // original routing keys
                "orders-queue",
                "rejected",
                3L,
                System.currentTimeMillis()
        );
        return new RabbitMessage(
                1,
                "orders.dead",          // exchange (the DLX)
                "orders.created",
                "orders.dlq",           // sourceQueue
                "{\"orderId\": 42}",
                "application/json",
                Map.of(
                        "x-death", List.of(Map.of("exchange", "orders")),
                        "x-first-death-exchange", "orders"
                ),
                List.of(death),
                2,
                "corr-123",
                "msg-456",
                false
        );
    }

    @Test
    void placeholderTest() {
        // Remove this test once you implement RepairPlan.from() and add real tests above.
        assertNotNull(sampleMessage());
    }
}
