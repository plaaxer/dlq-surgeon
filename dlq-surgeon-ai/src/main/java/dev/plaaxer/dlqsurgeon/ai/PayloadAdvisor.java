package dev.plaaxer.dlqsurgeon.ai;

import dev.plaaxer.dlqsurgeon.model.RabbitMessage;
import dev.plaaxer.dlqsurgeon.model.SuggestionResult;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Analyzes a dead-lettered message and suggests a corrected payload.
 */
public interface PayloadAdvisor {

    /**
     * Analyzes the dead-lettered message and returns an AI-generated suggestion.
     *
     * @param message    The full message as fetched from the DLQ, including x-death metadata.
     * @param schemaFile Path to a JSON Schema file that the fixed payload must satisfy,
     *                   or {@code null} if no schema is available.
     * @return A {@link SuggestionResult} — always non-null; check {@link SuggestionResult#hasSuggestion()}.
     * @throws IOException if the schema file cannot be read.
     */
    SuggestionResult suggest(RabbitMessage message, Path schemaFile) throws IOException;
}
