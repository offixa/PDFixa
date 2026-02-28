package io.offixa.pdfixa.core.text;

import java.util.HashMap;
import java.util.Map;

/**
 * Deterministic Base-14 font metrics for Latin-1 code points.
 *
 * <p>Tables are in glyph-space units (1/1000 em). Unknown glyph codes fall
 * back to a fixed ".notdef" width of {@value #NOTDEF_WIDTH}.
 */
public final class Base14FontMetrics implements FontMetrics {

    private static final short NOTDEF_WIDTH = 600;
    private static final Base14FontMetrics INSTANCE = new Base14FontMetrics();

    private final Map<String, short[]> widthsByFont;

    private Base14FontMetrics() {
        Map<String, short[]> map = new HashMap<>();

        short[] courier = buildCourier();
        short[] helvetica = buildHelvetica();
        short[] timesRoman = buildTimesRoman();
        short[] symbol = buildDefault();
        short[] zapfDingbats = buildDefault();

        map.put("Courier", courier);
        map.put("Courier-Bold", courier);
        map.put("Courier-Oblique", courier);
        map.put("Courier-BoldOblique", courier);

        map.put("Helvetica", helvetica);
        map.put("Helvetica-Bold", helvetica);
        map.put("Helvetica-Oblique", helvetica);
        map.put("Helvetica-BoldOblique", helvetica);

        map.put("Times-Roman", timesRoman);
        map.put("Times-Bold", timesRoman);
        map.put("Times-Italic", timesRoman);
        map.put("Times-BoldItalic", timesRoman);

        map.put("Symbol", symbol);
        map.put("ZapfDingbats", zapfDingbats);
        this.widthsByFont = Map.copyOf(map);
    }

    public static Base14FontMetrics getInstance() {
        return INSTANCE;
    }

    @Override
    public double textWidthPt(String text, String fontName, double fontSize) {
        if (fontSize <= 0) {
            throw new IllegalArgumentException("fontSize must be > 0");
        }
        if (text == null) {
            throw new NullPointerException("text");
        }

        String canonical = Base14Fonts.normalize(fontName);
        short[] widths = widthsByFont.get(canonical);
        if (widths == null) {
            throw new IllegalArgumentException("Unsupported Base-14 font: " + fontName);
        }

        long total1000 = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch > 0xFF) {
                throw new IllegalArgumentException(
                        "Character U+" + Integer.toHexString(ch).toUpperCase()
                                + " at index " + i + " is outside Latin-1 range");
            }
            total1000 += widths[ch & 0xFF];
        }

        return total1000 * fontSize / 1000.0;
    }

    private static short[] buildDefault() {
        short[] table = new short[256];
        fill(table, NOTDEF_WIDTH);
        return table;
    }

    private static short[] buildCourier() {
        short[] table = new short[256];
        fill(table, (short) 600);
        return table;
    }

    private static short[] buildHelvetica() {
        short[] t = buildDefault();

        set(t, 32, 278); // space
        set(t, 33, 278); // !
        set(t, 34, 355); // "
        set(t, 35, 556); // #
        set(t, 36, 556); // $
        set(t, 37, 889); // %
        set(t, 38, 667); // &
        set(t, 39, 191); // '
        set(t, 40, 333); // (
        set(t, 41, 333); // )
        set(t, 42, 389); // *
        set(t, 43, 584); // +
        set(t, 44, 278); // ,
        set(t, 45, 333); // -
        set(t, 46, 278); // .
        set(t, 47, 278); // /
        for (int c = 48; c <= 57; c++) set(t, c, 556); // 0-9
        set(t, 58, 278); // :
        set(t, 59, 278); // ;
        set(t, 60, 584); // <
        set(t, 61, 584); // =
        set(t, 62, 584); // >
        set(t, 63, 556); // ?
        set(t, 64, 1015); // @
        set(t, 65, 667); set(t, 66, 667); set(t, 67, 722); set(t, 68, 722);
        set(t, 69, 667); set(t, 70, 611); set(t, 71, 778); set(t, 72, 722);
        set(t, 73, 278); set(t, 74, 500); set(t, 75, 667); set(t, 76, 556);
        set(t, 77, 833); set(t, 78, 722); set(t, 79, 778); set(t, 80, 667);
        set(t, 81, 778); set(t, 82, 722); set(t, 83, 667); set(t, 84, 611);
        set(t, 85, 722); set(t, 86, 667); set(t, 87, 944); set(t, 88, 667);
        set(t, 89, 667); set(t, 90, 611);
        set(t, 91, 278); // [
        set(t, 92, 278); // \
        set(t, 93, 278); // ]
        set(t, 94, 469); // ^
        set(t, 95, 556); // _
        set(t, 96, 191); // `
        set(t, 97, 556); set(t, 98, 556); set(t, 99, 500); set(t, 100, 556);
        set(t, 101, 556); set(t, 102, 278); set(t, 103, 556); set(t, 104, 556);
        set(t, 105, 222); set(t, 106, 222); set(t, 107, 500); set(t, 108, 222);
        set(t, 109, 833); set(t, 110, 556); set(t, 111, 556); set(t, 112, 556);
        set(t, 113, 556); set(t, 114, 333); set(t, 115, 500); set(t, 116, 278);
        set(t, 117, 556); set(t, 118, 500); set(t, 119, 722); set(t, 120, 500);
        set(t, 121, 500); set(t, 122, 500);
        set(t, 123, 334); // {
        set(t, 124, 260); // |
        set(t, 125, 334); // }
        set(t, 126, 584); // ~

        return t;
    }

    private static short[] buildTimesRoman() {
        short[] t = buildDefault();

        set(t, 32, 250); // space
        set(t, 33, 333); // !
        set(t, 34, 408); // "
        set(t, 35, 500); // #
        set(t, 36, 500); // $
        set(t, 37, 833); // %
        set(t, 38, 778); // &
        set(t, 39, 180); // '
        set(t, 40, 333); // (
        set(t, 41, 333); // )
        set(t, 42, 500); // *
        set(t, 43, 564); // +
        set(t, 44, 250); // ,
        set(t, 45, 333); // -
        set(t, 46, 250); // .
        set(t, 47, 278); // /
        for (int c = 48; c <= 57; c++) set(t, c, 500); // 0-9
        set(t, 58, 278); // :
        set(t, 59, 278); // ;
        set(t, 60, 564); // <
        set(t, 61, 564); // =
        set(t, 62, 564); // >
        set(t, 63, 444); // ?
        set(t, 64, 921); // @
        set(t, 65, 722); set(t, 66, 667); set(t, 67, 667); set(t, 68, 722);
        set(t, 69, 611); set(t, 70, 556); set(t, 71, 722); set(t, 72, 722);
        set(t, 73, 333); set(t, 74, 389); set(t, 75, 722); set(t, 76, 611);
        set(t, 77, 889); set(t, 78, 722); set(t, 79, 722); set(t, 80, 556);
        set(t, 81, 722); set(t, 82, 667); set(t, 83, 556); set(t, 84, 611);
        set(t, 85, 722); set(t, 86, 722); set(t, 87, 944); set(t, 88, 722);
        set(t, 89, 722); set(t, 90, 611);
        set(t, 91, 333); // [
        set(t, 92, 278); // \
        set(t, 93, 333); // ]
        set(t, 94, 469); // ^
        set(t, 95, 500); // _
        set(t, 96, 333); // `
        set(t, 97, 444); set(t, 98, 500); set(t, 99, 444); set(t, 100, 500);
        set(t, 101, 444); set(t, 102, 333); set(t, 103, 500); set(t, 104, 500);
        set(t, 105, 278); set(t, 106, 278); set(t, 107, 500); set(t, 108, 278);
        set(t, 109, 778); set(t, 110, 500); set(t, 111, 500); set(t, 112, 500);
        set(t, 113, 500); set(t, 114, 333); set(t, 115, 389); set(t, 116, 278);
        set(t, 117, 500); set(t, 118, 500); set(t, 119, 722); set(t, 120, 500);
        set(t, 121, 500); set(t, 122, 444);
        set(t, 123, 480); // {
        set(t, 124, 200); // |
        set(t, 125, 480); // }
        set(t, 126, 541); // ~

        return t;
    }

    private static void fill(short[] table, short value) {
        for (int i = 0; i < table.length; i++) {
            table[i] = value;
        }
    }

    private static void set(short[] table, int code, int width) {
        table[code] = (short) width;
    }
}
