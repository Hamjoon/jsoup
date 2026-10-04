package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit4 test suite for {@link Element}.  The tests exercise the public API,
 * covering attribute handling, tree manipulation, CSS selector generation,
 * text extraction, class helpers and sibling navigation.
 *
 * The test file is deliberately self‑contained – it builds small DOM fragments
 * with {@code Jsoup.parse} or directly using the {@link Element} constructors
 * rather than relying on external files.
 */
public class ElementTest {

    private Element root;          // <div id="root">
    private Element paragraph;    // <p id="p1" class="intro">
    private Element span;         // <span id="s">inner</span>

    @Before
    public void setUp() {
        // Build a tiny DOM: <div id="root"><p id="p1" class="intro"><span id="s">inner</span></p></div>
        Document doc = Jsoup.parse("<div id='root'><p id='p1' class='intro'><span id='s'>inner</span></p></div>");
        root = doc.selectFirst("div#root");
        paragraph = doc.selectFirst("p#p1");
        span = doc.selectFirst("span#s");
        assertNotNull(root);
        assertNotNull(paragraph);
        assertNotNull(span);
    }

    /* --------------------------------------------------------------------- */
    /*  Basic tag & attribute handling                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTagNameAndTagObject() {
        Element el = new Element("div");
        assertEquals("div", el.tagName());
        assertNotNull(el.tag());
        assertTrue(el.isBlock()); // div is a block element

        el.tagName("span");
        assertEquals("span", el.tagName());
        assertEquals("span", el.tag().getName());
        assertFalse(el.isBlock()); // span is inline
    }

    @Test
    public void testAttributesAndBooleanAttr() {
        Element el = new Element("a");
        el.attr("href", "http://example.com")
          .attr("download", true);               // boolean attribute

        assertEquals("http://example.com", el.attr("href"));
        assertTrue(el.hasAttr("download"));
        assertEquals("", el.attr("download"));   // boolean attr stores empty string

        // removing boolean attr
        el.attr("download", false);
        assertFalse(el.hasAttr("download"));
    }

    @Test
    public void testDatasetView() {
        Element el = new Element("div");
        el.attr("data-user", "bob");
        el.attr("data-id", "123");
        el.attr("class", "test");

        Map<String, String> data = el.dataset();
        assertEquals(2, data.size());
        assertEquals("bob", data.get("user"));
        assertEquals("123", data.get("id"));

        // changes in the map reflect back to attributes
        data.put("role", "admin");
        assertEquals("admin", el.attr("data-role"));
    }

    @Test
    public void testIdAndParentRelationships() {
        assertEquals("s", span.id());
        assertEquals(paragraph, span.parent());
        Elements ancestors = span.parents();
        assertEquals(2, ancestors.size());
        assertEquals(paragraph, ancestors.get(0));
        assertEquals(root, ancestors.get(1));
    }

    /* --------------------------------------------------------------------- */
    /*  Child element handling & caching                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testChildrenAndChildCacheInvalidation() {
        Element parent = new Element("section");
        // initially empty
        assertTrue(parent.children().isEmpty());

        // add first child
        Element child1 = parent.appendElement("p");
        assertEquals(1, parent.children().size());
        assertEquals(child1, parent.child(0));

        // cache should be refreshed after another modification
        Element child2 = parent.appendElement("div");
        Elements children = parent.children(); // uses cached list
        assertEquals(2, children.size());
        assertTrue(children.contains(child1));
        assertTrue(children.contains(child2));
    }

    @Test
    public void testInsertChildrenPositiveAndNegativeIndex() {
        Element container = new Element("ul");
        Element li1 = container.appendElement("li").text("one");
        Element li3 = container.appendElement("li").text("three");

        // insert at index 1 (between li1 and li3)
        Element li2 = new Element("li").text("two");
        container.insertChildren(1, Collections.singleton(li2));

        assertEquals(3, container.children().size());
        assertEquals("one", container.child(0).text());
        assertEquals("two", container.child(1).text());
        assertEquals("three", container.child(2).text());

        // insert at -1 (end)
        Element li4 = new Element("li").text("four");
        container.insertChildren(-1, Collections.singleton(li4));
        assertEquals(4, container.children().size());
        assertEquals("four", container.child(3).text());
    }

    @Test
    public void testAppendAndPrependHtmlFragments() {
        Element div = new Element("div");
        div.append("<p>First</p>");
        div.prepend("<h1>Header</h1>");

        Elements children = div.children();
        assertEquals(2, children.size());
        assertEquals("h1", children.get(0).tagName());
        assertEquals("p", children.get(1).tagName());

        assertEquals("<h1>Header</h1>", children.get(0).outerHtml());
        assertEquals("<p>First</p>", children.get(1).outerHtml());
    }

    /* --------------------------------------------------------------------- */
    /*  Text extraction utilities                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTextOwnTextWholeText() {
        String html = "<p>One <span>Two</span> Three <br> Four</p>";
        Element p = Jsoup.parse(html).selectFirst("p");
        assertEquals("One Two Three Four", p.text());
        assertEquals("One Three Four", p.ownText());
        assertEquals("One Two Three  Four", p.wholeText()); // note double space before Four due to <br>
    }

    @Test
    public void testHasTextAndWhitespaceOnly() {
        Element div = new Element("div");
        div.appendText("   "); // whitespace only
        assertFalse(div.hasText());

        div.appendText("Hello");
        assertTrue(div.hasText());
    }

    @Test
    public void testDataExtraction() {
        String script = "<script>var x = 42;</script>";
        Element scriptEl = Jsoup.parse(script).selectFirst("script");
        assertEquals("var x = 42;", scriptEl.data());

        // data node directly
        DataNode dataNode = new DataNode("some data");
        Element div = new Element("div");
        div.appendChild(dataNode);
        assertEquals("some data", div.data());
    }

    /* --------------------------------------------------------------------- */
    /*  Class attribute helpers                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testClassHelpers() {
        Element el = new Element("div");
        el.attr("class", "first second");
        assertEquals("first second", el.className());

        Set<String> names = el.classNames();
        assertTrue(names.containsAll(Arrays.asList("first", "second")));

        el.addClass("third");
        assertTrue(el.hasClass("third"));
        assertEquals(3, el.classNames().size());

        el.removeClass("second");
        assertFalse(el.hasClass("second"));
        assertEquals(2, el.classNames().size());

        el.toggleClass("first"); // removes
        assertFalse(el.hasClass("first"));
        el.toggleClass("new");   // adds
        assertTrue(el.hasClass("new"));
    }

    /* --------------------------------------------------------------------- */
    /*  CSS selector generation                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCssSelectorWithId() {
        Element el = new Element("section");
        el.attr("id", "main");
        assertEquals("#main", el.cssSelector());
    }

    @Test
    public void testCssSelectorWithoutIdAndWithClasses() {
        // <div class="wrapper"><p class="intro">text</p></div>
        Document doc = Jsoup.parse("<div class='wrapper'><p class='intro'>text</p></div>");
        Element p = doc.selectFirst("p.intro");
        // selector should be "div > p.intro"
        assertEquals("div > p.intro", p.cssSelector());

        // add a sibling with same tag and class to force nth-child
        Element sibling = doc.selectFirst("div").appendElement("p").addClass("intro");
        // sibling is the second matching element
        assertTrue(sibling.cssSelector().endsWith(":nth-child(2)"));
    }

    /* --------------------------------------------------------------------- */
    /*  Sibling navigation methods                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSiblingNavigation() {
        Document doc = Jsoup.parse("<ul><li id='a'>A</li><li id='b'>B</li><li id='c'>C</li></ul>");
        Element a = doc.selectFirst("li#a");
        Element b = doc.selectFirst("li#b");
        Element c = doc.selectFirst("li#c");

        // siblingElements of b should be a and c
        Elements siblings = b.siblingElements();
        assertEquals(2, siblings.size());
        assertTrue(siblings.contains(a));
        assertTrue(siblings.contains(c));

        assertEquals(b, a.nextElementSibling());
        assertEquals(b, c.previousElementSibling());

        assertEquals(a, b.firstElementSibling());
        assertEquals(c, b.lastElementSibling());

        assertEquals(1, b.elementSiblingIndex());
    }

    /* --------------------------------------------------------------------- */
    /*  Form value handling                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testValForInputAndTextarea() {
        Element input = new Element("input");
        input.val("123");
        assertEquals("123", input.attr("value"));
        assertEquals("123", input.val());

        Element textarea = new Element("textarea");
        textarea.val("some text");
        assertEquals("some text", textarea.text());
        assertEquals("some text", textarea.val());
    }

    /* --------------------------------------------------------------------- */
    /*  Empty & clone utilities                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEmptyAndClone() {
        Element div = new Element("div");
        div.appendElement("p").text("para");
        div.appendText("raw");
        assertFalse(div.children().isEmpty());
        assertFalse(div.textNodes().isEmpty());

        div.empty();
        assertTrue(div.children().isEmpty());
        assertTrue(div.textNodes().isEmpty());

        // cloning should produce an equal but independent element
        Element original = new Element("section");
        original.attr("data-x", "1");
        Element clone = original.clone();
        assertEquals(original.tagName(), clone.tagName());
        assertEquals(original.attr("data-x"), clone.attr("data-x"));
        // modifying clone does not affect original
        clone.attr("data-x", "2");
        assertEquals("1", original.attr("data-x"));
        assertEquals("2", clone.attr("data-x"));
    }
}
