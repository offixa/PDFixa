package io.offixa.pdfixa.core.internal;

import io.offixa.pdfixa.core.internal.PdfWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Writes the PDF trailer section (§ 7.5.5 of PDF 1.7 spec):
 * the trailer dictionary, {@code startxref} offset, and {@code %%EOF} marker.
 */
public final class TrailerBuilder {

    private static final byte[] TRAILER = "trailer\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] STARTXREF = "startxref\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] EOF = "%%EOF\n".getBytes(StandardCharsets.US_ASCII);

    private TrailerBuilder() {}

    /**
     * Writes the complete trailer block.
     *
     * @param writer     the writer positioned immediately after the xref table
     * @param size       total number of objects including the free entry (objectCount + 1)
     * @param rootObjNum object number of the document catalog
     * @param startxref  byte offset of the {@code xref} keyword, as returned by
     *                   {@link io.offixa.pdfixa.core.internal.XrefTableBuilder#write}
     */
    public static void write(PdfWriter writer, int size, int rootObjNum, long startxref)
            throws IOException {
        writer.writeBytes(TRAILER);
        writer.beginDictionary();
        writer.writeSpace();
        writer.writeName("Size");
        writer.writeSpace();
        writer.writeInt(size);
        writer.writeSpace();
        writer.writeName("Root");
        writer.writeSpace();
        writer.writeReference(rootObjNum, 0);
        writer.writeSpace();
        writer.endDictionary();
        writer.writeNewline();
        writer.writeBytes(STARTXREF);
        writer.writeLong(startxref);
        writer.writeNewline();
        // writeComment() would produce "%%%EOF"; raw bytes are required here.
        writer.writeBytes(EOF);
    }

    /**
     * Writes the complete trailer block with {@code /Info} and deterministic {@code /ID}.
     */
    public static void write(
            PdfWriter writer,
            int size,
            int rootObjNum,
            int infoObjNum,
            byte[] fileId,
            long startxref) throws IOException {
        Objects.requireNonNull(fileId, "fileId");

        writer.writeBytes(TRAILER);
        writer.beginDictionary();
        writer.writeSpace();
        writer.writeName("Size");
        writer.writeSpace();
        writer.writeInt(size);
        writer.writeSpace();
        writer.writeName("Root");
        writer.writeSpace();
        writer.writeReference(rootObjNum, 0);
        writer.writeSpace();
        writer.writeName("Info");
        writer.writeSpace();
        writer.writeReference(infoObjNum, 0);
        writer.writeSpace();
        writer.writeName("ID");
        writer.writeSpace();
        writer.beginArray();
        writer.writeHexString(fileId);
        writer.writeSpace();
        writer.writeHexString(fileId);
        writer.endArray();
        writer.writeSpace();
        writer.endDictionary();
        writer.writeNewline();
        writer.writeBytes(STARTXREF);
        writer.writeLong(startxref);
        writer.writeNewline();
        // writeComment() would produce "%%%EOF"; raw bytes are required here.
        writer.writeBytes(EOF);
    }
}
