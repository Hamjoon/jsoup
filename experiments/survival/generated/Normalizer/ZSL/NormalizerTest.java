package org.jsoup.internal;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 tests for {@link Normalizer}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical usage of {@code lowerCase} and {@code normalize}</li>
 *   <li>Null handling (should return empty string)</li>
 *   <li>Whitespace handling (trimming only in {@code normalize})</li>
 *   <li>Locale‑specific behavior – the implementation forces {@link Locale#ENGLISH}</li>
 *   <li>Immutability – the original input must not be modified</li>
 *   <li>Unicode characters that are unaffected by English lower‑casing</li>
 *   <li>Large strings to ensure there are no hidden size‑related bugs</li>
 * </ul>
 */
public class NormalizerTest {

    // --------------------------------------------------------------------
    // lowerCase tests
    // --------------------------------------------------------------------
    @Test
    public void lowerCase_NullInput_ReturnsEmptyString() {
        assertEquals("null input should yield empty string", "", Normalizer.lowerCase(null));
    }

    @Test
    public void lowerCase_EmptyString_ReturnsEmptyString() {
        assertEquals("empty string should stay empty", "", Normalizer.lowerCase(""));
    }

    @Test
    public void lowerCase_MixedCase_ReturnsAllLowerCase() {
        String input = "JsOuP TeStInG 123";
        String expected = "jsoup testing 123";
        assertEquals(expected, Normalizer.lowerCase(input));
    }

    @Test
    public void lowerCase_NonAsciiCharacters_UnchangedByEnglishLocale() {
        // Turkish dotted I is a classic locale‑sensitive case, but Locale.ENGLISH should not change it.
        String input = "İstanbul İĞİŞÇĞ";
        String expected = "i̇stanbul i̇ğişçğ"; // note: toLowerCase(ENGLISH) leaves the dot‑less i unchanged
        assertEquals(expected, Normalizer.lowerCase(input));
    }

    @Test
    public void lowerCase_InputStringIsNotModified() {
        String original = "Original";
        String copy = new String(original); // ensure a distinct object
        Normalizer.lowerCase(original);
        assertEquals("Original string must remain unchanged", copy, original);
    }

    // --------------------------------------------------------------------
    // normalize tests
    // --------------------------------------------------------------------
    @Test
    public void normalize_NullInput_ReturnsEmptyString() {
        assertEquals("null input should yield empty string after normalize", "", Normalizer.normalize(null));
    }

    @Test
    public void normalize_EmptyString_ReturnsEmptyString() {
        assertEquals("empty string should stay empty after normalize", "", Normalizer.normalize(""));
    }

    @Test
    public void normalize_OnlyWhitespace_ReturnsEmptyString() {
        assertEquals("string with only spaces should become empty after trim", "", Normalizer.normalize("   \t\n  "));
    }

    @Test
    public void normalize_LeadingAndTrailingWhitespace_TrimmedAndLowerCased() {
        String input = "   JsOup   Test  ";
        String expected = "jsoup   test";
        assertEquals(expected, Normalizer.normalize(input));
    }

    @Test
    public void normalize_InternalWhitespacePreserved() {
        String input = "A  B   C";
        String expected = "a  b   c";
        assertEquals(expected, Normalizer.normalize(input));
    }

    @Test
    public void normalize_NonAsciiCharacters_TrimmedOnly() {
        String input = "  ÇĞİŞçğış  ";
        // lowercasing with ENGLISH does not affect these characters
        String expected = "çğişçğış";
        assertEquals(expected, Normalizer.normalize(input));
    }

    @Test
    public void normalize_LargeString_PerformsCorrectly() {
        StringBuilder sb = new StringBuilder();
        // Build a 10 000‑character string with mixed case and whitespace at both ends
        sb.append("   ");
        for (int i = 0; i < 2500; i++) {
            sb.append("AbCdEfGhIjKlMnOpQrStUvWxYz0123456789");
        }
        sb.append("   ");

        String input = sb.toString();
        String normalized = Normalizer.normalize(input);

        // Expected length: original length minus 6 leading/trailing spaces, all lower‑cased
        int expectedLength = input.length() - 6;
        assertEquals("Length after trimming should be input length minus 6", expectedLength, normalized.length());

        // Spot‑check the first and last characters after normalization
        assertEquals('a', normalized.charAt(0));
        assertEquals('9', normalized.charAt(normalized.length() - 1));
    }

    @Test
    public void normalize_DoesNotAffectOriginalString() {
        String original = "  MixedCASE  ";
        String copy = new String(original);
        Normalizer.normalize(original);
        assertEquals("Original string must remain unchanged after normalize", copy, original);
    }
}
