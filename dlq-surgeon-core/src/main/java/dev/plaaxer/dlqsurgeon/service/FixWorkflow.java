package dev.plaaxer.dlqsurgeon.service;

import dev.plaaxer.dlqsurgeon.config.ConnectionConfig;
import dev.plaaxer.dlqsurgeon.model.RabbitMessage;
import dev.plaaxer.dlqsurgeon.model.RepairPlan;
import dev.plaaxer.dlqsurgeon.model.SuggestionResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Orchestrates the full repair workflow: fetch → edit → validate → reinject.
 *
 * <p>All user-interaction (picking, editing, prompting) is delegated to {@link Hooks}
 * so this class has no dependency on any TUI or CLI module.
 */
public final class FixWorkflow {

    public record Options(
            ConnectionConfig connection,
            String queue,
            int count,
            Path schemaFile,
            String targetExchange,
            String targetRoutingKey,
            boolean stripDeathHeaders,
            boolean suggest
    ) {}

    public enum ValidationChoice { RE_EDIT, SKIP, ABORT }

    public interface Hooks {
        /** Return {@code null} to cancel. */
        RabbitMessage pick(List<RabbitMessage> messages) throws Exception;
        String edit(String payload) throws Exception;
        /** Called only when a schema is configured and validation fails. */
        ValidationChoice onValidationFailure(String formattedErrors);
        boolean confirm(RepairPlan plan);
        SuggestionResult aiSuggest(RabbitMessage message, Path schemaFile) throws IOException;
    }

    private FixWorkflow() {}

    /**
     * Runs the workflow end-to-end.
     *
     * @return 0 on success or user-initiated cancel; 1 if the queue is empty.
     * @throws Exception on fetch, validation I/O, or reinject errors — caller handles display.
     */
    public static int run(Options opts, Hooks hooks) throws Exception {
        MessageFetcher fetcher = new MessageFetcher(opts.connection());
        List<RabbitMessage> messages = fetcher.fetch(opts.queue(), opts.count());

        if (messages.isEmpty()) return 1;

        RabbitMessage selected = hooks.pick(messages);
        if (selected == null) return 0;

        PayloadEditor editor = new PayloadEditor(opts.schemaFile());

        if (opts.suggest()) {
            SuggestionResult suggestionResult = hooks.aiSuggest(selected, opts.schemaFile());
            // todo: continue flow
        }

        String payload = editor.edit(selected.payload());

        while (opts.schemaFile() != null) {
            try {
                SchemaValidator.validate(payload, opts.schemaFile());
                break;
            } catch (SchemaValidator.ValidationException e) {
                ValidationChoice choice = hooks.onValidationFailure(e.formatErrors());
                if (choice == ValidationChoice.RE_EDIT) {
                    payload = editor.edit(payload);
                } else if (choice == ValidationChoice.SKIP) {
                    break;
                } else {
                    return 0;
                }
            }
        }

        RepairPlan plan = RepairPlan.from(selected, payload, opts.targetExchange(), opts.targetRoutingKey(), opts.stripDeathHeaders());
        if (!hooks.confirm(plan)) return 0;

        new Reinjector(opts.connection()).reinjectAndDelete(plan, selected);
        return 0;
    }
}