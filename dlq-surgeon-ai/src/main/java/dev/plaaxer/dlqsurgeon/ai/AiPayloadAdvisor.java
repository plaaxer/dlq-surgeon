package dev.plaaxer.dlqsurgeon.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.plaaxer.dlqsurgeon.model.RabbitMessage;
import dev.plaaxer.dlqsurgeon.model.SuggestionResult;
import dev.plaaxer.dlqsurgeon.model.XDeathEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@link PayloadAdvisor} implementation backed by any langchain4j {@link ChatLanguageModel}.
 */
public class AiPayloadAdvisor implements PayloadAdvisor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ChatLanguageModel model;

    public AiPayloadAdvisor(ChatLanguageModel model) {
        this.model = model;
    }

    @Override
    public SuggestionResult suggest(RabbitMessage message, Path schemaFile) throws IOException {
        String schemaContent = schemaFile != null ? Files.readString(schemaFile) : null;
        String prompt = buildPrompt(message, schemaContent);
        String raw = model.generate(prompt);
        return parseResponse(raw);
    }

    private static String buildPrompt(RabbitMessage message, String schemaContent) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
                You are a RabbitMQ dead-letter-queue repair assistant.
                A message has been dead-lettered. Your job is to analyze it and suggest a corrected payload.

                Respond with a JSON object that has exactly two fields:
                  "suggestedPayload": the corrected message body as a JSON string (or null if no fix is possible),
                  "explanation": a plain-English explanation of what you think went wrong and what you changed.

                Do not include any text outside the JSON object.

                """);

        sb.append("## Dead-letter metadata\n");
        if (!message.xDeathEntries().isEmpty()) {
            XDeathEntry latest = message.xDeathEntries().getFirst();
            sb.append("- Rejection reason : ").append(latest.reason()).append("\n");
            sb.append("- Original exchange: ").append(latest.exchange()).append("\n");
            sb.append("- Routing key(s)   : ")
              .append(String.join(", ", latest.routingKeys())).append("\n");
            sb.append("- Death count      : ").append(message.xDeathEntries().size()).append("\n");
        } else {
            sb.append("- No x-death entries present (manually dead-lettered or first failure).\n");
        }
        sb.append("- Source queue     : ").append(message.sourceQueue()).append("\n");

        sb.append("\n## Raw payload\n```json\n").append(message.payload()).append("\n```\n");

        if (schemaContent != null) {
            sb.append("\n## Expected JSON Schema\n```json\n").append(schemaContent).append("\n```\n");
        }

        sb.append("""

                Analyze the payload against the metadata (and schema if provided).
                Look for: renamed fields, wrong types, missing required fields with inferable defaults,
                restructured nesting, or any other semantic mismatch that would explain the rejection.
                """);

        return sb.toString();
    }

    private static SuggestionResult parseResponse(String raw) {
        String trimmed = raw.strip();

        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).strip();
            }
        }

        try {
            JsonNode root = MAPPER.readTree(trimmed);
            String suggestedPayload = root.path("suggestedPayload").isNull()
                    ? null
                    : root.path("suggestedPayload").asText(null);
            String explanation = root.path("explanation").asText("(no explanation provided)");
            return new SuggestionResult(suggestedPayload, explanation);
        } catch (Exception e) {
            return new SuggestionResult(null, "AI response could not be parsed: " + trimmed);
        }
    }
}
