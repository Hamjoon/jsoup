package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Element}.
 *
 * Covers typical usage, edge‑cases and error paths for the public API.
 */
public class ElementTest {

    /** Simple HTML used by many tests. */
    private static final String SIMPLE_HTML =
            "<!doctype html>" +
            "<html>" +
            "  <head><title>Test</title></head>" +
            "  <body id='bdy' class='root bodyClass'>" +
            "    <div id='container' class='wrapper'>" +
            "      <p class='para'>Hello <span class='inner'>World</span>!</p>" +
            "      <a href='http://example.com' data-info='jsoup' data-number='42'>link</a>" +
            "      <script>var x = 1;</script>" +
            "      <textarea>Some text</textarea>" +
            "    </div>" +
            "    <br/>" +
            "  </body>" +
            "</html>";

    private Document document() {
        return Jsoup.parse(SIMPLE_HTML);
    }

    /* --------------------------------------------------------------------- *
     *  Basic construction & tag handling
     * --------------------------------------------------------------------- */
    @Test
    public void tagName_andTagChange() {
        Element el = new Element("span");
        assertEquals("span", el.tagName());

        el.tagName("div");
        assertEquals("div", el.tagName());
        assertSame(Tag.valueOf("div", Parser.parseSettings), el.tag());
    }

    @Test
    public void isBlock_trueForDiv_falseForSpan() {
        Element div = new Element("div");
        Element span = new Element("span");
        assertTrue(div.isBlock());
        assertFalse(span.isBlock());
    }

    /* --------------------------------------------------------------------- *
     *  Attribute handling
     * --------------------------------------------------------------------- */
    @Test
    public void id_andAttr() {
        Element el = document().selectFirst("#container");
        assertEquals("container", el.id());

        el.attr("id", "newId");
        assertEquals("newId", el.id());
    }

    @Test
    public void booleanAttribute() {
        Element el = new Element("input");
        el.attr("disabled", true);
        assertTrue(el.hasAttr("disabled"));
        assertEquals("", el.attr("disabled")); // boolean attrs have empty value

        el.attr("disabled", false);
        assertFalse(el.hasAttr("disabled"));
    }

    @Test
    public void datasetExtraction() {
        Element a = document().selectFirst("a");
        Map<String, String> data = a.dataset();

        assertEquals("jsoup", data.get("info"));
        assertEquals("42", data.get("number"));
        // modifying the dataset updates the underlying attributes
        data.put("new", "value");
        assertEquals("value", a.attr("data-new"));
    }

    /* --------------------------------------------------------------------- *
     *  Parent / child navigation
     * --------------------------------------------------------------------- */
    @Test
    public void parent_andAncestors() {
        Element span = document().selectFirst("span.inner");
        assertEquals("p", span.parent().tagName());

        Elements ancestors = span.parents();
        assertEquals(3, ancestors.size()); // p, div, body
        assertEquals("p", ancestors.get(0).tagName());
        assertEquals("div", ancestors.get(1).tagName());
        assertEquals("body", ancestors.get(2).tagName());
    }

    @Test
    public void child_andChildren() {
        Element div = document().selectFirst("#container");
        Element p = div.child(0);
        assertEquals("p", p.tagName());

        Elements children = div.children();
        assertEquals(4, children.size()); // p, a, script, textarea
        assertTrue(children.contains(p));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void child_invalidIndexThrows() {
        Element div = document().selectFirst("#container");
        div.child(100); // out of range
    }

    @Test
    public void textNodes_andDataNodes() {
        Element p = document().selectFirst("p");
        List<TextNode> texts = p.textNodes();
        assertEquals(3, texts.size()); // "Hello ", "World", "!"
        assertEquals("Hello ", texts.get(0).getWholeText());

        // data nodes – script contains a DataNode
        Element script = document().selectFirst("script");
        List<DataNode> dataNodes = script.dataNodes();
        assertEquals(1, dataNodes.size());
        assertEquals("var x = 1;", dataNodes.get(0).getWholeData());
    }

    @Test
    public void select_andSelectFirst() {
        Elements links = document().select("a[href]");
        assertEquals(1, links.size());
        Element link = links.first();
        assertEquals("a", link.tagName());

        Element first = document().selectFirst("a");
        assertSame(link, first);
    }

    @Test
    public void is_matchesEvaluator() {
        Element div = document().selectFirst("div");
        assertTrue(div.is("div"));
        assertTrue(div.is("div.wrapper"));
        assertFalse(div.is("p"));
    }

    /* --------------------------------------------------------------------- *
     *  Child mutation API
     * --------------------------------------------------------------------- */
    @Test
    public void appendChild_andSiblingIndex() {
        Element body = document().body();
        Element newDiv = new Element("div");
        body.appendChild(newDiv);
        assertEquals(body.childNodeSize() - 1, newDiv.siblingIndex());
    }

    @Test
    public void prependChild_setsIndexZero() {
        Element body = document().body();
        Element firstDiv = new Element("div");
        body.prependChild(firstDiv);
        assertEquals(0, firstDiv.siblingIndex());
    }

    @Test
    public void insertChildren_positiveAndNegativeIndex() {
        Element div = document().selectFirst("#container");
        Element extra = new Element("p").text("extra");
        div.insertChildren(1, Collections.singleton(extra));
        assertEquals("extra", div.child(1).text());

        // negative index = insert at end
        Element last = new Element("p").text("last");
        div.insertChildren(-1, last);
        assertEquals("last", div.children().last().text());
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertChildren_outOfBoundsThrows() {
        Element div = document().selectFirst("#container");
        div.insertChildren(100, new Element("p"));
    }

    @Test
    public void appendAndPrependElement() {
        Element body = document().body();
        Element appended = body.appendElement("section");
        assertEquals("section", appended.tagName());
        assertEquals(body.children().last(), appended);

        Element prepended = body.prependElement("header");
        assertEquals("header", prepended.tagName());
        assertEquals(body.children().first(), prepended);
    }

    @Test
    public void appendAndPrependText() {
        Element p = new Element("p");
        p.appendText("first");
        p.appendText("second");
        assertEquals("firstsecond", p.text());

        p.prependText("zero");
        assertEquals("zerofirstsecond", p.text());
    }

    @Test
    public void appendAndPrependHtml() {
        Element div = new Element("div");
        div.append("<p>One</p><span>Two</span>");
        assertEquals(2, div.childNodeSize());
        assertEquals("p", div.child(0).tagName());
        assertEquals("span", div.child(1).tagName());

        div.prepend("<b>Bold</b>");
        assertEquals("b", div.child(0).tagName());
    }

    @Test
    public void beforeAndAfter_withHtmlAndNode() {
        Element p = document().selectFirst("p");
        Element before = new Element("h2").text("Before");
        p.before(before);
        assertEquals("Before", p.previousElementSibling().text());

        p.after("<hr/>");
        assertEquals("hr", p.nextElementSibling().tagName());
    }

    @Test
    public void emptyRemovesAllChildren() {
        Element div = document().selectFirst("#container");
        assertTrue(div.childNodeSize() > 0);
        div.empty();
        assertEquals(0, div.childNodeSize());
    }

    @Test
    public void wrapCreatesNewParent() {
        Element span = document().selectFirst("span.inner");
        span.wrap("<div class='wrapper'></div>");
        Element wrapper = span.parent();
        assertEquals("div", wrapper.tagName());
        assertTrue(wrapper.hasClass("wrapper"));
        assertSame(wrapper, span.parent());
    }

    /* --------------------------------------------------------------------- *
     *  CSS selector generation
     * --------------------------------------------------------------------- */
    @Test
    public void cssSelector_usesIdIfPresent() {
        Element body = document().body();
        assertEquals("#bdy", body.cssSelector());
    }

    @Test
    public void cssSelector_withoutIdUsesClassAndNthChild() {
        Element span = document().selectFirst("span.inner");
        String selector = span.cssSelector();
        // should contain tag, class and nth-child
        assertTrue(selector.matches("span\\.inner: nth-child\\(\\d+\\)"));
    }

    /* --------------------------------------------------------------------- *
     *  Sibling navigation
     * --------------------------------------------------------------------- */
    @Test
    public void siblingElements_excludesSelf() {
        Element div = document().selectFirst("#container");
        Elements siblings = div.siblingElements();
        // body has only one child element (div), so siblings should be empty
        assertTrue(siblings.isEmpty());
    }

    @Test
    public void nextAndPreviousElementSibling() {
        Element p = document().selectFirst("p");
        Element a = p.nextElementSibling();
        assertEquals("a", a.tagName());

        Element before = a.previousElementSibling();
        assertSame(p, before);
    }

    @Test
    public void firstAndLastElementSibling() {
        Element body = document().body();
        Element first = body.firstElementSibling(); // should be null because body is the only top‑level element under html
        assertNull(first);

        Element div = document().selectFirst("#container");
        Element firstSibling = div.firstElementSibling(); // should be the first child of body (which is div itself) -> null
        assertNull(firstSibling);

        // add another sibling to body
        Element extra = new Element("section");
        body.appendChild(extra);
        Element last = body.lastElementSibling();
        assertSame(extra, last);
    }

    @Test
    public void elementSiblingIndexCountsElementsOnly() {
        Element body = document().body();
        Element div = document().selectFirst("#container");
        assertEquals(0, div.elementSiblingIndex());

        Element br = document().selectFirst("br");
        assertEquals(1, br.elementSiblingIndex());
    }

    /* --------------------------------------------------------------------- *
     *  Traversal / Collector helpers
     * --------------------------------------------------------------------- */
    @Test
    public void getElementsByVariousSelectors() {
        Element root = document();

        Elements byTag = root.getElementsByTag("a");
        assertEquals(1, byTag.size());

        Elements byClass = root.getElementsByClass("para");
        assertEquals(1, byClass.size());

        Elements byAttr = root.getElementsByAttribute("href");
        assertEquals(1, byAttr.size());

        Elements byAttrStart = root.getElementsByAttributeStarting("data-");
        assertEquals(2, byAttrStart.size());

        Elements byAttrValue = root.getElementsByAttributeValue("href", "http://example.com");
        assertEquals(1, byAttrValue.size());

        Elements byAttrValueNot = root.getElementsByAttributeValueNot("href", "http://example.com");
        assertEquals(0, byAttrValueNot.size());

        Elements byAttrValueStart = root.getElementsByAttributeValueStarting("href", "http");
        assertEquals(1, byAttrValueStart.size());

        Elements byAttrValueEnding = root.getElementsByAttributeValueEnding("href", ".com");
        assertEquals(1, byAttrValueEnding.size());

        Elements byAttrValueContaining = root.getElementsByAttributeValueContaining("href", "example");
        assertEquals(1, byAttrValueContaining.size());

        Elements byAttrValueRegex = root.getElementsByAttributeValueMatching("href", Pattern.compile(".*example.*"));
        assertEquals(1, byAttrValueRegex.size());

        Elements byAttrValueRegexString = root.getElementsByAttributeValueMatching("href", ".*example.*");
        assertEquals(1, byAttrValueRegexString.size());

        Elements byIndexLess = root.getElementsByIndexLessThan(2);
        assertTrue(byIndexLess.size() > 0);

        Elements byIndexGreater = root.getElementsByIndexGreaterThan(0);
        assertTrue(byIndexGreater.size() > 0);

        Elements byIndexEquals = root.getElementsByIndexEquals(0);
        assertTrue(byIndexEquals.size() > 0);

        Elements containingText = root.getElementsContainingText("World");
        assertEquals(1, containingText.size());

        Elements containingOwn = root.getElementsContainingOwnText("Hello");
        assertEquals(1, containingOwn.size());

        Elements matchingText = root.getElementsMatchingText("Hello\\s+World");
        assertEquals(1, matchingText.size());

        Elements matchingOwnText = root.getElementsMatchingOwnText("Hello\\s+World");
        assertEquals(1, matchingOwnText.size());

        Elements all = root.getAllElements();
        assertTrue(all.size() > 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getElementsByAttributeValueMatching_invalidRegex() {
        document().getElementsByAttributeValueMatching("href", "*invalid[");
    }

    /* --------------------------------------------------------------------- *
     *  Text extraction
     * --------------------------------------------------------------------- */
    @Test
    public void text_andOwnText_andWholeText() {
        Element p = document().selectFirst("p");
        assertEquals("Hello World!", p.text());
        assertEquals("Hello !", p.ownText()); // inner span excluded

        // wholeText preserves original whitespace
        String raw = p.wholeText();
        assertTrue(raw.contains("Hello "));
        assertTrue(raw.contains("World"));
    }

    @Test
    public void hasText_trueAndFalse() {
        Element empty = new Element("div");
        assertFalse(empty.hasText());

        empty.appendText("   ");
        assertFalse(empty.hasText()); // blank only

        empty.appendText("content");
        assertTrue(empty.hasText());
    }

    @Test
    public void dataExtractionFromScriptAndComment() {
        Element script = document().selectFirst("script");
        assertEquals("var x = 1;", script.data());

        // add a comment node and ensure it's included
        Comment comment = new Comment("comment");
        script.appendChild(comment);
        assertTrue(script.data().contains("comment"));
    }

    /* --------------------------------------------------------------------- *
     *  Class attribute helpers
     * --------------------------------------------------------------------- */
    @Test
    public void className_andClassNames() {
        Element div = document().selectFirst("#container");
        assertEquals("wrapper", div.className());

        Set<String> cls = div.classNames();
        assertTrue(cls.contains("wrapper"));
        assertEquals(1, cls.size());
    }

    @Test
    public void addRemoveToggleClass_andHasClass() {
        Element el = new Element("div");
        el.addClass("first");
        assertTrue(el.hasClass("first"));
        assertEquals("first", el.className());

        el.addClass("second");
        assertTrue(el.hasClass("second"));
        assertEquals(2, el.classNames().size());

        el.removeClass("first");
        assertFalse(el.hasClass("first"));
        assertEquals(1, el.classNames().size());

        el.toggleClass("second"); // remove
        assertFalse(el.hasClass("second"));
        el.toggleClass("third"); // add
        assertTrue(el.hasClass("third"));
    }

    @Test
    public void classNames_setMethodClearsWhenEmpty() {
        Element el = new Element("div");
        el.classNames(new LinkedHashSet<>(Arrays.asList("a", "b")));
        assertEquals("a b", el.className());

        el.classNames(Collections.emptySet());
        assertEquals("", el.className());
    }

    /* --------------------------------------------------------------------- *
     *  Form value handling
     * --------------------------------------------------------------------- */
    @Test
    public void val_forInputAndTextarea() {
        Element input = new Element("input");
        input.val("123");
        assertEquals("123", input.attr("value"));

        Element textarea = document().selectFirst("textarea");
        assertEquals("Some text", textarea.val());

        textarea.val("New value");
        assertEquals("New value", textarea.text());
    }

    /* --------------------------------------------------------------------- *
     *  HTML output helpers
     * --------------------------------------------------------------------- */
    @Test
    public void html_andToString() {
        Element div = new Element("div");
        div.append("<p>One</p><p>Two</p>");
        String inner = div.html();
        assertTrue(inner.contains("<p>One</p>"));
        assertTrue(inner.contains("<p>Two</p>"));

        String outer = div.outerHtml();
        assertTrue(outer.startsWith("<div"));
        assertTrue(outer.endsWith("</div>"));
        assertEquals(div.toString(), outer);
    }

    @Test
    public void outerHtmlHeadAndTail_indentBehaviour() throws IOException {
        Element div = new Element("div");
        StringBuilder accum = new StringBuilder();
        Document.OutputSettings out = new Document.OutputSettings().prettyPrint(true);
        div.outerHtmlHead(accum, 0, out);
        div.outerHtmlTail(accum, 0, out);
        assertEquals("<div></div>", accum.toString());
    }

    /* --------------------------------------------------------------------- *
     *  Cloning
     * --------------------------------------------------------------------- */
    @Test
    public void deepClone_preservesStructure() {
        Document doc = document();
        Element original = doc.selectFirst("#container");
        Element clone = original.clone();

        // different object references
        assertNotSame(original, clone);
        assertEquals(original.tagName(), clone.tagName());
        assertEquals(original.id(), clone.id());
        assertEquals(original.className(), clone.className());

        // children are also cloned
        assertEquals(original.childNodeSize(), clone.childNodeSize());
        assertNotSame(original.child(0), clone.child(0));
        assertEquals(original.child(0).tagName(), clone.child(0).tagName());
    }

    @Test
    public void shallowClone_copiesMetaOnly() {
        Element original = new Element("p", "http://base/", new Attributes().put("id", "shallow"));
        Element shallow = original.shallowClone();

        assertEquals(original.tagName(), shallow.tagName());
        assertEquals(original.baseUri(), shallow.baseUri());
        // No children should exist
        assertEquals(0, shallow.childNodeSize());
        // Attributes are shallow‑copied (same reference)
        assertSame(original.attributes(), shallow.attributes());
    }

    /* --------------------------------------------------------------------- *
     *  Defensive immutability checks
     * --------------------------------------------------------------------- */
    @Test(expected = UnsupportedOperationException.class)
    public void textNodes_isUnmodifiable() {
        Element p = document().selectFirst("p");
        List<TextNode> nodes = p.textNodes();
        nodes.add(new TextNode("illegal"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void dataNodes_isUnmodifiable() {
        Element script = document().selectFirst("script");
        List<DataNode> nodes = script.dataNodes();
        nodes.remove(0);
    }
}
