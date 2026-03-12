package dev.plaaxer.dlqsurgeon;

import dev.plaaxer.dlqsurgeon.cli.FixCommand;
import dev.plaaxer.dlqsurgeon.cli.ListCommand;
import dev.plaaxer.dlqsurgeon.cli.PeekCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
        name = "dlq-surgeon",
        mixinStandardHelpOptions = true,
        version = "dlq-surgeon 1.0-SNAPSHOT",
        description = {
                "",
                "  A sole-purpose sidecar scalpel for RabbitMQ Dead Letter Queues.",
                "  Fetch → edit → validate → re-inject. Stateless. No data loss.",
                ""
        },
        subcommands = {
                ListCommand.class,
                PeekCommand.class,
                FixCommand.class,
        }
)
public class DlqSurgeon implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.err);
    }
}
