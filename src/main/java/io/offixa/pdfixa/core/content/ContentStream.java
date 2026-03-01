package io.offixa.pdfixa.core.content;

import io.offixa.pdfixa.core.document.FontRegistry;
import io.offixa.pdfixa.core.internal.PdfWriter;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

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

    /**
     * Optional document-level font registry.  When set, {@link #setFont} resolves
     * font names to their aliases and tracks which aliases this stream uses.
     * When {@code null} the raw name is written verbatim (standalone / test use).
     */
    private final FontRegistry fontRegistry;

    /** Ordered set of font aliases used by this stream (populated only with a registry). */
    private final Set<String> usedAliases = new LinkedHashSet<>();

    // ── Constructors ───────────────────────────────────────────────────────

    /**
     * Creates a standalone stream with no font registry.
     * {@link #setFont} writes the supplied name verbatim.
     * Suitable for isolated unit tests and low-level usage.
     */
    public ContentStream() {
        this.fontRegistry = null;
    }

    /**
     * Creates a stream wired to {@code fontRegistry}.
     * {@link #setFont} will resolve font names to aliases and track usage.
     * Called by {@link io.offixa.pdfixa.core.document.PdfPage}.
     *
     * @param fontRegistry the document-level registry; must not be {@code null}
     */
    public ContentStream(FontRegistry fontRegistry) {
        this.fontRegistry = Objects.requireNonNull(fontRegistry, "fontRegistry");
    }

    /**
     * Returns an unmodifiable, insertion-ordered view of all font aliases
     * referenced by {@link #setFont} calls on this stream.
     * Empty when no registry is attached.
     */
    public Set<String> getUsedFontAliases() {
        return Collections.unmodifiableSet(usedAliases);
    }

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
     * Appends a {@code Tf} operator for the given font name and size.
     *
     * <p>Behaviour depends on whether a {@link FontRegistry} has been attached:
     * <ul>
     *   <li><b>With registry:</b> resolves {@code name} to its alias (e.g. {@code F1}),
     *       records the alias in the used-set, and writes {@code /F1 <size> Tf\n}.
     *   <li><b>Without registry:</b> writes {@code /<name> <size> Tf\n} verbatim —
     *       preserving backward-compatible behaviour for standalone / test use.
     * </ul>
     *
     * @param name font name (Base-14) or alias when registry is absent
     * @param size point size
     */
    public ContentStream setFont(String name, double size) {
        String token;
        if (fontRegistry != null) {
            token = fontRegistry.getAlias(name);
            usedAliases.add(token);
        } else {
            token = name;
        }
        buf.append('/').append(token)
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

    /**
     * Appends {@code <hexString> Tj\n} — show text from a hex-encoded string.
     *
     * <p>The caller is responsible for providing valid hex digits.
     * This method does not affect or share logic with {@link #showText}.
     *
     * @param hexString hex-encoded byte sequence (without angle brackets)
     */
    public ContentStream showTextHex(String hexString) {
        buf.append('<').append(hexString).append("> Tj\n");
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

    // ── Color operators ──────────────────────────────────────────────────────

    /**
     * Appends {@code <r> <g> <b> rg\n} — set fill color in DeviceRGB.
     *
     * @param r red component, 0.0–1.0
     * @param g green component, 0.0–1.0
     * @param b blue component, 0.0–1.0
     * @throws IllegalArgumentException if any component is outside [0, 1]
     */
    public ContentStream setFillColor(double r, double g, double b) {
        requireUnit(r, "r");
        requireUnit(g, "g");
        requireUnit(b, "b");
        buf.append(PdfWriter.formatReal(r))
           .append(' ').append(PdfWriter.formatReal(g))
           .append(' ').append(PdfWriter.formatReal(b))
           .append(" rg\n");
        return this;
    }

    /**
     * Appends {@code <r> <g> <b> RG\n} — set stroke color in DeviceRGB.
     *
     * @param r red component, 0.0–1.0
     * @param g green component, 0.0–1.0
     * @param b blue component, 0.0–1.0
     * @throws IllegalArgumentException if any component is outside [0, 1]
     */
    public ContentStream setStrokeColor(double r, double g, double b) {
        requireUnit(r, "r");
        requireUnit(g, "g");
        requireUnit(b, "b");
        buf.append(PdfWriter.formatReal(r))
           .append(' ').append(PdfWriter.formatReal(g))
           .append(' ').append(PdfWriter.formatReal(b))
           .append(" RG\n");
        return this;
    }

    /**
     * Appends {@code <gray> g\n} — set fill color in DeviceGray.
     *
     * @param gray gray level, 0.0 (black) – 1.0 (white)
     * @throws IllegalArgumentException if {@code gray} is outside [0, 1]
     */
    public ContentStream setGray(double gray) {
        requireUnit(gray, "gray");
        buf.append(PdfWriter.formatReal(gray)).append(" g\n");
        return this;
    }

    /**
     * Appends {@code <gray> G\n} — set stroke color in DeviceGray.
     *
     * @param gray gray level, 0.0 (black) – 1.0 (white)
     * @throws IllegalArgumentException if {@code gray} is outside [0, 1]
     */
    public ContentStream setGrayStroke(double gray) {
        requireUnit(gray, "gray");
        buf.append(PdfWriter.formatReal(gray)).append(" G\n");
        return this;
    }

    // ── Graphics state operators ────────────────────────────────────────────

    /** Appends {@code q\n} — save the current graphics state. */
    public ContentStream saveState() {
        buf.append("q\n");
        return this;
    }

    /** Appends {@code Q\n} — restore the most recently saved graphics state. */
    public ContentStream restoreState() {
        buf.append("Q\n");
        return this;
    }

    /**
     * Appends {@code <a> <b> <c> <d> <e> <f> cm\n} — concatenate transformation matrix.
     *
     * <p>To place an image at ({@code x}, {@code y}) with dimensions
     * {@code w}×{@code h}, call {@code concatMatrix(w, 0, 0, h, x, y)}.
     */
    public ContentStream concatMatrix(double a, double b, double c,
                                      double d, double e, double f) {
        buf.append(PdfWriter.formatReal(a))
           .append(' ').append(PdfWriter.formatReal(b))
           .append(' ').append(PdfWriter.formatReal(c))
           .append(' ').append(PdfWriter.formatReal(d))
           .append(' ').append(PdfWriter.formatReal(e))
           .append(' ').append(PdfWriter.formatReal(f))
           .append(" cm\n");
        return this;
    }

    /**
     * Appends {@code /<name> Do\n} — invoke a named XObject (e.g. an image).
     *
     * @param name XObject resource name without the leading slash, e.g. {@code "Im1"}
     */
    public ContentStream doXObject(String name) {
        buf.append('/').append(name).append(" Do\n");
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

    // ── Internal helpers ────────────────────────────────────────────────────

    private static void requireUnit(double value, String name) {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(
                    name + " must be in [0, 1], got " + value);
        }
    }
}
