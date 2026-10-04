###Test START##
```java
package org.jsoup.internal;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Normalizer}.
 * Covers normal usage, edge‑cases and locale‑specific behaviour.
 */
public class NormalizerTest {

    /* ---------------------------------------------------------------------- */
    /* lowerCase(String) tests                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testLowerCaseBasic() {
        assertEquals("hello world", Normalizer.lowerCase("Hello World"));
    }

    @Test
    public void testLowerCaseEmptyString() {
        assertEquals("", Normalizer.lowerCase(""));
    }

    @Test
    public void testLowerCaseNullInput() {
        // According to the implementation, null should return an empty string
        assertEquals("", Normalizer.lowerCase(null));
    }

    @Test
    public void testLowerCaseMixedCase() {
        assertEquals("jsoup", Normalizer.lowerCase("JsOuP"));
    }

    @Test
    public void testLowerCaseSpecialCharacters() {
        // Non‑alphabetic characters must be left untouched
        assertEquals("123!@# abc", Normalizer.lowerCase("123!@# ABC"));
    }

    @Test
    public void testLowerCaseLocaleSpecificCharacters() {
        // Using characters that behave differently in other locales (e.g., Turkish İ)
        // Locale.ENGLISH must be used, so 'İ' (U+0130) becomes 'i' + combining dot above,
        // which in Java results in "i̇". We just check that the conversion follows ENGLISH.
        String input = "\u0130stanbul"; // 'İstanbul' with capital dotted I
        String expected = "\u0069\u0307stanbul"; // 'i' + COMBINING DOT ABOVE + "stanbul"
        assertEquals(expected, Normalizer.lowerCase(input));
    }

    /* ---------------------------------------------------------------------- */
    /* normalize(String) tests                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNormalizeBasic() {
        // lower‑cases and trims whitespace
        assertEquals("hello world", Normalizer.normalize("  HeLLo WoRLd  "));
    }

    @Test
    public void testNormalizeEmptyString() {
        assertEquals("", Normalizer.normalize(""));
    }

    @Test
    public void testNormalizeNullInput() {
        // Null should be treated as empty string, then trimmed (still empty)
        assertEquals("", Normalizer.normalize(null));
    }

    @Test
    public void testNormalizeOnlyWhitespace() {
        // After trimming, result should be empty
        assertEquals("", Normalizer.normalize("   \t\n  "));
    }

    @Test
    public void testNormalizeMixedContent() {
        // Mixed letters, numbers and symbols, with surrounding spaces
        String input = "  123AbC!@#  ";
        String expected = "123abc!@#";
        assertEquals(expected, Normalizer.normalize(input));
    }

    @Test
    public void testNormalizeLocaleSpecificCharacters() {
        // Same logic as lowerCase test – ensure the locale used is ENGLISH
        String input = " \u0130stanbul ";
        String expected = "\u0069\u0307stanbul"; // lower‑cased, trimmed
        assertEquals(expected, Normalizer.normalize(input));
    }
}
```
###Test END##