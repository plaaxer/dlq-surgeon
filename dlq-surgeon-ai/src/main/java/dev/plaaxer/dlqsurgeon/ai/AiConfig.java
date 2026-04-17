package dev.plaaxer.dlqsurgeon.ai;

/**
 * Captures everything needed to build a {@link dev.langchain4j.model.chat.ChatLanguageModel}
 * without importing any provider SDK.
 *
 * <p>Expected env vars:
 * <ul>
 *   <li>{@code DLQ_AI_PROVIDER} — {@code anthropic} (default) | {@code openai} | {@code gemini} | {@code ollama}</li>
 *   <li>{@code ANTHROPIC_API_KEY} / {@code OPENAI_API_KEY} / {@code GEMINI_API_KEY} — provider API key</li>
 *   <li>{@code DLQ_AI_MODEL} — optional model-name override</li>
 *   <li>{@code DLQ_AI_BASE_URL} — base URL for local providers (default: {@code http://localhost:11434} for Ollama)</li>
 * </ul>
 */
public record AiConfig(String provider, String apiKey, String modelName, String baseUrl) {

    private static final String DEFAULT_PROVIDER        = "anthropic";
    private static final String DEFAULT_ANTHROPIC_MODEL = "claude-sonnet-4-6";
    private static final String DEFAULT_OPENAI_MODEL    = "gpt-4o";
    private static final String DEFAULT_GEMINI_MODEL    = "gemini-2.0-flash";
    private static final String DEFAULT_OLLAMA_MODEL    = "llama3.2";
    private static final String DEFAULT_OLLAMA_BASE_URL = "http://localhost:11434";

    public static AiConfig fromEnv() {
        String provider  = envOrDefault("DLQ_AI_PROVIDER", DEFAULT_PROVIDER).toLowerCase();
        String apiKey    = resolveApiKey(provider);
        String modelName = envOrDefault("DLQ_AI_MODEL", defaultModelFor(provider));
        String baseUrl   = envOrDefault("DLQ_AI_BASE_URL", defaultBaseUrlFor(provider));
        return new AiConfig(provider, apiKey, modelName, baseUrl);
    }

    /** {@code true} if the provider is usable — local providers need no key. */
    public boolean isConfigured() {
        if (isLocal()) return true;
        return apiKey != null && !apiKey.isBlank();
    }

    /** {@code true} for providers that run locally and need no API key. */
    public boolean isLocal() {
        return "ollama".equals(provider);
    }

    public String missingKeyVar() {
        return switch (provider) {
            case "openai"    -> "OPENAI_API_KEY";
            case "anthropic" -> "ANTHROPIC_API_KEY";
            case "gemini"    -> "GEMINI_API_KEY";
            default          -> "DLQ_AI_API_KEY";
        };
    }

    private static String resolveApiKey(String provider) {
        return switch (provider) {
            case "anthropic" -> System.getenv("ANTHROPIC_API_KEY");
            case "openai"    -> System.getenv("OPENAI_API_KEY");
            case "gemini"    -> System.getenv("GEMINI_API_KEY");
            default          -> System.getenv("DLQ_AI_API_KEY");
        };
    }

    private static String defaultModelFor(String provider) {
        return switch (provider) {
            case "openai"  -> DEFAULT_OPENAI_MODEL;
            case "gemini"  -> DEFAULT_GEMINI_MODEL;
            case "ollama"  -> DEFAULT_OLLAMA_MODEL;
            default        -> DEFAULT_ANTHROPIC_MODEL;
        };
    }

    private static String defaultBaseUrlFor(String provider) {
        return "ollama".equals(provider) ? DEFAULT_OLLAMA_BASE_URL : null;
    }

    private static String envOrDefault(String name, String fallback) {
        String val = System.getenv(name);
        return (val != null && !val.isBlank()) ? val : fallback;
    }
}