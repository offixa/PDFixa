package io.offixa.pdfixa.core.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic greedy text wrapper for a fixed maximum width.
 */
public final class TextWrapper {

    private TextWrapper() {
    }

    public static List<String> wrap(
            String text,
            String fontName,
            double fontSize,
            double maxWidthPt,
            FontMetrics metrics) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(fontName, "fontName");
        Objects.requireNonNull(metrics, "metrics");
        if (fontSize <= 0) {
            throw new IllegalArgumentException("fontSize must be > 0");
        }
        if (maxWidthPt <= 0) {
            throw new IllegalArgumentException("maxWidthPt must be > 0");
        }

        String normalized = normalizeNewlines(text);
        String[] paragraphs = normalized.split("\n", -1);

        List<String> out = new ArrayList<>();
        for (String paragraph : paragraphs) {
            String collapsed = collapseSpaces(paragraph);
            if (collapsed.isEmpty()) {
                out.add("");
                continue;
            }

            String[] words = collapsed.split(" ");
            String current = "";
            for (String word : words) {
                if (word.isEmpty()) {
                    continue;
                }

                if (current.isEmpty()) {
                    if (fits(word, fontName, fontSize, maxWidthPt, metrics)) {
                        current = word;
                    } else {
                        emitLongWordSegments(word, fontName, fontSize, maxWidthPt, metrics, out);
                    }
                    continue;
                }

                String candidate = current + " " + word;
                if (fits(candidate, fontName, fontSize, maxWidthPt, metrics)) {
                    current = candidate;
                } else {
                    out.add(current);
                    if (fits(word, fontName, fontSize, maxWidthPt, metrics)) {
                        current = word;
                    } else {
                        emitLongWordSegments(word, fontName, fontSize, maxWidthPt, metrics, out);
                        current = "";
                    }
                }
            }

            if (!current.isEmpty()) {
                out.add(current);
            }
        }

        return out;
    }

    private static boolean fits(
            String text,
            String fontName,
            double fontSize,
            double maxWidthPt,
            FontMetrics metrics) {
        return metrics.textWidthPt(text, fontName, fontSize) <= maxWidthPt;
    }

    private static void emitLongWordSegments(
            String word,
            String fontName,
            double fontSize,
            double maxWidthPt,
            FontMetrics metrics,
            List<String> out) {
        int start = 0;
        while (start < word.length()) {
            int bestEndExclusive = -1;
            int end = start + 1;

            while (end <= word.length()) {
                String candidate = word.substring(start, end);
                if (fits(candidate, fontName, fontSize, maxWidthPt, metrics)) {
                    bestEndExclusive = end;
                    end++;
                } else {
                    break;
                }
            }

            if (bestEndExclusive == -1) {
                // Ensure forward progress even if a single character is wider than maxWidthPt.
                bestEndExclusive = start + 1;
            }

            out.add(word.substring(start, bestEndExclusive));
            start = bestEndExclusive;
        }
    }

    private static String normalizeNewlines(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static String collapseSpaces(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        boolean prevSpace = false;
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch == ' ') {
                if (!prevSpace) {
                    sb.append(ch);
                    prevSpace = true;
                }
            } else {
                sb.append(ch);
                prevSpace = false;
            }
        }
        return trimSingleSpaceEdges(sb);
    }

    private static String trimSingleSpaceEdges(StringBuilder sb) {
        int start = 0;
        int end = sb.length();
        while (start < end && sb.charAt(start) == ' ') {
            start++;
        }
        while (end > start && sb.charAt(end - 1) == ' ') {
            end--;
        }
        return sb.substring(start, end);
    }
}
