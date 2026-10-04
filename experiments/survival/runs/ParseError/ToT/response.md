###Test START##
```java
package org.jsoup.parser;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ParseError}.
 *
 * The suite is written from the perspective of three “experts” collaborating:
 * <ul>
 *   <li>Expert A focuses on the simple getters.</li>
 *   <li>Expert B concentrates on the formatting constructor and edge‑cases.</li>
 *   <li>Expert C validates the {@code toString()} contract and error handling.</li>
 * </ul>
 * Each expert contributed one test per public method; the final file aggregates all
 * contributions into a single, runnable test class.
 */
public class ParseErrorTest {

    // -----------------------------------------------------------------------
    // Shared fixtures
    // -----------------------------------------------------------------------
    private static final int SAMPLE_POS = 42;
    private static final String SAMPLE_MSG = "Unexpected token";

    private ParseError simpleError;          // created with (int, String)
    private ParseError formattedError;       // created with (int, String, Object...)

    @Before
    public void setUp() {
        // Expert A's simple constructor test data
        simpleError = new ParseError(SAMPLE_POS, SAMPLE_MSG);

        // Expert B's formatted constructor test data
        formattedError = new ParseError(
                SAMPLE_POS,
                "Error at line %d, column %d: %s",
                7, 15, "missing '>'"
        );
    }

    // -----------------------------------------------------------------------
    // Expert A – tests for getters (simple constructor)
    // -----------------------------------------------------------------------

    /** 
     * Verify that {@link ParseError#getErrorMessage()} returns the exact message
     * supplied to the simple (int, String) constructor.
     */
    @Test
    public void testGetErrorMessage_SimpleConstructor() {
        assertEquals("The error message should match the constructor argument",
                SAMPLE_MSG, simpleError.getErrorMessage());
    }

    /**
     * Verify that {@link ParseError#getPosition()} returns the position supplied
     * to the simple (int, String) constructor.
     */
    @Test
    public void testGetPosition_SimpleConstructor() {
        assertEquals("The position should be stored unchanged",
                SAMPLE_POS, simpleError.getPosition());
    }

    // -----------------------------------------------------------------------
    // Expert B – tests for formatted constructor and edge cases
    // -----------------------------------------------------------------------

    /**
     * Ensure the formatting constructor correctly builds the message using
     * {@link String#format(String, Object...)} semantics.
     */
    @Test
    public void testFormattedConstructor_MessageFormatting() {
        String expected = "Error at line 7, column 15: missing '>'";
        assertEquals("Formatted message should be built from the format string and args",
                expected, formattedError.getErrorMessage());
    }

    /**
     * The position supplied to the formatted constructor must be stored unchanged.
     */
    @Test
    public void testFormattedConstructor_PositionPreserved() {
        assertEquals("Position should be the same as supplied to the formatted ctor",
                SAMPLE_POS, formattedError.getPosition());
    }

    /**
     * Verify that a {@code null} message is accepted (the class does not guard
     * against it). This is an edge case because many APIs reject null strings.
     */
    @Test
    public void testConstructor_AllowsNullMessage() {
        ParseError nullMsgError = new ParseError(0, (String) null);
        assertNull("getErrorMessage should return null when constructed with null",
                nullMsgError.getErrorMessage());
        assertEquals("Position should still be stored correctly", 0, nullMsgError.getPosition());
    }

    /**
     * The formatted constructor should propagate {@link java.util.IllegalFormatException}
     * when the format string and arguments are incompatible.
     */
    @Test(expected = java.util.IllegalFormatException.class)
    public void testFormattedConstructor_IllegalFormatThrows() {
        // Passing a non‑numeric argument for %d will cause IllegalFormatConversionException
        new ParseError(1, "Number: %d", "not-a-number");
    }

    // -----------------------------------------------------------------------
    // Expert C – tests for toString() and overall contract
    // -----------------------------------------------------------------------

    /**
     * {@code toString()} must concatenate the position, a colon, a space, and the
     * error message exactly as documented.
     */
    @Test
    public void testToString_SimpleError() {
        String expected = SAMPLE_POS + ": " + SAMPLE_MSG;
        assertEquals("toString() should follow the 'pos: message' format",
                expected, simpleError.toString());
    }

    /**
     * Verify {@code toString()} with a formatted message.
     */
    @Test
    public void testToString_FormattedError() {
        String expectedMessage = "Error at line 7, column 15: missing '>'";
        String expected = SAMPLE_POS + ": " + expectedMessage;
        assertEquals("toString() should include the formatted message",
                expected, formattedError.toString());
    }

    /**
     * Edge case: when the error message is {@code null}, {@code toString()}
     * should still return a string containing the position and the literal
     * "null" (as per Java's string concatenation rules).
     */
    @Test
    public void testToString_NullMessage() {
        ParseError nullMsgError = new ParseError(99, (String) null);
        String expected = "99: null";
        assertEquals("toString() should handle null messages gracefully",
                expected, nullMsgError.toString());
    }

    /**
     * Ensure that the class behaves correctly with negative positions.
     * The API does not forbid negative offsets, so we test that they are stored
     * and displayed unchanged.
     */
    @Test
    public void testNegativePositionHandling() {
        ParseError negativePosError = new ParseError(-5, "Negative position test");
        assertEquals(-5, negativePosError.getPosition());
        assertEquals("-5: Negative position test", negativePosError.toString());
    }
}
```
###Test END##