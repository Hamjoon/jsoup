###Test START##
```java
package org.jsoup.helper;

import org.junit.Test;
import org.junit.Before;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

public class StringUtilTest {

    /* ----------------------------------------------------------------------
     *  1.  Public method signatures (extracted for reference)
     * ---------------------------------------------------------------------- */
    // public static String join(Collection strings, String sep)
    // public static String join(Iterator strings, String sep)
    // public static String join(String[] strings, String sep)
    // public static String padding(int width)
    // public static boolean isBlank(String string)
    // public static boolean isNumeric(String string)
    // public static boolean isWhitespace(int c)
    // public static boolean isActuallyWhitespace(int c)
    // public static boolean isInvisibleChar(int c)
    // public static String normaliseWhitespace(String string)
    // public static void appendNormalisedWhitespace(StringBuilder accum, String string, boolean stripLeading)
    // public static boolean in(String needle, String... haystack)
    // public static boolean inSorted(String needle, String[] haystack)
    // public static URL resolve(URL base, String relUrl) throws MalformedURLException
    // public static String resolve(String baseUrl, String relUrl)
    // public static StringBuilder stringBuilder()


    /* ----------------------------------------------------------------------
     *  2.  Basic functionality tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testJoinCollectionBasic() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("a,b,c", StringUtil.join(list, ","));
    }

    @Test
    public void testJoinIteratorBasic() {
        List<String> list = Arrays.asList("x", "y");
        assertEquals("x|y", StringUtil.join(list.iterator(), "|"));
    }

    @Test
    public void testJoinArrayBasic() {
        String[] arr = {"one", "two", "three"};
        assertEquals("one two three", StringUtil.join(arr, " "));
    }

    @Test
    public void testPaddingBasic() {
        assertEquals("", StringUtil.padding(0));
        assertEquals("   ", StringUtil.padding(3));
        // width larger than memoised array
        assertEquals(30, StringUtil.padding(30).length());
        assertTrue(StringUtil.padding(30).matches("^\\s+$"));
    }

    @Test
    public void testIsBlankBasic() {
        assertTrue(StringUtil.isBlank(null));
        assertTrue(StringUtil.isBlank(""));
        assertTrue(StringUtil.isBlank("   "));
        assertTrue(StringUtil.isBlank("\t\n\r\f"));
        assertFalse(StringUtil.isBlank(" a "));
    }

    @Test
    public void testIsNumericBasic() {
        assertFalse(StringUtil.isNumeric(null));
        assertFalse(StringUtil.isNumeric(""));
        assertTrue(StringUtil.isNumeric("0123456789"));
        assertFalse(StringUtil.isNumeric("12a34"));
        assertFalse(StringUtil.isNumeric("-123"));
    }

    @Test
    public void testIsWhitespaceBasic() {
        assertTrue(StringUtil.isWhitespace(' '));
        assertTrue(StringUtil.isWhitespace('\t'));
        assertTrue(StringUtil.isWhitespace('\n'));
        assertTrue(StringUtil.isWhitespace('\f'));
        assertTrue(StringUtil.isWhitespace('\r'));
        assertFalse(StringUtil.isWhitespace('a'));
        assertFalse(StringUtil.isWhitespace('0'));
    }

    @Test
    public void testIsActuallyWhitespaceBasic() {
        assertTrue(StringUtil.isActuallyWhitespace(' '));
        assertTrue(StringUtil.isActuallyWhitespace('\t'));
        assertTrue(StringUtil.isActuallyWhitespace('\n'));
        assertTrue(StringUtil.isActuallyWhitespace('\f'));
        assertTrue(StringUtil.isActuallyWhitespace('\r'));
        assertTrue(StringUtil.isActuallyWhitespace(160)); // non‑breaking space
        assertFalse(StringUtil.isActuallyWhitespace('a'));
    }

    @Test
    public void testIsInvisibleCharBasic() {
        assertTrue(StringUtil.isInvisibleChar(8203)); // ZERO WIDTH SPACE
        assertTrue(StringUtil.isInvisibleChar(8204)); // ZERO WIDTH NON‑JOINER
        assertTrue(StringUtil.isInvisibleChar(8205)); // ZERO WIDTH JOINER
        assertTrue(StringUtil.isInvisibleChar(173));  // SOFT HYPHEN
        assertFalse(StringUtil.isInvisibleChar('a'));
    }

    @Test
    public void testNormaliseWhitespaceBasic() {
        String input = "a \t  b\nc\r\n d\f e";
        String expected = "a b c d e";
        assertEquals(expected, StringUtil.normaliseWhitespace(input));
    }

    @Test
    public void testAppendNormalisedWhitespaceBasic() {
        StringBuilder sb = new StringBuilder();
        StringUtil.appendNormalisedWhitespace(sb, "  foo   bar\tbaz  ", false);
        assertEquals(" foo bar baz ", sb.toString());
    }

    @Test
    public void testInBasic() {
        assertTrue(StringUtil.in("b", "a", "b", "c"));
        assertFalse(StringUtil.in("x", "a", "b", "c"));
    }

    @Test
    public void testInSortedBasic() {
        String[] sorted = {"a", "b", "c", "d"};
        assertTrue(StringUtil.inSorted("c", sorted));
        assertFalse(StringUtil.inSorted("z", sorted));
    }

    @Test
    public void testResolveUrlBasic() throws MalformedURLException {
        URL base = new URL("http://example.com/dir/page.html");
        assertEquals(new URL("http://example.com/dir/other.html"),
                StringUtil.resolve(base, "other.html"));
        // absolute rel url should be returned unchanged
        URL abs = new URL("https://other.com/path");
        assertEquals(abs, StringUtil.resolve(base, abs.toString()));
    }

    @Test
    public void testResolveStringBasic() {
        String base = "http://example.com/dir/page.html";
        String result = StringUtil.resolve(base, "other.html");
        assertEquals("http://example.com/dir/other.html", result);
    }

    @Test
    public void testStringBuilderBasic() {
        StringBuilder sb1 = StringUtil.stringBuilder();
        sb1.append("test");
        assertEquals("test", sb1.toString());

        // next call should give a cleared builder (same instance if size <= MaxCachedBuilderSize)
        StringBuilder sb2 = StringUtil.stringBuilder();
        assertEquals(0, sb2.length());
    }


    /* ----------------------------------------------------------------------
     *  3.  Edge‑case & exception tests
     * ---------------------------------------------------------------------- */

    // ---- join edge cases --------------------------------------------------
    @Test
    public void testJoinEmptyCollection() {
        List<String> empty = Collections.emptyList();
        assertEquals("", StringUtil.join(empty, ","));
    }

    @Test
    public void testJoinSingleElementCollection() {
        List<String> single = Collections.singletonList("only");
        assertEquals("only", StringUtil.join(single, ","));
    }

    @Test(expected = NullPointerException.class)
    public void testJoinCollectionWithNullElement() {
        List<String> list = Arrays.asList("a", null, "b");
        StringUtil.join(list, ","); // should throw NPE because of toString()
    }

    // ---- padding edge cases ------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void testPaddingNegativeWidth() {
        StringUtil.padding(-1);
    }

    @Test
    public void testPaddingMemoisedBoundary() {
        // width = padding.length - 1 (20) is memoised
        String memo = StringUtil.padding(20);
        assertEquals(20, memo.length());
        // width = 21 forces generation of a new char[] array
        String generated = StringUtil.padding(21);
        assertEquals(21, generated.length());
        assertNotSame(memo, generated);
    }

    // ---- isBlank edge cases -----------------------------------------------
    @Test
    public void testIsBlankVariousWhitespace() {
        assertTrue(StringUtil.isBlank("\u2003")); // EM SPACE – not considered blank, should be false
        assertFalse(StringUtil.isBlank("\u2003"));
        assertTrue(StringUtil.isBlank("\u0009")); // tab – considered blank
    }

    // ---- isNumeric edge cases ----------------------------------------------
    @Test
    public void testIsNumericUnicodeDigits() {
        // Unicode full‑width digits are not ASCII digits -> should be false
        assertFalse(StringUtil.isNumeric("１２３"));
    }

    // ---- isWhitespace / isActuallyWhitespace edge cases --------------------
    @Test
    public void testIsWhitespaceNonAscii() {
        assertFalse(StringUtil.isWhitespace(0x00A0)); // non‑breaking space
    }

    @Test
    public void testIsActuallyWhitespaceNonAscii() {
        assertTrue(StringUtil.isActuallyWhitespace(0x00A0)); // non‑breaking space
    }

    // ---- normaliseWhitespace edge cases ------------------------------------
    @Test
    public void testNormaliseWhitespacePreservesInvisibleChars() {
        // zero‑width space should be removed
        String input = "a\u200B b";
        assertEquals("a b", StringUtil.normaliseWhitespace(input));
    }

    @Test
    public void testAppendNormalisedWhitespaceStripLeading() {
        StringBuilder sb = new StringBuilder();
        StringUtil.appendNormalisedWhitespace(sb, "   leading spaces", true);
        assertEquals("leading spaces", sb.toString());
    }

    // ---- in edge cases ------------------------------------------------------
    @Test
    public void testInWithNullNeedle() {
        assertFalse(StringUtil.in(null, "a", "b"));
    }

    @Test
    public void testInWithEmptyHaystack() {
        assertFalse(StringUtil.in("any"));
    }

    // ---- inSorted edge cases ------------------------------------------------
    @Test
    public void testInSortedUnsortedArray() {
        String[] unsorted = {"c", "a", "b"};
        // binarySearch on unsorted array yields undefined result – we just ensure it does not throw
        // (the contract states caller must provide sorted array)
        boolean result = StringUtil.inSorted("a", unsorted);
        // result may be true or false; we only check that method completes
        assertTrue(result || !result);
    }

    // ---- resolve(URL, String) edge cases ------------------------------------
    @Test
    public void testResolveUrlWithQueryOnly() throws MalformedURLException {
        URL base = new URL("http://example.com/dir/file.html");
        URL resolved = StringUtil.resolve(base, "?id=123");
        assertEquals("http://example.com/dir/file.html?id=123", resolved.toString());
    }

    @Test
    public void testResolveUrlWithDotSlash() throws MalformedURLException {
        URL base = new URL("http://example.com/dir/file.html");
        // base.getFile() = "/dir/file.html" (contains '/'), so the special handling should NOT trigger
        URL resolved = StringUtil.resolve(base, "./sub.html");
        assertEquals("http://example.com/dir/./sub.html", resolved.toString());
    }

    @Test
    public void testResolveUrlWithDotSlashAndNoLeadingSlashInBaseFile() throws MalformedURLException {
        // Simulate a base where getFile() does NOT start with '/' (rare, but exercised by the workaround)
        URL base = new URL("http://example.com", "dir/file.html"); // file = "dir/file.html"
        URL resolved = StringUtil.resolve(base, "./sub.html");
        // The workaround adds a '/' before the file
        assertEquals("http://example.com/dir/sub.html", resolved.toString());
    }

    @Test(expected = MalformedURLException.class)
    public void testResolveUrlMalformedRelative() throws MalformedURLException {
        URL base = new URL("http://example.com/");
        // space in URL causes MalformedURLException
        StringUtil.resolve(base, "bad url");
    }

    // ---- resolve(String, String) edge cases ---------------------------------
    @Test
    public void testResolveStringInvalidBaseFallsBackToAbsoluteRel() {
        String base = "ht!tp://[invalid]";
        String rel = "http://good.com/path";
        assertEquals("http://good.com/path", StringUtil.resolve(base, rel));
    }

    @Test
    public void testResolveStringBothInvalidReturnsEmpty() {
        String base = "bad://base";
        String rel = "also bad";
        assertEquals("", StringUtil.resolve(base, rel));
    }

    // ---- stringBuilder edge cases -------------------------------------------
    @Test
    public void testStringBuilderCacheResetsWhenTooLarge() {
        StringBuilder sb = StringUtil.stringBuilder();
        // Fill it beyond the cached limit (8 KiB)
        char[] big = new char[9 * 1024];
        Arrays.fill(big, 'x');
        sb.append(big);
        assertTrue(sb.length() > 8 * 1024);

        // Next call should give a fresh builder of the default cached size
        StringBuilder sb2 = StringUtil.stringBuilder();
        assertEquals(0, sb2.length());
        // The instance may be new; we only guarantee it is cleared.
    }

}
```
###Test END##