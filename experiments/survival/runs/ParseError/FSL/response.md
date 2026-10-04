###Test START##
package org.jsoup.parser;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Comprehensive JUnit 4 test suite for {@link ParseError}.
 */
public class ParseErrorTest {

    // -----------------------------------------------------------------
    // Simple constructor with a plain message
    // -----------------------------------------------------------------
    @Test
    public void testPlainMessageConstructor() {
        int position = 5;
        String msg = "Simple error";
        ParseError err = new ParseError(position, msg);

        assertEquals("Position should be stored correctly", position, err.getPosition());
        assertEquals("Message should be stored unchanged", msg, err.getErrorMessage());
        assertEquals("toString format must be \"pos: message\"", position + ": " + msg, err.toString());
    }

    // -----------------------------------------------------------------
    // Constructor that uses String.format()
    // -----------------------------------------------------------------
    @Test
    public void testFormattedMessageConstructor() {
        int position = 12;
        String format = "Expected %s but got %d";
        String expected = "Expected foo but got 42";

        ParseError err = new ParseError(position, format, "foo", 42);

        assertEquals(position, err.getPosition());
        assertEquals(expected, err.getErrorMessage());
        assertEquals(position + ": " + expected, err.toString());
    }

    @Test
    public void testFormattedMessageNoPlaceholders() {
        int position = 0;
        String format = "Static message without placeholders";

        ParseError err = new ParseError(position, format);

        assertEquals(position, err.getPosition());
        assertEquals(format, err.getErrorMessage());
        assertEquals(position + ": " + format, err.toString());
    }

    @Test
    public void testFormattedMessageWithEscapedPercent() {
        int position = 7;
        String format = "Progress: 100%% complete";

        ParseError err = new ParseError(position, format);

        assertEquals("Progress: 100% complete", err.getErrorMessage());
        assertEquals(position + ": Progress: 100% complete", err.toString());
    }

    // -----------------------------------------------------------------
    // Edge cases for null values and unusual characters
    // -----------------------------------------------------------------
    @Test
    public void testNullMessage() {
        int position = 3;
        ParseError err = new ParseError(position, (String) null);

        assertEquals(position, err.getPosition());
        assertNull("Message should be null when null is passed", err.getErrorMessage());
        // String concatenation with null yields the literal "null"
        assertEquals(position + ": null", err.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testNullFormatThrowsNPE() {
        // String.format throws NullPointerException when the format string is null
        new ParseError(1, null, "arg");
    }

    @Test
    public void testMessageWithSpecialCharacters() {
        int position = 9;
        String specialMsg = "Line1\nLine2\t\u2603"; // newline, tab, Unicode snowman
        ParseError err = new ParseError(position, specialMsg);

        assertEquals(specialMsg, err.getErrorMessage());
        assertEquals(position + ": " + specialMsg, err.toString());
    }

    // -----------------------------------------------------------------
    // Negative and zero positions
    // -----------------------------------------------------------------
    @Test
    public void testZeroPosition() {
        ParseError err = new ParseError(0, "Zero position");
        assertEquals(0, err.getPosition());
        assertEquals("Zero position", err.getErrorMessage());
        assertEquals("0: Zero position", err.toString());
    }

    @Test
    public void testNegativePosition() {
        int negativePos = -15;
        ParseError err = new ParseError(negativePos, "Negative position");
        assertEquals(negativePos, err.getPosition());
        assertEquals("Negative position", err.getErrorMessage());
        assertEquals(negativePos + ": Negative position", err.toString());
    }

    // -----------------------------------------------------------------
    // Verify that multiple arguments of different types are formatted correctly
    // -----------------------------------------------------------------
    @Test
    public void testFormattedMessageWithVariousArgumentTypes() {
        int position = 4;
        String format = "Bool:%b Int:%d Double:%f Char:%c";
        String expected = "Bool:true Int:7 Double:3.140000 Char:A";

        ParseError err = new ParseError(position, format, true, 7, 3.14, 'A');

        assertEquals(expected, err.getErrorMessage());
        assertEquals(position + ": " + expected, err.toString());
    }
}
###Test END##