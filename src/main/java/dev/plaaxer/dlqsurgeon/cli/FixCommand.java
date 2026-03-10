package dev.plaaxer.dlqsurgeon.cli;

import dev.plaaxer.dlqsurgeon.model.RabbitMessage;
import dev.plaaxer.dlqsurgeon.model.RepairPlan;
import dev.plaaxer.dlqsurgeon.surgeon.MessageFetcher;
import dev.plaaxer.dlqsurgeon.surgeon.PayloadEditor;
import dev.plaaxer.dlqsurgeon.surgeon.Reinjector;
import dev.plaaxer.dlqsurgeon.surgeon.SchemaValidator;
import dev.plaaxer.dlqsurgeon.tui.Console;
import dev.plaaxer.dlqsurgeon.tui.MessagePicker;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

/**
 * `dlq-surgeon fix QUEUE`
 *
 * The core surgical workflow:
 *   1. Fetch messages from the DLQ (held in memory only).
 *   2. User picks which message to repair.
 *   3. Payload opens in $EDITOR (or a built-in fallback editor).
 *   4. On save, payload is validated against JSON Schema (if --schema given).
 *   5. After confirmation, the repaired message is published to the original
 *      exchange+routing-key with all original headers preserved.
 *   6. Only after a publisher confirm is received is the source message deleted
 *      from the DLQ. If publish fails, nothing is deleted.
 *
 * Safety invariants:
 *   - Never delete from DLQ before confirming successful re-injection.
 *   - Never modify message metadata (x-death, message-id, correlation-id, etc.)
 *     unless the user explicitly passes --strip-death-headers.
 *   - Always print the RepairPlan and ask for confirmation before acting,
 *     unless --yes is passed.
 */
@Command(
        name = "fix",
        description = "Interactively repair and re-inject a dead-lettered message.",
        mixinStandardHelpOptions = true
)
public class FixCommand implements Callable<Integer> {

    @Mixin
    ConnectOptions connect;

    @Parameters(index = "0", paramLabel = "QUEUE", description = "DLQ name to repair from.")
    String queueName;

    @Option(
            names = {"--count", "-n"},
            description = "Number of messages to load into the picker (default: ${DEFAULT-VALUE}).",
            defaultValue = "20"
    )
    int count;

    @Option(
            names = {"--schema", "-s"},
            description = "Path to a JSON Schema file to validate the edited payload against.",
            paramLabel = "FILE"
    )
    Path schemaFile;

    @Option(
            names = {"--target-exchange"},
            description = "Override the re-injection exchange (default: read from x-death headers).",
            paramLabel = "EXCHANGE"
    )
    String targetExchange;

    @Option(
            names = {"--target-routing-key"},
            description = "Override the routing key (default: read from x-death headers).",
            paramLabel = "KEY"
    )
    String targetRoutingKey;

    @Option(
            names = {"--strip-death-headers"},
            description = "Remove x-death and x-first-death-* headers before re-injection.",
            defaultValue = "false"
    )
    boolean stripDeathHeaders;

    @Option(
            names = {"--yes", "-y"},
            description = "Skip confirmation prompts (use with caution in scripts).",
            defaultValue = "false"
    )
    boolean autoConfirm;

    @Override
    public Integer call() throws Exception {

        if (connect.readOnly) {
            Console.error("Cannot run 'fix' in --read-only mode.");
            return 1;
        }

        MessageFetcher fetcher = new MessageFetcher(connect);
        List<RabbitMessage> messages;
        try {
            messages = fetcher.fetch(queueName, count);
        } catch (Exception e) {
            Console.error("Failed to fetch messages from '" + queueName + "': " + e.getMessage());
            return 1;
        }
        if (messages.isEmpty()) {
            Console.warn("Queue '" + queueName + "' is empty.");
            return 0;
        }

        RabbitMessage selected = MessagePicker.pick(messages);
        if (selected == null) return 0; // user quit

        PayloadEditor editor = new PayloadEditor(schemaFile);
        String editedPayload = editor.edit(selected.payload());

        while (schemaFile != null) {
            try {
                SchemaValidator.validate(editedPayload, schemaFile);
                break; // valid — proceed
            } catch (SchemaValidator.ValidationException e) {
                Console.error("Validation failed:\n" + e.formatErrors());
                String choice = promptValidationChoice();
                if ("e".equals(choice)) {
                    editedPayload = editor.edit(editedPayload);
                } else if ("s".equals(choice)) {
                    Console.warn("Skipping schema validation.");
                    break;
                } else {
                    Console.info("Aborted.");
                    return 0;
                }
            }
        }

        RepairPlan plan = RepairPlan.from(selected, editedPayload, targetExchange, targetRoutingKey, stripDeathHeaders);
        Console.printPlan(plan.summary());
        if (!autoConfirm && !Console.confirm("Proceed with re-injection?")) return 0;

        try {
            new Reinjector(connect).reinjectAndDelete(plan, selected);
        } catch (TimeoutException e) {
            Console.error("Timed out waiting for broker confirmation. The message was NOT re-injected and nothing was deleted from the DLQ.");
            return 1;
        } catch (IOException e) {
            Console.error("Broker rejected the message: " + e.getMessage() + ". Nothing was deleted from the DLQ.");
            return 1;
        }

        Console.success("Message repaired and re-injected successfully.");
        return 0;
    }

    private String promptValidationChoice() {
        System.out.print("  [e] re-open editor  [s] skip validation  [a] abort: ");
        String answer = System.console() != null ? System.console().readLine() : "a";
        if (answer == null) return "a";
        return answer.trim().toLowerCase();
    }
}
