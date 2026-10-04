###Test START##
```java
package org.jsoup.nodes;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link LeafNode}.
 *
 * The tests cover:
 * <ul>
 *   <li>normal behaviour of each public method</li>
 *   <li>edge cases such as null arguments, unknown attribute keys, and absence of attributes</li>
 *   <li>exceptional paths (e.g. {@code ensureChildNodes()} throws {@code UnsupportedOperationException})</li>
 * </ul>
 *
 * Because {@code LeafNode} is abstract we create a minimal concrete subclass
 * {@code TestLeafNode} that implements the remaining abstract members of {@code Node}.
 */
public class LeafNodeTest {

    /** Simple concrete implementation of {@link LeafNode} for testing purposes. */
    private static class TestLeafNode extends LeafNode {
        private final String name;

        TestLeafNode(String name) {
            this.name = name;
        }

        /** Node name used by {@code LeafNode} for core value handling. */
        @Override
        public String nodeName() {
            return name;
        }

        /** Required abstract method – not used in these tests. */
        @Override
        public void outerHtmlHead(Appendable accum, int depth, Document.OutputSettings out) {
            // no‑op
        }

        /** Required abstract method – not used in these tests. */
        @Override
        public void outerHtmlTail(Appendable accum, int depth, Document.OutputSettings out) {
            // no‑op
        }
    }

    private TestLeafNode leaf;

    @Before
    public void setUp() {
        leaf = new TestLeafNode("test");
    }

    /* -------------------------------------------------------------------- */
    /*  1. hasAttributes()                                                   */
    /* -------------------------------------------------------------------- */

    @Test
    public void hasAttributes_initiallyFalse() {
        assertFalse("LeafNode should have no attributes initially", leaf.hasAttributes());
    }

    @Test
    public void hasAttributes_afterCreatingAttributes_isTrue() {
        leaf.attributes(); // forces creation of Attributes object
        assertTrue("LeafNode should report having attributes after attributes() call", leaf.hasAttributes());
    }

    /* -------------------------------------------------------------------- */
    /*  2. attributes() – creates Attributes lazily and returns it          */
    /* -------------------------------------------------------------------- */

    @Test
    public void attributes_createsAndReturnsAttributes() {
        Attributes attrs = leaf.attributes();
        assertNotNull("attributes() must never return null", attrs);
        assertSame("Repeated call should return the same Attributes instance", attrs, leaf.attributes());
        assertTrue("After calling attributes() the node must report having attributes", leaf.hasAttributes());
    }

    /* -------------------------------------------------------------------- */
    /*  3. coreValue() / coreValue(String) – getter & setter using attr()   */
    /* -------------------------------------------------------------------- */

    @Test
    public void coreValue_setAndGet_usingCoreValueMethods() {
        leaf.coreValue("hello");
        assertEquals("coreValue getter should return the previously set value", "hello", leaf.coreValue());
    }

    @Test
    public void coreValue_whenNoAttributes_returnsEmptyString() {
        // No attributes and no core value set yet
        assertEquals("When no core value is present, coreValue() should return empty string",
                "", leaf.coreValue());
    }

    /* -------------------------------------------------------------------- */
    /*  4. attr(String) – read attribute / core value                        */
    /* -------------------------------------------------------------------- */

    @Test
    public void attr_keyEqualsNodeName_returnsCoreValue() {
        leaf.coreValue("core");
        assertEquals("attr(key) where key == nodeName should return core value",
                "core", leaf.attr("test"));
    }

    @Test
    public void attr_keyNotNodeNameAndNoAttributes_returnsEmptyString() {
        leaf.coreValue("core");
        assertEquals("attr(key) with unknown key and no attribute map should return empty string",
                "", leaf.attr("unknown"));
    }

    @Test(expected = NullPointerException.class)
    public void attr_nullKey_throwsException() {
        leaf.attr((String) null);
    }

    /* -------------------------------------------------------------------- */
    /*  5. attr(String, String) – set attribute / core value                */
    /* -------------------------------------------------------------------- */

    @Test
    public void attr_setCoreValue_whenKeyEqualsNodeName_andNoAttributes() {
        leaf.attr("test", "newCore");
        assertEquals("Core value should be updated directly", "newCore", leaf.attr("test"));
    }

    @Test
    public void attr_setAttributeWhenKeyDiffersFromNodeName_createsAttributes() {
        leaf.attr("other", "value");
        // after setting a non‑core attribute the internal value must be an Attributes instance
        assertTrue("LeafNode should now contain an Attributes object", leaf.hasAttributes());
        assertEquals("The attribute should be stored in the map", "value", leaf.attr("other"));
        // core value should be unchanged (still null)
        assertEquals("Core value (nodeName) should be empty string when not set", "", leaf.attr("test"));
    }

    @Test
    public void attr_setAttributeWithNullValue_isHandledGracefully() {
        leaf.attr("nullVal", null);
        assertTrue("Attributes map should be created", leaf.hasAttributes());
        assertNull("Stored value should be null", leaf.attr("nullVal"));
    }

    /* -------------------------------------------------------------------- */
    /*  6. hasAttr(String) – delegated to Attributes after ensureAttributes */
    /* -------------------------------------------------------------------- */

    @Test
    public void hasAttr_trueWhenAttributeExists() {
        leaf.attr("exists", "yes");
        assertTrue("hasAttr should return true for existing attribute", leaf.hasAttr("exists"));
    }

    @Test
    public void hasAttr_falseWhenAttributeMissing() {
        leaf.attr("exists", "yes");
        assertFalse("hasAttr should return false for missing attribute", leaf.hasAttr("missing"));
    }

    /* -------------------------------------------------------------------- */
    /*  7. removeAttr(String) – removes attribute from the map               */
    /* -------------------------------------------------------------------- */

    @Test
    public void removeAttr_deletesExistingAttribute() {
        leaf.attr("toRemove", "tmp");
        assertTrue(leaf.hasAttr("toRemove"));
        leaf.removeAttr("toRemove");
        assertFalse("Attribute should be removed", leaf.hasAttr("toRemove"));
    }

    /* -------------------------------------------------------------------- */
    /*  8. absUrl(String) – simply forwards to super (no special logic)    */
    /* -------------------------------------------------------------------- */

    @Test
    public void absUrl_delegatesWithoutException() {
        // No base URI is set; super.absUrl returns empty string.
        assertEquals("absUrl should return empty string when base URI is missing",
                "", leaf.absUrl("any"));
    }

    /* -------------------------------------------------------------------- */
    /*  9. baseUri() – returns parent base URI or empty string               */
    /* -------------------------------------------------------------------- */

    @Test
    public void baseUri_withoutParent_returnsEmptyString() {
        assertEquals("Leaf node without a parent must return empty base URI", "", leaf.baseUri());
    }

    @Test
    public void baseUri_withParent_returnsParentBaseUri() {
        Element parent = new Element("div");
        parent.attr("base", "http://example.com/");
        // jsoup's Element treats the attribute named "base" as the base URI when
        // Element.baseUri() is called. To keep the test simple we set the base URI
        // directly via the protected method.
        parent.setBaseUri("http://example.com/");
        leaf.setParentNode(parent);
        assertEquals("Leaf node should inherit the base URI from its parent",
                "http://example.com/", leaf.baseUri());
    }

    /* -------------------------------------------------------------------- */
    /* 10. doSetBaseUri(String) – no‑op                                      */
    /* -------------------------------------------------------------------- */

    @Test
    public void doSetBaseUri_isNoOp() {
        // The method is protected; we invoke it via reflection to ensure it does not throw.
        try {
            java.lang.reflect.Method m = LeafNode.class.getDeclaredMethod("doSetBaseUri", String.class);
            m.setAccessible(true);
            m.invoke(leaf, "http://newbase/");
        } catch (Exception e) {
            fail("doSetBaseUri should be a no‑op and must not throw any exception");
        }
    }

    /* -------------------------------------------------------------------- */
    /* 11. childNodeSize() – must always be zero                             */
    /* -------------------------------------------------------------------- */

    @Test
    public void childNodeSize_alwaysZero() {
        assertEquals("Leaf nodes have no children, childNodeSize must be 0", 0, leaf.childNodeSize());
    }

    /* -------------------------------------------------------------------- */
    /* 12. ensureChildNodes() – must throw UnsupportedOperationException    */
    /* -------------------------------------------------------------------- */

    @Test(expected = UnsupportedOperationException.class)
    public void ensureChildNodes_throwsUnsupportedOperationException() {
        // The method is protected; use reflection to call it.
        java.lang.reflect.Method m = LeafNode.class.getDeclaredMethod("ensureChildNodes");
        m.setAccessible(true);
        m.invoke(leaf);
    }
}
```
###Test END##