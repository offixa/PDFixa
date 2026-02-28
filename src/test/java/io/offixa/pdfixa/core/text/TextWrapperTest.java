package io.offixa.pdfixa.core.text;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TextWrapper}.
 *
 * <p>All scenarios use Courier (every glyph = 600 units) so width
 * calculations are trivially predictable: at fontSize=10 each character
 * occupies exactly 6 pt.  maxWidth=30 therefore fits exactly 5 characters.
 */
class TextWrapperTest {

    private static final Base14FontMetrics METRICS = Base14FontMetrics.getInstance();
    private static final String COURIER    = "Courier";
    private static final double FONT_SIZE  = 10.0;
    private static final double MAX_WIDTH  = 30.0; // fits 5 chars (5 × 6 pt)

    // ── Basic wrap ────────────────────────────────────────────────────────────

    @Test
    void basic_wrap_splits_two_words_into_two_lines() {
        // "hello"=30 pt fits; "hello world"=66 pt doesn't → split at space
        List<String> lines = wrap("hello world");
        assertEquals(List.of("hello", "world"), lines);
    }

    @Test
    void single_word_that_fits_is_returned_as_one_line() {
        List<String> lines = wrap("hello");
        assertEquals(List.of("hello"), lines);
    }

    @Test
    void two_words_both_fitting_on_one_line_stay_together() {
        // "ab cd" = 5 chars × 6 = 30 pt ≤ 30 → must NOT split
        List<String> lines = wrap("ab cd");
        assertEquals(List.of("ab cd"), lines);
    }

    @Test
    void three_word_sentence_wraps_into_correct_lines() {
        // "hi" (12) fits; "hi bye" = 6 chars = 36 pt > 30 → break after "hi"
        // "bye" (18) + " x" (6) = "bye x" (24) ≤ 30 → second line
        List<String> lines = wrap("hi bye x");
        assertEquals(List.of("hi", "bye x"), lines);
    }

    // ── Forced newline ────────────────────────────────────────────────────────

    @Test
    void forced_newline_produces_two_paragraphs() {
        // '\n' forces a break regardless of available width
        List<String> lines = wrap("a b\nc d");
        assertEquals(List.of("a b", "c d"), lines);
    }

    @Test
    void multiple_forced_newlines_produce_multiple_lines() {
        List<String> lines = wrap("one\ntwo\nthree");
        assertEquals(List.of("one", "two", "three"), lines);
    }

    @Test
    void crlf_is_treated_as_single_newline() {
        // "first" (5 chars = 30 pt) and "second" (6 chars = 36 pt) each live in
        // their own paragraph after CRLF normalisation; use a wider box so neither
        // word itself gets split by the long-word fallback.
        List<String> lines = TextWrapper.wrap(
                "first\r\nsecond", COURIER, FONT_SIZE, 60.0, METRICS);
        assertEquals(List.of("first", "second"), lines);
    }

    @Test
    void standalone_cr_is_treated_as_newline() {
        List<String> lines = wrap("a\rb");
        assertEquals(List.of("a", "b"), lines);
    }

    @Test
    void empty_paragraph_from_consecutive_newlines_produces_empty_line() {
        // "a\n\nb" → paragraphs ["a", "", "b"] → ["a", "", "b"]
        List<String> lines = wrap("a\n\nb");
        assertEquals(3, lines.size());
        assertEquals("a", lines.get(0));
        assertEquals("",  lines.get(1));
        assertEquals("b", lines.get(2));
    }

    // ── Long word fallback ────────────────────────────────────────────────────

    /**
     * maxWidth=18 fits exactly 3 Courier chars at size 10 (3 × 6 = 18 pt).
     * "abcdefghij" (10 chars) must be split into ["abc","def","ghi","j"].
     */
    @Test
    void long_word_is_split_deterministically_by_char() {
        double maxFits3 = 18.0; // 3 × 6 pt
        List<String> lines = TextWrapper.wrap(
                "abcdefghij", COURIER, FONT_SIZE, maxFits3, METRICS);
        assertEquals(List.of("abc", "def", "ghi", "j"), lines);
    }

    @Test
    void long_word_exactly_fills_segment_boundaries() {
        // "abcdef" with maxWidth=18 → ["abc", "def"]
        double maxFits3 = 18.0;
        List<String> lines = TextWrapper.wrap(
                "abcdef", COURIER, FONT_SIZE, maxFits3, METRICS);
        assertEquals(List.of("abc", "def"), lines);
    }

    @Test
    void single_oversized_char_still_emitted_as_own_line() {
        // maxWidth=3 is narrower than one Courier char (6 pt); char must still be emitted
        List<String> lines = TextWrapper.wrap(
                "a", COURIER, FONT_SIZE, 3.0, METRICS);
        assertEquals(List.of("a"), lines);
    }

    @Test
    void long_word_followed_by_normal_word_wraps_correctly() {
        // maxWidth=18 (3 chars); "abcdef" → "abc","def" then "hi" fits on next line
        double maxFits3 = 18.0;
        List<String> lines = TextWrapper.wrap(
                "abcdef hi", COURIER, FONT_SIZE, maxFits3, METRICS);
        assertEquals(List.of("abc", "def", "hi"), lines);
    }

    // ── Leading / trailing whitespace & multi-space collapse ─────────────────

    @Test
    void leading_and_trailing_spaces_are_stripped() {
        List<String> lines = wrap("  hello  ");
        assertEquals(List.of("hello"), lines);
    }

    @Test
    void multiple_internal_spaces_are_collapsed_to_one() {
        List<String> lines = wrap("a  b");
        assertEquals(List.of("a b"), lines);
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    void empty_string_returns_empty_list() {
        List<String> lines = wrap("");
        assertEquals(List.of(""), lines,
                "An empty paragraph should still produce one empty-string line");
    }

    @Test
    void null_text_throws_npe() {
        assertThrows(NullPointerException.class,
                () -> TextWrapper.wrap(null, COURIER, FONT_SIZE, MAX_WIDTH, METRICS));
    }

    @Test
    void null_font_throws_npe() {
        assertThrows(NullPointerException.class,
                () -> TextWrapper.wrap("text", null, FONT_SIZE, MAX_WIDTH, METRICS));
    }

    @Test
    void null_metrics_throws_npe() {
        assertThrows(NullPointerException.class,
                () -> TextWrapper.wrap("text", COURIER, FONT_SIZE, MAX_WIDTH, null));
    }

    @Test
    void zero_font_size_throws_iae() {
        assertThrows(IllegalArgumentException.class,
                () -> TextWrapper.wrap("text", COURIER, 0, MAX_WIDTH, METRICS));
    }

    @Test
    void zero_max_width_throws_iae() {
        assertThrows(IllegalArgumentException.class,
                () -> TextWrapper.wrap("text", COURIER, FONT_SIZE, 0, METRICS));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static List<String> wrap(String text) {
        return TextWrapper.wrap(text, COURIER, FONT_SIZE, MAX_WIDTH, METRICS);
    }
}
