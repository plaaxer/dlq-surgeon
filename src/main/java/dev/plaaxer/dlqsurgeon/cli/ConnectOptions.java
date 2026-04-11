package dev.plaaxer.dlqsurgeon.cli;

import dev.plaaxer.dlqsurgeon.client.SSLManager;
import dev.plaaxer.dlqsurgeon.tui.Console;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import javax.net.ssl.SSLContext;
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
            description = "Password (reads from RABBITMQ_PASSWORD env var if not set; prompted interactively when in a TTY and no value is given)",
            defaultValue = "${env:RABBITMQ_PASSWORD:-guest}",
            interactive = false
    )
    public char[] password;

    // ----------- TLS -----------

    @Option(
            names = {"--tls"},
            description = "Enable TLS verification",
            defaultValue = "false"
    )
    public boolean tls;

    @Option(
            names = {"--tls-cert"},
            description = "Path to client certificate file [.pem] (for mTLS)"
    )
    public String clientCertFilePath;

    @Option(
            names = {"--tls-key"},
            description = "Path to client private key [.pem] (for mTLS)"
    )
    public String clientPrivateKeyPath;

    @Option(
            names = {"--tls-ca"},
            description = "Path to CA certificate file [.pem]"
    )
    public String caCertFilePath;

    @Option(
            names = {"--read-only"},
            description = "Disable all write operations (list and peek still work).",
            defaultValue = "false"
    )
    public boolean readOnly;

    // ── TUI ──────────────────────────────────────────────────────────────
    @Option(
            names = {"--no-color"},
            description = "Disable coloured output. Can also be set on .env vars",
            defaultValue = "false"
    )
    public boolean noColor;

    /**
     * Prints the active connection target and warns about any options that were
     * not explicitly set on the CLI (defaults). If running in a TTY and no password
     * was provided, prompts for it interactively instead of falling back to the default.
     */
    public void printConnectionInfo() throws java.io.IOException {
        Console.configure(noColor);
        Console.dim("Connecting to: " + summary());

        var parseResult = spec.commandLine().getParseResult();
        boolean willPromptPassword = !parseResult.hasMatchedOption("--password") && System.console() != null;

        List<String> defaulted = new ArrayList<>();
        for (String opt : List.of("--host", "--user", "--password")) {
            if (!parseResult.hasMatchedOption(opt) && !(opt.equals("--password") && willPromptPassword)) {
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

        if (willPromptPassword) {
            char[] prompted = Console.promptPassword("Password: ");
            if (prompted.length > 0) {
                this.password = prompted;
            }
        }
    }

    /**
     * Builds an SSLContext from the TLS flags, or returns null when TLS is disabled.
     * The result should be cached by callers — this reads files from disk each call.
     */
    public SSLContext buildSslContext() throws Exception {
        if (!tls) return null;
        return SSLManager.build(caCertFilePath, clientCertFilePath, clientPrivateKeyPath);
    }

    public String summary() {
        return String.format("%s://%s@%s:%d/%s  (management: %d)",
                tls ? "amqps" : "amqp",
                user, host, amqpPort, vhost.equals("/") ? "%2F" : vhost, managementPort);
    }
}
