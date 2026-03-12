package dev.plaaxer.dlqsurgeon.cli;

import dev.plaaxer.dlqsurgeon.surgeon.MessageFetcher;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/**
 * `dlq-surgeon list [QUEUE]`
 *
 * Lists all DLQs visible to the authenticated user (or a single queue's stats).
 * Read-only: never fetches or acks any messages.
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
        connect.printConnectionInfo();
        MessageFetcher fetcher = new MessageFetcher(connect);

        if (queueName != null) {
            fetcher.printQueue(fetcher.getQueue(queueName));
        } else {
            fetcher.printQueueList(fetcher.listQueues());
        }

        return 0;
    }
}