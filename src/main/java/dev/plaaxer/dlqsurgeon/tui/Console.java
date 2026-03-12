package dev.plaaxer.dlqsurgeon.tui;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import java.io.PrintWriter;

/**
 * TODO: Read NO_COLOR env var at startup and disable ANSI codes if set.
 *       See https://no-color.org/
 * TODO: Add a --no-color flag to ConnectOptions (or a global mixin).
 * TODO: Add confirm(String prompt) → boolean for interactive yes/no prompts
 *       using JLine's LineReader.
 */
public class Console {

    private static PrintWriter out = new PrintWriter(System.out, true);
    private static PrintWriter err = new PrintWriter(System.err, true);

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

    /**
     * TODO: Accept a RepairPlan and call plan.summary().
     */
    public static void printPlan(Object plan) {
        info(plan.toString());
    }

    public static boolean confirm(String prompt) {
        System.out.print(prompt + " [y/N] ");
        String answer = System.console() != null
                ? System.console().readLine()
                : "n";
        return answer != null && (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes"));
    }

    private static String colour(String text, int ansiColour) {
        return new AttributedString(text,
                AttributedStyle.DEFAULT.foreground(ansiColour)).toAnsi();
    }

    public static void setOut(PrintWriter writer) {
        out = writer;
    }
}
