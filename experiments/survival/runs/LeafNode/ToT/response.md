###Test START##
```java
package org.jsoup.nodes;

import org.jsoup.helper.Validate;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link LeafNode}.
 *
 * The tests cover:
 * <ul>
 *   <li>attribute handling (core value vs. attribute map)</li>
 *   <li>edge cases such as null keys and unsupported operations</li>
 *   <li>behaviour of methods that delegate to the parent node</li>
 *   <li>correct exception types</li>
 * </ul>
 *
 * A minimal concrete subclass {@code TestLeafNode} is provided inside the test
 * file because {@code LeafNode} is abstract and depends on the abstract members
 * of {@link Node}.
 */
public class LeafNodeTest {

    /** Simple concrete implementation of {@link LeafNode} for testing. */
    private static class TestLeafNode extends LeafNode {
        private final String name;

        TestLeafNode(String name) {
            this.name = name;
        }

        /** Expose protected parent‑setting capability for the test. */
        void setParent(Node parent) {
            // Node provides a protected method for this purpose.
            super.parentNode = parent;
        }

        @Override
        public String nodeName() {
            return name;
        }

        @Override
        public void outerHtmlHead(Appendable accum, int depth, Document.OutputSettings out) throws IOException {
            // No‑op – not needed for these tests.
        }

        @Override
        public void outerHtmlTail(Appendable accum, int depth, Document.OutputSettings out) throws IOException {
            // No‑op – not needed for these tests.
        }
    }

    private TestLeafNode leaf;

    @Before
    public void setUp() {
        leaf = new TestLeafNode("test");
    }

    /** ---------- hasAttributes() & attributes() ---------- */

    @Test
    public void hasAttributes_initiallyFalse() {
        assertFalse("Leaf should start without an Attributes object", leaf.hasAttributes());
    }

    @Test
    public void attributes_createsAttributesWhenMissing() {
        // Ensure that calling attributes() forces the creation of an Attributes instance.
        Attributes attrs = leaf.attributes();
        assertNotNull("attributes() must never return null", attrs);
        assertTrue("After attributes() call, hasAttributes() should be true", leaf.hasAttributes());
    }

    @Test
    public void attributes_preservesExistingCoreValue() {
        leaf.coreValue("core");
        Attributes attrs = leaf.attributes(); // triggers ensureAttributes()
        assertEquals("core value should be moved to attribute map under node name",
                "core", attrs.get("test"));
        assertTrue(leaf.hasAttributes());
    }

    /** ---------- coreValue() & coreValue(String) ---------- */

    @Test
    public void coreValue_setAndGetWorks() {
        leaf.coreValue("hello");
        assertEquals("hello", leaf.coreValue());
    }

    @Test
    public void coreValue_afterEnsuringAttributesIsStillAccessible() {
        leaf.coreValue("initial");
        leaf.attributes(); // forces conversion to Attributes map
        assertEquals("initial", leaf.coreValue()); // should read from the map
    }

    /** ---------- attr(String) ---------- */

    @Test
    public void attr_returnsCoreValueWhenKeyIsNodeNameAndNoAttributes() {
        leaf.coreValue("myValue");
        assertEquals("myValue", leaf.attr("test"));
    }

    @Test
    public void attr_returnsEmptyStringWhenKeyIsNotNodeNameAndNoAttributes() {
        leaf.coreValue("myValue");
        assertEquals("", leaf.attr("other"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void attr_nullKeyThrowsException() {
        leaf.attr(null);
    }

    /** ---------- attr(String, String) ---------- */

    @Test
    public void attr_setCoreValueWhenKeyEqualsNodeNameAndNoAttributes() {
        leaf.attr("test", "newCore");
        assertEquals("newCore", leaf.coreValue());
        assertFalse("Should still be a core value, not an Attributes map", leaf.hasAttributes());
    }

    @Test
    public void attr_createsAttributesWhenKeyDiffersFromNodeName() {
        leaf.attr("foo", "bar");
        assertTrue("Attributes map should be created", leaf.hasAttributes());
        assertEquals("bar", leaf.attr("foo"));
    }

    @Test
    public void attr_mixedScenario_convertsCoreToAttributesWhenNeeded() {
        leaf.coreValue("core");
        leaf.attr("foo", "bar"); // this call must convert core to map
        assertTrue(leaf.hasAttributes());
        // Both core (under node name) and new attribute must exist.
        assertEquals("core", leaf.attr("test"));
        assertEquals("bar", leaf.attr("foo"));
    }

    /** ---------- hasAttr(String) ---------- */

    @Test
    public void hasAttr_detectsExistingAttribute() {
        leaf.attr("foo", "bar");
        assertTrue(leaf.hasAttr("foo"));
        assertFalse(leaf.hasAttr("missing"));
    }

    /** ---------- removeAttr(String) ---------- */

    @Test
    public void removeAttr_deletesAttributeAndLeavesOthersIntact() {
        leaf.attr("foo", "bar");
        leaf.attr("baz", "qux");
        leaf.removeAttr("foo");
        assertFalse(leaf.hasAttr("foo"));
        assertTrue(leaf.hasAttr("baz"));
    }

    /** ---------- absUrl(String) ---------- */

    @Test
    public void absUrl_callsEnsureAttributesAndDelegatesToSuper() {
        // No base URI set, so super.absUrl returns empty string.
        leaf.attr("href", "/relative");
        String result = leaf.absUrl("href");
        assertEquals("", result);
    }

    /** ---------- baseUri() ---------- */

    @Test
    public void baseUri_withoutParentReturnsEmptyString() {
        assertEquals("", leaf.baseUri());
    }

    @Test
    public void baseUri_withParentDelegatesToParent() {
        // Use an Element (which is a concrete Node) as parent because it implements baseUri().
        Element parent = new Element("div");
        parent.attr("base", "http://example.com/");
        leaf.setParent(parent);
        assertEquals("http://example.com/", leaf.baseUri());
    }

    /** ---------- childNodeSize() ---------- */

    @Test
    public void childNodeSize_alwaysZero() {
        assertEquals(0, leaf.childNodeSize());
    }

    /** ---------- ensureChildNodes() ---------- */

    @Test(expected = UnsupportedOperationException.class)
    public void ensureChildNodes_throwsUnsupportedOperationException() {
        leaf.ensureChildNodes();
    }

    /** ---------- Additional edge cases ---------- */

    @Test
    public void attr_settingNullValueIsAllowed() {
        leaf.attr("test", null);
        // According to Node.attr implementation, a null value becomes the string "null".
        // Our LeafNode forwards to super.attr after ensureAttributes().
        assertEquals("null", leaf.attr("test"));
    }

    @Test
    public void hasAttributes_remainsFalseWhenCoreValueIsNull() {
        leaf.coreValue(null);
        assertFalse(leaf.hasAttributes());
        // attributes() should still create an empty map without throwing.
        Attributes attrs = leaf.attributes();
        assertTrue(attrs.isEmpty());
    }

    @Test
    public void coreValue_returnsEmptyStringWhenNoCoreOrAttributes() {
        assertEquals("", leaf.coreValue());
    }
}
```
###Test END##