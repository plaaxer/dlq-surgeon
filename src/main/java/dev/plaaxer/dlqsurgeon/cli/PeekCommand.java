package dev.plaaxer.dlqsurgeon.cli;

import dev.plaaxer.dlqsurgeon.surgeon.MessageFetcher;
import dev.plaaxer.dlqsurgeon.tui.Console;
import dev.plaaxer.dlqsurgeon.tui.MessagePicker;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/**
 * `dlq-surgeon peek QUEUE`
 *
 * Fetches messages from the DLQ and displays them — with x-death metadata,
 * failure reasons, original exchange/routing-key, and the full payload.
 *
 * This command is intentionally READ-ONLY: it never publishes or acks anything.
 * Use `fix` to make edits and re-inject.
 *
 * Key design note: The Management HTTP API /api/queues/.../get endpoint is used
 * here (not AMQP). This requires us to re-enqueue messages after fetching (the
 * API will ack them unless requeue=true is set). MessageFetcher handles this.
 *
 * TODO: Add --filter flag to grep payload content (jq-style path expression).
 * TODO: Add --output json|table flag.
 */
@Command(
        name = "peek",
        description = "Inspect DLQ messages without modifying them.",
        mixinStandardHelpOptions = true
)
public class PeekCommand implements Callable<Integer> {

    @Mixin
    ConnectOptions connect;

    @Parameters(index = "0", paramLabel = "QUEUE", description = "DLQ name to inspect.")
    String queueName;

    @Option(
            names = {"--count", "-n"},
            description = "Number of messages to fetch (default: ${DEFAULT-VALUE}).",
            defaultValue = "10"
    )
    int count;

    @Override
    public Integer call() throws Exception {
        // TODO: Implement this method.
        //
        //  1. Build ManagementClient from connect options.
        //  2. Call MessageFetcher.fetch(queueName, count) — returns List<DeadLetteredMessage>.
        //  3. Pass messages to MessagePicker.show() for the interactive list.
        //     When the user selects a message, display its full detail view:
        //       - x-death entries (original exchange, routing key, death count, reason)
        //       - Headers (prettified)
        //       - Payload (syntax-highlighted JSON if applicable)
        //  4. Allow the user to press 'q' to quit or 'f' to jump to `fix` for that message.
        //
        //  See: MessageFetcher, MessagePicker, Console, DeadLetteredMessage
        Console.info("peek command — not yet implemented");
        return 0;
    }
}
