package io.offixa.pdfixa.core.document;

import io.offixa.pdfixa.core.writer.PdfWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class TrailerBuilderTest {

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

    // ── Full output snapshot ────────────────────────────────────────

    @Test
    void fullOutputMatchesSpec() throws IOException {
        TrailerBuilder.write(writer, 4, 1, 186L);
        String expected =
                "trailer\n" +
                "<< /Size 4 /Root 1 0 R >>\n" +
                "startxref\n" +
                "186\n" +
                "%%EOF\n";
        assertEquals(expected, output());
    }

    // ── Individual fields ──────────────────────────────────────────

    @Test
    void sizeIsWrittenCorrectly() throws IOException {
        TrailerBuilder.write(writer, 7, 1, 0L);
        assertTrue(output().contains("/Size 7"));
    }

    @Test
    void rootReferenceIsWrittenCorrectly() throws IOException {
        TrailerBuilder.write(writer, 4, 3, 0L);
        assertTrue(output().contains("/Root 3 0 R"));
    }

    @Test
    void startxrefValueIsWrittenOnOwnLine() throws IOException {
        TrailerBuilder.write(writer, 4, 1, 9999L);
        assertTrue(output().contains("startxref\n9999\n"));
    }

    // ── %%EOF correctness ──────────────────────────────────────────

    @Test
    void eofMarkerEndsOutput() throws IOException {
        TrailerBuilder.write(writer, 4, 1, 0L);
        assertTrue(output().endsWith("%%EOF\n"));
    }

    @Test
    void eofMarkerHasExactlyTwoPercentSigns() throws IOException {
        TrailerBuilder.write(writer, 4, 1, 0L);
        // writeComment("%%EOF") would produce %%%EOF — guard against that regression
        assertFalse(output().contains("%%%EOF"));
        assertTrue(output().contains("%%EOF"));
    }

    // ── Line endings ───────────────────────────────────────────────

    @Test
    void noCarriageReturnsAnywhere() throws IOException {
        TrailerBuilder.write(writer, 4, 1, 186L);
        assertFalse(output().contains("\r"));
    }

    @Test
    void everyLineEndsWithLF() throws IOException {
        TrailerBuilder.write(writer, 4, 1, 186L);
        // Split on \n — each "line" before the final \n should be non-empty
        String out = output();
        String[] lines = out.split("\n", -1);
        // Last element after trailing \n is empty — everything else must be non-empty
        for (int i = 0; i < lines.length - 1; i++) {
            assertFalse(lines[i].isEmpty(), "Line " + i + " should not be empty");
        }
        assertEquals("", lines[lines.length - 1], "Output must end with \\n");
    }
}
