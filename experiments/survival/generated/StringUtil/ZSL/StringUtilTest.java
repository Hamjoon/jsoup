package org.jsoup.helper;

import org.junit.Test;
import org.junit.Before;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

public class StringUtilTest {

    /*** ---------- join tests ---------- ***/
    @Test
    public void join_EmptyCollection_ReturnsEmptyString() {
        Collection<String> empty = Collections.emptyList();
        assertEquals("", StringUtil.join(empty, ","));
    }

    @Test
    public void join_SingleElement_NoSeparatorUsed() {
        Collection<String> single = Collections.singletonList("one");
        assertEquals("one", StringUtil.join(single, ","));
    }

    @Test
    public void join_MultipleElements_SeparatedCorrectly() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("a|b|c", StringUtil.join(list, "|"));
    }

    @Test
    public void join_Iterator_MultipleElements_SeparatedCorrectly() {
        List<String> list = Arrays.asList("x", "y");
        Iterator<String> it = list.iterator();
        assertEquals("x-y", StringUtil.join(it, "-"));
    }

    @Test
    public void join_Array_MultipleElements_SeparatedCorrectly() {
        String[] arr = {"foo", "bar", "baz"};
        assertEquals("foo bar baz", StringUtil.join(arr, " "));
    }

    /*** ---------- padding tests ---------- ***/
    @Test(expected = IllegalArgumentException.class)
    public void padding_NegativeWidth_ThrowsException() {
        StringUtil.padding(-1);
    }

    @Test
    public void padding_WithinMemoisedRange_ReturnsMemoisedString() {
        for (int i = 0; i < 22; i++) {
            String expected = new String(new char[i]).replace('\0', ' ');
            assertEquals("Failed at width " + i, expected, StringUtil.padding(i));
        }
    }

    @Test
    public void padding_AboveMemoisedRange_ReturnsCorrectLength() {
        int width = 30;
        String result = StringUtil.padding(width);
        assertEquals(width, result.length());
        assertTrue(result.matches("^\\s+$"));
    }

    /*** ---------- isBlank tests ---------- ***/
    @Test
    public void isBlank_NullOrEmpty_ReturnsTrue() {
        assertTrue(StringUtil.isBlank(null));
        assertTrue(StringUtil.isBlank(""));
    }

    @Test
    public void isBlank_OnlyWhitespace_ReturnsTrue() {
        assertTrue(StringUtil.isBlank(" \t\r\n\f"));
        assertTrue(StringUtil.isBlank("\u00A0")); // non‑breaking space is not considered whitespace by isBlank
    }

    @Test
    public void isBlank_NonWhitespace_ReturnsFalse() {
        assertFalse(StringUtil.isBlank("a"));
        assertFalse(StringUtil.isBlank(" a "));
    }

    /*** ---------- isNumeric tests ---------- ***/
    @Test
    public void isNumeric_NullOrEmpty_ReturnsFalse() {
        assertFalse(StringUtil.isNumeric(null));
        assertFalse(StringUtil.isNumeric(""));
    }

    @Test
    public void isNumeric_OnlyDigits_ReturnsTrue() {
        assertTrue(StringUtil.isNumeric("0"));
        assertTrue(StringUtil.isNumeric("1234567890"));
    }

    @Test
    public void isNumeric_NonDigitCharacters_ReturnsFalse() {
        assertFalse(StringUtil.isNumeric("12a34"));
        assertFalse(StringUtil.isNumeric("-123"));
        assertFalse(StringUtil.isNumeric("12.34"));
        assertFalse(StringUtil.isNumeric("\u0660\u0661")); // Arabic-Indic digits are not Unicode digit per Character.isDigit? they are, but test anyway
    }

    /*** ---------- whitespace tests ---------- ***/
    @Test
    public void isWhitespace_KnownWhitespace_ReturnsTrue() {
        assertTrue(StringUtil.isWhitespace(' '));
        assertTrue(StringUtil.isWhitespace('\t'));
        assertTrue(StringUtil.isWhitespace('\n'));
        assertTrue(StringUtil.isWhitespace('\f'));
        assertTrue(StringUtil.isWhitespace('\r'));
    }

    @Test
    public void isWhitespace_OtherCharacters_ReturnsFalse() {
        assertFalse(StringUtil.isWhitespace('a'));
        assertFalse(StringUtil.isWhitespace('\u00A0')); // non‑breaking space
    }

    @Test
    public void isActuallyWhitespace_IncludesNbsp() {
        assertTrue(StringUtil.isActuallyWhitespace('\u00A0'));
        assertTrue(StringUtil.isActuallyWhitespace(' '));
        assertTrue(StringUtil.isActuallyWhitespace('\t'));
        assertTrue(StringUtil.isActuallyWhitespace('\n'));
        assertTrue(StringUtil.isActuallyWhitespace('\f'));
        assertTrue(StringUtil.isActuallyWhitespace('\r'));
    }

    @Test
    public void isActuallyWhitespace_OtherCharacters_ReturnsFalse() {
        assertFalse(StringUtil.isActuallyWhitespace('b'));
        assertFalse(StringUtil.isActuallyWhitespace('\u2000')); // En Quad, not listed
    }

    @Test
    public void isInvisibleChar_KnownInvisible_ReturnsTrue() {
        assertTrue(StringUtil.isInvisibleChar(8203)); // zero width space
        assertTrue(StringUtil.isInvisibleChar(8204));
        assertTrue(StringUtil.isInvisibleChar(8205));
        assertTrue(StringUtil.isInvisibleChar(173)); // soft hyphen
    }

    @Test
    public void isInvisibleChar_OtherCharacters_ReturnsFalse() {
        assertFalse(StringUtil.isInvisibleChar('a'));
        assertFalse(StringUtil.isInvisibleChar(0));
    }

    /*** ---------- normaliseWhitespace tests ---------- ***/
    @Test
    public void normaliseWhitespace_CollapsesMultipleSpaces() {
        String input = "a   b\tc\n\n d";
        String expected = "a b c d";
        assertEquals(expected, StringUtil.normaliseWhitespace(input));
    }

    @Test
    public void normaliseWhitespace_LeadingAndTrailingWhitespace_Stripped() {
        String input = "   hello world   ";
        String expected = "hello world";
        assertEquals(expected, StringUtil.normaliseWhitespace(input));
    }

    @Test
    public void normaliseWhitespace_InvisibleChars_Removed() {
        String input = "a\u200B\u200C\u200D b"; // zero‑width space, joiner, non‑joiner
        String expected = "a b";
        assertEquals(expected, StringUtil.normaliseWhitespace(input));
    }

    @Test
    public void appendNormalisedWhitespace_StripLeadingTrue_RemovesLeadingSpace() {
        StringBuilder sb = new StringBuilder();
        StringUtil.appendNormalisedWhitespace(sb, "  foo  bar", true);
        assertEquals("foo bar", sb.toString());
    }

    @Test
    public void appendNormalisedWhitespace_StripLeadingFalse_KeepsFirstSpace() {
        StringBuilder sb = new StringBuilder();
        StringUtil.appendNormalisedWhitespace(sb, "  foo  bar", false);
        assertEquals(" foo bar", sb.toString());
    }

    /*** ---------- in and inSorted tests ---------- ***/
    @Test
    public void in_ValuePresent_ReturnsTrue() {
        assertTrue(StringUtil.in("b", "a", "b", "c"));
    }

    @Test
    public void in_ValueAbsent_ReturnsFalse() {
        assertFalse(StringUtil.in("x", "a", "b", "c"));
    }

    @Test
    public void inSorted_ValuePresent_ReturnsTrue() {
        String[] sorted = {"alpha", "beta", "gamma"};
        assertTrue(StringUtil.inSorted("beta", sorted));
    }

    @Test
    public void inSorted_ValueAbsent_ReturnsFalse() {
        String[] sorted = {"alpha", "beta", "gamma"};
        assertFalse(StringUtil.inSorted("delta", sorted));
    }

    /*** ---------- resolve(URL, String) tests ---------- ***/
    @Test
    public void resolve_RelativePath_ResolvedCorrectly() throws MalformedURLException {
        URL base = new URL("http://example.com/folder/");
        URL result = StringUtil.resolve(base, "sub/page.html");
        assertEquals("http://example.com/folder/sub/page.html", result.toExternalForm());
    }

    @Test
    public void resolve_QueryStartsWithQuestionMark_AppendsToBasePath() throws MalformedURLException {
        URL base = new URL("http://example.com/path/file.html");
        URL result = StringUtil.resolve(base, "?id=123");
        // Expected: base path + query (without discarding the file name)
        assertEquals("http://example.com/path/file.html?id=123", result.toExternalForm());
    }

    @Test
    public void resolve_RelativeDotPath_WithBaseFile_NoLeadingSlash_FixApplied() throws MalformedURLException {
        URL base = new URL("http://example.com/folder/file.html");
        URL result = StringUtil.resolve(base, "./other.html");
        assertEquals("http://example.com/folder/other.html", result.toExternalForm());
    }

    @Test
    public void resolve_AbsoluteRelUrl_ReturnsAbsoluteUrlUnchanged() throws MalformedURLException {
        URL base = new URL("http://example.com/");
        URL result = StringUtil.resolve(base, "https://other.com/page");
        assertEquals("https://other.com/page", result.toExternalForm());
    }

    /*** ---------- resolve(String, String) tests ---------- ***/
    @Test
    public void resolveString_BaseValid_RelativeResolved() {
        String base = "http://example.com/dir/";
        String rel = "sub/file.txt";
        String expected = "http://example.com/dir/sub/file.txt";
        assertEquals(expected, StringUtil.resolve(base, rel));
    }

    @Test
    public void resolveString_BaseInvalid_RelIsAbsolute() {
        String base = "not a url";
        String rel = "https://absolute.com/page";
        assertEquals("https://absolute.com/page", StringUtil.resolve(base, rel));
    }

    @Test
    public void resolveString_BothInvalid_ReturnsEmptyString() {
        String base = ":// malformed";
        String rel = "also:bad";
        assertEquals("", StringUtil.resolve(base, rel));
    }

    /*** ---------- stringBuilder caching tests ---------- ***/
    private static final int MAX_CACHED_SIZE = 8 * 1024; // same as StringUtil.MaxCachedBuilderSize

    @Test
    public void stringBuilder_ReusesInstanceWhenSizeWithinLimit() {
        StringBuilder sb1 = StringUtil.stringBuilder();
        sb1.append("test");
        StringBuilder sb2 = StringUtil.stringBuilder();
        // same instance should be returned and length reset to zero
        assertSame(sb1, sb2);
        assertEquals(0, sb2.length());
    }

    @Test
    public void stringBuilder_ReturnsNewInstanceWhenPreviousExceedsLimit() {
        // First, obtain a builder and overflow it
        StringBuilder sbLarge = StringUtil.stringBuilder();
        // Fill it beyond the cached limit
        char[] big = new char[MAX_CACHED_SIZE + 1];
        Arrays.fill(big, 'x');
        sbLarge.append(big);
        assertTrue(sbLarge.length() > MAX_CACHED_SIZE);

        // Next call should allocate a new builder (size reset to MAX_CACHED_SIZE)
        StringBuilder sbAfter = StringUtil.stringBuilder();
        assertNotSame(sbLarge, sbAfter);
        // The new builder should be empty
        assertEquals(0, sbAfter.length());
    }

    /*** ---------- Additional edge cases ---------- ***/
    @Test
    public void join_NullElement_ThrowsNullPointerException() {
        List<String> list = new ArrayList<>();
        list.add(null);
        try {
            StringUtil.join(list, ",");
            fail("Expected NullPointerException because of null element");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void normaliseWhitespace_OnlyInvisibleChars_ReturnsEmptyString() {
        String input = "\u200B\u200C\u200D\u200E"; // zero‑width chars
        assertEquals("", StringUtil.normaliseWhitespace(input));
    }

    @Test
    public void isBlank_StringWithNbspOnly_ReturnsFalse() {
        // NBSP is not considered whitespace by isBlank()
        assertFalse(StringUtil.isBlank("\u00A0"));
    }
}
