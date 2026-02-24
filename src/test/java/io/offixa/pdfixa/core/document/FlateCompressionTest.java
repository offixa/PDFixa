package io.offixa.pdfixa.core.document;

import io.offixa.pdfixa.ByteUtil;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.Inflater;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies Phase 2 Step 3 — Flate compression for content streams.
 *
 * <p>Tests confirm that:
 * <ul>
 *   <li>{@code /Filter /FlateDecode} appears in every content stream dictionary.
 *   <li>{@code /Length} equals the compressed (not raw) byte count.
 *   <li>Inflating the stream bytes produces the original PDF operators.
 *   <li>Output is deterministic across multiple {@code save()} calls.
 *   <li>An empty content stream still produces a valid compressed stream.
 * </ul>
 */
class FlateCompressionTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static byte[] buildSinglePagePdf(String text) throws Exception {
        PdfDocument doc = new PdfDocument();
        PdfPage page = doc.addPage();
        page.getContent()
            .beginText()
            .setFont("Helvetica", 12)
            .moveText(100, 700)
            .showText(text)
            .endText();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        return baos.toByteArray();
    }

    /**
     * Locates content stream object {@code objNum}, reads {@code /Length} bytes
     * from after {@code stream\n}, and inflates them.
     */
    private static byte[] extractAndInflate(byte[] pdf, int objNum) throws Exception {
        byte[] objMarker    = (objNum + " 0 obj\n").getBytes(StandardCharsets.US_ASCII);
        byte[] lengthKey    = "/Length ".getBytes(StandardCharsets.US_ASCII);
        byte[] streamMarker = "stream\n".getBytes(StandardCharsets.US_ASCII);

        int objPos = ByteUtil.indexOf(pdf, objMarker, 0);
        assertNotEquals(-1, objPos, "Object " + objNum + " 0 obj not found");

        int lengthPos = ByteUtil.indexOf(pdf, lengthKey, objPos);
        assertNotEquals(-1, lengthPos, "/Length key not found in object " + objNum);

        int numStart = lengthPos + lengthKey.length;
        int numEnd   = numStart;
        while (numEnd < pdf.length && pdf[numEnd] >= '0' && pdf[numEnd] <= '9') numEnd++;
        int declaredLength = (int) ByteUtil.parseAsciiLong(pdf, numStart, numEnd);

        int streamPos = ByteUtil.indexOf(pdf, streamMarker, objPos);
        assertNotEquals(-1, streamPos, "stream marker not found in object " + objNum);

        int dataStart  = streamPos + streamMarker.length;
        byte[] compressed = new byte[declaredLength];
        System.arraycopy(pdf, dataStart, compressed, 0, declaredLength);

        Inflater inflater = new Inflater(false);
        inflater.setInput(compressed);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        while (!inflater.finished()) {
            int n = inflater.inflate(buf);
            if (n > 0) out.write(buf, 0, n);
            else if (inflater.needsInput()) break;
        }
        inflater.end();
        return out.toByteArray();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void filter_flate_decode_appears_in_pdf() throws Exception {
        byte[] pdf     = buildSinglePagePdf("Hello Flate");
        String pdfText = new String(pdf, StandardCharsets.US_ASCII);
        assertTrue(pdfText.contains("/Filter /FlateDecode"),
                "/Filter /FlateDecode must appear in the content stream dictionary");
    }

    @Test
    void inflated_stream_contains_text_operators() throws Exception {
        byte[] pdf      = buildSinglePagePdf("Hello Flate");
        byte[] inflated = extractAndInflate(pdf, 4);
        String content  = new String(inflated, StandardCharsets.US_ASCII);

        assertTrue(content.contains("BT"),  "Inflated stream must contain 'BT'");
        assertTrue(content.contains("Tf"),  "Inflated stream must contain 'Tf'");
        assertTrue(content.contains("Td"),  "Inflated stream must contain 'Td'");
        assertTrue(content.contains("Tj"),  "Inflated stream must contain 'Tj'");
        assertTrue(content.contains("ET"),  "Inflated stream must contain 'ET'");
    }

    @Test
    void inflated_stream_contains_show_text_operator() throws Exception {
        byte[] pdf      = buildSinglePagePdf("Hello Flate");
        byte[] inflated = extractAndInflate(pdf, 4);
        String content  = new String(inflated, StandardCharsets.US_ASCII);

        assertTrue(content.contains("(Hello Flate) Tj"),
                "Inflated stream must contain the showText operator with the original string");
    }

    @Test
    void inflated_stream_contains_font_operator() throws Exception {
        byte[] pdf      = buildSinglePagePdf("Font test");
        byte[] inflated = extractAndInflate(pdf, 4);
        String content  = new String(inflated, StandardCharsets.US_ASCII);

        assertTrue(content.contains("/F1 12 Tf"),
                "Inflated stream must contain the font alias operator '/F1 12 Tf'");
    }

    @Test
    void length_equals_compressed_byte_count() throws Exception {
        byte[] pdf = buildSinglePagePdf("Length test");

        byte[] lengthKey = "/Length ".getBytes(StandardCharsets.US_ASCII);
        byte[] objMarker = "4 0 obj\n".getBytes(StandardCharsets.US_ASCII);
        byte[] streamMrk = "stream\n".getBytes(StandardCharsets.US_ASCII);
        byte[] endStream = "\nendstream".getBytes(StandardCharsets.US_ASCII);

        int objPos = ByteUtil.indexOf(pdf, objMarker, 0);
        assertNotEquals(-1, objPos, "Content object 4 0 obj not found");

        int lPos = ByteUtil.indexOf(pdf, lengthKey, objPos);
        int ns   = lPos + lengthKey.length;
        int ne   = ns;
        while (ne < pdf.length && pdf[ne] >= '0' && pdf[ne] <= '9') ne++;
        int declaredLength = (int) ByteUtil.parseAsciiLong(pdf, ns, ne);

        int dataStart = ByteUtil.indexOf(pdf, streamMrk, objPos) + streamMrk.length;
        int dataEnd   = ByteUtil.indexOf(pdf, endStream,  dataStart);
        assertNotEquals(-1, dataEnd, "endstream marker not found");

        int actualLength = dataEnd - dataStart;
        assertEquals(declaredLength, actualLength,
                "/Length must equal the actual number of compressed bytes in the stream");
    }

    @Test
    void compression_output_is_deterministic() throws Exception {
        byte[] first  = buildSinglePagePdf("Determinism");
        byte[] second = buildSinglePagePdf("Determinism");
        assertArrayEquals(first, second,
                "Two save() calls with identical content must produce byte-identical PDFs");
    }

    @Test
    void empty_content_stream_produces_valid_compressed_stream() throws Exception {
        PdfDocument doc = new PdfDocument();
        doc.addPage(); // no drawing operators
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        byte[] pdf     = baos.toByteArray();
        String pdfText = new String(pdf, StandardCharsets.US_ASCII);

        assertTrue(pdfText.contains("/Filter /FlateDecode"),
                "Empty stream must still carry /Filter /FlateDecode");

        byte[] inflated = extractAndInflate(pdf, 4);
        assertEquals(0, inflated.length,
                "Inflating a compressed empty stream must yield zero bytes");
    }

    @Test
    void multipage_each_content_stream_has_filter() throws Exception {
        PdfDocument doc = new PdfDocument();
        PdfPage p1 = doc.addPage();
        p1.getContent().beginText().setFont("Helvetica", 12).showText("Page 1").endText();
        PdfPage p2 = doc.addPage();
        p2.getContent().beginText().setFont("Helvetica", 12).showText("Page 2").endText();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        byte[] pdf = baos.toByteArray();

        // obj 4 = contents of page 1, obj 6 = contents of page 2
        byte[] inflated1 = extractAndInflate(pdf, 4);
        byte[] inflated2 = extractAndInflate(pdf, 6);

        assertTrue(new String(inflated1, StandardCharsets.US_ASCII).contains("Page 1"),
                "Page 1 inflated stream must contain its text");
        assertTrue(new String(inflated2, StandardCharsets.US_ASCII).contains("Page 2"),
                "Page 2 inflated stream must contain its text");
    }
}
