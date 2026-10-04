package org.jsoup.helper;

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Comprehensive JUnit 4 tests for {@link StringUtil}.
 */
public class StringUtilTest {

    /* --------------------------------------------------------------------- */
    /* join() tests                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void join_Collection_SingleElement() {
        Collection<String> col = Arrays.asList("one");
        assertEquals("one", StringUtil.join(col, ","));
    }

    @Test
    public void join_Collection_MultipleElements() {
        Collection<String> col = Arrays.asList("one", "two", "three");
        assertEquals("one|two|three", StringUtil.join(col, "|"));
    }

    @Test
    public void join_Collection_Empty() {
        Collection<String> col = new ArrayList<>();
        assertEquals("", StringUtil.join(col, ","));
    }

    @Test
    public void join_Iterator_MultipleElements() {
        List<String> list = Arrays.asList("a", "b", "c");
        Iterator<String> it = list.iterator();
        assertEquals("a b c", StringUtil.join(it, " "));
    }

    @Test
    public void join_Array_MultipleElements() {
        String[] arr = {"x", "y", "z"};
        assertEquals("x-y-z", StringUtil.join(arr, "-"));
    }

    @Test
    public void join_Array_Empty() {
        String[] arr = {};
        assertEquals("", StringUtil.join(arr, ","));
    }

    /* --------------------------------------------------------------------- */
    /* padding() tests                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void padding_MemoisedValues() {
        for (int i = 0; i < StringUtil.padding.length; i++) {
            String p = StringUtil.padding(i);
            assertEquals(i, p.length());
            for (char c : p.toCharArray()) {
                assertEquals(' ', c);
            }
        }
    }

    @Test
    public void padding_LargeWidth() {
        int width = 50; // greater than memoised array size (21)
        String p = StringUtil.padding(width);
        assertEquals(width, p.length());
        assertTrue(p.matches("\\s+"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void padding_NegativeWidth() {
        StringUtil.padding(-1);
    }

    /* --------------------------------------------------------------------- */
    /* isBlank() tests                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void isBlank_NullOrEmpty() {
        assertTrue(StringUtil.isBlank(null));
        assertTrue(StringUtil.isBlank(""));
    }

    @Test
    public void isBlank_WhitespaceOnly() {
        assertTrue(StringUtil.isBlank("   "));
        assertTrue(StringUtil.isBlank("\t\r\n"));
        assertTrue(StringUtil.isBlank("\f"));
        // mixture
        assertTrue(StringUtil.isBlank(" \t \n\r\f "));
    }

    @Test
    public void isBlank_NonBlank() {
        assertFalse(StringUtil.isBlank(" a "));
        assertFalse(StringUtil.isBlank("foo"));
        assertFalse(StringUtil.isBlank("\u00A0")); // non‑breaking space is NOT considered blank by isBlank
    }

    /* --------------------------------------------------------------------- */
    /* isNumeric() tests                                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void isNumeric_NullOrEmpty() {
        assertFalse(StringUtil.isNumeric(null));
        assertFalse(StringUtil.isNumeric(""));
    }

    @Test
    public void isNumeric_ValidDigits() {
        assertTrue(StringUtil.isNumeric("0"));
        assertTrue(StringUtil.isNumeric("1234567890"));
    }

    @Test
    public void isNumeric_WithNonDigits() {
        assertFalse(StringUtil.isNumeric("12a3"));
        assertFalse(StringUtil.isNumeric(" 123"));
        assertFalse(StringUtil.isNumeric("-123"));
    }

    /* --------------------------------------------------------------------- */
    /* Whitespace related tests                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void isWhitespace_SpecDefined() {
        assertTrue(StringUtil.isWhitespace(' '));
        assertTrue(StringUtil.isWhitespace('\t'));
        assertTrue(StringUtil.isWhitespace('\n'));
        assertTrue(StringUtil.isWhitespace('\f'));
        assertTrue(StringUtil.isWhitespace('\r'));
        assertFalse(StringUtil.isWhitespace('a'));
        assertFalse(StringUtil.isWhitespace('\u00A0')); // NBSP not spec whitespace
    }

    @Test
    public void isActuallyWhitespace_IncludesNbsp() {
        assertTrue(StringUtil.isActuallyWhitespace(' '));
        assertTrue(StringUtil.isActuallyWhitespace('\t'));
        assertTrue(StringUtil.isActuallyWhitespace('\n'));
        assertTrue(StringUtil.isActuallyWhitespace('\f'));
        assertTrue(StringUtil.isActuallyWhitespace('\r'));
        assertTrue(StringUtil.isActuallyWhitespace(160)); // &nbsp;
        assertFalse(StringUtil.isActuallyWhitespace('a'));
    }

    @Test
    public void isInvisibleChar_KnownCharacters() {
        assertTrue(StringUtil.isInvisibleChar(8203)); // zero width space
        assertTrue(StringUtil.isInvisibleChar(8204)); // zero width non‑joiner
        assertTrue(StringUtil.isInvisibleChar(8205)); // zero width joiner
        assertTrue(StringUtil.isInvisibleChar(173));  // soft hyphen
        assertFalse(StringUtil.isInvisibleChar('a'));
        assertFalse(StringUtil.isInvisibleChar(32)); // regular space
    }

    /* --------------------------------------------------------------------- */
    /* normaliseWhitespace() and appendNormalisedWhitespace() tests          */
    /* --------------------------------------------------------------------- */

    @Test
    public void normaliseWhitespace_CollapsesSpaces() {
        assertEquals("a b c", StringUtil.normaliseWhitespace("a   b\tc\n\n d"));
    }

    @Test
    public void normaliseWhitespace_LeadingAndTrailing() {
        assertEquals("a b", StringUtil.normaliseWhitespace("   a   b   "));
    }

    @Test
    public void normaliseWhitespace_WithInvisibleChars() {
        // invisible chars should be omitted
        String input = "a" + (char)8203 + "b" + (char)173 + "c";
        assertEquals("abc", StringUtil.normaliseWhitespace(input));
    }

    @Test
    public void appendNormalisedWhitespace_StripLeadingTrue() {
        StringBuilder sb = new StringBuilder();
        StringUtil.appendNormalisedWhitespace(sb, "   foo   bar", true);
        assertEquals("foo bar", sb.toString());
    }

    @Test
    public void appendNormalisedWhitespace_StripLeadingFalse() {
        StringBuilder sb = new StringBuilder();
        StringUtil.appendNormalisedWhitespace(sb, "   foo   bar", false);
        assertEquals(" foo bar", sb.toString());
    }

    @Test
    public void appendNormalisedWhitespace_MultipleCallsReuseBuilder() {
        StringBuilder sb1 = StringUtil.stringBuilder();
        sb1.append("first");
        StringBuilder sb2 = StringUtil.stringBuilder(); // should be same instance cleared
        assertSame(sb1, sb2);
        assertEquals(0, sb2.length());
    }

    /* --------------------------------------------------------------------- */
    /* in() and inSorted() tests                                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void in_FoundAndNotFound() {
        assertTrue(StringUtil.in("b", "a", "b", "c"));
        assertFalse(StringUtil.in("x", "a", "b", "c"));
    }

    @Test
    public void inSorted_FoundAndNotFound() {
        String[] sorted = {"a", "b", "c", "d"};
        assertTrue(StringUtil.inSorted("c", sorted));
        assertFalse(StringUtil.inSorted("z", sorted));
    }

    /* --------------------------------------------------------------------- */
    /* resolve(URL, String) tests                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void resolve_RelativePath() throws MalformedURLException {
        URL base = new URL("http://example.com/dir/page.html");
        URL resolved = StringUtil.resolve(base, "other.html");
        assertEquals("http://example.com/dir/other.html", resolved.toExternalForm());
    }

    @Test
    public void resolve_QueryStringOnly() throws MalformedURLException {
        URL base = new URL("http://example.com/dir/page.html");
        URL resolved = StringUtil.resolve(base, "?id=5");
        assertEquals("http://example.com/dir/page.html?id=5", resolved.toExternalForm());
    }

    @Test
    public void resolve_DotRelative_WithFilePathAdjustment() throws MalformedURLException {
        URL base = new URL("http://example.com/dir/file");
        URL resolved = StringUtil.resolve(base, "./sub");
        assertEquals("http://example.com/dir/sub", resolved.toExternalForm());
    }

    @Test
    public void resolve_AbsoluteRelUrl() throws MalformedURLException {
        URL base = new URL("http://example.com/base/");
        URL resolved = StringUtil.resolve(base, "http://other.com/xyz");
        assertEquals("http://other.com/xyz", resolved.toExternalForm());
    }

    /* --------------------------------------------------------------------- */
    /* resolve(String, String) tests                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void resolveString_ValidBaseAndRelative() {
        String result = StringUtil.resolve("http://example.com/dir/", "sub/page.html");
        assertEquals("http://example.com/dir/sub/page.html", result);
    }

    @Test
    public void resolveString_InvalidBaseButAbsoluteRel() {
        String result = StringUtil.resolve("not a url", "http://good.com/");
        assertEquals("http://good.com/", result);
    }

    @Test
    public void resolveString_BothInvalid_ReturnsEmpty() {
        String result = StringUtil.resolve("not a url", "also not a url");
        assertEquals("", result);
    }

    @Test
    public void resolveString_RelativeStartsWithQuery() {
        String result = StringUtil.resolve("http://example.com/dir/page.html", "?foo=bar");
        assertEquals("http://example.com/dir/page.html?foo=bar", result);
    }

    /* --------------------------------------------------------------------- */
    /* stringBuilder() cache behavior test                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void stringBuilder_CacheRespectsMaxSize() {
        // Obtain builder and grow it beyond the cache limit
        StringBuilder sb = StringUtil.stringBuilder();
        int limit = 8 * 1024;
        for (int i = 0; i < limit + 100; i++) {
            sb.append('x');
        }
        // Next call should give a fresh builder (length 0) but still respect max size
        StringBuilder sb2 = StringUtil.stringBuilder();
        assertEquals(0, sb2.length());
        // The instance may be new; we only guarantee it's functional and empty
        sb2.append("ok");
        assertEquals("ok", sb2.toString());
    }
}
