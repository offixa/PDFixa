package io.offixa.pdfixa;

import java.nio.charset.StandardCharsets;

/**
 * Byte-level utilities for parsing raw PDF byte arrays in tests.
 * No regex, no PDF libraries — pure byte scanning with US-ASCII semantics.
 */
public final class ByteUtil {

    private ByteUtil() {}

    /**
     * Returns the first index >= {@code start} where {@code needle} appears in {@code data},
     * or -1 if not found.
     */
    public static int indexOf(byte[] data, byte[] needle, int start) {
        if (needle.length == 0) return start;
        outer:
        for (int i = start; i <= data.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    /**
     * Returns the last index where {@code needle} appears in {@code data}, or -1 if not found.
     */
    public static int lastIndexOf(byte[] data, byte[] needle) {
        if (needle.length == 0) return data.length;
        for (int i = data.length - needle.length; i >= 0; i--) {
            boolean found = true;
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return i;
        }
        return -1;
    }

    /**
     * Returns the bytes from {@code start} up to (not including) the next {@code '\n'}.
     * Returns {@code null} if no newline is found at or after {@code start}.
     * The returned array never includes the newline itself.
     */
    public static byte[] readLine(byte[] data, int start) {
        int end = start;
        while (end < data.length && data[end] != '\n') end++;
        if (end >= data.length) return null;
        byte[] line = new byte[end - start];
        System.arraycopy(data, start, line, 0, end - start);
        return line;
    }

    /**
     * Parses ASCII decimal digits in {@code data[start..end)} as a {@code long}.
     * Throws {@link NumberFormatException} if the range is empty or contains non-digit bytes.
     */
    public static long parseAsciiLong(byte[] data, int start, int end) {
        if (start >= end) {
            throw new NumberFormatException("Empty range [" + start + ", " + end + ")");
        }
        long result = 0;
        for (int i = start; i < end; i++) {
            int digit = data[i] - '0';
            if (digit < 0 || digit > 9) {
                throw new NumberFormatException(
                        "Non-digit '" + (char) data[i] + "' at index " + i);
            }
            result = result * 10 + digit;
        }
        return result;
    }

    /** Convenience: convert a US-ASCII string literal to a byte array. */
    public static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }
}
