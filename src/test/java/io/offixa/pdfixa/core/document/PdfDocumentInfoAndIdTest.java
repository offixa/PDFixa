package io.offixa.pdfixa.core.document;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class PdfDocumentInfoAndIdTest {

    private static final Pattern INFO_REF_PATTERN =
            Pattern.compile("/Info\\s+(\\d+)\\s+0\\s+R");
    private static final Pattern ID_PATTERN =
            Pattern.compile("/ID\\s*\\[\\s*<([0-9A-F]+)>\\s*<([0-9A-F]+)>\\s*\\]");

    @Test
    void info_dictionary_always_has_producer() throws IOException {
        byte[] pdfBytes = buildMinimalDocumentBytes(null);
        String pdf = new String(pdfBytes, StandardCharsets.US_ASCII);

        int infoObjNum = extractInfoObjectNumber(pdf);
        String infoBody = extractObjectBody(pdf, infoObjNum);

        assertTrue(infoBody.contains("/Producer (PDFixa)"),
                "/Info dictionary must always contain /Producer (PDFixa)");
    }

    @Test
    void info_dictionary_writes_optional_fields_when_present() throws IOException {
        PdfInfo info = PdfInfo.builder()
                .title("Title")
                .author("Author")
                .subject("Subject")
                .keywords("k1,k2")
                .creator("Creator")
                .creationDate("D:20260301010101Z")
                .modDate("D:20260301010101Z")
                .build();

        byte[] pdfBytes = buildMinimalDocumentBytes(info);
        String pdf = new String(pdfBytes, StandardCharsets.US_ASCII);

        int infoObjNum = extractInfoObjectNumber(pdf);
        String infoBody = extractObjectBody(pdf, infoObjNum);

        assertTrue(infoBody.contains("/Producer (PDFixa)"));
        assertTrue(infoBody.contains("/Title (Title)"));
        assertTrue(infoBody.contains("/Author (Author)"));
        assertTrue(infoBody.contains("/Subject (Subject)"));
        assertTrue(infoBody.contains("/Keywords (k1,k2)"));
        assertTrue(infoBody.contains("/Creator (Creator)"));
        assertTrue(infoBody.contains("/CreationDate (D:20260301010101Z)"));
        assertTrue(infoBody.contains("/ModDate (D:20260301010101Z)"));
    }

    @Test
    void setInfo_after_save_throws() throws IOException {
        PdfDocument doc = minimalDoc();
        doc.save(new ByteArrayOutputStream());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> doc.setInfo(PdfInfo.builder().title("late").build())
        );
        assertTrue(ex.getMessage().contains("already been saved"));
    }

    @Test
    void trailer_contains_deterministic_id_pair() throws IOException {
        byte[] pdfBytes = buildMinimalDocumentBytes(PdfInfo.builder().title("A").build());
        String pdf = new String(pdfBytes, StandardCharsets.US_ASCII);

        Matcher matcher = ID_PATTERN.matcher(pdf);
        assertTrue(matcher.find(), "Trailer must contain /ID [<hex> <hex>]");

        String first = matcher.group(1);
        String second = matcher.group(2);

        assertEquals(first, second, "Both /ID entries must be identical");
        assertEquals(32, first.length(), "16-byte file id must be encoded as 32 hex chars");
    }

    @Test
    void same_input_produces_identical_bytes_including_id() throws IOException {
        PdfInfo info = PdfInfo.builder()
                .title("Deterministic")
                .author("PDFixa")
                .build();

        byte[] first = buildMinimalDocumentBytes(info);
        byte[] second = buildMinimalDocumentBytes(info);

        assertArrayEquals(first, second,
                "Same document input must produce identical bytes, including trailer /ID");
    }

    private static byte[] buildMinimalDocumentBytes(PdfInfo info) throws IOException {
        PdfDocument doc = minimalDoc();
        if (info != null) {
            doc.setInfo(info);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        return baos.toByteArray();
    }

    private static PdfDocument minimalDoc() {
        PdfDocument doc = new PdfDocument();
        PdfPage page = doc.addPage();
        page.getContent()
                .beginText()
                .setFont("Helvetica", 12)
                .moveText(100, 700)
                .showText("Hello PDFixa")
                .endText();
        return doc;
    }

    private static int extractInfoObjectNumber(String pdf) {
        Matcher infoMatcher = INFO_REF_PATTERN.matcher(pdf);
        assertTrue(infoMatcher.find(), "Trailer dictionary must contain /Info reference");
        return Integer.parseInt(infoMatcher.group(1));
    }

    private static String extractObjectBody(String pdf, int objNum) {
        String header = objNum + " 0 obj\n";
        int start = pdf.indexOf(header);
        assertNotEquals(-1, start, "Object " + objNum + " header not found");
        int bodyStart = start + header.length();
        int end = pdf.indexOf("endobj\n", bodyStart);
        assertNotEquals(-1, end, "endobj for object " + objNum + " not found");
        return pdf.substring(bodyStart, end);
    }
}
