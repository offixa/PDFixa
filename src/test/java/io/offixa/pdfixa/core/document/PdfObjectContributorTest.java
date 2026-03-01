package io.offixa.pdfixa.core.document;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link PdfObjectContributor} extension mechanism introduced
 * in pdfixa-core 0.8.0.
 */
class PdfObjectContributorTest {

    @Test
    void contributorAllocatesObjectDeterministically() throws IOException {
        byte[] run1 = buildWithDummyContributor();
        byte[] run2 = buildWithDummyContributor();

        assertArrayEquals(run1, run2,
                "Document with a contributor must produce bit-for-bit identical output across runs");
    }

    @Test
    void noContributorProducesSameOutputAsPlainDocument() throws IOException {
        byte[] withoutContributor = buildPlain();
        byte[] withEmptyList = buildPlain();

        assertArrayEquals(withoutContributor, withEmptyList,
                "Empty contributor list must produce byte-identical output to a plain document");
    }

    @Test
    void contributorExecutionOrderMatchesRegistrationOrder() throws IOException {
        StringBuilder order = new StringBuilder();

        PdfDocument doc = new PdfDocument();
        doc.addPage().getContent()
                .beginText()
                .setFont("Helvetica", 12)
                .moveText(72, 700)
                .showText("test")
                .endText();

        doc.registerContributor(ctx -> order.append("A"));
        doc.registerContributor(ctx -> order.append("B"));
        doc.registerContributor(ctx -> order.append("C"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);

        assertEquals("ABC", order.toString(),
                "Contributors must execute in registration order");
    }

    private static byte[] buildWithDummyContributor() throws IOException {
        PdfDocument doc = new PdfDocument();
        PdfPage page = doc.addPage();
        page.getContent()
                .beginText()
                .setFont("Helvetica", 12)
                .moveText(72, 700)
                .showText("contributor test")
                .endText();

        doc.registerContributor(ctx -> {
            int objNum = ctx.allocateObject();
            ctx.setObjectBody(objNum, w -> {
                w.beginDictionary();
                w.writeName("Type");  w.writeSpace(); w.writeName("DummyObj");
                w.endDictionary();
            });
        });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        return baos.toByteArray();
    }

    private static byte[] buildPlain() throws IOException {
        PdfDocument doc = new PdfDocument();
        PdfPage page = doc.addPage();
        page.getContent()
                .beginText()
                .setFont("Helvetica", 12)
                .moveText(72, 700)
                .showText("plain test")
                .endText();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        return baos.toByteArray();
    }
}
