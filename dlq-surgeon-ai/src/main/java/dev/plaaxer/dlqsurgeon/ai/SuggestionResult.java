package dev.plaaxer.dlqsurgeon.ai;

/**
 * The result of an AI-assisted payload analysis.
 *
 * @param suggestedPayload  The corrected payload as a JSON string, or null if the model
 *                          could not produce a well-formed suggestion.
 * @param explanation       Plain-English description of what the model thinks went wrong
 *                          and what it changed (always non-null).
 */
public record SuggestionResult(String suggestedPayload, String explanation) {

    /** true if non-blank suggested payload. */
    public boolean hasSuggestion() {
        return suggestedPayload != null && !suggestedPayload.isBlank();
    }
}
