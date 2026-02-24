package io.offixa.pdfixa;

import io.offixa.pdfixa.core.content.ContentStream;
import io.offixa.pdfixa.core.document.ObjectRegistry;
import io.offixa.pdfixa.core.document.TrailerBuilder;
import io.offixa.pdfixa.core.document.XrefTableBuilder;
import io.offixa.pdfixa.core.writer.PdfWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Minimal end-to-end demo: writes a two-page PDF with visible text to {@code output.pdf}.
 *
 * <p>Object layout:
 * <pre>
 *   1  Catalog   &lt;&lt; /Type /Catalog /Pages 2 0 R &gt;&gt;
 *   2  Pages     &lt;&lt; /Type /Pages /Kids [3 0 R 5 0 R] /Count 2 &gt;&gt;
 *   3  Page1     &lt;&lt; /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842]
 *                   /Resources &lt;&lt; /Font &lt;&lt; /F1 &lt;&lt; /Type /Font /Subtype /Type1 /BaseFont /Helvetica &gt;&gt; &gt;&gt; &gt;&gt;
 *                   /Contents 4 0 R &gt;&gt;
 *   4  Contents1 stream (BT … ET + line + rectangles)
 *   5  Page2     &lt;&lt; /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842]
 *                   /Resources &lt;&lt; /Font &lt;&lt; /F1 &lt;&lt; /Type /Font /Subtype /Type1 /BaseFont /Helvetica &gt;&gt; &gt;&gt; &gt;&gt;
 *                   /Contents 6 0 R &gt;&gt;
 *   6  Contents2 stream (BT "Second page" ET + rect)
 * </pre>
 */
public final class Main {

    private static final byte[] CONTENT_STREAM_BYTES = new ContentStream()
            .beginText()
            .setFont("F1", 12)
            .moveText(100, 700)
            .showText("Hello PDFixa")
            .endText()
            .setLineWidth(2)
            .moveTo(50, 650)
            .lineTo(300, 650)
            .stroke()
            .rectangle(50, 500, 200, 100)
            .stroke()
            .rectangle(300, 500, 100, 100)
            .fill()
            .toBytes();

    private static final byte[] CONTENT_STREAM_BYTES_PAGE2 = new ContentStream()
            .beginText()
            .setFont("F1", 12)
            .moveText(100, 700)
            .showText("Second page")
            .endText()
            .rectangle(200, 400, 150, 80)
            .stroke()
            .toBytes();

    public static void main(String[] args) throws Exception {
        try (FileOutputStream fos = new FileOutputStream("output.pdf");
             PdfWriter writer = new PdfWriter(fos)) {

            // ── Header ────────────────────────────────────────────────
            writer.writeBytes("%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII));
            writer.writeBytes(new byte[]{'%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'});

            // ── Object allocation ─────────────────────────────────────
            ObjectRegistry registry = new ObjectRegistry();

            int catalogNum   = registry.allocate(); // 1
            int pagesNum     = registry.allocate(); // 2
            int page1Num     = registry.allocate(); // 3
            int contents1Num = registry.allocate(); // 4
            int page2Num     = registry.allocate(); // 5
            int contents2Num = registry.allocate(); // 6

            registry.setBody(catalogNum, w -> {
                w.beginDictionary();
                w.writeName("Type");  w.writeSpace(); w.writeName("Catalog");
                w.writeSpace();
                w.writeName("Pages"); w.writeSpace(); w.writeReference(pagesNum, 0);
                w.endDictionary();
            });

            registry.setBody(pagesNum, w -> {
                w.beginDictionary();
                w.writeName("Type");  w.writeSpace(); w.writeName("Pages");
                w.writeSpace();
                w.writeName("Kids");  w.writeSpace();
                w.beginArray();
                w.writeReference(page1Num, 0); w.writeSpace();
                w.writeReference(page2Num, 0);
                w.endArray();
                w.writeSpace();
                w.writeName("Count"); w.writeSpace(); w.writeInt(2);
                w.endDictionary();
            });

            registry.setBody(page1Num, w -> writePageDict(w, pagesNum, contents1Num));
            registry.setBody(contents1Num, w -> w.writeStream(CONTENT_STREAM_BYTES));
            registry.setBody(page2Num, w -> writePageDict(w, pagesNum, contents2Num));
            registry.setBody(contents2Num, w -> w.writeStream(CONTENT_STREAM_BYTES_PAGE2));

            registry.setRoot(catalogNum);

            // ── Serialization ─────────────────────────────────────────
            registry.writeAll(writer);

            long startxref = XrefTableBuilder.write(
                    writer, registry.getOffsets(), registry.getObjectCount());

            TrailerBuilder.write(
                    writer,
                    registry.getObjectCount() + 1,
                    registry.getRootObjectNumber(),
                    startxref);

            writer.flush();
        }

        System.out.println("output.pdf written successfully.");
    }

    private static void writePageDict(PdfWriter w, int pagesNum, int contentsNum)
            throws IOException {
        w.beginDictionary();
        w.writeName("Type");     w.writeSpace(); w.writeName("Page");
        w.writeSpace();
        w.writeName("Parent");   w.writeSpace(); w.writeReference(pagesNum, 0);
        w.writeSpace();
        w.writeName("MediaBox"); w.writeSpace();
        w.beginArray();
        w.writeInt(0);   w.writeSpace();
        w.writeInt(0);   w.writeSpace();
        w.writeInt(595); w.writeSpace();
        w.writeInt(842);
        w.endArray();
        w.writeSpace();
        w.writeName("Resources"); w.writeSpace();
        w.beginDictionary();
        w.writeName("Font"); w.writeSpace();
        w.beginDictionary();
        w.writeName("F1"); w.writeSpace();
        w.beginDictionary();
        w.writeName("Type");     w.writeSpace(); w.writeName("Font");
        w.writeSpace();
        w.writeName("Subtype");  w.writeSpace(); w.writeName("Type1");
        w.writeSpace();
        w.writeName("BaseFont"); w.writeSpace(); w.writeName("Helvetica");
        w.endDictionary();
        w.endDictionary();
        w.endDictionary();
        w.writeSpace();
        w.writeName("Contents"); w.writeSpace(); w.writeReference(contentsNum, 0);
        w.endDictionary();
    }
}
