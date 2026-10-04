package org.jsoup.nodes;

import org.jsoup.parser.Parser;
import org.jsoup.select.NodeVisitor;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for the abstract {@link Node} class.
 *
 * Because {@code Node} is abstract we provide a minimal concrete implementation
 * {@code TestNode} that implements the required abstract methods. The implementation
 * is deliberately simple – it only stores a list of children, a map of attributes
 * and a {@code baseUri} string – so that the behaviour of the concrete methods in
 * {@code Node} can be exercised.
 *
 * The test suite covers:
 * <ul>
 *   <li>Basic attribute handling (attr, hasAttr, removeAttr, clearAttributes)</li>
 *   <li>Base‑uri handling and {@code absUrl}</li>
 *   <li>Child node access and index bounds</li>
 *   <li>Sibling navigation</li>
 *   <li>Node replacement and cloning</li>
 *   <li>Exception / edge‑case handling (null arguments, empty strings, out‑of‑bounds)</li>
 * </ul>
 */
public class NodeTest {

    /** Simple concrete subclass of {@link Node} used only for testing. */
    private static class TestNode extends Node {
        private final Attributes attrs = new Attributes();
        private final List<Node> children = new ArrayList<>();
        private String baseUri = "";

        TestNode() {
            // no‑arg constructor – parentNode and siblingIndex are managed by Node
        }

        TestNode(String baseUri) {
            this.baseUri = baseUri;
        }

        @Override
        public String nodeName() {
            return "testnode";
        }

        @Override
        protected boolean hasAttributes() {
            return !attrs.isEmpty();
        }

        @Override
        public Attributes attributes() {
            return attrs;
        }

        @Override
        public String baseUri() {
            return baseUri;
        }

        @Override
        protected void doSetBaseUri(String baseUri) {
            this.baseUri = baseUri;
        }

        @Override
        protected List<Node> ensureChildNodes() {
            return children;
        }

        @Override
        public int childNodeSize() {
            return children.size();
        }

        @Override
        void outerHtmlHead(Appendable accum, int depth, Document.OutputSettings out) throws IOException {
            accum.append("<" + nodeName() + ">");
        }

        @Override
        void outerHtmlTail(Appendable accum, int depth, Document.OutputSettings out) throws IOException {
            accum.append("</" + nodeName() + ">");
        }

        /** Helper to add a child node (used in tests). */
        void addChild(TestNode child) {
            child.setParentNode(this);
            child.setSiblingIndex(children.size());
            children.add(child);
        }
    }

    private TestNode root;
    private TestNode child1;
    private TestNode child2;
    private TestNode grandChild;

    @Before
    public void setUp() {
        root = new TestNode("http://example.com/");
        child1 = new TestNode();
        child2 = new TestNode();
        grandChild = new TestNode();

        root.addChild(child1);
        root.addChild(child2);
        child1.addChild(grandChild);
    }

    // ----------------------------------------------------------------------
    // 1. Public method signatures (for reference)
    // ----------------------------------------------------------------------
    // public abstract String nodeName()
    // protected abstract boolean hasAttributes()
    // public boolean hasParent()
    // public String attr(String attributeKey)
    // public abstract Attributes attributes()
    // public Node attr(String attributeKey, String attributeValue)
    // public boolean hasAttr(String attributeKey)
    // public Node removeAttr(String attributeKey)
    // public Node clearAttributes()
    // public abstract String baseUri()
    // protected abstract void doSetBaseUri(String baseUri)
    // public void setBaseUri(String baseUri)
    // public String absUrl(String attributeKey)
    // protected abstract List<Node> ensureChildNodes()
    // public Node childNode(int index)
    // public List<Node> childNodes()
    // public List<Node> childNodesCopy()
    // public abstract int childNodeSize()
    // protected Node[] childNodesAsArray()
    // public Node parent()
    // public final Node parentNode()
    // public Node root()
    // public Document ownerDocument()
    // public void remove()
    // public Node before(String html)
    // public Node before(Node node)
    // public Node after(String html)
    // public Node after(Node node)
    // public Node wrap(String html)
    // public Node unwrap()
    // public void replaceWith(Node in)
    // public List<Node> siblingNodes()
    // public Node nextSibling()
    // public Node previousSibling()
    // public int siblingIndex()
    // public Node traverse(NodeVisitor nodeVisitor)
    // public Node filter(NodeFilter nodeFilter)
    // public String outerHtml()
    // protected void outerHtml(Appendable accum)
    // Document.OutputSettings getOutputSettings()
    // abstract void outerHtmlHead(Appendable accum, int depth, Document.OutputSettings out) throws IOException
    // abstract void outerHtmlTail(Appendable accum, int depth, Document.OutputSettings out) throws IOException
    // public <T extends Appendable> T html(T appendable)
    // public boolean equals(Object o)
    // public boolean hasSameValue(Object o)
    // public Node clone()
    // public Node shallowClone()
    // protected Node doClone(Node parent)

    // ----------------------------------------------------------------------
    // 2. Basic functionality tests
    // ----------------------------------------------------------------------

    @Test
    public void testAttributeGetSetHasRemoveClear() {
        // set attribute
        root.attr("data-key", "value");
        assertEquals("value", root.attr("data-key"));
        assertTrue(root.hasAttr("data-key"));

        // case‑insensitive get
        assertEquals("value", root.attr("DATA-KEY"));
        assertTrue(root.hasAttr("DATA-KEY"));

        // remove attribute
        root.removeAttr("data-key");
        assertEquals("", root.attr("data-key"));
        assertFalse(root.hasAttr("data-key"));

        // clear attributes (no‑op when empty)
        root.attr("a", "1");
        root.attr("b", "2");
        root.clearAttributes();
        assertFalse(root.hasAttr("a"));
        assertFalse(root.hasAttr("b"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttrNullKeyThrows() {
        root.attr((String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHasAttrNullKeyThrows() {
        root.hasAttr(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveAttrNullKeyThrows() {
        root.removeAttr(null);
    }

    @Test
    public void testBaseUriPropagation() {
        // initial base uri of root is set in setUp()
        assertEquals("http://example.com/", root.baseUri());

        // child should inherit when setBaseUri is called on root
        root.setBaseUri("https://new.example/");
        assertEquals("https://new.example/", root.baseUri());
        assertEquals("https://new.example/", child1.baseUri());
        assertEquals("https://new.example/", child2.baseUri());
        assertEquals("https://new.example/", grandChild.baseUri());
    }

    @Test
    public void testAbsUrlResolution() {
        // attribute that does not exist → empty string
        assertEquals("", root.absUrl("missing"));

        // set a relative attribute and resolve it
        child1.attr("href", "page.html");
        // child1 inherits base uri from root after calling setBaseUri
        root.setBaseUri("http://example.com/");
        assertEquals("http://example.com/page.html", child1.absUrl("href"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAbsUrlEmptyKeyThrows() {
        root.absUrl("");
    }

    @Test
    public void testChildNodeAccessAndBounds() {
        // valid index
        assertSame(child1, root.childNode(0));
        assertSame(child2, root.childNode(1));

        // out of bounds → IndexOutOfBoundsException
        try {
            root.childNode(2);
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException ignored) {
        }
    }

    @Test
    public void testChildNodesCopyIsDeep() {
        List<Node> copy = root.childNodesCopy();
        assertEquals(2, copy.size());
        // the copies should be equal in value but not the same instance
        assertNotSame(child1, copy.get(0));
        assertTrue(((Node) copy.get(0)).hasSameValue(child1));
    }

    @Test
    public void testSiblingNavigation() {
        // sibling list of child1 should contain only child2
        List<Node> siblings = child1.siblingNodes();
        assertEquals(1, siblings.size());
        assertSame(child2, siblings.get(0));

        // next / previous sibling
        assertSame(child2, child1.nextSibling());
        assertNull(child2.nextSibling());

        assertNull(child1.previousSibling());
        assertSame(child1, child2.previousSibling());
    }

    @Test
    public void testSiblingNavigationWhenOrphan() {
        TestNode orphan = new TestNode();
        assertTrue(orphan.siblingNodes().isEmpty());
        assertNull(orphan.nextSibling());
        assertNull(orphan.previousSibling());
    }

    @Test
    public void testReplaceWith() {
        TestNode replacement = new TestNode();
        // replace child1 with replacement
        child1.replaceWith(replacement);
        // root's children should now be [replacement, child2]
        assertSame(replacement, root.childNode(0));
        assertSame(child2, root.childNode(1));
        // replacement's parent should be root
        assertSame(root, replacement.parent());
        // old node should have no parent
        assertNull(child1.parent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReplaceWithNullThrows() {
        child1.replaceWith(null);
    }

    @Test
    public void testCloneCreatesDeepCopy() {
        // add an attribute to child2 to verify deep copy
        child2.attr("key", "val");
        Node clonedRoot = root.clone();

        // cloned root must not be the same instance
        assertNotSame(root, clonedRoot);
        // structure should be identical
        assertEquals(2, clonedRoot.childNodeSize());

        Node clonedChild1 = clonedRoot.childNode(0);
        Node clonedChild2 = clonedRoot.childNode(1);

        // clonedChild2 should have the attribute
        assertEquals("val", clonedChild2.attr("key"));
        // modifying the clone must not affect original
        clonedChild2.attr("key", "new");
        assertEquals("val", child2.attr("key"));
    }

    @Test
    public void testShallowCloneDoesNotCopyChildren() {
        Node shallow = root.shallowClone();
        assertNotSame(root, shallow);
        // childNodeSize of shallow clone should be zero (no children copied)
        assertEquals(0, shallow.childNodeSize());
    }

    @Test
    public void testOuterHtmlProducesExpectedString() {
        // Our TestNode outerHtml implementation simply wraps the node name.
        String html = child1.outerHtml();
        assertEquals("<testnode></testnode>", html);
    }

    @Test
    public void testTraverseCallsVisitor() {
        final List<String> visited = new ArrayList<>();
        NodeVisitor visitor = new NodeVisitor() {
            @Override public void head(Node node, int depth) {
                visited.add("head:" + node.nodeName() + ":" + depth);
            }
            @Override public void tail(Node node, int depth) {
                visited.add("tail:" + node.nodeName() + ":" + depth);
            }
        };
        root.traverse(visitor);
        // Expect depth‑first order: root → child1 → grandChild → child2
        List<String> expected = Arrays.asList(
                "head:testnode:0",
                "head:testnode:1",
                "head:testnode:2",
                "tail:testnode:2",
                "tail:testnode:1",
                "head:testnode:1",
                "tail:testnode:1",
                "tail:testnode:0"
        );
        assertEquals(expected, visited);
    }

    // ----------------------------------------------------------------------
    // 3. Edge‑case / exception tests for methods that validate arguments
    // ----------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testSetBaseUriNullThrows() {
        root.setBaseUri(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBeforeNullNodeThrows() {
        root.before((Node) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAfterNullNodeThrows() {
        root.after((Node) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrapEmptyHtmlThrows() {
        root.wrap("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddSiblingHtmlNullHtmlThrows() {
        // invoke private method via reflection to ensure validation works
        try {
            java.lang.reflect.Method m = Node.class.getDeclaredMethod("addSiblingHtml", int.class, String.class);
            m.setAccessible(true);
            m.invoke(root, 0, null);
            fail("Expected IllegalArgumentException");
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNodeVisitorNullThrows() {
        root.traverse(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNodeFilterNullThrows() {
        root.filter(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReplaceChildWrongParentThrows() {
        // child1's parent is root; attempt to replace child2 with child1 using child1 as the parent (illegal)
        try {
            child1.replaceChild(child2, child1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
    }
}
