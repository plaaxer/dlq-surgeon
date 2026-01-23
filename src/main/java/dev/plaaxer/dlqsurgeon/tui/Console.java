package dev.plaaxer.dlqsurgeon.tui;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import java.io.PrintWriter;

/**
 * Centralised console output with ANSI colour support via JLine.
 *
 * All output from commands should go through this class rather than
 * System.out.println — it gives us a single place to:
 *   - Toggle colour on/off (--no-color flag or NO_COLOR env var).
 *   - Redirect output in tests without touching System.out.
 *   - Add structured logging hooks later without changing call sites.
 *
 * Colour conventions used across the tool:
 *   info    → default (no colour) — normal output
 *   success → bright green        — operation completed successfully
 *   warn    → bright yellow       — non-fatal warning, user should notice
 *   error   → bright red          — operation failed
 *   dim     → grey                — secondary detail, repair plan metadata
 *
 * TODO: Read NO_COLOR env var at startup and disable ANSI codes if set.
 *       See https://no-color.org/
 * TODO: Add a --no-color flag to ConnectOptions (or a global mixin).
 * TODO: Add confirm(String prompt) → boolean for interactive yes/no prompts
 *       using JLine's LineReader.
 */
public class Console {

    // Shared writer — replace in tests with a StringWriter-backed PrintWriter.
    private static PrintWriter out = new PrintWriter(System.out, true);
    private static PrintWriter err = new PrintWriter(System.err, true);

    public static void info(String message) {
        out.println(message);
    }

    public static void success(String message) {
        out.println(colour(message, AttributedStyle.GREEN));
    }

    public static void warn(String message) {
        err.println(colour("WARNING: " + message, AttributedStyle.YELLOW));
    }

    public static void error(String message) {
        err.println(colour("ERROR: " + message, AttributedStyle.RED));
    }

    public static void dim(String message) {
        out.println(colour(message, AttributedStyle.DEFAULT));
    }

    /**
     * Prints the RepairPlan summary in a box, ready for user confirmation.
     * TODO: Accept a RepairPlan and call plan.summary().
     */
    public static void printPlan(Object plan) {
        info(plan.toString());
    }

    /**
     * Prompts the user for a yes/no answer. Returns true if the user types "y" or "yes".
     *
     * TODO: Implement using JLine LineReader so we get proper terminal handling
     *       (history, ctrl+c, etc.). For now, use System.console().readLine().
     */
    public static boolean confirm(String prompt) {
        System.out.print(prompt + " [y/N] ");
        String answer = System.console() != null
                ? System.console().readLine()
                : "n";
        return answer != null && (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes"));
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    private static String colour(String text, int ansiColour) {
        return new AttributedString(text,
                AttributedStyle.DEFAULT.foreground(ansiColour)).toAnsi();
    }

    /** For testing: redirect output to a custom writer. */
    public static void setOut(PrintWriter writer) {
        out = writer;
    }
}
