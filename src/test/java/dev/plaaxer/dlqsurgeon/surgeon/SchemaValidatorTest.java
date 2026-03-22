package dev.plaaxer.dlqsurgeon.surgeon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 *  - valid payload against a simple schema → no exception.
 *  - missing required field → ValidationException with 1 error.
 *  - wrong type (string where number expected) → ValidationException.
 *  - additional properties disallowed → ValidationException.
 *  - invalid JSON payload → IOException or parsing error (not a silent pass).
 *  - Draft-04 schema is accepted.
 *  - Draft-2020-12 schema is accepted.
 */
class SchemaValidatorTest {

    private static final String SIMPLE_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "required": ["orderId", "currencyCode"],
              "properties": {
                "orderId":      { "type": "integer" },
                "currencyCode": { "type": "string", "minLength": 3, "maxLength": 3 }
              }
            }
            """;

    private static final String STRICT_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "required": ["orderId"],
              "properties": {
                "orderId": { "type": "integer" }
              },
              "additionalProperties": false
            }
            """;

    private static final String DRAFT04_SCHEMA = """
            {
              "$schema": "http://json-schema.org/draft-04/schema#",
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": { "type": "string" }
              }
            }
            """;

    @Test
    void validPayloadPassesValidation(@TempDir Path tmp) throws Exception {
        Path schema = tmp.resolve("schema.json");
        Files.writeString(schema, SIMPLE_SCHEMA);

        String validPayload = """
                {"orderId": 42, "currencyCode": "USD"}
                """;
        assertDoesNotThrow(() -> SchemaValidator.validate(validPayload, schema));
    }

    @Test
    void missingRequiredFieldFailsValidation(@TempDir Path tmp) throws Exception {
        Path schema = tmp.resolve("schema.json");
        Files.writeString(schema, SIMPLE_SCHEMA);

        String invalidPayload = """
                {"orderId": 42}
                """;
        var ex = assertThrows(SchemaValidator.ValidationException.class,
                () -> SchemaValidator.validate(invalidPayload, schema));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.getMessage().contains("currencyCode")));
    }

    @Test
    void wrongTypeFailsValidation(@TempDir Path tmp) throws Exception {
        Path schema = tmp.resolve("schema.json");
        Files.writeString(schema, SIMPLE_SCHEMA);

        String wrongTypePayload = """
                {"orderId": "not-a-number", "currencyCode": "USD"}
                """;
        var ex = assertThrows(SchemaValidator.ValidationException.class,
                () -> SchemaValidator.validate(wrongTypePayload, schema));
        assertFalse(ex.getErrors().isEmpty());
    }

    @Test
    void additionalPropertyDisallowedFailsValidation(@TempDir Path tmp) throws Exception {
        Path schema = tmp.resolve("schema.json");
        Files.writeString(schema, STRICT_SCHEMA);

        String extraFieldPayload = """
                {"orderId": 1, "unexpected": "value"}
                """;
        var ex = assertThrows(SchemaValidator.ValidationException.class,
                () -> SchemaValidator.validate(extraFieldPayload, schema));
        assertFalse(ex.getErrors().isEmpty());
    }

    @Test
    void invalidJsonPayloadThrowsIOException(@TempDir Path tmp) throws Exception {
        Path schema = tmp.resolve("schema.json");
        Files.writeString(schema, SIMPLE_SCHEMA);

        String brokenJson = "{ this is not json }";
        assertThrows(IOException.class,
                () -> SchemaValidator.validate(brokenJson, schema));
    }

    @Test
    void draft04SchemaIsAccepted(@TempDir Path tmp) throws Exception {
        Path schema = tmp.resolve("schema.json");
        Files.writeString(schema, DRAFT04_SCHEMA);

        assertDoesNotThrow(() -> SchemaValidator.validate("""
                {"name": "Alice"}
                """, schema));
    }

    @Test
    void draft202012SchemaIsAccepted(@TempDir Path tmp) throws Exception {
        Path schema = tmp.resolve("schema.json");
        Files.writeString(schema, SIMPLE_SCHEMA);

        assertDoesNotThrow(() -> SchemaValidator.validate("""
                {"orderId": 7, "currencyCode": "EUR"}
                """, schema));
    }
}
