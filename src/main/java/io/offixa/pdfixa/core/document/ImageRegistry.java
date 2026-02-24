package io.offixa.pdfixa.core.document;

/**
 * Document-level registry that assigns stable, deterministic aliases
 * ({@code Im1}, {@code Im2}, …) to embedded JPEG image XObjects.
 *
 * <p>Unlike {@link FontRegistry}, images are never deduplicated — every call
 * to {@link #allocate} produces a fresh alias regardless of content.
 *
 * <p>This class is not thread-safe.
 */
final class ImageRegistry {

    private int nextIndex = 1;

    /**
     * Allocates the next image alias and returns a {@link PdfImage} handle
     * populated with the given metadata.
     *
     * @param objectNumber the indirect-object number pre-allocated for the XObject stream
     * @param width        image width in pixels
     * @param height       image height in pixels
     * @return a new handle with alias {@code Im1}, {@code Im2}, …
     */
    PdfImage allocate(int objectNumber, int width, int height) {
        String alias = "Im" + nextIndex++;
        return new PdfImage(alias, objectNumber, width, height);
    }
}
