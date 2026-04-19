package dev.plaaxer.dlqsurgeon.config;

/**
 * Plain data record holding all connection parameters for a RabbitMQ broker.
 *
 * This record is deliberately free of picocli annotations so it can be built
 * programmatically by any caller (CLI, MCP server, tests, cloud agent) without
 * depending on the CLI module.
 *
 * The CLI builds one via {@code ConnectOptions.toConnectionConfig()}.
 */
public record ConnectionConfig(
        String host,
        int managementPort,
        int amqpPort,
        String vhost,
        String user,
        char[] password,
        boolean tls,
        String tlsCa,
        String tlsCert,
        String tlsKey
) {}
