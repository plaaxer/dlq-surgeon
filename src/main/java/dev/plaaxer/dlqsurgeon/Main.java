package dev.plaaxer.dlqsurgeon;

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
        int exitCode = new CommandLine(new DlqSurgeon()).execute(args);
        System.exit(exitCode);
    }
}
