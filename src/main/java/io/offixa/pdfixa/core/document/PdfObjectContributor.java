package io.offixa.pdfixa.core.document;

/**
 * Extension point for external modules to register indirect PDF objects
 * without accessing internal packages directly.
 *
 * <p>Implementations are registered via
 * {@link PdfDocument#registerContributor(PdfObjectContributor)} and invoked
 * during {@link PdfDocument#save} — before page resource wiring — in
 * registration order.
 */
public interface PdfObjectContributor {
    void contribute(PdfDocumentContext context);
}
