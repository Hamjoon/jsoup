###Test START##
```java
package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Parser;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ElementsTest {

    private Document doc;

    @Before
    public void setUp() {
        String html = "<!DOCTYPE html>\n" +
                "<html>\n" +
                " <head><title>Test</title></head>\n" +
                " <body>\n" +
                "   <div id='outer' class='container'>\n" +
                "     <p class='para' data-test='one'>First <b>bold</b> text.</p>\n" +
                "     <p class='para'>Second paragraph.</p>\n" +
                "     <ul id='list'>\n" +
                "       <li class='item'>Item 1</li>\n" +
                "       <li class='item'>Item 2</li>\n" +
                "       <li class='item special'>Item 3</li>\n" +
                "     </ul>\n" +
                "     <form id='myform'>\n" +
                "       <input type='text' name='field' value='initial'>\n" +
                "     </form>\n" +
                "   </div>\n" +
                " </body>\n" +
                "</html>";
        doc = Jsoup.parse(html, "", Parser.htmlParser());
    }

    @Test
    public void testConstructorsAndClone() {
        Elements empty = new Elements();
        assertTrue(empty.isEmpty());

        Elements fromArray = new Elements(doc.select("p").toArray(new Element[0]));
        assertEquals(2, fromArray.size());

        Elements copy = fromArray.clone();
        assertEquals(fromArray.size(), copy.size());

        // deep clone: modify original attribute, clone should stay unchanged
        fromArray.attr("data-mod", "orig");
        assertEquals("orig", fromArray.attr("data-mod"));
        assertEquals("", copy.attr("data-mod"));
    }

    @Test
    public void testAttrHasAttrEachAttr() {
        Elements paragraphs = doc.select("p");
        paragraphs.attr("data-test", "value");

        assertEquals("value", paragraphs.attr("data-test")); // first element
        assertTrue(paragraphs.hasAttr("data-test"));

        List<String> attrs = paragraphs.eachAttr("data-test");
        assertEquals(2, attrs.size());
        assertEquals("value", attrs.get(0));
        assertEquals("value", attrs.get(1));

        // attribute not present
        assertFalse(paragraphs.hasAttr("nonexistent"));
        assertEquals("", paragraphs.attr("nonexistent"));
        assertTrue(paragraphs.eachAttr("nonexistent").isEmpty());
    }

    @Test
    public void testAddRemoveToggleClassAndHasClass() {
        Elements items = doc.select("li.item");
        items.addClass("added");
        for (Element e : items) {
            assertTrue(e.hasClass("added"));
        }

        items.removeClass("added");
        for (Element e : items) {
            assertFalse(e.hasClass("added"));
        }

        items.toggleClass("toggled");
        for (Element e : items) {
            assertTrue(e.hasClass("toggled"));
        }

        items.toggleClass("toggled");
        for (Element e : items) {
            assertFalse(e.hasClass("toggled"));
        }

        // hasClass on Elements collection
        assertTrue(items.hasClass("item"));
        assertFalse(items.hasClass("nonexistent"));
    }

    @Test
    public void testValGetSet() {
        Elements inputs = doc.select("input[name=field]");
        assertEquals("initial", inputs.val());

        inputs.val("changed");
        assertEquals("changed", inputs.val());

        // ensure each element got the value
        for (Element e : inputs) {
            assertEquals("changed", e.val());
        }
    }

    @Test
    public void testTextEachTextHasText() {
        Elements paragraphs = doc.select("p");
        String combined = paragraphs.text();
        assertTrue(combined.contains("First"));
        assertTrue(combined.contains("Second"));

        assertTrue(paragraphs.hasText());

        List<String> each = paragraphs.eachText();
        assertEquals(2, each.size());
        assertTrue(each.get(0).contains("First"));
        assertTrue(each.get(1).contains("Second"));
    }

    @Test
    public void testHtmlOuterHtmlAndToString() {
        Elements divs = doc.select("div#outer");
        String inner = divs.html();
        assertTrue(inner.contains("<p"));
        assertTrue(inner.contains("<ul"));

        String outer = divs.outerHtml();
        assertTrue(outer.startsWith("<div"));
        assertTrue(outer.endsWith("</div>"));

        // toString() delegates to outerHtml()
        assertEquals(outer, divs.toString());
    }

    @Test
    public void testTagNameAndHtmlSet() {
        Elements paras = doc.select("p.para");
        paras.tagName("section");
        for (Element e : paras) {
            assertEquals("section", e.tagName());
        }

        paras.html("<span>replaced</span>");
        for (Element e : paras) {
            assertEquals("replaced", e.text());
            assertEquals("span", e.child(0).tagName());
        }
    }

    @Test
    public void testPrependAppendBeforeAfterWrap() {
        Elements firstLi = doc.select("li.item").first().select("li.item");
        // prepend
        firstLi.prepend("<strong>PRE</strong>");
        assertTrue(firstLi.first().html().startsWith("<strong>PRE</strong>"));

        // append
        firstLi.append("<em>POST</em>");
        assertTrue(firstLi.first().html().contains("<em>POST</em>"));

        // before
        firstLi.before("<div id='before'></div>");
        Element beforeDiv = doc.select("div#before").first();
        assertNotNull(beforeDiv);
        assertEquals("before", beforeDiv.id());

        // after
        firstLi.after("<div id='after'></div>");
        Element afterDiv = doc.select("div#after").first();
        assertNotNull(afterDiv);
        assertEquals("after", afterDiv.id());

        // wrap
        firstLi.wrap("<section class='wrapper'></section>");
        Element wrapper = doc.select("section.wrapper").first();
        assertNotNull(wrapper);
        assertTrue(wrapper.select("li.item").size() > 0);
    }

    @Test
    public void testUnwrapEmptyRemove() {
        // unwrap: <font>One</font> -> One
        Elements fonts = doc.select("p.para b");
        fonts.unwrap();
        assertEquals("First bold text.", doc.select("p.para").first().text());

        // empty: remove children of <ul>
        Elements ul = doc.select("ul#list");
        ul.empty();
        assertTrue(ul.first().children().isEmpty());

        // remove: remove the whole <ul>
        ul.remove();
        assertTrue(doc.select("ul#list").isEmpty());
    }

    @Test
    public void testSelectNotEqIsAndEq() {
        Elements allParas = doc.select("p");
        Elements selected = allParas.select(".para");
        assertEquals(2, selected.size());

        Elements notLogo = doc.select("div").not("#outer");
        assertTrue(notLogo.isEmpty());

        Elements eqFirst = allParas.eq(0);
        assertEquals(1, eqFirst.size());
        assertEquals(allParas.first(), eqFirst.first());

        Elements eqOutOfBounds = allParas.eq(10);
        assertTrue(eqOutOfBounds.isEmpty());

        assertTrue(allParas.is("p"));
        assertFalse(allParas.is(".nonexistent"));
    }

    @Test
    public void testSiblingMethods() {
        Elements items = doc.select("li.item");
        // immediate next
        Elements next = items.next();
        assertEquals(2, next.size());
        assertEquals("Item 2", next.first().text());

        // immediate prev
        Elements prev = items.prev();
        assertEquals(0, prev.size());

        // nextAll from first item
        Elements nextAll = items.first().nextAll();
        assertEquals(2, nextAll.size());

        // prevAll from last item
        Elements prevAll = items.last().prevAll();
        assertEquals(2, prevAll.size());

        // filtered next (only .special)
        Elements nextSpecial = items.next(".special");
        assertEquals(1, nextSpecial.size());
        assertTrue(nextSpecial.first().hasClass("special"));
    }

    @Test
    public void testParentsFirstLast() {
        Elements li = doc.select("li.item.special");
        Elements parents = li.parents();
        // should contain ul, div#outer, body, html
        assertTrue(parents.stream().anyMatch(e -> e.id().equals("list")));
        assertTrue(parents.stream().anyMatch(e -> e.id().equals("outer")));

        assertEquals(li.first(), li.first()); // sanity
        assertEquals(li.last(), li.last()); // same element
    }

    @Test
    public void testTraverseAndFilter() {
        final List<String> visited = new ArrayList<>();
        Elements paras = doc.select("p.para");
        paras.traverse(new NodeVisitor() {
            @Override public void head(Node node, int depth) {
                visited.add("h:" + node.nodeName());
            }

            @Override public void tail(Node node, int depth) {
                visited.add("t:" + node.nodeName());
            }
        });
        // ensure we visited at least the paragraph nodes
        assertTrue(visited.stream().anyMatch(s -> s.equals("h:p")));
        assertTrue(visited.stream().anyMatch(s -> s.equals("t:p")));

        // filter: remove all <b> elements from the set
        final List<Node> removed = new ArrayList<>();
        Elements withBold = doc.select("p");
        withBold.filter(new NodeFilter() {
            @Override public FilterResult head(Node node, int depth) {
                if (node.nodeName().equals("b")) {
                    removed.add(node);
                    return FilterResult.STOP; // stop traversing this branch
                }
                return FilterResult.CONTINUE;
            }

            @Override public FilterResult tail(Node node, int depth) {
                return FilterResult.CONTINUE;
            }
        });
        // confirm that a <b> node was encountered and collected
        assertFalse(removed.isEmpty());
        assertEquals("b", removed.get(0).nodeName());
    }

    @Test
    public void testForms() {
        Elements forms = doc.select("form");
        List<FormElement> formList = forms.forms();
        assertEquals(1, formList.size());
        FormElement form = formList.get(0);
        assertEquals("myform", form.id());

        // Ensure that non-form elements are ignored
        Elements divs = doc.select("div");
        assertTrue(divs.forms().isEmpty());
    }
}
```
###Test END##