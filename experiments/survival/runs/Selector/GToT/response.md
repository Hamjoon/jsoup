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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class SelectorTest {

    private Document doc;
    private Element body;
    private Element div1;
    private Element div2;
    private Element p1;
    private Element p2;
    private Element span;

    /**
     * Build a tiny HTML document used by most tests.
     *
     * <pre>
     * &lt;html&gt;
     *   &lt;body&gt;
     *     &lt;div id="d1"&gt;
     *       &lt;p class="c1"&gt;Paragraph 1&lt;/p&gt;
     *       &lt;span&gt;Span 1&lt;/span&gt;
     *     &lt;/div&gt;
     *     &lt;div id="d2"&gt;
     *       &lt;p class="c2"&gt;Paragraph 2&lt;/p&gt;
     *     &lt;/div&gt;
     *   &lt;/body&gt;
     * &lt;/html&gt;
     * </pre>
     */
    @Before
    public void setUp() {
        String html = "<html><body>"
                + "<div id='d1'><p class='c1'>Paragraph 1</p><span>Span 1</span></div>"
                + "<div id='d2'><p class='c2'>Paragraph 2</p></div>"
                + "</body></html>";
        doc = Jsoup.parse(html);
        body = doc.body();
        div1 = doc.getElementById("d1");
        div2 = doc.getElementById("d2");
        p1 = div1.selectFirst("p");
        p2 = div2.selectFirst("p");
        span = div1.selectFirst("span");
    }

    /* --------------------------------------------------------------------- */
    /* 1. public static Elements select(String query, Element root)          */
    /* --------------------------------------------------------------------- */

    @Test
    public void select_StringRoot_ReturnsMatchingElements() {
        Elements divs = Selector.select("div", body);
        assertEquals(2, divs.size());
        assertTrue(divs.contains(div1));
        assertTrue(divs.contains(div2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_StringRoot_EmptyQuery_Throws() {
        Selector.select("", body);
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_StringRoot_NullRoot_Throws() {
        Selector.select("div", null);
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void select_StringRoot_InvalidCss_ThrowsSelectorParseException() {
        // "???" is not a valid CSS selector and forces QueryParser to fail.
        Selector.select("???", body);
    }

    /* --------------------------------------------------------------------- */
    /* 2. public static Elements select(Evaluator evaluator, Element root)   */
    /* --------------------------------------------------------------------- */

    @Test
    public void select_EvaluatorRoot_ReturnsMatchingElements() {
        Evaluator evaluator = new Tag("p"); // selects all <p> elements
        Elements paragraphs = Selector.select(evaluator, body);
        assertEquals(2, paragraphs.size());
        assertTrue(paragraphs.contains(p1));
        assertTrue(paragraphs.contains(p2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_EvaluatorRoot_NullEvaluator_Throws() {
        Selector.select((Evaluator) null, body);
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_EvaluatorRoot_NullRoot_Throws() {
        Selector.select(new Tag("div"), null);
    }

    /* --------------------------------------------------------------------- */
    /* 3. public static Elements select(String query, Iterable<Element> roots) */
    /* --------------------------------------------------------------------- */

    @Test
    public void select_StringIterableRoots_DedupeElements() {
        // roots: body and div1 – both contain the same <span> element.
        List<Element> roots = Arrays.asList(body, div1);
        Elements spans = Selector.select("span", roots);
        // Only one span should be present despite being reachable from both roots.
        assertEquals(1, spans.size());
        assertEquals(span, spans.first());
    }

    @Test
    public void select_StringIterableRoots_EmptyIterable_ReturnsEmptyElements() {
        Elements result = Selector.select("div", Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_StringIterableRoots_NullQuery_Throws() {
        Selector.select(null, Arrays.asList(body));
    }

    @Test(expected = IllegalArgumentException.class)
    public void select_StringIterableRoots_NullRoots_Throws() {
        Selector.select("div", null);
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void select_StringIterableRoots_InvalidCss_ThrowsSelectorParseException() {
        Selector.select("!@#$", Arrays.asList(body));
    }

    /* --------------------------------------------------------------------- */
    /* 4. static Elements filterOut(Collection<Element> elements, Collection<Element> outs) */
    /* --------------------------------------------------------------------- */

    @Test
    public void filterOut_RemovesElementsPresentInOuts() {
        List<Element> all = new ArrayList<>(Arrays.asList(div1, div2, p1, p2, span));
        List<Element> toRemove = Arrays.asList(p1, span);
        Elements filtered = Selector.filterOut(all, toRemove);
        assertEquals(3, filtered.size());
        assertTrue(filtered.contains(div1));
        assertTrue(filtered.contains(div2));
        assertTrue(filtered.contains(p2));
        assertFalse(filtered.contains(p1));
        assertFalse(filtered.contains(span));
    }

    @Test
    public void filterOut_NoOverlap_ReturnsOriginalCollection() {
        List<Element> all = Arrays.asList(div1, div2);
        List<Element> toRemove = Arrays.asList(p1, p2);
        Elements filtered = Selector.filterOut(all, toRemove);
        assertEquals(2, filtered.size());
        assertTrue(filtered.contains(div1));
        assertTrue(filtered.contains(div2));
    }

    @Test
    public void filterOut_EmptyOuts_ReturnsAllElements() {
        List<Element> all = Arrays.asList(div1, p1);
        Elements filtered = Selector.filterOut(all, Collections.emptyList());
        assertEquals(2, filtered.size());
        assertTrue(filtered.contains(div1));
        assertTrue(filtered.contains(p1));
    }

    @Test(expected = NullPointerException.class)
    public void filterOut_NullElements_ThrowsNPE() {
        Selector.filterOut(null, Collections.emptyList());
    }

    @Test(expected = NullPointerException.class)
    public void filterOut_NullOuts_ThrowsNPE() {
        Selector.filterOut(Arrays.asList(div1), null);
    }

    /* --------------------------------------------------------------------- */
    /* 5. public static Element selectFirst(String cssQuery, Element root)  */
    /* --------------------------------------------------------------------- */

    @Test
    public void selectFirst_ValidQuery_ReturnsFirstMatch() {
        Element firstDiv = Selector.selectFirst("div", body);
        assertNotNull(firstDiv);
        // In the test HTML the first <div> is the one with id="d1".
        assertEquals("d1", firstDiv.id());
    }

    @Test
    public void selectFirst_NoMatch_ReturnsNull() {
        Element result = Selector.selectFirst("section", body);
        assertNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void selectFirst_EmptyQuery_Throws() {
        Selector.selectFirst("", body);
    }

    @Test(expected = IllegalArgumentException.class)
    public void selectFirst_NullRoot_Throws() {
        Selector.selectFirst("div", null);
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void selectFirst_InvalidCss_ThrowsSelectorParseException() {
        Selector.selectFirst("??invalid??", body);
    }

    /* --------------------------------------------------------------------- */
    /* 6. Verify that SelectorParseException carries a formatted message   */
    /* --------------------------------------------------------------------- */

    @Test
    public void selectorParseException_MessageFormatting() {
        Selector.SelectorParseException ex = new Selector.SelectorParseException(
                "Error parsing selector %s at position %d", "div>", 3);
        assertEquals("Error parsing selector div> at position 3", ex.getMessage());
    }
}
```
###Test END##