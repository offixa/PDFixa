package io.offixa.pdfixa.core.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Base14FontMetrics}.
 * Focuses on Courier (monospaced, every glyph = 600 units) and
 * boundary / error conditions shared across all fonts.
 */
class Base14FontMetricsTest {

    private static final Base14FontMetrics METRICS = Base14FontMetrics.getInstance();

    // ── Courier width calculations ────────────────────────────────────────────

    @Test
    void courier_single_char_width_at_size_10() {
        // 1 char × 600 units × 10pt / 1000 = 6.0 pt
        assertEquals(6.0, METRICS.textWidthPt("A", "Courier", 10), 1e-9);
    }

    @Test
    void courier_four_chars_width_at_size_10() {
        // 4 × 600 × 10 / 1000 = 24.0
        assertEquals(24.0, METRICS.textWidthPt("AAAA", "Courier", 10), 1e-9);
    }

    @Test
    void courier_width_scales_linearly_with_font_size() {
        double w12 = METRICS.textWidthPt("X", "Courier", 12);
        double w24 = METRICS.textWidthPt("X", "Courier", 24);
        assertEquals(w12 * 2, w24, 1e-9,
                "Width must double when fontSize doubles");
    }

    @Test
    void courier_all_variants_share_same_width_table() {
        String text = "Test";
        double base = METRICS.textWidthPt(text, "Courier", 10);
        assertEquals(base, METRICS.textWidthPt(text, "Courier-Bold",        10), 1e-9);
        assertEquals(base, METRICS.textWidthPt(text, "Courier-Oblique",     10), 1e-9);
        assertEquals(base, METRICS.textWidthPt(text, "Courier-BoldOblique", 10), 1e-9);
    }

    @Test
    void courier_space_char_has_width_600_units() {
        // space (0x20) is also 600 in Courier
        assertEquals(6.0, METRICS.textWidthPt(" ", "Courier", 10), 1e-9);
    }

    @Test
    void courier_empty_string_returns_zero() {
        assertEquals(0.0, METRICS.textWidthPt("", "Courier", 10), 1e-9);
    }

    // ── Helvetica sanity check ────────────────────────────────────────────────

    @Test
    void helvetica_capital_A_has_known_width() {
        // Helvetica 'A' (0x41) = 667 units; at size 10 → 6.67 pt
        assertEquals(6.67, METRICS.textWidthPt("A", "Helvetica", 10), 1e-9);
    }

    // ── Times-Roman sanity check ──────────────────────────────────────────────

    @Test
    void timesRoman_capital_A_has_known_width() {
        // Times-Roman 'A' (0x41) = 722 units; at size 10 → 7.22 pt
        assertEquals(7.22, METRICS.textWidthPt("A", "Times-Roman", 10), 1e-9);
    }

    // ── Error handling ────────────────────────────────────────────────────────

    @Test
    void rejects_unsupported_font_name() {
        assertThrows(IllegalArgumentException.class,
                () -> METRICS.textWidthPt("x", "Arial", 12));
    }

    @Test
    void rejects_font_size_zero() {
        assertThrows(IllegalArgumentException.class,
                () -> METRICS.textWidthPt("x", "Courier", 0));
    }

    @Test
    void rejects_negative_font_size() {
        assertThrows(IllegalArgumentException.class,
                () -> METRICS.textWidthPt("x", "Courier", -1));
    }

    @Test
    void rejects_null_text() {
        assertThrows(NullPointerException.class,
                () -> METRICS.textWidthPt(null, "Courier", 12));
    }

    @Test
    void rejects_character_outside_latin1() {
        assertThrows(IllegalArgumentException.class,
                () -> METRICS.textWidthPt("\u0100", "Courier", 12));
    }
}
