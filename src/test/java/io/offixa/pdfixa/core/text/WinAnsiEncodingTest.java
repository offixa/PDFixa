package io.offixa.pdfixa.core.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WinAnsiEncodingTest {

    @Test
    void encodes_ascii_identity() {
        assertEquals(0x41, WinAnsiEncoding.encode('A') & 0xFF);
    }

    @Test
    void encodes_latin1_identity() {
        assertEquals(0xE9, WinAnsiEncoding.encode('\u00E9') & 0xFF);
        assertEquals(0xFF, WinAnsiEncoding.encode('\u00FF') & 0xFF);
    }

    @Test
    void rejects_undefined_c1_code_points() {
        assertThrows(IllegalArgumentException.class, () -> WinAnsiEncoding.encode('\u0080'));
    }

    @Test
    void rejects_code_points_above_u00ff() {
        assertThrows(IllegalArgumentException.class, () -> WinAnsiEncoding.encode('\u0100'));
    }
}
