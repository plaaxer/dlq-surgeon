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
     */
    public void reinjectAndDelete(RepairPlan plan, RabbitMessage source) throws Exception {
        try (AmqpPublisher publisher = new AmqpPublisher(opts)) {
            publisher.publish(plan);
            // Confirm received — safe to delete. Both operations share the same connection.
            publisher.deleteFromDlq(plan.sourceQueue(), source);
        }
    }
}
