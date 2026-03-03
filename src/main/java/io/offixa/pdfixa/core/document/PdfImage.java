package io.offixa.pdfixa.core.document;

/**
 * Opaque handle for a JPEG image embedded in a {@link PdfDocument} as a PDF image XObject.
 *
 * <p>Instances are created exclusively by {@link PdfDocument#addJpegImage}.
 * The handle carries the alias ({@code Im1}, {@code Im2}, …) and the
 * indirect-object number needed when building page resource dictionaries.
 *
 * <p>This class is not thread-safe.
 */
public final class PdfImage {

    private final String alias;
    private final int objectNumber;
    private final int width;
    private final int height;

    PdfImage(String alias, int objectNumber, int width, int height) {
        this.alias        = alias;
        this.objectNumber = objectNumber;
        this.width        = width;
        this.height       = height;
    }

    /** Returns the resource alias used in content streams, e.g. {@code "Im1"}. */
    String getAlias() {
        return alias;
    }

    /** Returns the indirect-object number for this image's XObject stream. */
    int getObjectNumber() {
        return objectNumber;
    }

    /** Returns the image width in pixels as declared at embedding time. */
    public int getWidth() {
        return width;
    }

    /** Returns the image height in pixels as declared at embedding time. */
    public int getHeight() {
        return height;
    }
}
