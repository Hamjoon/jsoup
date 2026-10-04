###Test START##
```java
/****************************************************************************************
 *  JUnit 4 test suite for the abstract LeafNode class (jsoup)
 *
 *  The tests cover:
 *   – All public methods of LeafNode
 *   – Normal functionality
 *   – Edge‑cases such as null arguments, lazy attribute creation and exception paths
 *
 *  A minimal concrete subclass (TestLeafNode) is defined inside the test file to
 *  instantiate the abstract LeafNode.  The subclass implements the abstract members
 *  required by Node (nodeName, outerHtmlHead, outerHtmlTail) with no‑op behaviour.
 *
 *  The test class resides in the same package as LeafNode so that protected members
 *  (e.g. ensureChildNodes) are directly accessible.
 ****************************************************************************************/

package org.jsoup.nodes;

import org.jsoup.helper.Validate;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class LeafNodeTest {

    /**
     * Minimal concrete implementation of LeafNode used only for testing.
     */
    static class TestLeafNode extends LeafNode {
        @Override
        public String nodeName() {
            return "test";
        }

        @Override
        protected void outerHtmlHead(Appendable accum) {
            // no‑op – not needed for the tests
        }

        @Override
        protected void outerHtmlTail(Appendable accum) {
            // no‑op – not needed for the tests
        }
    }

    /* -------------------------------------------------
     *  1.  List of public methods (signatures) in LeafNode
     * -------------------------------------------------
     *
     * public final Attributes attributes()
     * public String attr(String key)
     * public Node attr(String key, String value)
     * public boolean hasAttr(String key)
     * public Node removeAttr(String key)
     * public String absUrl(String key)
     * public String baseUri()
     * public int childNodeSize()
     *
     */

    /*** BASIC FUNCTIONALITY TESTS ***/

    @Test
    public void testAttributesInitiallyCreatesEmptyAttributes() {
        TestLeafNode leaf = new TestLeafNode();
        // No attributes have been set yet – calling attributes() should lazily create the map
        Attributes attrs = leaf.attributes();
        assertNotNull("attributes() should never return null", attrs);
        assertTrue("Newly created attributes map must be empty", attrs.isEmpty());

        // After lazy creation, the internal value must be an Attributes instance
        assertTrue("Internal value should now be an Attributes object",
                leaf.attributes() instanceof Attributes);
    }

    @Test
    public void testAttrCoreValueWhenNoAttributes() {
        TestLeafNode leaf = new TestLeafNode();

        // No attributes, value is null – asking for the node's own key returns null
        assertNull("attr(nodeName) should return null when core value not set", leaf.attr("test"));

        // Asking for a different key returns the empty string constant defined in Node
        assertEquals("attr(otherKey) should return empty string when no attributes",
                "", leaf.attr("id"));
    }

    @Test
    public void testAttrSetCoreValue() {
        TestLeafNode leaf = new TestLeafNode();

        // Set core value using the same key as nodeName()
        leaf.attr("test", "core-value");

        // The core value should now be returned directly
        assertEquals("core-value", leaf.attr("test"));

        // The underlying Attributes map must contain a single entry (nodeName -> core-value)
        Attributes attrs = leaf.attributes();
        assertEquals(1, attrs.size());
        assertEquals("core-value", attrs.get("test"));
    }

    @Test
    public void testAttrSetNonCoreCreatesAttributes() {
        TestLeafNode leaf = new TestLeafNode();

        // Set an attribute whose key differs from nodeName()
        leaf.attr("id", "12345");

        // The attribute should be retrievable via attr()
        assertEquals("12345", leaf.attr("id"));

        // The core value (nodeName) should still be null
        assertNull("Core value should remain null when only non‑core attributes are set",
                leaf.attr("test"));

        // The Attributes map must contain exactly the non‑core entry
        Attributes attrs = leaf.attributes();
        assertEquals(1, attrs.size());
        assertEquals("12345", attrs.get("id"));
    }

    @Test
    public void testHasAttr() {
        TestLeafNode leaf = new TestLeafNode();
        leaf.attr("id", "abc");
        leaf.attr("test", "core");

        assertTrue("hasAttr should detect existing attribute 'id'", leaf.hasAttr("id"));
        assertTrue("hasAttr should detect existing core attribute 'test'", leaf.hasAttr("test"));
        assertFalse("hasAttr should return false for missing attribute", leaf.hasAttr("missing"));
    }

    @Test
    public void testRemoveAttr() {
        TestLeafNode leaf = new TestLeafNode();
        leaf.attr("id", "xyz");
        leaf.attr("test", "core");

        // Remove a non‑core attribute
        leaf.removeAttr("id");
        assertFalse("Attribute 'id' must be removed", leaf.hasAttr("id"));
        assertTrue("Core attribute must still exist after removing another attribute",
                leaf.hasAttr("test"));

        // Remove the core attribute – this should also clear the internal core value
        leaf.removeAttr("test");
        assertFalse("Core attribute should be removed", leaf.hasAttr("test"));
        assertNull("After removal, core value should be null", leaf.attr("test"));
    }

    @Test
    public void testAbsUrlWhenNoBaseUri() {
        TestLeafNode leaf = new TestLeafNode();
        leaf.attr("href", "/path/page.html");

        // No parent and no base URI – absUrl should resolve to the raw attribute value
        // (jsoup's Node.absUrl returns the attribute value when it cannot be resolved)
        assertEquals("/path/page.html", leaf.absUrl("href"));
    }

    @Test
    public void testBaseUriWithoutParent() {
        TestLeafNode leaf = new TestLeafNode();
        assertEquals("Base URI of a leaf node without a parent must be empty string",
                "", leaf.baseUri());
    }

    @Test
    public void testChildNodeSizeAlwaysZero() {
        TestLeafNode leaf = new TestLeafNode();
        assertEquals("Leaf nodes must report zero child nodes", 0, leaf.childNodeSize());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testEnsureChildNodesThrows() {
        TestLeafNode leaf = new TestLeafNode();
        // Direct call – method is protected but we are in the same package
        leaf.ensureChildNodes();
    }

    /*** EDGE‑CASE & EXCEPTION TESTS ***/

    @Test(expected = NullPointerException.class)
    public void testAttrNullKeyThrows() {
        TestLeafNode leaf = new TestLeafNode();
        leaf.attr(null); // Validate.notNull should trigger NPE
    }

    @Test(expected = NullPointerException.class)
    public void testAttrNullKeyWithValueThrows() {
        TestLeafNode leaf = new TestLeafNode();
        leaf.attr(null, "value"); // underlying super.attr will validate the key
    }

    @Test
    public void testAttrNullValueHandledGracefully() {
        TestLeafNode leaf = new TestLeafNode();

        // Setting a null value for a core attribute – this should replace the core value with null
        leaf.attr("test", null);
        assertNull("Core value should become null after setting null", leaf.attr("test"));

        // The Attributes map must still be created and contain the key with a null value
        Attributes attrs = leaf.attributes();
        assertTrue("Attributes map must contain the core key even when its value is null",
                attrs.hasKey("test"));
        assertNull("Stored value for core key should be null", attrs.get("test"));
    }

    @Test
    public void testMultipleAttributesConversionFromCoreValue() {
        TestLeafNode leaf = new TestLeafNode();

        // Set a core value first
        leaf.attr("test", "core1");

        // Now add another attribute – this forces conversion to an Attributes map
        leaf.attr("class", "myClass");

        Attributes attrs = leaf.attributes();
        assertEquals("Attributes map must contain both core and new attribute", 2, attrs.size());
        assertEquals("core1", attrs.get("test"));
        assertEquals("myClass", attrs.get("class"));
    }
}
```
###Test END##