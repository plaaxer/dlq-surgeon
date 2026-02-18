package dev.plaaxer.dlqsurgeon.surgeon;

import dev.plaaxer.dlqsurgeon.cli.ConnectOptions;
import dev.plaaxer.dlqsurgeon.client.AmqpPublisher;
import dev.plaaxer.dlqsurgeon.client.ManagementClient;
import dev.plaaxer.dlqsurgeon.model.RabbitMessage;
import dev.plaaxer.dlqsurgeon.model.RepairPlan;

/**
 * Executes the surgical re-injection: publish the repaired message, then delete
 * the original from the DLQ — in that strict order, with no exceptions.
 *
 * Safety invariant (NEVER VIOLATE):
 *   The source message is deleted from the DLQ ONLY after a publisher confirm
 *   is received from the broker for the repaired message. If publishing fails
 *   or times out, this method must throw without deleting anything.
 *
 * There is an intentional lack of a rollback mechanism: once the confirm is
 * received, the new message is in the broker. At that point deletion is safe.
 * If deletion fails after a successful publish, the message may be processed
 * twice — log a clear error and let the user handle it manually.
 *
 * TODO: For bulk repair, add a reinjectAndDeleteBatch() method that processes
 *       messages in order and reports partial success clearly.
 */
public class Reinjector {

    private final ConnectOptions opts;

    public Reinjector(ConnectOptions opts) {
        this.opts = opts;
    }

    /**
     * Publishes the repaired message and, on confirmed success, removes the
     * original from the DLQ.
     *
     * @param plan    The repair plan (edited payload, target exchange/key, properties).
     * @param source  The original dead-lettered message (needed to identify and
     *                delete the correct message from the DLQ after re-injection).
     *
     * TODO: Implement.
     *   1. try (AmqpPublisher publisher = new AmqpPublisher(opts)) {
     *          publisher.publish(plan);   // blocks until broker confirms
     *      }
     *      // If publish() throws, propagate — do NOT proceed to deletion.
     *
     *   2. After the try-with-resources closes cleanly (confirm received):
     *      deleteFromDlq(source);
     *
     *   3. deleteFromDlq: Use the Management API DELETE endpoint to remove the
     *      specific message. Note: the Management API does not support deleting
     *      a single message by ID. The standard approach is:
     *        a. Fetch the message again via AMQP basic.get (NOT requeue).
     *        b. Verify payload + message-id match the source message.
     *        c. basic.ack the delivery tag.
     *      This prevents accidentally acking the wrong message if a new message
     *      arrived in the DLQ between fetch and ack.
     *      See: https://www.rabbitmq.com/docs/consumers#acknowledgement-modes
     *
     *   Throw a descriptive RuntimeException if deletion fails after a successful
     *   publish — the user needs to know to manually clean up the DLQ.
     */
    public void reinjectAndDelete(RepairPlan plan, RabbitMessage source) throws Exception {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
