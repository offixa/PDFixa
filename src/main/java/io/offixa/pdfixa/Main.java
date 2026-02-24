package io.offixa.pdfixa;

import io.offixa.pdfixa.core.document.ObjectRegistry;
import io.offixa.pdfixa.core.document.TrailerBuilder;
import io.offixa.pdfixa.core.document.XrefTableBuilder;
import io.offixa.pdfixa.core.writer.PdfWriter;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Minimal end-to-end demo: writes a one-page, content-less PDF to {@code output.pdf}.
 *
 * <p>Object layout:
 * <pre>
 *   1  Catalog  << /Type /Catalog /Pages 2 0 R >>
 *   2  Pages    << /Type /Pages /Kids [3 0 R] /Count 1 >>
 *   3  Page     << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] >>
 * </pre>
 */
public final class Main {

    public static void main(String[] args) throws Exception {
        try (FileOutputStream fos = new FileOutputStream("output.pdf");
             PdfWriter writer = new PdfWriter(fos)) {

            // ── Header ────────────────────────────────────────────────
            writer.writeBytes("%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII));
            // Binary comment signals to transfer agents that this is a binary file.
            writer.writeBytes(new byte[]{'%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'});

            // ── Object allocation ─────────────────────────────────────
            ObjectRegistry registry = new ObjectRegistry();

            int catalogNum = registry.allocate(); // 1
            int pagesNum   = registry.allocate(); // 2
            int pageNum    = registry.allocate(); // 3

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
                w.endDictionary();
            });

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
}
