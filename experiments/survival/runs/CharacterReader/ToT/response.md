###Test START##
```java
package org.jsoup.parser;

import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CharacterReader}.
 *
 * The tests are deliberately grouped by the public API surface of {@code CharacterReader}
 * and cover:
 * <ul>
 *   <li>normal usage scenarios</li>
 *   <li>edge cases (empty input, EOF, buffer boundaries)</li>
 *   <li>error conditions (null reader, non‑markable reader)</li>
 *   <li>internal caching behaviour (string cache, range equality)</li>
 * </ul>
 *
 * The suite is written to be self‑contained and does not rely on any external
 * resources.
 */
public class CharacterReaderTest {

    private static final int SMALL_BUFFER = 16;          // forces buffer reloads in some tests
    private static final int LARGE_BUFFER = 1024 * 32;   // default max buffer size

    private CharacterReader reader;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /** Helper to create a reader from a plain string using the default buffer size. */
    private CharacterReader newReader(String data) {
        return new CharacterReader(new StringReader(data), Math.min(data.length(), LARGE_BUFFER));
    }

    /** Helper to create a reader with a custom buffer size (used to test buffer‑reload logic). */
    private CharacterReader newReader(String data, int bufferSize) {
        return new CharacterReader(new StringReader(data), bufferSize);
    }

    @Before
    public void setUp() {
        // default reader used by many tests – overridden where needed
        reader = newReader("abcde");
    }

    // -------------------------------------------------------------------------
    // Constructor validation
    // -------------------------------------------------------------------------
    @Test
    public void constructorRejectsNullReader() {
        thrown.expect(NullPointerException.class);
        new CharacterReader(null);
    }

    @Test
    public void constructorRejectsNonMarkSupportedReader() {
        thrown.expect(IllegalArgumentException.class);
        Reader nonMark = new Reader() {
            @Override public int read(char[] cbuf, int off, int len) { return -1; }
            @Override public void close() {}
        };
        new CharacterReader(nonMark);
    }

    // -------------------------------------------------------------------------
    // Position / empty handling
    // -------------------------------------------------------------------------
    @Test
    public void posReflectsReaderAndBufferPosition() {
        assertEquals(0, reader.pos());
        reader.consume(); // 'a'
        assertEquals(1, reader.pos());
        reader.advance(); // skip 'b'
        assertEquals(2, reader.pos());
    }

    @Test
    public void isEmptyTrueWhenBufferConsumed() {
        CharacterReader r = newReader("");
        assertTrue(r.isEmpty());
        assertEquals(CharacterReader.EOF, r.current());
    }

    // -------------------------------------------------------------------------
    // Current / consume / unconsume / advance
    // -------------------------------------------------------------------------
    @Test
    public void currentReturnsCharWithoutConsuming() {
        assertEquals('a', reader.current());
        assertEquals(0, reader.pos()); // still at start
    }

    @Test
    public void consumeAdvancesPositionAndReturnsChar() {
        assertEquals('a', reader.consume());
        assertEquals('b', reader.current());
        assertEquals(1, reader.pos());
    }

    @Test
    public void unconsumeMovesBackOnePosition() {
        reader.consume(); // 'a'
        reader.consume(); // 'b'
        assertEquals(2, reader.pos());
        reader.unconsume();
        assertEquals(1, reader.pos());
        assertEquals('b', reader.current());
    }

    @Test
    public void advanceMovesPointerWithoutReturningChar() {
        reader.advance();
        assertEquals('b', reader.current());
        assertEquals(1, reader.pos());
    }

    // -------------------------------------------------------------------------
    // Mark / rewindToMark
    // -------------------------------------------------------------------------
    @Test
    public void markAndRewindRestorePosition() {
        reader.consume(); // a
        reader.mark();    // mark at 'b'
        reader.consume(); // b
        reader.consume(); // c
        assertEquals(3, reader.pos());
        reader.rewindToMark();
        assertEquals(1, reader.pos());
        assertEquals('b', reader.current());
    }

    // -------------------------------------------------------------------------
    // nextIndexOf (char) and (CharSequence)
    // -------------------------------------------------------------------------
    @Test
    public void nextIndexOfCharFindsOffsetOrMinusOne() {
        assertEquals(2, reader.nextIndexOf('c')); // "ab[c]de"
        assertEquals(-1, reader.nextIndexOf('z'));
    }

    @Test
    public void nextIndexOfCharHandlesBufferReload() {
        // Buffer size < input length forces reload; we look for a char after the reload point.
        String data = "1234567890ABCDEFGHIJ";
        CharacterReader r = newReader(data, 8);
        // Position at start, look for 'H' which is after the first buffer (index 16)
        assertEquals(16, r.nextIndexOf('H'));
    }

    @Test
    public void nextIndexOfSequenceFindsOffsetOrMinusOne() {
        assertEquals(0, reader.nextIndexOf("ab"));
        assertEquals(2, reader.nextIndexOf("cd"));
        assertEquals(-1, reader.nextIndexOf("zz"));
    }

    @Test
    public void nextIndexOfSequenceHandlesPartialMatchAtBufferEnd() {
        // Force a buffer split such that the sequence straddles the split.
        String data = "1234ABCDABEF";
        CharacterReader r = newReader(data, 6); // split after "1234AB"
        // "AB" starts at index 4, but "ABCD" spans the split.
        assertEquals(4, r.nextIndexOf("ABCD"));
    }

    // -------------------------------------------------------------------------
    // consumeTo (char) and consumeTo(String)
    // -------------------------------------------------------------------------
    @Test
    public void consumeToCharStopsBeforeDelimiter() {
        String result = reader.consumeTo('c');
        assertEquals("ab", result);
        assertEquals('c', reader.current());
    }

    @Test
    public void consumeToCharReturnsRestWhenDelimiterAbsent() {
        String result = reader.consumeTo('z');
        assertEquals("abcde", result);
        assertTrue(reader.isEmpty());
    }

    @Test
    public void consumeToStringStopsBeforeSequence() {
        String result = reader.consumeTo("cd");
        assertEquals("ab", result);
        assertEquals('c', reader.current());
    }

    @Test
    public void consumeToStringReturnsRestWhenSeqAbsent() {
        String result = reader.consumeTo("zz");
        assertEquals("abcde", result);
        assertTrue(reader.isEmpty());
    }

    // -------------------------------------------------------------------------
    // consumeToAny / consumeToAnySorted
    // -------------------------------------------------------------------------
    @Test
    public void consumeToAnyStopsAtFirstMatchingDelimiter() {
        String data = "foo<bar>baz";
        CharacterReader r = newReader(data);
        String consumed = r.consumeToAny('<', '>');
        assertEquals("foo", consumed);
        assertEquals('<', r.current());
    }

    @Test
    public void consumeToAnyReturnsEmptyWhenFirstCharIsDelimiter() {
        CharacterReader r = newReader("<start");
        assertEquals("", r.consumeToAny('<', '>'));
        assertEquals('<', r.current());
    }

    @Test
    public void consumeToAnySortedUsesBinarySearch() {
        // chars must be sorted for binarySearch to work
        char[] sorted = new char[] {'#', '&', '<', '>'};
        String data = "abc&def<ghi";
        CharacterReader r = newReader(data);
        String consumed = r.consumeToAnySorted(sorted);
        assertEquals("abc", consumed);
        assertEquals('&', r.current());
    }

    // -------------------------------------------------------------------------
    // consumeData / consumeTagName
    // -------------------------------------------------------------------------
    @Test
    public void consumeDataStopsAtSpecialChars() {
        String data = "Hello&World<Bye\u0000End";
        CharacterReader r = newReader(data);
        String consumed = r.consumeData();
        assertEquals("Hello", consumed);
        assertEquals('&', r.current());
    }

    @Test
    public void consumeTagNameStopsAtTagDelimiters() {
        String data = "div class=\"test\">";
        CharacterReader r = newReader(data);
        String name = r.consumeTagName();
        assertEquals("div", name);
        assertEquals(' ', r.current()); // space after tag name
    }

    // -------------------------------------------------------------------------
    // consumeToEnd
    // -------------------------------------------------------------------------
    @Test
    public void consumeToEndConsumesRemainingChars() {
        reader.consume(); // a
        String rest = reader.consumeToEnd();
        assertEquals("bcde", rest);
        assertTrue(reader.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Letter / digit / hex sequences
    // -------------------------------------------------------------------------
    @Test
    public void consumeLetterSequenceStopsAtNonLetter() {
        String data = "Abc123";
        CharacterReader r = newReader(data);
        assertEquals("Abc", r.consumeLetterSequence());
        assertEquals('1', r.current());
    }

    @Test
    public void consumeLetterThenDigitSequenceStopsAtFirstNonLetterOrDigit() {
        String data = "Abc123xyz";
        CharacterReader r = newReader(data);
        assertEquals("Abc123", r.consumeLetterThenDigitSequence());
        assertEquals('x', r.current());
    }

    @Test
    public void consumeHexSequenceStopsAtFirstNonHexChar() {
        String data = "1a2B3g4";
        CharacterReader r = newReader(data);
        assertEquals("1a2B3", r.consumeHexSequence());
        assertEquals('g', r.current());
    }

    @Test
    public void consumeDigitSequenceStopsAtFirstNonDigit() {
        String data = "01234abc";
        CharacterReader r = newReader(data);
        assertEquals("01234", r.consumeDigitSequence());
        assertEquals('a', r.current());
    }

    // -------------------------------------------------------------------------
    // Matching helpers
    // -------------------------------------------------------------------------
    @Test
    public void matchesCharWorks() {
        assertTrue(reader.matches('a'));
        reader.consume(); // move to 'b'
        assertFalse(reader.matches('a'));
    }

    @Test
    public void matchesStringWorksAndDoesNotConsume() {
        assertTrue(reader.matches("ab"));
        assertEquals('a', reader.current()); // still at start
    }

    @Test
    public void matchesIgnoreCaseWorks() {
        CharacterReader r = newReader("AbC");
        assertTrue(r.matchesIgnoreCase("abc"));
        assertFalse(r.matchesIgnoreCase("abd"));
    }

    @Test
    public void matchesAnyWorks() {
        assertTrue(reader.matchesAny('x', 'a', 'y'));
        assertFalse(reader.matchesAny('x', 'y', 'z'));
    }

    @Test
    public void matchesAnySortedWorks() {
        char[] sorted = new char[] {'a', 'c', 'e'};
        assertTrue(reader.matchesAnySorted(sorted));
        reader.consume(); // now at 'b'
        assertFalse(reader.matchesAnySorted(sorted));
    }

    @Test
    public void matchesLetterAndDigit() {
        CharacterReader r = newReader("A9_");
        assertTrue(r.matchesLetter());
        r.consume(); // '9'
        assertTrue(r.matchesDigit());
        r.consume(); // '_'
        assertFalse(r.matchesLetter());
        assertFalse(r.matchesDigit());
    }

    // -------------------------------------------------------------------------
    // matchConsume / matchConsumeIgnoreCase
    // -------------------------------------------------------------------------
    @Test
    public void matchConsumeConsumesWhenMatch() {
        assertTrue(reader.matchConsume("ab"));
        assertEquals('c', reader.current());
    }

    @Test
    public void matchConsumeDoesNotConsumeWhenNoMatch() {
        assertFalse(reader.matchConsume("zz"));
        assertEquals('a', reader.current());
    }

    @Test
    public void matchConsumeIgnoreCaseConsumesCorrectly() {
        CharacterReader r = newReader("AbC");
        assertTrue(r.matchConsumeIgnoreCase("aBc"));
        assertEquals(' ', r.current()); // end of string (EOF)
    }

    // -------------------------------------------------------------------------
    // containsIgnoreCase
    // -------------------------------------------------------------------------
    @Test
    public void containsIgnoreCaseFindsBothCases() {
        CharacterReader r = newReader("<TiTlE>Example</TiTlE>");
        assertTrue(r.containsIgnoreCase("</title>"));
        assertTrue(r.containsIgnoreCase("</TITLE>"));
        assertFalse(r.containsIgnoreCase("</script>"));
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------
    @Test
    public void toStringReflectsRemainingBuffer() {
        reader.consume(); // 'a'
        assertEquals("bcde", reader.toString());
    }

    // -------------------------------------------------------------------------
    // Cache behaviour (private static method exercised via public API)
    // -------------------------------------------------------------------------
    @Test
    public void shortStringsAreCachedAndReturnedByIdentity() {
        CharacterReader r = newReader("abcabc");
        String first = r.consumeTo('a'); // consume nothing, returns ""
        assertSame("", first); // empty strings are always the same instance

        r = newReader("xyzxyz");
        String part1 = r.consumeTo('y'); // "x"
        String part2 = r.consumeTo('z'); // "y"
        // Both "x" and "y" are < maxStringCacheLen, should be cached.
        assertSame(part1, r.consumeTo('x')); // re‑consume "x" after rewinding
        r.unconsume(); // go back one char to 'y'
        assertSame(part2, r.consumeTo('z')); // re‑consume "y"
    }

    @Test
    public void longStringsAreNotCached() {
        // length > maxStringCacheLen (12) => new instance each call
        String longStr = "thisIsAVeryLongString";
        CharacterReader r = newReader(longStr);
        String first = r.consumeTo('Z'); // delimiter not present -> whole string
        r.rewindToMark(); // reset position
        String second = r.consumeTo('Z');
        assertNotSame(first, second);
        assertEquals(first, second);
    }

    // -------------------------------------------------------------------------
    // rangeEquals (public static and instance versions)
    // -------------------------------------------------------------------------
    @Test
    public void staticRangeEqualsDetectsEquality() {
        char[] buf = "hello".toCharArray();
        assertTrue(CharacterReader.rangeEquals(buf, 0, 5, "hello"));
        assertFalse(CharacterReader.rangeEquals(buf, 1, 4, "ello"));
    }

    @Test
    public void instanceRangeEqualsDelegatesToStatic() {
        CharacterReader r = newReader("world");
        assertTrue(r.rangeEquals(0, 5, "world"));
        assertFalse(r.rangeEquals(1, 3, "orl"));
    }

    // -------------------------------------------------------------------------
    // Buffer reload edge cases (readAheadLimit and split point)
    // -------------------------------------------------------------------------
    @Test
    public void bufferReloadOccursWhenPosReachesSplitPoint() {
        // Create a string longer than readAheadLimit (which is 75% of maxBufferLen)
        // Use a small buffer size to force multiple reloads.
        String data = "abcdefghijklmnopqrstuvwxyz";
        CharacterReader r = newReader(data, 8); // split after 6 chars (75% of 8 = 6)
        // Consume up to split point
        for (int i = 0; i < 6; i++) {
            assertEquals(data.charAt(i), r.consume());
        }
        // At this moment bufferUp() should have been called internally.
        // Next char should be the 7th character.
        assertEquals('g', r.current());
    }

    @Test
    public void consumeToEndAfterMultipleReloadsReturnsFullRemainingString() {
        String data = "0123456789ABCDEFGHIJ";
        CharacterReader r = newReader(data, 5); // forces several reloads
        // Consume first three chars manually
        r.consume(); // 0
        r.consume(); // 1
        r.consume(); // 2
        String rest = r.consumeToEnd();
        assertEquals("3456789ABCDEFGHIJ", rest);
        assertTrue(r.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Error scenario: unconsume beyond start (should not throw, but position becomes negative)
    // -------------------------------------------------------------------------
    @Test
    public void unconsumeCanMoveBeforeStartWithoutException() {
        // The class does not guard against negative bufPos; this test documents the behaviour.
        CharacterReader r = newReader("x");
        r.unconsume(); // now bufPos == -1
        // current() will trigger bufferUp and then return EOF because bufPos < 0 leads to ArrayIndexOutOfBounds
        // we verify that an exception is thrown rather than silent corruption.
        try {
            r.current();
            fail("Expected ArrayIndexOutOfBoundsException due to negative bufPos");
        } catch (ArrayIndexOutOfBoundsException expected) {
            // expected behaviour
        }
    }

    // -------------------------------------------------------------------------
    // toString on empty reader
    // -------------------------------------------------------------------------
    @Test
    public void toStringOnEmptyReaderReturnsEmptyString() {
        CharacterReader r = newReader("");
        assertEquals("", r.toString());
    }
}
```
###Test END##