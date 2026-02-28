package io.offixa.pdfixa.core.document;

import io.offixa.pdfixa.ByteUtil;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.Inflater;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link PdfPage#drawTextBox}.
 *
 * <p>All assertions scan the <em>inflated</em> content stream bytes directly —
 * no PDF library involved.  The content stream for the first (and only) page
 * lives in object 4 (catalog=1, pages=2, page-dict=3, contents=4).
 *
 * <p>Courier is used throughout because every glyph is exactly 600 units wide,
 * making width arithmetic trivially predictable: at fontSize=10 each character
 * occupies 6 pt, so maxWidth=30 fits exactly 5 characters.
 */
class PdfPageDrawTextBoxTest {

    // ── Two-line wrap ─────────────────────────────────────────────────────────

    /**
     * "hello world" must wrap into two lines because:
     * <ul>
     *   <li>"hello" = 5 × 6 pt = 30 pt ≤ maxWidth → first line
     *   <li>"hello world" = 11 × 6 pt = 66 pt > maxWidth → break
     *   <li>"world" → second line
     * </ul>
     * The inflated content stream must contain exactly two {@code ) Tj} tokens
     * with a {@code 0 -12 Td} operator between them.
     */
    @Test
    void drawTextBox_wraps_hello_world_into_two_lines() throws Exception {
        byte[] pdf     = buildPdf("hello world", 30.0, 12.0);
        byte[] content = extractAndInflate(pdf, 4);

        assertSeq(content, "(hello) Tj");
        assertSeq(content, "(world) Tj");
        assertSeq(content, "0 -12 Td");

        assertTjOrderWithTdBetween(content, "(hello) Tj", "0 -12 Td", "(world) Tj");
    }

    @Test
    void drawTextBox_two_lines_contain_exactly_two_tj_operators() throws Exception {
        byte[] pdf     = buildPdf("hello world", 30.0, 12.0);
        byte[] content = extractAndInflate(pdf, 4);
        String s       = new String(content, StandardCharsets.US_ASCII);

        assertEquals(2, countOccurrences(s, ") Tj"),
                "Content stream must contain exactly two ) Tj tokens for a two-line wrap");
    }

    @Test
    void drawTextBox_wraps_into_correct_line_strings() throws Exception {
        byte[] pdf     = buildPdf("hello world", 30.0, 12.0);
        byte[] content = extractAndInflate(pdf, 4);
        String s       = new String(content, StandardCharsets.US_ASCII);

        assertTrue(s.contains("(hello) Tj"), "First line must be '(hello) Tj'");
        assertTrue(s.contains("(world) Tj"), "Second line must be '(world) Tj'");
    }

    // ── Operator frame (BT / Tf / Td / ET) ───────────────────────────────────

    @Test
    void drawTextBox_emits_bt_and_et_frame() throws Exception {
        byte[] content = extractAndInflate(buildPdf("hello world", 30.0, 12.0), 4);
        assertSeq(content, "BT");
        assertSeq(content, "ET");
    }

    @Test
    void drawTextBox_emits_font_and_initial_position() throws Exception {
        byte[] content = extractAndInflate(buildPdf("hello world", 30.0, 12.0), 4);
        String s       = new String(content, StandardCharsets.US_ASCII);

        // Font alias F1 at size 10; Courier is the first (and only) font used
        assertTrue(s.contains("/F1 10 Tf"), "Must set font F1 at size 10");
        // Initial text position
        assertTrue(s.contains("50 700 Td"), "Must move text to (50, 700)");
    }

    // ── lineHeight in Td offset ───────────────────────────────────────────────

    @Test
    void drawTextBox_uses_negative_line_height_in_td_offset() throws Exception {
        byte[] content = extractAndInflate(buildPdf("hello world", 30.0, 14.0), 4);
        assertSeq(content, "0 -14 Td");
    }

    @Test
    void drawTextBox_fractional_line_height_is_formatted_correctly() throws Exception {
        // lineHeight=12.5 → "0 -12.5 Td"
        byte[] content = extractAndInflate(buildPdf("hello world", 30.0, 12.5), 4);
        assertSeq(content, "0 -12.5 Td");
    }

    // ── Single-line: no Td between lines ─────────────────────────────────────

    @Test
    void drawTextBox_single_line_emits_no_intermediate_td() throws Exception {
        // "hello" fits in maxWidth=30 on one line → no inter-line 0 -N Td
        byte[] content = extractAndInflate(buildPdf("hello", 30.0, 12.0), 4);
        String s       = new String(content, StandardCharsets.US_ASCII);

        assertEquals(1, countOccurrences(s, ") Tj"),
                "Single-line text must produce exactly one ) Tj token");
        assertFalse(s.contains("0 -12 Td"),
                "No inter-line Td must be emitted for a single-line text box");
    }

    // ── Forced newline via '\n' ───────────────────────────────────────────────

    @Test
    void drawTextBox_forced_newline_produces_two_lines() throws Exception {
        // "a b\nc d" → ["a b", "c d"] regardless of width
        byte[] content = extractAndInflate(buildPdf("a b\nc d", 200.0, 12.0), 4);
        String s       = new String(content, StandardCharsets.US_ASCII);

        assertTrue(s.contains("(a b) Tj"),  "First line must be 'a b'");
        assertTrue(s.contains("(c d) Tj"),  "Second line must be 'c d'");
        assertEquals(2, countOccurrences(s, ") Tj"),
                "Forced newline must produce exactly two ) Tj tokens");
    }

    // ── Long word fallback ────────────────────────────────────────────────────

    /**
     * maxWidth=18 fits exactly 3 Courier chars at size 10 (3 × 6 = 18 pt).
     * "abcdefghij" (10 chars) must be split into 4 lines: abc / def / ghi / j.
     */
    @Test
    void drawTextBox_long_word_split_into_char_segments() throws Exception {
        byte[] content = extractAndInflate(buildPdf("abcdefghij", 18.0, 12.0), 4);
        String s       = new String(content, StandardCharsets.US_ASCII);

        assertTrue(s.contains("(abc) Tj"), "First segment must be 'abc'");
        assertTrue(s.contains("(def) Tj"), "Second segment must be 'def'");
        assertTrue(s.contains("(ghi) Tj"), "Third segment must be 'ghi'");
        assertTrue(s.contains("(j) Tj"),   "Tail segment must be 'j'");
        assertEquals(4, countOccurrences(s, ") Tj"),
                "A 10-char word with max 3 chars per line must produce 4 Tj operators");
    }

    // ── Argument validation ───────────────────────────────────────────────────

    @Test
    void drawTextBox_rejects_zero_width() {
        PdfDocument doc  = new PdfDocument();
        PdfPage     page = doc.addPage();
        assertThrows(IllegalArgumentException.class,
                () -> page.drawTextBox(0, 700, 0, 12, "Courier", 10, "text"));
    }

    @Test
    void drawTextBox_rejects_zero_line_height() {
        PdfDocument doc  = new PdfDocument();
        PdfPage     page = doc.addPage();
        assertThrows(IllegalArgumentException.class,
                () -> page.drawTextBox(0, 700, 200, 0, "Courier", 10, "text"));
    }

    @Test
    void drawTextBox_rejects_zero_font_size() {
        PdfDocument doc  = new PdfDocument();
        PdfPage     page = doc.addPage();
        assertThrows(IllegalArgumentException.class,
                () -> page.drawTextBox(0, 700, 200, 12, "Courier", 0, "text"));
    }

    @Test
    void drawTextBox_rejects_null_font() {
        PdfDocument doc  = new PdfDocument();
        PdfPage     page = doc.addPage();
        assertThrows(NullPointerException.class,
                () -> page.drawTextBox(0, 700, 200, 12, null, 10, "text"));
    }

    @Test
    void drawTextBox_rejects_null_text() {
        PdfDocument doc  = new PdfDocument();
        PdfPage     page = doc.addPage();
        assertThrows(NullPointerException.class,
                () -> page.drawTextBox(0, 700, 200, 12, "Courier", 10, null));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds a single-page PDF with {@code drawTextBox} at a fixed position
     * using Courier/10 and the given {@code maxWidth} / {@code lineHeight}.
     */
    private static byte[] buildPdf(String text, double maxWidth, double lineHeight)
            throws Exception {
        PdfDocument doc  = new PdfDocument();
        PdfPage     page = doc.addPage();
        page.drawTextBox(50, 700, maxWidth, lineHeight, "Courier", 10, text);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        return baos.toByteArray();
    }

    /**
     * Locates content-stream object {@code objNum}, reads the declared /Length,
     * extracts that many bytes after {@code stream\n}, and inflates them.
     */
    private static byte[] extractAndInflate(byte[] pdf, int objNum) throws Exception {
        byte[] objMarker    = (objNum + " 0 obj\n").getBytes(StandardCharsets.US_ASCII);
        byte[] lengthKey    = "/Length ".getBytes(StandardCharsets.US_ASCII);
        byte[] streamMarker = "stream\n".getBytes(StandardCharsets.US_ASCII);

        int objPos = ByteUtil.indexOf(pdf, objMarker, 0);
        assertNotEquals(-1, objPos, "Object " + objNum + " 0 obj not found in PDF");

        int lengthPos = ByteUtil.indexOf(pdf, lengthKey, objPos);
        assertNotEquals(-1, lengthPos, "/Length key not found in object " + objNum);

        int numStart = lengthPos + lengthKey.length;
        int numEnd   = numStart;
        while (numEnd < pdf.length && pdf[numEnd] >= '0' && pdf[numEnd] <= '9') numEnd++;
        int declaredLength = (int) ByteUtil.parseAsciiLong(pdf, numStart, numEnd);

        int streamPos  = ByteUtil.indexOf(pdf, streamMarker, objPos);
        assertNotEquals(-1, streamPos, "stream marker not found in object " + objNum);

        int    dataStart   = streamPos + streamMarker.length;
        byte[] compressed  = new byte[declaredLength];
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

    /**
     * Asserts that {@code needle} (as US-ASCII) appears at least once in {@code data}.
     */
    private static void assertSeq(byte[] data, String needle) {
        int idx = ByteUtil.indexOf(data, ByteUtil.ascii(needle), 0);
        assertNotEquals(-1, idx,
                "Expected sequence not found in inflated content: \"" + needle + "\"");
    }

    /**
     * Verifies that {@code midToken} appears strictly between the first occurrence
     * of {@code before} and the first occurrence of {@code after} in the stream.
     */
    private static void assertTjOrderWithTdBetween(
            byte[] content, String before, String mid, String after) {
        int posBefore = ByteUtil.indexOf(content, ByteUtil.ascii(before), 0);
        assertNotEquals(-1, posBefore, "'" + before + "' not found");

        int posMid = ByteUtil.indexOf(content, ByteUtil.ascii(mid), posBefore);
        assertNotEquals(-1, posMid,
                "'" + mid + "' not found after '" + before + "'");
        assertTrue(posMid > posBefore,
                "'" + mid + "' must appear after '" + before + "'");

        int posAfter = ByteUtil.indexOf(content, ByteUtil.ascii(after), posMid);
        assertNotEquals(-1, posAfter,
                "'" + after + "' not found after '" + mid + "'");
        assertTrue(posAfter > posMid,
                "'" + after + "' must appear after '" + mid + "'");
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
