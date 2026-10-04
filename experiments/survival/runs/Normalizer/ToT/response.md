###Test START##
```java
package org.jsoup.internal;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Normalizer}.
 *
 * The suite covers:
 * <ul>
 *   <li>Typical use cases (regular strings, mixed case, Unicode characters)</li>
 *   <li>Edge cases (null, empty string, whitespace‑only strings, leading/trailing whitespace)</li>
 *   <li>Locale‑specific behaviour (ensuring ENGLISH locale is used, e.g. Turkish “İ”)</li>
 * </ul>
 */
public class NormalizerTest {

    /* --------------------------------------------------------------------- *
     * lowerCase(String) tests
     * --------------------------------------------------------------------- */

    @Test
    public void lowerCase_NullInput_ReturnsEmptyString() {
        assertEquals("", Normalizer.lowerCase(null));
    }

    @Test
    public void lowerCase_EmptyString_ReturnsEmptyString() {
        assertEquals("", Normalizer.lowerCase(""));
    }

    @Test
    public void lowerCase_MixedCase_ReturnsAllLowerCase() {
        String input = "JsOuP TeStInG 123";
        String expected = "jsoup testing 123";
        assertEquals(expected, Normalizer.lowerCase(input));
    }

    @Test
    public void lowerCase_UnicodeCharacters_AreLowerCasedCorrectly() {
        // Greek, Cyrillic, and German ß
        String input = "ÄÖÜ ß Δέλτα κόσμε";
        String expected = "äöü ß δέλτα κόσμε";
        assertEquals(expected, Normalizer.lowerCase(input));
    }

    @Test
    public void lowerCase_TurkishLocaleInsensitive_UsesEnglishLocale() {
        // The Turkish capital I (U+0130) should become a normal 'i' when using ENGLISH locale
        String input = "\u0130stanbul"; // "İstanbul"
        String expected = "i̇stanbul"; // lower‑case i + combining dot above
        assertEquals(expected, Normalizer.lowerCase(input));
    }

    /* --------------------------------------------------------------------- *
     * normalize(String) tests
     * --------------------------------------------------------------------- */

    @Test
    public void normalize_NullInput_ReturnsEmptyString() {
        assertEquals("", Normalizer.normalize(null));
    }

    @Test
    public void normalize_EmptyString_ReturnsEmptyString() {
        assertEquals("", Normalizer.normalize(""));
    }

    @Test
    public void normalize_WhitespaceOnlyString_ReturnsEmptyString() {
        assertEquals("", Normalizer.normalize("   \t\n  "));
    }

    @Test
    public void normalize_LeadingAndTrailingWhitespace_IsTrimmed() {
        String input = "   Jsoup   Testing   ";
        String expected = "jsoup   testing";
        assertEquals(expected, Normalizer.normalize(input));
    }

    @Test
    public void normalize_InternalWhitespace_IsPreserved() {
        String input = "Jsoup   \t  Testing";
        String expected = "jsoup   \t  testing";
        assertEquals(expected, Normalizer.normalize(input));
    }

    @Test
    public void normalize_MixedUnicodeAndWhitespace() {
        // Contains German ß and leading/trailing spaces
        String input = "  ßÄÖÜ  ";
        String expected = "ßäöü";
        assertEquals(expected, Normalizer.normalize(input));
    }

    @Test
    public void normalize_TurkishI_IsLowerCasedAndTrimmed() {
        String input = "\u0130stanbul   ";
        String expected = "i̇stanbul";
        assertEquals(expected, Normalizer.normalize(input));
    }
}
```
###Test END##