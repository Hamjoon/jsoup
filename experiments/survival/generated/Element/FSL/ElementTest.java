package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ElementTest {

    /**
     * Helper to create a simple element with a given tag name.
     */
    private Element newElement(String tag) {
        return new Element(Tag.valueOf(tag), "", new Attributes());
    }

    @Test
    public void testTagNameAndTagObject() {
        Element el = newElement("div");
        assertEquals("div", el.tagName());
        assertEquals("div", el.tag().getName());

        el.tagName("span");
        assertEquals("span", el.tagName());
        assertEquals("span", el.tag().getName());
    }

    @Test
    public void testIsBlockAndIdAttribute() {
        Element block = newElement("div");
        Element inline = newElement("span");
        assertTrue(block.isBlock());
        assertFalse(inline.isBlock());

        block.attr("id", "myId");
        assertEquals("myId", block.id());
    }

    @Test
    public void testAttributesAndDataset() {
        Element el = newElement("div");
        el.attr("data-key", "value");
        el.attr("data-number", "123");
        el.attr("title", "hello");

        Map<String, String> ds = el.dataset();
        assertEquals(2, ds.size());
        assertEquals("value", ds.get("key"));
        assertEquals("123", ds.get("number"));
        assertNull(ds.get("title"));
    }

    @Test
    public void testParentAndAncestors() {
        String html = "<div id='root'><section><p id='p1'>Text</p></section></div>";
        Document doc = Jsoup.parse(html);
        Element p = doc.getElementById("p1");
        Elements parents = p.parents();

        // parents should be [section, div]
        assertEquals(2, parents.size());
        assertEquals("section", parents.get(0).tagName());
        assertEquals("div", parents.get(1).tagName());
    }

    @Test
    public void testChildrenAndChildElementsListCacheInvalidation() {
        Element parent = newElement("ul");
        assertEquals(0, parent.children().size());

        // add first child
        Element li1 = parent.appendElement("li");
        li1.text("Item 1");
        assertEquals(1, parent.children().size());

        // add second child via prepend
        Element li2 = parent.prependElement("li");
        li2.text("Item 0");
        assertEquals(2, parent.children().size());

        // ensure order is correct (prepend puts at index 0)
        List<Element> childEls = parent.children();
        assertEquals("Item 0", childEls.get(0).text());
        assertEquals("Item 1", childEls.get(1).text());
    }

    @Test
    public void testTextNodesAndOwnText() {
        String html = "<p>One <span>Two</span> Three <br> Four</p>";
        Document doc = Jsoup.parse(html);
        Element p = doc.selectFirst("p");

        // text() normalises whitespace and includes text from children
        assertEquals("One Two Three Four", p.text());

        // ownText() excludes text inside <span>
        assertEquals("One  Three  Four", p.ownText());

        // textNodes() should return three TextNode instances
        List<TextNode> txtNodes = p.textNodes();
        assertEquals(3, txtNodes.size());
        assertEquals("One ", txtNodes.get(0).getWholeText());
        assertEquals(" Three ", txtNodes.get(1).getWholeText());
        assertEquals(" Four", txtNodes.get(2).getWholeText());
    }

    @Test
    public void testSelectAndSelectFirst() {
        String html = "<div><p class='c1'>A</p><p class='c2'>B</p></div>";
        Document doc = Jsoup.parse(html);
        Element div = doc.selectFirst("div");

        Elements ps = div.select("p.c2");
        assertEquals(1, ps.size());
        assertEquals("B", ps.first().text());

        Element first = div.selectFirst("p.c1");
        assertNotNull(first);
        assertEquals("A", first.text());
    }

    @Test
    public void testIsEvaluator() {
        String html = "<ul><li class='item'>One</li></ul>";
        Document doc = Jsoup.parse(html);
        Element li = doc.selectFirst("li");

        assertTrue(li.is("li.item"));
        assertFalse(li.is("div"));
    }

    @Test
    public void testAppendAndPrependText() {
        Element el = newElement("div");
        el.appendText("first");
        el.prependText("zero");
        assertEquals("zerofirst", el.text());

        // verify underlying child nodes order
        assertEquals(2, el.childNodeSize());
        assertTrue(el.childNode(0) instanceof TextNode);
        assertTrue(el.childNode(1) instanceof TextNode);
    }

    @Test
    public void testAppendAndPrependHtml() {
        Element el = newElement("div");
        el.append("<p>One</p><span>Two</span>");
        assertEquals(2, el.children().size());
        assertEquals("p", el.children().get(0).tagName());
        assertEquals("span", el.children().get(1).tagName());

        el.prepend("<b>Bold</b>");
        assertEquals(3, el.children().size());
        assertEquals("b", el.children().get(0).tagName());
    }

    @Test
    public void testEmptyRemovesChildrenButKeepsAttributes() {
        String html = "<div id='x' class='c'><p>text</p></div>";
        Document doc = Jsoup.parse(html);
        Element div = doc.selectFirst("div");
        assertEquals(1, div.childNodeSize());
        div.empty();
        assertEquals(0, div.childNodeSize());
        assertEquals("x", div.id());
        assertEquals("c", div.className());
    }

    @Test
    public void testSiblingNavigation() {
        String html = "<div><p id='a'>A</p><p id='b'>B</p><p id='c'>C</p></div>";
        Document doc = Jsoup.parse(html);
        Element a = doc.getElementById("a");
        Element b = doc.getElementById("b");
        Element c = doc.getElementById("c");

        assertSame(b, a.nextElementSibling());
        assertNull(c.nextElementSibling());

        assertSame(a, b.previousElementSibling());
        assertNull(a.previousElementSibling());

        Elements siblings = b.siblingElements();
        assertEquals(2, siblings.size());
        assertTrue(siblings.contains(a));
        assertTrue(siblings.contains(c));

        assertSame(a, b.firstElementSibling());
        assertSame(c, b.lastElementSibling());

        assertEquals(1, b.elementSiblingIndex());
    }

    @Test
    public void testGetElementsByVariousSelectors() {
        String html = "<div id='d' data-x='val' class='alpha beta'><p class='beta'>para</p><span title='t'>s</span></div>";
        Document doc = Jsoup.parse(html);
        Element div = doc.selectFirst("div");

        // by tag
        Elements byTag = div.getElementsByTag("p");
        assertEquals(1, byTag.size());
        assertEquals("para", byTag.first().text());

        // by id
        Element byId = div.getElementById("d");
        assertSame(div, byId);

        // by class
        Elements byClass = div.getElementsByClass("beta");
        assertEquals(2, byClass.size()); // div and p

        // by attribute presence
        Elements byAttr = div.getElementsByAttribute("title");
        assertEquals(1, byAttr.size());
        assertEquals("span", byAttr.first().tagName());

        // by attribute starting
        Elements byAttrStart = div.getElementsByAttributeStarting("data-");
        assertEquals(1, byAttrStart.size());

        // by attribute value
        Elements byAttrValue = div.getElementsByAttributeValue("class", "alpha beta");
        assertEquals(1, byAttrValue.size());

        // by attribute value not
        Elements byAttrNot = div.getElementsByAttributeValueNot("class", "alpha beta");
        assertEquals(2, byAttrNot.size()); // p and span

        // attribute value starting
        Elements byAttrValStart = div.getElementsByAttributeValueStarting("class", "alpha");
        assertEquals(1, byAttrValStart.size());

        // attribute value ending
        Elements byAttrValEnd = div.getElementsByAttributeValueEnding("class", "beta");
        assertEquals(2, byAttrValEnd.size());

        // attribute value containing
        Elements byAttrContain = div.getElementsByAttributeValueContaining("class", "lph");
        assertEquals(1, byAttrContain.size());

        // attribute value matching (regex)
        Elements byAttrRegex = div.getElementsByAttributeValueMatching("class", "a.*a");
        assertEquals(1, byAttrRegex.size());

        // index based selectors
        Elements allPs = div.select("p, span");
        assertEquals(2, allPs.size());
        assertEquals(0, allPs.get(0).elementSiblingIndex()); // first child element
        Elements lessThan = div.getElementsByIndexLessThan(1);
        assertTrue(lessThan.contains(allPs.get(0)));
        Elements greaterThan = div.getElementsByIndexGreaterThan(0);
        assertTrue(greaterThan.contains(allPs.get(1)));
        Elements equals = div.getElementsByIndexEquals(0);
        assertEquals(1, equals.size());
        assertSame(allPs.get(0), equals.first());
    }

    @Test
    public void testContainsTextAndMatchingText() {
        String html = "<div><p>hello world</p><p>goodbye</p></div>";
        Document doc = Jsoup.parse(html);
        Element div = doc.selectFirst("div");

        Elements containing = div.getElementsContainingText("hello");
        assertEquals(1, containing.size());
        assertEquals("p", containing.first().tagName());

        Elements ownContaining = div.getElementsContainingOwnText("hello");
        assertEquals(0, ownContaining.size());

        Elements matching = div.getElementsMatchingText("hello\\s+world");
        assertEquals(1, matching.size());

        Elements matchingOwn = div.getElementsMatchingOwnText("^good");
        assertEquals(1, matchingOwn.size());
        assertEquals("goodbye", matchingOwn.first().text());
    }

    @Test
    public void testGetAllElementsAndHasText() {
        String html = "<div><p></p><span> </span></div>";
        Document doc = Jsoup.parse(html);
        Element div = doc.selectFirst("div");

        Elements all = div.getAllElements();
        // includes the div itself + its children + the document root
        assertTrue(all.size() >= 3);

        assertFalse(div.hasText()); // only whitespace
        div.appendText("text");
        assertTrue(div.hasText());
    }

    @Test
    public void testClassHandling() {
        Element el = newElement("div");
        el.attr("class", "alpha beta");
        assertEquals("alpha beta", el.className());

        Set<String> names = el.classNames();
        assertTrue(names.contains("alpha"));
        assertTrue(names.contains("beta"));
        assertEquals(2, names.size());

        // hasClass (case insensitive)
        assertTrue(el.hasClass("Alpha"));
        assertFalse(el.hasClass("gamma"));

        // addClass
        el.addClass("gamma");
        assertTrue(el.hasClass("gamma"));
        assertEquals(3, el.classNames().size());

        // removeClass
        el.removeClass("beta");
        assertFalse(el.hasClass("beta"));
        assertEquals(2, el.classNames().size());

        // toggleClass (remove)
        el.toggleClass("alpha");
        assertFalse(el.hasClass("alpha"));
        // toggleClass (add)
        el.toggleClass("delta");
        assertTrue(el.hasClass("delta"));
    }

    @Test
    public void testValueHandlingForInputAndTextarea() {
        String html = "<input id='i' value='old'><textarea id='t'>old text</textarea>";
        Document doc = Jsoup.parse(html);
        Element input = doc.getElementById("i");
        Element textarea = doc.getElementById("t");

        // get values
        assertEquals("old", input.val());
        assertEquals("old text", textarea.val());

        // set values
        input.val("new");
        textarea.val("new text");
        assertEquals("new", input.attr("value"));
        assertEquals("new text", textarea.text());
    }

    @Test
    public void testHtmlAndOuterHtml() {
        Element el = newElement("div");
        el.append("<p>One</p><p>Two</p>");
        String inner = el.html();
        assertTrue(inner.contains("<p>One</p>"));
        assertTrue(inner.contains("<p>Two</p>"));

        String outer = el.outerHtml();
        assertTrue(outer.startsWith("<div"));
        assertTrue(outer.endsWith("</div>"));
    }

    @Test
    public void testCloneAndShallowCloneIndependence() {
        Element original = newElement("div");
        original.attr("id", "orig");
        original.append("<p>para</p>");

        // deep clone
        Element deep = original.clone();
        assertEquals("orig", deep.id());
        assertEquals(1, deep.childNodeSize());

        // modify deep clone, original unchanged
        deep.attr("id", "changed");
        deep.append("<span>new</span>");
        assertEquals("orig", original.id());
        assertEquals(1, original.childNodeSize());

        // shallow clone (no children)
        Element shallow = original.shallowClone();
        assertEquals("orig", shallow.id());
        assertEquals(0, shallow.childNodeSize());

        // ensure shallow clone shares attribute instance? (should be a copy)
        shallow.attr("id", "shallow");
        assertEquals("orig", original.id());
    }

    @Test
    public void testToStringEqualsOuterHtml() {
        Element el = newElement("p");
        el.text("Hello");
        assertEquals(el.outerHtml(), el.toString());
    }

    @Test
    public void testOuterHtmlHeadAndTailThroughHtmlPrettyPrint() {
        Document doc = Jsoup.parse("<div><p>Hi</p></div>");
        doc.outputSettings().prettyPrint(true);
        String pretty = doc.outerHtml();
        // There should be line breaks and indentation
        assertTrue(pretty.contains("\n"));
        assertTrue(pretty.contains("  <p>Hi</p>"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetElementsByAttributeValueMatchingInvalidRegex() {
        Element el = newElement("div");
        // This should throw because the regex is malformed
        el.getElementsByAttributeValueMatching("class", "[unclosed");
    }

    @Test
    public void testPreserveWhitespaceInPreAndCodeTags() {
        String html = "<pre>   spaced   </pre><code>code</code>";
        Document doc = Jsoup.parse(html);
        Element pre = doc.selectFirst("pre");
        Element code = doc.selectFirst("code");

        // preserveWhitespace should be true for <pre> and <code>
        assertTrue(Element.preserveWhitespace(pre));
        assertTrue(Element.preserveWhitespace(code));

        // text() should keep whitespace for <pre>
        assertEquals("   spaced   ", pre.text());
    }

    @Test
    public void testInsertChildrenAtVariousPositions() {
        Element parent = newElement("ul");
        Element li1 = parent.appendElement("li").text("1");
        Element li2 = parent.appendElement("li").text("2");
        Element li3 = parent.appendElement("li").text("3");

        // Insert at start
        Element newFirst = newElement("li").text("0");
        parent.insertChildren(0, Collections.singletonList(newFirst));
        assertEquals("0", parent.child(0).text());

        // Insert at negative index (end)
        Element newLast = newElement("li").text("4");
        parent.insertChildren(-1, newLast);
        assertEquals("4", parent.child(parent.children().size() - 1).text());

        // Insert in middle
        Element middle = newElement("li").text("mid");
        parent.insertChildren(2, middle);
        assertEquals("mid", parent.child(2).text());
    }

    @Test
    public void testRemoveAndToggleClassEdgeCases() {
        Element el = newElement("div");
        // start with no class attribute
        assertEquals("", el.className());

        // add class then toggle off
        el.addClass("test");
        assertTrue(el.hasClass("test"));
        el.toggleClass("test");
        assertFalse(el.hasClass("test"));
        assertEquals("", el.className());

        // remove non‑existent class should be a no‑op
        el.removeClass("nonexistent");
        assertEquals("", el.className());
    }

    @Test
    public void testAttributesChainingAndBooleanAttribute() {
        Element el = newElement("input");
        el.attr("type", "checkbox")
          .attr("checked", true); // boolean attribute

        // boolean attribute should be present with empty value
        assertTrue(el.hasAttr("checked"));
        assertEquals("", el.attr("checked"));

        // set to false should remove it
        el.attr("checked", false);
        assertFalse(el.hasAttr("checked"));
    }

    @Test
    public void testDataAndCommentNodes() {
        // Build an element manually with DataNode and Comment
        Element script = newElement("script");
        script.appendChild(new DataNode("var a = 1;", ""));
        script.appendChild(new Comment(" a comment "));
        assertEquals("var a = 1; a comment ", script.data());
    }
}
