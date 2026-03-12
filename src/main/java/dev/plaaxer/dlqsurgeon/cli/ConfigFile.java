package dev.plaaxer.dlqsurgeon.cli;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads ~/.dlq-surgeon/config.toml and exposes values for a named profile.
 *
 * Config file format:
 * <pre>
 *   [default]
 *   host     = "localhost"
 *   user     = "guest"
 *   password = "guest"
 *   vhost    = "/"
 *   management-port = 15672
 *   amqp-port       = 5672
 *
 *   [prod]
 *   host     = "rabbitmq.prod.internal"
 *   user     = "admin"
 *   password = "s3cr3t"
 * </pre>
 */
public class ConfigFile {

    static final Path DEFAULT_PATH =
            Path.of(System.getProperty("user.home"), ".dlq-surgeon", "config.toml");

    private final TomlTable profile;

    public ConfigFile(String profileName) {
        this(profileName, DEFAULT_PATH);
    }

    ConfigFile(String profileName, Path path) {
        TomlTable resolved = null;
        if (Files.exists(path)) {
            try {
                TomlParseResult result = Toml.parse(path);
                if (result.hasErrors()) {
                    System.err.println("WARNING: " + path + " has parse errors — ignoring config file:");
                    result.errors().forEach(e -> System.err.println("  " + e));
                } else {
                    resolved = result.getTable(profileName);
                    if (resolved == null) {
                        System.err.println("WARNING: profile '" + profileName + "' not found in " + path);
                    }
                }
            } catch (IOException e) {
                System.err.println("WARNING: could not read " + path + " — " + e.getMessage());
            }
        }
        this.profile = resolved;
    }

    public String get(String key) {
        if (profile == null || !profile.contains(key)) return null;
        Object val = profile.get(key);
        return val != null ? val.toString() : null;
    }
}