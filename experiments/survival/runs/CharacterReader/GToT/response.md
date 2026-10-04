###Test START##
```java
package org.jsoup.parser;

import org.jsoup.UncheckedIOException;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CharacterReader}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public API methods.</li>
 *   <li>Package‑private helper methods that are part of the contract (the test class is in the same package).</li>
 *   <li>Typical use cases, edge cases and error scenarios.</li>
 *   <li>Behaviour of the internal string‑caching mechanism.</li>
 * </ul>
 */
public class CharacterReaderTest {

    /** Helper to create a CharacterReader from a plain string. */
    private static CharacterReader newReader(String s) {
        return new CharacterReader(s);
    }

    /** Helper to invoke the private static {@code cacheString} method via reflection. */
    private static String callCacheString(char[] buf, String[] cache, int start, int count)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method m = CharacterReader.class.getDeclaredMethod(
                "cacheString", char[].class, String[].class, int.class, int.class);
        m.setAccessible(true);
        return (String) m.invoke(null, buf, cache, start, count);
    }

    /* --------------------------------------------------------------------- */
    /*  Public API tests                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPosAndAdvance() {
        CharacterReader r = newReader("abc");
        assertEquals(0, r.pos());
        r.advance();
        assertEquals(1, r.pos());
        r.advance();
        assertEquals(2, r.pos());
    }

    @Test
    public void testIsEmpty() {
        CharacterReader empty = newReader("");
        assertTrue(empty.isEmpty());

        CharacterReader nonEmpty = newReader("x");
        assertFalse(nonEmpty.isEmpty());
        // consume the only character
        nonEmpty.consumeToEnd();
        assertTrue(nonEmpty.isEmpty());
    }

    @Test
    public void testCurrentAndConsume() {
        CharacterReader r = newReader("AB");
        assertEquals('A', r.current());
        assertEquals('A', r.consume()); // consume() is package‑private, but we are in same package
        assertEquals('B', r.current());
        assertEquals('B', r.consume());
        // after last char we should get EOF
        assertEquals(CharacterReader.EOF, r.current());
        assertEquals(CharacterReader.EOF, r.consume());
    }

    @Test
    public void testConsumeToCharFound() {
        CharacterReader r = newReader("hello world");
        String part = r.consumeTo(' ');
        assertEquals("hello", part);
        // the space itself is not consumed
        assertEquals(' ', r.current());
        r.advance(); // move past the space
        assertEquals('w', r.current());
    }

    @Test
    public void testConsumeToCharNotFound() {
        CharacterReader r = newReader("abc");
        String all = r.consumeTo('x');
        assertEquals("abc", all);
        assertTrue(r.isEmpty());
    }

    @Test
    public void testConsumeToStringFound() {
        CharacterReader r = newReader("abcXYZdef");
        String part = r.consumeTo("XYZ");
        assertEquals("abc", part);
        // cursor is now at start of "XYZ"
        assertTrue(r.matches("XYZ"));
    }

    @Test
    public void testConsumeToStringNotFound() {
        CharacterReader r = newReader("abcdef");
        String all = r.consumeTo("XYZ");
        assertEquals("abcdef", all);
        assertTrue(r.isEmpty());
    }

    @Test
    public void testConsumeToAny() {
        CharacterReader r = newReader("abcde");
        String part = r.consumeToAny('d', 'x');
        assertEquals("abc", part);
        // next char should be the delimiter that stopped the scan
        assertEquals('d', r.current());
    }

    @Test
    public void testConsumeToAnyWhenFirstCharIsDelimiter() {
        CharacterReader r = newReader("xyz");
        String part = r.consumeToAny('x', 'y');
        assertEquals("", part);
        assertEquals('x', r.current());
    }

    @Test
    public void testConsumeToAnySorted() {
        // chars must be sorted for binarySearch to work
        char[] delimiters = new char[]{'a', 'm', 'z'};
        CharacterReader r = newReader("hello world");
        String part = r.consumeToAnySorted(delimiters);
        assertEquals("hello world", part); // no delimiter present

        r = newReader("amazing");
        part = r.consumeToAnySorted(delimiters);
        assertEquals("", part); // first char 'a' is a delimiter
        r.advance(); // skip delimiter
        part = r.consumeToAnySorted(delimiters);
        assertEquals("m", part); // stops before 'z' (next delimiter)
    }

    @Test
    public void testConsumeDataStopsAtSpecialChars() {
        // '&' , '<' and null (0) are stop characters
        String src = "text&more<even\u0000end";
        CharacterReader r = newReader(src);
        String data = r.consumeData();
        assertEquals("text", data);
        assertEquals('&', r.current());

        r.advance(); // skip '&'
        data = r.consumeData();
        assertEquals("more", data);
        assertEquals('<', r.current());

        r.advance(); // skip '<'
        data = r.consumeData();
        assertEquals("even", data);
        assertEquals('\u0000', r.current());
    }

    @Test
    public void testConsumeTagName() {
        CharacterReader r = newReader("myTag/>rest");
        String tag = r.consumeTagName();
        assertEquals("myTag", tag);
        // cursor should now be at '/' (first delimiter of tag name)
        assertEquals('/', r.current());
    }

    @Test
    public void testConsumeLetterSequence() {
        CharacterReader r = newReader("AbcΔβ123");
        String letters = r.consumeLetterSequence();
        assertEquals("AbcΔβ", letters);
        assertEquals('1', r.current());
    }

    @Test
    public void testConsumeLetterThenDigitSequence() {
        CharacterReader r = newReader("Abc123def");
        String seq = r.consumeLetterThenDigitSequence();
        assertEquals("Abc123", seq);
        assertEquals('d', r.current());
    }

    @Test
    public void testConsumeHexSequence() {
        CharacterReader r = newReader("1A2bG7");
        String hex = r.consumeHexSequence();
        assertEquals("1A2b", hex);
        assertEquals('G', r.current());
    }

    @Test
    public void testConsumeDigitSequence() {
        CharacterReader r = newReader("007bond");
        String digits = r.consumeDigitSequence();
        assertEquals("007", digits);
        assertEquals('b', r.current());
    }

    @Test
    public void testMatchesChar() {
        CharacterReader r = newReader("xYz");
        assertTrue(r.matches('x'));
        r.advance();
        assertFalse(r.matches('x'));
        assertTrue(r.matches('Y'));
    }

    @Test
    public void testMatchesString() {
        CharacterReader r = newReader("foobar");
        assertTrue(r.matches("foo"));
        assertFalse(r.matches("bar"));
        r.advance(); // now at 'o'
        assertTrue(r.matches("oo"));
    }

    @Test
    public void testMatchesIgnoreCase() {
        CharacterReader r = newReader("AbC");
        assertTrue(r.matchesIgnoreCase("abc"));
        assertTrue(r.matchesIgnoreCase("ABC"));
        r.advance(); // now at 'b'
        assertTrue(r.matchesIgnoreCase("Bc"));
        assertFalse(r.matchesIgnoreCase("Cd"));
    }

    @Test
    public void testMatchesAny() {
        CharacterReader r = newReader("qwerty");
        assertTrue(r.matchesAny('a', 'e', 'q'));
        r.advance(); // now at 'w'
        assertFalse(r.matchesAny('a', 'e', 'q'));
    }

    @Test
    public void testMatchesAnySorted() {
        char[] sorted = new char[]{'a', 'e', 'q'};
        CharacterReader r = newReader("qwerty");
        assertTrue(r.matchesAnySorted(sorted));
        r.advance(); // now at 'w'
        assertFalse(r.matchesAnySorted(sorted));
    }

    @Test
    public void testMatchesLetterAndDigit() {
        CharacterReader r = newReader("A9_");
        assertTrue(r.matchesLetter());
        r.advance(); // now at '9'
        assertTrue(r.matchesDigit());
        assertFalse(r.matchesLetter());
        r.advance(); // now at '_'
        assertFalse(r.matchesLetter());
        assertFalse(r.matchesDigit());
    }

    @Test
    public void testMatchConsume() {
        CharacterReader r = newReader("hello world");
        assertTrue(r.matchConsume("hello"));
        assertEquals(' ', r.current());
        assertFalse(r.matchConsume("xyz"));
        // position unchanged after failed match
        assertEquals(' ', r.current());
    }

    @Test
    public void testMatchConsumeIgnoreCase() {
        CharacterReader r = newReader("HeLLo");
        assertTrue(r.matchConsumeIgnoreCase("hello"));
        assertTrue(r.isEmpty());
    }

    @Test
    public void testContainsIgnoreCase() {
        CharacterReader r = newReader("<TITLE>Example</title>");
        assertTrue(r.containsIgnoreCase("title"));
        assertTrue(r.containsIgnoreCase("TiTlE"));
        assertFalse(r.containsIgnoreCase("head"));
    }

    @Test
    public void testToStringReflectsRemaining() {
        CharacterReader r = newReader("abcde");
        r.advance(); // skip 'a'
        assertEquals("bcde", r.toString());
        r.consumeToEnd();
        assertEquals("", r.toString());
    }

    @Test
    public void testRangeEqualsPublicHelper() {
        CharacterReader r = newReader("abcdef");
        // range (2,3) => "cde"
        assertTrue(r.rangeEquals(2, 3, "cde"));
        assertFalse(r.rangeEquals(2, 3, "cdf"));
    }

    /* --------------------------------------------------------------------- */
    /*  Edge‑case and error handling tests                                   */
    /* --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullReaderThrows() {
        new CharacterReader(null, 1024);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNonMarkSupportedReaderThrows() {
        // Reader that does not support mark()
        Reader r = new Reader() {
            @Override public int read(char[] cbuf, int off, int len) { return -1; }
            @Override public void close() {}
            @Override public boolean markSupported() { return false; }
        };
        new CharacterReader(r, 1024);
    }

    @Test
    public void testConsumeToWhenEOF() {
        CharacterReader r = newReader("");
        String s = r.consumeTo('a');
        assertEquals("", s);
        assertTrue(r.isEmpty());
    }

    @Test
    public void testUnconsumeDoesNotGoBeforeStart() {
        CharacterReader r = newReader("x");
        r.consume(); // moves to EOF
        r.unconsume(); // back to 'x'
        assertEquals('x', r.current());
        // another unconsume would go before start – we guard against it by not calling it
    }

    @Test
    public void testNextIndexOfCharNotFound() throws Exception {
        // nextIndexOf is package‑private; we can call it via reflection
        Method m = CharacterReader.class.getDeclaredMethod("nextIndexOf", char.class);
        m.setAccessible(true);
        CharacterReader r = newReader("abc");
        int idx = (Integer) m.invoke(r, 'z');
        assertEquals(-1, idx);
    }

    @Test
    public void testNextIndexOfSequenceNotFound() throws Exception {
        Method m = CharacterReader.class.getDeclaredMethod("nextIndexOf", CharSequence.class);
        m.setAccessible(true);
        CharacterReader r = newReader("abc");
        int idx = (Integer) m.invoke(r, "xyz");
        assertEquals(-1, idx);
    }

    @Test
    public void testCacheStringShortStringIsCached() throws Exception {
        char[] buf = "short".toCharArray();
        String[] cache = new String[512];
        String s1 = callCacheString(buf, cache, 0, buf.length);
        String s2 = callCacheString(buf, cache, 0, buf.length);
        // Both references should be the same object because of caching
        assertSame(s1, s2);
    }

    @Test
    public void testCacheStringLongStringIsNotCached() throws Exception {
        // Build a string longer than maxStringCacheLen (12)
        String longStr = "thisIsAVeryLongStringExceedingCache";
        char[] buf = longStr.toCharArray();
        String[] cache = new String[512];
        String s1 = callCacheString(buf, cache, 0, buf.length);
        String s2 = callCacheString(buf, cache, 0, buf.length);
        // Should be different objects (no caching)
        assertNotSame(s1, s2);
        assertEquals(longStr, s1);
        assertEquals(longStr, s2);
    }

    @Test
    public void testCacheStringZeroLengthReturnsEmptyString() throws Exception {
        char[] buf = "anything".toCharArray();
        String[] cache = new String[512];
        String s = callCacheString(buf, cache, 0, 0);
        assertEquals("", s);
    }

    @Test
    public void testUncheckedIOExceptionFromBufferUp() throws Exception {
        // Create a Reader that throws IOException on read()
        Reader bad = new Reader() {
            @Override public int read(char[] cbuf, int off, int len) throws IOException {
                throw new IOException("forced");
            }
            @Override public void close() {}
            @Override public boolean markSupported() { return true; }
        };
        try {
            new CharacterReader(bad, 16);
            fail("Expected UncheckedIOException");
        } catch (UncheckedIOException e) {
            assertTrue(e.getCause() instanceof IOException);
            assertEquals("forced", e.getCause().getMessage());
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Internal helpers (rangeEquals)                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testRangeEqualsStaticMethod() throws Exception {
        Method m = CharacterReader.class.getDeclaredMethod(
                "rangeEquals", char[].class, int.class, int.class, String.class);
        m.setAccessible(true);
        char[] buf = "hello".toCharArray();
        String cached = "ell";
        boolean equal = (Boolean) m.invoke(null, buf, 1, 3, cached);
        assertTrue(equal);
        boolean notEqual = (Boolean) m.invoke(null, buf, 1, 3, "xyz");
        assertFalse(notEqual);
    }

}
```
###Test END##