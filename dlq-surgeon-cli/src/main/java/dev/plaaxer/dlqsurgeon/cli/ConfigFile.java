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
    private final TomlTable ai;

    public ConfigFile(String profileName) {
        this(profileName, DEFAULT_PATH);
    }

    ConfigFile(String profileName, Path path) {
        TomlTable resolvedProfile = null;
        TomlTable resolvedAi = null;
        if (Files.exists(path)) {
            try {
                TomlParseResult result = Toml.parse(path);
                if (result.hasErrors()) {
                    System.err.println("WARNING: " + path + " has parse errors — ignoring config file:");
                    result.errors().forEach(e -> System.err.println("  " + e));
                } else {
                    resolvedProfile = result.getTable(profileName);
                    if (resolvedProfile == null) {
                        System.err.println("WARNING: profile '" + profileName + "' not found in " + path);
                    }
                    resolvedAi = result.getTable("ai");
                }
            } catch (IOException e) {
                System.err.println("WARNING: could not read " + path + " — " + e.getMessage());
            }
        }
        this.profile = resolvedProfile;
        this.ai = resolvedAi;
    }

    public String get(String key) {
        return lookup(profile, key);
    }

    /** Returns a value from the top-level {@code [ai]} table, or {@code null} if unset. */
    public String getAi(String key) {
        return lookup(ai, key);
    }

    private static String lookup(TomlTable table, String key) {
        if (table == null || !table.contains(key)) return null;
        Object val = table.get(key);
        return val != null ? val.toString() : null;
    }
}