package io.offixa.pdfixa.core.document;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies Phase 2 Step 2 — Resource Auto Management for fonts.
 *
 * <p>Rules under test:
 * <ul>
 *   <li>The same Base-14 font name always resolves to the same alias (F1, F2, …).
 *   <li>Each page's {@code /Resources /Font} dictionary contains exactly one entry
 *       per distinct font used on that page — no duplicates.
 *   <li>When "Helvetica" is the first font referenced, its alias is {@code F1}.
 * </ul>
 */
class FontResourceTest {

    // ── FontRegistry unit tests ──────────────────────────────────────────────

    @Test
    void registry_same_name_returns_same_alias() {
        FontRegistry reg = new FontRegistry();
        assertEquals("F1", reg.getAlias("Helvetica"));
        assertEquals("F1", reg.getAlias("Helvetica"), "second call must reuse F1");
    }

    @Test
    void registry_different_names_get_different_aliases_in_order() {
        FontRegistry reg = new FontRegistry();
        assertEquals("F1", reg.getAlias("Helvetica"));
        assertEquals("F2", reg.getAlias("Courier"));
        assertEquals("F3", reg.getAlias("Times-Roman"));
    }

    @Test
    void registry_reverse_lookup_returns_font_name() {
        FontRegistry reg = new FontRegistry();
        reg.getAlias("Helvetica");
        reg.getAlias("Courier");
        assertEquals("Helvetica", reg.getFontName("F1"));
        assertEquals("Courier",   reg.getFontName("F2"));
        assertNull(reg.getFontName("F99"), "unknown alias must return null");
    }

    // ── PdfDocument integration tests ────────────────────────────────────────

    /**
     * Core scenario: 2 pages, both use Helvetica.
     * <ul>
     *   <li>Content stream must reference /F1 on both pages.
     *   <li>Each page dictionary must define /F1 exactly once.
     *   <li>/BaseFont /Helvetica must appear once per page (total 2 occurrences).
     * </ul>
     */
    @Test
    void helvetica_on_two_pages_gets_alias_F1_and_appears_once_per_page() throws Exception {
        PdfDocument doc = new PdfDocument();

        PdfPage page1 = doc.addPage();
        page1.getContent()
             .beginText()
             .setFont("Helvetica", 12)
             .moveText(100, 700)
             .showText("Page one")
             .endText();

        PdfPage page2 = doc.addPage();
        page2.getContent()
             .beginText()
             .setFont("Helvetica", 14)
             .moveText(100, 700)
             .showText("Page two")
             .endText();

        String pdf = generate(doc);

        // Resource dictionaries must declare F1 as the alias for Helvetica on both pages
        assertTrue(extractObjectBody(pdf, "3 0 obj").contains("/F1 <<"),
                "Page 1 resource dict must define alias /F1");
        assertTrue(extractObjectBody(pdf, "5 0 obj").contains("/F1 <<"),
                "Page 2 resource dict must define alias /F1");

        // Total /BaseFont /Helvetica occurrences == 2 (one per page dictionary)
        assertEquals(2, countOccurrences(pdf, "/BaseFont /Helvetica"),
                "Each page must define /BaseFont /Helvetica exactly once");

        // Page 1 is object 3, Page 2 is object 5 (fixed allocation order in PdfDocument)
        assertPageHasExactlyOneFontDef(pdf, "3 0 obj", "Page 1");
        assertPageHasExactlyOneFontDef(pdf, "5 0 obj", "Page 2");
    }

    @Test
    void setFont_called_twice_with_same_name_produces_one_resource_entry() throws Exception {
        PdfDocument doc = new PdfDocument();
        PdfPage page = doc.addPage();
        page.getContent()
            .beginText()
            .setFont("Helvetica", 12)
            .showText("First")
            .setFont("Helvetica", 24)   // same font, larger size
            .showText("Second")
            .endText();

        String pdf = generate(doc);

        // Resource dict must declare F1 as the alias (content stream is compressed)
        assertTrue(extractObjectBody(pdf, "3 0 obj").contains("/F1 <<"),
                "Page resource dict must define alias /F1");

        // Still only ONE font definition in the page dictionary
        String pageBody = extractObjectBody(pdf, "3 0 obj");
        assertEquals(1, countOccurrences(pageBody, "/BaseFont /Helvetica"),
                "Calling setFont twice with the same name must not duplicate the resource entry");
    }

    @Test
    void two_different_fonts_get_sequential_aliases() throws Exception {
        PdfDocument doc = new PdfDocument();
        PdfPage page = doc.addPage();
        page.getContent()
            .beginText()
            .setFont("Helvetica", 12)
            .setFont("Courier", 10)
            .endText();

        String pdf = generate(doc);

        // Resource dict must assign sequential aliases (content stream is compressed)
        String pageBody = extractObjectBody(pdf, "3 0 obj");
        assertTrue(pageBody.contains("/F1 <<"), "Helvetica must get alias F1 in resource dict");
        assertTrue(pageBody.contains("/F2 <<"), "Courier must get alias F2 in resource dict");
        assertTrue(pageBody.contains("/BaseFont /Helvetica"));
        assertTrue(pageBody.contains("/BaseFont /Courier"));
    }

    @Test
    void page_with_no_fonts_has_no_resources_dict() throws Exception {
        PdfDocument doc = new PdfDocument();
        PdfPage page = doc.addPage();
        page.getContent()
            .moveTo(10, 10)
            .lineTo(100, 100)
            .stroke();

        String pdf = generate(doc);

        String pageBody = extractObjectBody(pdf, "3 0 obj");
        assertFalse(pageBody.contains("/Resources"),
                "A page that uses no fonts must not emit a /Resources dictionary");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String generate(PdfDocument doc) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        return baos.toString(StandardCharsets.US_ASCII);
    }

    /** Extracts the raw text between the object header and its {@code endobj}. */
    private static String extractObjectBody(String pdf, String objHeader) {
        int start = pdf.indexOf(objHeader);
        assertNotEquals(-1, start, objHeader + " not found in PDF");
        int bodyStart = start + objHeader.length();
        int end = pdf.indexOf("endobj", bodyStart);
        assertNotEquals(-1, end, "endobj not found after " + objHeader);
        return pdf.substring(bodyStart, end);
    }

    private static void assertPageHasExactlyOneFontDef(String pdf, String objHeader,
                                                        String pageName) {
        String body = extractObjectBody(pdf, objHeader);
        assertEquals(1, countOccurrences(body, "/BaseFont /Helvetica"),
                pageName + " must define /BaseFont /Helvetica exactly once");
    }

    private static int countOccurrences(String text, String pattern) {
        int count = 0;
        int idx   = 0;
        while ((idx = text.indexOf(pattern, idx)) != -1) {
            count++;
            idx += pattern.length();
        }
        return count;
    }
}
