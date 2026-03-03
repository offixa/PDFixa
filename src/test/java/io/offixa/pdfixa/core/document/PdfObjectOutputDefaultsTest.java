package io.offixa.pdfixa.core.document;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the forward-compatible default methods on {@link PdfObjectOutput}
 * throw {@link UnsupportedOperationException} with a clear message.
 */
class PdfObjectOutputDefaultsTest {

    private final PdfObjectOutput stub = new MinimalStub();

    @Test
    void rawBytes_throws_unsupported() {
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> stub.rawBytes(new byte[]{1, 2, 3}));
        assertTrue(ex.getMessage().contains("rawBytes"));
    }

    @Test
    void bool_throws_unsupported() {
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> stub.bool(true));
        assertTrue(ex.getMessage().contains("bool"));
    }

    @Test
    void nullValue_throws_unsupported() {
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                stub::nullValue);
        assertTrue(ex.getMessage().contains("nullValue"));
    }

    @Test
    void comment_throws_unsupported() {
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> stub.comment("test"));
        assertTrue(ex.getMessage().contains("comment"));
    }

    /**
     * Minimal implementation that satisfies the required abstract methods
     * but inherits all defaults.
     */
    private static final class MinimalStub implements PdfObjectOutput {
        @Override public void name(String name) throws IOException {}
        @Override public void string(String value) throws IOException {}
        @Override public void number(double value) throws IOException {}
        @Override public void integer(int value) throws IOException {}
        @Override public void reference(int objNum) throws IOException {}
        @Override public void beginDictionary() throws IOException {}
        @Override public void endDictionary() throws IOException {}
        @Override public void beginArray() throws IOException {}
        @Override public void endArray() throws IOException {}
        @Override public void beginStream(byte[] data) throws IOException {}
        @Override public void beginFontFileStream(byte[] ttfBytes) throws IOException {}
    }
}
