package io.offixa.pdfixa.core.content;

import io.offixa.pdfixa.core.writer.PdfWriter;

import java.nio.charset.StandardCharsets;

/**
 * Minimal, safe builder for PDF content stream operators.
 *
 * <p>Each method appends one operator line to an internal buffer and returns
 * {@code this} for chaining. Output is deterministic, ASCII-only, and uses
 * {@code \n} line endings throughout.
 *
 * <p>Number formatting delegates to {@link PdfWriter#formatReal(double)} — no
 * duplicate logic.
 *
 * <p>This class is not thread-safe.
 */
public final class ContentStream {

    private final StringBuilder buf = new StringBuilder();

    // ── Text operators ─────────────────────────────────────────────────────

    /** Appends {@code BT\n} — begin text object. */
    public ContentStream beginText() {
        buf.append("BT\n");
        return this;
    }

    /** Appends {@code ET\n} — end text object. */
    public ContentStream endText() {
        buf.append("ET\n");
        return this;
    }

    /**
     * Appends {@code /<name> <size> Tf\n} — set font and size.
     *
     * @param name font resource name (without leading slash)
     * @param size point size
     */
    public ContentStream setFont(String name, double size) {
        buf.append('/').append(name)
           .append(' ').append(PdfWriter.formatReal(size))
           .append(" Tf\n");
        return this;
    }

    /**
     * Appends {@code <x> <y> Td\n} — move text position.
     */
    public ContentStream moveText(double x, double y) {
        buf.append(PdfWriter.formatReal(x))
           .append(' ').append(PdfWriter.formatReal(y))
           .append(" Td\n");
        return this;
    }

    /**
     * Appends {@code (<escaped>) Tj\n} — show text string.
     *
     * <p>Escaping rules:
     * <ul>
     *   <li>{@code (} → {@code \(}
     *   <li>{@code )} → {@code \)}
     *   <li>{@code \} → {@code \\}
     *   <li>Control characters: {@code \n \r \t \b \f}
     * </ul>
     *
     * @throws IllegalArgumentException if any character is outside Latin-1 (> U+00FF)
     */
    public ContentStream showText(String text) {
        buf.append('(');
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch > 0xFF) {
                throw new IllegalArgumentException(
                        "showText: character U+" + Integer.toHexString(ch).toUpperCase()
                        + " at index " + i + " is outside Latin-1 range");
            }
            switch (ch) {
                case '('  -> buf.append("\\(");
                case ')'  -> buf.append("\\)");
                case '\\' -> buf.append("\\\\");
                case '\n' -> buf.append("\\n");
                case '\r' -> buf.append("\\r");
                case '\t' -> buf.append("\\t");
                case '\b' -> buf.append("\\b");
                case '\f' -> buf.append("\\f");
                default   -> buf.append(ch);
            }
        }
        buf.append(") Tj\n");
        return this;
    }

    // ── Graphics operators ─────────────────────────────────────────────────

    /**
     * Appends {@code <x> <y> m\n} — moveto.
     */
    public ContentStream moveTo(double x, double y) {
        buf.append(PdfWriter.formatReal(x))
           .append(' ').append(PdfWriter.formatReal(y))
           .append(" m\n");
        return this;
    }

    /**
     * Appends {@code <x> <y> l\n} — lineto.
     */
    public ContentStream lineTo(double x, double y) {
        buf.append(PdfWriter.formatReal(x))
           .append(' ').append(PdfWriter.formatReal(y))
           .append(" l\n");
        return this;
    }

    /**
     * Appends {@code S\n} — stroke current path.
     */
    public ContentStream stroke() {
        buf.append("S\n");
        return this;
    }

    /**
     * Appends {@code <x> <y> <w> <h> re\n} — append rectangle to path.
     */
    public ContentStream rectangle(double x, double y, double w, double h) {
        buf.append(PdfWriter.formatReal(x))
           .append(' ').append(PdfWriter.formatReal(y))
           .append(' ').append(PdfWriter.formatReal(w))
           .append(' ').append(PdfWriter.formatReal(h))
           .append(" re\n");
        return this;
    }

    /**
     * Appends {@code f\n} — fill current path using non-zero winding rule.
     */
    public ContentStream fill() {
        buf.append("f\n");
        return this;
    }

    /**
     * Appends {@code <w> w\n} — set line width.
     */
    public ContentStream setLineWidth(double w) {
        buf.append(PdfWriter.formatReal(w)).append(" w\n");
        return this;
    }

    // ── Output ─────────────────────────────────────────────────────────────

    /**
     * Returns the accumulated operators as US-ASCII bytes.
     * Output is deterministic; no trailing whitespace is added beyond operator newlines.
     */
    public byte[] toBytes() {
        return buf.toString().getBytes(StandardCharsets.US_ASCII);
    }
}
