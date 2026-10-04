###Test START##
```java
package org.jsoup.select;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator.Tag;
import org.junit.Test;

/**
 * Unit tests for {@link Selector}. The tests cover:
 * <ul>
 *   <li>normal selector parsing and element collection</li>
 *   <li>use of an {@link Evaluator} directly</li>
 *   <li>deduplication when selecting from multiple root elements</li>
 *   <li>the package‑private {@code filterOut} helper</li>
 *   <li>selectFirst behaviour (found / not‑found)</li>
 *   <li>validation of illegal arguments (null / empty)</li>
 *   <li>exception handling for malformed CSS queries</li>
 * </ul>
 */
public class SelectorTest {

    /** Simple HTML used by most tests */
    private static final String HTML = ""
            + "<html><head><title>Test</title></head>"
            + "<body>"
            + "  <div id=\"a\" class=\"c\">Div A</div>"
            + "  <p id=\"b\" class=\"c\">Paragraph B</p>"
            + "  <span id=\"c\">Span C</span>"
            + "  <div id=\"d\"><span id=\"e\">Nested Span</span></div>"
            + "</body></html>";

    /** -------------------------------------------------------------------- *
     *  Test {@link Selector#select(String, Element)} with a normal selector. *
     *  -------------------------------------------------------------------- */
    @Test
    public void select_WithCssQuery_ReturnsMatchingElements() {
        Document doc = Jsoup.parse(HTML);
        Elements found = Selector.select(".c", doc);
        // .c matches the div#a and p#b
        assertEquals(2, found.size());
        assertNotNull(found.select("#a").first());
        assertNotNull(found.select("#b").first());
    }

    /** --------------------------------------------------------------- *
     *  Test {@link Selector#select(Evaluator, Element)} using an Evaluator. *
     *  --------------------------------------------------------------- */
    @Test
    public void select_WithEvaluator_ReturnsMatchingElements() {
        Document doc = Jsoup.parse(HTML);
        Evaluator evaluator = new Tag("div"); // matches all <div> elements
        Elements found = Selector.select(evaluator, doc);
        // there are two <div> elements (id=a and id=d)
        assertEquals(2, found.size());
        assertNotNull(found.select("#a").first());
        assertNotNull(found.select("#d").first());
    }

    /** --------------------------------------------------------------- *
     *  Test {@link Selector#select(String, Iterable)} for deduplication *
     *  when the same element can be reached from multiple roots.         *
     *  --------------------------------------------------------------- */
    @Test
    public void select_MultipleRoots_DeduplicatesElements() {
        Document doc = Jsoup.parse(HTML);
        Element root1 = doc;                     // whole document
        Element root2 = doc.select("div#d").first(); // the inner <div>

        List<Element> roots = Arrays.asList(root1, root2);
        // The selector "span" matches both <span id="c"> and <span id="e">
        Elements result = Selector.select("span", roots);

        // Expect exactly two spans, no duplicates even though root2 is a child of root1
        assertEquals(2, result.size());
        assertNotNull(result.select("#c").first());
        assertNotNull(result.select("#e").first());

        // Add the same root twice – still only two unique spans should be returned
        roots = Arrays.asList(root1, root1, root2);
        Elements resultDup = Selector.select("span", roots);
        assertEquals(2, resultDup.size());
    }

    /** ---------------------------------------------------------------- *
     *  Test the package‑private {@code filterOut} helper.                *
     *  ---------------------------------------------------------------- */
    @Test
    public void filterOut_RemovesElementsPresentInOuts() {
        Document doc = Jsoup.parse(HTML);
        Elements source = doc.select("div, p, span"); // three elements
        Elements outs   = doc.select("p");             // only the <p> should be removed

        Elements filtered = Selector.filterOut(source, outs);
        assertEquals(2, filtered.size());
        assertNotNull(filtered.select("#a").first()); // <div id="a">
        assertNotNull(filtered.select("#c").first()); // <span id="c">
        assertNull(filtered.select("#b").first());    // <p id="b"> must be gone
    }

    /** --------------------------------------------------------------- *
     *  Verify that {@code filterOut} uses {@code equals} (not identity) *
     *  to decide whether an element is filtered.                        *
     *  --------------------------------------------------------------- */
    @Test
    public void filterOut_UsesEquals_NotIdentity() {
        Document doc = Jsoup.parse(HTML);
        Element divA = doc.selectFirst("#a");

        // Create a new Element instance that represents the same underlying node
        // (by cloning the node and then re‑selecting the same id)
        Element divAClone = Jsoup.parse(divA.outerHtml()).selectFirst("#a");

        // The two objects are different instances but are equal according to Element.equals()
        assertNotSame(divA, divAClone);
        assertEquals(divA, divAClone);

        Elements source = new Elements(divA, divAClone);
        Elements outs   = new Elements(divA);

        Elements filtered = Selector.filterOut(source, outs);
        // Both entries should be filtered because they are equal
        assertTrue(filtered.isEmpty());
    }

    /** --------------------------------------------------------------- *
     *  Test {@link Selector#selectFirst(String, Element)} – found case *
     *  --------------------------------------------------------------- */
    @Test
    public void selectFirst_Found_ReturnsFirstMatch() {
        Document doc = Jsoup.parse(HTML);
        Element first = Selector.selectFirst(".c", doc);
        // The first element with class "c" in document order is the <div id="a">
        assertNotNull(first);
        assertEquals("div", first.tagName());
        assertEquals("a", first.id());
    }

    /** --------------------------------------------------------------- *
     *  Test {@link Selector#selectFirst(String, Element)} – not‑found case *
     *  --------------------------------------------------------------- */
    @Test
    public void selectFirst_NotFound_ReturnsNull() {
        Document doc = Jsoup.parse(HTML);
        Element result = Selector.selectFirst(".nonexistent", doc);
        assertNull(result);
    }

    /** --------------------------------------------------------------- *
     *  Verify that malformed CSS queries raise {@link SelectorParseException}
     *  --------------------------------------------------------------- */
    @Test
    public void select_MalformedQuery_ThrowsSelectorParseException() {
        Document doc = Jsoup.parse(HTML);
        try {
            Selector.select("??", doc);
            fail("Expected SelectorParseException to be thrown for malformed query");
        } catch (Selector.SelectorParseException e) {
            // expected – message should contain the offending query fragment
            assertTrue(e.getMessage().contains("??"));
        }
    }

    /** --------------------------------------------------------------- *
     *  Validation – null or empty arguments should cause IllegalArgumentException *
     *  --------------------------------------------------------------- */
    @Test
    public void validation_NullOrEmptyArguments_ThrowIllegalArgumentException() {
        Document doc = Jsoup.parse(HTML);
        Evaluator eval = new Tag("div");

        // select(String, Element)
        try {
            Selector.select(null, doc);
            fail("null query should throw IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}

        try {
            Selector.select("   ", doc);
            fail("empty query should throw IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}

        try {
            Selector.select(".c", null);
            fail("null root should throw IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}

        // select(Evaluator, Element)
        try {
            Selector.select((Evaluator) null, doc);
            fail("null evaluator should throw IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}

        try {
            Selector.select(eval, null);
            fail("null root element should throw IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}

        // select(String, Iterable<Element>)
        try {
            Selector.select(".c", (Iterable<Element>) null);
            fail("null roots iterable should throw IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}

        try {
            Selector.select("", Collections.emptyList());
            fail("empty query for iterable roots should throw IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}

        // selectFirst(String, Element)
        try {
            Selector.selectFirst(null, doc);
            fail("null cssQuery for selectFirst should throw IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}

        try {
            Selector.selectFirst("", doc);
            fail("empty cssQuery for selectFirst should throw IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}

        try {
            Selector.selectFirst(".c", null);
            fail("null root for selectFirst should throw IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {}
    }
}
```
###Test END##