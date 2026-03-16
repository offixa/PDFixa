package io.offixa.pdfixa.core.text;

/**
 * Minimal Unicode-to-WinAnsi encoder for Base-14 text output.
 *
 * <p>This implementation intentionally supports only BMP characters in the
 * U+0000..U+00FF range. Characters in U+0080..U+009F are rejected because
 * WinAnsi (Windows-1252) maps those bytes to non-Latin-1 code points.
 */
public final class WinAnsiEncoding {

    private static final int[] ENCODE_TABLE = buildEncodeTable();

    private WinAnsiEncoding() {
    }

    /**
     * Encodes one Unicode character into a single WinAnsi byte.
     *
     * @param ch character to encode
     * @return encoded WinAnsi byte
     * @throws IllegalArgumentException if the character is not encodable
     */
    public static byte encode(char ch) {
        if (ch > 0xFF) {
            throw cannotEncode(ch);
        }

        int cp = ch & 0xFF;
        int encoded = ENCODE_TABLE[cp];
        if (encoded < 0) {
            throw cannotEncode(ch);
        }
        return (byte) encoded;
    }

    private static IllegalArgumentException cannotEncode(char ch) {
        return new IllegalArgumentException(
                "Character U+" + Integer.toHexString(ch).toUpperCase() + " cannot be encoded in WinAnsi");
    }

    private static int[] buildEncodeTable() {
        int[] table = new int[256];
        for (int i = 0; i < table.length; i++) {
            table[i] = -1;
        }
        for (int i = 0; i <= 0x7F; i++) {
            table[i] = i;
        }
        for (int i = 0xA0; i <= 0xFF; i++) {
            table[i] = i;
        }
        return table;
    }
}
