package dev.plaaxer.dlqsurgeon.it;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import dev.plaaxer.dlqsurgeon.client.AmqpPublisher;
import dev.plaaxer.dlqsurgeon.model.RepairPlan;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link AmqpPublisher} against a real RabbitMQ container.
 *
 * Focus: publisher confirms are received and the message actually lands in the
 * target exchange/queue.
 *
 * TODO: Implement the test cases below.
 */
class AmqpPublisherIT extends RabbitContainerBase {

    private static final String TARGET_EXCHANGE = "it.publisher.exchange";
    private static final String TARGET_QUEUE = "it.publisher.target";
    private static final String ROUTING_KEY = "it.key";
    private static final String SOURCE_QUEUE = "it.publisher.dlq";

    private Channel ch;

    @BeforeEach
    void setUp() throws Exception {
        ch = channel();
        // TODO: declare TARGET_EXCHANGE (direct), TARGET_QUEUE, bind TARGET_QUEUE
        //       to TARGET_EXCHANGE with ROUTING_KEY.
        // TODO: declare SOURCE_QUEUE (plain, holds the "dead" messages to be deleted).
    }

    @AfterEach
    void tearDown() throws Exception {
        // TODO: purge and delete TARGET_QUEUE, SOURCE_QUEUE, TARGET_EXCHANGE.
        if (ch != null && ch.isOpen()) ch.close();
    }

    // ── publish ──────────────────────────────────────────────────────────────

    @Test
    void publish_messageArrivesInTargetQueue() throws Exception {
        // TODO: build a RepairPlan pointing to TARGET_EXCHANGE / ROUTING_KEY with
        //       payload "{\"id\":1}", call publisher.publish(plan), then
        //       basicGet(TARGET_QUEUE) and assert payload matches.
    }

    @Test
    void publish_preservesContentType() throws Exception {
        // TODO: set contentType="application/json" in the plan, publish, consume
        //       the message and assert AMQP props.getContentType() == "application/json".
    }

    @Test
    void publish_preservesCustomHeaders() throws Exception {
        // TODO: include a custom header "x-trace-id"="abc" in the plan, publish,
        //       consume and assert the header survives.
    }

    // ── deleteFromDlq ────────────────────────────────────────────────────────

    @Test
    void deleteFromDlq_removesExactlyOneMessage() throws Exception {
        // TODO: publish 2 identical messages to SOURCE_QUEUE, call
        //       publisher.deleteFromDlq(SOURCE_QUEUE, firstMessage),
        //       assert SOURCE_QUEUE has 1 message left.
        //
        // NOTE: AmqpPublisher.deleteFromDlq() likely uses basicConsume+basicAck
        //       or basicGet+basicAck — confirm the implementation and test
        //       accordingly.
    }

    @Test
    void deleteFromDlq_doesNotDeleteOnPublishFailure() throws Exception {
        // TODO: publish to a non-existent exchange so the publish fails (returned
        //       or NACK'd), assert SOURCE_QUEUE still has its message and an
        //       exception was thrown by publish().
        //
        // This is the core safety invariant: no deletion without confirm.
    }
}