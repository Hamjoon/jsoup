package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.parser.Parser;
import org.jsoup.select.NodeFilter;
import org.jsoup.select.NodeVisitor;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

public class NodeTest {

    /*** Helper concrete subclass of abstract Node for testing protected methods ***/
    private static class DummyNode extends Node {
        private final List<Node> children = new ArrayList<>();
        private String baseUri = "";
        private final Attributes attrs = new Attributes();

        DummyNode() {}

        DummyNode(String baseUri) { this.baseUri = baseUri; }

        @Override public String nodeName() { return "#dummy"; }

        @Override protected boolean hasAttributes() { return !attrs.isEmpty(); }

        @Override public Attributes attributes() { return attrs; }

        @Override public String baseUri() { return baseUri; }

        @Override protected void doSetBaseUri(String baseUri) { this.baseUri = baseUri; }

        @Override protected List<Node> ensureChildNodes() { return children; }

        @Override public int childNodeSize() { return children.size(); }

        @Override void outerHtmlHead(Appendable accum, int depth, Document.OutputSettings out) throws IOException {
            accum.append("<dummy>");
        }

        @Override void outerHtmlTail(Appendable accum, int depth, Document.OutputSettings out) throws IOException {
            accum.append("</dummy>");
        }

        /** expose protected indent for testing */
        public String callIndent(int depth) throws IOException {
            StringBuilder sb = new StringBuilder();
            super.indent(sb, depth, getOutputSettings());
            return sb.toString();
        }
    }

    /*** 1. attr(), hasAttr(), removeAttr(), clearAttributes() ***/
    @Test
    public void testAttributeManipulation() {
        Element el = new Element("a");
        // set attribute and retrieve (case‑insensitive)
        el.attr("Href", "http://example.com");
        assertEquals("http://example.com", el.attr("href"));
        assertTrue(el.hasAttr("HREF"));
        // remove attribute
        el.removeAttr("HREF");
        assertEquals("", el.attr("href"));
        assertFalse(el.hasAttr("href"));
        // clear all attributes
        el.attr("id", "test");
        el.attr("class", "cls");
        el.clearAttributes();
        assertFalse(el.hasAttr("id"));
        assertFalse(el.hasAttr("class"));
    }

    /*** 2. absUrl() – relative and absolute handling ***/
    @Test
    public void testAbsUrlResolution() {
        Document doc = Jsoup.parse("<a href=\"/path/page.html\">link</a>", "http://example.com/base/");
        Element a = doc.selectFirst("a");
        // relative URL should be resolved against base URI
        assertEquals("http://example.com/path/page.html", a.absUrl("href"));
        // absolute URL stays unchanged
        a.attr("href", "https://other.com/abs");
        assertEquals("https://other.com/abs", a.absUrl("href"));
        // missing attribute yields empty string
        assertEquals("", a.absUrl("src"));
    }

    /*** 3. childNode, childNodes, childNodesCopy, childNodeSize ***/
    @Test
    public void testChildAccessors() {
        Element parent = new Element("div");
        Element child1 = new Element("p").text("One");
        Element child2 = new Element("p").text("Two");
        parent.appendChild(child1);
        parent.appendChild(child2);

        assertEquals(2, parent.childNodeSize());
        assertSame(child1, parent.childNode(0));
        assertSame(child2, parent.childNode(1));

        List<Node> unmod = parent.childNodes();
        assertEquals(2, unmod.size());
        try {
            unmod.add(new Element("span"));
            fail("childNodes() should be unmodifiable");
        } catch (UnsupportedOperationException ignored) { }

        // copy should be deep – modifying copy does not affect original
        List<Node> copy = parent.childNodesCopy();
        ((Element) copy.get(0)).attr("id", "copy");
        assertEquals("", child1.attr("id"));
    }

    /*** 4. sibling navigation ***/
    @Test
    public void testSiblingNavigation() {
        Document doc = Jsoup.parse("<ul><li id='a'>A</li><li id='b'>B</li><li id='c'>C</li></ul>");
        Element a = doc.selectFirst("li#a");
        Element b = doc.selectFirst("li#b");
        Element c = doc.selectFirst("li#c");

        // sibling index
        assertEquals(0, a.siblingIndex());
        assertEquals(1, b.siblingIndex());
        assertEquals(2, c.siblingIndex());

        // next / previous sibling
        assertSame(b, a.nextSibling());
        assertNull(a.previousSibling());
        assertSame(c, b.nextSibling());
        assertSame(a, b.previousSibling());
        assertNull(c.nextSibling());

        // sibling list (excludes self)
        List<Node> siblingsOfB = b.siblingNodes();
        assertEquals(2, siblingsOfB.size());
        assertTrue(siblingsOfB.contains(a));
        assertTrue(siblingsOfB.contains(c));
    }

    /*** 5. root() and ownerDocument() ***/
    @Test
    public void testRootAndOwnerDocument() {
        Document doc = Jsoup.parse("<div><p><span>text</span></p></div>");
        Element span = doc.selectFirst("span");
        assertSame(doc, span.root());
        assertSame(doc, span.ownerDocument());
        // root of the document is itself
        assertSame(doc, doc.root());
        assertNull(doc.ownerDocument()); // Document has no owning document
    }

    /*** 6. remove() ***/
    @Test
    public void testRemoveNode() {
        Document doc = Jsoup.parse("<div><p>keep</p><p>remove</p></div>");
        Element removeMe = doc.select("p").get(1);
        removeMe.remove();
        assertEquals(1, doc.select("p").size());
        assertEquals("keep", doc.selectFirst("p").text());
    }

    /*** 7. before(String) and after(String) ***/
    @Test
    public void testInsertHtmlSiblings() {
        Document doc = Jsoup.parse("<div><p id='mid'>mid</p></div>");
        Element mid = doc.selectFirst("p#mid");
        mid.before("<span id='before'>B</span>");
        mid.after("<span id='after'>A</span>");

        List<Element> spans = doc.select("span");
        assertEquals(2, spans.size());
        assertEquals("before", spans.get(0).id());
        assertEquals("after", spans.get(1).id());

        // verify order: before, mid, after
        List<Element> children = doc.selectFirst("div").children();
        assertEquals("before", children.get(0).id());
        assertEquals("mid", children.get(1).id());
        assertEquals("after", children.get(2).id());
    }

    /*** 8. before(Node) and after(Node) ***/
    @Test
    public void testInsertNodeSiblings() {
        Document doc = Jsoup.parse("<ul><li id='first'>1</li><li id='second'>2</li></ul>");
        Element first = doc.selectFirst("li#first");
        Element second = doc.selectFirst("li#second");
        Element newNode = new Element("li").attr("id", "new").text("new");

        first.after(newNode); // insert after first, before second
        assertEquals("new", first.nextSibling().text());
        assertSame(second, first.nextSibling().nextSibling());

        Element another = new Element("li").attr("id", "another").text("another");
        second.before(another);
        assertEquals("another", second.previousSibling().text());
    }

    /*** 9. wrap(String) ***/
    @Test
    public void testWrap() {
        Document doc = Jsoup.parse("<p>hello <b>world</b></p>");
        Element b = doc.selectFirst("b");
        b.wrap("<div class='wrapper'><section></section></div>");

        Element wrapper = doc.selectFirst("div.wrapper");
        assertNotNull(wrapper);
        // the deepest element (section) should now contain the <b>
        Element deepest = wrapper.selectFirst("section");
        assertNotNull(deepest);
        assertEquals("b", deepest.child(0).nodeName());

        // original parent (<p>) should now have the wrapper as its child
        assertEquals(wrapper, doc.selectFirst("p").child(0));
    }

    /*** 10. unwrap() ***/
    @Test
    public void testUnwrap() {
        Document doc = Jsoup.parse("<div>One <span>Two <b>Three</b></span> Four</div>");
        Element span = doc.selectFirst("span");
        Node firstChild = span.unwrap(); // should return the <b> node
        assertEquals("b", firstChild.nodeName());

        // After unwrap, the <span> is gone and its children are now direct children of <div>
        Element div = doc.selectFirst("div");
        List<Node> children = div.childNodes();
        assertEquals(4, children.size()); // text "One ", text " Two ", <b>, text " Four"
        assertEquals("b", children.get(2).nodeName());
    }

    /*** 11. replaceWith(Node) ***/
    @Test
    public void testReplaceWith() {
        Document doc = Jsoup.parse("<ul><li id='old'>old</li></ul>");
        Element old = doc.selectFirst("li#old");
        Element replacement = new Element("li").attr("id", "new").text("new");
        old.replaceWith(replacement);
        assertNull(doc.selectFirst("li#old"));
        assertNotNull(doc.selectFirst("li#new"));
        assertEquals("new", doc.selectFirst("li#new").text());
    }

    /*** 12. equals() – identity test ***/
    @Test
    public void testIdentityEquals() {
        Element e1 = new Element("p");
        Element e2 = new Element("p");
        assertTrue(e1.equals(e1));
        assertFalse(e1.equals(e2));
        assertFalse(e1.equals(null));
    }

    /*** 13. hasSameValue() – structural equality ***/
    @Test
    public void testHasSameValue() {
        Element a = Jsoup.parse("<div><p>Text</p></div>").selectFirst("div");
        Element b = Jsoup.parse("<div><p>Text</p></div>").selectFirst("div");
        assertTrue(a.hasSameValue(b));

        b.appendElement("span");
        assertFalse(a.hasSameValue(b));
    }

    /*** 14. deep clone – modifications do not affect original ***/
    @Test
    public void testDeepCloneIndependence() {
        Document doc = Jsoup.parse("<div><p id='p'>Hello</p></div>");
        Element div = doc.selectFirst("div");
        Element clone = (Element) div.clone();

        // modify clone
        clone.selectFirst("p").attr("id", "changed");
        clone.appendElement("span").text("extra");

        // original stays untouched
        assertEquals("p", div.selectFirst("p").id());
        assertEquals(1, div.select("span").size());
    }

    /*** 15. shallowClone – children are not cloned ***/
    @Test
    public void testShallowClone() {
        Element original = new Element("section");
        original.appendElement("p").text("para");
        Element shallow = (Element) original.shallowClone();

        assertEquals(0, shallow.childNodeSize());
        assertEquals("section", shallow.nodeName());
    }

    /*** 16. html(Appendable) and toString() ***/
    @Test
    public void testHtmlAppendableAndToString() {
        Element el = Jsoup.parse("<div><p>Hi</p></div>").selectFirst("div");
        StringBuilder sb = new StringBuilder();
        el.html(sb);
        assertEquals(el.outerHtml(), sb.toString());
        assertEquals(el.outerHtml(), el.toString());
    }

    /*** 17. indent (protected) via subclass ***/
    @Test
    public void testIndentFormatting() throws IOException {
        DummyNode node = new DummyNode();
        String indented = node.callIndent(3); // depth 3
        // default indent amount is 1 space per depth in Jsoup's OutputSettings
        assertTrue(indented.startsWith("\n"));
        assertEquals(3, indented.trim().length()); // three spaces after newline
    }

    /*** 18. traverse(NodeVisitor) and filter(NodeFilter) ***/
    @Test
    public void testTraverseAndFilter() {
        Document doc = Jsoup.parse("<div><p>One</p><p>Two</p></div>");
        final List<String> visited = new ArrayList<>();

        // Visitor that records node names
        NodeVisitor visitor = new NodeVisitor() {
            public void head(Node node, int depth) { visited.add("head:" + node.nodeName()); }
            public void tail(Node node, int depth) { visited.add("tail:" + node.nodeName()); }
        };
        doc.traverse(visitor);
        assertTrue(visited.contains("head:div"));
        assertTrue(visited.contains("head:p"));
        assertTrue(visited.contains("tail:p"));
        assertTrue(visited.contains("tail:div"));

        // Filter that removes all <p> elements
        NodeFilter filter = new NodeFilter() {
            public FilterResult head(Node node, int depth) {
                return node.nodeName().equals("p") ? FilterResult.STOP : FilterResult.CONTINUE;
            }
            public FilterResult tail(Node node, int depth) { return FilterResult.CONTINUE; }
        };
        doc.filter(filter);
        assertEquals(1, doc.select("p").size()); // one <p> stopped, the other removed by STOP
    }

    /*** 19. setBaseUri – propagation to descendants ***/
    @Test
    public void testSetBaseUriPropagation() {
        Document doc = Jsoup.parse("<div><a href=\"/page\">link</a></div>", "http://example.com");
        Element a = doc.selectFirst("a");
        // Change base URI on the document; all descendants should reflect it
        doc.setBaseUri("http://newbase.org");
        assertEquals("http://newbase.org/page", a.absUrl("href"));
    }

    /*** 20. Validation of null arguments – expect IllegalArgumentException ***/
    @Test(expected = IllegalArgumentException.class)
    public void testValidateNotNullAttrKey() {
        Element el = new Element("p");
        el.attr(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateNotNullBeforeNode() {
        Element el = new Element("p");
        el.before((Node) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateNotNullAfterNode() {
        Element el = new Element("p");
        el.after((Node) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateNotNullWrap() {
        Element el = new Element("p");
        el.wrap("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateNotNullSetBaseUri() {
        DummyNode node = new DummyNode();
        node.setBaseUri(null);
    }
}
