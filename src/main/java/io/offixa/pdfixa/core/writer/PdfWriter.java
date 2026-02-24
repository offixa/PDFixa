package io.offixa.pdfixa.core.writer;

import io.offixa.pdfixa.core.io.CountingOutputStream;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/**
 * Low-level byte serializer for PDF 1.7 syntax tokens.
 *
 * <p>This class writes individual PDF tokens (numbers, names, strings, booleans,
 * dictionaries, arrays, indirect object wrappers) to a byte stream while tracking
 * the exact byte offset of every write. It enforces ASCII-safe output with
 * platform-independent line endings ({@code \n} only).
 *
 * <p><strong>Design contract:</strong> PdfWriter writes exactly what you tell it
 * to write. It does not insert implicit whitespace between tokens — the caller
 * controls spacing via {@link #writeSpace()} and {@link #writeNewline()}.
 * This ensures deterministic, byte-reproducible output.
 *
 * <p>This class is not thread-safe.
 */
public final class PdfWriter implements Closeable {

    private static final byte[] NEWLINE = {'\n'};
    private static final byte[] SPACE = {' '};

    private static final byte[] OBJ_INTRO = " obj\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] OBJ_OUTRO = "endobj\n".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] STREAM_BEGIN = "stream\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] STREAM_END   = "\nendstream".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] DICT_OPEN = "<<".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] DICT_CLOSE = ">>".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] BOOL_TRUE = "true".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] BOOL_FALSE = "false".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] NULL_TOKEN = "null".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] REF_SUFFIX = " R".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] HEX_DIGITS = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    private final CountingOutputStream out;

    public PdfWriter(OutputStream out) {
        this.out = new CountingOutputStream(Objects.requireNonNull(out, "out"));
    }

    // ── Position tracking ──────────────────────────────────────────────

    /**
     * Returns the current byte offset from the start of the stream.
     * Used to record object positions for the cross-reference table.
     */
    public long getPosition() {
        return out.getPosition();
    }

    // ── Raw output ─────────────────────────────────────────────────────

    /**
     * Writes raw bytes with no interpretation or escaping.
     * Use for pre-encoded data such as stream content or the PDF header.
     */
    public void writeBytes(byte[] data) throws IOException {
        Objects.requireNonNull(data, "data");
        out.write(data);
    }

    /** Writes a portion of a byte array with no interpretation. */
    public void writeBytes(byte[] data, int off, int len) throws IOException {
        Objects.requireNonNull(data, "data");
        out.write(data, off, len);
    }

    /** Writes a single {@code \n}. Never {@code \r\n}. */
    public void writeNewline() throws IOException {
        out.write(NEWLINE);
    }

    /** Writes a single ASCII space. */
    public void writeSpace() throws IOException {
        out.write(SPACE);
    }

    // ── PDF scalar tokens ──────────────────────────────────────────────

    /** Writes an integer token. Example output: {@code 42} */
    public void writeInt(int value) throws IOException {
        writeAscii(Integer.toString(value));
    }

    /** Writes a long integer token. Example output: {@code 100000} */
    public void writeLong(long value) throws IOException {
        writeAscii(Long.toString(value));
    }

    /**
     * Writes a real number token per PDF 1.7 spec (no exponential notation).
     * Integral values are written without a decimal point.
     * Up to 6 fractional digits; trailing zeros are stripped.
     */
    public void writeReal(double value) throws IOException {
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("NaN is not a valid PDF real number");
        }
        if (Double.isInfinite(value)) {
            throw new IllegalArgumentException("Infinite values are not valid PDF real numbers");
        }
        writeAscii(formatReal(value));
    }

    /**
     * Writes a PDF name object: {@code /Name}.
     *
     * <p>Characters outside the regular printable ASCII range (0x21–0x7E)
     * and PDF delimiter characters are escaped as {@code #XX}.
     *
     * @param name the name <em>without</em> the leading slash
     */
    public void writeName(String name) throws IOException {
        Objects.requireNonNull(name, "name");
        out.write('/');
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch > 0xFF) {
                throw new IllegalArgumentException(
                        "PDF name contains non-Latin-1 character U+" + Integer.toHexString(ch).toUpperCase() + " at index " + i);
            }
            if (mustEscapeInName(ch)) {
                out.write('#');
                writeHexByte((byte) ch);
            } else {
                out.write(ch);
            }
        }
    }

    /**
     * Writes a PDF literal string: {@code (text)}.
     *
     * <p>Parentheses and backslashes inside the string are escaped.
     * Only bytes in the Latin-1 range are supported (PDF standard encoding).
     */
    public void writeLiteralString(String value) throws IOException {
        Objects.requireNonNull(value, "value");
        out.write('(');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch > 0xFF) {
                throw new IllegalArgumentException(
                        "PDF literal string contains non-Latin-1 character U+" + Integer.toHexString(ch).toUpperCase() + " at index " + i);
            }
            switch (ch) {
                case '('  -> { out.write('\\'); out.write('(');  }
                case ')'  -> { out.write('\\'); out.write(')');  }
                case '\\' -> { out.write('\\'); out.write('\\'); }
                case '\n' -> { out.write('\\'); out.write('n');  }
                case '\r' -> { out.write('\\'); out.write('r');  }
                case '\t' -> { out.write('\\'); out.write('t');  }
                case '\b' -> { out.write('\\'); out.write('b');  }
                case '\f' -> { out.write('\\'); out.write('f');  }
                default   -> out.write(ch & 0xFF);
            }
        }
        out.write(')');
    }

    /** Writes a PDF hex string: {@code <4E6F>}. */
    public void writeHexString(byte[] data) throws IOException {
        Objects.requireNonNull(data, "data");
        out.write('<');
        for (byte b : data) {
            writeHexByte(b);
        }
        out.write('>');
    }

    /** Writes {@code true} or {@code false}. */
    public void writeBoolean(boolean value) throws IOException {
        out.write(value ? BOOL_TRUE : BOOL_FALSE);
    }

    /** Writes the PDF {@code null} keyword. */
    public void writeNull() throws IOException {
        out.write(NULL_TOKEN);
    }

    /**
     * Writes an indirect reference token: {@code objNum gen R}.
     * Example output: {@code 4 0 R}
     */
    public void writeReference(int objNum, int gen) throws IOException {
        writeAscii(Integer.toString(objNum));
        out.write(' ');
        writeAscii(Integer.toString(gen));
        out.write(REF_SUFFIX);
    }

    // ── Stream objects ─────────────────────────────────────────────────

    /**
     * Writes a PDF stream body (no compression, Phase 1).
     *
     * <p>Produces exactly:
     * <pre>
     * &lt;&lt; /Length N &gt;&gt;\n
     * stream\n
     * &lt;raw bytes&gt;\n
     * endstream
     * </pre>
     * where {@code N} is {@code data.length} — the raw byte count only,
     * not including any surrounding newlines.
     *
     * <p>This method is intended to be used as a {@link PdfSerializable} body
     * inside an indirect object; the surrounding {@code obj}/{@code endobj}
     * wrappers are managed by {@link io.offixa.pdfixa.core.document.ObjectRegistry}.
     */
    public void writeStream(byte[] data) throws IOException {
        Objects.requireNonNull(data, "data");
        out.write(DICT_OPEN);
        out.write(SPACE);
        writeName("Length");
        out.write(SPACE);
        writeInt(data.length);
        out.write(SPACE);
        out.write(DICT_CLOSE);
        out.write(NEWLINE);
        out.write(STREAM_BEGIN);
        out.write(data);
        out.write(STREAM_END);
    }

    // ── Indirect object wrappers ───────────────────────────────────────

    /**
     * Writes the start of an indirect object definition: {@code N G obj\n}.
     *
     * @return the byte offset where this object begins — store this for the xref table
     */
    public long beginObject(int objNum, int gen) throws IOException {
        long offset = out.getPosition();
        writeAscii(Integer.toString(objNum));
        out.write(' ');
        writeAscii(Integer.toString(gen));
        out.write(OBJ_INTRO);
        return offset;
    }

    /** Writes {@code endobj\n}. */
    public void endObject() throws IOException {
        out.write(OBJ_OUTRO);
    }

    // ── Composite structure delimiters ─────────────────────────────────

    /** Writes {@code <<} (dictionary open). */
    public void beginDictionary() throws IOException {
        out.write(DICT_OPEN);
    }

    /** Writes {@code >>} (dictionary close). */
    public void endDictionary() throws IOException {
        out.write(DICT_CLOSE);
    }

    /** Writes {@code [} (array open). */
    public void beginArray() throws IOException {
        out.write('[');
    }

    /** Writes {@code ]} (array close). */
    public void endArray() throws IOException {
        out.write(']');
    }

    // ── Comment ────────────────────────────────────────────────────────

    /**
     * Writes a PDF comment line: {@code %text\n}.
     * The caller must not include the leading {@code %}.
     */
    public void writeComment(String text) throws IOException {
        Objects.requireNonNull(text, "text");
        out.write('%');
        writeAscii(text);
        writeNewline();
    }

    // ── Lifecycle ──────────────────────────────────────────────────────

    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    // ── Internal helpers ───────────────────────────────────────────────

    private void writeAscii(String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.US_ASCII));
    }

    private void writeHexByte(byte b) throws IOException {
        int unsigned = b & 0xFF;
        out.write(HEX_DIGITS[unsigned >>> 4]);
        out.write(HEX_DIGITS[unsigned & 0x0F]);
    }

    private static boolean mustEscapeInName(char ch) {
        if (ch < 0x21 || ch > 0x7E) return true;
        return switch (ch) {
            case '#', '(', ')', '<', '>', '[', ']', '{', '}', '/', '%' -> true;
            default -> false;
        };
    }

    /**
     * Formats a double as a PDF real-number token.
     * No exponential notation. Trailing fractional zeros are stripped.
     * Integral values omit the decimal point entirely.
     */
    static String formatReal(double value) {
        long longVal = (long) value;
        if (value == longVal) {
            return Long.toString(longVal);
        }

        BigDecimal bd = BigDecimal.valueOf(value)
                .setScale(6, RoundingMode.HALF_UP)
                .stripTrailingZeros();

        String s = bd.toPlainString();

        // Avoid "-0"
        if (s.equals("-0")) {
            return "0";
        }

        return s;
    }
}
