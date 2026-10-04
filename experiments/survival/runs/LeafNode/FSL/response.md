###Test START##
package org.jsoup.nodes;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Concrete implementation of the abstract {@link LeafNode} for testing purposes.
 */
class TestLeafNode extends LeafNode {
    private Node parent;

    TestLeafNode() {
        this.value = null;
    }

    @Override
    public String nodeName() {
        return "test";
    }

    // Minimal implementations required by {@link Node}
    @Override
    void outerHtmlHead(Appendable accum, int depth, Document.OutputSettings out) { }

    @Override
    void outerHtmlTail(Appendable accum, int depth, Document.OutputSettings out) { }

    @Override
    public Node parent() {
        return parent;
    }

    void setParent(Node parent) {
        this.parent = parent;
    }
}

/**
 * JUnit‑4 test suite that exercises every public and protected behaviour of {@link LeafNode}.
 */
public class LeafNodeTest {

    @Test
    public void hasAttributesInitiallyFalse() {
        TestLeafNode node = new TestLeafNode();
        assertFalse("A freshly created leaf node must not have an Attributes instance", node.hasAttributes());
    }

    @Test
    public void coreValueWhenNoAttributes() {
        TestLeafNode node = new TestLeafNode();

        // set core value through the dedicated setter
        node.coreValue("core-value");

        // attr(key) where key == nodeName() should return the core value
        assertEquals("core-value", node.attr("test"));

        // attr for any other key must return the empty string (Node.EmptyString)
        assertEquals("", node.attr("other"));

        // the node must still report that it does **not** have an Attributes object
        assertFalse(node.hasAttributes());
    }

    @Test
    public void attrSetsCoreValueWhenKeyEqualsNodeName() {
        TestLeafNode node = new TestLeafNode();

        // using the generic attr(key, value) API with the node's own name should store a core value
        node.attr("test", "hello");
        assertEquals("hello", node.coreValue());
        assertFalse("Setting the node's own attribute must not create an Attributes map", node.hasAttributes());
    }

    @Test
    public void attrCreatesAttributesWhenKeyIsDifferent() {
        TestLeafNode node = new TestLeafNode();

        // Setting a non‑node‑name attribute forces conversion to an Attributes map
        node.attr("id", "my-id");
        assertTrue("Attributes map should be created after a non‑core attribute is added", node.hasAttributes());

        // The attribute must be retrievable via attr()
        assertEquals("my-id", node.attr("id"));

        // Core value is still null because we never set it
        assertNull(node.coreValue());
    }

    @Test
    public void coreValueConvertedWhenAddingOtherAttribute() {
        TestLeafNode node = new TestLeafNode();

        // first set a core value
        node.coreValue("core");

        // now add a different attribute – this should trigger conversion to Attributes
        node.attr("class", "myClass");

        assertTrue("Attributes map must exist after conversion", node.hasAttributes());

        // both the original core value and the newly added attribute must be present
        assertEquals("core", node.attr("test"));
        assertEquals("myClass", node.attr("class"));
    }

    @Test
    public void hasAttrAndRemoveAttrBehaviour() {
        TestLeafNode node = new TestLeafNode();

        node.attr("title", "myTitle");
        assertTrue(node.hasAttr("title"));
        assertFalse(node.hasAttr("nonexistent"));

        // remove the attribute and verify it disappears
        node.removeAttr("title");
        assertFalse(node.hasAttr("title"));
    }

    @Test
    public void absUrlDelegatesToSuperWhenAttributesExist() {
        TestLeafNode node = new TestLeafNode();

        // set a base attribute that would normally be used to resolve a URL
        node.attr("href", "/path");
        // The baseUri of a leaf node without a parent is the empty string, so absUrl should return the raw value
        assertEquals("/path", node.absUrl("href"));
    }

    @Test
    public void baseUriReturnsEmptyWhenNoParent() {
        TestLeafNode node = new TestLeafNode();
        assertEquals("Leaf nodes without a parent must have an empty baseUri", "", node.baseUri());
    }

    @Test
    public void childNodeSizeIsZero() {
        TestLeafNode node = new TestLeafNode();
        assertEquals("Leaf nodes must report zero child nodes", 0, node.childNodeSize());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void ensureChildNodesThrows() {
        TestLeafNode node = new TestLeafNode();
        node.ensureChildNodes(); // should throw UnsupportedOperationException
    }

    @Test(expected = NullPointerException.class)
    public void attrRejectsNullKey() {
        TestLeafNode node = new TestLeafNode();
        node.attr(null, "value"); // Validate.notNull should throw NPE
    }

    @Test
    public void attrRejectsNullKeyOnGetter() {
        TestLeafNode node = new TestLeafNode();
        try {
            node.attr(null);
            fail("Calling attr(String) with a null key must throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }
}
###Test END##