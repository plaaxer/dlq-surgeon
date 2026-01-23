package dev.plaaxer.dlqsurgeon;

import dev.plaaxer.dlqsurgeon.cli.FixCommand;
import dev.plaaxer.dlqsurgeon.cli.ListCommand;
import dev.plaaxer.dlqsurgeon.cli.PeekCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * Root Picocli command — the "dlq-surgeon" binary itself.
 *
 * This class owns the top-level --help output and wires together all
 * subcommands. It does not contain any business logic.
 *
 * To add a new subcommand:
 *   1. Create a class in cli/ annotated with @Command.
 *   2. Add it to the subcommands list below.
 *   3. Inject ConnectOptions as a @Mixin for free connection flags.
 */
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

    /**
     * Invoked when the user runs `dlq-surgeon` with no subcommand.
     * Print usage instead of doing nothing silently.
     */
    @Override
    public void run() {
        spec.commandLine().usage(System.err);
    }
}
