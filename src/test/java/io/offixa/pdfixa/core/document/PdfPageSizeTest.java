package io.offixa.pdfixa.core.document;

import io.offixa.pdfixa.ByteUtil;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PdfPageSize} value object and its wiring into {@link PdfDocument}.
 */
class PdfPageSizeTest {

    // ── Value object ────────────────────────────────────────────────────────

    @Test
    void a4_has_correct_dimensions() {
        assertEquals(595, PdfPageSize.A4.getWidthPt());
        assertEquals(842, PdfPageSize.A4.getHeightPt());
    }

    @Test
    void letter_has_correct_dimensions() {
        assertEquals(612, PdfPageSize.LETTER.getWidthPt());
        assertEquals(792, PdfPageSize.LETTER.getHeightPt());
    }

    @Test
    void custom_size_stores_dimensions() {
        PdfPageSize size = new PdfPageSize(300, 400);
        assertEquals(300, size.getWidthPt());
        assertEquals(400, size.getHeightPt());
    }

    @Test
    void zero_width_throws() {
        assertThrows(IllegalArgumentException.class, () -> new PdfPageSize(0, 100));
    }

    @Test
    void negative_height_throws() {
        assertThrows(IllegalArgumentException.class, () -> new PdfPageSize(100, -1));
    }

    @Test
    void equals_and_hashCode() {
        PdfPageSize a = new PdfPageSize(595, 842);
        PdfPageSize b = new PdfPageSize(595, 842);
        PdfPageSize c = new PdfPageSize(612, 792);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    void toString_contains_dimensions() {
        String s = PdfPageSize.A4.toString();
        assertTrue(s.contains("595"));
        assertTrue(s.contains("842"));
    }

    // ── MediaBox wiring ─────────────────────────────────────────────────────

    @Test
    void default_constructor_produces_a4_mediaBox() throws IOException {
        byte[] pdf = buildOnePageDoc(new PdfDocument());
        assertMediaBox(pdf, 595, 842);
    }

    @Test
    void letter_constructor_produces_letter_mediaBox() throws IOException {
        byte[] pdf = buildOnePageDoc(new PdfDocument(PdfPageSize.LETTER));
        assertMediaBox(pdf, 612, 792);
    }

    @Test
    void custom_size_produces_correct_mediaBox() throws IOException {
        byte[] pdf = buildOnePageDoc(new PdfDocument(new PdfPageSize(300, 400)));
        assertMediaBox(pdf, 300, 400);
    }

    @Test
    void determinism_with_custom_size() throws IOException {
        byte[] run1 = buildOnePageDoc(new PdfDocument(PdfPageSize.A3));
        byte[] run2 = buildOnePageDoc(new PdfDocument(PdfPageSize.A3));
        assertArrayEquals(run1, run2);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static byte[] buildOnePageDoc(PdfDocument doc) throws IOException {
        PdfPage page = doc.addPage();
        page.getContent()
                .beginText()
                .setFont("Helvetica", 12)
                .moveText(50, 700)
                .showText("test")
                .endText();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        return baos.toByteArray();
    }

    private static void assertMediaBox(byte[] pdf, int expectedW, int expectedH) {
        String mediaBox = "/MediaBox [0 0 " + expectedW + " " + expectedH + "]";
        int idx = ByteUtil.indexOf(pdf, ByteUtil.ascii(mediaBox), 0);
        assertNotEquals(-1, idx,
                "Expected MediaBox \"" + mediaBox + "\" not found in PDF output");
    }
}
