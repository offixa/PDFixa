package io.offixa.pdfixa.core.document;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Document-level registry that assigns stable, deterministic aliases (F1, F2, …)
 * to Base-14 font names.
 *
 * <p>Aliases are allocated in insertion order — the first font name registered
 * gets F1, the second F2, and so on. Asking for the same font name twice always
 * returns the same alias.
 *
 * <p>Example:
 * <pre>
 *   registry.getAlias("Helvetica") // → "F1"
 *   registry.getAlias("Courier")   // → "F2"
 *   registry.getAlias("Helvetica") // → "F1"  (reused)
 * </pre>
 *
 * <p>This class is not thread-safe.
 */
public final class FontRegistry {

    private final Map<String, String> nameToAlias = new LinkedHashMap<>();
    private final Map<String, String> aliasToName = new LinkedHashMap<>();
    private int nextIndex = 1;

    /**
     * Returns the alias for {@code fontName}, creating one if this is the first
     * time the name is seen.
     *
     * @param fontName Base-14 font name, e.g. {@code "Helvetica"}
     * @return alias such as {@code "F1"}, {@code "F2"}, …
     */
    public String getAlias(String fontName) {
        return nameToAlias.computeIfAbsent(fontName, k -> {
            String alias = "F" + nextIndex++;
            aliasToName.put(alias, fontName);
            return alias;
        });
    }

    /**
     * Returns the font name that was registered under {@code alias}, or
     * {@code null} if the alias is unknown.
     */
    public String getFontName(String alias) {
        return aliasToName.get(alias);
    }

    /**
     * Returns an unmodifiable, insertion-ordered view of all alias → font-name
     * mappings currently held by this registry.
     */
    public Map<String, String> aliasToNameView() {
        return Collections.unmodifiableMap(aliasToName);
    }
}
