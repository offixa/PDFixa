package io.offixa.pdfixa.core.internal;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Minimal PNG parser that extracts image metadata and raw IDAT data from a
 * PNG byte array.
 *
 * <p><strong>Supported subset:</strong>
 * <ul>
 *   <li>8-bit depth</li>
 *   <li>Color type 2 (truecolor RGB)</li>
 *   <li>No alpha, no interlace, no palette</li>
 *   <li>Compression method 0, filter method 0</li>
 *   <li>Single IHDR, one or more IDAT chunks (concatenated)</li>
 * </ul>
 *
 * <p>All other PNG variants cause {@link IllegalArgumentException} to be thrown.
 *
 * <p>IDAT bytes are returned verbatim (zlib-compressed, with PNG filter bytes
 * intact). Callers must not inflate them; they are written directly as the PDF
 * stream body alongside {@code /Filter /FlateDecode} and {@code /DecodeParms}
 * {@code /Predictor 15}.
 *
 * <p>This class is not thread-safe.
 */
public final class PngParser {

    private static final byte[] PNG_SIGNATURE = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    public final int    width;
    public final int    height;
    public final byte[] idatData;

    private PngParser(int width, int height, byte[] idatData) {
        this.width    = width;
        this.height   = height;
        this.idatData = idatData;
    }

    /**
     * Parses {@code pngBytes} and returns a {@link PngParser} carrying the
     * extracted metadata and concatenated IDAT payload.
     *
     * @param pngBytes raw PNG file bytes; must not be {@code null}
     * @throws IllegalArgumentException if the bytes are not a supported PNG
     */
    public static PngParser parse(byte[] pngBytes) {
        Objects.requireNonNull(pngBytes, "pngBytes");

        if (pngBytes.length < 8) {
            throw new IllegalArgumentException("Not a PNG: file too short");
        }
        for (int i = 0; i < 8; i++) {
            if (pngBytes[i] != PNG_SIGNATURE[i]) {
                throw new IllegalArgumentException("Not a PNG: invalid signature");
            }
        }

        int pos = 8;
        int width = -1, height = -1;
        boolean ihdrSeen = false;
        ByteArrayOutputStream idatStream = new ByteArrayOutputStream();

        while (pos + 8 <= pngBytes.length) {
            int chunkLength = readInt(pngBytes, pos);
            if (chunkLength < 0) {
                throw new IllegalArgumentException(
                        "Invalid PNG chunk length at offset " + pos);
            }
            pos += 4;

            String chunkType = new String(pngBytes, pos, 4, StandardCharsets.US_ASCII);
            pos += 4;

            if (pos + chunkLength + 4 > pngBytes.length) {
                throw new IllegalArgumentException(
                        "Truncated PNG chunk: " + chunkType);
            }

            switch (chunkType) {
                case "IHDR" -> {
                    if (ihdrSeen) {
                        throw new IllegalArgumentException("Multiple IHDR chunks found");
                    }
                    if (chunkLength < 13) {
                        throw new IllegalArgumentException("IHDR chunk too short");
                    }
                    ihdrSeen = true;
                    width  = readInt(pngBytes, pos);
                    height = readInt(pngBytes, pos + 4);
                    int bitDepth          = pngBytes[pos + 8]  & 0xFF;
                    int colorType         = pngBytes[pos + 9]  & 0xFF;
                    int compressionMethod = pngBytes[pos + 10] & 0xFF;
                    int filterMethod      = pngBytes[pos + 11] & 0xFF;
                    int interlaceMethod   = pngBytes[pos + 12] & 0xFF;

                    if (bitDepth != 8) {
                        throw new IllegalArgumentException(
                                "Only 8-bit depth is supported, got: " + bitDepth);
                    }
                    if (colorType != 2) {
                        throw new IllegalArgumentException(
                                "Only color type 2 (RGB truecolor) is supported, got: " + colorType);
                    }
                    if (compressionMethod != 0) {
                        throw new IllegalArgumentException(
                                "Unknown PNG compression method: " + compressionMethod);
                    }
                    if (filterMethod != 0) {
                        throw new IllegalArgumentException(
                                "Unknown PNG filter method: " + filterMethod);
                    }
                    if (interlaceMethod != 0) {
                        throw new IllegalArgumentException(
                                "Interlaced PNG is not supported");
                    }
                }
                case "IDAT" -> idatStream.write(pngBytes, pos, chunkLength);
                case "IEND" -> {
                    pos += chunkLength + 4;
                    // stop processing; remaining bytes are irrelevant
                    pos = pngBytes.length;
                    continue;
                }
                default -> { /* ancillary chunks are silently skipped */ }
            }

            pos += chunkLength + 4; // skip data + CRC
        }

        if (!ihdrSeen) {
            throw new IllegalArgumentException("No IHDR chunk found in PNG");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Invalid PNG image dimensions: " + width + "x" + height);
        }

        byte[] idat = idatStream.toByteArray();
        if (idat.length == 0) {
            throw new IllegalArgumentException("No IDAT data found in PNG");
        }

        return new PngParser(width, height, idat);
    }

    private static int readInt(byte[] data, int offset) {
        return ((data[offset]     & 0xFF) << 24)
             | ((data[offset + 1] & 0xFF) << 16)
             | ((data[offset + 2] & 0xFF) << 8)
             |  (data[offset + 3] & 0xFF);
    }
}
