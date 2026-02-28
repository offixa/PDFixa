package io.offixa.pdfixa.core.writer;

import io.offixa.pdfixa.core.internal.PdfWriter;

import java.io.IOException;

/**
 * A functional interface for objects that know how to serialize themselves
 * into a {@link PdfWriter} stream.
 *
 * <p>Implementations write exactly the body tokens of an indirect object —
 * no {@code obj}/{@code endobj} wrappers; those are managed by the caller.
 */
@FunctionalInterface
public interface PdfSerializable {
    void writeTo(PdfWriter writer) throws IOException;
}
