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

        // WinAnsi extension (0x80-0x9F)
        set(t, 0x80, 556); // Euro
        set(t, 0x82, 222); // quotesinglbase
        set(t, 0x83, 556); // florin
        set(t, 0x84, 333); // quotedblbase
        set(t, 0x85, 1000); // ellipsis
        set(t, 0x86, 556); // dagger
        set(t, 0x87, 556); // daggerdbl
        set(t, 0x88, 333); // circumflex
        set(t, 0x89, 1000); // perthousand
        set(t, 0x8A, 667); // Scaron
        set(t, 0x8B, 333); // guilsinglleft
        set(t, 0x8C, 1000); // OE
        set(t, 0x8E, 611); // Zcaron
        set(t, 0x91, 222); // quoteleft
        set(t, 0x92, 222); // quoteright
        set(t, 0x93, 333); // quotedblleft
        set(t, 0x94, 333); // quotedblright
        set(t, 0x95, 350); // bullet
        set(t, 0x96, 556); // endash
        set(t, 0x97, 1000); // emdash
        set(t, 0x98, 333); // tilde
        set(t, 0x99, 1000); // trademark
        set(t, 0x9A, 500); // scaron
        set(t, 0x9B, 333); // guilsinglright
        set(t, 0x9C, 944); // oe
        set(t, 0x9E, 500); // zcaron
        set(t, 0x9F, 667); // Ydieresis

        // WinAnsi Latin-1 range (0xA0-0xFF)
        set(t, 0xA0, 278); // space
        set(t, 0xA1, 333); // exclamdown
        set(t, 0xA2, 556); // cent
        set(t, 0xA3, 556); // sterling
        set(t, 0xA4, 556); // currency
        set(t, 0xA5, 556); // yen
        set(t, 0xA6, 260); // brokenbar
        set(t, 0xA7, 556); // section
        set(t, 0xA8, 333); // dieresis
        set(t, 0xA9, 737); // copyright
        set(t, 0xAA, 370); // ordfeminine
        set(t, 0xAB, 556); // guillemotleft
        set(t, 0xAC, 584); // logicalnot
        set(t, 0xAD, 333); // hyphen
        set(t, 0xAE, 737); // registered
        set(t, 0xAF, 333); // macron
        set(t, 0xB0, 400); // degree
        set(t, 0xB1, 584); // plusminus
        set(t, 0xB2, 333); // twosuperior
        set(t, 0xB3, 333); // threesuperior
        set(t, 0xB4, 333); // acute
        set(t, 0xB5, 556); // mu
        set(t, 0xB6, 537); // paragraph
        set(t, 0xB7, 278); // periodcentered
        set(t, 0xB8, 333); // cedilla
        set(t, 0xB9, 333); // onesuperior
        set(t, 0xBA, 365); // ordmasculine
        set(t, 0xBB, 556); // guillemotright
        set(t, 0xBC, 834); // onequarter
        set(t, 0xBD, 834); // onehalf
        set(t, 0xBE, 834); // threequarters
        set(t, 0xBF, 611); // questiondown
        set(t, 0xC0, 667); // Agrave
        set(t, 0xC1, 667); // Aacute
        set(t, 0xC2, 667); // Acircumflex
        set(t, 0xC3, 667); // Atilde
        set(t, 0xC4, 667); // Adieresis
        set(t, 0xC5, 667); // Aring
        set(t, 0xC6, 1000); // AE
        set(t, 0xC7, 722); // Ccedilla
        set(t, 0xC8, 667); // Egrave
        set(t, 0xC9, 667); // Eacute
        set(t, 0xCA, 667); // Ecircumflex
        set(t, 0xCB, 667); // Edieresis
        set(t, 0xCC, 278); // Igrave
        set(t, 0xCD, 278); // Iacute
        set(t, 0xCE, 278); // Icircumflex
        set(t, 0xCF, 278); // Idieresis
        set(t, 0xD0, 722); // Eth
        set(t, 0xD1, 722); // Ntilde
        set(t, 0xD2, 778); // Ograve
        set(t, 0xD3, 778); // Oacute
        set(t, 0xD4, 778); // Ocircumflex
        set(t, 0xD5, 778); // Otilde
        set(t, 0xD6, 778); // Odieresis
        set(t, 0xD7, 584); // multiply
        set(t, 0xD8, 778); // Oslash
        set(t, 0xD9, 722); // Ugrave
        set(t, 0xDA, 722); // Uacute
        set(t, 0xDB, 722); // Ucircumflex
        set(t, 0xDC, 722); // Udieresis
        set(t, 0xDD, 667); // Yacute
        set(t, 0xDE, 667); // Thorn
        set(t, 0xDF, 611); // germandbls
        set(t, 0xE0, 556); // agrave
        set(t, 0xE1, 556); // aacute
        set(t, 0xE2, 556); // acircumflex
        set(t, 0xE3, 556); // atilde
        set(t, 0xE4, 556); // adieresis
        set(t, 0xE5, 556); // aring
        set(t, 0xE6, 889); // ae
        set(t, 0xE7, 500); // ccedilla
        set(t, 0xE8, 556); // egrave
        set(t, 0xE9, 556); // eacute
        set(t, 0xEA, 556); // ecircumflex
        set(t, 0xEB, 556); // edieresis
        set(t, 0xEC, 222); // igrave
        set(t, 0xED, 222); // iacute
        set(t, 0xEE, 222); // icircumflex
        set(t, 0xEF, 222); // idieresis
        set(t, 0xF0, 556); // eth
        set(t, 0xF1, 556); // ntilde
        set(t, 0xF2, 556); // ograve
        set(t, 0xF3, 556); // oacute
        set(t, 0xF4, 556); // ocircumflex
        set(t, 0xF5, 556); // otilde
        set(t, 0xF6, 556); // odieresis
        set(t, 0xF7, 584); // divide
        set(t, 0xF8, 611); // oslash
        set(t, 0xF9, 556); // ugrave
        set(t, 0xFA, 556); // uacute
        set(t, 0xFB, 556); // ucircumflex
        set(t, 0xFC, 556); // udieresis
        set(t, 0xFD, 500); // yacute
        set(t, 0xFE, 556); // thorn
        set(t, 0xFF, 500); // ydieresis

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

        // WinAnsi extension (0x80-0x9F)
        set(t, 0x80, 500); // Euro
        set(t, 0x82, 180); // quotesinglbase
        set(t, 0x83, 500); // florin
        set(t, 0x84, 444); // quotedblbase
        set(t, 0x85, 1000); // ellipsis
        set(t, 0x86, 500); // dagger
        set(t, 0x87, 500); // daggerdbl
        set(t, 0x88, 333); // circumflex
        set(t, 0x89, 1000); // perthousand
        set(t, 0x8A, 556); // Scaron
        set(t, 0x8B, 333); // guilsinglleft
        set(t, 0x8C, 889); // OE
        set(t, 0x8E, 611); // Zcaron
        set(t, 0x91, 333); // quoteleft
        set(t, 0x92, 333); // quoteright
        set(t, 0x93, 444); // quotedblleft
        set(t, 0x94, 444); // quotedblright
        set(t, 0x95, 350); // bullet
        set(t, 0x96, 500); // endash
        set(t, 0x97, 1000); // emdash
        set(t, 0x98, 333); // tilde
        set(t, 0x99, 980); // trademark
        set(t, 0x9A, 389); // scaron
        set(t, 0x9B, 333); // guilsinglright
        set(t, 0x9C, 722); // oe
        set(t, 0x9E, 444); // zcaron
        set(t, 0x9F, 722); // Ydieresis

        // WinAnsi Latin-1 range (0xA0-0xFF)
        set(t, 0xA0, 250); // space
        set(t, 0xA1, 333); // exclamdown
        set(t, 0xA2, 500); // cent
        set(t, 0xA3, 500); // sterling
        set(t, 0xA4, 500); // currency
        set(t, 0xA5, 500); // yen
        set(t, 0xA6, 200); // brokenbar
        set(t, 0xA7, 500); // section
        set(t, 0xA8, 333); // dieresis
        set(t, 0xA9, 760); // copyright
        set(t, 0xAA, 276); // ordfeminine
        set(t, 0xAB, 500); // guillemotleft
        set(t, 0xAC, 564); // logicalnot
        set(t, 0xAD, 333); // hyphen
        set(t, 0xAE, 760); // registered
        set(t, 0xAF, 333); // macron
        set(t, 0xB0, 400); // degree
        set(t, 0xB1, 564); // plusminus
        set(t, 0xB2, 300); // twosuperior
        set(t, 0xB3, 300); // threesuperior
        set(t, 0xB4, 333); // acute
        set(t, 0xB5, 500); // mu
        set(t, 0xB6, 453); // paragraph
        set(t, 0xB7, 250); // periodcentered
        set(t, 0xB8, 333); // cedilla
        set(t, 0xB9, 300); // onesuperior
        set(t, 0xBA, 310); // ordmasculine
        set(t, 0xBB, 500); // guillemotright
        set(t, 0xBC, 750); // onequarter
        set(t, 0xBD, 750); // onehalf
        set(t, 0xBE, 750); // threequarters
        set(t, 0xBF, 444); // questiondown
        set(t, 0xC0, 722); // Agrave
        set(t, 0xC1, 722); // Aacute
        set(t, 0xC2, 722); // Acircumflex
        set(t, 0xC3, 722); // Atilde
        set(t, 0xC4, 722); // Adieresis
        set(t, 0xC5, 722); // Aring
        set(t, 0xC6, 889); // AE
        set(t, 0xC7, 667); // Ccedilla
        set(t, 0xC8, 611); // Egrave
        set(t, 0xC9, 611); // Eacute
        set(t, 0xCA, 611); // Ecircumflex
        set(t, 0xCB, 611); // Edieresis
        set(t, 0xCC, 333); // Igrave
        set(t, 0xCD, 333); // Iacute
        set(t, 0xCE, 333); // Icircumflex
        set(t, 0xCF, 333); // Idieresis
        set(t, 0xD0, 722); // Eth
        set(t, 0xD1, 722); // Ntilde
        set(t, 0xD2, 722); // Ograve
        set(t, 0xD3, 722); // Oacute
        set(t, 0xD4, 722); // Ocircumflex
        set(t, 0xD5, 722); // Otilde
        set(t, 0xD6, 722); // Odieresis
        set(t, 0xD7, 564); // multiply
        set(t, 0xD8, 722); // Oslash
        set(t, 0xD9, 722); // Ugrave
        set(t, 0xDA, 722); // Uacute
        set(t, 0xDB, 722); // Ucircumflex
        set(t, 0xDC, 722); // Udieresis
        set(t, 0xDD, 722); // Yacute
        set(t, 0xDE, 556); // Thorn
        set(t, 0xDF, 500); // germandbls
        set(t, 0xE0, 444); // agrave
        set(t, 0xE1, 444); // aacute
        set(t, 0xE2, 444); // acircumflex
        set(t, 0xE3, 444); // atilde
        set(t, 0xE4, 444); // adieresis
        set(t, 0xE5, 444); // aring
        set(t, 0xE6, 667); // ae
        set(t, 0xE7, 444); // ccedilla
        set(t, 0xE8, 444); // egrave
        set(t, 0xE9, 444); // eacute
        set(t, 0xEA, 444); // ecircumflex
        set(t, 0xEB, 444); // edieresis
        set(t, 0xEC, 278); // igrave
        set(t, 0xED, 278); // iacute
        set(t, 0xEE, 278); // icircumflex
        set(t, 0xEF, 278); // idieresis
        set(t, 0xF0, 500); // eth
        set(t, 0xF1, 500); // ntilde
        set(t, 0xF2, 500); // ograve
        set(t, 0xF3, 500); // oacute
        set(t, 0xF4, 500); // ocircumflex
        set(t, 0xF5, 500); // otilde
        set(t, 0xF6, 500); // odieresis
        set(t, 0xF7, 564); // divide
        set(t, 0xF8, 500); // oslash
        set(t, 0xF9, 500); // ugrave
        set(t, 0xFA, 500); // uacute
        set(t, 0xFB, 500); // ucircumflex
        set(t, 0xFC, 500); // udieresis
        set(t, 0xFD, 500); // yacute
        set(t, 0xFE, 500); // thorn
        set(t, 0xFF, 500); // ydieresis

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
