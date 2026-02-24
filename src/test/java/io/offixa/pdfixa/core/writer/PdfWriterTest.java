package io.offixa.pdfixa.core.writer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PdfWriterTest {

    private ByteArrayOutputStream baos;
    private PdfWriter writer;

    @BeforeEach
    void setUp() {
        baos = new ByteArrayOutputStream();
        writer = new PdfWriter(baos);
    }

    private String output() {
        return baos.toString(StandardCharsets.US_ASCII);
    }

    // ── Position tracking ──────────────────────────────────────────

    @Test
    void positionStartsAtZero() {
        assertEquals(0L, writer.getPosition());
    }

    @Test
    void positionTracksWrittenBytes() throws IOException {
        writer.writeInt(42);
        assertEquals(2L, writer.getPosition()); // "42" = 2 bytes
    }

    // ── Scalar tokens ──────────────────────────────────────────────

    @Nested
    class IntegerTokens {
        @Test
        void positiveInteger() throws IOException {
            writer.writeInt(42);
            assertEquals("42", output());
        }

        @Test
        void negativeInteger() throws IOException {
            writer.writeInt(-7);
            assertEquals("-7", output());
        }

        @Test
        void zero() throws IOException {
            writer.writeInt(0);
            assertEquals("0", output());
        }

        @Test
        void longValue() throws IOException {
            writer.writeLong(3_000_000_000L);
            assertEquals("3000000000", output());
        }
    }

    @Nested
    class RealTokens {
        @Test
        void integralValueOmitsDecimalPoint() throws IOException {
            writer.writeReal(5.0);
            assertEquals("5", output());
        }

        @Test
        void fractionalValue() throws IOException {
            writer.writeReal(3.14);
            assertEquals("3.14", output());
        }

        @Test
        void trailingZerosStripped() throws IOException {
            writer.writeReal(1.500000);
            assertEquals("1.5", output());
        }

        @Test
        void negativeReal() throws IOException {
            writer.writeReal(-0.25);
            assertEquals("-0.25", output());
        }

        @Test
        void smallFraction() throws IOException {
            writer.writeReal(0.001);
            assertEquals("0.001", output());
        }

        @Test
        void nanIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> writer.writeReal(Double.NaN));
        }

        @Test
        void positiveInfinityIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> writer.writeReal(Double.POSITIVE_INFINITY));
        }

        @Test
        void negativeInfinityIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> writer.writeReal(Double.NEGATIVE_INFINITY));
        }
    }

    @Nested
    class FormatRealUnit {
        @Test
        void integralDouble() {
            assertEquals("7", PdfWriter.formatReal(7.0));
        }

        @Test
        void negativeIntegral() {
            assertEquals("-3", PdfWriter.formatReal(-3.0));
        }

        @Test
        void sixDecimalPlacesMax() {
            assertEquals("0.333333", PdfWriter.formatReal(1.0 / 3.0));
        }

        @Test
        void noExponentialNotation() {
            String result = PdfWriter.formatReal(0.000001);
            assertFalse(result.contains("E"), "PDF reals must not use exponential notation");
            assertFalse(result.contains("e"), "PDF reals must not use exponential notation");
        }
    }

    // ── Name tokens ────────────────────────────────────────────────

    @Nested
    class NameTokens {
        @Test
        void simpleName() throws IOException {
            writer.writeName("Type");
            assertEquals("/Type", output());
        }

        @Test
        void nameWithSpace() throws IOException {
            writer.writeName("My Name");
            assertEquals("/My#20Name", output());
        }

        @Test
        void nameWithHash() throws IOException {
            writer.writeName("A#B");
            assertEquals("/A#23B", output());
        }

        @Test
        void nameWithDelimiters() throws IOException {
            writer.writeName("A/B");
            assertEquals("/A#2FB", output());
        }

        @Test
        void nullNameThrowsNPE() {
            assertThrows(NullPointerException.class, () -> writer.writeName(null));
        }

        @Test
        void nameWithNonLatin1CharThrows() {
            assertThrows(IllegalArgumentException.class, () -> writer.writeName("caf\u0100"));
        }
    }

    // ── String tokens ──────────────────────────────────────────────

    @Nested
    class StringTokens {
        @Test
        void literalStringSimple() throws IOException {
            writer.writeLiteralString("Hello");
            assertEquals("(Hello)", output());
        }

        @Test
        void literalStringEscapesParens() throws IOException {
            writer.writeLiteralString("a(b)c");
            assertEquals("(a\\(b\\)c)", output());
        }

        @Test
        void literalStringEscapesBackslash() throws IOException {
            writer.writeLiteralString("path\\to");
            assertEquals("(path\\\\to)", output());
        }

        @Test
        void literalStringEscapesNewline() throws IOException {
            writer.writeLiteralString("line1\nline2");
            assertEquals("(line1\\nline2)", output());
        }

        @Test
        void literalStringEscapesCarriageReturn() throws IOException {
            writer.writeLiteralString("a\rb");
            assertEquals("(a\\rb)", output());
        }

        @Test
        void literalStringEscapesTab() throws IOException {
            writer.writeLiteralString("a\tb");
            assertEquals("(a\\tb)", output());
        }

        @Test
        void literalStringEscapesBackspace() throws IOException {
            writer.writeLiteralString("a\bb");
            assertEquals("(a\\bb)", output());
        }

        @Test
        void literalStringEscapesFormFeed() throws IOException {
            writer.writeLiteralString("a\fb");
            assertEquals("(a\\fb)", output());
        }

        @Test
        void nullLiteralStringThrowsNPE() {
            assertThrows(NullPointerException.class, () -> writer.writeLiteralString(null));
        }

        @Test
        void literalStringWithNonLatin1CharThrows() {
            assertThrows(IllegalArgumentException.class, () -> writer.writeLiteralString("caf\u0100"));
        }

        @Test
        void hexString() throws IOException {
            writer.writeHexString(new byte[]{(byte) 0xDE, (byte) 0xAD});
            assertEquals("<DEAD>", output());
        }

        @Test
        void hexStringEmpty() throws IOException {
            writer.writeHexString(new byte[0]);
            assertEquals("<>", output());
        }
    }

    // ── writeBytes ─────────────────────────────────────────────────

    @Test
    void nullWriteBytesThrowsNPE() {
        assertThrows(NullPointerException.class, () -> writer.writeBytes(null));
    }

    // ── Boolean and null ───────────────────────────────────────────

    @Test
    void booleanTrue() throws IOException {
        writer.writeBoolean(true);
        assertEquals("true", output());
    }

    @Test
    void booleanFalse() throws IOException {
        writer.writeBoolean(false);
        assertEquals("false", output());
    }

    @Test
    void nullToken() throws IOException {
        writer.writeNull();
        assertEquals("null", output());
    }

    // ── References ─────────────────────────────────────────────────

    @Test
    void indirectReference() throws IOException {
        writer.writeReference(4, 0);
        assertEquals("4 0 R", output());
    }

    // ── Indirect objects ───────────────────────────────────────────

    @Nested
    class IndirectObjects {
        @Test
        void beginObjectReturnsOffset() throws IOException {
            writer.writeBytes("JUNK".getBytes(StandardCharsets.US_ASCII));
            long offset = writer.beginObject(3, 0);
            assertEquals(4L, offset); // "JUNK" = 4 bytes
        }

        @Test
        void objectWrapperSyntax() throws IOException {
            writer.beginObject(1, 0);
            writer.writeInt(42);
            writer.writeNewline();
            writer.endObject();
            assertEquals("1 0 obj\n42\nendobj\n", output());
        }
    }

    // ── Dictionary delimiters ──────────────────────────────────────

    @Test
    void dictionarySyntax() throws IOException {
        writer.beginDictionary();
        writer.writeName("Type");
        writer.writeSpace();
        writer.writeName("Catalog");
        writer.endDictionary();
        assertEquals("<</Type /Catalog>>", output());
    }

    // ── Array delimiters ───────────────────────────────────────────

    @Test
    void arraySyntax() throws IOException {
        writer.beginArray();
        writer.writeInt(1);
        writer.writeSpace();
        writer.writeInt(2);
        writer.writeSpace();
        writer.writeInt(3);
        writer.endArray();
        assertEquals("[1 2 3]", output());
    }

    // ── Comment ────────────────────────────────────────────────────

    @Test
    void comment() throws IOException {
        writer.writeComment("PDF-1.7");
        assertEquals("%PDF-1.7\n", output());
    }

    // ── Whitespace control ─────────────────────────────────────────

    @Test
    void newlineIsOnlyLF() throws IOException {
        writer.writeNewline();
        byte[] bytes = baos.toByteArray();
        assertEquals(1, bytes.length);
        assertEquals((byte) '\n', bytes[0]);
    }

    @Test
    void spaceIsSingleByte() throws IOException {
        writer.writeSpace();
        byte[] bytes = baos.toByteArray();
        assertEquals(1, bytes.length);
        assertEquals((byte) ' ', bytes[0]);
    }

    // ── Determinism: identical calls produce identical bytes ───────

    @Test
    void deterministicOutput() throws IOException {
        byte[] run1 = generateSample();
        byte[] run2 = generateSample();
        assertArrayEquals(run1, run2, "Two identical write sequences must produce identical bytes");
    }

    private byte[] generateSample() throws IOException {
        var buf = new ByteArrayOutputStream();
        try (var w = new PdfWriter(buf)) {
            w.writeComment("PDF-1.7");
            w.beginObject(1, 0);
            w.beginDictionary();
            w.writeName("Type");
            w.writeSpace();
            w.writeName("Catalog");
            w.endDictionary();
            w.writeNewline();
            w.endObject();
        }
        return buf.toByteArray();
    }

    // ── Integrated mini-example ────────────────────────────────────

    @Test
    void miniCatalogObject() throws IOException {
        long offset = writer.beginObject(1, 0);
        writer.beginDictionary();
        writer.writeName("Type");
        writer.writeSpace();
        writer.writeName("Catalog");
        writer.writeName("Pages");
        writer.writeSpace();
        writer.writeReference(2, 0);
        writer.endDictionary();
        writer.writeNewline();
        writer.endObject();

        assertEquals(0L, offset);
        String expected = "1 0 obj\n<</Type /Catalog/Pages 2 0 R>>\nendobj\n";
        assertEquals(expected, output());
        assertEquals(expected.length(), writer.getPosition());
    }
}
