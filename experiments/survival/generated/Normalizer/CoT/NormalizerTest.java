package org.jsoup.internal;

import org.junit.Test;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link Normalizer}.
 * 
 * The tests cover:
 * <ul>
 *   <li>Basic functionality of each public method.</li>
 *   <li>Edge‑case handling such as {@code null}, empty strings, whitespace only strings, 
 *       and locale‑sensitive characters.</li>
 *   <li>Verification that no unexpected exceptions are thrown.</li>
 * </ul>
 */
public class NormalizerTest {

    /* --------------------------------------------------------------------- */
    /* 1.  Public method signatures                                           */
    /* --------------------------------------------------------------------- */
    // public static String lowerCase(final String input)
    // public static String normalize(final String input)

    /* --------------------------------------------------------------------- */
    /* 2.  Basic functionality tests                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void lowerCase_basicMixedCase() {
        String input = "JsOuP TeSt";
        String expected = "jsoup test";
        assertEquals(expected, Normalizer.lowerCase(input));
    }

    @Test
    public void lowerCase_alreadyLowerCase() {
        String input = "already lower";
        assertEquals(input, Normalizer.lowerCase(input));
    }

    @Test
    public void lowerCase_emptyString() {
        assertEquals("", Normalizer.lowerCase(""));
    }

    @Test
    public void lowerCase_nullInput() {
        // Contract: null input should yield an empty string, never null.
        assertEquals("", Normalizer.lowerCase(null));
    }

    @Test
    public void normalize_basicTrimAndLower() {
        String input = "  JsOuP TeSt  ";
        String expected = "jsoup test";
        assertEquals(expected, Normalizer.normalize(input));
    }

    @Test
    public void normalize_noWhitespace() {
        String input = "NoSpace";
        assertEquals("nospace", Normalizer.normalize(input));
    }

    @Test
    public void normalize_emptyString() {
        assertEquals("", Normalizer.normalize(""));
    }

    @Test
    public void normalize_nullInput() {
        // null -> lowerCase returns "", trim on "" stays ""
        assertEquals("", Normalizer.normalize(null));
    }

    @Test
    public void normalize_whitespaceOnly() {
        assertEquals("", Normalizer.normalize("   \t\n  "));
    }

    /* --------------------------------------------------------------------- */
    /* 3.  Edge‑case and locale‑sensitive tests                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void lowerCase_localeSensitiveCharacters() {
        // Turkish dotted capital I (U+0130) should be lower‑cased according to EN locale,
        // resulting in 'i' followed by a combining dot above (U+0307).
        String input = "İstanbul";
        String expected = input.toLowerCase(Locale.ENGLISH); // delegate to JDK for expected value
        assertEquals(expected, Normalizer.lowerCase(input));
    }

    @Test
    public void lowerCase_nonAlphabeticCharacters() {
        String input = "1234!@# $%^&*()_+";
        // Non‑alphabetic characters are unchanged.
        assertEquals(input, Normalizer.lowerCase(input));
    }

    @Test
    public void normalize_preservesInternalWhitespace() {
        String input = "  Multiple   Spaces\tBetween Words  ";
        // Only leading/trailing whitespace is trimmed; internal spacing stays as‑is,
        // and all letters are lower‑cased.
        String expected = "multiple   spaces\tbetween words";
        assertEquals(expected, Normalizer.normalize(input));
    }

    @Test
    public void normalize_newLineAndTabCharacters() {
        String input = "\n\t  Mixed \tWhitespace\n";
        String expected = "mixed \twhitespace";
        assertEquals(expected, Normalizer.normalize(input));
    }

    @Test
    public void lowerCase_immutableInput() {
        String input = "Immutable";
        String copy = new String(input); // ensure a distinct object
        Normalizer.lowerCase(input);
        // The original string must remain unchanged.
        assertSame(copy, input);
        assertEquals("immutable", Normalizer.lowerCase(input));
    }

    @Test
    public void normalize_immutableInput() {
        String input = "  ImmutableTest  ";
        String copy = new String(input);
        Normalizer.normalize(input);
        assertSame(copy, input);
        assertEquals("immutabltest", Normalizer.normalize(input));
    }

    /* --------------------------------------------------------------------- */
    /* 4.  Exception safety (no exceptions should be thrown)                */
    /* --------------------------------------------------------------------- */

    @Test
    public void lowerCase_neverThrowsException() {
        try {
            Normalizer.lowerCase(null);
            Normalizer.lowerCase("");
            Normalizer.lowerCase("AnyString");
        } catch (Exception e) {
            fail("lowerCase should not throw any exception, but threw: " + e);
        }
    }

    @Test
    public void normalize_neverThrowsException() {
        try {
            Normalizer.normalize(null);
            Normalizer.normalize("");
            Normalizer.normalize("Any String");
        } catch (Exception e) {
            fail("normalize should not throw any exception, but threw: " + e);
        }
    }
}
