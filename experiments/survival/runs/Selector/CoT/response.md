###Test START##
```java
package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator.Tag;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class SelectorTest {

    private Document doc;
    private Element divA;
    private Element divB;
    private Element p1;
    private Element p2;

    @Before
    public void setUp() {
        // simple HTML used by almost all tests
        String html = "<html><head><title>Test</title></head>"
                + "<body>"
                + "  <div id='a' class='foo'>"
                + "    <p id='p1'>Hello</p>"
                + "    <p id='p2' class='bar'>World</p>"
                + "  </div>"
                + "  <div id='b' class='foo'>"
                + "    <p>Another</p>"
                + "  </div>"
                + "</body></html>";
        doc = Jsoup.parse(html);
        divA = doc.selectFirst("div#a");
        divB = doc.selectFirst("div#b");
        p1 = doc.selectFirst("p#p1");
        p2 = doc.selectFirst("p#p2");
    }

    /*** 1. public static Elements select(String query, Element root) ***/

    @Test
    public void select_StringElement_basicMatch() {
        Elements els = Selector.select("p.bar", divA);
        assertEquals(1, els.size());
        assertEquals(p2, els.first());
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_StringElement_nullQuery() {
        Selector.select(null, divA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_StringElement_emptyQuery() {
        Selector.select("   ", divA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_StringElement_nullRoot() {
        Selector.select("p", null);
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void select_StringElement_invalidQueryThrows() {
        // an obviously malformed selector
        Selector.select("div..foo", divA);
    }

    /*** 2. public static Elements select(Evaluator evaluator, Element root) ***/

    @Test
    public void select_Evaluator_basicMatch() {
        Evaluator ev = new Tag("p");
        Elements els = Selector.select(ev, divA);
        assertEquals(2, els.size());
        assertTrue(els.contains(p1));
        assertTrue(els.contains(p2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_Evaluator_nullEvaluator() {
        Selector.select((Evaluator) null, divA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_Evaluator_nullRoot() {
        Selector.select(new Tag("p"), null);
    }

    /*** 3. public static Elements select(String query, Iterable<Element> roots) ***/

    @Test
    public void select_StringIterable_basicMatch() {
        List<Element> roots = List.of(divA, divB);
        Elements els = Selector.select("p", roots);
        // 3 <p> elements in total
        assertEquals(3, els.size());
        assertTrue(els.contains(p1));
        assertTrue(els.contains(p2));
        assertNotNull(els.selectFirst("p:contains(Another)"));
    }

    @Test
    public void select_StringIterable_deduplication() {
        // both roots contain the same element (divA is added twice)
        List<Element> roots = List.of(divA, divA);
        Elements els = Selector.select("p", roots);
        // deduplication should give only the two distinct <p> elements
        assertEquals(2, els.size());
        assertTrue(els.contains(p1));
        assertTrue(els.contains(p2));
    }

    @Test
    public void select_StringIterable_emptyRoots() {
        Elements els = Selector.select("p", Collections.emptyList());
        assertTrue(els.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_StringIterable_nullQuery() {
        Selector.select(null, List.of(divA));
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_StringIterable_emptyQuery() {
        Selector.select("   ", List.of(divA));
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_StringIterable_nullRoots() {
        Selector.select("p", null);
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void select_StringIterable_invalidQueryThrows() {
        Selector.select("div..foo", List.of(divA));
    }

    /*** 4. static Elements filterOut(Collection<Element> elements, Collection<Element> outs) ***/

    @Test
    public void filterOut_removesMatches() {
        List<Element> all = List.of(p1, p2, divA);
        List<Element> outs = List.of(p2);
        Elements result = Selector.filterOut(all, outs);
        assertEquals(2, result.size());
        assertTrue(result.contains(p1));
        assertTrue(result.contains(divA));
        assertFalse(result.contains(p2));
    }

    @Test
    public void filterOut_noMatchesReturnsOriginal() {
        List<Element> all = List.of(p1, p2);
        List<Element> outs = List.of(divA);
        Elements result = Selector.filterOut(all, outs);
        assertEquals(2, result.size());
        assertTrue(result.contains(p1));
        assertTrue(result.contains(p2));
    }

    @Test
    public void filterOut_emptyElements() {
        Elements result = Selector.filterOut(Collections.emptyList(), List.of(p1));
        assertTrue(result.isEmpty());
    }

    /*** 5. public static Element selectFirst(String cssQuery, Element root) ***/

    @Test
    public void selectFirst_basicMatch() {
        Element first = Selector.selectFirst("p", divA);
        assertEquals(p1, first);
    }

    @Test
    public void selectFirst_noMatchReturnsNull() {
        Element el = Selector.selectFirst("span", divA);
        assertNull(el);
    }

    @Test(expected = IllegalArgumentException.class)
    public void selectFirst_nullQuery() {
        Selector.selectFirst(null, divA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void selectFirst_emptyQuery() {
        Selector.selectFirst("   ", divA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void selectFirst_nullRoot() {
        Selector.selectFirst("p", null);
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void selectFirst_invalidQueryThrows() {
        Selector.selectFirst("div..foo", divA);
    }

    /*** 6. public static class SelectorParseException ***/

    @Test
    public void selectorParseExceptionMessageFormatting() {
        Selector.SelectorParseException ex = new Selector.SelectorParseException(
                "Invalid selector %s at position %d", "div..foo", 4);
        assertEquals("Invalid selector div..foo at position 4", ex.getMessage());
    }
}
```
###Test END##