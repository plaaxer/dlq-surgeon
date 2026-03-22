package dev.plaaxer.dlqsurgeon.it;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import dev.plaaxer.dlqsurgeon.client.ManagementClient;
import dev.plaaxer.dlqsurgeon.model.RabbitMessage;
import dev.plaaxer.dlqsurgeon.model.RepairPlan;
import dev.plaaxer.dlqsurgeon.surgeon.Reinjector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link Reinjector} against a real RabbitMQ container.
 *
 * These tests guard the core safety invariant:
 *   "Delete the source message ONLY after a publisher confirm is received."
 */
class ReinjectorIT extends RabbitContainerBase {

    private static final String TARGET_EXCHANGE = "it.reinjector.exchange";
    private static final String TARGET_QUEUE = "it.reinjector.target";
    private static final String DEAD_EXCHANGE = "it.reinjector.dead_exchange";
    private static final String DEAD_QUEUE = "it.reinjector.dlq";
    private static final String ROUTING_KEY = "it.key";
    private static final String DEAD_ROUTING_KEY = "it.dead_key";

    private Channel ch;
    private Reinjector reinjector;

    @BeforeEach
    void setUp() throws Exception {

        ch = channel();
        reinjector = new Reinjector(containerOpts());

        HashMap<String, Object> target_args = new HashMap<>();
        target_args.put("x-dead-letter-exchange", DEAD_EXCHANGE);
        target_args.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);

        ch.exchangeDeclare(TARGET_EXCHANGE, BuiltinExchangeType.DIRECT, true);
        ch.queueDeclare(TARGET_QUEUE, true, false, false, target_args);
        ch.exchangeDeclare(DEAD_EXCHANGE, BuiltinExchangeType.DIRECT, true);
        ch.queueDeclare(DEAD_QUEUE, true, false, false, null);

        ch.queueBind(TARGET_QUEUE, TARGET_EXCHANGE, ROUTING_KEY);
        ch.queueBind(DEAD_QUEUE, DEAD_EXCHANGE, DEAD_ROUTING_KEY);
    }

    // AfterEach runs even if something breaks, guaranteeing that the connection is always closed.
    @AfterEach
    void tearDown() throws Exception {

        if (ch != null && ch.isOpen()) {
            ch.exchangeDeleteNoWait(TARGET_EXCHANGE, false);
            ch.exchangeDeleteNoWait(DEAD_EXCHANGE, false);
            ch.queueDeleteNoWait(TARGET_QUEUE, false, false);
            ch.queueDeleteNoWait(DEAD_QUEUE, false, false);
            ch.close();
        }
    }

    /**
     * Publishes a message directly to the DLQ. Builds repair plan and then reinjects it to the target queue.
     * Asserts that the reinjection was successful.
     * @throws Exception
     */
    @Test
    void reinjectAndDelete_publishesAndRemovesSource() throws Exception {
        byte[] payload = "{\"test\":\"payload\"}".getBytes();

        ch.basicPublish(DEAD_EXCHANGE, DEAD_ROUTING_KEY, null, payload);

        ManagementClient mgmt = new ManagementClient(containerOpts());
        List<RabbitMessage> messages = mgmt.fetchMessages(DEAD_QUEUE, 1);
        assertEquals(1, messages.size());
        RabbitMessage source = messages.get(0);

        RepairPlan plan = RepairPlan.from(source, source.payload(), TARGET_EXCHANGE, ROUTING_KEY, false);

        reinjector.reinjectAndDelete(plan, source);

        GetResponse got = ch.basicGet(TARGET_QUEUE, true);
        assertNotNull(got);
        assertEquals(source.payload(), new String(got.getBody()));

        assertEquals(0, ch.messageCount(DEAD_QUEUE));
    }

    @Test
    void reinjectAndDelete_preservesEditedPayload() throws Exception {
        // TODO: original DLQ message has payload A; RepairPlan has edited payload B.
        //       After reinject, TARGET_QUEUE message must have payload B, not A.
    }

    // ── Safety invariant ─────────────────────────────────────────────────────

    @Test
    void reinjectAndDelete_doesNotDeleteWhenPublishFails() throws Exception {
        // TODO: build a RepairPlan targeting a non-existent exchange so publish
        //       fails (mandatory flag or unroutable → returned/NACK).
        //       Assert: exception is thrown AND DLQ still has the original message.
        //
        // This is the most important test in the entire suite. Future me better implement it soon.
    }

    @Test
    void reinjectAndDelete_isIdempotentOnRetry() throws Exception {
        // TODO: call reinjectAndDelete twice with the same source message.
        //       Second call should either succeed (idempotent) or throw a clear error —
        //       decide on the expected contract and assert accordingly.
        //       The goal is to make sure a retry after partial failure does not
        //       duplicate the message in TARGET_QUEUE.
    }
}