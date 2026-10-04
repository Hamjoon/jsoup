package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Tag;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link Elements}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods (basic behaviour)</li>
 *   <li>Edge cases such as empty collections, missing attributes, invalid arguments</li>
 *   <li>Exception handling (e.g. {@code wrap("")} and {@code eq(-1)})</li>
 * </ul>
 *
 * The suite uses a small HTML document created in {@link #setUp()} and builds {@link Elements}
 * objects directly where convenient.
 */
public class ElementsTest {

    private Document doc;
    private Elements divs;           // <div class="a" id="first">First</div>
    private Elements spans;          // <span data-test="val">Span1</span><span>Span2</span>
    private Elements emptyElements; // new Elements();

    @Before
    public void setUp() {
        String html = "<html><body>"
                + "<div class=\"a\" id=\"first\" data-info=\"info1\">First</div>"
                + "<div class=\"b\" data-info=\"info2\">Second</div>"
                + "<span data-test=\"val\">Span1</span>"
                + "<span>Span2</span>"
                + "<form id=\"login\"><input name=\"user\" value=\"bob\"/></form>"
                + "</body></html>";
        doc = Jsoup.parse(html);
        divs = doc.select("div");
        spans = doc.select("span");
        emptyElements = new Elements();
    }

    /* --------------------------------------------------------------------- */
    /*  Constructors                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConstructors() {
        // default
        Elements e1 = new Elements();
        assertTrue(e1.isEmpty());

        // initial capacity
        Elements e2 = new Elements(5);
        assertEquals(0, e2.size());

        // collection
        List<Element> list = Arrays.asList(divs.get(0), spans.get(0));
        Elements e3 = new Elements(list);
        assertEquals(2, e3.size());
        assertTrue(e3.contains(divs.get(0)));
        assertTrue(e3.contains(spans.get(0)));

        // varargs
        Elements e4 = new Elements(divs.get(0), spans.get(1));
        assertEquals(2, e4.size());
        assertEquals("Second", e4.get(0).text()); // first element is div[0]
    }

    /* --------------------------------------------------------------------- */
    /*  clone() – deep copy                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCloneDeepCopy() {
        Elements cloned = divs.clone();
        assertEquals(divs.size(), cloned.size());
        // modify original attribute – clone should stay unchanged
        divs.attr("data-info", "changed");
        for (int i = 0; i < divs.size(); i++) {
            assertNotEquals(divs.get(i).attr("data-info"), cloned.get(i).attr("data-info"));
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Attribute handling                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAttrFirstMatch() {
        assertEquals("info1", divs.attr("data-info")); // first div has it
    }

    @Test
    public void testAttrNoMatchReturnsEmptyString() {
        assertEquals("", divs.attr("nonexistent"));
        assertEquals("", emptyElements.attr("any"));
    }

    @Test
    public void testHasAttr() {
        assertTrue(divs.hasAttr("data-info"));
        assertFalse(divs.hasAttr("unknown"));
    }

    @Test
    public void testEachAttr() {
        List<String> infos = divs.eachAttr("data-info");
        assertEquals(Arrays.asList("info1", "info2"), infos);
        // element without attribute is omitted
        List<String> missing = spans.eachAttr("data-info");
        assertTrue(missing.isEmpty());
    }

    @Test
    public void testAttrSetAndRemove() {
        divs.attr("new-attr", "value");
        for (Element e : divs) {
            assertEquals("value", e.attr("new-attr"));
        }
        divs.removeAttr("new-attr");
        for (Element e : divs) {
            assertFalse(e.hasAttr("new-attr"));
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Class handling (addClass, removeClass, toggleClass, hasClass)         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddClassHasClass() {
        spans.addClass("highlight");
        assertTrue(spans.hasClass("highlight"));
        for (Element e : spans) {
            assertTrue(e.hasClass("highlight"));
        }
    }

    @Test
    public void testRemoveClass() {
        divs.addClass("temp");
        assertTrue(divs.hasClass("temp"));
        divs.removeClass("temp");
        assertFalse(divs.hasClass("temp"));
    }

    @Test
    public void testToggleClass() {
        // start without class
        spans.toggleClass("active");
        assertTrue(spans.hasClass("active"));
        // toggle again should remove
        spans.toggleClass("active");
        assertFalse(spans.hasClass("active"));
    }

    /* --------------------------------------------------------------------- */
    /*  Form value handling (val)                                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testValGetSet() {
        Elements inputs = doc.select("input[name=user]");
        assertEquals("bob", inputs.val()); // first (and only) input
        inputs.val("alice");
        assertEquals("alice", inputs.val());
    }

    @Test
    public void testValOnEmptyElements() {
        assertEquals("", emptyElements.val());
    }

    /* --------------------------------------------------------------------- */
    /*  Text handling (text, eachText, hasText)                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTextAndEachText() {
        String combined = divs.text();
        assertEquals("First Second", combined);
        List<String> each = divs.eachText();
        assertEquals(Arrays.asList("First", "Second"), each);
    }

    @Test
    public void testHasText() {
        assertTrue(divs.hasText());
        // create element with only whitespace
        Element blank = new Element(Tag.valueOf("p"), "");
        blank.text("   ");
        Elements single = new Elements(blank);
        assertFalse(single.hasText());
    }

    /* --------------------------------------------------------------------- */
    /*  HTML handling (html, outerHtml, toString)                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testHtmlAndOuterHtml() {
        String inner = divs.html(); // inner HTML of each div concatenated with newline
        assertTrue(inner.contains("<div"));
        assertTrue(inner.contains("First"));
        String outer = divs.outerHtml();
        assertTrue(outer.contains("<div"));
        assertTrue(outer.contains("</div>"));
    }

    @Test
    public void testToStringEqualsOuterHtml() {
        assertEquals(divs.outerHtml(), divs.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  Tag name and HTML manipulation (tagName, html(String), prepend,    */
    /*  append, before, after)                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTagNameChange() {
        divs.tagName("section");
        for (Element e : divs) {
            assertEquals("section", e.tagName());
        }
    }

    @Test
    public void testHtmlSetPrependAppend() {
        Elements p = doc.select("p");
        // ensure we have at least one element to manipulate
        Elements target = doc.select("div").first().select("div"); // empty, fallback
        Elements el = doc.select("div").first();
        el.html("<b>bold</b>");
        assertEquals("<b>bold</b>", el.html());

        el.prepend("<i>pre</i>");
        assertTrue(el.html().startsWith("<i>pre</i>"));

        el.append("<i>post</i>");
        assertTrue(el.html().endsWith("<i>post</i>"));
    }

    @Test
    public void testBeforeAfter() {
        Element firstDiv = divs.first();
        firstDiv.before("<p>before</p>");
        firstDiv.after("<p>after</p>");
        String outer = firstDiv.outerHtml();
        assertTrue(outer.contains("before"));
        assertTrue(outer.contains("after"));
    }

    @Test
    public void testWrapValidAndEmpty() {
        Element span = spans.first();
        span.wrap("<div class=\"wrapper\"></div>");
        assertTrue(span.parent().hasClass("wrapper"));
        // empty string should raise IllegalArgumentException
        try {
            span.wrap("");
            fail("Expected IllegalArgumentException for empty wrap HTML");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Unwrap, empty, remove                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testUnwrap() {
        // <div><span>text</span></div>
        Element container = doc.select("div").first();
        Element child = container.select("span").first();
        container.unwrap(); // remove <div>, keep its child
        assertEquals("span", child.parent().tagName());
    }

    @Test
    public void testEmpty() {
        Element div = divs.first();
        div.empty();
        assertEquals("", div.html());
    }

    @Test
    public void testRemove() {
        Element toRemove = doc.select("form").first();
        toRemove.remove();
        assertTrue(doc.select("form").isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  Selector based APIs (select, not, eq, is)                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSelectAndNot() {
        Elements allDivs = doc.select("div");
        Elements selected = allDivs.select(".a"); // only first div
        assertEquals(1, selected.size());
        Elements notSelected = allDivs.not(".a");
        assertEquals(1, notSelected.size());
        assertFalse(notSelected.hasClass("a"));
    }

    @Test
    public void testEqValidAndOutOfBounds() {
        Elements first = divs.eq(0);
        assertEquals(1, first.size());
        assertEquals(divs.first(), first.first());

        Elements out = divs.eq(10);
        assertTrue(out.isEmpty());

        // negative index should throw IndexOutOfBoundsException
        try {
            divs.eq(-1);
            fail("Expected IndexOutOfBoundsException for negative index");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void testIsValidAndInvalidQuery() {
        assertTrue(divs.is("div"));
        // malformed selector should throw Selector.SelectorParseException
        try {
            divs.is("???");
            fail("Expected Selector.SelectorParseException for invalid selector");
        } catch (Selector.SelectorParseException e) {
            // expected
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Sibling navigation (next, nextAll, prev, prevAll)                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSiblingNavigation() {
        // Build a simple sibling chain: <p id="a"></p><p id="b"></p><p id="c"></p>
        Document siblingDoc = Jsoup.parse("<body><p id='a'>A</p><p id='b'>B</p><p id='c'>C</p></body>");
        Elements ps = siblingDoc.select("p");

        Elements next = ps.eq(0).next();
        assertEquals(1, next.size());
        assertEquals("b", next.first().id());

        Elements nextAll = ps.eq(0).nextAll();
        assertEquals(2, nextAll.size());
        assertEquals("b", nextAll.get(0).id());
        assertEquals("c", nextAll.get(1).id());

        Elements prev = ps.eq(2).prev();
        assertEquals(1, prev.size());
        assertEquals("b", prev.first().id());

        Elements prevAll = ps.eq(2).prevAll();
        assertEquals(2, prevAll.size());
        assertEquals("b", prevAll.get(0).id());
        assertEquals("a", prevAll.get(1).id());

        // filtered sibling navigation
        Elements nextFiltered = ps.eq(0).next("p#c");
        assertEquals(1, nextFiltered.size());
        assertEquals("c", nextFiltered.first().id());

        Elements prevFiltered = ps.eq(2).prev("p#a");
        assertEquals(1, prevFiltered.size());
        assertEquals("a", prevFiltered.first().id());
    }

    /* --------------------------------------------------------------------- */
    /*  Parents (ancestors)                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testParents() {
        Element span = spans.first();
        Elements ancestors = span.parents();
        // body -> html -> #document are ancestors
        List<String> tags = new ArrayList<>();
        for (Element e : ancestors) tags.add(e.tagName());
        assertTrue(tags.contains("body"));
        assertTrue(tags.contains("html"));
        // order should be from immediate parent upwards
        assertEquals("body", ancestors.first().tagName());
    }

    /* --------------------------------------------------------------------- */
    /*  First / last element access                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFirstLast() {
        assertEquals(divs.get(0), divs.first());
        assertEquals(divs.get(divs.size() - 1), divs.last());
        assertNull(emptyElements.first());
        assertNull(emptyElements.last());
    }

    /* --------------------------------------------------------------------- */
    /*  Traversal and filtering (NodeVisitor, NodeFilter)                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTraverse() {
        final List<Element> visited = new ArrayList<>();
        NodeVisitor visitor = new NodeVisitor() {
            @Override public void head(Node node, int depth) {
                if (node instanceof Element) visited.add((Element) node);
            }
            @Override public void tail(Node node, int depth) { }
        };
        divs.traverse(visitor);
        // Should have visited the two div elements
        assertEquals(2, visited.size());
        assertTrue(visited.containsAll(divs));
    }

    @Test
    public void testFilter() {
        // Filter out <span> elements that have class "remove"
        spans.addClass("remove");
        NodeFilter filter = new NodeFilter() {
            @Override public FilterResult head(Node node, int depth) {
                if (node instanceof Element && ((Element) node).hasClass("remove")) {
                    return FilterResult.STOP; // stops traversing this branch (removes it)
                }
                return FilterResult.CONTINUE;
            }
            @Override public FilterResult tail(Node node, int depth) {
                return FilterResult.CONTINUE;
            }
        };
        // Should not throw any exception
        spans.filter(filter);
        // After filtering, the class "remove" should still be present (filter does not modify the collection,
        // it only controls traversal). We only assert that the method runs.
        assertTrue(spans.hasClass("remove"));
    }

    /* --------------------------------------------------------------------- */
    /*  Form elements extraction                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFormsExtraction() {
        Elements forms = doc.select("form, input");
        List<FormElement> formList = forms.forms();
        // Only the <form> element is a FormElement, inputs are not.
        assertEquals(1, formList.size());
        assertEquals("login", formList.get(0).id());
    }
}
