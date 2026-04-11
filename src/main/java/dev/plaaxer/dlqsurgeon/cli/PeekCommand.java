package dev.plaaxer.dlqsurgeon.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 */
@Command(
        name = "peek",
        description = "Inspect DLQ messages without modifying them.",
        mixinStandardHelpOptions = true
)
public class PeekCommand implements Callable<Integer> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    @Option(
            names = {"--filter", "-f"},
            description = "Filter messages by jq-style JSON path (e.g. .status or .status=FAILED). Non-JSON payloads are excluded when a filter is set."
    )
    String filter;

    @Option(
            names = {"--output", "-o"},
            description = "Output format: json or table. Omit for interactive picker.",
            paramLabel = "FORMAT"
    )
    String outputFormat;

    @Override
    public Integer call() throws Exception {
        connect.printConnectionInfo();
        MessageFetcher fetcher = new MessageFetcher(connect);
        List<RabbitMessage> messages = fetcher.fetch(queueName, count);

        if (messages.isEmpty()) {
            Console.warn("Queue '" + queueName + "' is empty.");
            return 0;
        }

        List<RabbitMessage> filtered = applyFilter(messages);

        if (filtered.isEmpty()) {
            Console.warn("No messages matched the filter.");
            return 0;
        }

        if ("json".equalsIgnoreCase(outputFormat)) {
            fetcher.printMessagesJson(filtered);
        } else if ("table".equalsIgnoreCase(outputFormat)) {
            fetcher.printMessagesTable(filtered);
        } else {
            RabbitMessage selected = MessagePicker.pick(filtered);
            if (selected != null) {
                fetcher.printMessage(selected);
            }
        }

        return 0;
    }

    private List<RabbitMessage> applyFilter(List<RabbitMessage> messages) {
        if (filter == null) return messages;

        int eqIdx = filter.indexOf('=');
        String path = eqIdx >= 0 ? filter.substring(0, eqIdx) : filter;
        String expectedValue = eqIdx >= 0 ? filter.substring(eqIdx + 1) : null;

        // ".a.b.c" → ["a", "b", "c"]
        String stripped = path.startsWith(".") ? path.substring(1) : path;
        String[] parts = stripped.isEmpty() ? new String[0] : stripped.split("\\.");

        return messages.stream()
                .filter(msg -> matchesFilter(msg, parts, expectedValue))
                .toList();
    }

    private boolean matchesFilter(RabbitMessage msg, String[] pathParts, String expectedValue) {
        try {
            JsonNode node = MAPPER.readTree(msg.payload());
            for (String part : pathParts) {
                node = node.get(part);
                if (node == null) return false;
            }
            return expectedValue == null || expectedValue.equals(node.asText());
        } catch (Exception e) {
            return false;
        }
    }
}
