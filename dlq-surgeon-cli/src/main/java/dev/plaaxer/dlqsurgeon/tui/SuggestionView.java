package dev.plaaxer.dlqsurgeon.tui;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.plaaxer.dlqsurgeon.model.SuggestionResult;
import dev.plaaxer.dlqsurgeon.service.FixWorkflow.SuggestionChoice;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jline.utils.AttributedStyle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Renders an {@link SuggestionResult} to the terminal with an explanation block
 * and a colorised line-diff against the original payload, then prompts the
 * user for a {@link SuggestionChoice}.
 */
public final class SuggestionView {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int WIDTH = 72;
    private static final int DIM = 8;

    private SuggestionView() {}

    public static void render(SuggestionResult result, String originalPayload) {
        String rule = "━".repeat(WIDTH);

        System.out.println();
        System.out.println(Console.colour(rule, AttributedStyle.CYAN));
        System.out.println(Console.colour(center(" AI SUGGESTION ", WIDTH), AttributedStyle.CYAN));
        System.out.println(Console.colour(rule, AttributedStyle.CYAN));
        System.out.println();

        System.out.println(Console.colour("Explanation", AttributedStyle.CYAN));
        for (String line : wrap(result.explanation(), WIDTH - 2)) {
            System.out.println("  " + line);
        }
        System.out.println();

        if (result.hasSuggestion()) {
            System.out.println(Console.colour("Diff  (original → suggested)", AttributedStyle.CYAN));
            List<String> original = prettyLines(originalPayload);
            List<String> suggested = prettyLines(result.suggestedPayload());
            for (DiffLine line : diff(original, suggested)) {
                String body = "  " + line.marker() + " " + line.text();
                int style = switch (line.marker()) {
                    case '+' -> AttributedStyle.GREEN;
                    case '-' -> AttributedStyle.RED;
                    default  -> DIM;
                };
                System.out.println(Console.colour(body, style));
            }
        } else {
            System.out.println(Console.colour("  (no payload fix proposed)", DIM));
        }

        System.out.println();
        System.out.println(Console.colour(rule, AttributedStyle.CYAN));
    }

    public static SuggestionChoice prompt(boolean hasSuggestion) {
        String options = hasSuggestion
                ? "  [a] accept as-is   [e] accept & edit   [r] reject & edit original   [q] abort: "
                : "  [r] edit original   [q] abort: ";
        try {
            LineReader reader = Console.lineReader();
            String input = reader.readLine(options).trim().toLowerCase();
            return switch (input) {
                case "a" -> hasSuggestion ? SuggestionChoice.ACCEPT_AS_IS : SuggestionChoice.ABORT;
                case "e" -> hasSuggestion ? SuggestionChoice.ACCEPT_AND_EDIT : SuggestionChoice.ABORT;
                case "r" -> SuggestionChoice.REJECT;
                default  -> SuggestionChoice.ABORT;
            };
        } catch (UserInterruptException | EndOfFileException e) {
            return SuggestionChoice.ABORT;
        } catch (IOException e) {
            return SuggestionChoice.ABORT;
        }
    }

    private static List<String> prettyLines(String payload) {
        try {
            String pretty = MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(MAPPER.readTree(payload));
            return Arrays.asList(pretty.split("\n", -1));
        } catch (Exception e) {
            return Arrays.asList(payload.split("\n", -1));
        }
    }

    private record DiffLine(char marker, String text) {}

    private static List<DiffLine> diff(List<String> a, List<String> b) {
        int n = a.size(), m = b.size();
        int[][] lcs = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                lcs[i][j] = a.get(i).equals(b.get(j))
                        ? lcs[i + 1][j + 1] + 1
                        : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }
        List<DiffLine> out = new ArrayList<>();
        int i = 0, j = 0;
        while (i < n && j < m) {
            if (a.get(i).equals(b.get(j))) {
                out.add(new DiffLine(' ', a.get(i))); i++; j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                out.add(new DiffLine('-', a.get(i))); i++;
            } else {
                out.add(new DiffLine('+', b.get(j))); j++;
            }
        }
        while (i < n) out.add(new DiffLine('-', a.get(i++)));
        while (j < m) out.add(new DiffLine('+', b.get(j++)));
        return out;
    }

    private static List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isBlank()) { out.add(""); continue; }
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                if (!line.isEmpty() && line.length() + 1 + word.length() > width) {
                    out.add(line.toString());
                    line.setLength(0);
                }
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
            if (!line.isEmpty()) out.add(line.toString());
        }
        return out;
    }

    private static String center(String text, int width) {
        if (text.length() >= width) return text;
        int padLeft = (width - text.length()) / 2;
        int padRight = width - text.length() - padLeft;
        return "━".repeat(padLeft) + text + "━".repeat(padRight);
    }
}
