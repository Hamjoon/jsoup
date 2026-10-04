###Test START##
```java
package org.jsoup.helper;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Validate}.
 *
 * <p>Each public static method is exercised with:
 * <ul>
 *   <li>a normal (non‑exception) case, confirming that no exception is thrown;</li>
 *   <li>the default‑message failure case (where applicable);</li>
 *   <li>a custom‑message failure case, asserting that the supplied message is propagated;</li>
 *   <li>additional edge‑case tests (null arrays, empty collections, whitespace strings, etc.).</li>
 * </ul>
 *
 * The suite purposefully avoids using any third‑party test helpers – only JUnit‑4 core
 * annotations and assertions are employed, keeping the code easy to read and maintain.
 */
public class ValidateTest {

    /* ----------------------------------------------------------------------
     *  notNull(Object)
     * ---------------------------------------------------------------------- */

    @Test
    public void notNull_Object_PassesWhenNotNull() {
        // normal usage – should not throw
        Validate.notNull(new Object());
    }

    @Test
    public void notNull_Object_ThrowsWithDefaultMessageWhenNull() {
        try {
            Validate.notNull(null);
            fail("Expected IllegalArgumentException for null argument");
        } catch (IllegalArgumentException e) {
            assertEquals("Object must not be null", e.getMessage());
        }
    }

    /* ----------------------------------------------------------------------
     *  notNull(Object, String)
     * ---------------------------------------------------------------------- */

    @Test
    public void notNull_ObjectWithMessage_PassesWhenNotNull() {
        Validate.notNull("test", "custom message");
    }

    @Test
    public void notNull_ObjectWithMessage_ThrowsWithCustomMessageWhenNull() {
        final String customMsg = "my custom null message";
        try {
            Validate.notNull(null, customMsg);
            fail("Expected IllegalArgumentException for null argument");
        } catch (IllegalArgumentException e) {
            assertEquals(customMsg, e.getMessage());
        }
    }

    /* ----------------------------------------------------------------------
     *  isTrue(boolean)
     * ---------------------------------------------------------------------- */

    @Test
    public void isTrue_NoMessage_PassesWhenTrue() {
        Validate.isTrue(true);
    }

    @Test
    public void isTrue_NoMessage_ThrowsWithDefaultMessageWhenFalse() {
        try {
            Validate.isTrue(false);
            fail("Expected IllegalArgumentException for false value");
        } catch (IllegalArgumentException e) {
            assertEquals("Must be true", e.getMessage());
        }
    }

    /* ----------------------------------------------------------------------
     *  isTrue(boolean, String)
     * ---------------------------------------------------------------------- */

    @Test
    public void isTrue_WithMessage_PassesWhenTrue() {
        Validate.isTrue(true, "should not fail");
    }

    @Test
    public void isTrue_WithMessage_ThrowsWithCustomMessageWhenFalse() {
        final String msg = "value must be true!";
        try {
            Validate.isTrue(false, msg);
            fail("Expected IllegalArgumentException for false value");
        } catch (IllegalArgumentException e) {
            assertEquals(msg, e.getMessage());
        }
    }

    /* ----------------------------------------------------------------------
     *  isFalse(boolean)
     * ---------------------------------------------------------------------- */

    @Test
    public void isFalse_NoMessage_PassesWhenFalse() {
        Validate.isFalse(false);
    }

    @Test
    public void isFalse_NoMessage_ThrowsWithDefaultMessageWhenTrue() {
        try {
            Validate.isFalse(true);
            fail("Expected IllegalArgumentException for true value");
        } catch (IllegalArgumentException e) {
            assertEquals("Must be false", e.getMessage());
        }
    }

    /* ----------------------------------------------------------------------
     *  isFalse(boolean, String)
     * ---------------------------------------------------------------------- */

    @Test
    public void isFalse_WithMessage_PassesWhenFalse() {
        Validate.isFalse(false, "should not fail");
    }

    @Test
    public void isFalse_WithMessage_ThrowsWithCustomMessageWhenTrue() {
        final String msg = "value must be false!";
        try {
            Validate.isFalse(true, msg);
            fail("Expected IllegalArgumentException for true value");
        } catch (IllegalArgumentException e) {
            assertEquals(msg, e.getMessage());
        }
    }

    /* ----------------------------------------------------------------------
     *  noNullElements(Object[])
     * ---------------------------------------------------------------------- */

    @Test
    public void noNullElements_NoMessage_PassesWithValidArray() {
        Object[] array = { "a", 1, new Object() };
        Validate.noNullElements(array);
    }

    @Test
    public void noNullElements_NoMessage_PassesWithEmptyArray() {
        Validate.noNullElements(new Object[0]); // empty array is valid
    }

    @Test
    public void noNullElements_NoMessage_ThrowsWithDefaultMessageWhenNullElementPresent() {
        Object[] array = { "a", null, "b" };
        try {
            Validate.noNullElements(array);
            fail("Expected IllegalArgumentException because of null element");
        } catch (IllegalArgumentException e) {
            assertEquals("Array must not contain any null objects", e.getMessage());
        }
    }

    @Test(expected = NullPointerException.class)
    public void noNullElements_NoMessage_ThrowsNPEWhenArrayIsNull() {
        // The method does not guard against a null array reference.
        Validate.noNullElements(null);
    }

    /* ----------------------------------------------------------------------
     *  noNullElements(Object[], String)
     * ---------------------------------------------------------------------- */

    @Test
    public void noNullElements_WithMessage_PassesWithValidArray() {
        Object[] array = { "x", 2 };
        Validate.noNullElements(array, "custom message");
    }

    @Test
    public void noNullElements_WithMessage_ThrowsWithCustomMessageWhenNullElementPresent() {
        final String msg = "my custom null‑element message";
        Object[] array = { null };
        try {
            Validate.noNullElements(array, msg);
            fail("Expected IllegalArgumentException because of null element");
        } catch (IllegalArgumentException e) {
            assertEquals(msg, e.getMessage());
        }
    }

    /* ----------------------------------------------------------------------
     *  notEmpty(String)
     * ---------------------------------------------------------------------- */

    @Test
    public void notEmpty_NoMessage_PassesWhenStringHasContent() {
        Validate.notEmpty("hello");
    }

    @Test
    public void notEmpty_NoMessage_ThrowsWithDefaultMessageWhenEmpty() {
        try {
            Validate.notEmpty("");
            fail("Expected IllegalArgumentException for empty string");
        } catch (IllegalArgumentException e) {
            assertEquals("String must not be empty", e.getMessage());
        }
    }

    @Test
    public void notEmpty_NoMessage_ThrowsWithDefaultMessageWhenNull() {
        try {
            Validate.notEmpty(null);
            fail("Expected IllegalArgumentException for null string");
        } catch (IllegalArgumentException e) {
            assertEquals("String must not be empty", e.getMessage());
        }
    }

    @Test
    public void notEmpty_NoMessage_PassesWhenStringContainsOnlyWhitespace() {
        Validate.notEmpty("   "); // whitespace counts as non‑empty
    }

    /* ----------------------------------------------------------------------
     *  notEmpty(String, String)
     * ---------------------------------------------------------------------- */

    @Test
    public void notEmpty_WithMessage_PassesWhenStringHasContent() {
        Validate.notEmpty("world", "should not fail");
    }

    @Test
    public void notEmpty_WithMessage_ThrowsWithCustomMessageWhenEmpty() {
        final String msg = "my custom empty‑string message";
        try {
            Validate.notEmpty("", msg);
            fail("Expected IllegalArgumentException for empty string");
        } catch (IllegalArgumentException e) {
            assertEquals(msg, e.getMessage());
        }
    }

    @Test
    public void notEmpty_WithMessage_ThrowsWithCustomMessageWhenNull() {
        final String msg = "null not allowed";
        try {
            Validate.notEmpty(null, msg);
            fail("Expected IllegalArgumentException for null string");
        } catch (IllegalArgumentException e) {
            assertEquals(msg, e.getMessage());
        }
    }

    /* ----------------------------------------------------------------------
     *  fail(String)
     * ---------------------------------------------------------------------- */

    @Test
    public void fail_AlwaysThrowsWithProvidedMessage() {
        final String msg = "forced failure";
        try {
            Validate.fail(msg);
            fail("Validate.fail should always throw");
        } catch (IllegalArgumentException e) {
            assertEquals(msg, e.getMessage());
        }
    }
}
```
###Test END##