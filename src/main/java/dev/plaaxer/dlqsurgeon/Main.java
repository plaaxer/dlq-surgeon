package dev.plaaxer.dlqsurgeon;

import dev.plaaxer.dlqsurgeon.cli.ProfileConfigProvider;
import picocli.CommandLine;

/**
 * Entry point. Delegates immediately to the root Picocli command.
 *
 * Keep this class as thin as possible — all real logic lives in DlqSurgeon
 * and its subcommands. The exit code returned by CommandLine.execute() is
 * passed straight to System.exit() so that shell scripts can check $?.
 */
public class Main {

    public static void main(String[] args) {
        // Pre-scan for --profile before Picocli parses: the default-value provider
        // needs the profile name at construction time, before any option is resolved.
        String profile = extractProfile(args);

        int exitCode = new CommandLine(new DlqSurgeon())
                .setDefaultValueProvider(new ProfileConfigProvider(profile))
                .execute(args);
        System.exit(exitCode);
    }

    /**
     * Reads --profile <name> from raw args without invoking the full Picocli parser.
     * Falls back to "default" if not present.
     */
    private static String extractProfile(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--profile".equals(args[i])) return args[i + 1];
        }
        return "default";
    }
}