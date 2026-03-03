package io.offixa.pdfixa.core.document;

import io.offixa.pdfixa.core.internal.ObjectRegistry;
import io.offixa.pdfixa.core.internal.PdfWriter;
import io.offixa.pdfixa.core.internal.PngParser;
import io.offixa.pdfixa.core.internal.TrailerBuilder;
import io.offixa.pdfixa.core.internal.XrefTableBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.Deflater;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    private static final boolean PROFILING = Boolean.getBoolean("pdfixa.profiling");

    private static final byte[] HEADER_LINE1 =
            "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] HEADER_LINE2 =
            new byte[]{'%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'};
    private static final String PRODUCER_VALUE = "PDFixa";

    private final ObjectRegistry registry;
    private final FontRegistry fontRegistry;
    private final ImageRegistry imageRegistry;
    private final PdfPageSize pageSize;
    private final int catalogNum;
    private final int pagesNum;
    private final List<PdfPage> pages = new ArrayList<>();
    private final List<PdfObjectContributor> contributors = new ArrayList<>();
    private PdfInfo info;
    private boolean saved;

    /**
     * Creates a new, empty A4 document.
     * Equivalent to {@code new PdfDocument(PdfPageSize.A4)}.
     */
    public PdfDocument() {
        this(PdfPageSize.A4);
    }

    /**
     * Creates a new, empty document with the given page size.
     * Allocates the Catalog and Pages objects in the internal registry
     * but does not write any bytes.
     *
     * @param pageSize the page dimensions for every page; must not be {@code null}
     */
    public PdfDocument(PdfPageSize pageSize) {
        Objects.requireNonNull(pageSize, "pageSize");
        this.pageSize  = pageSize;
        registry       = new ObjectRegistry();
        fontRegistry   = new FontRegistry();
        imageRegistry  = new ImageRegistry();
        catalogNum     = registry.allocate(); // always 1
        pagesNum       = registry.allocate(); // always 2
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
     * Registers a contributor that will be invoked during {@link #save} to
     * inject additional indirect objects into the document.
     *
     * <p>Contributors are executed in registration order, before page
     * resource wiring begins.
     *
     * @param contributor the contributor to register; must not be {@code null}
     */
    public void registerContributor(PdfObjectContributor contributor) {
        Objects.requireNonNull(contributor, "contributor");
        if (saved) {
            throw new IllegalStateException("document has already been saved");
        }
        contributors.add(contributor);
    }

    /**
     * Sets document metadata to be written into the PDF {@code /Info} dictionary.
     *
     * @param info info value object with optional metadata fields
     * @throws IllegalStateException if the document has already been saved
     */
    public void setInfo(PdfInfo info) {
        Objects.requireNonNull(info, "info");
        if (saved) {
            throw new IllegalStateException("document has already been saved");
        }
        this.info = info;
    }

    /**
     * Serializes the complete document to {@code out}.
     *
     * <p>The stream is not closed by this method; the caller is responsible
     * for closing {@code out}. This method can be called only once.
     * Once saving begins, subsequent calls fail even if an I/O error occurs.
     *
     * @param out destination stream
     * @throws IOException              if an I/O error occurs
     * @throws IllegalStateException    if no pages were added or the document was already saved
     */
    public void save(OutputStream out) throws IOException {
        Objects.requireNonNull(out, "out");
        if (saved) {
            throw new IllegalStateException("document has already been saved");
        }
        if (pages.isEmpty()) {
            throw new IllegalStateException("document has no pages");
        }
        saved = true;

        for (PdfPage page : pages) {
            page.getContent().seal();
        }

        long t = PROFILING ? System.nanoTime() : 0;

        if (!contributors.isEmpty()) {
            PdfDocumentContext ctx = new PdfDocumentContext(registry, fontRegistry);
            for (PdfObjectContributor c : contributors) {
                c.contribute(ctx);
            }
        }

        long tContribute = 0;
        if (PROFILING) { tContribute = System.nanoTime() - t; t = System.nanoTime(); }

        MessageDigest digest = sha256Digest();
        DigestOutputStream digestOut = new DigestOutputStream(out, digest);
        PdfWriter writer = new PdfWriter(digestOut);
        writer.writeBytes(HEADER_LINE1);
        writer.writeBytes(HEADER_LINE2);

        int infoObjNum = wireBodies();

        long tWireBodies = 0;
        if (PROFILING) { tWireBodies = System.nanoTime() - t; t = System.nanoTime(); }

        registry.writeAll(writer);

        long tWriteAll = 0;
        if (PROFILING) { tWriteAll = System.nanoTime() - t; t = System.nanoTime(); }

        long startxref = XrefTableBuilder.write(
                writer, registry.getOffsets(), registry.getObjectCount());

        writer.flush();
        byte[] fileId = Arrays.copyOf(digest.digest(), 16);

        TrailerBuilder.write(
                writer,
                registry.getObjectCount() + 1,
                registry.getRootObjectNumber(),
                infoObjNum,
                fileId,
                startxref);

        writer.finish();

        if (PROFILING) {
            long tXrefTrailer = System.nanoTime() - t;
            long total = tContribute + tWireBodies + tWriteAll + tXrefTrailer;
            System.err.printf("[pdfixa-profile] save() breakdown:%n");
            System.err.printf("[pdfixa-profile]   contribute:          %8.3f ms%n", tContribute / 1e6);
            System.err.printf("[pdfixa-profile]   wireBodies+compress: %8.3f ms%n", tWireBodies / 1e6);
            System.err.printf("[pdfixa-profile]   registry.writeAll:   %8.3f ms%n", tWriteAll / 1e6);
            System.err.printf("[pdfixa-profile]   xref+trailer+finish: %8.3f ms%n", tXrefTrailer / 1e6);
            System.err.printf("[pdfixa-profile]   TOTAL:               %8.3f ms%n", total / 1e6);
        }

        registry.clearPostSaveState();
        pages.clear();
    }

    // ── Private wiring ────────────────────────────────────────────────────

    private int wireBodies() {
        int infoObjNum = wireInfo();
        wireCatalog();
        wirePages();
        for (PdfPage page : pages) {
            wirePage(page);
            wireContents(page);
        }
        registry.setRoot(catalogNum);
        return infoObjNum;
    }

    private int wireInfo() {
        int infoObjNum = registry.allocate();
        PdfInfo snapshot = info;
        registry.setBody(infoObjNum, w -> {
            w.beginDictionary();
            boolean first = true;
            first = writeInfoEntry(w, "Producer", PRODUCER_VALUE, first);
            if (snapshot != null) {
                first = writeInfoEntry(w, "Title", snapshot.getTitle(), first);
                first = writeInfoEntry(w, "Author", snapshot.getAuthor(), first);
                first = writeInfoEntry(w, "Subject", snapshot.getSubject(), first);
                first = writeInfoEntry(w, "Keywords", snapshot.getKeywords(), first);
                first = writeInfoEntry(w, "Creator", snapshot.getCreator(), first);
                first = writeInfoEntry(w, "CreationDate", snapshot.getCreationDate(), first);
                first = writeInfoEntry(w, "ModDate", snapshot.getModDate(), first);
            }
            w.endDictionary();
        });
        return infoObjNum;
    }

    private static boolean writeInfoEntry(PdfWriter writer, String key, String value, boolean first)
            throws IOException {
        if (value == null) {
            return first;
        }
        if (!first) {
            writer.writeSpace();
        }
        writer.writeName(key);
        writer.writeSpace();
        writer.writeLiteralString(value);
        return false;
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
            w.writeInt(0);                      w.writeSpace();
            w.writeInt(0);                      w.writeSpace();
            w.writeInt(pageSize.getWidthPt());  w.writeSpace();
            w.writeInt(pageSize.getHeightPt());
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
                        if (fontRegistry.isIndirect(alias)) {
                            w.writeName(alias); w.writeSpace();
                            w.writeReference(fontRegistry.getIndirectObjectNumber(alias), 0);
                        } else {
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
     *
     * <p>Uses a direct byte array instead of BAOS to avoid the double-copy
     * overhead of {@code ByteArrayOutputStream.toByteArray()}.
     * Initial capacity is set to half the input size (text typically compresses
     * well), growing on demand if the data is less compressible.
     */
    private static byte[] compress(byte[] input) {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, false);
        try {
            deflater.setInput(input);
            deflater.finish();
            int capacity = Math.max(128, input.length / 2);
            byte[] out = new byte[capacity];
            int pos = 0;
            while (!deflater.finished()) {
                if (pos == out.length) {
                    out = Arrays.copyOf(out, out.length + (out.length >>> 1));
                }
                pos += deflater.deflate(out, pos, out.length - pos);
            }
            return (pos == out.length) ? out : Arrays.copyOf(out, pos);
        } finally {
            deflater.end();
        }
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
