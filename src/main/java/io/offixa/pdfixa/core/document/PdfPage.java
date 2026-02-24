package io.offixa.pdfixa.core.document;

import io.offixa.pdfixa.core.content.ContentStream;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents a single page in a {@link PdfDocument}.
 *
 * <p>Wraps the {@link ContentStream} for the page and records the indirect
 * object numbers allocated for the Page dictionary and its Contents stream.
 * Instances are created exclusively by {@link PdfDocument#addPage()}.
 *
 * <p>This class is not thread-safe.
 */
public final class PdfPage {

    private final ContentStream content;
    private final int pageObjNum;
    private final int contentsObjNum;

    /** Ordered set of image XObjects placed on this page via {@link #drawImage}. */
    private final Set<PdfImage> usedImages = new LinkedHashSet<>();

    PdfPage(int pageObjNum, int contentsObjNum, FontRegistry fontRegistry) {
        this.pageObjNum     = pageObjNum;
        this.contentsObjNum = contentsObjNum;
        this.content        = new ContentStream(fontRegistry);
    }

    /**
     * Returns the {@link ContentStream} for this page.
     * Use the returned instance to append drawing operators before calling
     * {@link PdfDocument#save}.
     */
    public ContentStream getContent() {
        return content;
    }

    /**
     * Places {@code img} on this page at position ({@code x}, {@code y}) with
     * dimensions {@code w}×{@code h} in user-space units.
     *
     * <p>Emits the canonical image-drawing sequence into the page content stream:
     * <pre>
     *   q
     *   w 0 0 h x y cm
     *   /Im1 Do
     *   Q
     * </pre>
     *
     * @param img the image handle returned by {@link PdfDocument#addJpegImage}
     * @param x   left edge of the image in user units (origin at bottom-left)
     * @param y   bottom edge of the image in user units
     * @param w   rendered width in user units
     * @param h   rendered height in user units
     */
    public void drawImage(PdfImage img, double x, double y, double w, double h) {
        content.saveState();
        content.concatMatrix(w, 0, 0, h, x, y);
        content.doXObject(img.getAlias());
        content.restoreState();
        usedImages.add(img);
    }

    int getPageObjNum() {
        return pageObjNum;
    }

    int getContentsObjNum() {
        return contentsObjNum;
    }

    /**
     * Returns the ordered set of font aliases actually used by this page's
     * content stream.  Used by {@link PdfDocument} to build the per-page
     * {@code /Resources} dictionary at save time.
     */
    Set<String> getUsedFontAliases() {
        return content.getUsedFontAliases();
    }

    /**
     * Returns the ordered set of image XObjects placed on this page.
     * Used by {@link PdfDocument} to build the per-page {@code /XObject}
     * resource sub-dictionary at save time.
     */
    Set<PdfImage> getUsedImages() {
        return Collections.unmodifiableSet(usedImages);
    }
}
