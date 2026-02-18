package dev.plaaxer.dlqsurgeon.cli;

import dev.plaaxer.dlqsurgeon.model.RabbitMessage;
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

import java.nio.file.Path;
import java.util.concurrent.Callable;

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
        // TODO: Implement this method. Suggested step-by-step:
        //
        //  Step 1 — Fetch messages
        //    MessageFetcher fetcher = new MessageFetcher(connect);
        //    List<RabbitMessage> messages = fetcher.fetch(queueName, count);
        //    if (messages.isEmpty()) { Console.warn("Queue is empty."); return 0; }
        //
        //  Step 2 — Let user pick a message
        //    RabbitMessage selected = MessagePicker.pick(messages);
        //    if (selected == null) return 0; // user quit
        //
        //  Step 3 — Open editor
        //    PayloadEditor editor = new PayloadEditor(schemaFile);
        //    String editedPayload = editor.edit(selected.payload());
        //    // PayloadEditor writes payload to a temp file, opens $EDITOR, reads back the result.
        //
        //  Step 4 — Validate (if schema provided)
        //    if (schemaFile != null) {
        //        SchemaValidator.validate(editedPayload, schemaFile); // throws on failure
        //    }
        //
        //  Step 5 — Build and confirm RepairPlan
        //    RepairPlan plan = RepairPlan.from(selected, editedPayload,
        //                                      targetExchange, targetRoutingKey, stripDeathHeaders);
        //    Console.printPlan(plan);
        //    if (!autoConfirm && !Console.confirm("Proceed?")) return 0;
        //
        //  Step 6 — Re-inject then delete
        //    Reinjector reinjector = new Reinjector(connect);
        //    reinjector.reinjectAndDelete(plan, selected);
        //    Console.success("Message repaired and re-injected.");
        //    return 0;
        //
        //  Catch exceptions and return non-zero exit codes for scripting.
        //
        //  See: MessageFetcher, MessagePicker, PayloadEditor, SchemaValidator,
        //       RepairPlan, Reinjector, Console

        Console.info("fix command — not yet implemented");
        return 0;
    }
}
