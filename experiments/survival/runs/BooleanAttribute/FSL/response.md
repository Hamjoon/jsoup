###Test START##
package org.jsoup.nodes;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for {@link BooleanAttribute}.
 * These tests verify the constructor behavior, the overridden
 * {@code isBooleanAttribute()} method, and the inherited contract
 * from {@link Attribute}.
 *
 * The test class resides in the same package as {@code BooleanAttribute}
 * so that the protected {@code isBooleanAttribute()} method can be accessed.
 */
public class BooleanAttributeTest {

    /**
     * Verify that the constructor stores the supplied key and forces the value to {@code null}.
     */
    @Test
    public void testConstructorInitialisesKeyAndNullValue() {
        String key = "checked";
        BooleanAttribute attr = new BooleanAttribute(key);

        // key must be stored unchanged
        assertEquals("Key should be stored as given", key, attr.getKey());

        // value must be null for a boolean attribute
        assertNull("Value of a BooleanAttribute must be null", attr.getValue());

        // hasValue() (inherited from Attribute) should report false when value is null
        assertFalse("hasValue() should be false for a BooleanAttribute", attr.hasValue());
    }

    /**
     * The {@code isBooleanAttribute()} method is protected, but because this test
     * class is in the same package we can call it directly.  It must always return {@code true}.
     */
    @Test
    public void testIsBooleanAttributeAlwaysTrue() {
        BooleanAttribute attr = new BooleanAttribute("disabled");
        assertTrue("isBooleanAttribute() must return true", attr.isBooleanAttribute());

        // also test with an empty key – the result must still be true
        BooleanAttribute emptyKeyAttr = new BooleanAttribute("");
        assertTrue("isBooleanAttribute() must return true even with an empty key", emptyKeyAttr.isBooleanAttribute());
    }

    /**
     * Boolean attributes are a special kind of {@link Attribute}.  Two BooleanAttributes
     * with the same key should be equal, and their hash codes must match.
     */
    @Test
    public void testEqualsAndHashCode() {
        BooleanAttribute a1 = new BooleanAttribute("required");
        BooleanAttribute a2 = new BooleanAttribute("required");
        BooleanAttribute a3 = new BooleanAttribute("readonly");

        // equality based on key (value is always null)
        assertEquals("Attributes with the same key should be equal", a1, a2);
        assertEquals("Hash codes must be equal for equal objects", a1.hashCode(), a2.hashCode());

        // different keys → not equal
        assertNotEquals("Attributes with different keys must not be equal", a1, a3);
    }

    /**
     * The {@code toString()} implementation of {@link Attribute} prints the attribute
     * in a form suitable for HTML.  For a boolean attribute the value part must be omitted.
     */
    @Test
    public void testToStringDoesNotContainEqualsSign() {
        BooleanAttribute attr = new BooleanAttribute("selected");
        String representation = attr.toString();

        // The string must contain the key
        assertTrue("toString() must contain the attribute key", representation.contains("selected"));

        // There must be no '=' sign because the value is null
        assertFalse("toString() for a BooleanAttribute must not contain '='", representation.contains("="));
    }

    /**
     * Passing a {@code null} key to the constructor should result in a {@link NullPointerException}
     * (the superclass {@link Attribute} validates the key).  This test confirms that the
     * exception is thrown as expected.
     */
    @Test(expected = NullPointerException.class)
    public void testConstructorRejectsNullKey() {
        new BooleanAttribute(null);
    }
}
###Test END##