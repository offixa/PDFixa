package io.offixa.pdfixa.core.text;

/**
 * Computes text width in PDF points for a specific font and size.
 */
public interface FontMetrics {

    /**
     * Computes:
     * {@code widthPt = sum(glyphWidth1000) * fontSize / 1000.0}.
     *
     * @param text text to measure
     * @param fontName Base-14 font canonical name
     * @param fontSize font size in points; must be {@code > 0}
     * @return width in points
     */
    double textWidthPt(String text, String fontName, double fontSize);
}
