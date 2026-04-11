package dev.plaaxer.dlqsurgeon.tui;

import dev.plaaxer.dlqsurgeon.model.RabbitMessage;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class MessagePicker {

    /**
     * Shows the interactive picker and returns the selected message, or null if cancelled.
     *
     * Supports "/" search mode: typing "/term" filters the list to messages whose label or
     * payload contains "term" (case-insensitive). Typing "/" alone clears the filter.
     *
     * @param messages Non-empty list of messages to pick from.
     * @return The selected message, or null if the user typed "q" or pressed Ctrl+C.
     */
    public static RabbitMessage pick(List<RabbitMessage> messages) throws IOException {
        try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
            PrintWriter writer = terminal.writer();
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

            List<RabbitMessage> filtered = messages;
            String activeFilter = null;
            int lastPrintedLines = printList(writer, filtered, activeFilter);

            while (true) {
                String input;
                try {
                    input = reader.readLine(buildPrompt(filtered.size(), activeFilter)).trim();
                } catch (org.jline.reader.UserInterruptException | org.jline.reader.EndOfFileException e) {
                    return null;
                }

                if (input.equalsIgnoreCase("q")) return null;

                if (input.startsWith("/")) {
                    String term = input.substring(1).trim();
                    activeFilter = term.isEmpty() ? null : term;
                    String filterTerm = activeFilter;
                    filtered = filterTerm == null ? messages
                            : messages.stream()
                                .filter(m -> matchesFilter(m, filterTerm))
                                .toList();
                } else {
                    if (!filtered.isEmpty()) {
                        try {
                            int idx = Integer.parseInt(input);
                            if (idx >= 1 && idx <= filtered.size()) {
                                return filtered.get(idx - 1);
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }

                // Redraw after every non-selecting input: clears the previous list and prompt line,
                // then reprints so lastPrintedLines stays accurate for the next redraw.
                clearLines(writer, lastPrintedLines);
                lastPrintedLines = printList(writer, filtered, activeFilter);
            }
        }
    }

    private static String buildPrompt(int count, String filter) {
        String range = count == 0 ? "—" : "1-" + count;
        if (filter != null) {
            return "Filter [/" + filter + "] select [" + range + "], / to search, q to quit: ";
        }
        return "Select [" + range + "], / to search, q to quit: ";
    }

    private static int printList(PrintWriter writer, List<RabbitMessage> messages, String filter) {
        int lines = 0;
        String separator = "─".repeat(70);

        writer.println(separator); lines++;
        if (filter != null) {
            writer.println("Search: /" + filter + "  (" + messages.size()
                    + (messages.size() == 1 ? " match" : " matches") + ")");
            lines++;
        }
        if (messages.isEmpty()) {
            writer.println("  (no messages match)"); lines++;
        } else {
            for (RabbitMessage msg : messages) {
                writer.println(msg.label()); lines++;
            }
        }
        writer.println(separator); lines++;
        writer.flush();
        return lines;
    }

    /**
     * Moves the cursor up to erase the printed list plus the prompt line rendered by readLine,
     * then clears from that position to the end of the screen, ready for a fresh reprint.
     */
    private static void clearLines(PrintWriter writer, int listLines) {
        writer.print("\033[" + (listLines + 1) + "A\033[J");
        writer.flush();
    }

    private static boolean matchesFilter(RabbitMessage msg, String term) {
        String lower = term.toLowerCase();
        return msg.label().toLowerCase().contains(lower)
                || msg.payload().toLowerCase().contains(lower);
    }
}