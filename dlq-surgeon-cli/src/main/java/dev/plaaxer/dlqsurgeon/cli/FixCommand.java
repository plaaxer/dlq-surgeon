package dev.plaaxer.dlqsurgeon.cli;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.plaaxer.dlqsurgeon.ai.AiConfig;
import dev.plaaxer.dlqsurgeon.ai.AiPayloadAdvisor;
import dev.plaaxer.dlqsurgeon.model.RabbitMessage;
import dev.plaaxer.dlqsurgeon.model.RepairPlan;
import dev.plaaxer.dlqsurgeon.model.SuggestionResult;
import dev.plaaxer.dlqsurgeon.service.FixWorkflow;
import dev.plaaxer.dlqsurgeon.service.PayloadEditor;
import dev.plaaxer.dlqsurgeon.tui.Console;
import dev.plaaxer.dlqsurgeon.tui.MessagePicker;
import dev.plaaxer.dlqsurgeon.tui.SuggestionView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

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
 *   3. Payload opens in $EDITOR.
 *   4. On save, payload is validated against JSON Schema (if --schema given).
 *   5. After confirmation, the repaired message is published to the original
 *      exchange+routing-key with all original headers preserved.
 *   6. Only after a publisher confirm is received is the source message deleted
 *      from the DLQ. If publish fails, nothing is deleted.
 */
@Command(
        name = "fix",
        description = "Interactively repair and re-inject a dead-lettered message.",
        mixinStandardHelpOptions = true
)
public class FixCommand implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(FixCommand.class);

    @Mixin
    ConnectOptions connect;

    @Spec
    CommandSpec spec;

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

    @Option(
            names = {"--suggest"},
            description = "Enable AI suggestions for this fix. Can also be set in the config profile as: suggest = true.",
            defaultValue = "false"
    )
    boolean suggest;

    @Override
    public Integer call() throws Exception {
        connect.printConnectionInfo();

        if (connect.readOnly) {
            Console.error("Cannot run 'fix' in --read-only mode.");
            return 1;
        }

        if (suggest && schemaFile == null) {
            Console.warn("--suggest works best with --schema: the AI uses the schema to understand the expected payload shape.");
        }

        var opts = new FixWorkflow.Options(
                connect.toConnectionConfig(), queueName, count, schemaFile,
                targetExchange, targetRoutingKey, stripDeathHeaders, suggest);

        try {
            int result = FixWorkflow.run(opts, new FixWorkflow.Hooks() {
                final PayloadEditor editor = new PayloadEditor(schemaFile);

                public RabbitMessage pick(List<RabbitMessage> messages) throws Exception {
                    return MessagePicker.pick(messages);
                }

                public String edit(String payload) throws Exception {
                    return editor.edit(payload);
                }

                public FixWorkflow.ValidationChoice onValidationFailure(String errors) {
                    Console.error("Validation failed:\n" + errors);
                    return switch (promptValidationChoice()) {
                        case "e" -> FixWorkflow.ValidationChoice.RE_EDIT;
                        case "s" -> { Console.warn("Skipping schema validation."); yield FixWorkflow.ValidationChoice.SKIP; }
                        default  -> { Console.info("Aborted."); yield FixWorkflow.ValidationChoice.ABORT; }
                    };
                }

                public boolean confirm(RepairPlan plan) {
                    Console.printPlan(plan);
                    return autoConfirm || Console.confirm("Proceed with re-injection?");
                }

                public SuggestionResult aiSuggest(RabbitMessage msg, Path schemaFile) {
                    log.debug("AI suggestion called for message {}", msg.messageNumber());

                    if (!suggest) {
                        throw new IllegalStateException("AI suggestion should not be called under this context. Suggestion not needed");
                    }

                    try {
                        ChatLanguageModel chatModel = ModelFactory.build(resolveAiConfig());
                        AiPayloadAdvisor aiAdvisor = new AiPayloadAdvisor(chatModel);
                        return aiAdvisor.suggest(msg, schemaFile);
                    } catch (IOException e) {
                        log.warn("AI suggestion failed", e);
                        return new SuggestionResult(null,
                                "AI suggestion unavailable: " + e.getMessage()
                                        + "\nYou can still edit the original payload manually.");
                    }
                }

                public FixWorkflow.SuggestionChoice onSuggestion(SuggestionResult result, String originalPayload) {
                    SuggestionView.render(result, originalPayload);
                    return SuggestionView.prompt(result.hasSuggestion());
                }
            });
            if (result == 1) Console.warn("Queue '" + queueName + "' is empty.");
            else Console.success("Message repaired and re-injected successfully.");
            return result;
        } catch (TimeoutException e) {
            Console.error("Timed out waiting for broker confirmation. The message was NOT re-injected and nothing was deleted from the DLQ.");
            return 1;
        } catch (IOException e) {
            Console.error("Broker rejected the message: " + e.getMessage() + ". Nothing was deleted from the DLQ.");
            return 1;
        } catch (Exception e) {
            Console.error("Failed to fix messages: " + e.getMessage());
            return 1;
        }
    }

    private AiConfig resolveAiConfig() {
        var provider = spec.commandLine().getDefaultValueProvider();
        if (provider instanceof ProfileConfigProvider p) {
            return AiConfig.resolve(p.configFile()::getAi);
        }
        return AiConfig.fromEnv();
    }

    private String promptValidationChoice() {
        System.out.print("  [e] re-open editor  [s] skip validation  [a] abort: ");
        String answer = System.console() != null ? System.console().readLine() : "a";
        if (answer == null) return "a";
        return answer.trim().toLowerCase();
    }
}
