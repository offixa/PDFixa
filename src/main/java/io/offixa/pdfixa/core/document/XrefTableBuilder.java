package io.offixa.pdfixa.core.document;

import io.offixa.pdfixa.core.writer.PdfWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Writes a classic PDF cross-reference table (§ 7.5.4 of PDF 1.7 spec).
 *
 * <p>Each xref entry is exactly 20 bytes, using the " \n" (space + LF)
 * two-byte end-of-line sequence mandated by the spec (the alternative is
 * CR+LF, but "\n" alone would give only 19 bytes and violate the fixed-width
 * requirement that every conforming reader depends on for random access).
 */
public final class XrefTableBuilder {

    private XrefTableBuilder() {}

    /**
     * Writes the xref table for {@code objectCount} objects and returns the
     * byte offset where the {@code xref} keyword begins (the {@code startxref} value).
     *
     * @param writer      the writer positioned immediately after the last object
     * @param offsets     offset array from {@link ObjectRegistry#getOffsets()};
     *                    {@code offsets[i]} is the byte offset of object {@code i}
     * @param objectCount number of real objects (length of the xref section minus the free entry)
     * @return the byte offset of the {@code xref} keyword
     */
    public static long write(PdfWriter writer, long[] offsets, int objectCount) throws IOException {
        long startxrefOffset = writer.getPosition();

        writer.writeBytes("xref\n".getBytes(StandardCharsets.US_ASCII));
        writer.writeBytes(("0 " + (objectCount + 1) + "\n").getBytes(StandardCharsets.US_ASCII));

        // Free-list head: object 0, generation 65535, type 'f'
        writer.writeBytes("0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII));

        for (int i = 1; i <= objectCount; i++) {
            String entry = String.format(Locale.ROOT, "%010d 00000 n \n", offsets[i]);
            writer.writeBytes(entry.getBytes(StandardCharsets.US_ASCII));
        }

        return startxrefOffset;
    }
}
