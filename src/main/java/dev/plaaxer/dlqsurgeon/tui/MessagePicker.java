package dev.plaaxer.dlqsurgeon.tui;

import dev.plaaxer.dlqsurgeon.model.DeadLetteredMessage;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.List;

/**
 * Interactive message picker: presents a numbered list of dead-lettered messages
 * and lets the user select one by number or navigate with arrow keys.
 *
 * Used by both PeekCommand (view only) and FixCommand (select for repair).
 *
 * Interaction model:
 *   - Display the list with labels from DeadLetteredMessage.label().
 *   - Accept a number input (e.g., "3") to select a message.
 *   - Accept "q" or Ctrl+C to exit without selecting.
 *   - Accept "/" to enter search/filter mode (filter by routing key or payload content).
 *
 * JLine 3 notes:
 *   - TerminalBuilder.terminal() auto-detects the terminal type (xterm, dumb, etc.).
 *   - On "dumb" terminals (CI, piped output), fall back to simple stdin readline.
 *   - Close the Terminal in a finally block — it holds a native file descriptor.
 *
 * TODO: Implement arrow-key navigation (up/down highlights, enter selects).
 *       This requires using Terminal.reader() directly (raw mode) rather than
 *       LineReader (which is line-oriented). See JLine's BindingReader for key mapping.
 * TODO: Show a scrollable window if the message list is longer than the terminal height.
 * TODO: Display a detail preview pane on the right half of the terminal for the
 *       currently highlighted message (requires split-terminal rendering).
 */
public class MessagePicker {

    /**
     * Shows the interactive picker and returns the selected message, or null if cancelled.
     *
     * @param messages Non-empty list of messages to pick from.
     * @return The selected message, or null if the user typed "q" or pressed Ctrl+C.
     *
     * TODO: Implement.
     *   1. Print a header line (queue name, total count).
     *   2. Print each message.label() with its index number.
     *   3. Open a JLine LineReader for input (handles history, ctrl+c, etc.).
     *   4. Prompt: "Select [1-N], / to search, q to quit: "
     *   5. Parse input:
     *      - integer in range → return messages.get(index - 1)
     *      - "/" → enter filter loop (reprint filtered list, prompt again)
     *      - "q" or EOFException → return null
     *      - anything else → print "Invalid selection" and prompt again
     */
    public static DeadLetteredMessage pick(List<DeadLetteredMessage> messages) throws IOException {
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

    private static void printList(Terminal terminal, List<DeadLetteredMessage> messages) {
        // TODO: Use terminal.writer() for output so JLine can manage cursor position.
        Console.info("─".repeat(70));
        for (DeadLetteredMessage msg : messages) {
            Console.info(msg.label());
        }
        Console.info("─".repeat(70));
    }
}
