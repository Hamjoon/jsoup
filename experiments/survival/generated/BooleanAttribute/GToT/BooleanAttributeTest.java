package org.jsoup.nodes;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.lang.reflect.Method;

/**
 * JUnit‑4 test suite for {@link BooleanAttribute}.
 *
 * <p>This test class covers:
 * <ul>
 *   <li>Public API – the constructor.</li>
 *   <li>Protected behaviour – {@code isBooleanAttribute()} (exposed via a test‑only subclass).</li>
 *   <li>Typical use cases, edge cases and expected exceptions.</li>
 * </ul>
 *
 * <p>All tests are written for JUnit 4 so they can be run with any
 * JUnit‑4 runner (e.g. Maven Surefire, IDE JUnit runner, etc.).</p>
 *
 * @author  “Three Experts”
 */
public class BooleanAttributeTest {

    // -----------------------------------------------------------------
    // Helper subclass to expose the protected {@code isBooleanAttribute()}
    // -----------------------------------------------------------------
    private static class TestableBooleanAttribute extends BooleanAttribute {
        public TestableBooleanAttribute(String key) {
            super(key);
        }

        /** Expose the protected method for testing. */
        @Override
        public boolean isBooleanAttribute() {
            return super.isBooleanAttribute();
        }
    }

    // -----------------------------------------------------------------
    // Fixtures
    // -----------------------------------------------------------------
    private BooleanAttribute attr;
    private final String normalKey = "checked";

    @Before
    public void setUp() {
        attr = new BooleanAttribute(normalKey);
    }

    @After
    public void tearDown() {
        attr = null;
    }

    // -----------------------------------------------------------------
    // 1️⃣  Constructor – typical use case
    // -----------------------------------------------------------------
    @Test
    public void constructor_setsKeyAndLeavesValueNull() {
        assertEquals("Key should be stored unchanged", normalKey, attr.getKey());
        // The super‑class stores the value; for a boolean attribute it must be null.
        assertNull("Value must be null for a BooleanAttribute", attr.getValue());
    }

    // -----------------------------------------------------------------
    // 2️⃣  Constructor – edge cases
    // -----------------------------------------------------------------
    @Test
    public void constructor_allowsEmptyKey() {
        BooleanAttribute emptyKeyAttr = new BooleanAttribute("");
        assertEquals("Empty string key should be accepted", "", emptyKeyAttr.getKey());
        assertNull("Value must still be null", emptyKeyAttr.getValue());
    }

    @Test
    public void constructor_allowsWhitespaceKey() {
        String wsKey = "  ";
        BooleanAttribute wsAttr = new BooleanAttribute(wsKey);
        assertEquals("Whitespace key should be stored as‑is", wsKey, wsAttr.getKey());
        assertNull(wsAttr.getValue());
    }

    @Test(expected = NullPointerException.class)
    public void constructor_nullKey_throwsNullPointerException() {
        // The parent {@code Attribute} class validates the key and throws NPE
        // when a null key is supplied. We assert that behaviour here.
        new BooleanAttribute(null);
    }

    // -----------------------------------------------------------------
    // 3️⃣  isBooleanAttribute() – normal behaviour
    // -----------------------------------------------------------------
    @Test
    public void isBooleanAttribute_returnsTrue() {
        TestableBooleanAttribute testable = new TestableBooleanAttribute("disabled");
        assertTrue("BooleanAttribute must report itself as a boolean attribute",
                testable.isBooleanAttribute());
    }

    // -----------------------------------------------------------------
    // 4️⃣  isBooleanAttribute() – reflection based verification (alternative)
    // -----------------------------------------------------------------
    @Test
    public void isBooleanAttribute_reflectionReturnsTrue() throws Exception {
        Method m = BooleanAttribute.class.getDeclaredMethod("isBooleanAttribute");
        m.setAccessible(true);
        Boolean result = (Boolean) m.invoke(attr);
        assertTrue("Reflection call should also return true", result);
    }

    // -----------------------------------------------------------------
    // 5️⃣  Inheritance checks – ensure BooleanAttribute behaves like Attribute
    // -----------------------------------------------------------------
    @Test
    public void booleanAttribute_isInstanceOfAttribute() {
        assertTrue("BooleanAttribute must be an instance of Attribute",
                attr instanceof Attribute);
    }

    // -----------------------------------------------------------------
    // 6️⃣  toString / equals / hashCode (inherited) – sanity checks
    // -----------------------------------------------------------------
    @Test
    public void equalsAndHashCode_behaveConsistently() {
        BooleanAttribute a1 = new BooleanAttribute("checked");
        BooleanAttribute a2 = new BooleanAttribute("checked");
        BooleanAttribute a3 = new BooleanAttribute("selected");

        // Equality based on key (value is always null)
        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());

        assertNotEquals(a1, a3);
        // hashCode may differ but is not required; we just ensure no NPE
        assertNotNull(a1.hashCode());
    }

    @Test
    public void toString_containsKeyAndNoValue() {
        String s = attr.toString();
        assertTrue("String representation should contain the key", s.contains("checked"));
        // Since value is null, there must be no '=' sign
        assertFalse("Boolean attribute should not contain '=null' or '='", s.contains("="));
    }
}
