package io.offixa.pdfixa;

import io.offixa.pdfixa.core.document.PdfDocument;
import io.offixa.pdfixa.core.document.PdfImage;
import io.offixa.pdfixa.core.document.PdfPage;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * End-to-end demo for PDFixa.
 *
 * <p>Produces {@code output.pdf} with two pages:
 * <ul>
 *   <li><b>Page 1</b> — text, lines, rectangles, and an embedded PNG image (blue square)</li>
 *   <li><b>Page 2</b> — second page with heading and a filled rectangle</li>
 * </ul>
 */
public final class Main {

    public static void main(String[] args) throws Exception {
        PdfDocument doc = new PdfDocument();

        // ── Embed a 32×32 blue PNG image ──────────────────────────────────────
        byte[] bluePng = buildSolidColorPng(32, 32, 0x00, 0x6E, 0xFF);
        PdfImage blueBox = doc.addPngImage(bluePng);

        // ── Page 1 ────────────────────────────────────────────────────────────
        PdfPage page1 = doc.addPage();

        page1.getContent()
                // Title
                .beginText()
                .setFont("Helvetica-Bold", 18)
                .moveText(72, 760)
                .showText("PDFixa - Feature Demo")
                .endText()

                // Subtitle
                .beginText()
                .setFont("Helvetica", 11)
                .moveText(72, 735)
                .showText("Text  |  Lines  |  Rectangles  |  PNG Image XObject")
                .endText()

                // Horizontal divider line
                .setLineWidth(0.5)
                .moveTo(72, 725)
                .lineTo(523, 725)
                .stroke()

                // Body text paragraph
                .beginText()
                .setFont("Helvetica", 11)
                .moveText(72, 700)
                .showText("This PDF was generated entirely in Java without any third-party library.")
                .endText()

                .beginText()
                .setFont("Helvetica", 11)
                .moveText(72, 682)
                .showText("All bytes (objects, streams, xref table, trailer) are hand-crafted.")
                .endText()

                // Section: shapes
                .beginText()
                .setFont("Helvetica-Bold", 13)
                .moveText(72, 648)
                .showText("Shapes")
                .endText()

                // Stroked rectangle
                .setLineWidth(1.5)
                .rectangle(72, 560, 120, 60)
                .stroke()

                // Filled rectangle
                .rectangle(220, 560, 120, 60)
                .fill()

                // Diagonal line
                .setLineWidth(2)
                .moveTo(370, 560)
                .lineTo(490, 620)
                .stroke()

                // Labels for shapes
                .beginText()
                .setFont("Helvetica", 9)
                .moveText(88, 546)
                .showText("stroke")
                .endText()
                .beginText()
                .setFont("Helvetica", 9)
                .moveText(256, 546)
                .showText("fill")
                .endText()
                .beginText()
                .setFont("Helvetica", 9)
                .moveText(404, 546)
                .showText("lineTo")
                .endText()

                // Section: PNG image
                .beginText()
                .setFont("Helvetica-Bold", 13)
                .moveText(72, 510)
                .showText("PNG Image XObject  (32x32, blue)")
                .endText();

        // Draw the PNG at (72, 440) rendered as 64×64 pts
        page1.drawImage(blueBox, 72, 440, 64, 64);

        page1.getContent()
                .beginText()
                .setFont("Helvetica", 9)
                .moveText(72, 428)
                .showText("/Filter /FlateDecode  /DecodeParms /Predictor 15")
                .endText();

        // ── Page 2 ────────────────────────────────────────────────────────────
        PdfPage page2 = doc.addPage();

        page2.getContent()
                .beginText()
                .setFont("Helvetica-Bold", 16)
                .moveText(72, 760)
                .showText("Page 2 - Second Page Example")
                .endText()

                .setLineWidth(0.5)
                .moveTo(72, 750)
                .lineTo(523, 750)
                .stroke()

                .beginText()
                .setFont("Helvetica", 11)
                .moveText(72, 725)
                .showText("Deterministic output: saving the same document twice")
                .endText()
                .beginText()
                .setFont("Helvetica", 11)
                .moveText(72, 707)
                .showText("always produces byte-identical PDF files.")
                .endText()

                // Large filled block as a decorative element
                .rectangle(72, 600, 451, 80)
                .fill();

        // ── Save ──────────────────────────────────────────────────────────────
        try (FileOutputStream fos = new FileOutputStream("output.pdf")) {
            doc.save(fos);
        }

        System.out.println("output.pdf written (2 pages).");
    }

    // ── PNG builder ───────────────────────────────────────────────────────────

    /** Builds a solid-color 8-bit RGB PNG with the given dimensions and fill color. */
    private static byte[] buildSolidColorPng(int w, int h, int r, int g, int b) throws Exception {
        // Raw scanlines: filter byte (0 = None) + w×3 RGB bytes per row
        ByteArrayOutputStream raw = new ByteArrayOutputStream(h * (1 + w * 3));
        for (int y = 0; y < h; y++) {
            raw.write(0);
            for (int x = 0; x < w; x++) {
                raw.write(r); raw.write(g); raw.write(b);
            }
        }

        byte[] idat = zlibCompress(raw.toByteArray());

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        // Signature
        png.write(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        // IHDR
        ByteArrayOutputStream ihdr = new ByteArrayOutputStream(13);
        writeInt4(ihdr, w); writeInt4(ihdr, h);
        ihdr.write(8); ihdr.write(2); ihdr.write(0); ihdr.write(0); ihdr.write(0);
        writeChunk(png, "IHDR", ihdr.toByteArray());
        // IDAT
        writeChunk(png, "IDAT", idat);
        // IEND
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
        out.write((v >>>  8) & 0xFF);
        out.write(v          & 0xFF);
    }

    private static byte[] zlibCompress(byte[] input) {
        Deflater d = new Deflater(Deflater.DEFAULT_COMPRESSION, false);
        try {
            d.setInput(input);
            d.finish();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            while (!d.finished()) { int n = d.deflate(buf); baos.write(buf, 0, n); }
            return baos.toByteArray();
        } finally {
            d.end();
        }
    }
}
