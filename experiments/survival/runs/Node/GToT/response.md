###Test START##
```java
package org.jsoup.nodes;

import org.jsoup.parser.Parser;
import org.jsoup.select.NodeVisitor;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link Node}.  Because {@code Node} is abstract we provide a very small concrete
 * implementation {@code TestNode} that supplies the required abstract behaviour.  The tests cover:
 * <ul>
 *   <li>basic public method contracts</li>
 *   <li>edge‑case handling (null arguments, empty strings, out‑of‑range indexes)</li>
 *   <li>exception scenarios thrown by {@link Validate}</li>
 *   <li>behaviour of tree manipulation methods (before/after, wrap, unwrap, replaceWith, etc.)</li>
 *   <li>identity vs. value equality, cloning and shallow cloning</li>
 * </ul>
 */
public class NodeTest {

    /** Minimal concrete subclass of {@link Node} used for testing. */
    private static class TestNode extends Node {
        private final Attributes attrs = new Attributes();
        private final List<Node> children = new ArrayList<>();
        private String baseUri = "";
        private final String name; // nodeName

        TestNode(String name) {
            this.name = name;
        }

        @Override public String nodeName() { return name; }

        @Override protected boolean hasAttributes() { return !attrs.isEmpty(); }

        @Override public Attributes attributes() { return attrs; }

        @Override public String baseUri() { return baseUri; }

        @Override protected void doSetBaseUri(String baseUri) { this.baseUri = baseUri; }

        @Override protected List<Node> ensureChildNodes() { return children; }

        @Override public int childNodeSize() { return children.size(); }

        /** Simple outer HTML – just wraps the node name. */
        @Override void outerHtmlHead(Appendable accum, int depth, Document.OutputSettings out) throws IOException {
            accum.append("<").append(name).append(">");
        }

        @Override void outerHtmlTail(Appendable accum, int depth, Document.OutputSettings out) throws IOException {
            accum.append("</").append(name).append(">");
        }
    }

    private TestNode parent;
    private TestNode child1;
    private TestNode child2;

    @Before
    public void setUp() {
        parent = new TestNode("parent");
        child1 = new TestNode("child1");
        child2 = new TestNode("child2");
        parent.addChildren(child1, child2); // establish hierarchy
    }

    // -----------------------------------------------------------------------
    // 1.  attr(String)  &  attr(String, String)
    // -----------------------------------------------------------------------
    @Test
    public void testAttrGetSet() {
        // initially missing attribute -> empty string
        assertEquals("", child1.attr("href"));

        // set and retrieve (case‑insensitive)
        child1.attr("HREF", "http://example.com");
        assertEquals("http://example.com", child1.attr("href"));
        assertEquals("http://example.com", child1.attr("HREF"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttrNullKeyThrows() {
        child1.attr((String) null);
    }

    // -----------------------------------------------------------------------
    // 2.  hasAttr(String)
    // -----------------------------------------------------------------------
    @Test
    public void testHasAttr() {
        child1.attr("data", "value");
        assertTrue(child1.hasAttr("DATA"));
        assertFalse(child1.hasAttr("missing"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHasAttrNullKeyThrows() {
        child1.hasAttr(null);
    }

    // -----------------------------------------------------------------------
    // 3.  removeAttr(String)
    // -----------------------------------------------------------------------
    @Test
    public void testRemoveAttr() {
        child1.attr("title", "hello");
        assertTrue(child1.hasAttr("title"));
        child1.removeAttr("title");
        assertFalse(child1.hasAttr("title"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveAttrNullKeyThrows() {
        child1.removeAttr(null);
    }

    // -----------------------------------------------------------------------
    // 4.  clearAttributes()
    // -----------------------------------------------------------------------
    @Test
    public void testClearAttributes() {
        child1.attr("a", "1").attr("b", "2");
        assertEquals(2, child1.attributes().size());
        child1.clearAttributes();
        assertEquals(0, child1.attributes().size());
    }

    // -----------------------------------------------------------------------
    // 5.  setBaseUri(String)  &  absUrl(String)
    // -----------------------------------------------------------------------
    @Test
    public void testSetBaseUriAndAbsUrl() {
        child1.attr("href", "/path");
        child1.setBaseUri("http://example.com");
        assertEquals("http://example.com/path", child1.absUrl("href"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetBaseUriNullThrows() {
        child1.setBaseUri(null);
    }

    @Test
    public void testAbsUrlMissingAttrReturnsEmpty() {
        child1.setBaseUri("http://example.com");
        assertEquals("", child1.absUrl("nonexistent"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAbsUrlEmptyKeyThrows() {
        child1.absUrl("");
    }

    // -----------------------------------------------------------------------
    // 6.  childNode(int)  &  childNodes()
    // -----------------------------------------------------------------------
    @Test
    public void testChildNodeAndList() {
        assertSame(child1, parent.childNode(0));
        assertSame(child2, parent.childNode(1));
        List<Node> list = parent.childNodes();
        assertEquals(2, list.size());
        assertTrue(list.contains(child1));
        assertTrue(list.contains(child2));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testChildNodeOutOfBounds() {
        parent.childNode(5);
    }

    // -----------------------------------------------------------------------
    // 7.  childNodesCopy()
    // -----------------------------------------------------------------------
    @Test
    public void testChildNodesCopyDeepClone() {
        List<Node> copy = parent.childNodesCopy();
        assertEquals(2, copy.size());
        // modify copy – original must stay unchanged
        copy.get(0).attr("new", "val");
        assertFalse(parent.childNode(0).hasAttr("new"));
    }

    // -----------------------------------------------------------------------
    // 8.  siblingNodes(), nextSibling(), previousSibling()
    // -----------------------------------------------------------------------
    @Test
    public void testSiblingNavigation() {
        assertEquals(child2, child1.nextSibling());
        assertNull(child2.nextSibling());

        assertNull(child1.previousSibling());
        assertEquals(child1, child2.previousSibling());

        List<Node> siblings = child2.siblingNodes();
        assertEquals(1, siblings.size());
        assertSame(child1, siblings.get(0));
    }

    @Test
    public void testSiblingMethodsWhenNoParent() {
        TestNode lone = new TestNode("lone");
        assertTrue(lone.siblingNodes().isEmpty());
        assertNull(lone.nextSibling());
        assertNull(lone.previousSibling());
    }

    // -----------------------------------------------------------------------
    // 9.  before(Node) / after(Node)
    // -----------------------------------------------------------------------
    @Test
    public void testInsertSiblingNode() {
        TestNode newNode = new TestNode("new");
        child1.before(newNode); // insert before child1 (becomes first child)
        assertSame(newNode, parent.childNode(0));
        assertEquals(3, parent.childNodeSize());

        TestNode afterNode = new TestNode("after");
        child2.after(afterNode); // insert after child2 (now last)
        assertSame(afterNode, parent.childNode(3));
        assertEquals(4, parent.childNodeSize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBeforeNullNodeThrows() {
        child1.before((Node) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAfterNullNodeThrows() {
        child1.after((Node) null);
    }

    // -----------------------------------------------------------------------
    // 10. wrap(String) – normal case and error handling
    // -----------------------------------------------------------------------
    @Test
    public void testWrapSimpleDiv() {
        // wrap child1 with <div class="wrapper"></div>
        child1.wrap("<div class=\"wrapper\"></div>");
        // after wrap, child1's parent should be the new wrapper element
        Node wrapper = child1.parent();
        assertNotNull(wrapper);
        assertEquals("div", wrapper.nodeName());
        assertEquals("wrapper", wrapper.attr("class"));
        // wrapper's parent should still be the original parent
        assertSame(parent, wrapper.parent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrapEmptyHtmlThrows() {
        child1.wrap("");
    }

    // -----------------------------------------------------------------------
    // 11. unwrap()
    // -----------------------------------------------------------------------
    @Test
    public void testUnwrapMovesChildrenUp() {
        TestNode inner = new TestNode("inner");
        child1.addChildren(inner);
        // child1 has one child (inner). Unwrap child1.
        Node first = child1.unwrap();
        assertSame(inner, first);
        // inner is now a direct child of original parent, at child1's previous index (0)
        assertSame(parent, inner.parent());
        assertEquals(2, parent.childNodeSize()); // child2 + inner
        assertFalse(parent.ensureChildNodes().contains(child1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnwrapRootThrows() {
        TestNode root = new TestNode("root");
        root.unwrap(); // no parent
    }

    // -----------------------------------------------------------------------
    // 12. replaceWith(Node)
    // -----------------------------------------------------------------------
    @Test
    public void testReplaceWith() {
        TestNode repl = new TestNode("repl");
        child1.replaceWith(repl);
        assertSame(repl, parent.childNode(0));
        assertNull(child1.parent()); // old node detached
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReplaceWithNullThrows() {
        child1.replaceWith(null);
    }

    // -----------------------------------------------------------------------
    // 13. equals() – identity test
    // -----------------------------------------------------------------------
    @Test
    public void testEqualsIdentity() {
        assertTrue(child1.equals(child1));
        assertFalse(child1.equals(new TestNode("child1")));
    }

    // -----------------------------------------------------------------------
    // 14. hasSameValue(Object)
    // -----------------------------------------------------------------------
    @Test
    public void testHasSameValue() {
        TestNode a = new TestNode("a");
        TestNode b = new TestNode("a");
        a.attr("x", "1");
        b.attr("x", "1");
        assertTrue(a.hasSameValue(b));

        b.attr("x", "2");
        assertFalse(a.hasSameValue(b));
    }

    // -----------------------------------------------------------------------
    // 15. clone() and shallowClone()
    // -----------------------------------------------------------------------
    @Test
    public void testDeepCloneIndependence() {
        TestNode original = new TestNode("orig");
        original.attr("k", "v");
        original.addChildren(new TestNode("c1"), new TestNode("c2"));
        Node deep = original.clone();

        // deep clone should have same html
        assertEquals(original.outerHtml(), deep.outerHtml());

        // modify deep clone – original unchanged
        ((TestNode) deep).attr("k", "new");
        assertEquals("v", original.attr("k"));
    }

    @Test
    public void testShallowCloneNoChildren() {
        TestNode original = new TestNode("orig");
        original.addChildren(new TestNode("c"));
        Node shallow = original.shallowClone();

        assertEquals(0, ((TestNode) shallow).childNodeSize()); // no children
        assertEquals(original.attr("id"), shallow.attr("id"));
    }

    // -----------------------------------------------------------------------
    // 16. traverse(NodeVisitor) and filter(NodeFilter) – basic contract
    // -----------------------------------------------------------------------
    @Test
    public void testTraverseCallsVisitor() {
        final List<String> visited = new ArrayList<>();
        NodeVisitor visitor = new NodeVisitor() {
            @Override public void head(Node node, int depth) { visited.add("head:" + node.nodeName()); }
            @Override public void tail(Node node, int depth) { visited.add("tail:" + node.nodeName()); }
        };
        parent.traverse(visitor);
        // Expected order: parent head, child1 head, child1 tail, child2 head, child2 tail, parent tail
        List<String> expected = Arrays.asList(
                "head:parent", "head:child1", "tail:child1",
                "head:child2", "tail:child2", "tail:parent");
        assertEquals(expected, visited);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTraverseNullVisitorThrows() {
        parent.traverse((NodeVisitor) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFilterNullFilterThrows() {
        parent.filter(null);
    }

    // -----------------------------------------------------------------------
    // 17. outerHtml / html(Appendable) – sanity check
    // -----------------------------------------------------------------------
    @Test
    public void testOuterHtmlProducesExpectedString() {
        String html = parent.outerHtml();
        assertTrue(html.contains("<parent>"));
        assertTrue(html.contains("</parent>"));
    }

    @Test
    public void testHtmlAppendableReturnsSameInstance() {
        StringBuilder sb = new StringBuilder();
        StringBuilder ret = parent.html(sb);
        assertSame(sb, ret);
        assertTrue(sb.length() > 0);
    }

    // -----------------------------------------------------------------------
    // 18. ownerDocument() – returns null for non‑document trees
    // -----------------------------------------------------------------------
    @Test
    public void testOwnerDocumentIsNullWhenNoDocument() {
        assertNull(parent.ownerDocument());
    }

    // -----------------------------------------------------------------------
    // 19. root() – returns self when no parent
    // -----------------------------------------------------------------------
    @Test
    public void testRootSelfWhenNoParent() {
        TestNode lone = new TestNode("lone");
        assertSame(lone, lone.root());
    }

    // -----------------------------------------------------------------------
    // 20. remove()
    // -----------------------------------------------------------------------
    @Test
    public void testRemoveDetachesFromParent() {
        child1.remove();
        assertNull(child1.parentNode());
        assertEquals(1, parent.childNodeSize());
        assertSame(child2, parent.childNode(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRootThrows() {
        TestNode root = new TestNode("root");
        root.remove(); // no parent
    }
}
```
###Test END##