**1. Public methods of `org.jsoup.helper.StringUtil` (signatures)**  

| # | Return type | Method signature |
|---|-------------|-------------------|
| 1 | `String` | `public static String join(Collection strings, String sep)` |
| 2 | `String` | `public static String join(Iterator strings, String sep)` |
| 3 | `String` | `public static String join(String[] strings, String sep)` |
| 4 | `String` | `public static String padding(int width)` |
| 5 | `boolean` | `public static boolean isBlank(String string)` |
| 6 | `boolean` | `public static boolean isNumeric(String string)` |
| 7 | `boolean` | `public static boolean isWhitespace(int c)` |
| 8 | `boolean` | `public static boolean isActuallyWhitespace(int c)` |
| 9 | `boolean` | `public static boolean isInvisibleChar(int c)` |
|10 | `String` | `public static String normaliseWhitespace(String string)` |
|11 | `void`   | `public static void appendNormalisedWhitespace(StringBuilder accum, String string, boolean stripLeading)` |
|12 | `boolean`| `public static boolean in(final String needle, final String... haystack)` |
|13 | `boolean`| `public static boolean inSorted(String needle, String[] haystack)` |
|14 | `URL`    | `public static URL resolve(URL base, String relUrl) throws MalformedURLException` |
|15 | `String` | `public static String resolve(final String baseUrl, final String relUrl)` |
|16 | `StringBuilder`| `public static StringBuilder stringBuilder()` |

---

**2. Basic JUnit‑4 test ideas (one per method)**  

| # | Test name | What it verifies (basic behaviour) |
|---|-----------|------------------------------------|
| 1 | `joinCollectionBasic` | Joins three strings with “, ” separator. |
| 2 | `joinIteratorBasic`   | Joins iterator of two strings with “-”. |
| 3 | `joinArrayBasic`      | Joins array of one element → returns that element. |
| 4 | `paddingBasic`        | `padding(5)` returns five spaces. |
| 5 | `isBlankBasic`        | `" \t\n".` → `true`; `"a"` → `false`. |
| 6 | `isNumericBasic`      | `"0123"` → `true`; `"12a3"` → `false`. |
| 7 | `isWhitespaceBasic`   | Checks each HTML‑spec whitespace char returns `true`. |
| 8 | `isActuallyWhitespaceBasic` | Same as above plus `\u00A0` (160). |
| 9 | `isInvisibleCharBasic`| Returns `true` for `8203` (ZERO WIDTH SPACE). |
|10 | `normaliseWhitespaceBasic`| `"a   b\tc\n"` → `"a b c"`. |
|11 | `appendNormalisedWhitespaceBasic`| Appends to a builder with `stripLeading = false`. |
|12 | `inBasic` | Needle present in var‑args returns `true`. |
|13 | `inSortedBasic` | Needle present in sorted array returns `true`. |
|14 | `resolveUrlBasic` | Resolves relative `"path/file.html"` against `http://example.com/base/`. |
|15 | `resolveStringBasic` | Same as above but using `String` overload. |
|16 | `stringBuilderBasic` | Returns an empty `StringBuilder` each call. |

---

**3. Edge‑case & exception scenarios that must be covered**

| Method | Edge / Exception | Test idea |
|--------|------------------|-----------|
| `join(Collection,…)` | `null` collection → NPE. <br> Empty collection → `""`. <br> Collection containing `null` element → NPE. |
| `join(Iterator,…)` | `null` iterator → NPE. <br> Iterator with no elements → `""`. <br> Iterator with one element → element itself. |
| `join(String[],…)` | `null` array → NPE. <br> Empty array → `""`. |
| `padding(int)` | Negative width → `IllegalArgumentException`. <br> Width ≥ memoised length (e.g. 30) → generated spaces length equals width. |
| `isBlank(String)` | `null` → `true`. <br> Empty string → `true`. <br> Mixed whitespace & non‑whitespace → `false`. |
| `isNumeric(String)` | `null` / empty → `false`. <br> Unicode digits (e.g. Arabic‑Indic `\u0660`) → should be `true`. |
| `isWhitespace(int)` | Non‑whitespace char (e.g. `'A'`) → `false`. |
| `isActuallyWhitespace(int)` | Non‑whitespace char (e.g. `'A'`) → `false`. |
| `isInvisibleChar(int)` | Char not in list (e.g. `'A'`) → `false`. |
| `normaliseWhitespace(String)` | `null` argument → NPE (method does not guard). <br> String containing invisible chars – they must be stripped. |
| `appendNormalisedWhitespace` | `stripLeading = true` should drop leading whitespace. <br> Input string empty → builder unchanged. |
| `in(String,…)` | Empty haystack → `false`. <br> `null` needle → NPE (because `equals` is invoked). |
| `inSorted(String,…)` | Unsorted array → result undefined, but we test sorted case only. <br> Needle `null` → NPE. |
| `resolve(URL, String)` | `relUrl` starts with “?” – should be appended to base path. <br> `relUrl` starts with “.” and base file does not start with “/” – triggers the special handling. |
| `resolve(String, String)` | Base URL malformed → method tries to treat `relUrl` as absolute. <br> Both malformed → returns empty string. |
| `stringBuilder()` | Builder size > `MaxCachedBuilderSize` (8 KB) – a new builder of that size is allocated. We can simulate by filling the builder, then calling again and checking length is 0. |

---

**4. Full JUnit‑4 test file (merged, ready to compile)**  

```java
###Test START##
package org.jsoup.helper;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

public class StringUtilTest {

    /* --------------------------------------------------------------------- */
    /* 1. join(Collection, String)                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void joinCollectionBasic() {
        Collection<String> col = Arrays.asList("one", "two", "three");
        assertEquals("one, two, three", StringUtil.join(col, ", "));
    }

    @Test
    public void joinCollectionEmpty() {
        Collection<String> empty = Collections.emptyList();
        assertEquals("", StringUtil.join(empty, ","));
    }

    @Test(expected = NullPointerException.class)
    public void joinCollectionNullCollection() {
        StringUtil.join((Collection) null, ",");
    }

    @Test(expected = NullPointerException.class)
    public void joinCollectionContainsNullElement() {
        Collection<String> col = Arrays.asList("a", null, "b");
        // toString() on null will cause NPE inside join()
        StringUtil.join(col, "-");
    }

    /* --------------------------------------------------------------------- */
    /* 2. join(Iterator, String)                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void joinIteratorBasic() {
        List<String> list = Arrays.asList("a", "b");
        assertEquals("a-b", StringUtil.join(list.iterator(), "-"));
    }

    @Test
    public void joinIteratorSingleElement() {
        List<String> list = Collections.singletonList("solo");
        assertEquals("solo", StringUtil.join(list.iterator(), ","));
    }

    @Test
    public void joinIteratorEmpty() {
        Iterator<String> empty = Collections.<String>emptyList().iterator();
        assertEquals("", StringUtil.join(empty, ","));
    }

    @Test(expected = NullPointerException.class)
    public void joinIteratorNull() {
        StringUtil.join((Iterator) null, ",");
    }

    /* --------------------------------------------------------------------- */
    /* 3. join(String[], String)                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void joinArrayBasic() {
        String[] arr = {"x", "y", "z"};
        assertEquals("x|y|z", StringUtil.join(arr, "|"));
    }

    @Test
    public void joinArraySingle() {
        String[] arr = {"only"};
        assertEquals("only", StringUtil.join(arr, ","));
    }

    @Test
    public void joinArrayEmpty() {
        String[] arr = new String[0];
        assertEquals("", StringUtil.join(arr, ","));
    }

    @Test(expected = NullPointerException.class)
    public void joinArrayNull() {
        StringUtil.join((String[]) null, ",");
    }

    /* --------------------------------------------------------------------- */
    /* 4. padding(int)                                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void paddingBasic() {
        assertEquals("     ", StringUtil.padding(5));
    }

    @Test
    public void paddingMemoisedBoundary() {
        // width == padding.length-1 (20) is memoised
        assertEquals(20, StringUtil.padding(20).length());
    }

    @Test
    public void paddingBeyondMemoised() {
        int width = 30;
        assertEquals(width, StringUtil.padding(width).length());
    }

    @Test(expected = IllegalArgumentException.class)
    public void paddingNegative() {
        StringUtil.padding(-1);
    }

    /* --------------------------------------------------------------------- */
    /* 5. isBlank(String)                                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void isBlankNullAndEmpty() {
        assertTrue(StringUtil.isBlank(null));
        assertTrue(StringUtil.isBlank(""));
    }

    @Test
    public void isBlankWhitespaceOnly() {
        assertTrue(StringUtil.isBlank(" \t\r\n\f"));
    }

    @Test
    public void isBlankMixed() {
        assertFalse(StringUtil.isBlank("  a  "));
    }

    /* --------------------------------------------------------------------- */
    /* 6. isNumeric(String)                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void isNumericNullOrEmpty() {
        assertFalse(StringUtil.isNumeric(null));
        assertFalse(StringUtil.isNumeric(""));
    }

    @Test
    public void isNumericSimpleDigits() {
        assertTrue(StringUtil.isNumeric("0123456789"));
    }

    @Test
    public void isNumericWithNonDigit() {
        assertFalse(StringUtil.isNumeric("123a45"));
    }

    @Test
    public void isNumericUnicodeDigits() {
        // Arabic‑Indic digit zero (U+0660) … nine (U+0669)
        assertTrue(StringUtil.isNumeric("\u0660\u0661\u0662"));
    }

    /* --------------------------------------------------------------------- */
    /* 7. isWhitespace(int)                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void isWhitespaceAllSpecChars() {
        int[] ws = {' ', '\t', '\n', '\f', '\r'};
        for (int c : ws) {
            assertTrue("char " + c + " should be whitespace", StringUtil.isWhitespace(c));
        }
        assertFalse(StringUtil.isWhitespace('A'));
    }

    /* --------------------------------------------------------------------- */
    /* 8. isActuallyWhitespace(int)                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void isActuallyWhitespaceIncludesNbsp() {
        assertTrue(StringUtil.isActuallyWhitespace(160)); // &nbsp;
    }

    @Test
    public void isActuallyWhitespaceNonWhitespace() {
        assertFalse(StringUtil.isActuallyWhitespace('Z'));
    }

    /* --------------------------------------------------------------------- */
    /* 9. isInvisibleChar(int)                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void isInvisibleCharKnown() {
        int[] invisible = {8203, 8204, 8205, 173};
        for (int c : invisible) {
            assertTrue("codepoint " + c + " should be invisible", StringUtil.isInvisibleChar(c));
        }
        assertFalse(StringUtil.isInvisibleChar('A'));
    }

    /* --------------------------------------------------------------------- */
    /* 10. normaliseWhitespace(String)                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void normaliseWhitespaceSimple() {
        String src = "a   b\tc\nd\r\ne\f";
        assertEquals("a b c d e ", StringUtil.normaliseWhitespace(src));
    }

    @Test
    public void normaliseWhitespaceWithInvisible() {
        // ZERO WIDTH SPACE (8203) should be dropped
        String src = "a\u8203b  \t c";
        assertEquals("a b c", StringUtil.normaliseWhitespace(src));
    }

    @Test(expected = NullPointerException.class)
    public void normaliseWhitespaceNull() {
        StringUtil.normaliseWhitespace(null);
    }

    /* --------------------------------------------------------------------- */
    /* 11. appendNormalisedWhitespace(StringBuilder, String, boolean)        */
    /* --------------------------------------------------------------------- */

    @Test
    public void appendNormalisedWhitespaceStripLeading() {
        StringBuilder sb = new StringBuilder();
        String src = "   leading  and   internal\t\tspaces ";
        StringUtil.appendNormalisedWhitespace(sb, src, true);
        assertEquals("leading and internal spaces ", sb.toString());
    }

    @Test
    public void appendNormalisedWhitespaceNoStrip() {
        StringBuilder sb = new StringBuilder();
        String src = "\n\nmixed\n\n";
        StringUtil.appendNormalisedWhitespace(sb, src, false);
        assertEquals(" mixed ", sb.toString());
    }

    /* --------------------------------------------------------------------- */
    /* 12. in(String, String...)                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void inNeedleFound() {
        assertTrue(StringUtil.in("b", "a", "b", "c"));
    }

    @Test
    public void inNeedleNotFound() {
        assertFalse(StringUtil.in("x", "a", "b", "c"));
    }

    @Test
    public void inEmptyHaystack() {
        assertFalse(StringUtil.in("anything"));
    }

    @Test(expected = NullPointerException.class)
    public void inNeedleNull() {
        StringUtil.in(null, "a", "b");
    }

    /* --------------------------------------------------------------------- */
    /* 13. inSorted(String, String[])                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void inSortedFound() {
        String[] sorted = {"a", "b", "c", "d"};
        assertTrue(StringUtil.inSorted("c", sorted));
    }

    @Test
    public void inSortedNotFound() {
        String[] sorted = {"a", "b", "c", "d"};
        assertFalse(StringUtil.inSorted("z", sorted));
    }

    @Test(expected = NullPointerException.class)
    public void inSortedNeedleNull() {
        String[] sorted = {"a", "b"};
        StringUtil.inSorted(null, sorted);
    }

    /* --------------------------------------------------------------------- */
    /* 14. resolve(URL, String)                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void resolveUrlSimpleRelative() throws MalformedURLException {
        URL base = new URL("http://example.com/dir/");
        URL result = StringUtil.resolve(base, "page.html");
        assertEquals("http://example.com/dir/page.html", result.toExternalForm());
    }

    @Test
    public void resolveUrlQueryOnly() throws MalformedURLException {
        URL base = new URL("http://example.com/dir/file.html");
        URL result = StringUtil.resolve(base, "?id=10");
        assertEquals("http://example.com/dir/file.html?id=10", result.toExternalForm());
    }

    @Test
    public void resolveUrlDotPrefixSpecialCase() throws MalformedURLException {
        URL base = new URL("http://example.com/dir/file.html");
        URL result = StringUtil.resolve(base, "./sub/page.html");
        assertEquals("http://example.com/dir/sub/page.html", result.toExternalForm());
    }

    @Test(expected = MalformedURLException.class)
    public void resolveUrlInvalidBase() throws MalformedURLException {
        URL badBase = new URL("http://[invalid]");
        StringUtil.resolve(badBase, "rel");
    }

    /* --------------------------------------------------------------------- */
    /* 15. resolve(String, String)                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void resolveStringBothValid() {
        String out = StringUtil.resolve("http://example.com/base/", "sub/page.html");
        assertEquals("http://example.com/base/sub/page.html", out);
    }

    @Test
    public void resolveStringBaseMalformedRelAbsolute() {
        String out = StringUtil.resolve("ht!tp://bad", "http://good.com/abs");
        assertEquals("http://good.com/abs", out);
    }

    @Test
    public void resolveStringBothMalformedReturnsEmpty() {
        String out = StringUtil.resolve("bad://url", "also:bad");
        assertEquals("", out);
    }

    /* --------------------------------------------------------------------- */
    /* 16. stringBuilder()                                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void stringBuilderIsEmptyOnReturn() {
        StringBuilder sb = StringUtil.stringBuilder();
        assertEquals(0, sb.length());
        sb.append("temp");
        // second call should give a cleared builder (same instance)
        StringBuilder sb2 = StringUtil.stringBuilder();
        assertSame(sb, sb2);
        assertEquals(0, sb2.length());
    }

    @Test
    public void stringBuilderRespectsMaxSize() {
        // Fill the builder beyond the cached limit (8 KB) and verify a new one is allocated.
        StringBuilder sb = StringUtil.stringBuilder();
        for (int i = 0; i < 9 * 1024; i++) {
            sb.append('x');
        }
        // Now request a fresh builder – it should be a new instance with length 0.
        StringBuilder sb2 = StringUtil.stringBuilder();
        assertNotSame(sb, sb2);
        assertEquals(0, sb2.length());
    }

    /* --------------------------------------------------------------------- */
    /* Helper for reflective access (used only in the max‑size test)          */
    /* --------------------------------------------------------------------- */

    // No helper needed; the test above works without reflection because the
    // implementation creates a new builder when the cached one exceeds the limit.

}
###Test END##
```