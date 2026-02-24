package io.offixa.pdfixa.core.document;

import io.offixa.pdfixa.ByteUtil;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies Phase 2 Step 4B — PNG Image XObject support.
 *
 * <p>Tests confirm that:
 * <ul>
 *   <li>Width and height are extracted from the PNG IHDR chunk.
 *   <li>Alpha (color type 6) and indexed (color type 3) PNGs are rejected.
 *   <li>The image XObject carries {@code /Filter /FlateDecode} and
 *       {@code /DecodeParms << /Predictor 15 /Colors 3 ... >>}.
 *   <li>The raw IDAT payload is written verbatim without re-compression.
 *   <li>Output is deterministic across multiple {@code save()} calls.
 *   <li>{@code addPngImage} rejects {@code null} input and non-PNG bytes.
 * </ul>
 *
 * <p>All PNGs are built programmatically — no files are read from disk.
 * No PDF library is used — all assertions are done via raw byte scanning.
 */
class PngImageXObjectTest {

    /**
     * A valid 2×2 RGB PNG (red pixels).  Built once for all tests that need
     * a structurally correct input.
     */
    private static final byte[] TINY_RGB_PNG = buildRgbPng(2, 2);

    // ── Width / Height extraction ─────────────────────────────────────────────

    @Test
    void valid_rgb_png_produces_image_handle_with_correct_dimensions() {
        PdfDocument doc = new PdfDocument();
        PdfImage img = doc.addPngImage(TINY_RGB_PNG);
        assertNotNull(img);
        assertEquals(2, img.getWidth(),  "width must match IHDR");
        assertEquals(2, img.getHeight(), "height must match IHDR");
    }

    @Test
    void width_and_height_extracted_from_larger_png() {
        byte[] png = buildRgbPng(7, 5);
        PdfDocument doc = new PdfDocument();
        PdfImage img = doc.addPngImage(png);
        assertEquals(7, img.getWidth(),  "width 7 must be read from IHDR");
        assertEquals(5, img.getHeight(), "height 5 must be read from IHDR");
    }

    // ── Unsupported PNG rejection ─────────────────────────────────────────────

    @Test
    void alpha_png_color_type_6_throws_illegal_argument() {
        byte[] alphaPng = buildPngWithColorType(2, 2, 6);
        PdfDocument doc = new PdfDocument();
        assertThrows(IllegalArgumentException.class,
                () -> doc.addPngImage(alphaPng),
                "RGBA (color type 6) PNG must be rejected");
    }

    @Test
    void indexed_png_color_type_3_throws_illegal_argument() {
        byte[] indexedPng = buildPngWithColorType(2, 2, 3);
        PdfDocument doc = new PdfDocument();
        assertThrows(IllegalArgumentException.class,
                () -> doc.addPngImage(indexedPng),
                "Indexed (color type 3) PNG must be rejected");
    }

    // ── /DecodeParms in image XObject ─────────────────────────────────────────

    @Test
    void pdf_image_xobject_contains_flatedecode_and_decode_parms() throws Exception {
        byte[] pdf = buildPngPdf(TINY_RGB_PNG);
        String text = new String(pdf, StandardCharsets.US_ASCII);

        assertTrue(text.contains("/Filter /FlateDecode"),
                "/Filter /FlateDecode must appear in the image XObject dictionary");
        assertTrue(text.contains("/DecodeParms"),
                "/DecodeParms must appear in the image XObject dictionary");
        assertTrue(text.contains("/Predictor 15"),
                "/Predictor 15 must be declared inside /DecodeParms");
        assertTrue(text.contains("/Colors 3"),
                "/Colors 3 must be declared inside /DecodeParms");
        assertTrue(text.contains("/Columns 2"),
                "/Columns 2 (equal to image width) must be declared inside /DecodeParms");
    }

    @Test
    void decode_parms_bits_per_component_is_8() throws Exception {
        byte[] pdf  = buildPngPdf(TINY_RGB_PNG);
        String text = new String(pdf, StandardCharsets.US_ASCII);

        // Locate the image XObject (object 3 in the standard layout)
        String imageObj = extractObjectBody(text, "3 0 obj");
        assertTrue(imageObj.contains("/BitsPerComponent 8"),
                "/BitsPerComponent 8 must appear in the image XObject or /DecodeParms");
    }

    // ── IDAT bytes stored verbatim ────────────────────────────────────────────

    @Test
    void idat_bytes_are_stored_verbatim_not_re_compressed() throws Exception {
        // Parse the PNG the same way PngParser does, then verify the
        // exact bytes appear in the PDF.
        PngParser parsed = PngParser.parse(TINY_RGB_PNG);
        byte[] pdf = buildPngPdf(TINY_RGB_PNG);

        int idx = ByteUtil.indexOf(pdf, parsed.idatData, 0);
        assertNotEquals(-1, idx,
                "Raw IDAT bytes must be stored verbatim in the PDF stream");
    }

    @Test
    void idat_length_in_stream_dict_matches_actual_idat_length() throws Exception {
        PngParser parsed  = PngParser.parse(TINY_RGB_PNG);
        byte[]    pdf     = buildPngPdf(TINY_RGB_PNG);

        // Image XObject is object 3 in buildPngPdf layout
        byte[] objMarker  = "3 0 obj\n".getBytes(StandardCharsets.US_ASCII);
        byte[] lengthKey  = "/Length ".getBytes(StandardCharsets.US_ASCII);

        int objPos  = ByteUtil.indexOf(pdf, objMarker, 0);
        assertNotEquals(-1, objPos, "Image XObject object 3 not found");

        int lPos = ByteUtil.indexOf(pdf, lengthKey, objPos);
        assertNotEquals(-1, lPos, "/Length not found in image XObject");

        int ns = lPos + lengthKey.length;
        int ne = ns;
        while (ne < pdf.length && pdf[ne] >= '0' && pdf[ne] <= '9') ne++;
        int declaredLength = (int) ByteUtil.parseAsciiLong(pdf, ns, ne);

        assertEquals(parsed.idatData.length, declaredLength,
                "/Length must equal the raw IDAT byte count");
    }

    // ── Content-stream draw operators ─────────────────────────────────────────

    @Test
    void content_stream_contains_cm_and_im1_do() throws Exception {
        byte[] pdf = buildPngPdf(TINY_RGB_PNG);
        // Content stream is object 5 (image XObject=3, page=4)
        byte[] inflated = extractAndInflate(pdf, 5);
        String content  = new String(inflated, StandardCharsets.US_ASCII);
        assertTrue(content.contains("cm"),      "Content stream must contain 'cm' operator");
        assertTrue(content.contains("/Im1 Do"), "Content stream must contain '/Im1 Do'");
    }

    // ── Determinism ───────────────────────────────────────────────────────────

    @Test
    void output_is_deterministic_with_png_image() throws Exception {
        byte[] first  = buildPngPdf(TINY_RGB_PNG);
        byte[] second = buildPngPdf(TINY_RGB_PNG);
        assertArrayEquals(first, second,
                "Two save() calls with identical content must produce byte-identical PDFs");
    }

    // ── Null / invalid input guards ───────────────────────────────────────────

    @Test
    void add_png_image_rejects_null() {
        PdfDocument doc = new PdfDocument();
        assertThrows(NullPointerException.class, () -> doc.addPngImage(null));
    }

    @Test
    void add_png_image_rejects_invalid_signature() {
        byte[] garbage = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
        PdfDocument doc = new PdfDocument();
        assertThrows(IllegalArgumentException.class, () -> doc.addPngImage(garbage),
                "Bytes with wrong PNG signature must be rejected");
    }

    // ── PNG alias sequencing ──────────────────────────────────────────────────

    @Test
    void png_image_gets_sequential_alias_after_jpeg_image() {
        byte[] tinyJpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9};
        PdfDocument doc  = new PdfDocument();
        PdfImage jpeg    = doc.addJpegImage(tinyJpeg, 1, 1);
        PdfImage png     = doc.addPngImage(buildRgbPng(1, 1));
        assertEquals("Im1", jpeg.getAlias(), "first image must get Im1");
        assertEquals("Im2", png.getAlias(),  "second image must get Im2");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds a single-page PDF with one PNG image placed at (10, 10) with
     * dimensions 100×100.  Object layout:
     * <pre>
     *   1  Catalog
     *   2  Pages
     *   3  Image XObject (Im1)
     *   4  Page
     *   5  Contents (FlateDecode)
     * </pre>
     */
    private static byte[] buildPngPdf(byte[] pngBytes) throws Exception {
        PdfDocument doc = new PdfDocument();
        PdfImage img = doc.addPngImage(pngBytes);
        PdfPage page = doc.addPage();
        page.drawImage(img, 10, 10, 100, 100);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        return baos.toByteArray();
    }

    /** Locates an indirect object, reads its /Length, inflates the stream data. */
    private static byte[] extractAndInflate(byte[] pdf, int objNum) throws Exception {
        byte[] objMarker    = (objNum + " 0 obj\n").getBytes(StandardCharsets.US_ASCII);
        byte[] lengthKey    = "/Length ".getBytes(StandardCharsets.US_ASCII);
        byte[] streamMarker = "stream\n".getBytes(StandardCharsets.US_ASCII);

        int objPos = ByteUtil.indexOf(pdf, objMarker, 0);
        assertNotEquals(-1, objPos, "Object " + objNum + " not found");

        int lPos = ByteUtil.indexOf(pdf, lengthKey, objPos);
        assertNotEquals(-1, lPos, "/Length not found in object " + objNum);

        int ns = lPos + lengthKey.length;
        int ne = ns;
        while (ne < pdf.length && pdf[ne] >= '0' && pdf[ne] <= '9') ne++;
        int len = (int) ByteUtil.parseAsciiLong(pdf, ns, ne);

        int streamPos = ByteUtil.indexOf(pdf, streamMarker, objPos);
        assertNotEquals(-1, streamPos, "stream marker not found in object " + objNum);

        int dataStart = streamPos + streamMarker.length;
        byte[] compressed = new byte[len];
        System.arraycopy(pdf, dataStart, compressed, 0, len);

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

    private static String extractObjectBody(String pdf, String objHeader) {
        int start = pdf.indexOf(objHeader);
        assertNotEquals(-1, start, objHeader + " not found in PDF");
        int bodyStart = start + objHeader.length();
        int end = pdf.indexOf("endobj", bodyStart);
        assertNotEquals(-1, end, "endobj not found after " + objHeader);
        return pdf.substring(bodyStart, end);
    }

    // ── PNG builder utilities ─────────────────────────────────────────────────

    /**
     * Builds a valid minimal RGB (color type 2) PNG with the given dimensions.
     * Each pixel is pure red (0xFF, 0x00, 0x00).
     */
    private static byte[] buildRgbPng(int width, int height) {
        try {
            byte[] ihdrData  = buildIhdrData(width, height, 8, 2);
            byte[] rawPixels = buildRawScanlines(width, height);
            byte[] idatData  = zlibCompress(rawPixels);

            ByteArrayOutputStream png = new ByteArrayOutputStream();
            writePngSignature(png);
            writePngChunk(png, "IHDR", ihdrData);
            writePngChunk(png, "IDAT", idatData);
            writePngChunk(png, "IEND", new byte[0]);
            return png.toByteArray();
        } catch (Exception e) {
            throw new AssertionError("Failed to build test RGB PNG: " + e.getMessage(), e);
        }
    }

    /**
     * Builds a PNG whose IHDR declares the given {@code colorType} but whose
     * IDAT section is omitted.  Used only to test early rejection — the parser
     * throws before reaching IDAT when the color type is unsupported.
     */
    private static byte[] buildPngWithColorType(int width, int height, int colorType) {
        try {
            byte[] ihdrData = buildIhdrData(width, height, 8, colorType);
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            writePngSignature(png);
            writePngChunk(png, "IHDR", ihdrData);
            writePngChunk(png, "IEND", new byte[0]);
            return png.toByteArray();
        } catch (Exception e) {
            throw new AssertionError("Failed to build test PNG: " + e.getMessage(), e);
        }
    }

    private static byte[] buildIhdrData(int width, int height, int bitDepth, int colorType) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(13);
        writeInt4(out, width);
        writeInt4(out, height);
        out.write(bitDepth);
        out.write(colorType);
        out.write(0); // compression method
        out.write(0); // filter method
        out.write(0); // interlace method
        return out.toByteArray();
    }

    /** Raw scanlines: each row starts with a filter byte (0 = None), then R G B per pixel. */
    private static byte[] buildRawScanlines(int width, int height) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(height * (1 + width * 3));
        for (int y = 0; y < height; y++) {
            out.write(0); // filter byte: None
            for (int x = 0; x < width; x++) {
                out.write(0xFF); // R
                out.write(0x00); // G
                out.write(0x00); // B
            }
        }
        return out.toByteArray();
    }

    private static void writePngSignature(ByteArrayOutputStream out) {
        out.write(0x89);
        out.write(0x50); out.write(0x4E); out.write(0x47);
        out.write(0x0D); out.write(0x0A); out.write(0x1A); out.write(0x0A);
    }

    private static void writePngChunk(ByteArrayOutputStream out, String type, byte[] data)
            throws Exception {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc32 = new CRC32();
        crc32.update(typeBytes);
        crc32.update(data);
        writeInt4(out, data.length);
        out.write(typeBytes);
        out.write(data);
        writeInt4(out, (int) crc32.getValue());
    }

    private static void writeInt4(ByteArrayOutputStream out, int value) {
        out.write((value >>> 24) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 8)  & 0xFF);
        out.write(value          & 0xFF);
    }

    private static byte[] zlibCompress(byte[] input) {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, false);
        try {
            deflater.setInput(input);
            deflater.finish();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            while (!deflater.finished()) {
                int n = deflater.deflate(buf);
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();
        } finally {
            deflater.end();
        }
    }
}
