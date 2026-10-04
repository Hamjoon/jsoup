###Test START##
```java
package org.jsoup.nodes;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * JUnit4 test suite for {@link BooleanAttribute}.
 *
 * <p>These tests cover:
 * <ul>
 *   <li>Typical usage – creating a BooleanAttribute with a normal key.</li>
 *   <li>Edge cases – empty string as a key.</li>
 *   <li>Error scenarios – passing {@code null} as a key (expected to throw NPE).</li>
 *   <li>Behaviour inherited from {@link Attribute} – ensuring the value is {@code null}
 *       and that {@code isBooleanAttribute()} returns {@code true}.</li>
 * </ul>
 *
 * <p>The tests are written against JUnit 4 and only rely on the public API of
 * {@link BooleanAttribute} and its superclass {@link Attribute}.
 */
public class BooleanAttributeTest {

    private static final String SAMPLE_KEY = "checked";
    private static final String EMPTY_KEY = "";

    private BooleanAttribute attr;
    private BooleanAttribute emptyKeyAttr;

    @Before
    public void setUp() {
        attr = new BooleanAttribute(SAMPLE_KEY);
        emptyKeyAttr = new BooleanAttribute(EMPTY_KEY);
    }

    /**
     * Verify that the constructor correctly stores the key and forces the value to {@code null}.
     */
    @Test
    public void testConstructorSetsKeyAndNullValue() {
        // typical key
        assertEquals("Key should be stored exactly as supplied", SAMPLE_KEY, attr.getKey());
        assertNull("Value of a BooleanAttribute must be null", attr.getValue());

        // edge case – empty key
        assertEquals("Empty string key should be stored", EMPTY_KEY, emptyKeyAttr.getKey());
        assertNull("Value must still be null for empty‑key attribute", emptyKeyAttr.getValue());
    }

    /**
     * {@link BooleanAttribute#isBooleanAttribute()} must always return {@code true},
     * regardless of the key supplied.
     */
    @Test
    public void testIsBooleanAttribute() {
        assertTrue("BooleanAttribute must report itself as a boolean attribute", attr.isBooleanAttribute());
        assertTrue("BooleanAttribute with empty key must also report true", emptyKeyAttr.isBooleanAttribute());
    }

    /**
     * The superclass {@link Attribute} does not allow a {@code null} key.
     * Passing {@code null} to the BooleanAttribute constructor should therefore
     * result in a {@link NullPointerException}.
     */
    @Test(expected = NullPointerException.class)
    public void testConstructorNullKeyThrowsException() {
        new BooleanAttribute(null);
    }

    /**
     * Equality and hash‑code are delegated to {@link Attribute}.
     * Two BooleanAttributes with the same key should be equal and have the same hash code.
     */
    @Test
    public void testEqualsAndHashCode() {
        BooleanAttribute another = new BooleanAttribute(SAMPLE_KEY);
        assertEquals("Attributes with identical keys should be equal", attr, another);
        assertEquals("Equal objects must have identical hash codes", attr.hashCode(), another.hashCode());

        BooleanAttribute different = new BooleanAttribute("disabled");
        assertNotEquals("Attributes with different keys must not be equal", attr, different);
    }

    /**
     * Ensure that {@link Attribute#toString()} reflects the boolean nature – it should
     * render only the key without an '=' sign or value.
     *
     * <p>Note: The exact format depends on the implementation of {@code Attribute#toString()},
     * but the contract of a boolean attribute is that the output contains only the key.
     */
    @Test
    public void testToStringContainsOnlyKey() {
        String repr = attr.toString();
        assertTrue("String representation should contain the key", repr.contains(SAMPLE_KEY));
        assertFalse("String representation of a boolean attribute should not contain '='", repr.contains("="));
        assertFalse("String representation should not contain 'null'", repr.contains("null"));
    }
}
```
###Test END##