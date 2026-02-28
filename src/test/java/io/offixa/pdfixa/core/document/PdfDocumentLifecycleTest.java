package io.offixa.pdfixa.core.document;

import io.offixa.pdfixa.TrackingOutputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for Phase 0 lifecycle contract of {@link PdfDocument}.
 *
 * <p>Covered scenarios:
 * <ol>
 *   <li>{@code save(OutputStream)} must NOT close the provided stream.</li>
 *   <li>Calling {@code save()} twice must throw {@link IllegalStateException}.</li>
 *   <li>Two identical documents must produce bit-for-bit identical output (determinism).</li>
 * </ol>
 */
class PdfDocumentLifecycleTest {

    // ── Test 1: save() must not close the caller's stream ──────────────────

    @Test
    void saveDoesNotCloseProvidedStream() throws IOException {
        PdfDocument doc = minimalDoc();
        TrackingOutputStream tracking = new TrackingOutputStream();

        doc.save(tracking);

        assertFalse(tracking.isClosed(),
                "save() must not close the provided OutputStream — closing is the caller's responsibility");

        // Stream must still accept writes after save() returns.
        assertDoesNotThrow(() -> tracking.write(0x25),
                "OutputStream must remain open and writable after save() returns");

        tracking.close();
    }

    // ── Test 2: save() called twice must throw IllegalStateException ────────

    @Test
    void saveCalledTwiceThrowsIllegalStateException() throws IOException {
        PdfDocument doc = minimalDoc();

        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        doc.save(out1);

        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> doc.save(out2),
                "Second call to save() must throw IllegalStateException"
        );
        assertTrue(ex.getMessage().contains("already been saved"),
                "Exception message must contain \"already been saved\", but was: " + ex.getMessage());
    }

    // ── Test 4: Determinism — same document must produce identical bytes ────

    @Test
    void identicalDocumentProducesIdenticalBytes() throws IOException {
        byte[] run1 = buildMinimalDocument();
        byte[] run2 = buildMinimalDocument();

        assertArrayEquals(run1, run2,
                "The same document built and saved twice must produce bit-for-bit identical output");
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Builds a minimal one-page document with a single short text run and
     * saves it to a fresh {@link ByteArrayOutputStream}.
     */
    private static byte[] buildMinimalDocument() throws IOException {
        PdfDocument doc = minimalDoc();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        return baos.toByteArray();
    }

    /**
     * Returns a new {@link PdfDocument} with one page containing one text line.
     * Kept deliberately minimal to make test failures easy to diagnose.
     */
    private static PdfDocument minimalDoc() {
        PdfDocument doc = new PdfDocument();
        PdfPage page = doc.addPage();
        page.getContent()
                .beginText()
                .setFont("Helvetica", 12)
                .moveText(100, 700)
                .showText("Hello PDFixa")
                .endText();
        return doc;
    }
}
