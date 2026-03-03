package io.offixa.pdfixa.core.document;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 3: Determinism Hard Proof.
 *
 * <p>Proves byte-for-byte reproducibility of PDFixa output across JVM runs,
 * locales, and repeated invocations. The canonical document exercises every
 * major feature path:
 * <ul>
 *   <li>Base-14 font text ({@code showText})</li>
 *   <li>Unicode raw text ({@code showTextUnicodeRaw})</li>
 *   <li>JPEG image XObject</li>
 *   <li>PNG image XObject</li>
 *   <li>Document metadata ({@link PdfInfo})</li>
 *   <li>Custom page size ({@link PdfPageSize})</li>
 * </ul>
 *
 * <p>No test depends on {@link System#currentTimeMillis()} or any other
 * non-deterministic system state. All metadata dates are fixed string literals.
 */
class CanonicalDeterminismTest {

    private static final byte[] TINY_JPEG = {
            (byte) 0xFF, (byte) 0xD8,
            (byte) 0xFF, (byte) 0xD9
    };

    private static final PdfPageSize CUSTOM_PAGE = new PdfPageSize(700, 500);

    private static final String EXPECTED_SHA256 =
            "042051ed6a438ce1a99760363ab1eb5ffb4b9f87e684d26f8fefb545037742dd";

    /**
     * Builds the canonical PDF exercising all major feature paths.
     * Every input is a compile-time constant — no runtime entropy.
     */
    static byte[] buildCanonicalPdf() throws Exception {
        PdfDocument doc = new PdfDocument(CUSTOM_PAGE);

        doc.setInfo(PdfInfo.builder()
                .title("Determinism Proof")
                .author("PDFixa CI")
                .subject("Phase 3 canonical document")
                .keywords("determinism, test, canonical")
                .creator("CanonicalDeterminismTest")
                .creationDate("D:20250101120000+00'00'")
                .modDate("D:20250101120000+00'00'")
                .build());

        byte[] pngBytes = buildSolidColorPng(16, 16, 0xFF, 0x00, 0x00);
        PdfImage jpegImg = doc.addJpegImage(TINY_JPEG, 10, 8);
        PdfImage pngImg = doc.addPngImage(pngBytes);

        PdfPage page = doc.addPage();

        page.getContent()
                .beginText()
                .setFont("Helvetica-Bold", 14)
                .moveText(50, 450)
                .showText("Determinism Proof: Base14 Text")
                .endText()

                .beginText()
                .setFont("Helvetica", 11)
                .moveText(50, 430)
                .showText("The quick brown fox jumps over the lazy dog.")
                .endText()

                .beginText()
                .setFont("Courier", 10)
                .moveText(50, 410)
                .showText("0123456789 !@#$%^&*()")
                .endText()

                .beginText()
                .setFont("Helvetica", 11)
                .moveText(50, 380)
                .showTextUnicodeRaw("Salom Dunyo!")
                .endText()

                .beginText()
                .setFont("Helvetica", 11)
                .moveText(50, 360)
                .showTextUnicodeRaw("\u041F\u0440\u0438\u0432\u0435\u0442 \u043C\u0438\u0440!")
                .endText()

                .beginText()
                .setFont("Helvetica", 11)
                .moveText(50, 340)
                .showTextUnicodeRaw("\u4F60\u597D\u4E16\u754C")
                .endText();

        page.drawImage(jpegImg, 50, 250, 100, 80);
        page.drawImage(pngImg, 200, 250, 64, 64);

        page.getContent()
                .setStrokeColor(0.2, 0.4, 0.8)
                .setLineWidth(1.5)
                .rectangle(50, 200, 200, 30)
                .stroke()

                .setFillColor(0.9, 0.1, 0.1)
                .rectangle(300, 200, 100, 30)
                .fill();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        return baos.toByteArray();
    }

    static String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }

    // ── 1. Canonical hash test ──────────────────────────────────────────────

    @Test
    void canonical_pdf_matches_expected_sha256() throws Exception {
        byte[] pdf = buildCanonicalPdf();
        String hash = sha256Hex(pdf);

        assertEquals(EXPECTED_SHA256, hash,
                "Canonical PDF SHA-256 mismatch — if the document builder changed "
                        + "intentionally, update EXPECTED_SHA256 to: " + hash);
    }

    // ── 2. Multi-run stability ──────────────────────────────────────────────

    @Test
    void three_generations_produce_identical_bytes() throws Exception {
        byte[] run1 = buildCanonicalPdf();
        byte[] run2 = buildCanonicalPdf();
        byte[] run3 = buildCanonicalPdf();

        assertArrayEquals(run1, run2, "Run 1 vs Run 2 must be byte-identical");
        assertArrayEquals(run2, run3, "Run 2 vs Run 3 must be byte-identical");
    }

    // ── 3. Locale stability ─────────────────────────────────────────────────

    @Test
    void output_is_identical_across_locales() throws Exception {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            byte[] usBytes = buildCanonicalPdf();

            Locale.setDefault(Locale.GERMANY);
            byte[] deBytes = buildCanonicalPdf();

            Locale.setDefault(new Locale("tr", "TR"));
            byte[] trBytes = buildCanonicalPdf();

            Locale.setDefault(Locale.JAPAN);
            byte[] jpBytes = buildCanonicalPdf();

            Locale.setDefault(new Locale("ar", "SA"));
            byte[] arBytes = buildCanonicalPdf();

            assertArrayEquals(usBytes, deBytes, "US vs DE locale must be byte-identical");
            assertArrayEquals(usBytes, trBytes, "US vs TR locale must be byte-identical");
            assertArrayEquals(usBytes, jpBytes, "US vs JP locale must be byte-identical");
            assertArrayEquals(usBytes, arBytes, "US vs AR locale must be byte-identical");
        } finally {
            Locale.setDefault(original);
        }
    }

    // ── 4. No system-time dependency ────────────────────────────────────────

    @Test
    void metadata_uses_fixed_dates_not_system_time() throws Exception {
        byte[] pdf = buildCanonicalPdf();
        String text = new String(pdf, StandardCharsets.US_ASCII);

        assertTrue(text.contains("D:20250101120000+00'00'"),
                "PDF must contain the fixed CreationDate literal");
        assertFalse(text.contains("D:20" + java.time.Year.now().getValue()),
                "PDF must not embed current-year timestamps");
    }

    // ── PNG builder (deterministic, self-contained) ─────────────────────────

    static byte[] buildSolidColorPng(int w, int h, int r, int g, int b) throws Exception {
        ByteArrayOutputStream raw = new ByteArrayOutputStream(h * (1 + w * 3));
        for (int y = 0; y < h; y++) {
            raw.write(0);
            for (int x = 0; x < w; x++) {
                raw.write(r);
                raw.write(g);
                raw.write(b);
            }
        }

        byte[] idat = zlibCompress(raw.toByteArray());

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        png.write(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        ByteArrayOutputStream ihdr = new ByteArrayOutputStream(13);
        writeInt4(ihdr, w);
        writeInt4(ihdr, h);
        ihdr.write(8);
        ihdr.write(2);
        ihdr.write(0);
        ihdr.write(0);
        ihdr.write(0);
        writeChunk(png, "IHDR", ihdr.toByteArray());
        writeChunk(png, "IDAT", idat);
        writeChunk(png, "IEND", new byte[0]);
        return png.toByteArray();
    }

    private static void writeChunk(ByteArrayOutputStream out, String type, byte[] data)
            throws Exception {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        writeInt4(out, data.length);
        out.write(typeBytes);
        out.write(data);
        writeInt4(out, (int) crc.getValue());
    }

    private static void writeInt4(ByteArrayOutputStream out, int v) {
        out.write((v >>> 24) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 8) & 0xFF);
        out.write(v & 0xFF);
    }

    private static byte[] zlibCompress(byte[] input) {
        Deflater d = new Deflater(Deflater.DEFAULT_COMPRESSION, false);
        try {
            d.setInput(input);
            d.finish();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            while (!d.finished()) {
                int n = d.deflate(buf);
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();
        } finally {
            d.end();
        }
    }
}
