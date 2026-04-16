package dev.plaaxer.dlqsurgeon.cli;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.plaaxer.dlqsurgeon.ai.AiConfig;

/**
 * Builds a concrete {@link ChatLanguageModel} from an {@link AiConfig}.
 *
 * <p>This is the only place in the codebase that imports a provider SDK directly.
 * Adding a new provider means adding a case here (and its dep to the CLI pom) — nothing else changes.
 */
public final class ModelFactory {

    private ModelFactory() {}

    /**
     * @throws IllegalArgumentException if the provider in {@code config} is not supported.
     * @throws IllegalStateException    if {@code config} is not configured (missing API key).
     */
    public static ChatLanguageModel build(AiConfig config) {
        if (!config.isConfigured()) {
            throw new IllegalStateException(
                    "AI provider '" + config.provider() + "' requires " + config.missingKeyVar() + " to be set.");
        }

        return switch (config.provider()) {
            case "anthropic" -> AnthropicChatModel.builder()
                    .apiKey(config.apiKey())
                    .modelName(config.modelName())
                    .build();
            default -> throw new IllegalArgumentException(
                    "Unsupported AI provider: '" + config.provider() + "'. Set DLQ_AI_PROVIDER to 'anthropic'.");
        };
    }
}