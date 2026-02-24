package io.offixa.pdfixa;

import io.offixa.pdfixa.core.document.ObjectRegistry;
import io.offixa.pdfixa.core.document.TrailerBuilder;
import io.offixa.pdfixa.core.document.XrefTableBuilder;
import io.offixa.pdfixa.core.writer.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Test-only helper that replicates the Main document layout and returns the
 * generated PDF as a {@code byte[]}.
 *
 * <p>Object layout:
 * <pre>
 *   1  Catalog  &lt;&lt; /Type /Catalog /Pages 2 0 R &gt;&gt;
 *   2  Pages    &lt;&lt; /Type /Pages /Kids [3 0 R] /Count 1 &gt;&gt;
 *   3  Page     &lt;&lt; /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842]
 *                  /Resources &lt;&lt; /Font &lt;&lt; /F1 &lt;&lt; /Type /Font /Subtype /Type1 /BaseFont /Helvetica &gt;&gt; &gt;&gt; &gt;&gt;
 *                  /Contents 4 0 R &gt;&gt;
 *   4  Contents stream (BT … ET)
 * </pre>
 */
public final class PdfGenerator {

    /** Number of real (non-free) PDF objects written by {@link #generate()}. */
    public static final int OBJECT_COUNT = 4;

    /** Object number of the document catalog (/Root). */
    public static final int ROOT_OBJECT_NUM = 1;

    /** Raw US-ASCII bytes of the content stream (no compression). */
    public static final byte[] CONTENT_STREAM_BYTES =
            "BT\n/F1 12 Tf\n100 700 Td\n(Hello PDFixa) Tj\nET"
                    .getBytes(StandardCharsets.US_ASCII);

    private PdfGenerator() {}

    /**
     * Generates a minimal but structurally complete PDF 1.7 document and
     * returns it as a byte array. Produces deterministic output on every call.
     */
    public static byte[] generate() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(baos)) {

            writer.writeBytes("%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII));
            writer.writeBytes(new byte[]{'%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'});

            ObjectRegistry registry = new ObjectRegistry();
            int catalogNum  = registry.allocate(); // 1
            int pagesNum    = registry.allocate(); // 2
            int pageNum     = registry.allocate(); // 3
            int contentsNum = registry.allocate(); // 4

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
                w.beginArray(); w.writeReference(pageNum, 0); w.endArray();
                w.writeSpace();
                w.writeName("Count"); w.writeSpace(); w.writeInt(1);
                w.endDictionary();
            });

            registry.setBody(pageNum, w -> {
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
            });

            registry.setBody(contentsNum, w -> w.writeStream(CONTENT_STREAM_BYTES));

            registry.setRoot(catalogNum);
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
        return baos.toByteArray();
    }
}
