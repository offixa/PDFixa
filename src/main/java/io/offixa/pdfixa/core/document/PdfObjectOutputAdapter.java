package io.offixa.pdfixa.core.document;

import io.offixa.pdfixa.core.internal.PdfWriter;

import java.io.IOException;

/**
 * Package-private bridge between the public {@link PdfObjectOutput} contract
 * and the internal {@link PdfWriter}.
 *
 * <p>Token-separating whitespace is inserted automatically: a single space is
 * written before every "regular" token (name, string, number, integer,
 * reference) when the preceding token was also a regular token or a closing
 * delimiter.  No space is emitted immediately after {@code <<}.
 */
final class PdfObjectOutputAdapter implements PdfObjectOutput {

    private final PdfWriter w;
    private boolean needsSpace;

    PdfObjectOutputAdapter(PdfWriter w) {
        this.w = w;
    }

    private void sep() throws IOException {
        if (needsSpace) {
            w.writeSpace();
        }
    }

    @Override
    public void name(String name) throws IOException {
        sep();
        w.writeName(name);
        needsSpace = true;
    }

    @Override
    public void string(String value) throws IOException {
        sep();
        w.writeLiteralString(value);
        needsSpace = true;
    }

    @Override
    public void number(double value) throws IOException {
        sep();
        w.writeReal(value);
        needsSpace = true;
    }

    @Override
    public void integer(int value) throws IOException {
        sep();
        w.writeInt(value);
        needsSpace = true;
    }

    @Override
    public void reference(int objNum) throws IOException {
        sep();
        w.writeReference(objNum, 0);
        needsSpace = true;
    }

    @Override
    public void beginDictionary() throws IOException {
        sep();
        w.beginDictionary();
        needsSpace = false;
    }

    @Override
    public void endDictionary() throws IOException {
        w.endDictionary();
        needsSpace = true;
    }

    @Override
    public void beginArray() throws IOException {
        sep();
        w.beginArray();
        needsSpace = false;
    }

    @Override
    public void endArray() throws IOException {
        w.endArray();
        needsSpace = true;
    }

    @Override
    public void beginStream(byte[] data) throws IOException {
        w.writeStream(data);
        needsSpace = false;
    }

    @Override
    public void beginFontFileStream(byte[] ttfBytes) throws IOException {
        w.writeFontFileStream(ttfBytes);
        needsSpace = false;
    }
}
