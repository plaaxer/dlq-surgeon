package dev.plaaxer.dlqsurgeon.ai;

/**
 * Captures everything needed to build a {@link dev.langchain4j.model.chat.ChatLanguageModel}
 * without importing any provider SDK.
 *
 * <p>Reading env vars here keeps provider-wiring logic out of the CLI command and out of
 * the AI module itself (which stays on {@code langchain4j-core} only).
 *
 * <p>Expected env vars:
 * <ul>
 *   <li>{@code DLQ_AI_PROVIDER} — {@code anthropic} (default) | {@code openai} | …</li>
 *   <li>{@code ANTHROPIC_API_KEY} / {@code OPENAI_API_KEY} — provider API key</li>
 *   <li>{@code DLQ_AI_MODEL} — optional model-name override (e.g. {@code claude-opus-4-6})</li>
 * </ul>
 *
 * <p>The concrete {@code ChatLanguageModel} instantiation belongs in the CLI/MCP module
 * (wherever the provider SDK dep lives), not here.
 */
public record AiConfig(String provider, String apiKey, String modelName) {

    private static final String DEFAULT_PROVIDER = "anthropic";
    private static final String DEFAULT_ANTHROPIC_MODEL = "claude-sonnet-4-6";
    private static final String DEFAULT_OPENAI_MODEL    = "gpt-4o";

    /**
     * Builds an {@link AiConfig} by reading the standard environment variables.
     * Never returns {@code null} — check {@link #isConfigured()} before building a model.
     */
    public static AiConfig fromEnv() {
        String provider  = envOrDefault("DLQ_AI_PROVIDER", DEFAULT_PROVIDER).toLowerCase();
        String apiKey    = resolveApiKey(provider);
        String modelName = envOrDefault("DLQ_AI_MODEL", defaultModelFor(provider));
        return new AiConfig(provider, apiKey, modelName);
    }

    /** {@code true} if an API key is present — use this to gate the --suggest path. */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Human-readable hint shown when the key is missing. */
    public String missingKeyVar() {
        return switch (provider) {
            case "openai"    -> "OPENAI_API_KEY";
            case "anthropic" -> "ANTHROPIC_API_KEY";
            default          -> "DLQ_AI_API_KEY";
        };
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static String resolveApiKey(String provider) {
        return switch (provider) {
            case "anthropic" -> System.getenv("ANTHROPIC_API_KEY");
            case "openai"    -> System.getenv("OPENAI_API_KEY");
            default          -> System.getenv("DLQ_AI_API_KEY");
        };
    }

    private static String defaultModelFor(String provider) {
        return switch (provider) {
            case "openai" -> DEFAULT_OPENAI_MODEL;
            default       -> DEFAULT_ANTHROPIC_MODEL;
        };
    }

    private static String envOrDefault(String name, String fallback) {
        String val = System.getenv(name);
        return (val != null && !val.isBlank()) ? val : fallback;
    }
}