package io.offixa.pdfixa;

import io.offixa.pdfixa.core.document.PdfDocument;
import io.offixa.pdfixa.core.document.PdfPage;

import java.io.FileOutputStream;

/**
 * Minimal end-to-end demo: writes a single-page PDF via the {@link PdfDocument} facade.
 */
public final class Main {

    public static void main(String[] args) throws Exception {
        PdfDocument doc = new PdfDocument();

        PdfPage page = doc.addPage();
        page.getContent()
                .beginText()
                .setFont("F1", 12)
                .moveText(100, 700)
                .showText("Hello from facade")
                .endText();

        try (FileOutputStream fos = new FileOutputStream("output.pdf")) {
            doc.save(fos);
        }

        System.out.println("output.pdf written successfully.");
    }
}
