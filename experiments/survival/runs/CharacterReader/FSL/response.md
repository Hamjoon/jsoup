###Test START##
package org.jsoup.parser;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.Locale;

import org.junit.Test;

public class CharacterReaderTest {

    // Helper to create a CharacterReader with a small buffer to force bufferUp logic
    private CharacterReader reader(String data) {
        return new CharacterReader(new StringReader(data), 32);
    }

    @Test
    public void testPosAndIsEmpty() {
        CharacterReader r = reader("abc");
        assertEquals(0, r.pos());
        assertFalse(r.isEmpty());

        r.consume(); // 'a'
        assertEquals(1, r.pos());
        assertFalse(r.isEmpty());

        r.consume(); // 'b'
        r.consume(); // 'c'
        assertEquals(3, r.pos());
        assertTrue(r.isEmpty());
    }

    @Test
    public void testCurrentConsumeUnconsume() {
        CharacterReader r = reader("xy");
        assertEquals('x', r.current());
        assertEquals('x', r.consume());
        assertEquals('y', r.current());
        r.unconsume();
        assertEquals('x', r.current());
        assertEquals('x', r.consume());
        assertEquals('y', r.consume());
        assertEquals(CharacterReader.EOF, r.current());
    }

    @Test
    public void testAdvanceMarkRewind() {
        CharacterReader r = reader("12345");
        r.advance(); // skip '1'
        assertEquals('2', r.current());
        r.mark();    // mark at '2'
        r.advance(); // now at '3'
        assertEquals('3', r.current());
        r.rewindToMark();
        assertEquals('2', r.current());
    }

    @Test
    public void testNextIndexOfChar() {
        CharacterReader r = reader("abca");
        assertEquals(1, r.nextIndexOf('b'));
        assertEquals(3, r.nextIndexOf('a'));
        assertEquals(-1, r.nextIndexOf('z'));
        r.consume(); // move past first 'a'
        assertEquals(0, r.nextIndexOf('b')); // now at 'b'
    }

    @Test
    public void testNextIndexOfCharSequence() {
        CharacterReader r = reader("foo bar foo");
        assertEquals(0, r.nextIndexOf("foo"));
        r.consume(); // consume 'f'
        r.consume(); // consume 'o'
        r.consume(); // consume 'o'
        assertEquals(4, r.nextIndexOf("bar"));
        assertEquals(-1, r.nextIndexOf("baz"));
    }

    @Test
    public void testConsumeToChar() {
        CharacterReader r = reader("abc,def");
        String consumed = r.consumeTo(',');
        assertEquals("abc", consumed);
        assertEquals(',', r.current()); // delimiter not consumed
        r.consume(); // consume delimiter
        assertEquals('d', r.current());
    }

    @Test
    public void testConsumeToString() {
        CharacterReader r = reader("hello world END more");
        String consumed = r.consumeTo("END");
        assertEquals("hello world ", consumed);
        assertEquals('E', r.current()); // start of END not consumed
    }

    @Test
    public void testConsumeToAny() {
        CharacterReader r = reader("one|two,three;four");
        String consumed = r.consumeToAny('|', ',', ';');
        assertEquals("one", consumed);
        assertEquals('|', r.current());
        r.consume(); // skip delimiter
        consumed = r.consumeToAny('|', ',', ';');
        assertEquals("two", consumed);
        assertEquals(',', r.current());
    }

    @Test
    public void testConsumeToAnySorted() {
        CharacterReader r = reader("abc,def;ghi");
        // chars must be sorted for binarySearch
        char[] delimiters = {',', ';'};
        String consumed = r.consumeToAnySorted(delimiters);
        assertEquals("abc", consumed);
        assertEquals(',', r.current());
        r.consume(); // skip delimiter
        consumed = r.consumeToAnySorted(delimiters);
        assertEquals("def", consumed);
        assertEquals(';', r.current());
    }

    @Test
    public void testConsumeDataStopsAtSpecialChars() {
        CharacterReader r = reader("text&more<end");
        String consumed = r.consumeData();
        assertEquals("text", consumed);
        assertEquals('&', r.current());
        r.consume(); // consume '&'
        consumed = r.consumeData();
        assertEquals("more", consumed);
        assertEquals('<', r.current());
    }

    @Test
    public void testConsumeTagName() {
        CharacterReader r = reader("div class='test'>");
        String tag = r.consumeTagName();
        assertEquals("div", tag);
        // next char should be space
        assertEquals(' ', r.current());
    }

    @Test
    public void testConsumeLetterSequence() {
        CharacterReader r = reader("Abc123");
        String letters = r.consumeLetterSequence();
        assertEquals("Abc", letters);
        assertEquals('1', r.current());
    }

    @Test
    public void testConsumeLetterThenDigitSequence() {
        CharacterReader r = reader("Abc123def");
        String seq = r.consumeLetterThenDigitSequence();
        assertEquals("Abc123", seq);
        assertEquals('d', r.current());
    }

    @Test
    public void testConsumeHexSequence() {
        CharacterReader r = reader("1aFz");
        String hex = r.consumeHexSequence();
        assertEquals("1aF", hex);
        assertEquals('z', r.current());
    }

    @Test
    public void testConsumeDigitSequence() {
        CharacterReader r = reader("007bond");
        String digits = r.consumeDigitSequence();
        assertEquals("007", digits);
        assertEquals('b', r.current());
    }

    @Test
    public void testMatchesAndMatchConsume() {
        CharacterReader r = reader("xyz");
        assertTrue(r.matches('x'));
        assertTrue(r.matches("xy"));
        assertFalse(r.matches("xz"));
        assertTrue(r.matchesIgnoreCase("XY"));
        assertTrue(r.matchesAny('a', 'x', 'z'));
        assertFalse(r.matchesAny('a', 'b'));
        assertTrue(r.matchesLetter());
        assertFalse(r.matchesDigit());

        assertTrue(r.matchConsume("xy"));
        assertEquals('z', r.current());

        // reset
        r = reader("ABC");
        assertTrue(r.matchConsumeIgnoreCase("ab"));
        assertEquals('C', r.current());
    }

    @Test
    public void testMatchesAnySorted() {
        CharacterReader r = reader("m");
        char[] sorted = {'a', 'm', 'z'};
        assertTrue(r.matchesAnySorted(sorted));
        r = reader("b");
        assertFalse(r.matchesAnySorted(sorted));
    }

    @Test
    public void testContainsIgnoreCase() {
        CharacterReader r = reader("Hello World");
        assertTrue(r.containsIgnoreCase("world"));
        assertTrue(r.containsIgnoreCase("WORLD"));
        assertFalse(r.containsIgnoreCase("test"));
    }

    @Test
    public void testToStringRemaining() {
        CharacterReader r = reader("abcde");
        r.consume(); // 'a'
        r.consume(); // 'b'
        assertEquals("cde", r.toString());
    }

    @Test
    public void testRangeEqualsStaticAndInstance() {
        char[] buf = {'h', 'e', 'l', 'l', 'o'};
        String s = "hello";
        assertTrue(CharacterReader.rangeEquals(buf, 0, 5, s));

        CharacterReader r = reader("hello");
        assertTrue(r.rangeEquals(0, 5, s));
        assertFalse(r.rangeEquals(1, 4, "hell"));
    }

    @Test
    public void testConsumeToEnd() {
        CharacterReader r = reader("final");
        String rest = r.consumeToEnd();
        assertEquals("final", rest);
        assertTrue(r.isEmpty());
        assertEquals(CharacterReader.EOF, r.current());
    }
}
###Test END##