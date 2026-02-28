package io.offixa.pdfixa;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Test helper that wraps a {@link ByteArrayOutputStream} and tracks whether
 * {@link #close()} has been called.
 *
 * <p>Used to verify that {@code PdfDocument.save()} does not close the
 * caller-supplied stream.  After a save, the stream must still accept further
 * writes; closing must remain the caller's responsibility.
 */
public final class TrackingOutputStream extends OutputStream {

    private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();
    private boolean closed = false;

    @Override
    public void write(int b) throws IOException {
        requireOpen();
        delegate.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        requireOpen();
        delegate.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        requireOpen();
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        closed = true;
        delegate.close();
    }

    /** Returns {@code true} if {@link #close()} has been called at least once. */
    public boolean isClosed() {
        return closed;
    }

    /** Returns all bytes written to this stream so far. */
    public byte[] toByteArray() {
        return delegate.toByteArray();
    }

    private void requireOpen() throws IOException {
        if (closed) {
            throw new IOException("Stream has already been closed");
        }
    }
}
