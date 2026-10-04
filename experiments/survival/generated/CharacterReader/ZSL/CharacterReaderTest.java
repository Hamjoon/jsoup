package org.jsoup.parser;

import org.junit.Test;
import org.junit.Before;

import java.io.StringReader;
import java.util.Locale;

import static org.junit.Assert.*;

public class CharacterReaderTest {

    private static final char EOF = CharacterReader.EOF;

    /** Helper to create a reader with default buffer size. */
    private CharacterReader newReader(String data) {
        return new CharacterReader(new StringReader(data));
    }

    /** Helper to create a reader with a custom buffer size (used to force buffer refill). */
    private CharacterReader newReader(String data, int bufSize) {
        return new CharacterReader(new StringReader(data), bufSize);
    }

    @Test
    public void testEmptyInput() {
        CharacterReader r = newReader("");
        assertTrue(r.isEmpty());
        assertEquals(0, r.pos());
        assertEquals(EOF, r.current());
        assertEquals("", r.toString());
    }

    @Test
    public void testCurrentConsumeAndAdvance() {
        CharacterReader r = newReader("abc");
        assertEquals('a', r.current());
        assertEquals('a', r.consume());
        assertEquals(1, r.pos());
        assertEquals('b', r.current());
        r.advance(); // skip 'b'
        assertEquals('c', r.current());
        assertEquals('c', r.consume());
        assertTrue(r.isEmpty());
        assertEquals(EOF, r.current());
    }

    @Test
    public void testUnconsumeAndMarkRewind() {
        CharacterReader r = newReader("xyz");
        assertEquals('x', r.consume()); // pos 1
        r.unconsume(); // back to 0
        assertEquals('x', r.current());
        r.consume(); // pos 1 again
        r.mark(); // mark at 1
        r.consume(); // consume 'y' -> pos 2
        r.consume(); // consume 'z' -> pos 3 (empty)
        assertTrue(r.isEmpty());
        r.rewindToMark(); // back to mark (pos 1)
        assertEquals('y', r.current());
    }

    @Test
    public void testNextIndexOfChar() {
        CharacterReader r = newReader("abca");
        assertEquals(2, r.nextIndexOf('c'));
        r.consume(); // now at 'b'
        assertEquals(2, r.nextIndexOf('a')); // positions: b c a => a at offset 2
        r.consume(); // at 'c'
        assertEquals(1, r.nextIndexOf('a'));
        r.consume(); // at 'a'
        assertEquals(-1, r.nextIndexOf('z'));
    }

    @Test
    public void testNextIndexOfSeq() {
        CharacterReader r = newReader("abcabc");
        assertEquals(2, r.nextIndexOf("cab"));
        assertEquals(0, r.nextIndexOf("abc"));
        r.consume(); // move pos 1
        assertEquals(1, r.nextIndexOf("abc"));
        assertEquals(-1, r.nextIndexOf("zzz"));
    }

    @Test
    public void testConsumeToChar() {
        CharacterReader r = newReader("hello world");
        String consumed = r.consumeTo(' ');
        assertEquals("hello", consumed);
        assertEquals(' ', r.current()); // delimiter not consumed
        assertEquals(5, r.pos());
    }

    @Test
    public void testConsumeToString() {
        CharacterReader r = newReader("foo=bar;baz");
        String consumed = r.consumeTo(";");
        assertEquals("foo=bar", consumed);
        assertEquals(';', r.current());
    }

    @Test
    public void testConsumeToAny() {
        CharacterReader r = newReader("one,two;three|four");
        String consumed = r.consumeToAny(',', ';', '|');
        assertEquals("one", consumed);
        assertEquals(',', r.current());

        // advance past delimiter and test next call
        r.advance();
        consumed = r.consumeToAny(',', ';', '|');
        assertEquals("two", consumed);
        assertEquals(';', r.current());
    }

    @Test
    public void testConsumeToAnySorted() {
        // chars must be sorted for binarySearch
        char[] delimiters = new char[]{',', ';', '|'};
        CharacterReader r = newReader("alpha;beta,gamma|delta");
        String consumed = r.consumeToAnySorted(delimiters);
        assertEquals("alpha", consumed);
        assertEquals(';', r.current());
    }

    @Test
    public void testConsumeDataStopsAtSpecialChars() {
        CharacterReader r = newReader("text&more<stuff");
        String data = r.consumeData();
        assertEquals("text", data);
        assertEquals('&', r.current());
    }

    @Test
    public void testConsumeTagName() {
        CharacterReader r = newReader("div class=\"x\"");
        String tag = r.consumeTagName();
        assertEquals("div", tag);
        assertTrue(r.matches(' '));
    }

    @Test
    public void testConsumeToEnd() {
        CharacterReader r = newReader("partial");
        r.consume(); // consume 'p'
        String rest = r.consumeToEnd();
        assertEquals("artial", rest);
        assertTrue(r.isEmpty());
    }

    @Test
    public void testConsumeLetterSequence() {
        CharacterReader r = newReader("Abc123XYZ");
        String letters = r.consumeLetterSequence();
        assertEquals("Abc", letters);
        assertEquals('1', r.current());
    }

    @Test
    public void testConsumeLetterThenDigitSequence() {
        CharacterReader r = newReader("Abc123XYZ");
        String seq = r.consumeLetterThenDigitSequence();
        assertEquals("Abc123", seq);
        assertEquals('X', r.current());
    }

    @Test
    public void testConsumeHexSequence() {
        CharacterReader r = newReader("1a2Fz");
        String hex = r.consumeHexSequence();
        assertEquals("1a2F", hex);
        assertEquals('z', r.current());
    }

    @Test
    public void testConsumeDigitSequence() {
        CharacterReader r = newReader("007bond");
        String digits = r.consumeDigitSequence();
        assertEquals("007", digits);
        assertEquals('b', r.current());
    }

    @Test
    public void testMatchesMethods() {
        CharacterReader r = newReader("abcDEF");
        assertTrue(r.matches('a'));
        assertTrue(r.matches("abc"));
        assertFalse(r.matches("abC"));
        assertTrue(r.matchesIgnoreCase("ABC"));
        assertTrue(r.matchesIgnoreCase("Abc"));
        assertFalse(r.matchesIgnoreCase("abx"));
        assertTrue(r.matchesAny('x', 'a', 'y'));
        assertFalse(r.matchesAny('x', 'y', 'z'));

        // sorted version
        char[] sorted = new char[]{'a', 'b', 'c'};
        assertTrue(r.matchesAnySorted(sorted));
        char[] sorted2 = new char[]{'x', 'y', 'z'};
        assertFalse(r.matchesAnySorted(sorted2));
    }

    @Test
    public void testMatchesLetterAndDigit() {
        CharacterReader r = newReader("A9_");
        assertTrue(r.matchesLetter());
        r.consume(); // now at '9'
        assertTrue(r.matchesDigit());
        r.consume(); // now at '_'
        assertFalse(r.matchesLetter());
        assertFalse(r.matchesDigit());
    }

    @Test
    public void testMatchConsumeMethods() {
        CharacterReader r = newReader("helloWorld");
        assertTrue(r.matchConsume("hello"));
        assertEquals('W', r.current());
        assertFalse(r.matchConsume("world")); // case-sensitive
        assertTrue(r.matchConsumeIgnoreCase("world"));
        assertTrue(r.isEmpty());
    }

    @Test
    public void testContainsIgnoreCase() {
        CharacterReader r = newReader("TheQuickBrownFox");
        assertTrue(r.containsIgnoreCase("quick"));
        assertTrue(r.containsIgnoreCase("QUICK"));
        assertFalse(r.containsIgnoreCase("slow"));
    }

    @Test
    public void testToStringReflectsRemaining() {
        CharacterReader r = newReader("remaining");
        r.consume(); // consume 'r'
        assertEquals("emaining", r.toString());
    }

    @Test
    public void testRangeEqualsStatic() {
        char[] arr = "sample".toCharArray();
        assertTrue(CharacterReader.rangeEquals(arr, 0, 6, "sample"));
        assertFalse(CharacterReader.rangeEquals(arr, 0, 6, "example"));
        assertFalse(CharacterReader.rangeEquals(arr, 1, 5, "ample")); // start offset mismatch
    }

    @Test
    public void testCacheStringReferenceEquality() {
        // Strings of length <= 12 are cached. Call consumeLetterSequence twice on same content.
        String input = "abcXYZabcXYZ";
        CharacterReader r = newReader(input);
        String first = r.consumeLetterSequence(); // "abcXYZ"
        String second = r.consumeLetterSequence(); // now reads "abcXYZ" again after digits? Actually after first call, pos at after letters (no digits). We'll reset the reader.
        r = newReader(input);
        String a = r.consumeLetterSequence(); // "abcXYZ"
        String b = r.consumeLetterSequence(); // same substring again (since we didn't move beyond)
        // The two strings should be the same cached instance
        assertSame(a, b);
    }

    @Test
    public void testBufferRefillWithSmallBuffer() {
        // Use a small buffer size to force multiple bufferUp calls.
        String data = "abcdefghijklmnopqrstuvwxyz";
        int bufSize = 10; // smaller than data length
        CharacterReader r = newReader(data, bufSize);
        StringBuilder sb = new StringBuilder();
        while (!r.isEmpty()) {
            sb.append(r.consume());
        }
        assertEquals(data, sb.toString());
        assertEquals(data.length(), r.pos());
    }

    @Test
    public void testConsumeToAnyWhenNoDelimiter() {
        CharacterReader r = newReader("nospecialchars");
        String consumed = r.consumeToAny('!', '@');
        assertEquals("nospecialchars", consumed);
        assertTrue(r.isEmpty());
    }

    @Test
    public void testConsumeToAnySortedWhenNoDelimiter() {
        char[] delimiters = new char[]{'!', '@'};
        CharacterReader r = newReader("plaintext");
        String consumed = r.consumeToAnySorted(delimiters);
        assertEquals("plaintext", consumed);
        assertTrue(r.isEmpty());
    }
}
