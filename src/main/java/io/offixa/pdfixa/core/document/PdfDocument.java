package io.offixa.pdfixa.core.document;

import io.offixa.pdfixa.core.writer.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.Deflater;

/**
 * High-level facade for building a PDF document.
 *
 * <p>Hides all low-level object wiring (Catalog, Pages, Page, Contents) and
 * exposes a simple two-method API:
 * <pre>
 *   PdfDocument doc = new PdfDocument();
 *   PdfPage page = doc.addPage();
 *   page.getContent().beginText() ...
 *   doc.save(outputStream);
 * </pre>
 *
 * <p>Object numbers are allocated eagerly in {@link #PdfDocument()} and
 * {@link #addPage()}; no PDF bytes are written until {@link #save} is called.
 *
 * <p>This class is not thread-safe.
 */
public final class PdfDocument {

    private static final byte[] HEADER_LINE1 =
            "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] HEADER_LINE2 =
            new byte[]{'%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'};

    private final ObjectRegistry registry;
    private final FontRegistry fontRegistry;
    private final ImageRegistry imageRegistry;
    private final int catalogNum;
    private final int pagesNum;
    private final List<PdfPage> pages = new ArrayList<>();

    /**
     * Creates a new, empty document.
     * Allocates the Catalog and Pages objects in the internal registry
     * but does not write any bytes.
     */
    public PdfDocument() {
        registry      = new ObjectRegistry();
        fontRegistry  = new FontRegistry();
        imageRegistry = new ImageRegistry();
        catalogNum    = registry.allocate(); // always 1
        pagesNum      = registry.allocate(); // always 2
    }

    /**
     * Adds a new blank page to the document and returns a {@link PdfPage}
     * whose {@link PdfPage#getContent()} can be used to append drawing operators.
     *
     * <p>Pages appear in the PDF in the order they were added.
     *
     * @return the newly created page wrapper
     */
    public PdfPage addPage() {
        int pageNum     = registry.allocate();
        int contentsNum = registry.allocate();
        PdfPage page    = new PdfPage(pageNum, contentsNum, fontRegistry);
        pages.add(page);
        return page;
    }

    /**
     * Embeds a JPEG image in the document and returns a {@link PdfImage} handle
     * that can be passed to {@link PdfPage#drawImage} on any page.
     *
     * <p>The JPEG bytes are stored as-is using {@code /Filter /DCTDecode} — no
     * transcoding or pixel decoding is performed.  A defensive copy of
     * {@code jpegBytes} is taken to guarantee deterministic output.
     *
     * @param jpegBytes raw JPEG file bytes; must not be {@code null}
     * @param width     image width in pixels (must be &gt; 0)
     * @param height    image height in pixels (must be &gt; 0)
     * @return a handle for placing the image on pages
     */
    public PdfImage addJpegImage(byte[] jpegBytes, int width, int height) {
        Objects.requireNonNull(jpegBytes, "jpegBytes");
        if (width  <= 0) throw new IllegalArgumentException("width must be > 0");
        if (height <= 0) throw new IllegalArgumentException("height must be > 0");

        int imageObjNum = registry.allocate();
        PdfImage img    = imageRegistry.allocate(imageObjNum, width, height);

        byte[] imageData = jpegBytes.clone(); // defensive copy for determinism
        registry.setBody(imageObjNum,
                w -> w.writeJpegImageStream(imageData, width, height));
        return img;
    }

    /**
     * Embeds a PNG image in the document and returns a {@link PdfImage} handle
     * that can be passed to {@link PdfPage#drawImage} on any page.
     *
     * <p>Only 8-bit, non-interlaced, truecolor RGB (color type 2) PNGs are accepted.
     * Alpha channel, indexed-color, grayscale, and interlaced variants all cause
     * {@link IllegalArgumentException} to be thrown.
     *
     * <p>Width and height are extracted automatically from the IHDR chunk.
     * The raw IDAT payload (zlib-compressed, PNG filter bytes intact) is written
     * verbatim as the PDF stream body with {@code /Filter /FlateDecode} and
     * {@code /DecodeParms << /Predictor 15 ... >>}. No re-compression is applied.
     * A defensive copy of {@code pngBytes} is taken to guarantee deterministic output.
     *
     * @param pngBytes raw PNG file bytes; must not be {@code null}
     * @return a handle for placing the image on pages
     * @throws IllegalArgumentException if the PNG is not a supported variant
     */
    public PdfImage addPngImage(byte[] pngBytes) {
        Objects.requireNonNull(pngBytes, "pngBytes");
        PngParser png       = PngParser.parse(pngBytes.clone());
        int       objNum    = registry.allocate();
        PdfImage  img       = imageRegistry.allocate(objNum, png.width, png.height);
        byte[]    idatData  = png.idatData; // already a fresh array from PngParser
        registry.setBody(objNum, w -> w.writePngImageStream(idatData, png.width, png.height));
        return img;
    }

    /**
     * Serializes the complete document to {@code out}.
     *
     * <p>The stream is not closed by this method; the caller is responsible
     * for closing {@code out}.
     *
     * @param out destination stream
     * @throws IOException              if an I/O error occurs
     * @throws IllegalStateException    if no pages were added
     */
    public void save(OutputStream out) throws IOException {
        Objects.requireNonNull(out, "out");
        if (pages.isEmpty()) {
            throw new IllegalStateException("document has no pages");
        }

        try (PdfWriter writer = new PdfWriter(out)) {
            writer.writeBytes(HEADER_LINE1);
            writer.writeBytes(HEADER_LINE2);

            wireBodies();

            registry.writeAll(writer);

            long startxref = XrefTableBuilder.write(
                    writer, registry.getOffsets(), registry.getObjectCount());

            TrailerBuilder.write(
                    writer,
                    registry.getObjectCount() + 1,
                    registry.getRootObjectNumber(),
                    startxref);

            writer.flush();
        }
    }

    // ── Private wiring ────────────────────────────────────────────────────

    private void wireBodies() {
        wireCatalog();
        wirePages();
        for (PdfPage page : pages) {
            wirePage(page);
            wireContents(page);
        }
        registry.setRoot(catalogNum);
    }

    private void wireCatalog() {
        registry.setBody(catalogNum, w -> {
            w.beginDictionary();
            w.writeName("Type");  w.writeSpace(); w.writeName("Catalog");
            w.writeSpace();
            w.writeName("Pages"); w.writeSpace(); w.writeReference(pagesNum, 0);
            w.endDictionary();
        });
    }

    private void wirePages() {
        registry.setBody(pagesNum, w -> {
            w.beginDictionary();
            w.writeName("Type"); w.writeSpace(); w.writeName("Pages");
            w.writeSpace();
            w.writeName("Kids"); w.writeSpace();
            w.beginArray();
            for (int i = 0; i < pages.size(); i++) {
                if (i > 0) w.writeSpace();
                w.writeReference(pages.get(i).getPageObjNum(), 0);
            }
            w.endArray();
            w.writeSpace();
            w.writeName("Count"); w.writeSpace(); w.writeInt(pages.size());
            w.endDictionary();
        });
    }

    private void wirePage(PdfPage page) {
        int pageNum     = page.getPageObjNum();
        int contentsNum = page.getContentsObjNum();
        // Snapshot resource sets now; by save() time content is finalised.
        Set<String>   usedFonts  = page.getUsedFontAliases();
        Set<PdfImage> usedImages = page.getUsedImages();

        registry.setBody(pageNum, w -> {
            w.beginDictionary();
            w.writeName("Type");     w.writeSpace(); w.writeName("Page");
            w.writeSpace();
            w.writeName("Parent");   w.writeSpace(); w.writeReference(pagesNum, 0);
            w.writeSpace();
            w.writeName("MediaBox"); w.writeSpace();
            w.beginArray();
            w.writeInt(0);   w.writeSpace();
            w.writeInt(0);   w.writeSpace();
            w.writeInt(595); w.writeSpace();
            w.writeInt(842);
            w.endArray();
            if (!usedFonts.isEmpty() || !usedImages.isEmpty()) {
                w.writeSpace();
                w.writeName("Resources"); w.writeSpace();
                w.beginDictionary();
                if (!usedFonts.isEmpty()) {
                    w.writeName("Font"); w.writeSpace();
                    w.beginDictionary();
                    boolean firstFont = true;
                    for (String alias : usedFonts) {
                        if (!firstFont) w.writeSpace();
                        firstFont = false;
                        String baseFontName = fontRegistry.getFontName(alias);
                        w.writeName(alias); w.writeSpace();
                        w.beginDictionary();
                        w.writeName("Type");     w.writeSpace(); w.writeName("Font");
                        w.writeSpace();
                        w.writeName("Subtype");  w.writeSpace(); w.writeName("Type1");
                        w.writeSpace();
                        w.writeName("BaseFont"); w.writeSpace(); w.writeName(baseFontName);
                        w.endDictionary();
                    }
                    w.endDictionary();
                }
                if (!usedImages.isEmpty()) {
                    if (!usedFonts.isEmpty()) w.writeSpace();
                    w.writeName("XObject"); w.writeSpace();
                    w.beginDictionary();
                    boolean firstImage = true;
                    for (PdfImage img : usedImages) {
                        if (!firstImage) w.writeSpace();
                        firstImage = false;
                        w.writeName(img.getAlias()); w.writeSpace();
                        w.writeReference(img.getObjectNumber(), 0);
                    }
                    w.endDictionary();
                }
                w.endDictionary();
            }
            w.writeSpace();
            w.writeName("Contents"); w.writeSpace(); w.writeReference(contentsNum, 0);
            w.endDictionary();
        });
    }

    private void wireContents(PdfPage page) {
        byte[] raw        = page.getContent().toBytes();
        byte[] compressed = compress(raw);
        registry.setBody(page.getContentsObjNum(), w -> w.writeCompressedStream(compressed));
    }

    /**
     * Compresses {@code input} using raw zlib (Deflater with {@code nowrap=false}).
     * Output is deterministic: same input always yields the same compressed bytes.
     * No timestamps or metadata are embedded.
     */
    private static byte[] compress(byte[] input) {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, false);
        try {
            deflater.setInput(input);
            deflater.finish();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.max(64, input.length));
            byte[] buf = new byte[4096];
            while (!deflater.finished()) {
                int n = deflater.deflate(buf);
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();
        } finally {
            deflater.end();
        }
    }
}
