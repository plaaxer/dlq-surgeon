package dev.plaaxer.dlqsurgeon.cli;

import dev.plaaxer.dlqsurgeon.client.ManagementClient;
import dev.plaaxer.dlqsurgeon.model.QueueInfo;
import dev.plaaxer.dlqsurgeon.tui.Console;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * `dlq-surgeon list [QUEUE]`
 *
 * Lists all DLQs visible to the authenticated user (or a single queue's stats).
 * Read-only: never fetches or acks any messages.
 *
 * Output columns: queue name | message count | consumer count | state
 *
 * TODO: Add --pattern flag (glob/regex) to filter queue names.
 * TODO: Add --sort flag (name | messages | consumers).
 * TODO: Support --output json for machine-readable output.
 */
@Command(
        name = "list",
        description = "List DLQs and their message counts.",
        mixinStandardHelpOptions = true
)
public class ListCommand implements Callable<Integer> {

    @Mixin
    ConnectOptions connect;

    @Parameters(
            index = "0",
            paramLabel = "QUEUE",
            description = "Optional queue name to show detailed stats for.",
            arity = "0..1"
    )
    String queueName;

    @Override
    public Integer call() throws Exception {
        ManagementClient client = new ManagementClient(connect);

        if (queueName != null) {
            QueueInfo queue = client.getQueue(queueName);
            printQueue(queue);
        } else {
            List<QueueInfo> queues = client.listQueues();
            Console.dim(String.format("%-50s  %5s  %s", "QUEUE", "MSGS", "TYPE"));
            Console.dim("-".repeat(65));
            for (QueueInfo queue : queues) {
                printQueue(queue);
            }
        }

        return 0;
    }

    private void printQueue(QueueInfo queue) {
        String line = queue.label();
        if (queue.messageCount() > 0) {
            Console.error(line);
        } else {
            Console.success(line);
        }
    }
}