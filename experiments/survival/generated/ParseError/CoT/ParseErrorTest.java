package org.jsoup.parser;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test suite for {@link ParseError}.
 * All tests are written using JUnit 4.
 */
public class ParseErrorTest {

    /* ----------------------------------------------------------------------
     *  Public method signatures extracted from ParseError
     * ---------------------------------------------------------------------- */
    // public String getErrorMessage()
    // public int getPosition()
    // public String toString()

    /* ----------------------------------------------------------------------
     *  Basic functionality tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetErrorMessage_ReturnsMessageProvidedInConstructor() {
        String msg = "unexpected token";
        ParseError error = new ParseError(10, msg);
        assertEquals("Error message should be returned unchanged", msg, error.getErrorMessage());
    }

    @Test
    public void testGetPosition_ReturnsPositionProvidedInConstructor() {
        int position = 42;
        ParseError error = new ParseError(position, "some error");
        assertEquals("Position should be returned unchanged", position, error.getPosition());
    }

    @Test
    public void testToString_FormatsCorrectly() {
        int pos = 5;
        String msg = "missing >";
        ParseError error = new ParseError(pos, msg);
        assertEquals("toString should concatenate position and message", pos + ": " + msg, error.toString());
    }

    @Test
    public void testFormattedConstructor_FormatsMessageCorrectly() {
        int pos = 7;
        String fmt = "expected %s but found %s";
        ParseError error = new ParseError(pos, fmt, "foo", "bar");
        assertEquals("Message should be formatted according to the format string",
                "expected foo but found bar", error.getErrorMessage());
        assertEquals("Position should be stored correctly", pos, error.getPosition());
    }

    /* ----------------------------------------------------------------------
     *  Edge‑case and exception handling tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testConstructor_AllowsNullMessage() {
        ParseError error = new ParseError(0, (String) null);
        assertNull("Error message may be null", error.getErrorMessage());
        assertEquals(0, error.getPosition());
        assertEquals("0: null", error.toString()); // String concatenation of null yields "null"
    }

    @Test
    public void testFormattedConstructor_AllowsNullArgumentsArray() {
        // Passing null as the var‑args array is legal – it is treated as a single null argument.
        int pos = 3;
        String fmt = "value is %s";
        ParseError error = new ParseError(pos, fmt, (Object[]) null);
        assertEquals("value is null", error.getErrorMessage());
    }

    @Test(expected = NullPointerException.class)
    public void testFormattedConstructor_NullFormatStringThrowsNPE() {
        // String.format(null, ...) throws NullPointerException
        new ParseError(1, null, "arg");
    }

    @Test(expected = java.util.IllegalFormatException.class)
    public void testFormattedConstructor_MismatchedFormatThrowsException() {
        // Too few arguments for the format specifiers
        new ParseError(2, "expected %s and %d", "onlyOne");
    }

    @Test
    public void testNegativePositionIsStoredUnchanged() {
        int negativePos = -15;
        ParseError error = new ParseError(negativePos, "negative position test");
        assertEquals("Negative position should be stored as‑is", negativePos, error.getPosition());
        assertEquals("-15: negative position test", error.toString());
    }

    @Test
    public void testEmptyMessageAndZeroPosition() {
        ParseError error = new ParseError(0, "");
        assertEquals("", error.getErrorMessage());
        assertEquals(0, error.getPosition());
        assertEquals("0: ", error.toString());
    }

    @Test
    public void testLargePositionValue() {
        int largePos = Integer.MAX_VALUE;
        String msg = "overflow test";
        ParseError error = new ParseError(largePos, msg);
        assertEquals(largePos, error.getPosition());
        assertEquals(msg, error.getErrorMessage());
        assertEquals(largePos + ": " + msg, error.toString());
    }
}
