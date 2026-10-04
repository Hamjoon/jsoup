package org.jsoup.nodes;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * JUnit 4 test cases for {@link BooleanAttribute}.
 *
 * The test class covers:
 * 1. Public API (the constructor) – normal, empty and {@code null} keys.
 * 2. Behaviour inherited from {@link Attribute} (key/value handling).
 * 3. The protected {@code isBooleanAttribute()} implementation – verified via a
 *    small test‑specific subclass that exposes the method.
 *
 * The tests are written against the public contract of {@link BooleanAttribute}
 * and the documented behaviour of its superclass {@link Attribute}.
 */
public class BooleanAttributeTest {

    /** Simple subclass that makes the protected method visible for testing. */
    private static class TestableBooleanAttribute extends BooleanAttribute {
        TestableBooleanAttribute(String key) {
            super(key);
        }

        /** Expose the protected method so we can assert its result. */
        boolean callIsBooleanAttribute() {
            return super.isBooleanAttribute();
        }
    }

    /* --------------------------------------------------------------------- *
     *  1.  Public constructor tests
     * --------------------------------------------------------------------- */

    /** Normal key – the attribute must keep the key and have a {@code null} value. */
    @Test
    public void constructor_WithValidKey_ShouldSetKeyAndNullValue() {
        String key = "checked";
        BooleanAttribute attr = new BooleanAttribute(key);

        // Attribute API (inherited) – key must be exactly the one supplied.
        assertEquals("Key should be stored unchanged", key, attr.getKey());

        // Boolean attributes are written without a value; the super constructor
        // is called with {@code null}, therefore getValue() must return null.
        assertNull("Value of a BooleanAttribute must be null", attr.getValue());
    }

    /** Empty string as key – still a valid key for the underlying {@link Attribute}. */
    @Test
    public void constructor_WithEmptyKey_ShouldSetEmptyKeyAndNullValue() {
        String key = "";
        BooleanAttribute attr = new BooleanAttribute(key);

        assertEquals("Empty key must be stored", key, attr.getKey());
        assertNull("Value must still be null for empty key", attr.getValue());
    }

    /**
     * Passing {@code null} as the key should result in a {@link NullPointerException}
     * (the {@link Attribute} constructor validates the key argument).
     */
    @Test(expected = NullPointerException.class)
    public void constructor_WithNullKey_ShouldThrowNPE() {
        new BooleanAttribute(null);
    }

    /* --------------------------------------------------------------------- *
     *  2.  Protected boolean‑attribute flag
     * --------------------------------------------------------------------- */

    /**
     * The overridden {@code isBooleanAttribute()} method must always return {@code true}.
     * Because the method is {@code protected}, we use a test‑specific subclass that
     * exposes it.
     */
    @Test
    public void isBooleanAttribute_ShouldReturnTrue() {
        TestableBooleanAttribute attr = new TestableBooleanAttribute("selected");
        assertTrue("isBooleanAttribute() must return true for BooleanAttribute", attr.callIsBooleanAttribute());
    }

    /* --------------------------------------------------------------------- *
     *  3.  Edge‑case & defensive tests
     * --------------------------------------------------------------------- */

    /**
     * Verify that the attribute behaves like a normal {@link Attribute} when
     * {@code equals()} and {@code hashCode()} are used.  Two BooleanAttributes
     * with the same key must be equal and have the same hash code even though
     * the value is {@code null}.
     */
    @Test
    public void equalsAndHashCode_WithSameKey_ShouldBeEqual() {
        BooleanAttribute a1 = new BooleanAttribute("disabled");
        BooleanAttribute a2 = new BooleanAttribute("disabled");

        assertEquals("Attributes with same key should be equal", a1, a2);
        assertEquals("Equal objects must have same hashCode", a1.hashCode(), a2.hashCode());
    }

    /**
     * Two BooleanAttributes with different keys must not be equal.
     */
    @Test
    public void equals_WithDifferentKey_ShouldNotBeEqual() {
        BooleanAttribute a1 = new BooleanAttribute("readonly");
        BooleanAttribute a2 = new BooleanAttribute("multiple");

        assertNotEquals("Attributes with different keys should not be equal", a1, a2);
    }

    /**
     * The {@code toString()} representation of a BooleanAttribute should be just the key
     * (no ``=`` or value part).  This matches the documented behaviour of boolean
     * attributes in jsoup.
     */
    @Test
    public void toString_ShouldContainOnlyKey() {
        String key = "required";
        BooleanAttribute attr = new BooleanAttribute(key);

        // Expected format: key (no '=value')
        assertEquals("toString() must return the raw key for boolean attributes", key, attr.toString());
    }
}
