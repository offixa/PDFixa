package io.offixa.pdfixa.core.document;

import io.offixa.pdfixa.core.writer.PdfWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class XrefTableBuilderTest {

    private ByteArrayOutputStream baos;
    private PdfWriter writer;

    @BeforeEach
    void setUp() {
        baos = new ByteArrayOutputStream();
        writer = new PdfWriter(baos);
    }

    private String output() {
        return baos.toString(StandardCharsets.US_ASCII);
    }

    // ── Return value: startxref offset ─────────────────────────────

    @Test
    void returnedOffsetCapturedBeforeXrefKeyword() throws IOException {
        // Write some bytes first so the stream position is non-zero.
        writer.writeBytes("PADDING".getBytes(StandardCharsets.US_ASCII));
        long startxref = XrefTableBuilder.write(writer, new long[]{-1L, 0L}, 1);
        assertEquals(7L, startxref); // "PADDING" = 7 bytes
    }

    @Test
    void returnedOffsetIsZeroWhenWriterIsAtStart() throws IOException {
        long startxref = XrefTableBuilder.write(writer, new long[]{-1L, 0L}, 1);
        assertEquals(0L, startxref);
    }

    // ── xref keyword and subsection header ─────────────────────────

    @Test
    void outputBeginsWithXrefNewline() throws IOException {
        XrefTableBuilder.write(writer, new long[]{-1L, 0L}, 1);
        assertTrue(output().startsWith("xref\n"));
    }

    @Test
    void subsectionHeaderIsZeroToObjectCountPlusOne() throws IOException {
        XrefTableBuilder.write(writer, new long[]{-1L, 0L, 50L}, 2);
        // objectCount=2 → header "0 3\n"
        assertTrue(output().contains("0 3\n"));
    }

    @Test
    void subsectionHeaderForSingleObject() throws IOException {
        XrefTableBuilder.write(writer, new long[]{-1L, 0L}, 1);
        assertTrue(output().contains("0 2\n"));
    }

    // ── Entry format and byte count ────────────────────────────────

    @Test
    void freeEntryLiteralIsExactlyTwentyBytes() {
        // Verified statically — no I/O needed.
        byte[] entry = "0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII);
        assertEquals(20, entry.length);
    }

    @Test
    void inUseEntryIsExactlyTwentyBytes() throws IOException {
        XrefTableBuilder.write(writer, new long[]{-1L, 123L}, 1);
        byte[] bytes = baos.toByteArray();
        // "xref\n"=5  +  "0 2\n"=4  +  free(20)  = 29 bytes before the in-use entry
        int entryStart = 5 + 4 + 20;
        assertEquals(20, bytes.length - entryStart);
    }

    @Test
    void offsetZeroPaddedToTenDigits() throws IOException {
        XrefTableBuilder.write(writer, new long[]{-1L, 42L}, 1);
        assertTrue(output().contains("0000000042 00000 n \n"));
    }

    @Test
    void offsetAtMaxTenDigits() throws IOException {
        XrefTableBuilder.write(writer, new long[]{-1L, 9_999_999_999L}, 1);
        assertTrue(output().contains("9999999999 00000 n \n"));
    }

    @Test
    void freeEntryAppearsBeforeInUseEntries() throws IOException {
        XrefTableBuilder.write(writer, new long[]{-1L, 50L}, 1);
        String out = output();
        int freePos   = out.indexOf("0000000000 65535 f \n");
        int inUsePos  = out.indexOf("0000000050 00000 n \n");
        assertTrue(freePos < inUsePos);
    }

    // ── Full output snapshot ────────────────────────────────────────

    @Test
    void fullOutputForTwoObjects() throws IOException {
        XrefTableBuilder.write(writer, new long[]{-1L, 15L, 60L}, 2);
        String expected =
                "xref\n" +
                "0 3\n" +
                "0000000000 65535 f \n" +
                "0000000015 00000 n \n" +
                "0000000060 00000 n \n";
        assertEquals(expected, output());
    }

    // ── No carriage returns ────────────────────────────────────────

    @Test
    void noCarriageReturnsAnywhere() throws IOException {
        XrefTableBuilder.write(writer, new long[]{-1L, 0L, 50L, 100L}, 3);
        assertFalse(output().contains("\r"));
    }
}
