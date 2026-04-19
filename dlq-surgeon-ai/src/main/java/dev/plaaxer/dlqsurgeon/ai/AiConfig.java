package dev.plaaxer.dlqsurgeon.ai;

import java.util.function.Function;

/**
 * Captures everything needed to build a {@link dev.langchain4j.model.chat.ChatLanguageModel}
 * without importing any provider SDK.
 *
 * <p>Resolution order for each field: {@code cfg} lookup (e.g. TOML
 * {@code [ai]} table) → env var → built-in default.
 *
 * <p>Config keys / env vars:
 * <ul>
 *   <li>{@code provider}  / {@code DLQ_AI_PROVIDER} — {@code anthropic} (default) | {@code openai} | {@code gemini} | {@code ollama}</li>
 *   <li>{@code api_key}   / {@code ANTHROPIC_API_KEY} · {@code OPENAI_API_KEY} · {@code GEMINI_API_KEY} · {@code DLQ_AI_API_KEY}</li>
 *   <li>{@code model}     / {@code DLQ_AI_MODEL} — optional model-name override</li>
 *   <li>{@code base_url}  / {@code DLQ_AI_BASE_URL} — base URL for local providers (default: {@code http://localhost:11434} for Ollama)</li>
 * </ul>
 */
public record AiConfig(String provider, String apiKey, String modelName, String baseUrl) {

    private static final String DEFAULT_PROVIDER        = "anthropic";
    private static final String DEFAULT_ANTHROPIC_MODEL = "claude-sonnet-4-6";
    private static final String DEFAULT_OPENAI_MODEL    = "gpt-4o";
    private static final String DEFAULT_GEMINI_MODEL    = "gemini-2.0-flash";
    private static final String DEFAULT_OLLAMA_MODEL    = "llama3.2";
    private static final String DEFAULT_OLLAMA_BASE_URL = "http://localhost:11434";

    /** Env-only resolution (useful for tests or contexts without a config file). */
    public static AiConfig fromEnv() {
        return resolve(key -> null);
    }

    /**
     * Resolves an {@code AiConfig} where {@code cfg.apply(key)} is checked first
     * for each field, then the matching env var, then the hard-coded default.
     * Recognized keys: {@code provider}, {@code api_key}, {@code model}, {@code base_url}.
     */
    public static AiConfig resolve(Function<String, String> cfg) {
        String provider  = firstNonBlank(cfg.apply("provider"), System.getenv("DLQ_AI_PROVIDER"), DEFAULT_PROVIDER).toLowerCase();
        String apiKey    = firstNonBlank(cfg.apply("api_key"),  envApiKey(provider));
        String modelName = firstNonBlank(cfg.apply("model"),    System.getenv("DLQ_AI_MODEL"),    defaultModelFor(provider));
        String baseUrl   = firstNonBlank(cfg.apply("base_url"), System.getenv("DLQ_AI_BASE_URL"), defaultBaseUrlFor(provider));
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

    private static String envApiKey(String provider) {
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

    private static String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }
}