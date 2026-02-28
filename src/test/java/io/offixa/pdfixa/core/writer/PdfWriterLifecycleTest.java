package io.offixa.pdfixa.core.writer;

import io.offixa.pdfixa.core.internal.PdfWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for Phase 0 lifecycle contract of {@link PdfWriter}.
 *
 * <p>Covered scenario:
 * <ol>
 *   <li>{@code finish()} transitions the writer to a FINISHED state, after which
 *       any write operation must throw {@link IllegalStateException}.</li>
 * </ol>
 */
class PdfWriterLifecycleTest {

    // ── Test 3: write operations after finish() must throw ─────────────────

    @Test
    void writeIntAfterFinishThrowsIllegalStateException() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);

        writer.writeInt(1);
        writer.finish();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> writer.writeInt(2),
                "writeInt() after finish() must throw IllegalStateException"
        );
        assertNotNull(ex.getMessage(),
                "IllegalStateException thrown after finish() must carry a non-null message");
    }

    @Test
    void writeBytesAfterFinishThrowsIllegalStateException() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);

        writer.finish();

        assertThrows(IllegalStateException.class,
                () -> writer.writeBytes(new byte[]{0x25}),
                "writeBytes() after finish() must throw IllegalStateException");
    }

    @Test
    void writeNameAfterFinishThrowsIllegalStateException() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);

        writer.finish();

        assertThrows(IllegalStateException.class,
                () -> writer.writeName("Type"),
                "writeName() after finish() must throw IllegalStateException");
    }

    @Test
    void finishIsIdempotent() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);

        writer.finish();

        assertDoesNotThrow(writer::finish,
                "Calling finish() a second time must be a no-op, not throw");
    }
}
