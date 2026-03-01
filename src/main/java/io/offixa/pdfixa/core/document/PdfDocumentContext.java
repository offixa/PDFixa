package io.offixa.pdfixa.core.document;

import io.offixa.pdfixa.core.internal.ObjectRegistry;

import java.util.Objects;

/**
 * Facade provided to {@link PdfObjectContributor} implementations during
 * {@link PdfDocument#save}.
 *
 * <p>Exposes only the operations a contributor needs — object allocation,
 * body assignment, and indirect font registration — without leaking the
 * {@link ObjectRegistry} type or any other internal infrastructure.
 *
 * <p>Instances are created exclusively by {@link PdfDocument}; the
 * constructor is package-private.
 */
public final class PdfDocumentContext {

    private final ObjectRegistry registry;
    private final FontRegistry fontRegistry;

    PdfDocumentContext(ObjectRegistry registry, FontRegistry fontRegistry) {
        this.registry = registry;
        this.fontRegistry = fontRegistry;
    }

    /**
     * Allocates the next indirect object number.
     *
     * @return the newly allocated object number
     */
    public int allocateObject() {
        return registry.allocate();
    }

    /**
     * Allocates the next indirect object number and immediately assigns its body.
     *
     * <p>Equivalent to calling {@link #allocateObject()} followed by
     * {@link #setObjectBody(int, PdfObjectWriter)}, but in a single step.
     *
     * @param writer non-null callback that writes the object body
     * @return the newly allocated object number
     */
    public int allocateObject(PdfObjectWriter writer) {
        Objects.requireNonNull(writer, "writer");
        int objNum = registry.allocate();
        registry.setBody(objNum, pdfWriter -> writer.write(new PdfObjectOutputAdapter(pdfWriter)));
        return objNum;
    }

    /**
     * Assigns the serialization body for a previously allocated object.
     *
     * <p>The supplied {@link PdfObjectWriter} receives a {@link PdfObjectOutput}
     * facade that hides all internal types. Whitespace between tokens is
     * managed automatically.
     *
     * @param objNum object number returned by {@link #allocateObject()}
     * @param writer non-null callback that writes the object body
     */
    public void setObjectBody(int objNum, PdfObjectWriter writer) {
        Objects.requireNonNull(writer, "writer");
        registry.setBody(objNum, pdfWriter -> {
            writer.write(new PdfObjectOutputAdapter(pdfWriter));
        });
    }

    /**
     * Registers an indirect font whose definition lives in a separate
     * indirect object (allocated via {@link #allocateObject()}).
     *
     * @param fontName     logical font name
     * @param objectNumber the indirect object number holding the font definition
     * @return the alias assigned (e.g. {@code "F3"})
     */
    public String registerIndirectFont(String fontName, int objectNumber) {
        return fontRegistry.registerIndirectFont(fontName, objectNumber);
    }
}
