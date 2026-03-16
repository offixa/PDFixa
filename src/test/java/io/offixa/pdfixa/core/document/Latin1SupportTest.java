package io.offixa.pdfixa.core.document;

import io.offixa.pdfixa.ByteUtil;
import io.offixa.pdfixa.core.text.Base14FontMetrics;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.zip.Inflater;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies correct Latin-1 (WinAnsiEncoding) support for Base-14 fonts.
 *
 * <p>Characters under test:
 * <ul>
 *   <li>{@code é} U+00E9 — e-acute (common in French/Spanish/Portuguese)
 *   <li>{@code ñ} U+00F1 — n-tilde (Spanish)
 *   <li>{@code ç} U+00E7 — c-cedilla (French/Portuguese/Turkish)
 *   <li>{@code ü} U+00FC — u-dieresis (German/Turkish)
 *   <li>{@code ö} U+00F6 — o-dieresis (German/Turkish/Swedish)
 * </ul>
 *
 * <p>Test coverage:
 * <ol>
 *   <li>drawTextBox renders Latin-1 characters without exception.</li>
 *   <li>PDF structure is valid (header, xref, WinAnsiEncoding declared).</li>
 *   <li>Text width calculations are correct and deterministic.</li>
 *   <li>Byte output is deterministic across multiple builds (SHA-256 golden).</li>
 * </ol>
 */
class Latin1SupportTest {

    // ── Characters under test ───────────────────────────────────────────────

    private static final char E_ACUTE  = '\u00E9'; // é
    private static final char N_TILDE  = '\u00F1'; // ñ
    private static final char C_CEDIL  = '\u00E7'; // ç
    private static final char U_UMLAUT = '\u00FC'; // ü
    private static final char O_UMLAUT = '\u00F6'; // ö

    /** All five characters as a single string for combined use. */
    private static final String ALL_FIVE = new String(
            new char[]{E_ACUTE, N_TILDE, C_CEDIL, U_UMLAUT, O_UMLAUT});

    private static final Base14FontMetrics METRICS = Base14FontMetrics.getInstance();

    // ── Golden SHA-256 for the canonical Latin-1 document ──────────────────

    private static final String EXPECTED_SHA256 =
            "d50c025d7a743676c3a8e15d05a7b07082914bbf5824c1bf4c3fe0cb8f4a1979";

    // ── 1. drawTextBox renders without exception ────────────────────────────

    @Test
    void drawTextBox_e_acute_renders_without_exception() {
        assertDoesNotThrow(() -> buildPdf(String.valueOf(E_ACUTE)));
    }

    @Test
    void drawTextBox_n_tilde_renders_without_exception() {
        assertDoesNotThrow(() -> buildPdf(String.valueOf(N_TILDE)));
    }

    @Test
    void drawTextBox_c_cedilla_renders_without_exception() {
        assertDoesNotThrow(() -> buildPdf(String.valueOf(C_CEDIL)));
    }

    @Test
    void drawTextBox_u_umlaut_renders_without_exception() {
        assertDoesNotThrow(() -> buildPdf(String.valueOf(U_UMLAUT)));
    }

    @Test
    void drawTextBox_o_umlaut_renders_without_exception() {
        assertDoesNotThrow(() -> buildPdf(String.valueOf(O_UMLAUT)));
    }

    @Test
    void drawTextBox_all_latin1_chars_combined_renders_without_exception() {
        assertDoesNotThrow(() -> buildPdf(ALL_FIVE));
    }

    // ── 2. PDF structure is valid ───────────────────────────────────────────

    @Test
    void pdf_starts_with_pdf_header() throws Exception {
        byte[] pdf = buildPdf(ALL_FIVE);
        assertTrue(pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D'
                        && pdf[3] == 'F' && pdf[4] == '-',
                "PDF must begin with %PDF-");
    }

    @Test
    void pdf_ends_with_eof_marker() throws Exception {
        byte[] pdf = buildPdf(ALL_FIVE);
        String tail = new String(pdf, pdf.length - 10, 10, StandardCharsets.US_ASCII);
        assertTrue(tail.contains("%%EOF"),
                "PDF must end with %%EOF marker");
    }

    @Test
    void pdf_declares_win_ansi_encoding_in_font_dict() throws Exception {
        byte[] pdf = buildPdf(ALL_FIVE);
        assertTrue(contains(pdf, "/Encoding /WinAnsiEncoding"),
                "Font resource dict must declare /Encoding /WinAnsiEncoding");
    }

    @Test
    void pdf_declares_type1_subtype() throws Exception {
        byte[] pdf = buildPdf(ALL_FIVE);
        assertTrue(contains(pdf, "/Subtype /Type1"),
                "Font resource dict must declare /Subtype /Type1");
    }

    @Test
    void content_stream_contains_encoded_e_acute_byte() throws Exception {
        byte[] content = buildAndInflate(String.valueOf(E_ACUTE));
        assertContainsByte(content, (byte) 0xE9, "é (0xE9)");
    }

    @Test
    void content_stream_contains_encoded_n_tilde_byte() throws Exception {
        byte[] content = buildAndInflate(String.valueOf(N_TILDE));
        assertContainsByte(content, (byte) 0xF1, "ñ (0xF1)");
    }

    @Test
    void content_stream_contains_encoded_c_cedilla_byte() throws Exception {
        byte[] content = buildAndInflate(String.valueOf(C_CEDIL));
        assertContainsByte(content, (byte) 0xE7, "ç (0xE7)");
    }

    @Test
    void content_stream_contains_encoded_u_umlaut_byte() throws Exception {
        byte[] content = buildAndInflate(String.valueOf(U_UMLAUT));
        assertContainsByte(content, (byte) 0xFC, "ü (0xFC)");
    }

    @Test
    void content_stream_contains_encoded_o_umlaut_byte() throws Exception {
        byte[] content = buildAndInflate(String.valueOf(O_UMLAUT));
        assertContainsByte(content, (byte) 0xF6, "ö (0xF6)");
    }

    // ── 3. Text width calculations are correct and deterministic ───────────

    @Test
    void e_acute_width_matches_helvetica_afm() {
        // Helvetica é (0xE9) = 556 units; at size 10 → 5.56 pt
        assertEquals(5.56, METRICS.textWidthPt(String.valueOf(E_ACUTE), "Helvetica", 10), 1e-9);
    }

    @Test
    void n_tilde_width_matches_helvetica_afm() {
        // Helvetica ñ (0xF1) = 556 units; at size 10 → 5.56 pt
        assertEquals(5.56, METRICS.textWidthPt(String.valueOf(N_TILDE), "Helvetica", 10), 1e-9);
    }

    @Test
    void c_cedilla_width_matches_helvetica_afm() {
        // Helvetica ç (0xE7) = 500 units; at size 10 → 5.0 pt
        assertEquals(5.0, METRICS.textWidthPt(String.valueOf(C_CEDIL), "Helvetica", 10), 1e-9);
    }

    @Test
    void u_umlaut_width_matches_helvetica_afm() {
        // Helvetica ü (0xFC) = 556 units; at size 10 → 5.56 pt
        assertEquals(5.56, METRICS.textWidthPt(String.valueOf(U_UMLAUT), "Helvetica", 10), 1e-9);
    }

    @Test
    void o_umlaut_width_matches_helvetica_afm() {
        // Helvetica ö (0xF6) = 556 units; at size 10 → 5.56 pt
        assertEquals(5.56, METRICS.textWidthPt(String.valueOf(O_UMLAUT), "Helvetica", 10), 1e-9);
    }

    @Test
    void width_calculation_is_deterministic_for_all_five_chars() {
        double w1 = METRICS.textWidthPt(ALL_FIVE, "Helvetica", 12);
        double w2 = METRICS.textWidthPt(ALL_FIVE, "Helvetica", 12);
        assertEquals(w1, w2, "Width must be identical across repeated calls");
    }

    @Test
    void times_roman_e_acute_width_matches_afm() {
        // Times-Roman é (0xE9) = 444 units; at size 10 → 4.44 pt
        assertEquals(4.44, METRICS.textWidthPt(String.valueOf(E_ACUTE), "Times-Roman", 10), 1e-9);
    }

    @Test
    void courier_all_latin1_chars_remain_monospaced() {
        for (char ch : ALL_FIVE.toCharArray()) {
            assertEquals(6.0,
                    METRICS.textWidthPt(String.valueOf(ch), "Courier", 10),
                    1e-9,
                    "Courier must return 6.0 pt for every glyph at size 10: " + ch);
        }
    }

    // ── 4. Byte output determinism + SHA-256 golden ─────────────────────────

    @Test
    void latin1_pdf_output_is_byte_identical_across_two_builds() throws Exception {
        byte[] run1 = buildCanonicalLatinPdf();
        byte[] run2 = buildCanonicalLatinPdf();
        assertArrayEquals(run1, run2,
                "Two canonical Latin-1 PDF builds must produce byte-identical output");
    }

    @Test
    void latin1_pdf_output_is_byte_identical_across_three_builds() throws Exception {
        byte[] run1 = buildCanonicalLatinPdf();
        byte[] run2 = buildCanonicalLatinPdf();
        byte[] run3 = buildCanonicalLatinPdf();
        assertArrayEquals(run1, run2, "Run 1 vs Run 2");
        assertArrayEquals(run2, run3, "Run 2 vs Run 3");
    }

    @Test
    void latin1_canonical_pdf_matches_golden_sha256() throws Exception {
        byte[] pdf  = buildCanonicalLatinPdf();
        String hash = sha256Hex(pdf);
        assertEquals(EXPECTED_SHA256, hash,
                "Latin-1 canonical PDF SHA-256 mismatch — "
                        + "if the document builder changed intentionally, "
                        + "update EXPECTED_SHA256 to: " + hash);
    }

    // ── Canonical builder ──────────────────────────────────────────────────

    /**
     * Builds the canonical Latin-1 test document.
     * Every input is a compile-time constant — no runtime entropy.
     */
    private static byte[] buildCanonicalLatinPdf() throws Exception {
        PdfDocument doc = new PdfDocument();

        PdfPage page = doc.addPage();
        page.getContent()
                .beginText()
                .setFont("Helvetica", 14)
                .moveText(72, 700)
                // é ñ ç ü ö — one per word so width wrapping is predictable
                .showText("caf\u00E9 r\u00E9sum\u00E9")
                .endText()

                .beginText()
                .setFont("Helvetica", 12)
                .moveText(72, 680)
                .showText("ni\u00F1o se\u00F1or")
                .endText()

                .beginText()
                .setFont("Helvetica", 12)
                .moveText(72, 660)
                .showText("fran\u00E7ais gar\u00E7on")
                .endText()

                .beginText()
                .setFont("Times-Roman", 12)
                .moveText(72, 640)
                .showText("\u00FCber n\u00FCance")
                .endText()

                .beginText()
                .setFont("Times-Roman", 12)
                .moveText(72, 620)
                .showText("k\u00F6nnen m\u00F6glich")
                .endText();

        // drawTextBox path with Latin-1 text
        page.drawTextBox(72, 580, 400, 16, "Helvetica", 11,
                "\u00E9\u00F1\u00E7\u00FC\u00F6 Latin-1 WinAnsiEncoding");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        return baos.toByteArray();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static byte[] buildPdf(String text) throws Exception {
        PdfDocument doc  = new PdfDocument();
        PdfPage     page = doc.addPage();
        page.drawTextBox(72, 700, 400, 14, "Helvetica", 12, text);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        return baos.toByteArray();
    }

    private static byte[] buildAndInflate(String text) throws Exception {
        byte[] pdf = buildPdf(text);
        return extractAndInflate(pdf, 4);
    }

    /**
     * Locates content-stream object {@code objNum} in the raw PDF, reads its
     * declared /Length, and inflates the compressed stream data.
     */
    private static byte[] extractAndInflate(byte[] pdf, int objNum) throws Exception {
        byte[] objMarker    = (objNum + " 0 obj\n").getBytes(StandardCharsets.US_ASCII);
        byte[] lengthKey    = "/Length ".getBytes(StandardCharsets.US_ASCII);
        byte[] streamMarker = "stream\n".getBytes(StandardCharsets.US_ASCII);

        int objPos    = ByteUtil.indexOf(pdf, objMarker, 0);
        int lengthPos = ByteUtil.indexOf(pdf, lengthKey, objPos);
        int numStart  = lengthPos + lengthKey.length;
        int numEnd    = numStart;
        while (numEnd < pdf.length && pdf[numEnd] >= '0' && pdf[numEnd] <= '9') numEnd++;
        int declaredLength = (int) ByteUtil.parseAsciiLong(pdf, numStart, numEnd);

        int streamPos = ByteUtil.indexOf(pdf, streamMarker, objPos);
        int dataStart = streamPos + streamMarker.length;
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

    private static boolean contains(byte[] data, String needle) {
        // Search using ISO-8859-1 for Latin-1 aware matching in the raw PDF
        byte[] seq = needle.getBytes(StandardCharsets.ISO_8859_1);
        return ByteUtil.indexOf(data, seq, 0) != -1;
    }

    private static void assertContainsByte(byte[] data, byte target, String label) {
        for (byte b : data) {
            if (b == target) return;
        }
        fail("Expected byte " + label + " (0x"
                + Integer.toHexString(target & 0xFF).toUpperCase()
                + ") not found in content stream");
    }

    private static String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }
}
