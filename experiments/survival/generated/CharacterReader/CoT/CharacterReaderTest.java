package org.jsoup.parser;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.Reader;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.*;

/**
 * JUnit4 test suite for {@link CharacterReader}.
 *
 * The tests cover:
 * 1. Public API signatures.
 * 2. Basic functional behaviour of each public method.
 * 3. Edge‑cases such as empty input, delimiters not present, EOF handling and
 *    constructor validation (reader without mark support).
 */
public class CharacterReaderTest {

    // ----------------------------------------------------------------------
    // 1.  List of public methods (including constructors) of CharacterReader
    // ----------------------------------------------------------------------
    //
    // public CharacterReader(Reader input, int sz)
    // public CharacterReader(Reader input)
    // public CharacterReader(String input)
    // public int pos()
    // public boolean isEmpty()
    // public char current()
    // public void advance()
    // public String consumeTo(char c)
    // public String consumeToAny(char... chars)
    // public String toString()
    //

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // ----------------------------------------------------------------------
    // Helper Readers
    // ----------------------------------------------------------------------
    /** A Reader that does NOT support mark/reset – used to test constructor validation. */
    private static class NonMarkReader extends Reader {
        private final String data;
        private int idx = 0;

        NonMarkReader(String data) { this.data = data; }

        @Override public int read(char[] cbuf, int off, int len) throws IOException {
            if (idx >= data.length()) return -1;
            int toCopy = Math.min(len, data.length() - idx);
            data.getChars(idx, idx + toCopy, cbuf, off);
            idx += toCopy;
            return toCopy;
        }

        @Override public void close() throws IOException { }

        @Override public boolean markSupported() { return false; }

        @Override public void mark(int readAheadLimit) throws IOException {
            throw new IOException("mark not supported");
        }

        @Override public void reset() throws IOException {
            throw new IOException("reset not supported");
        }
    }

    // ----------------------------------------------------------------------
    // Basic functional tests for each public method
    // ----------------------------------------------------------------------
    @Test
    public void testStringConstructor_andBasicReading() {
        String src = "abc";
        CharacterReader cr = new CharacterReader(src);

        // initial position
        assertEquals(0, cr.pos());

        // current char should be first character
        assertEquals('a', cr.current());

        // advance one step
        cr.advance();
        assertEquals(1, cr.pos());
        assertEquals('b', cr.current());

        // consumeTo should read up to delimiter and leave cursor before it
        String consumed = cr.consumeTo('c');
        assertEquals("b", consumed);
        // cursor now points at 'c'
        assertEquals('c', cr.current());

        // after consuming the last character, next current() returns EOF
        cr.advance();
        assertTrue(cr.isEmpty());
        assertEquals(CharacterReader.EOF, cr.current());
    }

    @Test
    public void testReaderConstructor_withExplicitSize() {
        Reader r = new StringReader("xyz");
        CharacterReader cr = new CharacterReader(r, 64);   // size less than maxBufferLen

        assertEquals('x', cr.current());
        cr.advance();
        assertEquals('y', cr.current());

        // consumeTo with a delimiter that does not exist returns the rest of the input
        String rest = cr.consumeTo('q'); // 'q' not present
        assertEquals("yz", rest);
        assertTrue(cr.isEmpty());
    }

    @Test
    public void testConsumeToAny_delimitersAtVariousPositions() {
        CharacterReader cr = new CharacterReader("ab<c>d");

        // delimiter '<' appears at position 2
        String before = cr.consumeToAny('<', '>');
        assertEquals("ab", before);
        // cursor now points at '<'
        assertEquals('<', cr.current());

        // advance over '<' and consume to '>'
        cr.advance();
        String middle = cr.consumeToAny('>');
        assertEquals("c", middle);
        assertEquals('>', cr.current());

        // consume the rest (nothing before next '>') should give empty string
        cr.advance(); // move past '>'
        String empty = cr.consumeToAny('x', 'y');
        assertEquals("", empty);
        assertTrue(cr.isEmpty());
    }

    @Test
    public void testToStringReflectsRemainingBuffer() {
        CharacterReader cr = new CharacterReader("hello world");
        assertEquals("hello world", cr.toString());

        cr.advance(); // skip 'h'
        assertEquals("ello world", cr.toString());

        cr.consumeTo(' ');
        assertEquals("world", cr.toString());
    }

    @Test
    public void testPosAdvancesCorrectlyAcrossMultipleCalls() {
        CharacterReader cr = new CharacterReader("012345");
        assertEquals(0, cr.pos());
        cr.advance(); // 1
        assertEquals(1, cr.pos());
        cr.consumeTo('4'); // consumes "123"
        assertEquals(4, cr.pos()); // now at '4'
        cr.advance(); // skip '4'
        assertEquals(5, cr.pos());
        cr.consumeToEnd(); // consumes "5"
        assertEquals(6, cr.pos()); // end of input
        assertTrue(cr.isEmpty());
    }

    @Test
    public void testCurrentReturnsEOFWhenEmpty() {
        CharacterReader cr = new CharacterReader("");
        assertTrue(cr.isEmpty());
        assertEquals(CharacterReader.EOF, cr.current());
    }

    // ----------------------------------------------------------------------
    // Edge‑case and exception handling tests
    // ----------------------------------------------------------------------
    @Test
    public void testConsumeToWhenDelimiterIsFirstChar() {
        CharacterReader cr = new CharacterReader(",abc");
        String consumed = cr.consumeTo(',');
        // delimiter is at current position, so offset = 0, result should be empty string
        assertEquals("", consumed);
        // cursor should still be at the delimiter
        assertEquals(',', cr.current());
    }

    @Test
    public void testConsumeToAnyWithEmptyInput() {
        CharacterReader cr = new CharacterReader("");
        String result = cr.consumeToAny('a', 'b');
        assertEquals("", result);
        assertTrue(cr.isEmpty());
    }

    @Test
    public void testConsumeToAnyWhenDelimiterIsAtStart() {
        CharacterReader cr = new CharacterReader("!start");
        String result = cr.consumeToAny('!');
        // delimiter is at position 0, so nothing is consumed
        assertEquals("", result);
        assertEquals('!', cr.current());
    }

    @Test
    public void testConstructorRejectsReaderWithoutMarkSupport() {
        thrown.expect(IllegalArgumentException.class);
        Reader bad = new NonMarkReader("no-mark");
        new CharacterReader(bad); // should throw because markSupported() == false
    }

    @Test
    public void testStringCacheReusesIdenticalSubstrings() {
        // The cache works only for strings <= maxStringCacheLen (12). Use a short repeat.
        CharacterReader cr = new CharacterReader("abcabcabc");
        String first = cr.consumeTo('c'); // consumes "ab"
        cr.advance(); // skip 'c'
        String second = cr.consumeTo('c'); // consumes "ab" again

        // Because both substrings are equal and short, they should be the SAME object
        assertSame(first, second);
    }

    @Test
    public void testConsumeToWhenDelimiterNotPresentReturnsRest() {
        CharacterReader cr = new CharacterReader("foobar");
        String consumed = cr.consumeTo('z'); // 'z' never appears
        assertEquals("foobar", consumed);
        assertTrue(cr.isEmpty());
    }

    @Test
    public void testAdvanceBeyondEndDoesNotThrow() {
        CharacterReader cr = new CharacterReader("x");
        assertEquals('x', cr.current());
        cr.advance(); // now at EOF
        assertTrue(cr.isEmpty());
        // advancing again should stay at EOF and not throw
        cr.advance();
        assertTrue(cr.isEmpty());
        assertEquals(CharacterReader.EOF, cr.current());
    }

    @Test
    public void testPosAfterConsumeToEnd() {
        CharacterReader cr = new CharacterReader("data");
        cr.consumeToEnd(); // consumes everything
        assertEquals(4, cr.pos());
        assertTrue(cr.isEmpty());
    }
}
