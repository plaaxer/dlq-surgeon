package dev.plaaxer.dlqsurgeon.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RepairPlan.
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
        Map<String, Object> headers = new HashMap<>();
        headers.put("x-death", List.of(Map.of("exchange", "orders")));
        headers.put("x-first-death-exchange", "orders");
        headers.put("content-type", "application/json");
        return new RabbitMessage(
                1,
                "orders.dead",          // exchange (the DLX)
                "orders.created",
                "orders.dlq",           // sourceQueue
                "{\"orderId\": 42}",
                "application/json",
                headers,
                List.of(death),
                2,
                "corr-123",
                "msg-456",
                false
        );
    }

    private RabbitMessage messageWithoutXDeath() {
        return new RabbitMessage(
                1,
                "payments.dlx",
                "payments.retry",
                "payments.dlq",
                "{\"amount\": 99}",
                "application/json",
                new HashMap<>(),
                List.of(),
                2,
                null,
                null,
                false
        );
    }

    @Test
    void defaultsToXDeathExchangeAndRoutingKey() {
        RepairPlan plan = RepairPlan.from(sampleMessage(), "{}", null, null, false);

        assertEquals("orders", plan.targetExchange());
        assertEquals("orders.created", plan.targetRoutingKey());
    }

    @Test
    void appliesTargetExchangeOverride() {
        RepairPlan plan = RepairPlan.from(sampleMessage(), "{}", "custom.exchange", null, false);

        assertEquals("custom.exchange", plan.targetExchange());
        assertEquals("orders.created", plan.targetRoutingKey()); // routing key unchanged
    }

    @Test
    void appliesTargetRoutingKeyOverride() {
        RepairPlan plan = RepairPlan.from(sampleMessage(), "{}", null, "custom.key", false);

        assertEquals("orders", plan.targetExchange()); // exchange unchanged
        assertEquals("custom.key", plan.targetRoutingKey());
    }

    @Test
    void removesXDeathHeadersWhenStripEnabled() {
        RepairPlan plan = RepairPlan.from(sampleMessage(), "{}", null, null, true);

        Map<String, Object> headers = plan.properties().getHeaders();
        assertFalse(headers.containsKey("x-death"), "x-death should be stripped");
        assertFalse(headers.containsKey("x-first-death-exchange"), "x-first-death-exchange should be stripped");
        assertTrue(plan.stripDeathHeaders());
    }

    @Test
    void preservesOtherHeadersWhenStripping() {
        RepairPlan plan = RepairPlan.from(sampleMessage(), "{}", null, null, true);

        Map<String, Object> headers = plan.properties().getHeaders();
        assertTrue(headers.containsKey("content-type"), "non-death headers must be preserved");
    }

    @Test
    void fallsBackToEnvelopeWhenXDeathIsEmpty() {
        RepairPlan plan = RepairPlan.from(messageWithoutXDeath(), "{}", null, null, false);

        assertEquals("payments.dlx", plan.targetExchange());
        assertEquals("payments.retry", plan.targetRoutingKey());
    }

    @Test
    void summaryContainsQueueExchangeAndRoutingKey() {
        RabbitMessage msg = sampleMessage();
        RepairPlan plan = RepairPlan.from(msg, "{}", null, null, false);

        String summary = plan.summary();
        assertTrue(summary.contains("orders.dlq"), "summary should contain source queue");
        assertTrue(summary.contains("orders"), "summary should contain target exchange");
        assertTrue(summary.contains("orders.created"), "summary should contain routing key");
    }
}
