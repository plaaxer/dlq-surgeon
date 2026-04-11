package dev.plaaxer.dlqsurgeon.tui;

import dev.plaaxer.dlqsurgeon.model.RepairPlan;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import java.io.IOException;
import java.io.PrintWriter;

public class Console {

    private static boolean noColor = System.getenv("NO_COLOR") != null;

    private static PrintWriter out = new PrintWriter(System.out, true);
    private static final PrintWriter err = new PrintWriter(System.err, true);
    private static LineReader lineReader;

    public static void configure(boolean disableColor) {
        noColor = noColor || disableColor;
    }

    public static void info(String message) {
        out.println(message);
    }

    public static void success(String message) {
        out.println(colour(message, AttributedStyle.GREEN));  // GREEN = 2 (ANSI colour index)
    }

    public static void warn(String message) {
        err.println(colour("WARNING: " + message, AttributedStyle.YELLOW));  // YELLOW = 3
    }

    public static void error(String message) {
        err.println(colour("ERROR: " + message, AttributedStyle.RED));  // RED = 1
    }

    public static void dim(String message) {
        out.println(colour(message, 8));
    }

    public static void printPlan(RepairPlan plan) {
        info(plan.summary());
    }

    public static char[] promptPassword(String prompt) throws IOException {
        try {
            String input = lineReader().readLine(prompt, '*');
            return input == null ? new char[0] : input.toCharArray();
        } catch (UserInterruptException | EndOfFileException e) {
            return new char[0];
        }
    }

    public static boolean confirm(String prompt) {
        try {
            String answer = lineReader().readLine(prompt + " [y/N] ");
            return answer != null && (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes"));
        } catch (UserInterruptException | EndOfFileException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private static LineReader lineReader() throws IOException {
        if (lineReader == null) {
            Terminal terminal = TerminalBuilder.builder().system(true).dumb(true).build();
            lineReader = LineReaderBuilder.builder().terminal(terminal).build();
        }
        return lineReader;
    }

    private static String colour(String text, int ansiColour) {
        if (noColor) return text;
        return new AttributedString(text,
                AttributedStyle.DEFAULT.foreground(ansiColour)).toAnsi();
    }

    public static void setOut(PrintWriter writer) {
        out = writer;
    }
}
