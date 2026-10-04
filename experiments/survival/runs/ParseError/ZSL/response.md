###Test START##
```java
package org.jsoup.parser;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link ParseError}.
 *
 * These tests cover:
 * <ul>
 *   <li>Both constructors (plain and formatted)</li>
 *   <li>Getter methods {@code getErrorMessage()} and {@code getPosition()}</li>
 *   <li>The {@code toString()} implementation</li>
 *   <li>Edge‑cases such as {@code null} messages, negative and extreme positions</li>
 * </ul>
 *
 * The test class resides in the same package as {@code ParseError}
 * so it can access the package‑private constructors directly.
 */
public class ParseErrorTest {

    /** Simple constructor should store values unchanged. */
    @Test
    public void simpleConstructor_storesValues() {
        int pos = 42;
        String msg = "Unexpected token";

        ParseError error = new ParseError(pos, msg);

        assertEquals("Position should be stored correctly", pos, error.getPosition());
        assertEquals("Message should be stored unchanged", msg, error.getErrorMessage());
        assertEquals("toString should combine position and message", pos + ": " + msg, error.toString());
    }

    /** Formatted constructor should apply {@link String#format(String, Object...)}. */
    @Test
    public void formattedConstructor_appliesFormatting() {
        int pos = 7;
        String format = "Tag <%s> is not allowed inside <%s>";
        String expectedMsg = "Tag <script> is not allowed inside <div>";

        ParseError error = new ParseError(pos, format, "script", "div");

        assertEquals("Formatted message should be generated", expectedMsg, error.getErrorMessage());
        assertEquals("Position should be stored correctly", pos, error.getPosition());
        assertEquals("toString should reflect formatted message", pos + ": " + expectedMsg, error.toString());
    }

    /** Passing {@code null} as the message should be handled gracefully. */
    @Test
    public void constructor_acceptsNullMessage() {
        int pos = 0;
        ParseError error = new ParseError(pos, (String) null);

        assertNull("Message should be null when passed as null", error.getErrorMessage());
        assertEquals("Position should still be stored", pos, error.getPosition());
        assertEquals("toString should display \"null\" as the message", pos + ": null", error.toString());
    }

    /** Formatted constructor with {@code null} arguments should behave like {@code String.format}. */
    @Test
    public void formattedConstructor_withNullArguments() {
        int pos = 15;
        String format = "Value is %s";
        String expected = "Value is null";

        ParseError error = new ParseError(pos, format, (Object) null);

        assertEquals("Message should contain the literal \"null\" for null arguments", expected, error.getErrorMessage());
        assertEquals("toString should reflect the formatted null", pos + ": " + expected, error.toString());
    }

    /** Negative positions are allowed – the class does not validate the range. */
    @Test
    public void negativePosition_isStoredAsIs() {
        int negPos = -123;
        String msg = "Negative offset";

        ParseError error = new ParseError(negPos, msg);

        assertEquals("Negative position should be stored unchanged", negPos, error.getPosition());
        assertEquals("Message should be stored unchanged", msg, error.getErrorMessage());
        assertEquals("toString should correctly display negative position", negPos + ": " + msg, error.toString());
    }

    /** Very large integer positions should be handled without overflow. */
    @Test
    public void largePosition_isHandled() {
        int largePos = Integer.MAX_VALUE;
        String msg = "Maximum int position";

        ParseError error = new ParseError(largePos, msg);

        assertEquals("Maximum int position should be stored", largePos, error.getPosition());
        assertEquals("Message should be stored unchanged", msg, error.getErrorMessage());
        assertEquals("toString should concatenate correctly", largePos + ": " + msg, error.toString());
    }

    /** Ensure that multiple format specifiers are processed correctly. */
    @Test
    public void formattedConstructor_multipleSpecifiers() {
        int pos = 3;
        String format = "Error %d: expected \"%s\" but found \"%s\"";
        String expected = "Error 404: expected \"<div>\" but found \"<span>\"";

        ParseError error = new ParseError(pos, format, 404, "<div>", "<span>");

        assertEquals("Formatted message with multiple arguments should match", expected, error.getErrorMessage());
        assertEquals("toString must reflect the full formatted message", pos + ": " + expected, error.toString());
    }

    /** Verify that the class is immutable from the perspective of its public API. */
    @Test
    public void gettersReturnIndependentValues() {
        int pos = 10;
        String msg = "Immutable test";

        ParseError error = new ParseError(pos, msg);

        // The returned String is the original reference; however, Strings are immutable.
        // This test ensures that subsequent changes to the original variable do not affect the object.
        msg = "Changed after construction";

        assertEquals("Original message stored in object should remain unchanged", "Immutable test", error.getErrorMessage());
        assertEquals("Position should remain unchanged", 10, error.getPosition());
    }
}
```
###Test END##