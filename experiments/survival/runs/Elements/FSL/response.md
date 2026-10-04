###Test START##
package org.jsoup.select;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeFilter;
import org.jsoup.select.NodeFilter.FilterResult;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link Elements}.
 * The tests cover every public method, exercising both the happy path
 * and edge‑cases (empty collections, null values, out‑of‑range indexes, etc.).
 */
public class ElementsTest {

    private Document doc;
    private Elements paras;
    private Elements listItems;
    private Elements forms;

    @Before
    public void setUp() {
        String html = ""
                + "<html><head><title>Test</title></head>"
                + "<body>"
                + "  <div id='container'>"
                + "    <p id='p1' class='c1' data-x='1'>First <b>bold</b></p>"
                + "    <p id='p2' class='c2'>Second</p>"
                + "    <p id='p3'>   </p>"                                   // whitespace only
                + "    <ul id='list'>"
                + "      <li id='li1'>One</li>"
                + "      <li id='li2' class='selected'>Two</li>"
                + "      <li id='li3'>Three</li>"
                + "    </ul>"
                + "    <form id='form1'>"
                + "      <input name='field' value='val1'/>"
                + "    </form>"
                + "    <form id='form2'></form>"
                + "  </div>"
                + "</body></html>";
        doc = Jsoup.parse(html);
        paras = doc.select("p");
        listItems = doc.select("li");
        forms = doc.select("form");
    }

    // -------------------------------------------------------------------------
    // Constructors & clone
    // -------------------------------------------------------------------------

    @Test
    public void testConstructorsAndClone() {
        Elements empty = new Elements();
        assertTrue(empty.isEmpty());

        Elements fromCollection = new Elements(paras);
        assertEquals(paras.size(), fromCollection.size());

        Elements fromArray = new Elements(paras.get(0), paras.get(1));
        assertEquals(2, fromArray.size());

        Elements clone = paras.clone();
        assertNotSame(paras, clone);
        assertEquals(paras.size(), clone.size());

        // deep copy – modify original attribute, clone must stay unchanged
        paras.attr("data-test", "orig");
        assertEquals("orig", paras.attr("data-test"));
        assertEquals("", clone.attr("data-test"));
    }

    // -------------------------------------------------------------------------
    // Attribute handling
    // -------------------------------------------------------------------------

    @Test
    public void testAttrHasAttrEachAttr() {
        // first element has data-x, second does not
        assertEquals("1", paras.attr("data-x"));
        assertTrue(paras.hasAttr("data-x"));
        assertFalse(paras.hasAttr("nonexistent"));

        List<String> attrs = paras.eachAttr("class");
        // only p1 and p2 have a class attribute
        assertEquals(2, attrs.size());
        assertTrue(attrs.contains("c1"));
        assertTrue(attrs.contains("c2"));
    }

    @Test
    public void testAttrSetterAndRemoveAttr() {
        paras.attr("data-added", "yes");
        for (Element e : paras) {
            assertEquals("yes", e.attr("data-added"));
        }

        paras.removeAttr("data-added");
        for (Element e : paras) {
            assertFalse(e.hasAttr("data-added"));
        }
    }

    // -------------------------------------------------------------------------
    // Class handling
    // -------------------------------------------------------------------------

    @Test
    public void testClassManipulation() {
        // add a new class to all paragraphs
        paras.addClass("newClass");
        for (Element e : paras) {
            assertTrue(e.hasClass("newClass"));
        }

        // remove existing class from first paragraph only
        paras.get(0).removeClass("c1");
        assertFalse(paras.get(0).hasClass("c1"));
        assertTrue(paras.get(1).hasClass("c2"));

        // toggle: first paragraph gets class toggled on, second gets toggled off
        paras.toggleClass("toggle");
        assertTrue(paras.get(0).hasClass("toggle"));
        assertTrue(paras.get(1).hasClass("toggle")); // added
        paras.toggleClass("toggle");
        assertFalse(paras.get(0).hasClass("toggle"));
        assertFalse(paras.get(1).hasClass("toggle"));

        // hasClass on the collection
        assertTrue(paras.hasClass("c2"));
        assertFalse(paras.hasClass("nonexistent"));
    }

    // -------------------------------------------------------------------------
    // Form value handling
    // -------------------------------------------------------------------------

    @Test
    public void testValGetterSetter() {
        Elements inputs = doc.select("input[name=field]");
        assertEquals("val1", inputs.val());

        inputs.val("newVal");
        assertEquals("newVal", inputs.val());
    }

    // -------------------------------------------------------------------------
    // Text handling
    // -------------------------------------------------------------------------

    @Test
    public void testTextEachTextHasText() {
        String combined = paras.text();
        // The whitespace‑only <p id='p3'> should contribute nothing
        assertTrue(combined.contains("First"));
        assertTrue(combined.contains("Second"));
        assertFalse(combined.contains("p3")); // not present

        assertTrue(paras.hasText());

        List<String> each = paras.eachText();
        assertEquals(2, each.size()); // only p1 and p2 have non‑blank text
        assertTrue(each.get(0).contains("First"));
        assertTrue(each.get(1).contains("Second"));
    }

    // -------------------------------------------------------------------------
    // HTML output methods
    // -------------------------------------------------------------------------

    @Test
    public void testHtmlOuterHtmlToString() {
        String inner = paras.html(); // concatenated inner HTML of each <p>
        assertTrue(inner.contains("<b>bold</b>"));

        String outer = paras.outerHtml();
        assertTrue(outer.contains("<p"));
        assertTrue(outer.contains("</p>"));

        assertEquals(outer, paras.toString());
    }

    // -------------------------------------------------------------------------
    // Tag, HTML, prepend/append, before/after, wrap, unwrap, empty, remove
    // -------------------------------------------------------------------------

    @Test
    public void testTagNameAndHtmlManipulations() {
        // change <p> to <section>
        paras.tagName("section");
        for (Element e : paras) {
            assertEquals("section", e.tagName());
        }

        // set inner HTML
        paras.html("<span>inner</span>");
        for (Element e : paras) {
            assertEquals("<span>inner</span>", e.html());
        }

        // prepend / append
        paras.prepend("<i>pre</i>").append("<i>post</i>");
        for (Element e : paras) {
            assertTrue(e.html().startsWith("<i>pre</i>"));
            assertTrue(e.html().endsWith("<i>post</i>"));
        }

        // before / after
        Elements firstPara = new Elements(paras.get(0));
        firstPara.before("<div id='before'></div>").after("<div id='after'></div>");
        assertNotNull(doc.getElementById("before"));
        assertNotNull(doc.getElementById("after"));

        // wrap
        Elements toWrap = doc.select("#li2");
        toWrap.wrap("<em class='wrapper'></em>");
        Element wrapped = doc.select("#li2").first();
        assertEquals("em", wrapped.parent().tagName());
        assertTrue(wrapped.parent().hasClass("wrapper"));

        // unwrap – after unwrapping, the <em> disappears but its child stays
        Elements wrappedEm = doc.select("em.wrapper");
        wrappedEm.unwrap();
        assertTrue(doc.select("em.wrapper").isEmpty());
        assertEquals("Two", doc.select("#li2").first().text());

        // empty
        Elements ul = doc.select("ul#list");
        ul.empty();
        assertEquals("", ul.html());

        // remove
        Elements toRemove = doc.select("p#p1");
        toRemove.remove();
        assertNull(doc.getElementById("p1"));
    }

    // -------------------------------------------------------------------------
    // Filtering & selection helpers
    // -------------------------------------------------------------------------

    @Test
    public void testSelectNotEqIs() {
        // select only <li> with class selected
        Elements selected = listItems.select(".selected");
        assertEquals(1, selected.size());
        assertEquals("li2", selected.first().id());

        // not – remove the selected one
        Elements notSelected = listItems.not(".selected");
        assertEquals(2, notSelected.size());
        for (Element e : notSelected) {
            assertFalse(e.hasClass("selected"));
        }

        // eq – valid index
        Elements second = listItems.eq(1);
        assertEquals(1, second.size());
        assertEquals("li2", second.first().id());

        // eq – out of bounds
        Elements out = listItems.eq(10);
        assertTrue(out.isEmpty());

        // is – any <li> with id 'li3'?
        assertTrue(listItems.is("#li3"));
        assertFalse(listItems.is(".nonexistent"));
    }

    // -------------------------------------------------------------------------
    // Sibling navigation
    // -------------------------------------------------------------------------

    @Test
    public void testSiblingMethods() {
        Elements firstLi = new Elements(listItems.get(0)); // li1
        Elements next = firstLi.next();
        assertEquals(1, next.size());
        assertEquals("li2", next.first().id());

        Elements nextAll = firstLi.nextAll();
        assertEquals(2, nextAll.size());
        assertEquals("li2", nextAll.get(0).id());
        assertEquals("li3", nextAll.get(1).id());

        Elements prev = next.prev();
        assertEquals(1, prev.size());
        assertEquals("li1", prev.first().id());

        Elements prevAll = nextAll.prevAll();
        assertEquals(2, prevAll.size());
        assertEquals("li2", prevAll.get(0).id());
        assertEquals("li1", prevAll.get(1).id());

        // with query filter
        Elements filteredNext = firstLi.next(".selected");
        assertEquals(1, filteredNext.size());
        assertEquals("li2", filteredNext.first().id());

        Elements filteredPrev = next.prev(".nonexistent");
        assertTrue(filteredPrev.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Parents & first/last
    // -------------------------------------------------------------------------

    @Test
    public void testParentsFirstLast() {
        Elements li = doc.select("li#li3");
        Elements parents = li.parents();
        // order is from immediate parent upwards, unique
        assertTrue(parents.stream().anyMatch(e -> e.id().equals("list")));
        assertTrue(parents.stream().anyMatch(e -> e.id().equals("container")));

        assertEquals(paras.first(), paras.get(0));
        assertEquals(paras.last(), paras.get(paras.size() - 1));
    }

    // -------------------------------------------------------------------------
    // Traversal & filtering
    // -------------------------------------------------------------------------

    @Test
    public void testTraverseAndFilter() {
        final List<String> visited = new ArrayList<>();
        NodeVisitor visitor = new NodeVisitor() {
            @Override public void head(org.jsoup.nodes.Node node, int depth) {
                if (node instanceof Element) visited.add(((Element) node).tagName());
            }
            @Override public void tail(org.jsoup.nodes.Node node, int depth) {}
        };
        paras.traverse(visitor);
        // Should have visited each <p> and its children (<b> inside first <p>)
        assertTrue(visited.contains("p"));
        assertTrue(visited.contains("b"));

        final List<String> filtered = new ArrayList<>();
        NodeFilter filter = new NodeFilter() {
            @Override public FilterResult head(org.jsoup.nodes.Node node, int depth) {
                if (node instanceof Element && ((Element) node).tagName().equals("p")) {
                    filtered.add(((Element) node).id());
                }
                return FilterResult.CONTINUE;
            }
            @Override public FilterResult tail(org.jsoup.nodes.Node node, int depth) {
                return FilterResult.CONTINUE;
            }
        };
        paras.filter(filter);
        assertEquals(3, filtered.size()); // three <p> elements
        assertTrue(filtered.contains("p1"));
        assertTrue(filtered.contains("p2"));
        assertTrue(filtered.contains("p3"));
    }

    // -------------------------------------------------------------------------
    // Forms extraction
    // -------------------------------------------------------------------------

    @Test
    public void testFormsExtraction() {
        List<FormElement> formList = forms.forms();
        assertEquals(2, formList.size());
        assertEquals("form1", formList.get(0).id());
        assertEquals("form2", formList.get(1).id());
    }
}
###Test END##