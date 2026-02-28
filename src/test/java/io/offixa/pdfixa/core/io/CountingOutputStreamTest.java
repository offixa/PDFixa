package io.offixa.pdfixa.core.io;

import io.offixa.pdfixa.core.internal.CountingOutputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CountingOutputStreamTest {

    @Test
    void positionStartsAtZero() {
        var cos = new CountingOutputStream(new ByteArrayOutputStream());
        assertEquals(0L, cos.getPosition());
    }

    @Test
    void writeSingleByteIncrementsPosition() throws IOException {
        var cos = new CountingOutputStream(new ByteArrayOutputStream());
        cos.write(0x41);
        assertEquals(1L, cos.getPosition());
        cos.write(0x42);
        assertEquals(2L, cos.getPosition());
    }

    @Test
    void writeByteArrayIncrementsPositionByLength() throws IOException {
        var baos = new ByteArrayOutputStream();
        var cos = new CountingOutputStream(baos);
        byte[] data = {0x01, 0x02, 0x03, 0x04, 0x05};
        cos.write(data);
        assertEquals(5L, cos.getPosition());
        assertArrayEquals(data, baos.toByteArray());
    }

    @Test
    void writeByteArraySliceIncrementsPositionByLen() throws IOException {
        var baos = new ByteArrayOutputStream();
        var cos = new CountingOutputStream(baos);
        byte[] data = {0x0A, 0x0B, 0x0C, 0x0D, 0x0E};
        cos.write(data, 1, 3);
        assertEquals(3L, cos.getPosition());
        assertArrayEquals(new byte[]{0x0B, 0x0C, 0x0D}, baos.toByteArray());
    }

    @Test
    void mixedWritesAccumulateCorrectly() throws IOException {
        var cos = new CountingOutputStream(new ByteArrayOutputStream());
        cos.write(0xFF);                          // 1
        cos.write(new byte[]{1, 2, 3});           // +3 = 4
        cos.write(new byte[]{4, 5, 6, 7}, 0, 2); // +2 = 6
        assertEquals(6L, cos.getPosition());
    }

    @Test
    void delegatesToUnderlyingStreamExactly() throws IOException {
        var baos = new ByteArrayOutputStream();
        var cos = new CountingOutputStream(baos);
        cos.write('H');
        cos.write("ello".getBytes());
        cos.flush();
        assertEquals("Hello", baos.toString("US-ASCII"));
    }

    @Test
    void nullStreamThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new CountingOutputStream(null));
    }

    @Test
    void nullByteArrayThrowsNPE() {
        var cos = new CountingOutputStream(new ByteArrayOutputStream());
        assertThrows(NullPointerException.class, () -> cos.write((byte[]) null));
        assertThrows(NullPointerException.class, () -> cos.write(null, 0, 0));
    }

    @Test
    void invalidBoundsThrowsIndexOutOfBounds() {
        var cos = new CountingOutputStream(new ByteArrayOutputStream());
        byte[] data = {1, 2, 3};
        assertThrows(IndexOutOfBoundsException.class, () -> cos.write(data, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> cos.write(data, 0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> cos.write(data, 2, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> cos.write(data, 0, -1));
    }

    @Test
    void writeByteArrayDelegatesToSliceOverload() throws IOException {
        var baos = new ByteArrayOutputStream();
        var cos = new CountingOutputStream(baos);
        byte[] data = {0x41, 0x42, 0x43};
        cos.write(data);
        assertEquals(3L, cos.getPosition());
        assertArrayEquals(data, baos.toByteArray());
    }
}
