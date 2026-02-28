package io.offixa.pdfixa.core.internal;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * An {@link OutputStream} wrapper that tracks the exact number of bytes written.
 *
 * <p>Every write call delegates directly to the underlying stream and increments
 * the position counter by the exact byte count. No hidden buffering is introduced.
 *
 * <p><strong>Critical override note:</strong> {@link FilterOutputStream#write(byte[])}
 * delegates to {@link FilterOutputStream#write(byte[], int, int)}, which in turn calls
 * {@link #write(int)} in a loop. We override all three methods to delegate directly to
 * the underlying stream, avoiding per-byte dispatch overhead and ensuring the position
 * counter increments correctly regardless of which overload the caller uses.
 *
 * <p>This class is not thread-safe.
 */
public final class CountingOutputStream extends FilterOutputStream {

    private long position;

    public CountingOutputStream(OutputStream out) {
        super(Objects.requireNonNull(out, "out"));
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        position++;
    }

    /**
     * Writes all bytes of {@code b}, delegating to {@link #write(byte[], int, int)}.
     * This guarantees the position counter is maintained through a single code path.
     */
    @Override
    public void write(byte[] b) throws IOException {
        Objects.requireNonNull(b, "b");
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        Objects.requireNonNull(b, "b");
        Objects.checkFromIndexSize(off, len, b.length);
        out.write(b, off, len);
        position += len;
    }

    /**
     * Returns the total number of bytes written through this stream since construction.
     */
    public long getPosition() {
        return position;
    }
}
