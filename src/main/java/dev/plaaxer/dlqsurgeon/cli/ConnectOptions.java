package dev.plaaxer.dlqsurgeon.cli;

import picocli.CommandLine.Option;

/**
 * Reusable connection options, injected into every subcommand via @Mixin.
 *
 * All flags here can also be sourced from environment variables as a fallback
 * (see the defaultValue expressions). This lets CI pipelines and Docker setups
 * avoid passing credentials on the command line.
 *
 * TODO: Add support for loading these from a ~/.dlq-surgeon/config.toml file
 *       so power users can define named connection profiles (e.g., --profile prod).
 */
public class ConnectOptions {

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
    // TODO: Add --tls, --tls-cert, --tls-key, --tls-ca options.
    //       Use SSLContext with KeyManagerFactory + TrustManagerFactory.
    //       Both ManagementClient (HttpClient) and AmqpPublisher (ConnectionFactory)
    //       accept an SSLContext directly.

    // ── Safety ──────────────────────────────────────────────────────────────

    @Option(
            names = {"--read-only"},
            description = "Disable all write operations (list and peek still work).",
            defaultValue = "false"
    )
    public boolean readOnly;

    /**
     * Returns a human-readable summary for use in confirmation prompts.
     * Never includes the password.
     */
    public String summary() {
        return String.format("amqp://%s@%s:%d/%s  (management: %d)",
                user, host, amqpPort, vhost.equals("/") ? "%2F" : vhost, managementPort);
    }
}
