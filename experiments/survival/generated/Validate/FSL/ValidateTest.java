package org.jsoup.helper;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.Rule;

/**
 * Comprehensive JUnit4 tests for {@link Validate}.
 */
public class ValidateTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* ----------------------------------------------------------------------
     * notNull(Object)
     * ---------------------------------------------------------------------- */

    @Test
    public void notNull_passesWhenObjectIsNotNull() {
        // No exception expected
        Validate.notNull(new Object());
    }

    @Test
    public void notNull_throwsWhenObjectIsNull() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Object must not be null");
        Validate.notNull(null);
    }

    /* ----------------------------------------------------------------------
     * notNull(Object, String)
     * ---------------------------------------------------------------------- */

    @Test
    public void notNullWithMessage_passesWhenObjectIsNotNull() {
        Validate.notNull("test", "custom message");
    }

    @Test
    public void notNullWithMessage_throwsWithCustomMessage() {
        String custom = "my custom null message";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(custom);
        Validate.notNull(null, custom);
    }

    /* ----------------------------------------------------------------------
     * isTrue(boolean)
     * ---------------------------------------------------------------------- */

    @Test
    public void isTrue_passesWhenTrue() {
        Validate.isTrue(true);
    }

    @Test
    public void isTrue_throwsWhenFalse() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Must be true");
        Validate.isTrue(false);
    }

    /* ----------------------------------------------------------------------
     * isTrue(boolean, String)
     * ---------------------------------------------------------------------- */

    @Test
    public void isTrueWithMessage_passesWhenTrue() {
        Validate.isTrue(true, "won't be used");
    }

    @Test
    public void isTrueWithMessage_throwsWithCustomMessage() {
        String msg = "must be true custom";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(msg);
        Validate.isTrue(false, msg);
    }

    /* ----------------------------------------------------------------------
     * isFalse(boolean)
     * ---------------------------------------------------------------------- */

    @Test
    public void isFalse_passesWhenFalse() {
        Validate.isFalse(false);
    }

    @Test
    public void isFalse_throwsWhenTrue() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Must be false");
        Validate.isFalse(true);
    }

    /* ----------------------------------------------------------------------
     * isFalse(boolean, String)
     * ---------------------------------------------------------------------- */

    @Test
    public void isFalseWithMessage_passesWhenFalse() {
        Validate.isFalse(false, "unused");
    }

    @Test
    public void isFalseWithMessage_throwsWithCustomMessage() {
        String msg = "must be false custom";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(msg);
        Validate.isFalse(true, msg);
    }

    /* ----------------------------------------------------------------------
     * noNullElements(Object[])
     * ---------------------------------------------------------------------- */

    @Test
    public void noNullElements_passesWithEmptyArray() {
        Validate.noNullElements(new Object[0]);
    }

    @Test
    public void noNullElements_passesWithAllNonNull() {
        Object[] arr = { "a", Integer.valueOf(1), new Object() };
        Validate.noNullElements(arr);
    }

    @Test
    public void noNullElements_throwsWhenArrayContainsNull() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Array must not contain any null objects");
        Object[] arr = { "a", null, "b" };
        Validate.noNullElements(arr);
    }

    /* ----------------------------------------------------------------------
     * noNullElements(Object[], String)
     * ---------------------------------------------------------------------- */

    @Test
    public void noNullElementsWithMessage_passesWhenAllNonNull() {
        Object[] arr = { "x", "y" };
        Validate.noNullElements(arr, "custom msg");
    }

    @Test
    public void noNullElementsWithMessage_throwsWithCustomMessage() {
        String custom = "my array null message";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(custom);
        Object[] arr = { null };
        Validate.noNullElements(arr, custom);
    }

    /* ----------------------------------------------------------------------
     * notEmpty(String)
     * ---------------------------------------------------------------------- */

    @Test
    public void notEmpty_passesWhenStringHasContent() {
        Validate.notEmpty("hello");
    }

    @Test
    public void notEmpty_throwsWhenStringIsNull() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("String must not be empty");
        Validate.notEmpty((String) null);
    }

    @Test
    public void notEmpty_throwsWhenStringIsEmpty() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("String must not be empty");
        Validate.notEmpty("");
    }

    /* ----------------------------------------------------------------------
     * notEmpty(String, String)
     * ---------------------------------------------------------------------- */

    @Test
    public void notEmptyWithMessage_passesWhenStringHasContent() {
        Validate.notEmpty("world", "unused");
    }

    @Test
    public void notEmptyWithMessage_throwsWithCustomMessageWhenNull() {
        String custom = "custom empty message";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(custom);
        Validate.notEmpty((String) null, custom);
    }

    @Test
    public void notEmptyWithMessage_throwsWithCustomMessageWhenEmpty() {
        String custom = "empty string not allowed";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(custom);
        Validate.notEmpty("", custom);
    }

    /* ----------------------------------------------------------------------
     * fail(String)
     * ---------------------------------------------------------------------- */

    @Test
    public void fail_alwaysThrowsWithProvidedMessage() {
        String msg = "forced failure";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(msg);
        Validate.fail(msg);
    }
}
