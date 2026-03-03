package io.offixa.pdfixa.core.document;

import io.offixa.pdfixa.ByteUtil;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.Inflater;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies Phase 2 Step 4A — JPEG image XObject support.
 *
 * <p>Tests confirm that:
 * <ul>
 *   <li>The image XObject stream carries {@code /Subtype /Image} and
 *       {@code /Filter /DCTDecode}.
 *   <li>The page resource dictionary contains {@code /XObject} with an
 *       {@code /Im1} → indirect-reference entry.
 *   <li>The page content stream (after inflate) contains {@code cm} and
 *       {@code /Im1 Do}.
 *   <li>Output is deterministic across multiple {@code save()} calls.
 *   <li>An image-only page (no fonts) emits {@code /Resources} with
 *       {@code /XObject} but without {@code /Font}.
 *   <li>A page with both a font and an image emits both sub-dictionaries.
 *   <li>{@link PdfImage} accessors return the values given at registration.
 *   <li>JPEG bytes are stored verbatim (no Flate wrapping).
 *   <li>{@code addJpegImage} rejects null bytes and non-positive dimensions.
 * </ul>
 *
 * <p>No PDF library is used — all assertions are done via raw byte scanning.
 */
class ImageXObjectTest {

    /**
     * Minimal "JPEG": SOI marker followed immediately by EOI.
     * Not a renderable image, but structurally correct for embedding tests.
     */
    private static final byte[] TINY_JPEG = {
        (byte) 0xFF, (byte) 0xD8, // SOI
        (byte) 0xFF, (byte) 0xD9  // EOI
    };

    private static final int IMAGE_W = 10;
    private static final int IMAGE_H = 8;

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Builds a single-page PDF with one JPEG image placed at (50, 100) with
     * dimensions 200×160.  The image XObject is allocated before the page,
     * making the object layout:
     * <pre>
     *   1  Catalog
     *   2  Pages
     *   3  Image XObject (Im1)
     *   4  Page
     *   5  Contents (FlateDecode)
     * </pre>
     */
    private static byte[] buildImagePdf() throws Exception {
        PdfDocument doc = new PdfDocument();
        PdfImage img = doc.addJpegImage(TINY_JPEG, IMAGE_W, IMAGE_H);
        PdfPage page = doc.addPage();
        page.drawImage(img, 50, 100, 200, 160);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        return baos.toByteArray();
    }

    /**
     * Locates object {@code objNum}, reads its {@code /Length} bytes from after
     * {@code stream\n}, and inflates them (FlateDecode).
     */
    private static byte[] extractAndInflate(byte[] pdf, int objNum) throws Exception {
        byte[] objMarker    = (objNum + " 0 obj\n").getBytes(StandardCharsets.US_ASCII);
        byte[] lengthKey    = "/Length ".getBytes(StandardCharsets.US_ASCII);
        byte[] streamMarker = "stream\n".getBytes(StandardCharsets.US_ASCII);

        int objPos = ByteUtil.indexOf(pdf, objMarker, 0);
        assertNotEquals(-1, objPos, "Object " + objNum + " not found");

        int lengthPos = ByteUtil.indexOf(pdf, lengthKey, objPos);
        assertNotEquals(-1, lengthPos, "/Length not found in object " + objNum);

        int numStart = lengthPos + lengthKey.length;
        int numEnd   = numStart;
        while (numEnd < pdf.length && pdf[numEnd] >= '0' && pdf[numEnd] <= '9') numEnd++;
        int declaredLength = (int) ByteUtil.parseAsciiLong(pdf, numStart, numEnd);

        int streamPos = ByteUtil.indexOf(pdf, streamMarker, objPos);
        assertNotEquals(-1, streamPos, "stream marker not found in object " + objNum);

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

    // ── PdfImage unit tests ───────────────────────────────────────────────────

    @Test
    void pdfImage_accessors_return_registered_values() {
        PdfImage img = new PdfImage("Im3", 7, 320, 240);
        assertEquals("Im3", img.getAlias());
        assertEquals(7,     img.getObjectNumber());
        assertEquals(320,   img.getWidth());
        assertEquals(240,   img.getHeight());
    }

    @Test
    void pdfImage_width_height_are_public() throws Exception {
        var getWidth  = PdfImage.class.getDeclaredMethod("getWidth");
        var getHeight = PdfImage.class.getDeclaredMethod("getHeight");
        assertTrue(java.lang.reflect.Modifier.isPublic(getWidth.getModifiers()),
                "getWidth() must be public");
        assertTrue(java.lang.reflect.Modifier.isPublic(getHeight.getModifiers()),
                "getHeight() must be public");
    }

    @Test
    void pdfImage_alias_and_objectNumber_are_not_public() throws Exception {
        var getAlias = PdfImage.class.getDeclaredMethod("getAlias");
        var getObjNo = PdfImage.class.getDeclaredMethod("getObjectNumber");
        assertFalse(java.lang.reflect.Modifier.isPublic(getAlias.getModifiers()),
                "getAlias() must not be public");
        assertFalse(java.lang.reflect.Modifier.isPublic(getObjNo.getModifiers()),
                "getObjectNumber() must not be public");
    }

    // ── ImageRegistry unit tests ──────────────────────────────────────────────

    @Test
    void imageRegistry_assigns_sequential_aliases() {
        ImageRegistry reg = new ImageRegistry();
        PdfImage a = reg.allocate(3, 100, 50);
        PdfImage b = reg.allocate(5, 200, 150);
        assertEquals("Im1", a.getAlias());
        assertEquals("Im2", b.getAlias());
    }

    @Test
    void imageRegistry_each_call_gets_new_alias() {
        ImageRegistry reg = new ImageRegistry();
        PdfImage x = reg.allocate(3, 1, 1);
        PdfImage y = reg.allocate(4, 1, 1);
        assertNotEquals(x.getAlias(), y.getAlias());
    }

    // ── ContentStream operator tests ──────────────────────────────────────────

    @Test
    void saveState_produces_q_newline() {
        byte[] bytes = new io.offixa.pdfixa.core.content.ContentStream()
                .saveState().toBytes();
        assertArrayEquals(ByteUtil.ascii("q\n"), bytes);
    }

    @Test
    void restoreState_produces_Q_newline() {
        byte[] bytes = new io.offixa.pdfixa.core.content.ContentStream()
                .restoreState().toBytes();
        assertArrayEquals(ByteUtil.ascii("Q\n"), bytes);
    }

    @Test
    void concatMatrix_produces_correct_sequence() {
        byte[] bytes = new io.offixa.pdfixa.core.content.ContentStream()
                .concatMatrix(200, 0, 0, 160, 50, 100).toBytes();
        assertArrayEquals(ByteUtil.ascii("200 0 0 160 50 100 cm\n"), bytes);
    }

    @Test
    void doXObject_produces_name_Do_newline() {
        byte[] bytes = new io.offixa.pdfixa.core.content.ContentStream()
                .doXObject("Im1").toBytes();
        assertArrayEquals(ByteUtil.ascii("/Im1 Do\n"), bytes);
    }

    @Test
    void image_drawing_sequence_is_q_cm_Do_Q() {
        byte[] bytes = new io.offixa.pdfixa.core.content.ContentStream()
                .saveState()
                .concatMatrix(200, 0, 0, 160, 50, 100)
                .doXObject("Im1")
                .restoreState()
                .toBytes();
        assertArrayEquals(
                ByteUtil.ascii("q\n200 0 0 160 50 100 cm\n/Im1 Do\nQ\n"),
                bytes);
    }

    // ── PDF structural tests ──────────────────────────────────────────────────

    @Test
    void pdf_contains_subtype_image_and_dct_decode() throws Exception {
        byte[] pdf     = buildImagePdf();
        String pdfText = new String(pdf, StandardCharsets.US_ASCII);
        assertTrue(pdfText.contains("/Subtype /Image"),
                "/Subtype /Image must appear in the image XObject dictionary");
        assertTrue(pdfText.contains("/Filter /DCTDecode"),
                "/Filter /DCTDecode must appear in the image XObject dictionary");
    }

    @Test
    void pdf_contains_xobject_resource_with_im1_reference() throws Exception {
        byte[] pdf     = buildImagePdf();
        String pdfText = new String(pdf, StandardCharsets.US_ASCII);
        assertTrue(pdfText.contains("/XObject"),
                "/XObject must appear in the page resource dictionary");
        assertTrue(pdfText.contains("/Im1"),
                "/Im1 alias must appear in the XObject sub-dictionary");
        // Im1 must point to the image XObject object (obj 3 in our layout)
        assertTrue(pdfText.contains("/Im1 3 0 R"),
                "/Im1 must reference the image XObject as '3 0 R'");
    }

    @Test
    void content_stream_contains_cm_and_im1_do() throws Exception {
        byte[] pdf      = buildImagePdf();
        // Content stream is object 5 (image XObject is 3, page is 4)
        byte[] inflated = extractAndInflate(pdf, 5);
        String content  = new String(inflated, StandardCharsets.US_ASCII);
        assertTrue(content.contains("cm"),
                "Inflated content stream must contain the 'cm' operator");
        assertTrue(content.contains("/Im1 Do"),
                "Inflated content stream must contain '/Im1 Do'");
    }

    @Test
    void content_stream_has_save_restore_state_around_image() throws Exception {
        byte[] pdf      = buildImagePdf();
        byte[] inflated = extractAndInflate(pdf, 5);
        String content  = new String(inflated, StandardCharsets.US_ASCII);
        // The canonical sequence must appear in order
        int qPos    = content.indexOf("q\n");
        int cmPos   = content.indexOf("cm\n");
        int doPos   = content.indexOf("/Im1 Do\n");
        int bigQPos = content.indexOf("Q\n");
        assertTrue(qPos  != -1, "Content stream must contain 'q'");
        assertTrue(cmPos != -1, "Content stream must contain 'cm'");
        assertTrue(doPos != -1, "Content stream must contain '/Im1 Do'");
        assertTrue(bigQPos != -1, "Content stream must contain 'Q'");
        assertTrue(qPos < cmPos && cmPos < doPos && doPos < bigQPos,
                "Sequence must be: q … cm … /Im1 Do … Q");
    }

    @Test
    void image_xobject_length_matches_jpeg_byte_count() throws Exception {
        byte[] pdf = buildImagePdf();
        // Image XObject is object 3.  Its /Length must equal TINY_JPEG.length.
        byte[] objMarker = "3 0 obj\n".getBytes(StandardCharsets.US_ASCII);
        byte[] lengthKey = "/Length ".getBytes(StandardCharsets.US_ASCII);
        int objPos = ByteUtil.indexOf(pdf, objMarker, 0);
        assertNotEquals(-1, objPos, "Image XObject object 3 not found");
        int lPos = ByteUtil.indexOf(pdf, lengthKey, objPos);
        int ns   = lPos + lengthKey.length;
        int ne   = ns;
        while (ne < pdf.length && pdf[ne] >= '0' && pdf[ne] <= '9') ne++;
        int declaredLength = (int) ByteUtil.parseAsciiLong(pdf, ns, ne);
        assertEquals(TINY_JPEG.length, declaredLength,
                "/Length in image XObject must equal the raw JPEG byte count");
    }

    @Test
    void jpeg_bytes_are_stored_verbatim_not_flate_compressed() throws Exception {
        byte[] pdf = buildImagePdf();
        // The TINY_JPEG bytes must appear literally in the file
        int idx = ByteUtil.indexOf(pdf, TINY_JPEG, 0);
        assertNotEquals(-1, idx, "Raw JPEG bytes must be stored verbatim in the PDF");
    }

    @Test
    void image_only_page_has_xobject_but_no_font_resource() throws Exception {
        PdfDocument doc = new PdfDocument();
        PdfImage img = doc.addJpegImage(TINY_JPEG, IMAGE_W, IMAGE_H);
        PdfPage page = doc.addPage();
        page.drawImage(img, 0, 0, 100, 80);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        String pdf = baos.toString(StandardCharsets.US_ASCII);

        // Page is obj 4 when image is added first
        String pageBody = extractObjectBody(pdf, "4 0 obj");
        assertTrue(pageBody.contains("/XObject"),
                "Page with image must declare /XObject in /Resources");
        assertFalse(pageBody.contains("/Font"),
                "Page with image but no text must not declare /Font");
    }

    @Test
    void page_with_font_and_image_has_both_resources() throws Exception {
        PdfDocument doc = new PdfDocument();
        PdfImage img = doc.addJpegImage(TINY_JPEG, IMAGE_W, IMAGE_H);
        PdfPage page = doc.addPage();
        page.getContent()
            .beginText()
            .setFont("Helvetica", 12)
            .moveText(100, 700)
            .showText("caption")
            .endText();
        page.drawImage(img, 50, 100, 200, 160);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        String pdf = baos.toString(StandardCharsets.US_ASCII);

        // Page is obj 4
        String pageBody = extractObjectBody(pdf, "4 0 obj");
        assertTrue(pageBody.contains("/Font"),
                "Page with text must declare /Font in /Resources");
        assertTrue(pageBody.contains("/XObject"),
                "Page with image must declare /XObject in /Resources");
        assertTrue(pageBody.contains("/Im1"),
                "XObject dict must contain /Im1 alias");
    }

    @Test
    void two_images_get_sequential_aliases_im1_and_im2() throws Exception {
        PdfDocument doc = new PdfDocument();
        PdfImage img1 = doc.addJpegImage(TINY_JPEG, 10, 8);
        PdfImage img2 = doc.addJpegImage(TINY_JPEG, 20, 16);
        assertEquals("Im1", img1.getAlias());
        assertEquals("Im2", img2.getAlias());

        PdfPage page = doc.addPage();
        page.drawImage(img1, 0,  0, 100, 80);
        page.drawImage(img2, 0, 90, 200, 160);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        String pdf = baos.toString(StandardCharsets.US_ASCII);

        assertTrue(pdf.contains("/Im1"), "/Im1 must appear in PDF");
        assertTrue(pdf.contains("/Im2"), "/Im2 must appear in PDF");
    }

    @Test
    void output_is_deterministic_with_image() throws Exception {
        byte[] first  = buildImagePdf();
        byte[] second = buildImagePdf();
        assertArrayEquals(first, second,
                "Two save() calls with identical content must produce byte-identical PDFs");
    }

    @Test
    void addJpegImage_rejects_null_bytes() {
        PdfDocument doc = new PdfDocument();
        assertThrows(NullPointerException.class,
                () -> doc.addJpegImage(null, 10, 10));
    }

    @Test
    void addJpegImage_rejects_zero_width() {
        PdfDocument doc = new PdfDocument();
        assertThrows(IllegalArgumentException.class,
                () -> doc.addJpegImage(TINY_JPEG, 0, 10));
    }

    @Test
    void addJpegImage_rejects_negative_height() {
        PdfDocument doc = new PdfDocument();
        assertThrows(IllegalArgumentException.class,
                () -> doc.addJpegImage(TINY_JPEG, 10, -1));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String extractObjectBody(String pdf, String objHeader) {
        int start = pdf.indexOf(objHeader);
        assertNotEquals(-1, start, objHeader + " not found in PDF");
        int bodyStart = start + objHeader.length();
        int end = pdf.indexOf("endobj", bodyStart);
        assertNotEquals(-1, end, "endobj not found after " + objHeader);
        return pdf.substring(bodyStart, end);
    }
}
