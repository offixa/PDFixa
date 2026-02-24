package io.offixa.pdfixa;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sprint 2 hardening tests — verifies the structural integrity of the generated
 * PDF byte stream without using any PDF library.
 *
 * <p>All parsing is done manually via {@link ByteUtil}. US-ASCII encoding is used
 * for every string-to-byte conversion.
 */
class PdfStructureTest {

    private static final java.nio.charset.Charset ASCII = StandardCharsets.US_ASCII;

    /** Shared PDF bytes generated once for the whole test class. */
    private static byte[] PDF;

    @BeforeAll
    static void generatePdf() throws Exception {
        PDF = PdfGenerator.generate();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: startxref value points to the xref keyword
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void startxref_pointsToXref() {
        byte[] marker = "startxref\n".getBytes(ASCII);
        int markerPos = ByteUtil.lastIndexOf(PDF, marker);
        assertNotEquals(-1, markerPos, "startxref keyword not found in PDF");

        // The number immediately follows "startxref\n"
        int numStart = markerPos + marker.length;
        int numEnd   = numStart;
        while (numEnd < PDF.length && PDF[numEnd] != '\n') numEnd++;

        long xrefOffset = ByteUtil.parseAsciiLong(PDF, numStart, numEnd);

        byte[] expected = "xref\n".getBytes(ASCII);
        assertTrue(xrefOffset + expected.length <= PDF.length,
                "xrefOffset " + xrefOffset + " is out of bounds");

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], PDF[(int) xrefOffset + i],
                    "Byte " + i + " at xref offset: expected '"
                            + (char) expected[i] + "' (0x"
                            + Integer.toHexString(expected[i] & 0xFF) + ")");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: every xref entry line is exactly 20 bytes
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void xref_entry_lines_are_20_bytes() {
        byte[] xrefMarker = "xref\n".getBytes(ASCII);
        int xrefPos = ByteUtil.indexOf(PDF, xrefMarker, 0);
        assertNotEquals(-1, xrefPos, "xref keyword not found");

        // Skip "xref\n" (5 bytes) → subsection header
        int pos = xrefPos + xrefMarker.length;

        byte[] headerLine = ByteUtil.readLine(PDF, pos);
        assertNotNull(headerLine, "xref subsection header not found");
        String header = new String(headerLine, ASCII);
        assertTrue(header.startsWith("0 "), "Subsection header must begin with '0 '");

        int n = Integer.parseInt(header.substring(2).trim());
        pos += headerLine.length + 1; // +1 for the '\n' after the header

        byte[] freeEntry = "0000000000 65535 f \n".getBytes(ASCII);

        for (int i = 0; i < n; i++) {
            assertTrue(pos + 20 <= PDF.length,
                    "PDF too short to contain entry " + i);

            // Verify the fixed-width 20-byte structure
            assertEquals((byte) ' ',  PDF[pos + 18],
                    "Entry " + i + " byte[18] must be SP");
            assertEquals((byte) '\n', PDF[pos + 19],
                    "Entry " + i + " byte[19] must be LF");

            byte[] entry = new byte[20];
            System.arraycopy(PDF, pos, entry, 0, 20);

            if (i == 0) {
                assertArrayEquals(freeEntry, entry,
                        "Entry 0 must be the free-list head '0000000000 65535 f \\n'");
            } else {
                // In-use entries must end with " n \n" (bytes 16..19)
                assertEquals((byte) ' ',  entry[16], "Entry " + i + " byte[16] must be SP");
                assertEquals((byte) 'n',  entry[17], "Entry " + i + " byte[17] must be 'n'");
                assertEquals((byte) ' ',  entry[18], "Entry " + i + " byte[18] must be SP");
                assertEquals((byte) '\n', entry[19], "Entry " + i + " byte[19] must be LF");
            }

            pos += 20;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: xref offsets resolve to the correct object headers
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void xref_offsets_match_obj_headers() {
        byte[] xrefMarker = "xref\n".getBytes(ASCII);
        int xrefPos = ByteUtil.indexOf(PDF, xrefMarker, 0);
        assertNotEquals(-1, xrefPos, "xref keyword not found");

        int pos = xrefPos + xrefMarker.length;

        byte[] headerLine = ByteUtil.readLine(PDF, pos);
        assertNotNull(headerLine);
        int n = Integer.parseInt(new String(headerLine, ASCII).substring(2).trim());
        pos += headerLine.length + 1;

        // Skip free entry 0 — no object to validate for it
        pos += 20;

        for (int i = 1; i < n; i++) {
            // Offset is the first 10 ASCII-digit bytes of each entry
            long offset = ByteUtil.parseAsciiLong(PDF, pos, pos + 10);

            byte[] expected = (i + " 0 obj\n").getBytes(ASCII);
            assertTrue(offset + expected.length <= PDF.length,
                    "Offset " + offset + " for object " + i + " is out of bounds");

            for (int j = 0; j < expected.length; j++) {
                assertEquals(expected[j], PDF[(int) offset + j],
                        "Object " + i + " header byte[" + j + "]: expected '"
                                + (char) expected[j] + "' but found '"
                                + (char) PDF[(int) offset + j] + "'");
            }

            pos += 20;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 4: trailer dictionary contains /Size and /Root with correct values
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void trailer_has_root_and_size() {
        byte[] trailerMarker = "trailer\n".getBytes(ASCII);
        int trailerPos = ByteUtil.indexOf(PDF, trailerMarker, 0);
        assertNotEquals(-1, trailerPos, "trailer keyword not found");

        // The trailer dictionary is on the very next line after "trailer\n"
        int dictStart = trailerPos + trailerMarker.length;
        byte[] dictLine = ByteUtil.readLine(PDF, dictStart);
        assertNotNull(dictLine, "trailer dictionary line not found");
        String dictText = new String(dictLine, ASCII);

        assertTrue(dictText.contains("/Size"), "Trailer dict must contain /Size");
        assertTrue(dictText.contains("/Root"), "Trailer dict must contain /Root");

        // ── Parse /Size ───────────────────────────────────────────────────────
        int sizeTokenStart = dictText.indexOf("/Size") + "/Size ".length();
        int sizeTokenEnd   = sizeTokenStart;
        while (sizeTokenEnd < dictText.length()
                && Character.isDigit(dictText.charAt(sizeTokenEnd))) {
            sizeTokenEnd++;
        }
        int actualSize = Integer.parseInt(dictText.substring(sizeTokenStart, sizeTokenEnd));
        assertEquals(PdfGenerator.OBJECT_COUNT + 1, actualSize,
                "/Size must equal objectCount+1 (" + (PdfGenerator.OBJECT_COUNT + 1) + ")");

        // ── Parse /Root ───────────────────────────────────────────────────────
        int rootTokenStart = dictText.indexOf("/Root") + "/Root ".length();
        String rootRegion  = dictText.substring(rootTokenStart).stripLeading();
        String expectedRef = PdfGenerator.ROOT_OBJECT_NUM + " 0 R";
        assertTrue(rootRegion.startsWith(expectedRef),
                "/Root must begin with '" + expectedRef + "' but found: '" + rootRegion + "'");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 5: generating the PDF twice produces identical byte arrays
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void deterministic_output() throws Exception {
        byte[] first  = PdfGenerator.generate();
        byte[] second = PdfGenerator.generate();
        assertArrayEquals(first, second,
                "Two PDF generations in the same JVM must produce identical bytes");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 6: content stream bytes appear verbatim in the file
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void contentStream_bytes_appear_in_file() {
        // Verify each key line of the content stream is present in the output
        byte[] streamMarker  = "stream\n".getBytes(ASCII);
        byte[] endStream     = "endstream".getBytes(ASCII);
        byte[] btLine        = "BT\n".getBytes(ASCII);
        byte[] helloLine     = "(Hello PDFixa) Tj\n".getBytes(ASCII);
        byte[] etToken       = "ET".getBytes(ASCII);

        assertNotEquals(-1, ByteUtil.indexOf(PDF, streamMarker, 0),
                "\"stream\" keyword not found in PDF");
        assertNotEquals(-1, ByteUtil.indexOf(PDF, endStream, 0),
                "\"endstream\" keyword not found in PDF");
        assertNotEquals(-1, ByteUtil.indexOf(PDF, btLine, 0),
                "\"BT\" operator not found in content stream");
        assertNotEquals(-1, ByteUtil.indexOf(PDF, helloLine, 0),
                "\"(Hello PDFixa) Tj\" not found in content stream");
        assertNotEquals(-1, ByteUtil.indexOf(PDF, etToken, 0),
                "\"ET\" operator not found in content stream");

        // Verify /Length matches the actual raw byte count
        byte[] lengthKey = "/Length ".getBytes(ASCII);
        int lengthPos = ByteUtil.indexOf(PDF, lengthKey, 0);
        assertNotEquals(-1, lengthPos, "/Length key not found in stream dictionary");

        int numStart = lengthPos + lengthKey.length;
        int numEnd   = numStart;
        while (numEnd < PDF.length && PDF[numEnd] >= '0' && PDF[numEnd] <= '9') numEnd++;
        long declaredLength = ByteUtil.parseAsciiLong(PDF, numStart, numEnd);

        assertEquals(PdfGenerator.CONTENT_STREAM_BYTES.length, declaredLength,
                "/Length must equal the raw content byte count ("
                        + PdfGenerator.CONTENT_STREAM_BYTES.length + ")");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 7: content stream contains all expected graphics operator sequences
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void contentStream_containsGraphicsOperators() {
        // Line width setting
        assertSeqPresent("2 w\n",             "line-width operator '2 w'");

        // Horizontal line: moveto → lineto → stroke
        assertSeqPresent("50 650 m\n",        "moveto '50 650 m'");
        assertSeqPresent("300 650 l\n",       "lineto '300 650 l'");

        // Stroke-only rectangle
        assertSeqPresent("50 500 200 100 re\n", "rect '50 500 200 100 re'");

        // Filled rectangle
        assertSeqPresent("300 500 100 100 re\n", "rect '300 500 100 100 re'");
        assertSeqPresent("300 500 100 100 re\nf", "fill operator 'f' after filled rect");

        // The standalone S stroke operators are present after the re sequences
        int reStrokeIdx = ByteUtil.indexOf(PDF, "50 500 200 100 re\nS".getBytes(ASCII), 0);
        assertNotEquals(-1, reStrokeIdx,
                "Stroked rectangle sequence '50 500 200 100 re\\nS' not found in PDF");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 8: Pages object declares /Count 2 and /Kids with both page refs
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void pages_object_has_count2_and_both_kids() {
        byte[] objHeader = "2 0 obj\n".getBytes(ASCII);
        int objStart = ByteUtil.indexOf(PDF, objHeader, 0);
        assertNotEquals(-1, objStart, "Pages object (2 0 obj) not found in PDF");

        byte[] endobjMarker = "endobj\n".getBytes(ASCII);
        int objEnd = ByteUtil.indexOf(PDF, endobjMarker, objStart);
        assertNotEquals(-1, objEnd, "endobj not found after Pages object");

        int bodyStart = objStart + objHeader.length;
        byte[] body = new byte[objEnd - bodyStart];
        System.arraycopy(PDF, bodyStart, body, 0, body.length);
        String bodyText = new String(body, ASCII);

        assertTrue(bodyText.contains("/Count 2"),
                "Pages object must contain /Count 2, got: " + bodyText);
        assertTrue(bodyText.contains("3 0 R"),
                "Pages /Kids must contain ref to Page1 (3 0 R), got: " + bodyText);
        assertTrue(bodyText.contains("5 0 R"),
                "Pages /Kids must contain ref to Page2 (5 0 R), got: " + bodyText);
    }

    /** Asserts that {@code sequence} (US-ASCII) appears at least once in {@code PDF}. */
    private static void assertSeqPresent(String sequence, String description) {
        int idx = ByteUtil.indexOf(PDF, sequence.getBytes(ASCII), 0);
        assertNotEquals(-1, idx, "Expected graphics sequence not found: " + description
                + " — sequence: \"" + sequence.replace("\n", "\\n") + "\"");
    }
}
