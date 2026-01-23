package dev.plaaxer.dlqsurgeon.cli;

import dev.plaaxer.dlqsurgeon.client.ManagementClient;
import dev.plaaxer.dlqsurgeon.tui.Console;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

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
        // TODO: Implement this method.
        //
        //  1. Build a ManagementClient from connect options.
        //  2. If queueName is null, call managementClient.listQueues() and print a table.
        //     If queueName is set, call managementClient.getQueue(queueName) and print details.
        //  3. Use Console for coloured output (red if messages > 0, green if 0).
        //  4. Return 0 on success, non-zero on error.
        //
        //  See: ManagementClient, Console
        Console.info("list command — not yet implemented");
        return 0;
    }
}
