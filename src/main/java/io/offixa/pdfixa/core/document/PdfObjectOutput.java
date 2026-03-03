package io.offixa.pdfixa.core.document;

import java.io.IOException;

/**
 * Public, high-level facade for writing PDF tokens inside an indirect object
 * body.
 *
 * <p>Implementations delegate to the internal {@code PdfWriter}, but that type
 * is never exposed to callers. Whitespace between tokens is managed
 * automatically — callers never need to insert explicit spaces.
 *
 * <p>This interface is consumed by {@link PdfObjectWriter} lambdas that are
 * passed to {@link PdfDocumentContext#setObjectBody}.
 */
public interface PdfObjectOutput {

    /**
     * Writes a PDF name token ({@code /Name}).
     *
     * @param name the name without the leading slash
     */
    void name(String name) throws IOException;

    /**
     * Writes a PDF literal string token ({@code (text)}).
     *
     * @param value the string value; parentheses and backslashes are escaped
     */
    void string(String value) throws IOException;

    /**
     * Writes a PDF real-number token.
     *
     * @param value the numeric value (no exponential notation)
     */
    void number(double value) throws IOException;

    /**
     * Writes a PDF integer token.
     *
     * @param value the integer value
     */
    void integer(int value) throws IOException;

    /**
     * Writes an indirect reference token ({@code objNum 0 R}).
     *
     * @param objNum the target object number
     */
    void reference(int objNum) throws IOException;

    /** Writes {@code <<} — begin dictionary. */
    void beginDictionary() throws IOException;

    /** Writes {@code >>} — end dictionary. */
    void endDictionary() throws IOException;

    /** Writes {@code [} — begin array. */
    void beginArray() throws IOException;

    /** Writes {@code ]} — end array. */
    void endArray() throws IOException;

    /**
     * Writes a complete PDF stream: dictionary header ({@code /Length}),
     * {@code stream}/{@code endstream} wrapper, and the raw bytes.
     *
     * @param data the uncompressed stream bytes
     */
    void beginStream(byte[] data) throws IOException;

    /**
     * Writes a TrueType font file stream with {@code /Length} and {@code /Length1}
     * declared in the stream dictionary, as required by PDF spec §9.9.
     *
     * @param ttfBytes raw TrueType font bytes
     */
    void beginFontFileStream(byte[] ttfBytes) throws IOException;

    // ── Forward-compatible default methods ──────────────────────────────────
    // Added in 0.9.0 so that future expansion does not break existing
    // PdfObjectWriter lambdas or third-party PdfObjectOutput implementations.

    /**
     * Writes raw bytes verbatim into the object body.
     *
     * @param data bytes to write
     */
    default void rawBytes(byte[] data) throws IOException {
        throw new UnsupportedOperationException(
                "rawBytes is not supported by this PdfObjectOutput implementation");
    }

    /**
     * Writes a PDF boolean token ({@code true} / {@code false}).
     *
     * @param value the boolean value
     */
    default void bool(boolean value) throws IOException {
        throw new UnsupportedOperationException(
                "bool is not supported by this PdfObjectOutput implementation");
    }

    /**
     * Writes the PDF {@code null} object.
     */
    default void nullValue() throws IOException {
        throw new UnsupportedOperationException(
                "nullValue is not supported by this PdfObjectOutput implementation");
    }

    /**
     * Writes a PDF comment ({@code % text}).
     *
     * @param text comment text (without the leading {@code %})
     */
    default void comment(String text) throws IOException {
        throw new UnsupportedOperationException(
                "comment is not supported by this PdfObjectOutput implementation");
    }
}
