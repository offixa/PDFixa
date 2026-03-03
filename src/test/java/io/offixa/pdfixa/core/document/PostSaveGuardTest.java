package io.offixa.pdfixa.core.document;

import io.offixa.pdfixa.core.content.ContentStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that {@link PdfDocument#save} seals all page content streams,
 * making post-save modification impossible.
 */
class PostSaveGuardTest {

    private static final byte[] TINY_JPEG = {
        (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9
    };

    @Test
    void contentStream_not_sealed_before_save() {
        PdfDocument doc = new PdfDocument();
        PdfPage page = doc.addPage();
        ContentStream cs = page.getContent();
        cs.beginText().setFont("Helvetica", 12).moveText(72, 700).showText("test").endText();

        assertFalse(cs.isSealed(), "ContentStream must not be sealed before save");
    }

    @Test
    void contentStream_sealed_after_save() throws IOException {
        PdfDocument doc = new PdfDocument();
        PdfPage page = doc.addPage();
        ContentStream cs = page.getContent();
        cs.beginText().setFont("Helvetica", 12).moveText(72, 700).showText("test").endText();

        doc.save(new ByteArrayOutputStream());

        assertTrue(cs.isSealed(), "ContentStream must be sealed after save");
    }

    @Test
    void all_pages_sealed_after_save() throws IOException {
        PdfDocument doc = new PdfDocument();

        PdfPage p1 = doc.addPage();
        p1.getContent().beginText().setFont("Helvetica", 12)
                .moveText(72, 700).showText("page 1").endText();

        PdfPage p2 = doc.addPage();
        p2.getContent().beginText().setFont("Courier", 10)
                .moveText(72, 700).showText("page 2").endText();

        ContentStream cs1 = p1.getContent();
        ContentStream cs2 = p2.getContent();

        doc.save(new ByteArrayOutputStream());

        assertTrue(cs1.isSealed(), "Page 1 ContentStream must be sealed after save");
        assertTrue(cs2.isSealed(), "Page 2 ContentStream must be sealed after save");
    }

    @Test
    void beginText_after_save_throws() throws IOException {
        PdfDocument doc = new PdfDocument();
        PdfPage page = doc.addPage();
        ContentStream cs = page.getContent();
        cs.beginText().setFont("Helvetica", 12).moveText(72, 700).showText("test").endText();

        doc.save(new ByteArrayOutputStream());

        assertThrows(IllegalStateException.class, cs::beginText,
                "Mutating a sealed ContentStream must throw IllegalStateException");
    }

    @Test
    void drawImage_after_save_throws() throws IOException {
        PdfDocument doc = new PdfDocument();
        PdfImage img = doc.addJpegImage(TINY_JPEG, 10, 8);
        PdfPage page = doc.addPage();
        page.getContent().beginText().setFont("Helvetica", 12)
                .moveText(72, 700).showText("test").endText();
        page.drawImage(img, 0, 0, 100, 80);

        doc.save(new ByteArrayOutputStream());

        assertThrows(IllegalStateException.class,
                () -> page.drawImage(img, 0, 0, 50, 40),
                "drawImage after save must throw because ContentStream is sealed");
    }

    @Test
    void drawTextBox_after_save_throws() throws IOException {
        PdfDocument doc = new PdfDocument();
        PdfPage page = doc.addPage();
        page.getContent().beginText().setFont("Helvetica", 12)
                .moveText(72, 700).showText("test").endText();

        doc.save(new ByteArrayOutputStream());

        assertThrows(IllegalStateException.class,
                () -> page.drawTextBox(72, 700, 400, 14, "Helvetica", 12, "text"),
                "drawTextBox after save must throw because ContentStream is sealed");
    }

    @Test
    void determinism_preserved_with_seal() throws IOException {
        byte[] run1 = buildAndSave();
        byte[] run2 = buildAndSave();
        assertArrayEquals(run1, run2,
                "Seal must not affect deterministic output");
    }

    private static byte[] buildAndSave() throws IOException {
        PdfDocument doc = new PdfDocument();
        PdfPage page = doc.addPage();
        page.getContent()
                .beginText()
                .setFont("Helvetica", 12)
                .moveText(100, 700)
                .showText("Determinism check")
                .endText();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        return baos.toByteArray();
    }
}
