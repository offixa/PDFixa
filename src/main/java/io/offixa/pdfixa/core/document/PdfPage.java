package io.offixa.pdfixa.core.document;

import io.offixa.pdfixa.core.content.ContentStream;

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
}
