package dev.plaaxer.dlqsurgeon.cli;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.plaaxer.dlqsurgeon.ai.AiConfig;

/**
 * Builds a concrete {@link ChatLanguageModel} from an {@link AiConfig}.
 *
 * TODO: adding more providers.
 */
public final class ModelFactory {

    private ModelFactory() {}

    /**
     * @throws IllegalArgumentException if the provider in {@code config} is not supported.
     * @throws IllegalStateException    if {@code config} is not configured (missing API key for cloud providers).
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
            case "gemini" -> GoogleAiGeminiChatModel.builder()
                    .apiKey(config.apiKey())
                    .modelName(config.modelName())
                    .build();
            case "openai" -> {
                var builder = OpenAiChatModel.builder()
                        .apiKey(config.apiKey())
                        .modelName(config.modelName());
                if (config.baseUrl() != null) builder.baseUrl(config.baseUrl());
                yield builder.build();
            }
            case "ollama" -> OllamaChatModel.builder()
                    .baseUrl(config.baseUrl())
                    .modelName(config.modelName())
                    .build();
            default -> throw new IllegalArgumentException(
                    "Unsupported AI provider: '" + config.provider() + "'. "
                    + "Set DLQ_AI_PROVIDER to 'anthropic', 'gemini', 'openai', or 'ollama'.");
        };
    }
}