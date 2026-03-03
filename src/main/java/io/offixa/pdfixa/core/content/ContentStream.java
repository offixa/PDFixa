package io.offixa.pdfixa.core.content;

import io.offixa.pdfixa.core.internal.PdfWriter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;

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
 * <p>Once {@link #seal() sealed}, all mutating operations throw
 * {@link IllegalStateException}. Read-only operations ({@link #toBytes()},
 * {@link #getUsedFontAliases()}) remain available.
 *
 * <p>This class is not thread-safe.
 */
public final class ContentStream {

    private final StringBuilder buf = new StringBuilder();

    /**
     * Optional font-name resolver.  When set, {@link #setFont} resolves
     * font names to their aliases and tracks which aliases this stream uses.
     * When {@code null} the raw name is written verbatim (standalone / test use).
     */
    private final UnaryOperator<String> fontResolver;

    /** Ordered set of font aliases used by this stream (populated only with a resolver). */
    private final Set<String> usedAliases = new LinkedHashSet<>();

    private boolean sealed;

    // ── Constructors ───────────────────────────────────────────────────────

    /**
     * Creates a standalone stream with no font resolver.
     * {@link #setFont} writes the supplied name verbatim.
     * Suitable for isolated unit tests and low-level usage.
     */
    public ContentStream() {
        this.fontResolver = null;
    }

    /**
     * Creates a stream wired to {@code fontResolver}.
     * {@link #setFont} will resolve font names to aliases and track usage.
     *
     * @param fontResolver maps a font name (e.g. {@code "Helvetica"}) to its
     *                     resource alias (e.g. {@code "F1"}); must not be {@code null}
     */
    public ContentStream(UnaryOperator<String> fontResolver) {
        this.fontResolver = Objects.requireNonNull(fontResolver, "fontResolver");
    }

    // ── Seal lifecycle ────────────────────────────────────────────────────

    /**
     * Seals this stream, preventing any further mutating operations.
     * Subsequent calls to drawing/text/graphics methods will throw
     * {@link IllegalStateException}. Idempotent.
     */
    public void seal() {
        this.sealed = true;
    }

    /**
     * Returns {@code true} if this stream has been {@link #seal() sealed}.
     */
    public boolean isSealed() {
        return sealed;
    }

    private void ensureOpen() {
        if (sealed) {
            throw new IllegalStateException(
                    "ContentStream has been sealed and cannot be modified");
        }
    }

    /**
     * Returns an unmodifiable, insertion-ordered view of all font aliases
     * referenced by {@link #setFont} calls on this stream.
     * Empty when no resolver is attached.
     */
    public Set<String> getUsedFontAliases() {
        return Collections.unmodifiableSet(usedAliases);
    }

    // ── Text operators ─────────────────────────────────────────────────────

    /** Appends {@code BT\n} — begin text object. */
    public ContentStream beginText() {
        ensureOpen();
        buf.append("BT\n");
        return this;
    }

    /** Appends {@code ET\n} — end text object. */
    public ContentStream endText() {
        ensureOpen();
        buf.append("ET\n");
        return this;
    }

    /**
     * Appends a {@code Tf} operator for the given font name and size.
     *
     * <p>Behaviour depends on whether a font resolver has been attached:
     * <ul>
     *   <li><b>With resolver:</b> resolves {@code name} to its alias (e.g. {@code F1}),
     *       records the alias in the used-set, and writes {@code /F1 <size> Tf\n}.
     *   <li><b>Without resolver:</b> writes {@code /<name> <size> Tf\n} verbatim —
     *       preserving backward-compatible behaviour for standalone / test use.
     * </ul>
     *
     * @param name font name (Base-14) or alias when resolver is absent
     * @param size point size
     */
    public ContentStream setFont(String name, double size) {
        ensureOpen();
        String token;
        if (fontResolver != null) {
            token = fontResolver.apply(name);
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
        ensureOpen();
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
        ensureOpen();
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
        ensureOpen();
        buf.append('<').append(hexString).append("> Tj\n");
        return this;
    }

    /**
     * Appends {@code <FEFF...hex...> Tj\n} — show text encoded as a UTF-16BE
     * hex string with a leading BOM ({@code U+FEFF}).
     *
     * <p>This is a minimal Unicode path that does <b>not</b> require CIDFont,
     * ToUnicode CMaps, or font subsetting. The caller must ensure that the
     * active font's encoding can display the glyphs.
     *
     * <p>Uppercase hex digits are used for deterministic, spec-friendly output.
     *
     * @param text the Unicode string to encode (must not be {@code null})
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public ContentStream showTextUnicodeRaw(String text) {
        Objects.requireNonNull(text, "text");
        ensureOpen();
        buf.append('<');
        buf.append("FEFF");
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            buf.append(HEX_DIGITS[(ch >> 12) & 0xF]);
            buf.append(HEX_DIGITS[(ch >> 8)  & 0xF]);
            buf.append(HEX_DIGITS[(ch >> 4)  & 0xF]);
            buf.append(HEX_DIGITS[ ch        & 0xF]);
        }
        buf.append("> Tj\n");
        return this;
    }

    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    // ── Graphics operators ─────────────────────────────────────────────────

    /**
     * Appends {@code <x> <y> m\n} — moveto.
     */
    public ContentStream moveTo(double x, double y) {
        ensureOpen();
        buf.append(PdfWriter.formatReal(x))
           .append(' ').append(PdfWriter.formatReal(y))
           .append(" m\n");
        return this;
    }

    /**
     * Appends {@code <x> <y> l\n} — lineto.
     */
    public ContentStream lineTo(double x, double y) {
        ensureOpen();
        buf.append(PdfWriter.formatReal(x))
           .append(' ').append(PdfWriter.formatReal(y))
           .append(" l\n");
        return this;
    }

    /**
     * Appends {@code S\n} — stroke current path.
     */
    public ContentStream stroke() {
        ensureOpen();
        buf.append("S\n");
        return this;
    }

    /**
     * Appends {@code <x> <y> <w> <h> re\n} — append rectangle to path.
     */
    public ContentStream rectangle(double x, double y, double w, double h) {
        ensureOpen();
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
        ensureOpen();
        buf.append("f\n");
        return this;
    }

    /**
     * Appends {@code <w> w\n} — set line width.
     */
    public ContentStream setLineWidth(double w) {
        ensureOpen();
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
        ensureOpen();
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
        ensureOpen();
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
        ensureOpen();
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
        ensureOpen();
        requireUnit(gray, "gray");
        buf.append(PdfWriter.formatReal(gray)).append(" G\n");
        return this;
    }

    // ── Graphics state operators ────────────────────────────────────────────

    /** Appends {@code q\n} — save the current graphics state. */
    public ContentStream saveState() {
        ensureOpen();
        buf.append("q\n");
        return this;
    }

    /** Appends {@code Q\n} — restore the most recently saved graphics state. */
    public ContentStream restoreState() {
        ensureOpen();
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
        ensureOpen();
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
        ensureOpen();
        buf.append('/').append(name).append(" Do\n");
        return this;
    }

    // ── Output ─────────────────────────────────────────────────────────────

    /**
     * Returns the accumulated operators as US-ASCII bytes.
     * Output is deterministic; no trailing whitespace is added beyond operator newlines.
     *
     * <p>Copies directly from the StringBuilder char buffer to a byte array,
     * avoiding the intermediate {@code String} allocation that
     * {@code buf.toString().getBytes()} would create. Safe because all
     * content stream operators are 7-bit ASCII.
     *
     * <p>This method is available even after {@link #seal()}.
     */
    public byte[] toBytes() {
        int len = buf.length();
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = (byte) buf.charAt(i);
        }
        return result;
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    private static void requireUnit(double value, String name) {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(
                    name + " must be in [0, 1], got " + value);
        }
    }
}
