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
final class FontRegistry {

    private final Map<String, String> nameToAlias = new LinkedHashMap<>();
    private final Map<String, String> aliasToName = new LinkedHashMap<>();
    private final Map<String, Integer> indirectFontObjects = new LinkedHashMap<>();
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
     * Registers an indirect font whose definition lives in a separate
     * indirect PDF object.
     *
     * <p>Alias generation uses the same {@code nextIndex} counter as
     * {@link #getAlias} to preserve deterministic numbering.
     *
     * @param fontName     logical font name
     * @param objectNumber the indirect object number holding the font definition
     * @return the alias assigned (e.g. {@code "F3"})
     */
    public String registerIndirectFont(String fontName, int objectNumber) {
        String existing = nameToAlias.get(fontName);
        if (existing != null) {
            indirectFontObjects.put(existing, objectNumber);
            return existing;
        }
        String alias = "F" + nextIndex++;
        nameToAlias.put(fontName, alias);
        aliasToName.put(alias, fontName);
        indirectFontObjects.put(alias, objectNumber);
        return alias;
    }

    /**
     * Returns {@code true} if the given alias refers to an indirect font
     * (registered via {@link #registerIndirectFont}).
     */
    public boolean isIndirect(String alias) {
        return indirectFontObjects.containsKey(alias);
    }

    /**
     * Returns the indirect object number for the given alias.
     *
     * @throws IllegalArgumentException if the alias is not an indirect font
     */
    public int getIndirectObjectNumber(String alias) {
        Integer objNum = indirectFontObjects.get(alias);
        if (objNum == null) {
            throw new IllegalArgumentException("not an indirect font alias: " + alias);
        }
        return objNum;
    }

    /**
     * Returns an unmodifiable, insertion-ordered view of all alias → font-name
     * mappings currently held by this registry.
     */
    public Map<String, String> aliasToNameView() {
        return Collections.unmodifiableMap(aliasToName);
    }
}
