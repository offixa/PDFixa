package io.offixa.pdfixa.core.internal;

import io.offixa.pdfixa.core.internal.PdfWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Writes the PDF trailer section (§ 7.5.5 of PDF 1.7 spec):
 * the trailer dictionary, {@code startxref} offset, and {@code %%EOF} marker.
 */
public final class TrailerBuilder {

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
        writer.writeBytes("trailer\n".getBytes(StandardCharsets.US_ASCII));
        writer.writeBytes(
                ("<< /Size " + size + " /Root " + rootObjNum + " 0 R >>\n")
                        .getBytes(StandardCharsets.US_ASCII));
        writer.writeBytes("startxref\n".getBytes(StandardCharsets.US_ASCII));
        writer.writeBytes((startxref + "\n").getBytes(StandardCharsets.US_ASCII));
        // writeComment() would produce "%%%EOF"; raw bytes are required here.
        writer.writeBytes("%%EOF\n".getBytes(StandardCharsets.US_ASCII));
    }
}
