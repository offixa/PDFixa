package io.offixa.pdfixa.core.internal;

import io.offixa.pdfixa.core.internal.PdfWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Writes a classic PDF cross-reference table (§ 7.5.4 of PDF 1.7 spec).
 *
 * <p>Each xref entry is exactly 20 bytes, using the " \n" (space + LF)
 * two-byte end-of-line sequence mandated by the spec (the alternative is
 * CR+LF, but "\n" alone would give only 19 bytes and violate the fixed-width
 * requirement that every conforming reader depends on for random access).
 */
public final class XrefTableBuilder {

    private static final byte[] XREF_KEYWORD =
            "xref\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] FREE_ENTRY =
            "0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII);

    private XrefTableBuilder() {}

    /**
     * Writes the xref table for {@code objectCount} objects and returns the
     * byte offset where the {@code xref} keyword begins (the {@code startxref} value).
     *
     * <p>Uses a single reusable 20-byte buffer for all in-use entries to
     * eliminate per-entry {@code String.format()} and {@code getBytes()} allocations.
     *
     * @param writer      the writer positioned immediately after the last object
     * @param offsets     offset array from {@link ObjectRegistry#getOffsets()};
     *                    {@code offsets[i]} is the byte offset of object {@code i}
     * @param objectCount number of real objects (length of the xref section minus the free entry)
     * @return the byte offset of the {@code xref} keyword
     */
    public static long write(PdfWriter writer, long[] offsets, int objectCount) throws IOException {
        long startxrefOffset = writer.getPosition();

        writer.writeBytes(XREF_KEYWORD);
        writer.writeBytes(("0 " + (objectCount + 1) + "\n").getBytes(StandardCharsets.US_ASCII));
        writer.writeBytes(FREE_ENTRY);

        // Fixed suffix: " 00000 n \n"  (bytes 10–19)
        byte[] entry = new byte[20];
        entry[10] = ' ';
        entry[11] = '0'; entry[12] = '0'; entry[13] = '0'; entry[14] = '0'; entry[15] = '0';
        entry[16] = ' ';
        entry[17] = 'n';
        entry[18] = ' ';
        entry[19] = '\n';

        for (int i = 1; i <= objectCount; i++) {
            long off = offsets[i];
            for (int d = 9; d >= 0; d--) {
                entry[d] = (byte) ('0' + (off % 10));
                off /= 10;
            }
            writer.writeBytes(entry);
        }

        return startxrefOffset;
    }
}
