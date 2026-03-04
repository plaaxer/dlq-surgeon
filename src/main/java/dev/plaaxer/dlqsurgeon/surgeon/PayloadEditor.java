package dev.plaaxer.dlqsurgeon.surgeon;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Opens the message payload in the user's preferred editor and returns the edited result.
 *
 * Strategy (same as git commit -m):
 *   1. Pretty-print the payload to a temp file in /tmp/dlq-surgeon-*.json.
 *   2. Resolve the editor: $VISUAL → $EDITOR → "nano" → "vi" (first found on PATH).
 *   3. exec() the editor process, inheriting stdin/stdout/stderr so the user gets
 *      a full interactive session.
 *   4. After the editor exits, read the file back and return the content.
 *   5. Delete the temp file in a finally block.
 *
 * If a schemaFile was provided, validate after reading back. If validation fails,
 * offer the user a choice: re-open the editor, skip validation, or abort.
 *
 * TODO: For the native binary, ensure the editor path is resolved from the user's
 *       actual PATH rather than a hardcoded list. ProcessBuilder inherits the
 *       environment by default, so this should work out of the box.
 *
 * TODO: Consider adding a diff view after editing (show original vs edited)
 *       before the confirmation prompt in FixCommand.
 */
public class PayloadEditor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path schemaFile;

    public PayloadEditor(Path schemaFile) {
        this.schemaFile = schemaFile; // may be null if no schema was supplied
    }

    /**
     * Opens the payload in the user's editor and returns the (possibly modified) content.
     *
     * @param originalPayload The raw payload string from the fetched message.
     * @return The content of the file after the editor exits.
     * @throws IOException          on file or process I/O errors.
     * @throws InterruptedException if the editor process is interrupted.
     */
    public String edit(String originalPayload) throws IOException, InterruptedException {
        Path tmp = Files.createTempFile("dlq-surgeon-", ".json");
        try {
            String prettyPayload;
            try {
                prettyPayload = MAPPER.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(MAPPER.readTree(originalPayload));
            } catch (Exception e) {
                prettyPayload = originalPayload; // not valid JSON — write as-is
            }
            Files.writeString(tmp, prettyPayload);

            String editor = resolveEditor();
            int exitCode = new ProcessBuilder(editor, tmp.toString())
                    .inheritIO()
                    .start()
                    .waitFor();
            if (exitCode != 0) {
                throw new IOException("Editor exited with code " + exitCode);
            }

            return Files.readString(tmp);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /**
     * Resolves the editor to use by trying candidates in order and returning
     * the first one found on PATH.
     *
     * Resolution order: $VISUAL → $EDITOR → "nano" → "vi" → "notepad" (Windows fallback).
     * Throws if none is available.
     */
    private String resolveEditor() {
        List<String> candidates = new java.util.ArrayList<>();

        String visual = System.getenv("VISUAL");
        if (visual != null && !visual.isBlank()) candidates.add(visual);

        String editor = System.getenv("EDITOR");
        if (editor != null && !editor.isBlank()) candidates.add(editor);

        candidates.addAll(List.of("nano", "vi", "notepad"));

        for (String candidate : candidates) {
            // Use `which`/`where` to probe PATH without launching the editor itself.
            String probe = System.getProperty("os.name", "").toLowerCase().contains("win")
                    ? "where" : "which";
            try {
                int result = new ProcessBuilder(probe, candidate)
                        .redirectErrorStream(true)
                        .start()
                        .waitFor();
                if (result == 0) return candidate;
            } catch (Exception ignored) {}
        }

        throw new IllegalStateException(
                "No editor found. Set $VISUAL or $EDITOR, or install nano/vi.");
    }
}
