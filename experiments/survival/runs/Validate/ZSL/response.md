###Test START##
package org.jsoup.helper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Comprehensive JUnit4 test suite for {@link Validate}.
 */
public class ValidateTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* ---------- notNull ---------- */

    @Test
    public void notNull_passesWhenObjectIsNotNull() {
        Validate.notNull(new Object());
    }

    @Test
    public void notNull_throwsWithDefaultMessageWhenNull() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Object must not be null");
        Validate.notNull(null);
    }

    @Test
    public void notNullWithMessage_passesWhenObjectIsNotNull() {
        Validate.notNull(new Object(), "custom message");
    }

    @Test
    public void notNullWithMessage_throwsWithCustomMessageWhenNull() {
        String custom = "my custom null error";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(custom);
        Validate.notNull(null, custom);
    }

    /* ---------- isTrue ---------- */

    @Test
    public void isTrue_passesWhenTrue() {
        Validate.isTrue(true);
    }

    @Test
    public void isTrue_throwsWithDefaultMessageWhenFalse() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Must be true");
        Validate.isTrue(false);
    }

    @Test
    public void isTrueWithMessage_throwsWithCustomMessageWhenFalse() {
        String custom = "must be true custom";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(custom);
        Validate.isTrue(false, custom);
    }

    /* ---------- isFalse ---------- */

    @Test
    public void isFalse_passesWhenFalse() {
        Validate.isFalse(false);
    }

    @Test
    public void isFalse_throwsWithDefaultMessageWhenTrue() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Must be false");
        Validate.isFalse(true);
    }

    @Test
    public void isFalseWithMessage_throwsWithCustomMessageWhenTrue() {
        String custom = "must be false custom";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(custom);
        Validate.isFalse(true, custom);
    }

    /* ---------- noNullElements ---------- */

    @Test
    public void noNullElements_passesWhenArrayHasNoNulls() {
        Object[] array = {"a", 1, new Object()};
        Validate.noNullElements(array);
    }

    @Test
    public void noNullElements_passesWhenArrayIsEmpty() {
        Object[] empty = new Object[0];
        Validate.noNullElements(empty);
    }

    @Test
    public void noNullElements_throwsWithDefaultMessageWhenContainsNull() {
        Object[] array = {"a", null, "b"};
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Array must not contain any null objects");
        Validate.noNullElements(array);
    }

    @Test
    public void noNullElementsWithMessage_throwsWithCustomMessageWhenContainsNull() {
        String custom = "null element found!";
        Object[] array = {null};
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(custom);
        Validate.noNullElements(array, custom);
    }

    /* ---------- notEmpty (String) ---------- */

    @Test
    public void notEmpty_passesWhenStringIsNonEmpty() {
        Validate.notEmpty("hello");
    }

    @Test
    public void notEmpty_throwsWithDefaultMessageWhenNull() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("String must not be empty");
        Validate.notEmpty((String) null);
    }

    @Test
    public void notEmpty_throwsWithDefaultMessageWhenEmpty() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("String must not be empty");
        Validate.notEmpty("");
    }

    @Test
    public void notEmptyWithMessage_throwsWithCustomMessageWhenNull() {
        String custom = "string cannot be null or empty";
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

    /* ---------- fail ---------- */

    @Test
    public void fail_alwaysThrowsIllegalArgumentExceptionWithGivenMessage() {
        String msg = "forced failure";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(msg);
        Validate.fail(msg);
    }
}
###Test END##