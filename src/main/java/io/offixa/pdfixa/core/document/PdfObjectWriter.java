package io.offixa.pdfixa.core.document;

import java.io.IOException;

/**
 * Functional interface for writing the body of an indirect PDF object.
 *
 * <p>External modules pass a {@code PdfObjectWriter} lambda to
 * {@link PdfDocumentContext#setObjectBody}. The lambda receives a
 * {@link PdfObjectOutput} facade that hides all internal types.
 *
 * <p>Example:
 * <pre>
 *   ctx.setObjectBody(objNum, out -&gt; {
 *       out.beginDictionary();
 *       out.name("Type");  out.name("Font");
 *       out.name("Subtype"); out.name("Type1");
 *       out.endDictionary();
 *   });
 * </pre>
 */
@FunctionalInterface
public interface PdfObjectWriter {
    void write(PdfObjectOutput out) throws IOException;
}
