###Test START##
```java
package org.jsoup.helper;

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link StringUtil}.
 *
 * The tests cover:
 * <ul>
 *   <li>All overloads of {@code join}</li>
 *   <li>{@code padding}</li>
 *   <li>{@code isBlank}, {@code isNumeric}</li>
 *   <li>Whitespace helpers {@code isWhitespace},
 *       {@code isActuallyWhitespace}, {@code isInvisibleChar}</li>
 *   <li>Whitespace normalisation utilities</li>
 *   <li>Membership helpers {@code in} and {@code inSorted}</li>
 *   <li>URL resolution (both {@code URL} and {@code String} overloads)</li>
 *   <li>Thread‑local {@code StringBuilder} cache handling</li>
 * </ul>
 *
 * Each test method focuses on a single public method and contains
 * typical, edge‑case and error scenarios.
 */
public class StringUtilTest {

    // ----------------------------------------------------------------------
    // join(...)
    // ----------------------------------------------------------------------
    @Test
    public void join_Collection_Empty() {
        Collection<String> empty = Collections.emptyList();
        assertEquals("", StringUtil.join(empty, ","));
    }

    @Test
    public void join_Collection_SingleElement() {
        Collection<String> one = Collections.singleton("only");
        assertEquals("only", StringUtil.join(one, ","));
    }

    @Test
    public void join_Collection_MultipleElements() {
        Collection<String> many = Arrays.asList("a", "b", "c");
        assertEquals("a|b|c", StringUtil.join(many, "|"));
    }

    @Test
    public void join_Collection_NullElement() {
        // The implementation calls toString() on each element; a null element throws NPE.
        Collection<String> withNull = Arrays.asList("first", null, "third");
        try {
            StringUtil.join(withNull, ",");
            fail("Expected NullPointerException because of null element");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void join_Iterator_MultipleElements() {
        List<String> list = Arrays.asList("x", "y", "z");
        assertEquals("x:y:z", StringUtil.join(list.iterator(), ":"));
    }

    @Test
    public void join_Array_MultipleElements() {
        String[] array = {"one", "two", "three"};
        assertEquals("one+two+three", StringUtil.join(array, "+"));
    }

    @Test
    public void join_Array_Empty() {
        String[] empty = new String[0];
        assertEquals("", StringUtil.join(empty, ","));
    }

    // ----------------------------------------------------------------------
    // padding(...)
    // ----------------------------------------------------------------------
    @Test
    public void padding_WithinMemoisedRange() {
        for (int i = 0; i < 21; i++) {
            assertEquals(StringUtil.padding(i).length(), i);
            // ensure the memoised string is returned (reference equality)
            assertSame(StringUtil.padding(i), StringUtil.padding(i));
        }
    }

    @Test
    public void padding_OutsideMemoisedRange() {
        int width = 30;
        String pad = StringUtil.padding(width);
        assertEquals(width, pad.length());
        for (char c : pad.toCharArray()) {
            assertEquals(' ', c);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void padding_NegativeWidth() {
        StringUtil.padding(-1);
    }

    // ----------------------------------------------------------------------
    // isBlank(...)
    // ----------------------------------------------------------------------
    @Test
    public void isBlank_NullOrEmpty() {
        assertTrue(StringUtil.isBlank(null));
        assertTrue(StringUtil.isBlank(""));
    }

    @Test
    public void isBlank_WhitespaceOnly() {
        assertTrue(StringUtil.isBlank(" \t\r\n\f"));
        assertTrue(StringUtil.isBlank("\u00A0")); // non‑breaking space is NOT considered blank
        // NB: isBlank uses isWhitespace (HTML spec) which does NOT include NBSP.
        assertFalse(StringUtil.isBlank("\u00A0"));
    }

    @Test
    public void isBlank_MixedContent() {
        assertFalse(StringUtil.isBlank(" a "));
        assertFalse(StringUtil.isBlank("\nX"));
    }

    // ----------------------------------------------------------------------
    // isNumeric(...)
    // ----------------------------------------------------------------------
    @Test
    public void isNumeric_NullOrEmpty() {
        assertFalse(StringUtil.isNumeric(null));
        assertFalse(StringUtil.isNumeric(""));
    }

    @Test
    public void isNumeric_OnlyDigits() {
        assertTrue(StringUtil.isNumeric("0"));
        assertTrue(StringUtil.isNumeric("1234567890"));
    }

    @Test
    public void isNumeric_WithNonDigits() {
        assertFalse(StringUtil.isNumeric("12a34"));
        assertFalse(StringUtil.isNumeric(" 123"));
        assertFalse(StringUtil.isNumeric("-123"));
    }

    // ----------------------------------------------------------------------
    // isWhitespace / isActuallyWhitespace / isInvisibleChar
    // ----------------------------------------------------------------------
    @Test
    public void isWhitespace_SpecDefined() {
        int[] ws = {' ', '\t', '\n', '\f', '\r'};
        for (int c : ws) {
            assertTrue(StringUtil.isWhitespace(c));
        }
        assertFalse(StringUtil.isWhitespace('a'));
        assertFalse(StringUtil.isWhitespace(0x00A0)); // NBSP not spec‑whitespace
    }

    @Test
    public void isActuallyWhitespace_IncludesNbsp() {
        int[] ws = {' ', '\t', '\n', '\f', '\r', 0x00A0};
        for (int c : ws) {
            assertTrue(StringUtil.isActuallyWhitespace(c));
        }
        assertFalse(StringUtil.isActuallyWhitespace('b'));
    }

    @Test
    public void isInvisibleChar_KnownValues() {
        int[] invisible = {8203, 8204, 8205, 173};
        for (int c : invisible) {
            assertTrue(StringUtil.isInvisibleChar(c));
        }
        assertFalse(StringUtil.isInvisibleChar('a'));
        assertFalse(StringUtil.isInvisibleChar(0));
    }

    // ----------------------------------------------------------------------
    // normaliseWhitespace & appendNormalisedWhitespace
    // ----------------------------------------------------------------------
    @Test
    public void normaliseWhitespace_CollapsesAndConverts() {
        String input = "  a \t \n  b \r\n c  ";
        String expected = "a b c";
        assertEquals(expected, StringUtil.normaliseWhitespace(input));
    }

    @Test
    public void normaliseWhitespace_HandlesInvisibleChars() {
        // zero‑width space (8203) should be removed
        String input = "a\u200B\u200C b";
        assertEquals("a b", StringUtil.normaliseWhitespace(input));
    }

    @Test
    public void appendNormalisedWhitespace_StripLeading() {
        StringBuilder sb = new StringBuilder();
        String input = "   leading   spaces ";
        StringUtil.appendNormalisedWhitespace(sb, input, true);
        assertEquals("leading spaces ", sb.toString());
    }

    @Test
    public void appendNormalisedWhitespace_NoStripLeading() {
        StringBuilder sb = new StringBuilder();
        String input = "   leading   spaces ";
        StringUtil.appendNormalisedWhitespace(sb, input, false);
        assertEquals(" leading spaces ", sb.toString());
    }

    // ----------------------------------------------------------------------
    // in(...) and inSorted(...)
    // ----------------------------------------------------------------------
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

    // ----------------------------------------------------------------------
    // resolve(URL, String) – URL overload
    // ----------------------------------------------------------------------
    @Test
    public void resolve_RelativePath() throws MalformedURLException {
        URL base = new URL("http://example.com/dir/page.html");
        URL result = StringUtil.resolve(base, "sub/file.txt");
        assertEquals("http://example.com/dir/sub/file.txt", result.toExternalForm());
    }

    @Test
    public void resolve_QueryOnly() throws MalformedURLException {
        URL base = new URL("http://example.com/path/page.html");
        URL result = StringUtil.resolve(base, "?id=123");
        // query is appended to the base's path, not to the root
        assertEquals("http://example.com/path/page.html?id=123", result.toExternalForm());
    }

    @Test
    public void resolve_DotStartWhenBaseHasFileNoSlash() throws MalformedURLException {
        URL base = new URL("http://example.com/dirfile");
        URL result = StringUtil.resolve(base, "./sub");
        // The workaround adds a leading '/' before resolution.
        assertEquals("http://example.com/./sub", result.toExternalForm());
    }

    @Test(expected = MalformedURLException.class)
    public void resolve_MalformedRelative() throws MalformedURLException {
        URL base = new URL("http://example.com/");
        // space in URL makes it malformed
        StringUtil.resolve(base, "bad url");
    }

    // ----------------------------------------------------------------------
    // resolve(String, String) – String overload
    // ----------------------------------------------------------------------
    @Test
    public void resolveString_ValidBaseAndRelative() {
        String result = StringUtil.resolve("http://example.com/base/", "sub/page.html");
        assertEquals("http://example.com/base/sub/page.html", result);
    }

    @Test
    public void resolveString_RelativeIsAbsolute() {
        String result = StringUtil.resolve("http://example.com/base/", "https://other.com/abs");
        assertEquals("https://other.com/abs", result);
    }

    @Test
    public void resolveString_InvalidBase_AbsoluteRelativeUsed() {
        // base is malformed, relative is absolute – should return the absolute URL.
        String result = StringUtil.resolve("ht!tp://bad base", "http://good.com/");
        assertEquals("http://good.com/", result);
    }

    @Test
    public void resolveString_BothInvalid_ReturnsEmptyString() {
        String result = StringUtil.resolve("bad base", "also bad");
        assertEquals("", result);
    }

    // ----------------------------------------------------------------------
    // stringBuilder() – thread‑local cache handling
    // ----------------------------------------------------------------------
    private StringBuilder sb1;
    private StringBuilder sb2;

    @Before
    public void setUpBuilders() {
        sb1 = StringUtil.stringBuilder();
        sb2 = StringUtil.stringBuilder();
    }

    @Test
    public void stringBuilder_ReusesSameInstanceWithinThread() {
        // The two calls in setUp() should return the *same* instance (cleared each time)
        assertSame(sb1, sb2);
        sb1.append("test");
        assertEquals("test", sb2.toString()); // sb2 is the same buffer, now containing "test"
    }

    @Test
    public void stringBuilder_ResetsWhenTooLarge() {
        // Force the cached builder to exceed the max size
        StringBuilder large = StringUtil.stringBuilder();
        // Fill beyond MaxCachedBuilderSize (8KB)
        char[] big = new char[9 * 1024];
        Arrays.fill(big, 'x');
        large.append(big);
        // Next call should allocate a fresh builder trimmed to MaxCachedBuilderSize
        StringBuilder next = StringUtil.stringBuilder();
        assertNotSame(large, next);
        assertTrue(next.capacity() <= 8 * 1024);
        assertEquals(0, next.length());
    }

    // ----------------------------------------------------------------------
    // Additional sanity checks
    // ----------------------------------------------------------------------
    @Test
    public void join_Iterator_Empty() {
        Iterator<String> empty = Collections.<String>emptyList().iterator();
        assertEquals("", StringUtil.join(empty, ","));
    }

    @Test
    public void isWhitespace_NullCharIsZero() {
        // Java char defaults to '\u0000' which is not whitespace per spec
        assertFalse(StringUtil.isWhitespace(0));
    }
}
```
###Test END##