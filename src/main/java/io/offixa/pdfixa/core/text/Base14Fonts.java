package io.offixa.pdfixa.core.text;

import java.util.Set;

/**
 * Canonical Base-14 PDF font names.
 */
public final class Base14Fonts {

    private static final Set<String> SUPPORTED = Set.of(
            "Courier",
            "Courier-Bold",
            "Courier-Oblique",
            "Courier-BoldOblique",
            "Helvetica",
            "Helvetica-Bold",
            "Helvetica-Oblique",
            "Helvetica-BoldOblique",
            "Times-Roman",
            "Times-Bold",
            "Times-Italic",
            "Times-BoldItalic",
            "Symbol",
            "ZapfDingbats"
    );

    private Base14Fonts() {
    }

    public static boolean isSupported(String name) {
        return name != null && SUPPORTED.contains(name);
    }

    /**
     * Returns canonical name if supported, otherwise throws.
     */
    public static String normalize(String name) {
        if (!isSupported(name)) {
            throw new IllegalArgumentException("Unsupported Base-14 font: " + name);
        }
        return name;
    }
}
