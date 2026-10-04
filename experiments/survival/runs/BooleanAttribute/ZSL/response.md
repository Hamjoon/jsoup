###Test START##
```java
package org.jsoup.nodes;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link BooleanAttribute}.
 *
 * These tests focus on the behaviour that is specific to BooleanAttribute
 * (constructor handling and the overridden {@code isBooleanAttribute} method),
 * as well as a few inherited aspects from {@link Attribute} that are relevant
 * for a boolean attribute (e.g., the stored value should be {@code null}).
 *
 * The tests are written for JUnit 4.
 */
public class BooleanAttributeTest {

    /**
     * Verify that the constructor correctly stores the provided key and
     * initializes the value to {@code null} (as required for boolean
     * attributes). The super‑class {@link Attribute} stores the key and value,
     * so we validate both through the inherited accessor methods.
     */
    @Test
    public void testConstructorStoresKeyAndNullValue() {
        String key = "checked";
        BooleanAttribute attr = new BooleanAttribute(key);

        // The key should be exactly what we passed in.
        assertEquals("Key should match constructor argument", key, attr.getKey());

        // Boolean attributes are written without a value, therefore the
        // stored value must be {@code null}.
        assertNull("Value should be null for a BooleanAttribute", attr.getValue());

        // The toString representation of a boolean attribute in jsoup is the key
        // alone (no '=value'), but we guard against a possible implementation
        // that could return the key when the value is null.
        assertEquals("String representation should be just the key", key, attr.toString());
    }

    /**
     * Verify that {@code isBooleanAttribute()} is overridden to return {@code true}.
     * This method is protected in the super‑class, but we can invoke it via a
     * subclass instance because the test class resides in the same package.
     */
    @Test
    public void testIsBooleanAttributeReturnsTrue() {
        BooleanAttribute attr = new BooleanAttribute("disabled");
        // Direct call because the method is protected and the test class is
        // in the same package (org.jsoup.nodes).
        assertTrue("BooleanAttribute should report itself as a boolean attribute",
                attr.isBooleanAttribute());
    }

    /**
     * Ensure that a BooleanAttribute behaves like a normal Attribute regarding
     * equality and hashCode when only the key differs (value is always null).
     * This indirectly tests that the superclass's equals/hashCode contract
     * works with a null value.
     */
    @Test
    public void testEqualityAndHashCode() {
        BooleanAttribute a1 = new BooleanAttribute("readonly");
        BooleanAttribute a2 = new BooleanAttribute("readonly");
        BooleanAttribute a3 = new BooleanAttribute("multiple");

        // Same key → should be equal
        assertEquals("Attributes with same key should be equal", a1, a2);
        assertEquals("Hash codes should match for equal attributes", a1.hashCode(), a2.hashCode());

        // Different key → not equal
        assertNotEquals("Attributes with different keys should not be equal", a1, a3);
    }

    /**
     * Test that the attribute correctly reports that it has no value.
     * The {@link Attribute#hasValue()} method (commonly present in jsoup)
     * returns {@code false} when the stored value is {@code null}.
     */
    @Test
    public void testHasValueIsFalse() {
        BooleanAttribute attr = new BooleanAttribute("selected");
        // Assuming the super class provides hasValue(); if not, this test will
        // simply compile‑time fail, indicating the need to adjust it according to
        // the actual API.
        assertFalse("BooleanAttribute should not have a value", attr.hasValue());
    }

    /**
     * Verify that the attribute can be used in a collection without
     * unexpected side effects (e.g., the key is used as the map key).
     */
    @Test
    public void testUsageInCollections() {
        java.util.Set<Attribute> set = new java.util.HashSet<>();
        BooleanAttribute attr = new BooleanAttribute("hidden");
        set.add(attr);

        // The set should contain an attribute with the same key.
        assertTrue("Set should contain the added BooleanAttribute", set.contains(attr));

        // Adding another BooleanAttribute with the same key should not increase size.
        BooleanAttribute duplicate = new BooleanAttribute("hidden");
        set.add(duplicate);
        assertEquals("Set size should remain 1 after adding duplicate key", 1, set.size());
    }
}
```
###Test END##