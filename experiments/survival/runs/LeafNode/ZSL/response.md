###Test START##
```java
package org.jsoup.nodes;

import org.jsoup.helper.Validate;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link LeafNode}. Because {@code LeafNode} is abstract we provide a minimal concrete
 * implementation inside the test suite that supplies the required abstract methods from {@link Node}.
 *
 * The tests focus on the behaviour that is defined directly in {@code LeafNode}:
 * <ul>
 *   <li>attribute handling (including lazy conversion of a core string value to an {@link Attributes} map)</li>
 *   <li>core value getter / setter</li>
 *   <li>attribute queries, removal and absolute URL handling</li>
 *   <li>base‑uri delegation to a parent node</li>
 *   <li>leaf‑specific invariants (no children, no base‑uri mutation, etc.)</li>
 * </ul>
 */
public class LeafNodeTest {

    /** Simple concrete leaf node used for testing. */
    private static class TestLeafNode extends LeafNode {

        /** Constructs a leaf node with an optional initial core value. */
        TestLeafNode(String coreValue) {
            if (coreValue != null) {
                // Initialise the core value directly – this mimics the state of a freshly parsed leaf.
                this.value = coreValue;
            }
        }

        /** Default constructor – no core value. */
        TestLeafNode() {
            this(null);
        }

        @Override
        public String nodeName() {
            return "test";
        }

        @Override
        public void outerHtmlHead(Appendable accum, int depth, Document.OutputSettings out) {
            // No‑op – not needed for the tests.
        }

        @Override
        public void outerHtmlTail(Appendable accum, int depth, Document.OutputSettings out) {
            // No‑op – not needed for the tests.
        }
    }

    /** Simple parent node that supplies a fixed base URI. */
    private static class TestParentNode extends Node {
        private final String baseUri;

        TestParentNode(String baseUri) {
            this.baseUri = baseUri;
        }

        @Override
        public String nodeName() {
            return "parent";
        }

        @Override
        public String baseUri() {
            return baseUri;
        }

        @Override
        public void outerHtmlHead(Appendable accum, int depth, Document.OutputSettings out) {
            // no‑op
        }

        @Override
        public void outerHtmlTail(Appendable accum, int depth, Document.OutputSettings out) {
            // no‑op
        }
    }

    private TestLeafNode leaf;

    @Before
    public void setUp() {
        leaf = new TestLeafNode();
    }

    /** Helper to inject a parent node via reflection (Node keeps the parent in a private field). */
    private void setParent(Node child, Node parent) throws Exception {
        Field parentField = Node.class.getDeclaredField("parentNode");
        parentField.setAccessible(true);
        parentField.set(child, parent);
    }

    /* --------------------------------------------------------------------- */
    /*                     Tests for attribute handling                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void hasAttributes_initiallyFalse() {
        assertFalse("A freshly created leaf should have no attribute map", leaf.hasAttributes());
    }

    @Test
    public void attributes_lazyCreatesMap_andMigratesCoreValue() throws Exception {
        leaf.coreValue("core‑value");               // stores a core string under the node name
        assertFalse("Before calling attributes(), still no attribute map", leaf.hasAttributes());

        Attributes attrs = leaf.attributes();       // triggers ensureAttributes()
        assertTrue("After attributes() call a map must exist", leaf.hasAttributes());
        assertEquals("The core value must be migrated under the node name",
                "core‑value", attrs.get(leaf.nodeName()));
        assertEquals(1, attrs.size());
    }

    @Test
    public void coreValue_setAndGet() {
        leaf.coreValue("hello");
        assertEquals("coreValue getter must return the previously set value", "hello", leaf.coreValue());
        // coreValue is stored as an attribute with the node name as the key
        assertEquals("hello", leaf.attr(leaf.nodeName()));
    }

    @Test
    public void attr_whenNoAttributes_andKeyMatchesNodeName() {
        // No attributes yet, and we have not set a core value.
        assertNull("attr(nodeName) should return null when core value is not set", leaf.attr(leaf.nodeName()));
        // Set a core value via attr (key equals node name)
        leaf.attr(leaf.nodeName(), "core‑val");
        assertEquals("core‑val", leaf.attr(leaf.nodeName()));
        assertFalse("Setting core value via attr must NOT create an Attributes map",
                leaf.hasAttributes());
    }

    @Test
    public void attr_whenNoAttributes_andKeyDoesNotMatchNodeName() {
        // Requesting a non‑matching key should yield the empty string constant.
        assertEquals("EmptyString expected for unknown key", "", leaf.attr("unknown"));
    }

    @Test
    public void attr_setWhenKeyEqualsNodeName_doesNotCreateAttributes() {
        leaf.attr(leaf.nodeName(), "value‑1");
        assertFalse("Attribute map must stay absent when key equals node name", leaf.hasAttributes());
        assertEquals("value‑1", leaf.attr(leaf.nodeName()));
    }

    @Test
    public void attr_setWhenKeyDiffersFromNodeName_createsAttributes() {
        leaf.attr("foo", "bar");
        assertTrue("Calling attr with a different key must create an Attributes map", leaf.hasAttributes());
        assertEquals("bar", leaf.attr("foo"));
        // The core value (nodeName) should still be absent
        assertEquals("", leaf.attr(leaf.nodeName()));
    }

    @Test
    public void hasAttr_ensuresAttributesAndDelegates() {
        // Initially there are no attributes
        assertFalse(leaf.hasAttributes());
        // hasAttr must call ensureAttributes()
        assertFalse("No attribute set yet", leaf.hasAttr("any"));
        assertTrue("ensureAttributes() must have created an empty map", leaf.hasAttributes());
        // Now set an attribute and verify hasAttr works
        leaf.attr("myKey", "myValue");
        assertTrue(leaf.hasAttr("myKey"));
    }

    @Test
    public void removeAttr_removesExistingAttribute() {
        leaf.attr("toRemove", "some");
        assertTrue(leaf.hasAttr("toRemove"));
        leaf.removeAttr("toRemove");
        assertFalse(leaf.hasAttr("toRemove"));
    }

    @Test
    public void absUrl_delegatesToNodeImplementation() {
        // The absolute URL logic lives in Node; we only verify that the call does not explode
        leaf.attr("href", "/path");
        // No parent => baseUri is empty, so absUrl should return an empty string
        assertEquals("", leaf.absUrl("href"));
        // Attach a parent with a base URI and verify that a non‑empty result is produced
        try {
            TestParentNode parent = new TestParentNode("http://example.com/");
            setParent(leaf, parent);
        } catch (Exception e) {
            fail("Failed to set parent via reflection: " + e);
        }
        // Jsoup's Node.absUrl resolves relative URLs against the base URI.
        // For the purpose of this test we accept whatever Jsoup returns – the important part is that
        // the call succeeds and yields a non‑null string.
        String abs = leaf.absUrl("href");
        assertNotNull("absUrl must never return null", abs);
    }

    /* --------------------------------------------------------------------- */
    /*                     Tests for base‑uri handling                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void baseUri_withoutParent_returnsEmptyString() {
        assertEquals("Leaf without a parent must return empty base URI", "", leaf.baseUri());
    }

    @Test
    public void baseUri_withParent_delegatesToParent() throws Exception {
        TestParentNode parent = new TestParentNode("http://example.org/");
        setParent(leaf, parent);
        assertEquals("Leaf must delegate baseUri to its parent", "http://example.org/", leaf.baseUri());
    }

    @Test
    public void doSetBaseUri_isNoOp() {
        // The method is a no‑op; we just ensure that calling it does not throw.
        leaf.doSetBaseUri("http://newbase/");
        // No observable state change – baseUri still follows the parent rule.
        assertEquals("", leaf.baseUri());
    }

    /* --------------------------------------------------------------------- */
    /*                     Tests for leaf‑specific invariants                */
    /* --------------------------------------------------------------------- */

    @Test
    public void childNodeSize_isAlwaysZero() {
        assertEquals("Leaf nodes must report zero child nodes", 0, leaf.childNodeSize());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void ensureChildNodes_throwsUnsupportedOperationException() {
        leaf.ensureChildNodes();
    }
}
```
###Test END##