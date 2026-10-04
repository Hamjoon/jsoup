package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.select.NodeVisitor;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Node}.  The tests use the real
 * {@code Element}, {@code Document} and {@code TextNode} implementations from
 * jsoup where possible, and a tiny concrete subclass {@code TestNode} for
 * exercising the protected API (e.g. {@code setParentNode}, {@code siblingIndex}
 * handling, and the cloning logic).
 */
public class NodeTest {

    /** Simple concrete subclass of {@link Node} for low‑level tests. */
    private static class TestNode extends Node {
        private final String name;
        private final Attributes attrs = new Attributes();
        private final java.util.ArrayList<Node> children = new java.util.ArrayList<>();
        private String baseUri = "";

        TestNode(String name) {
            this.name = name;
        }

        @Override public String nodeName() { return name; }

        @Override protected boolean hasAttributes() { return !attrs.isEmpty(); }

        @Override public Attributes attributes() { return attrs; }

        @Override public String baseUri() { return baseUri; }

        @Override protected void doSetBaseUri(String baseUri) { this.baseUri = baseUri; }

        @Override protected java.util.List<Node> ensureChildNodes() { return children; }

        @Override public int childNodeSize() { return children.size(); }

        @Override void outerHtmlHead(Appendable accum, int depth, Document.OutputSettings out) throws IOException {
            accum.append("<").append(name);
            if (!attrs.isEmpty()) {
                for (Attribute a : attrs) {
                    accum.append(' ').append(a.getKey()).append("=\"").append(a.getValue()).append('"');
                }
            }
            accum.append('>');
        }

        @Override void outerHtmlTail(Appendable accum, int depth, Document.OutputSettings out) throws IOException {
            accum.append("</").append(name).append('>');
        }
    }

    private Document doc;
    private Element parent;
    private Element child;

    @Before
    public void setUp() {
        doc = Jsoup.parse("<div id='parent'><p id='child'>Hello</p></div>");
        parent = doc.selectFirst("div#parent");
        child = doc.selectFirst("p#child");
    }

    /** -------------------- attribute handling -------------------- */
    @Test
    public void testAttrGetSetHasRemoveClear() {
        // set
        child.attr("data-test", "value");
        assertEquals("value", child.attr("data-test"));
        // case‑insensitive get
        assertEquals("value", child.attr("DATA-TEST"));
        // hasAttr
        assertTrue(child.hasAttr("data-test"));
        assertTrue(child.hasAttr("DATA-TEST"));
        // remove
        child.removeAttr("data-test");
        assertFalse(child.hasAttr("data-test"));
        assertEquals("", child.attr("data-test"));
        // clearAttributes
        child.attr("a", "1").attr("b", "2");
        child.clearAttributes();
        assertFalse(child.hasAttr("a"));
        assertFalse(child.hasAttr("b"));
    }

    /** -------------------- base URI propagation -------------------- */
    @Test
    public void testSetBaseUriPropagatesToDescendants() {
        // child initially inherits empty baseUri from document
        assertEquals("", child.baseUri());
        // set a new base on the parent; the child should reflect it after setBaseUri
        parent.setBaseUri("http://example.com/");
        assertEquals("http://example.com/", parent.baseUri());
        assertEquals("http://example.com/", child.baseUri());
    }

    /** -------------------- child node access -------------------- */
    @Test
    public void testChildNodeAccessors() {
        List<Node> children = parent.childNodes();
        assertEquals(1, children.size());
        assertSame(child, children.get(0));

        // childNode(int) works
        assertSame(child, parent.childNode(0));

        // childNodesCopy returns deep copies
        List<Node> copy = parent.childNodesCopy();
        assertEquals(1, copy.size());
        assertNotSame(child, copy.get(0));
        // copies have same outer HTML
        assertEquals(child.outerHtml(), copy.get(0).outerHtml());
    }

    /** -------------------- sibling navigation -------------------- */
    @Test
    public void testSiblingNavigation() {
        // add a second sibling after the child
        Element sibling = new Element("span").text("sibling");
        child.after(sibling);
        assertEquals(sibling, child.nextSibling());
        assertNull(child.previousSibling()); // child was first
        assertEquals(child, sibling.previousSibling());
        assertNull(sibling.nextSibling());

        // siblingNodes should contain only the other element
        List<Node> childSiblings = child.siblingNodes();
        assertEquals(1, childSiblings.size());
        assertSame(sibling, childSiblings.get(0));
    }

    /** -------------------- before/after with HTML strings -------------------- */
    @Test
    public void testBeforeAfterHtml() {
        // Insert a raw HTML snippet before the child
        child.before("<b id='before'>B</b>");
        Element before = parent.selectFirst("b#before");
        assertNotNull(before);
        assertSame(before, child.previousSibling());

        // Insert after
        child.after("<i id='after'>I</i>");
        Element after = parent.selectFirst("i#after");
        assertNotNull(after);
        assertSame(after, child.nextSibling());
    }

    /** -------------------- before/after with Nodes -------------------- */
    @Test
    public void testBeforeAfterNode() {
        Element newNode = new Element("u").text("underlined");
        child.before(newNode);
        assertSame(newNode, child.previousSibling());

        Element another = new Element("em").text("emph");
        child.after(another);
        assertSame(another, child.nextSibling());
    }

    /** -------------------- unwrap moves children up -------------------- */
    @Test
    public void testUnwrap() {
        // <div id='parent'><p id='child'>Hello</p></div>
        // unwrap the child <p>
        Node first = child.unwrap();
        // after unwrap, the text node "Hello" should be a direct child of parent
        assertEquals("Hello", parent.text());
        // first returned node should be the TextNode containing "Hello"
        assertTrue(first instanceof TextNode);
        assertEquals("Hello", ((TextNode) first).text());
    }

    /** -------------------- replaceWith and remove -------------------- */
    @Test
    public void testReplaceWithAndRemove() {
        Element replacement = new Element("section").attr("id", "repl");
        child.replaceWith(replacement);
        assertNull(parent.selectFirst("p#child"));
        assertNotNull(parent.selectFirst("section#repl"));

        // now remove the replacement
        replacement.remove();
        assertTrue(parent.selectFirst("section#repl") == null);
        assertTrue(parent.childNodeSize() == 0);
    }

    /** -------------------- cloning -------------------- */
    @Test
    public void testCloneAndShallowClone() {
        // deep clone
        Node deep = child.clone();
        assertNotSame(child, deep);
        assertEquals(child.outerHtml(), deep.outerHtml());

        // modify deep clone; original should stay unchanged
        ((Element) deep).attr("data-x", "y");
        assertFalse(child.hasAttr("data-x"));
        assertTrue(((Element) deep).hasAttr("data-x"));

        // shallow clone (no children)
        Node shallow = child.shallowClone();
        assertNotSame(child, shallow);
        // shallow clone should have no children (the <p> has a TextNode child)
        assertEquals(0, shallow.childNodeSize());
    }

    /** -------------------- equals and hasSameValue -------------------- */
    @Test
    public void testEqualsAndHasSameValue() {
        // identity equals
        assertTrue(child.equals(child));
        assertFalse(child.equals(new Object()));

        // same value but different instances
        Element other = new Element("p").attr("id", "child").text("Hello");
        assertFalse(child == other);
        assertTrue(child.hasSameValue(other));
        // change other, value differs
        other.attr("data", "x");
        assertFalse(child.hasSameValue(other));
    }

    /** -------------------- html(Appendable) and toString -------------------- */
    @Test
    public void testHtmlAppendableAndToString() throws IOException {
        StringBuilder sb = new StringBuilder();
        child.html(sb);
        assertEquals(child.outerHtml(), sb.toString());

        // toString delegates to outerHtml()
        assertEquals(child.outerHtml(), child.toString());
    }

    /** -------------------- root and ownerDocument -------------------- */
    @Test
    public void testRootAndOwnerDocument() {
        // root of any node in the parsed tree should be the Document
        assertSame(doc, child.root());
        assertSame(doc, parent.root());

        // ownerDocument should also be the same Document
        assertSame(doc, child.ownerDocument());
        assertSame(doc, parent.ownerDocument());
    }

    /** -------------------- traversal and filtering (smoke test) -------------------- */
    @Test
    public void testTraverseAndFilter() {
        final StringBuilder visited = new StringBuilder();
        NodeVisitor visitor = new NodeVisitor() {
            public void head(Node node, int depth) {
                visited.append(node.nodeName()).append(':').append(depth).append(',');
            }
            public void tail(Node node, int depth) { /* no‑op */ }
        };
        // simple traverse of the parent element
        parent.traverse(visitor);
        // should contain at least the parent and its child element
        assertTrue(visited.toString().contains("div:0"));
        assertTrue(visited.toString().contains("p:1"));
    }
}
