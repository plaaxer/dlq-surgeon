package dev.plaaxer.dlqsurgeon;

import dev.plaaxer.dlqsurgeon.cli.ProfileConfigProvider;
import picocli.CommandLine;

public class Main {

    public static void main(String[] args) {
        String profile = extractProfile(args);

        int exitCode = new CommandLine(new DlqSurgeon())
                .setDefaultValueProvider(new ProfileConfigProvider(profile))
                .execute(args);
        System.exit(exitCode);
    }

    private static String extractProfile(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--profile".equals(args[i])) return args[i + 1];
        }
        return "default";
    }
}