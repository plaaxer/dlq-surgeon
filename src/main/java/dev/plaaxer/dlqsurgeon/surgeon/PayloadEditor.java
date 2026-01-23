package dev.plaaxer.dlqsurgeon.surgeon;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
     *
     * TODO: Implement.
     *   1. Path tmp = Files.createTempFile("dlq-surgeon-", ".json");
     *   2. Pretty-print originalPayload into tmp (use MAPPER.readTree + writerWithDefaultPrettyPrinter).
     *      If the payload is not valid JSON, write it as-is (don't block the user from editing it).
     *   3. String editor = resolveEditor();
     *   4. new ProcessBuilder(editor, tmp.toString())
     *          .inheritIO()
     *          .start()
     *          .waitFor();
     *   5. return Files.readString(tmp);
     *   6. In finally: Files.deleteIfExists(tmp);
     */
    public String edit(String originalPayload) throws IOException, InterruptedException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * TODO: Implement.
     * Resolution order: $VISUAL → $EDITOR → "nano" → "vi" → "notepad" (Windows fallback).
     * Throw a descriptive exception if none is found on PATH.
     */
    private String resolveEditor() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
