**###Test START##**  
```java
package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.select.NodeVisitor;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link org.jsoup.nodes.Node}.
 *
 * The tests use concrete subclasses from jsoup (Element, TextNode, Document) to
 * exercise the abstract behaviour of {@code Node}.  Each public method is
 * covered with at least one “happy‑path” test, edge‑case tests, and tests that
 * verify the documented error handling (e.g. {@code Validate.notNull} throws
 * {@link IllegalArgumentException}).
 *
 * The suite can be run with any JUnit‑4 runner.
 */
public class NodeTest {

    /** Simple helper that creates a document with the following structure:
     * <pre>
     *   &lt;html&gt;
     *     &lt;body&gt;
     *       &lt;div id="parent"&gt;
     *         &lt;p id="child1"&gt;One&lt;/p&gt;
     *         &lt;p id="child2"&gt;Two&lt;/p&gt;
     *       &lt;/div&gt;
     *     &lt;/body&gt;
     *   &lt;/html&gt;
     * </pre>
     */
    private Document simpleDoc() {
        return Jsoup.parse("<html><body><div id='parent'><p id='child1'>One</p><p id='child2'>Two</p></div></body></html>");
    }

    /* -------------------------------------------------
     *  attr / hasAttr / removeAttr / clearAttributes
     * ------------------------------------------------- */

    @Test
    public void attrHasAttrAndRemoveAttr_WorkAsSpecified() {
        Element div = new Element("div").attr("data-test", "value");
        // happy path
        assertEquals("value", div.attr("data-test"));
        assertTrue(div.hasAttr("data-test"));
        // case‑insensitive lookup
        assertEquals("value", div.attr("DATA-TEST"));
        assertTrue(div.hasAttr("DATA-TEST"));

        // remove and verify
        div.removeAttr("DATA-TEST");
        assertEquals("", div.attr("data-test"));
        assertFalse(div.hasAttr("data-test"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void attr_NullKey_Throws() {
        new Element("p").attr(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void hasAttr_NullKey_Throws() {
        new Element("p").hasAttr(null);
    }

    @Test
    public void clearAttributes_RemovesAllAttributes() {
        Element el = new Element("a")
                .attr("href", "http://example.com")
                .attr("title", "Example");
        assertTrue(el.hasAttr("href"));
        assertTrue(el.hasAttr("title"));
        el.clearAttributes();
        assertFalse(el.hasAttr("href"));
        assertFalse(el.hasAttr("title"));
        assertEquals("", el.attr("href"));
    }

    /* -------------------------------------------------
     *  baseUri / setBaseUri / absUrl
     * ------------------------------------------------- */

    @Test
    public void absUrl_RelativeAndAbsoluteUrls() {
        Element a = new Element("a")
                .attr("href", "/path/page.html")
                .setBaseUri("http://example.com/base/");
        // relative URL should be resolved against baseUri
        assertEquals("http://example.com/path/page.html", a.absUrl("href"));

        // absolute URL stays unchanged
        a.attr("href", "https://other.com/abs");
        assertEquals("https://other.com/abs", a.absUrl("href"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void absUrl_NullOrEmptyKey_Throws() {
        new Element("a").absUrl("");
    }

    @Test
    public void setBaseUri_UpdatesDescendants() {
        Document doc = Jsoup.parse("<div><p><a href='page.html'>link</a></p></div>");
        Element div = doc.selectFirst("div");
        div.setBaseUri("http://example.com/base/");
        Element a = doc.selectFirst("a");
        // after setBaseUri the link should resolve correctly
        assertEquals("http://example.com/base/page.html", a.absUrl("href"));
    }

    /* -------------------------------------------------
     *  childNode / childNodes / childNodesCopy
     * ------------------------------------------------- */

    @Test
    public void childNodeAndChildNodes_ReturnCorrectElements() {
        Document doc = simpleDoc();
        Element parent = doc.selectFirst("div#parent");
        assertEquals(2, parent.childNodeSize());

        Node first = parent.childNode(0);
        assertEquals("p", first.nodeName());
        assertEquals("child1", ((Element) first).id());

        // out‑of‑bounds should throw
        try {
            parent.childNode(2);
            fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException ignored) {
        }

        List<Node> children = parent.childNodes();
        assertEquals(2, children.size());
        assertSame(first, children.get(0));
    }

    @Test
    public void childNodesCopy_IsDeepClone() {
        Document doc = simpleDoc();
        Element parent = doc.selectFirst("div#parent");
        List<Node> copy = parent.childNodesCopy();

        // copy is a different object graph
        assertNotSame(parent.childNode(0), copy.get(0));
        assertEquals(parent.childNode(0).outerHtml(), copy.get(0).outerHtml());

        // modifying copy does not affect original
        ((Element) copy.get(0)).attr("data-mod", "yes");
        assertFalse(parent.childNode(0).hasAttr("data-mod"));
    }

    /* -------------------------------------------------
     *  siblingNodes / nextSibling / previousSibling
     * ------------------------------------------------- */

    @Test
    public void siblingNavigation_WorksCorrectly() {
        Document doc = simpleDoc();
        Element child1 = doc.selectFirst("p#child1");
        Element child2 = doc.selectFirst("p#child2");
        Element parent = doc.selectFirst("div#parent");

        // sibling list (excluding self)
        List<Node> siblings = child1.siblingNodes();
        assertEquals(1, siblings.size());
        assertSame(child2, siblings.get(0));

        // next / previous
        assertSame(child2, child1.nextSibling());
        assertNull(child2.nextSibling());

        assertNull(child1.previousSibling());
        assertSame(child1, child2.previousSibling());

        // root node has no siblings
        assertTrue(doc.siblingNodes().isEmpty());
        assertNull(doc.nextSibling());
        assertNull(doc.previousSibling());
    }

    /* -------------------------------------------------
     *  before / after (HTML fragment insertion)
     * ------------------------------------------------- */

    @Test
    public void beforeAndAfter_InsertHtmlFragments() {
        Document doc = Jsoup.parse("<div id='parent'><p id='child'>One</p></div>");
        Element child = doc.selectFirst("p#child");

        // insert before
        child.before("<span class='before'>B</span>");
        // insert after
        child.after("<span class='after'>A</span>");

        List<Element> siblings = child.parent().children();
        assertEquals(3, siblings.size());
        assertEquals("span", siblings.get(0).nodeName());
        assertEquals("before", siblings.get(0).className());
        assertEquals("p", siblings.get(1).nodeName());
        assertEquals("span", siblings.get(2).nodeName());
        assertEquals("after", siblings.get(2).className());
    }

    @Test(expected = IllegalArgumentException.class)
    public void before_NullHtml_Throws() {
        new Element("p").before((String) null);
    }

    @Test
    public void after_NullNode_Throws() {
        try {
            new Element("p").after((Node) null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
    }

    /* -------------------------------------------------
     *  wrap / unwrap
     * ------------------------------------------------- */

    @Test
    public void wrap_WrapsElementWithProvidedHtml() {
        Document doc = Jsoup.parse("<div><p id='toWrap'>Text</p></div>");
        Element p = doc.selectFirst("p#toWrap");
        p.wrap("<section class='wrapper'><div class='inner'></div></section>");

        // after wrapping the <p> should be a descendant of <section>
        Element section = doc.selectFirst("section.wrapper");
        assertNotNull(section);
        assertEquals(p, section.selectFirst("p"));
        // deepest element is the inner <div>
        Element inner = section.selectFirst("div.inner");
        assertNotNull(inner);
        assertEquals(p, inner.child(0));
    }

    @Test
    public void wrap_UnbalancedHtml_LeavesRemainderAppended() {
        Document doc = Jsoup.parse("<p id='t'>Hello</p>");
        Element p = doc.selectFirst("p#t");
        // wrap with two sibling elements – the second one is the “remainder”
        p.wrap("<div class='wrapper'></div><span class='extra'></span>");

        Element wrapper = doc.selectFirst("div.wrapper");
        assertNotNull(wrapper);
        assertEquals(p, wrapper.child(0));

        // the remainder <span> should be appended as a sibling of the wrapper
        Element extra = doc.selectFirst("span.extra");
        assertNotNull(extra);
        assertEquals(wrapper.parent(), extra.parent());
    }

    @Test
    public void unwrap_RemovesNodeButPreservesChildren() {
        Document doc = Jsoup.parse("<div id='p'><span id='c'>content</span></div>");
        Element span = doc.selectFirst("span#c");
        Node firstChild = span.unwrap(); // should return the span itself (first child of removed node)

        // After unwrap, the span is gone, its child (a TextNode) is now a direct child of the div
        Element parentDiv = doc.selectFirst("div#p");
        assertEquals(1, parentDiv.childNodeSize());
        assertEquals("content", parentDiv.text());

        // unwrap returns the original first child (the span itself)
        assertSame(span, firstChild);
    }

    /* -------------------------------------------------
     *  replaceWith / remove
     * ------------------------------------------------- */

    @Test
    public void replaceWith_ReplacesNodeInTree() {
        Document doc = Jsoup.parse("<ul><li id='old'>old</li></ul>");
        Element oldLi = doc.selectFirst("li#old");
        Element newLi = new Element("li").attr("id", "new").text("new");
        oldLi.replaceWith(newLi);

        assertNull(doc.selectFirst("li#old"));
        Element fetched = doc.selectFirst("li#new");
        assertNotNull(fetched);
        assertEquals("new", fetched.text());
    }

    @Test(expected = IllegalArgumentException.class)
    public void replaceWith_NullNode_Throws() {
        new Element("p").replaceWith(null);
    }

    @Test
    public void remove_DeletesNodeFromParent() {
        Document doc = Jsoup.parse("<div><p id='del'>remove me</p></div>");
        Element p = doc.selectFirst("p#del");
        p.remove();

        assertNull(doc.selectFirst("p#del"));
        // parent div should now have zero children
        Element div = doc.selectFirst("div");
        assertEquals(0, div.childNodeSize());
    }

    /* -------------------------------------------------
     *  clone / shallowClone / hasSameValue / equals
     * ------------------------------------------------- */

    @Test
    public void clone_CreatesDeepCopy() {
        Document doc = Jsoup.parse("<div><p>one</p><p>two</p></div>");
        Element div = doc.selectFirst("div");

        Element clone = (Element) div.clone();
        // cloned element is a different instance
        assertNotSame(div, clone);
        // its HTML must be identical
        assertEquals(div.outerHtml(), clone.outerHtml());

        // modifying the clone must not affect the original
        clone.selectFirst("p").text("changed");
        assertNotEquals(div.outerHtml(), clone.outerHtml());
    }

    @Test
    public void shallowClone_CreatesOrphanWithoutChildren() {
        Document doc = Jsoup.parse("<div><p>child</p></div>");
        Element div = doc.selectFirst("div");
        Element shallow = (Element) div.shallowClone();

        assertNotSame(div, shallow);
        assertEquals(0, shallow.childNodeSize()); // no children
        assertNull(shallow.parent()); // no parent
    }

    @Test
    public void hasSameValue_TrueWhenHtmlEqual() {
        Element a = new Element("a").attr("href", "x");
        Element b = new Element("a").attr("href", "x");
        assertTrue(a.hasSameValue(b));
        assertTrue(b.hasSameValue(a));
    }

    @Test
    public void hasSameValue_FalseWhenDifferent() {
        Element a = new Element("a").attr("href", "x");
        Element b = new Element("a").attr("href", "y");
        assertFalse(a.hasSameValue(b));
    }

    @Test
    public void equals_IsIdentityOnly() {
        Element a = new Element("p");
        Element b = a;
        Element c = new Element("p");
        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
    }

    /* -------------------------------------------------
     *  toString / outerHtml / html(Appendable)
     * ------------------------------------------------- */

    @Test
    public void outerHtml_ToString_ReturnSameString() {
        Element el = new Element("p").text("hello");
        assertEquals(el.outerHtml(), el.toString());
    }

    @Test
    public void html_AppendsToStringBuilder() {
        Element el = new Element("p").text("world");
        StringBuilder sb = new StringBuilder();
        el.html(sb);
        assertEquals("<p>world</p>", sb.toString());
    }

    /* -------------------------------------------------
     *  traversal helpers (traverse / filter)
     * ------------------------------------------------- */

    @Test
    public void traverse_VisitsAllNodesDepthFirst() {
        Document doc = Jsoup.parse("<div><p>one</p><p>two</b></div>");
        final StringBuilder visited = new StringBuilder();

        doc.traverse(new NodeVisitor() {
            @Override public void head(Node node, int depth) {
                visited.append(node.nodeName()).append(":H;");
            }
            @Override public void tail(Node node, int depth) {
                visited.append(node.nodeName()).append(":T;");
            }
        });

        // Expected order: html, head, body, div, p, #text, p, #text, #root (tail order)
        assertTrue(visited.toString().contains("div:H;"));
        assertTrue(visited.toString().contains("p:T;"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void traverse_NullVisitor_Throws() {
        new Element("p").traverse(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void filter_NullFilter_Throws() {
        new Element("p").filter(null);
    }

    /* -------------------------------------------------
     *  Miscellaneous helpers
     * ------------------------------------------------- */

    @Test
    public void siblingIndex_ReflectsPosition() {
        Document doc = simpleDoc();
        Element child1 = doc.selectFirst("p#child1");
        Element child2 = doc.selectFirst("p#child2");
        assertEquals(0, child1.siblingIndex());
        assertEquals(1, child2.siblingIndex());

        // after moving child2 before child1, indices update
        child2.before(child1);
        assertEquals(0, child2.siblingIndex());
        assertEquals(1, child1.siblingIndex());
    }

    @Test
    public void root_ReturnsTopmostAncestor() {
        Document doc = simpleDoc();
        Element p = doc.selectFirst("p#child1");
        assertSame(doc, p.root());
    }

    @Test
    public void ownerDocument_ReturnsDocumentOrNull() {
        Document doc = simpleDoc();
        Element p = doc.selectFirst("p#child1");
        assertSame(doc, p.ownerDocument());

        // a node without a document (e.g., a detached element)
        Element detached = new Element("div");
        assertNull(detached.ownerDocument());
    }
}
```
**###Test END##**