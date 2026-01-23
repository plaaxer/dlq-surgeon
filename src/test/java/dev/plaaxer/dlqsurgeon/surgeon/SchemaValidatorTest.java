package dev.plaaxer.dlqsurgeon.surgeon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SchemaValidator.
 *
 * These tests write temporary schema files using JUnit's @TempDir — no mocking needed,
 * no network calls, no RabbitMQ required.
 *
 * TODO: Implement tests once SchemaValidator.validate() is implemented.
 *
 * Suggested test cases:
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

    @Test
    void validPayloadPassesValidation(@TempDir Path tmp) throws Exception {
        Path schema = tmp.resolve("schema.json");
        Files.writeString(schema, SIMPLE_SCHEMA);

        // TODO: Remove assertThrows wrapper once validate() is implemented.
        //       Replace with: assertDoesNotThrow(() -> SchemaValidator.validate(payload, schema));
        String validPayload = """
                {"orderId": 42, "currencyCode": "USD"}
                """;
        assertThrows(UnsupportedOperationException.class,
                () -> SchemaValidator.validate(validPayload, schema));
    }

    @Test
    void missingRequiredFieldFailsValidation(@TempDir Path tmp) throws Exception {
        Path schema = tmp.resolve("schema.json");
        Files.writeString(schema, SIMPLE_SCHEMA);

        // TODO: Remove assertThrows(UnsupportedOperationException) wrapper once implemented.
        //       Replace with:
        //         var ex = assertThrows(SchemaValidator.ValidationException.class, ...);
        //         assertTrue(ex.getErrors().stream().anyMatch(e -> e.getMessage().contains("currencyCode")));
        String invalidPayload = """
                {"orderId": 42}
                """;
        assertThrows(UnsupportedOperationException.class,
                () -> SchemaValidator.validate(invalidPayload, schema));
    }
}
