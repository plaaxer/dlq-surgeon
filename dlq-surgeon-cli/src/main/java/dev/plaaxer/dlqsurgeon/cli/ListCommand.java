package dev.plaaxer.dlqsurgeon.cli;

import dev.plaaxer.dlqsurgeon.model.QueueInfo;
import dev.plaaxer.dlqsurgeon.service.MessageFetcher;
import dev.plaaxer.dlqsurgeon.tui.Console;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

/**
 * `dlq-surgeon list [QUEUE]`
 *
 * Lists all DLQs visible to the authenticated user (or a single queue's stats).
 * Read-only: never fetches or acks any messages.
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

    @Option(
            names = {"--pattern"},
            description = "Glob filter on queue names (e.g. '*dlq*', 'orders.*'). Case-insensitive."
    )
    String pattern;

    @Option(
            names = {"--sort"},
            description = "Sort by: name (default), messages, consumers.",
            defaultValue = "name"
    )
    String sort;

    @Option(
            names = {"--output", "-o"},
            description = "Output format: json. Omit for default table output.",
            paramLabel = "FORMAT"
    )
    String outputFormat;

    @Override
    public Integer call() throws Exception {
        connect.printConnectionInfo();
        MessageFetcher fetcher = new MessageFetcher(connect.toConnectionConfig());
        MessagePresenter presenter = new MessagePresenter();

        if (queueName != null) {
            QueueInfo queue = fetcher.getQueue(queueName);
            if ("json".equalsIgnoreCase(outputFormat)) {
                presenter.printQueuesJson(List.of(queue));
            } else {
                presenter.printQueue(queue);
            }
            return 0;
        }

        List<QueueInfo> queues = fetcher.listQueues();

        if (pattern != null) {
            Pattern regex = globToPattern(pattern);
            queues = queues.stream()
                    .filter(q -> regex.matcher(q.name()).matches())
                    .toList();
        }

        queues = queues.stream().sorted(comparatorFor(sort)).toList();

        if (queues.isEmpty()) {
            Console.warn("No queues matched the pattern.");
            return 0;
        }

        if ("json".equalsIgnoreCase(outputFormat)) {
            presenter.printQueuesJson(queues);
        } else {
            presenter.printQueueList(queues);
        }

        return 0;
    }

    private static Pattern globToPattern(String glob) {
        StringBuilder sb = new StringBuilder("(?i)");
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*' -> sb.append(".*");
                case '?' -> sb.append('.');
                default  -> sb.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return Pattern.compile(sb.toString());
    }

    private static Comparator<QueueInfo> comparatorFor(String sort) {
        return switch (sort.toLowerCase()) {
            case "messages"  -> Comparator.comparingInt(QueueInfo::messageCount).reversed();
            case "consumers" -> Comparator.comparingInt(QueueInfo::consumers).reversed();
            default          -> Comparator.comparing(QueueInfo::name);
        };
    }
}