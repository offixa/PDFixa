package io.offixa.pdfixa.core.content;

import io.offixa.pdfixa.ByteUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ContentStream}.
 * All assertions use raw byte scanning — no PDF library involved.
 */
class ContentStreamTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Operator output
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void all_operators_produce_expected_sequences() {
        byte[] bytes = new ContentStream()
                .beginText()
                .setFont("F1", 12)
                .moveText(50, 700)
                .showText("Hello")
                .endText()
                .setLineWidth(1.5)
                .moveTo(10, 20)
                .lineTo(100, 20)
                .stroke()
                .rectangle(10, 50, 200, 100)
                .fill()
                .toBytes();

        assertSeq(bytes, "BT\n");
        assertSeq(bytes, "/F1 12 Tf\n");
        assertSeq(bytes, "50 700 Td\n");
        assertSeq(bytes, "(Hello) Tj\n");
        assertSeq(bytes, "ET\n");
        assertSeq(bytes, "1.5 w\n");
        assertSeq(bytes, "10 20 m\n");
        assertSeq(bytes, "100 20 l\n");
        assertSeq(bytes, "S\n");
        assertSeq(bytes, "10 50 200 100 re\n");
        assertSeq(bytes, "f\n");
    }

    @Test
    void stroke_appends_S_newline() {
        byte[] bytes = new ContentStream().stroke().toBytes();
        assertArrayEquals(ByteUtil.ascii("S\n"), bytes);
    }

    @Test
    void fill_appends_f_newline() {
        byte[] bytes = new ContentStream().fill().toBytes();
        assertArrayEquals(ByteUtil.ascii("f\n"), bytes);
    }

    @Test
    void beginText_endText_wrap_correctly() {
        byte[] bytes = new ContentStream().beginText().endText().toBytes();
        assertArrayEquals(ByteUtil.ascii("BT\nET\n"), bytes);
    }

    @Test
    void setLineWidth_integer_value_no_decimal() {
        byte[] bytes = new ContentStream().setLineWidth(2).toBytes();
        assertArrayEquals(ByteUtil.ascii("2 w\n"), bytes);
    }

    @Test
    void setLineWidth_fractional_value() {
        byte[] bytes = new ContentStream().setLineWidth(0.5).toBytes();
        assertArrayEquals(ByteUtil.ascii("0.5 w\n"), bytes);
    }

    @Test
    void setFont_integer_size_no_decimal() {
        byte[] bytes = new ContentStream().setFont("Helvetica", 14).toBytes();
        assertArrayEquals(ByteUtil.ascii("/Helvetica 14 Tf\n"), bytes);
    }

    @Test
    void moveText_integer_coords() {
        byte[] bytes = new ContentStream().moveText(100, 700).toBytes();
        assertArrayEquals(ByteUtil.ascii("100 700 Td\n"), bytes);
    }

    @Test
    void moveTo_lineTo_produce_correct_output() {
        byte[] bytes = new ContentStream().moveTo(0, 0).lineTo(595, 0).toBytes();
        assertArrayEquals(ByteUtil.ascii("0 0 m\n595 0 l\n"), bytes);
    }

    @Test
    void rectangle_produces_correct_output() {
        byte[] bytes = new ContentStream().rectangle(50, 100, 200, 150).toBytes();
        assertArrayEquals(ByteUtil.ascii("50 100 200 150 re\n"), bytes);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Color operators
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void setFillColor_produces_rg_operator() {
        byte[] bytes = new ContentStream().setFillColor(1, 0, 0).toBytes();
        assertArrayEquals(ByteUtil.ascii("1 0 0 rg\n"), bytes);
    }

    @Test
    void setFillColor_fractional_values() {
        byte[] bytes = new ContentStream().setFillColor(0.2, 0.4, 0.6).toBytes();
        assertArrayEquals(ByteUtil.ascii("0.2 0.4 0.6 rg\n"), bytes);
    }

    @Test
    void setStrokeColor_produces_RG_operator() {
        byte[] bytes = new ContentStream().setStrokeColor(0, 0, 1).toBytes();
        assertArrayEquals(ByteUtil.ascii("0 0 1 RG\n"), bytes);
    }

    @Test
    void setGray_produces_g_operator() {
        byte[] bytes = new ContentStream().setGray(0.5).toBytes();
        assertArrayEquals(ByteUtil.ascii("0.5 g\n"), bytes);
    }

    @Test
    void setGray_zero_is_black() {
        byte[] bytes = new ContentStream().setGray(0).toBytes();
        assertArrayEquals(ByteUtil.ascii("0 g\n"), bytes);
    }

    @Test
    void setGray_one_is_white() {
        byte[] bytes = new ContentStream().setGray(1).toBytes();
        assertArrayEquals(ByteUtil.ascii("1 g\n"), bytes);
    }

    @Test
    void setGrayStroke_produces_G_operator() {
        byte[] bytes = new ContentStream().setGrayStroke(0.75).toBytes();
        assertArrayEquals(ByteUtil.ascii("0.75 G\n"), bytes);
    }

    @Test
    void setFillColor_rejects_negative_component() {
        assertThrows(IllegalArgumentException.class,
                () -> new ContentStream().setFillColor(-0.1, 0, 0));
    }

    @Test
    void setFillColor_rejects_component_above_one() {
        assertThrows(IllegalArgumentException.class,
                () -> new ContentStream().setFillColor(0, 1.1, 0));
    }

    @Test
    void setStrokeColor_rejects_out_of_range() {
        assertThrows(IllegalArgumentException.class,
                () -> new ContentStream().setStrokeColor(0, 0, 2));
    }

    @Test
    void setGray_rejects_negative() {
        assertThrows(IllegalArgumentException.class,
                () -> new ContentStream().setGray(-0.01));
    }

    @Test
    void setGray_rejects_above_one() {
        assertThrows(IllegalArgumentException.class,
                () -> new ContentStream().setGray(1.001));
    }

    @Test
    void setGrayStroke_rejects_out_of_range() {
        assertThrows(IllegalArgumentException.class,
                () -> new ContentStream().setGrayStroke(1.5));
    }

    @Test
    void color_operators_chain_correctly() {
        byte[] bytes = new ContentStream()
                .setFillColor(1, 0, 0)
                .setStrokeColor(0, 0, 1)
                .setGray(0.5)
                .setGrayStroke(0.25)
                .toBytes();
        assertSeq(bytes, "1 0 0 rg\n");
        assertSeq(bytes, "0 0 1 RG\n");
        assertSeq(bytes, "0.5 g\n");
        assertSeq(bytes, "0.25 G\n");
    }

    @Test
    void setFillColor_boundary_values_accepted() {
        assertDoesNotThrow(() -> new ContentStream().setFillColor(0, 0, 0));
        assertDoesNotThrow(() -> new ContentStream().setFillColor(1, 1, 1));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // showText escaping
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void showText_plain_string() {
        byte[] bytes = new ContentStream().showText("Hello").toBytes();
        assertArrayEquals(ByteUtil.ascii("(Hello) Tj\n"), bytes);
    }

    @Test
    void showText_escapes_parentheses() {
        byte[] bytes = new ContentStream().showText("a(b)c").toBytes();
        assertArrayEquals(ByteUtil.ascii("(a\\(b\\)c) Tj\n"), bytes);
    }

    @Test
    void showText_escapes_backslash() {
        byte[] bytes = new ContentStream().showText("a\\b").toBytes();
        assertArrayEquals(ByteUtil.ascii("(a\\\\b) Tj\n"), bytes);
    }

    @Test
    void showText_escapes_all_special_chars() {
        byte[] bytes = new ContentStream().showText("(\\\n\r\t\b\f)").toBytes();
        assertArrayEquals(ByteUtil.ascii("(\\(\\\\\\n\\r\\t\\b\\f\\)) Tj\n"), bytes);
    }

    @Test
    void showText_rejects_character_above_latin1() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new ContentStream().showText("\u0100"));
        assertTrue(ex.getMessage().contains("U+100"),
                "Exception message should name the offending code point");
    }

    @Test
    void showText_accepts_max_latin1_char() {
        assertDoesNotThrow(() -> new ContentStream().showText("\u00FF"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // toBytes / determinism
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void toBytes_is_deterministic() {
        ContentStream cs = new ContentStream()
                .beginText().setFont("F1", 12).moveText(0, 0).showText("X").endText();
        assertArrayEquals(cs.toBytes(), cs.toBytes());
    }

    @Test
    void empty_stream_produces_empty_bytes() {
        assertEquals(0, new ContentStream().toBytes().length);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private static void assertSeq(byte[] data, String seq) {
        int idx = ByteUtil.indexOf(data, ByteUtil.ascii(seq), 0);
        assertNotEquals(-1, idx,
                "Expected sequence not found: \"" + seq.replace("\n", "\\n") + "\"");
    }
}
