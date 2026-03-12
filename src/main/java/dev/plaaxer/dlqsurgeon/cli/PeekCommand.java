package dev.plaaxer.dlqsurgeon.cli;

import dev.plaaxer.dlqsurgeon.model.RabbitMessage;
import dev.plaaxer.dlqsurgeon.surgeon.MessageFetcher;
import dev.plaaxer.dlqsurgeon.tui.Console;
import dev.plaaxer.dlqsurgeon.tui.MessagePicker;

import java.util.List;
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
        MessageFetcher fetcher = new MessageFetcher(connect);
        List<RabbitMessage> messages = fetcher.fetch(queueName, count);

        if (messages.isEmpty()) {
            Console.warn("Queue '" + queueName + "' is empty.");
            return 0;
        }

        RabbitMessage selected = MessagePicker.pick(messages);
        if (selected != null) {
            fetcher.printMessage(selected);
        }

        return 0;
    }
}
