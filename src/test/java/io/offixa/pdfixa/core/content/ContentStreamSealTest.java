package io.offixa.pdfixa.core.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link ContentStream} seal lifecycle guard.
 *
 * <p>After {@link ContentStream#seal()}, all mutating operations must throw
 * {@link IllegalStateException}, while read-only operations must remain
 * available.
 */
class ContentStreamSealTest {

    // ── seal() state ────────────────────────────────────────────────────────

    @Test
    void isSealed_false_initially() {
        assertFalse(new ContentStream().isSealed());
    }

    @Test
    void isSealed_true_after_seal() {
        ContentStream cs = new ContentStream();
        cs.seal();
        assertTrue(cs.isSealed());
    }

    @Test
    void seal_is_idempotent() {
        ContentStream cs = new ContentStream();
        cs.seal();
        cs.seal();
        assertTrue(cs.isSealed());
    }

    // ── Read-only methods still work after seal ─────────────────────────────

    @Test
    void toBytes_works_after_seal() {
        ContentStream cs = new ContentStream();
        cs.beginText().endText();
        cs.seal();
        byte[] bytes = cs.toBytes();
        assertTrue(bytes.length > 0);
    }

    @Test
    void getUsedFontAliases_works_after_seal() {
        ContentStream cs = new ContentStream();
        cs.seal();
        assertNotNull(cs.getUsedFontAliases());
    }

    // ── Text operators throw after seal ──────────────────────────────────────

    @Test
    void beginText_throws_after_seal() {
        ContentStream cs = sealed();
        assertThrows(IllegalStateException.class, cs::beginText);
    }

    @Test
    void endText_throws_after_seal() {
        ContentStream cs = sealed();
        assertThrows(IllegalStateException.class, cs::endText);
    }

    @Test
    void setFont_throws_after_seal() {
        ContentStream cs = sealed();
        assertThrows(IllegalStateException.class, () -> cs.setFont("F1", 12));
    }

    @Test
    void moveText_throws_after_seal() {
        ContentStream cs = sealed();
        assertThrows(IllegalStateException.class, () -> cs.moveText(0, 0));
    }

    @Test
    void showText_throws_after_seal() {
        ContentStream cs = sealed();
        assertThrows(IllegalStateException.class, () -> cs.showText("x"));
    }

    @Test
    void showTextHex_throws_after_seal() {
        ContentStream cs = sealed();
        assertThrows(IllegalStateException.class, () -> cs.showTextHex("48"));
    }

    @Test
    void showTextUnicodeRaw_throws_after_seal() {
        ContentStream cs = sealed();
        assertThrows(IllegalStateException.class,
                () -> cs.showTextUnicodeRaw("Тест"));
    }

    // ── Graphics operators throw after seal ──────────────────────────────────

    @Test
    void moveTo_throws_after_seal() {
        ContentStream cs = sealed();
        assertThrows(IllegalStateException.class, () -> cs.moveTo(0, 0));
    }

    @Test
    void lineTo_throws_after_seal() {
        ContentStream cs = sealed();
        assertThrows(IllegalStateException.class, () -> cs.lineTo(10, 10));
    }

    @Test
    void stroke_throws_after_seal() {
        ContentStream cs = sealed();
        assertThrows(IllegalStateException.class, cs::stroke);
    }

    @Test
    void rectangle_throws_after_seal() {
        ContentStream cs = sealed();
        assertThrows(IllegalStateException.class, () -> cs.rectangle(0, 0, 10, 10));
    }

    @Test
    void fill_throws_after_seal() {
        ContentStream cs = sealed();
        assertThrows(IllegalStateException.class, cs::fill);
    }

    @Test
    void setLineWidth_throws_after_seal() {
        ContentStream cs = sealed();
        assertThrows(IllegalStateException.class, () -> cs.setLineWidth(1));
    }

    // ── Color operators throw after seal ─────────────────────────────────────

    @Test
    void setFillColor_throws_after_seal() {
        ContentStream cs = sealed();
        assertThrows(IllegalStateException.class, () -> cs.setFillColor(1, 0, 0));
    }

    @Test
    void setStrokeColor_throws_after_seal() {
        ContentStream cs = sealed();
        assertThrows(IllegalStateException.class, () -> cs.setStrokeColor(0, 0, 1));
    }

    @Test
    void setGray_throws_after_seal() {
        ContentStream cs = sealed();
        assertThrows(IllegalStateException.class, () -> cs.setGray(0.5));
    }

    @Test
    void setGrayStroke_throws_after_seal() {
        ContentStream cs = sealed();
        assertThrows(IllegalStateException.class, () -> cs.setGrayStroke(0.5));
    }

    // ── State operators throw after seal ─────────────────────────────────────

    @Test
    void saveState_throws_after_seal() {
        ContentStream cs = sealed();
        assertThrows(IllegalStateException.class, cs::saveState);
    }

    @Test
    void restoreState_throws_after_seal() {
        ContentStream cs = sealed();
        assertThrows(IllegalStateException.class, cs::restoreState);
    }

    @Test
    void concatMatrix_throws_after_seal() {
        ContentStream cs = sealed();
        assertThrows(IllegalStateException.class,
                () -> cs.concatMatrix(1, 0, 0, 1, 0, 0));
    }

    @Test
    void doXObject_throws_after_seal() {
        ContentStream cs = sealed();
        assertThrows(IllegalStateException.class, () -> cs.doXObject("Im1"));
    }

    // ── Exception message ───────────────────────────────────────────────────

    @Test
    void exception_message_mentions_sealed() {
        ContentStream cs = sealed();
        IllegalStateException ex = assertThrows(
                IllegalStateException.class, cs::beginText);
        assertTrue(ex.getMessage().contains("sealed"),
                "Message must mention 'sealed', got: " + ex.getMessage());
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private static ContentStream sealed() {
        ContentStream cs = new ContentStream();
        cs.seal();
        return cs;
    }
}
