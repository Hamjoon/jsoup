package org.jsoup.helper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * JUnit 4 test suite for {@link Validate}.
 *
 * The test cases are divided into two groups:
 * 1. **Basic functionality** – verifies that the validation methods succeed
 *    when the input satisfies the contract.
 * 2. **Edge‑case / exception handling** – verifies that the methods throw
 *    {@link IllegalArgumentException} (or the expected exception) with the
 *    correct message when the contract is violated.
 *
 * All public static methods of {@link Validate} are covered.
 */
public class ValidateTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* --------------------------------------------------------------------- *
     *  1.  Public method signatures (extracted from Validate)
     * --------------------------------------------------------------------- *
     *
     * public static void notNull(Object obj)
     * public static void notNull(Object obj, String msg)
     *
     * public static void isTrue(boolean val)
     * public static void isTrue(boolean val, String msg)
     *
     * public static void isFalse(boolean val)
     * public static void isFalse(boolean val, String msg)
     *
     * public static void noNullElements(Object[] objects)
     * public static void noNullElements(Object[] objects, String msg)
     *
     * public static void notEmpty(String string)
     * public static void notEmpty(String string, String msg)
     *
     * public static void fail(String msg)
     */

    /* --------------------------------------------------------------------- *
     *  2.  Basic positive test cases (no exception expected)
     * --------------------------------------------------------------------- */

    @Test
    public void testNotNull_Pass() {
        Validate.notNull(new Object());
    }

    @Test
    public void testNotNull_WithMessage_Pass() {
        Validate.notNull("non‑null", "should not fail");
    }

    @Test
    public void testIsTrue_Pass() {
        Validate.isTrue(true);
    }

    @Test
    public void testIsTrue_WithMessage_Pass() {
        Validate.isTrue(true, "must be true");
    }

    @Test
    public void testIsFalse_Pass() {
        Validate.isFalse(false);
    }

    @Test
    public void testIsFalse_WithMessage_Pass() {
        Validate.isFalse(false, "must be false");
    }

    @Test
    public void testNoNullElements_Pass() {
        Object[] array = { "a", 1, new Object() };
        Validate.noNullElements(array);
    }

    @Test
    public void testNoNullElements_WithMessage_Pass() {
        Object[] array = { "x", "y" };
        Validate.noNullElements(array, "custom message – should not be used");
    }

    @Test
    public void testNoNullElements_EmptyArray_Pass() {
        Object[] empty = new Object[0];
        Validate.noNullElements(empty);
    }

    @Test
    public void testNotEmpty_Pass() {
        Validate.notEmpty("hello");
    }

    @Test
    public void testNotEmpty_WithMessage_Pass() {
        Validate.notEmpty("world", "custom ok message");
    }

    @Test
    public void testFail_AlwaysThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("forced failure");
        Validate.fail("forced failure");
    }

    /* --------------------------------------------------------------------- *
     *  3.  Edge‑case / exception handling test cases
     * --------------------------------------------------------------------- */

    // ----- notNull -----
    @Test
    public void testNotNull_Fail_DefaultMessage() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Object must not be null");
        Validate.notNull(null);
    }

    @Test
    public void testNotNull_Fail_CustomMessage() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("my custom null message");
        Validate.notNull(null, "my custom null message");
    }

    // ----- isTrue -----
    @Test
    public void testIsTrue_Fail_DefaultMessage() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Must be true");
        Validate.isTrue(false);
    }

    @Test
    public void testIsTrue_Fail_CustomMessage() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("value must be true");
        Validate.isTrue(false, "value must be true");
    }

    // ----- isFalse -----
    @Test
    public void testIsFalse_Fail_DefaultMessage() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Must be false");
        Validate.isFalse(true);
    }

    @Test
    public void testIsFalse_Fail_CustomMessage() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("value must be false");
        Validate.isFalse(true, "value must be false");
    }

    // ----- noNullElements -----
    @Test
    public void testNoNullElements_Fail_DefaultMessage() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Array must not contain any null objects");
        Object[] array = { "ok", null, "still ok" };
        Validate.noNullElements(array);
    }

    @Test
    public void testNoNullElements_Fail_CustomMessage() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("custom array null message");
        Object[] array = { null };
        Validate.noNullElements(array, "custom array null message");
    }

    @Test
    public void testNoNullElements_NullArray_NullPointerException() {
        thrown.expect(NullPointerException.class);
        Validate.noNullElements(null);
    }

    // ----- notEmpty -----
    @Test
    public void testNotEmpty_Fail_NullString_DefaultMessage() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("String must not be empty");
        Validate.notEmpty((String) null);
    }

    @Test
    public void testNotEmpty_Fail_EmptyString_DefaultMessage() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("String must not be empty");
        Validate.notEmpty("");
    }

    @Test
    public void testNotEmpty_Fail_NullString_CustomMessage() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("custom string null");
        Validate.notEmpty((String) null, "custom string null");
    }

    @Test
    public void testNotEmpty_Fail_EmptyString_CustomMessage() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("custom empty");
        Validate.notEmpty("", "custom empty");
    }

    // ----- fail -----
    @Test
    public void testFail_ThrowsWithProvidedMessage() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("something went wrong");
        Validate.fail("something went wrong");
    }
}
