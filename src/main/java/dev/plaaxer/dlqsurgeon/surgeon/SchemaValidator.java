package dev.plaaxer.dlqsurgeon.surgeon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Validates a JSON payload against a JSON Schema file before re-injection.
 *
 * Supports JSON Schema drafts: 04, 06, 07, 2019-09, 2020-12.
 * The draft version is auto-detected from the "$schema" field in the schema file.
 *
 * If validation fails, the errors are printed to the console and a
 * ValidationException is thrown. The caller (FixCommand) catches this and
 * offers the user the option to re-open the editor or abort.
 *
 * TODO: Add support for loading schemas from a remote URL ($ref with http/https).
 * TODO: Cache parsed JsonSchema objects — parsing is expensive and the schema
 *       doesn't change within a session.
 */
public class SchemaValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Validates {@code payload} against the schema at {@code schemaFile}.
     *
     * @throws ValidationException if any validation errors are found.
     * @throws IOException         if the schema file cannot be read.
     *
     * TODO: Implement.
     *   1. Read schemaFile into a String and parse with MAPPER.readTree().
     *   2. Detect draft version from the "$schema" URI in the root node.
     *      Use JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012) as default.
     *   3. JsonSchema schema = factory.getSchema(schemaNode);
     *   4. JsonNode payloadNode = MAPPER.readTree(payload);
     *   5. Set<ValidationMessage> errors = schema.validate(payloadNode);
     *   6. If errors is not empty, format them nicely and throw ValidationException.
     */
    public static void validate(String payload, Path schemaFile) throws IOException, ValidationException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Checked exception thrown when the payload fails schema validation.
     * Contains the raw set of ValidationMessage objects for detailed reporting.
     */
    public static class ValidationException extends Exception {

        private final Set<ValidationMessage> errors;

        public ValidationException(Set<ValidationMessage> errors) {
            super("Payload failed JSON Schema validation (" + errors.size() + " error(s))");
            this.errors = errors;
        }

        public Set<ValidationMessage> getErrors() {
            return errors;
        }

        /**
         * Returns a human-readable multi-line summary of all validation errors.
         */
        public String formatErrors() {
            StringBuilder sb = new StringBuilder();
            int i = 1;
            for (ValidationMessage msg : errors) {
                sb.append(String.format("  %d. [%s] %s%n", i++, msg.getType(), msg.getMessage()));
            }
            return sb.toString();
        }
    }
}
