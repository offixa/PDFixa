package io.offixa.pdfixa.core.document;

/**
 * Immutable value object representing a PDF page size in points (1 point = 1/72 inch).
 *
 * <p>Pre-defined constants are provided for common paper sizes.
 * Custom sizes can be created via the public constructor.
 *
 * <p>This class is immutable and thread-safe.
 */
public final class PdfPageSize {

    /** ISO A4: 210 mm × 297 mm (595 × 842 pt). */
    public static final PdfPageSize A4 = new PdfPageSize(595, 842);

    /** ISO A3: 297 mm × 420 mm (842 × 1191 pt). */
    public static final PdfPageSize A3 = new PdfPageSize(842, 1191);

    /** ISO A5: 148 mm × 210 mm (420 × 595 pt). */
    public static final PdfPageSize A5 = new PdfPageSize(420, 595);

    /** US Letter: 8.5 × 11 in (612 × 792 pt). */
    public static final PdfPageSize LETTER = new PdfPageSize(612, 792);

    /** US Legal: 8.5 × 14 in (612 × 1008 pt). */
    public static final PdfPageSize LEGAL = new PdfPageSize(612, 1008);

    private final int widthPt;
    private final int heightPt;

    /**
     * Creates a page size with the given dimensions in points.
     *
     * @param widthPt  page width in points; must be &gt; 0
     * @param heightPt page height in points; must be &gt; 0
     * @throws IllegalArgumentException if either dimension is &le; 0
     */
    public PdfPageSize(int widthPt, int heightPt) {
        if (widthPt <= 0) throw new IllegalArgumentException("widthPt must be > 0, got " + widthPt);
        if (heightPt <= 0) throw new IllegalArgumentException("heightPt must be > 0, got " + heightPt);
        this.widthPt = widthPt;
        this.heightPt = heightPt;
    }

    /** Returns the page width in points. */
    public int getWidthPt() {
        return widthPt;
    }

    /** Returns the page height in points. */
    public int getHeightPt() {
        return heightPt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PdfPageSize that)) return false;
        return widthPt == that.widthPt && heightPt == that.heightPt;
    }

    @Override
    public int hashCode() {
        return 31 * widthPt + heightPt;
    }

    @Override
    public String toString() {
        return "PdfPageSize[" + widthPt + " × " + heightPt + " pt]";
    }
}
