package dev.plaaxer.dlqsurgeon.cli;

import dev.plaaxer.dlqsurgeon.tui.Console;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable connection options, injected into every subcommand via @Mixin.
 *
 * Resolution order (highest to lowest priority):
 *   1. CLI flag
 *   2. ~/.dlq-surgeon/config.toml (profile selected with --profile)
 *   3. Environment variable (RABBITMQ_HOST, RABBITMQ_USER, etc.)
 *   4. Built-in default
 */
public class ConnectOptions {

    @Spec(Spec.Target.MIXEE)
    CommandSpec spec;

    // ── Profile ──────────────────────────────────────────────────────────────

    @Option(
            names = {"--profile"},
            description = "Config file profile to load from ~/.dlq-surgeon/config.toml (default: ${DEFAULT-VALUE}).",
            defaultValue = "default"
    )
    public String profile;

    // ── Management HTTP API ──────────────────────────────────────────────────

    @Option(
            names = {"--host", "-h"},
            description = "RabbitMQ host (default: ${DEFAULT-VALUE})",
            defaultValue = "${env:RABBITMQ_HOST:-localhost}"
    )
    public String host;

    @Option(
            names = {"--management-port", "-P"},
            description = "Management API port (default: ${DEFAULT-VALUE})",
            defaultValue = "${env:RABBITMQ_MANAGEMENT_PORT:-15672}"
    )
    public int managementPort;

    @Option(
            names = {"--amqp-port"},
            description = "AMQP port for re-injection (default: ${DEFAULT-VALUE})",
            defaultValue = "${env:RABBITMQ_AMQP_PORT:-5672}"
    )
    public int amqpPort;

    @Option(
            names = {"--vhost", "-v"},
            description = "Virtual host (default: ${DEFAULT-VALUE})",
            defaultValue = "${env:RABBITMQ_VHOST:-/}"
    )
    public String vhost;

    @Option(
            names = {"--user", "-u"},
            description = "Username (default: ${DEFAULT-VALUE})",
            defaultValue = "${env:RABBITMQ_USER:-guest}"
    )
    public String user;

    @Option(
            names = {"--password", "-p"},
            description = "Password (reads from RABBITMQ_PASSWORD env var if not set)",
            defaultValue = "${env:RABBITMQ_PASSWORD:-guest}",
            // TODO: switch to interactive prompt when running in a TTY and no value is given
            interactive = false
    )
    public char[] password;

    // ── TLS / mTLS ──────────────────────────────────────────────────────────
    // TODO: add --tls, --tls-cert, --tls-key, --tls-ca options.
    //       Can use SSLContext with KeyManagerFactory and TrustManagerFactory.

    // ── Safety ──────────────────────────────────────────────────────────────

    @Option(
            names = {"--read-only"},
            description = "Disable all write operations (list and peek still work).",
            defaultValue = "false"
    )
    public boolean readOnly;

    /**
     * Prints the active connection target and warns about any options that were
     * not explicitly set on the CLI (defaults).
     */
    public void printConnectionInfo() {
        Console.dim("Connecting to: " + summary());

        List<String> defaulted = new ArrayList<>();
        var parseResult = spec.commandLine().getParseResult();
        for (String opt : List.of("--host", "--user", "--password")) {
            if (!parseResult.hasMatchedOption(opt)) {
                defaulted.add(opt);
            }
        }

        if (!defaulted.isEmpty()) {
            Console.warn("Using default value for: " + String.join(", ", defaulted)
                    + ". Set via CLI flag, ~/.dlq-surgeon/config.toml, or env var.");
        }

        if ("default".equals(profile) && !parseResult.hasMatchedOption("--profile")) {
            Console.warn("Using [default] config profile. Pass --profile <name> to use a named profile.");
        }
    }

    public String summary() {
        return String.format("amqp://%s@%s:%d/%s  (management: %d)",
                user, host, amqpPort, vhost.equals("/") ? "%2F" : vhost, managementPort);
    }
}
