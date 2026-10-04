package org.jsoup.parser;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.IllegalFormatException;

/**
 * JUnit‑4 test suite for {@link ParseError}.
 *
 * Covers normal usage, edge cases and exception propagation.
 */
public class ParseErrorTest {

    /*=== Basic functionality =====================================================*/

    @Test
    public void testGetErrorMessage_simple() {
        ParseError err = new ParseError(10, "simple message");
        assertEquals("simple message", err.getErrorMessage());
    }

    @Test
    public void testGetErrorMessage_fromFormat() {
        ParseError err = new ParseError(5, "value %d is %s", 7, "odd");
        assertEquals("value 7 is odd", err.getErrorMessage());
    }

    @Test
    public void testGetPosition() {
        ParseError err = new ParseError(42, "any");
        assertEquals(42, err.getPosition());
    }

    @Test
    public void testToString_normal() {
        ParseError err = new ParseError(7, "boom");
        assertEquals("7: boom", err.toString());
    }

    /*=== Edge‑case & exception tests ===========================================*/

    @Test
    public void testErrorMessage_null() {
        ParseError err = new ParseError(3, (String) null);
        assertNull(err.getErrorMessage());
        assertEquals("3: null", err.toString());
    }

    @Test
    public void testConstructor_nullFormatString_throwsNPE() {
        try {
            new ParseError(0, null, new Object[] {});
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testConstructor_invalidFormat_throwsIllegalFormatException() {
        try {
            new ParseError(1, "%d", "not a number");
            fail("Expected IllegalFormatException");
        } catch (IllegalFormatException expected) {
            // expected
        }
    }

    @Test
    public void testNegativePosition() {
        ParseError err = new ParseError(-15, "negative pos");
        assertEquals(-15, err.getPosition());
        assertEquals("-15: negative pos", err.toString());
    }

    @Test
    public void testMaxIntPosition() {
        ParseError err = new ParseError(Integer.MAX_VALUE, "max int");
        assertEquals(Integer.MAX_VALUE, err.getPosition());
        assertEquals(Integer.MAX_VALUE + ": max int", err.toString());
    }

    @Test
    public void testMessageWithSpecialChars() {
        String special = "line1\nline2\r\t\"quotes\" and \\backslashes\\";
        ParseError err = new ParseError(9, special);
        assertEquals(special, err.getErrorMessage());
        assertEquals("9: " + special, err.toString());
    }

    @Test
    public void testEmptyMessage() {
        ParseError err = new ParseError(0, "");
        assertEquals("", err.getErrorMessage());
        assertEquals("0: ", err.toString());
    }

    @Test
    public void testFormatWithoutPlaceholders_andNoArgs() {
        ParseError err = new ParseError(2, "fixed message");
        assertEquals("fixed message", err.getErrorMessage());
        assertEquals("2: fixed message", err.toString());
    }

    @Test
    public void testFormatWithPlaceholdersAndEmptyArgsArray() {
        try {
            new ParseError(4, "needs %s", new Object[0]);
            fail("Expected IllegalFormatException due to missing argument");
        } catch (IllegalFormatException expected) {
            // expected
        }
    }
}
