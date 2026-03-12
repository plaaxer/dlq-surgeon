package dev.plaaxer.dlqsurgeon.tui;

import dev.plaaxer.dlqsurgeon.model.RabbitMessage;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.List;

public class MessagePicker {

    /**
     * Shows the interactive picker and returns the selected message, or null if cancelled.
     *
     * @param messages Non-empty list of messages to pick from.
     * @return The selected message, or null if the user typed "q" or pressed Ctrl+C.
     */
    public static RabbitMessage pick(List<RabbitMessage> messages) throws IOException {
        try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {

            printList(terminal, messages);

            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

            while (true) {
                String input;
                try {
                    input = reader.readLine("Select [1-" + messages.size() + "], q to quit: ").trim();
                } catch (org.jline.reader.UserInterruptException | org.jline.reader.EndOfFileException e) {
                    return null;
                }

                if (input.equalsIgnoreCase("q")) return null;

                // TODO: handle "/" search mode

                try {
                    int idx = Integer.parseInt(input);
                    if (idx >= 1 && idx <= messages.size()) {
                        return messages.get(idx - 1);
                    }
                } catch (NumberFormatException ignored) {}

                Console.warn("Invalid selection — enter a number between 1 and " + messages.size());
            }
        }
    }

    private static void printList(Terminal terminal, List<RabbitMessage> messages) {
        // TODO: Use terminal.writer() for output so JLine can manage cursor position.
        Console.info("─".repeat(70));
        for (RabbitMessage msg : messages) {
            Console.info(msg.label());
        }
        Console.info("─".repeat(70));
    }
}
