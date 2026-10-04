package org.jsoup.helper;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Comprehensive JUnit‑4 test suite for {@link Validate}.
 * <p>
 * The suite is written as if three independent experts each contributed one
 * test per public method.  The tests cover:
 * <ul>
 *   <li>Typical (valid) usage – the method should return silently.</li>
 *   <li>Edge cases – e.g. empty strings, empty arrays, boolean extremes.</li>
 *   <li>Error scenarios – the method must throw {@link IllegalArgumentException}
 *        with the correct default or custom message.</li>
 * </ul>
 * </p>
 */
public class ValidateTest {

    /** Helper rule to assert exception type **and** message. */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* --------------------------------------------------------------------- */
    /*  notNull(Object)                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void notNull_WithNonNullObject_ShouldPass() {
        // typical usage – no exception expected
        Validate.notNull(new Object());
    }

    @Test
    public void notNull_WithNullObject_ShouldThrowDefaultMessage() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Object must not be null");
        Validate.notNull(null);
    }

    /* --------------------------------------------------------------------- */
    /*  notNull(Object, String)                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void notNull_WithCustomMessageAndNull_ShouldThrowCustomMessage() {
        final String custom = "My custom null error";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(custom);
        Validate.notNull(null, custom);
    }

    @Test
    public void notNull_WithCustomMessageAndNonNull_ShouldPass() {
        Validate.notNull("hello", "won't be used");
    }

    /* --------------------------------------------------------------------- */
    /*  isTrue(boolean)                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void isTrue_WithTrue_ShouldPass() {
        Validate.isTrue(true);
    }

    @Test
    public void isTrue_WithFalse_ShouldThrowDefaultMessage() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Must be true");
        Validate.isTrue(false);
    }

    /* --------------------------------------------------------------------- */
    /*  isTrue(boolean, String)                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void isTrue_WithCustomMessageAndFalse_ShouldThrowCustomMessage() {
        final String msg = "must be true indeed";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(msg);
        Validate.isTrue(false, msg);
    }

    @Test
    public void isTrue_WithCustomMessageAndTrue_ShouldPass() {
        Validate.isTrue(true, "ignored");
    }

    /* --------------------------------------------------------------------- */
    /*  isFalse(boolean)                                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void isFalse_WithFalse_ShouldPass() {
        Validate.isFalse(false);
    }

    @Test
    public void isFalse_WithTrue_ShouldThrowDefaultMessage() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Must be false");
        Validate.isFalse(true);
    }

    /* --------------------------------------------------------------------- */
    /*  isFalse(boolean, String)                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void isFalse_WithCustomMessageAndTrue_ShouldThrowCustomMessage() {
        final String msg = "must be false indeed";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(msg);
        Validate.isFalse(true, msg);
    }

    @Test
    public void isFalse_WithCustomMessageAndFalse_ShouldPass() {
        Validate.isFalse(false, "ignored");
    }

    /* --------------------------------------------------------------------- */
    /*  noNullElements(Object[])                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void noNullElements_WithAllNonNull_ShouldPass() {
        Object[] array = { "a", Integer.valueOf(1), new Object() };
        Validate.noNullElements(array);
    }

    @Test
    public void noNullElements_WithEmptyArray_ShouldPass() {
        Validate.noNullElements(new Object[0]);
    }

    @Test
    public void noNullElements_WithNullElement_ShouldThrowDefaultMessage() {
        Object[] array = { "a", null, "b" };
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Array must not contain any null objects");
        Validate.noNullElements(array);
    }

    /* --------------------------------------------------------------------- */
    /*  noNullElements(Object[], String)                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void noNullElements_WithCustomMessageAndNullElement_ShouldThrowCustomMessage() {
        final String msg = "custom null element error";
        Object[] array = { null };
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(msg);
        Validate.noNullElements(array, msg);
    }

    @Test
    public void noNullElements_WithCustomMessageAndAllNonNull_ShouldPass() {
        Object[] array = { "x", "y" };
        Validate.noNullElements(array, "won't be used");
    }

    /* --------------------------------------------------------------------- */
    /*  notEmpty(String)                                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void notEmpty_WithNonEmptyString_ShouldPass() {
        Validate.notEmpty("hello");
    }

    @Test
    public void notEmpty_WithEmptyString_ShouldThrowDefaultMessage() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("String must not be empty");
        Validate.notEmpty("");
    }

    @Test
    public void notEmpty_WithNullString_ShouldThrowDefaultMessage() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("String must not be empty");
        Validate.notEmpty((String) null);
    }

    /* --------------------------------------------------------------------- */
    /*  notEmpty(String, String)                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void notEmpty_WithCustomMessageAndEmptyString_ShouldThrowCustomMessage() {
        final String msg = "my custom empty string error";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(msg);
        Validate.notEmpty("", msg);
    }

    @Test
    public void notEmpty_WithCustomMessageAndNullString_ShouldThrowCustomMessage() {
        final String msg = "null string not allowed";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(msg);
        Validate.notEmpty((String) null, msg);
    }

    @Test
    public void notEmpty_WithCustomMessageAndValidString_ShouldPass() {
        Validate.notEmpty("valid", "ignored");
    }

    /* --------------------------------------------------------------------- */
    /*  fail(String)                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void fail_ShouldAlwaysThrowWithGivenMessage() {
        final String msg = "forced failure";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(msg);
        Validate.fail(msg);
    }
}
