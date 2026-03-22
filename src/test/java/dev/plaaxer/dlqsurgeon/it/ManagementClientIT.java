package dev.plaaxer.dlqsurgeon.it;

import dev.plaaxer.dlqsurgeon.client.ManagementClient;
import dev.plaaxer.dlqsurgeon.model.QueueInfo;
import dev.plaaxer.dlqsurgeon.model.RabbitMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link ManagementClient} against a real RabbitMQ container.
 *
 * Each test declares a queue, exercises the client, then tears down.
 * The queue name includes the test name prefix to avoid collisions when tests
 * run in parallel.
 *
 * TODO: Implement the test cases below once the wiring is confirmed green.
 */
class ManagementClientIT extends RabbitContainerBase {

    private static final String TEST_QUEUE = "it.management.test-queue";
    private static final String DLQ = "it.management.dlq";
    private static final String DLX = "it.management.dlx";

    private ManagementClient client;

    @BeforeEach
    void setUp() throws Exception {
        client = new ManagementClient(containerOpts());

        // TODO: declare TEST_QUEUE with x-dead-letter-exchange=DLX so hasDlx=true,
        //       and declare DLQ as a plain queue to hold dead letters.
        //       Use channel().queueDeclare(...) + channel().exchangeDeclare(DLX, "direct").
    }

    @AfterEach
    void tearDown() throws Exception {
        // TODO: delete TEST_QUEUE, DLQ, DLX to leave the broker clean.
    }

    // ── listQueues ───────────────────────────────────────────────────────────

    @Test
    void listQueues_returnsAtLeastTheTestQueue() throws Exception {
        // TODO: declare TEST_QUEUE, call client.listQueues(), assert the result
        //       contains a QueueInfo with name=TEST_QUEUE.
    }

    @Test
    void listQueues_flagsHasDlxCorrectly() throws Exception {
        // TODO: TEST_QUEUE has x-dead-letter-exchange set → hasDlx=true.
        //       DLQ has no DLX → hasDlx=false.
        //       Assert both are reflected in the list.
    }

    // ── getQueue ─────────────────────────────────────────────────────────────

    @Test
    void getQueue_returnsCorrectMessageCount() throws Exception {
        // TODO: publish 3 messages to TEST_QUEUE via channel().basicPublish(...),
        //       call client.getQueue(TEST_QUEUE), assert messages == 3.
    }

    @Test
    void getQueue_throwsForUnknownQueue() throws Exception {
        // TODO: call client.getQueue("non-existent-queue") and assert an exception
        //       is thrown (HTTP 404 from the management API).
    }

    // ── fetchMessages ────────────────────────────────────────────────────────

    @Test
    void fetchMessages_returnsPublishedPayload() throws Exception {
        // TODO: publish a JSON message to TEST_QUEUE, call client.fetchMessages(TEST_QUEUE, 1),
        //       assert the result has size=1 and payload matches.
    }

    @Test
    void fetchMessages_doesNotDeleteMessages() throws Exception {
        // TODO: publish 1 message, fetch it, fetch again — message should still
        //       be there (requeue=true is the contract).
    }

    @Test
    void fetchMessages_respectsCountLimit() throws Exception {
        // TODO: publish 10 messages, fetchMessages(queue, 3) → assert result.size() == 3.
    }
}