###Test START##
package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class SelectorTest {

    private Document doc;
    private Element divRoot;
    private Element pFirst;
    private Element pSecond;
    private Element span;

    @Before
    public void setUp() {
        String html = "<html><head><title>Test</title></head>"
                + "<body>"
                + "<div id='root'>"
                + "  <p class='first'>First</p>"
                + "  <p class='second'>Second</p>"
                + "  <span class='only'>Only</span>"
                + "</div>"
                + "</body></html>";
        doc = Jsoup.parse(html);
        divRoot = doc.selectFirst("div#root");
        pFirst = doc.selectFirst("p.first");
        pSecond = doc.selectFirst("p.second");
        span = doc.selectFirst("span.only");
    }

    /*** select(String, Element) ***/
    @Test
    public void testSelectByQuerySimple() {
        Elements els = Selector.select("p.first", divRoot);
        assertEquals(1, els.size());
        assertEquals(pFirst, els.first());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSelectByQueryEmptyStringThrows() {
        Selector.select("", divRoot);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSelectByQueryNullStringThrows() {
        Selector.select(null, divRoot);
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void testSelectByQueryInvalidSelectorThrows() {
        Selector.select("??", divRoot);
    }

    /*** select(Evaluator, Element) ***/
    @Test
    public void testSelectByEvaluatorTag() {
        Evaluator evaluator = new Evaluator.Tag("p");
        Elements els = Selector.select(evaluator, divRoot);
        assertEquals(2, els.size());
        assertTrue(els.contains(pFirst));
        assertTrue(els.contains(pSecond));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSelectByEvaluatorNullEvaluatorThrows() {
        Selector.select((Evaluator) null, divRoot);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSelectByEvaluatorNullRootThrows() {
        Evaluator evaluator = new Evaluator.Tag("p");
        Selector.select(evaluator, (Element) null);
    }

    /*** select(String, Iterable<Element>) ***/
    @Test
    public void testSelectMultipleRootsDeduplication() {
        // root1 contains the <p class='dup'> element, root2 is the same <p> element itself.
        String html = "<div id='parent'><p class='dup'>Dup</p></div>";
        Document d = Jsoup.parse(html);
        Element parent = d.selectFirst("div#parent");
        Element dup = d.selectFirst("p.dup");

        List<Element> roots = Arrays.asList(parent, dup);
        Elements result = Selector.select("p.dup", roots);
        assertEquals(1, result.size());
        assertEquals(dup, result.first());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSelectMultipleRootsNullQueryThrows() {
        Selector.select(null, Collections.singletonList(divRoot));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSelectMultipleRootsEmptyQueryThrows() {
        Selector.select("", Collections.singletonList(divRoot));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSelectMultipleRootsNullRootsThrows() {
        Selector.select("p", null);
    }

    @Test
    public void testSelectMultipleRootsNormalCase() {
        Elements result = Selector.select("p", Arrays.asList(pFirst, pSecond));
        // each root is a <p> element, selector "p" matches the root itself.
        assertEquals(2, result.size());
        assertTrue(result.contains(pFirst));
        assertTrue(result.contains(pSecond));
    }

    /*** filterOut ***/
    @Test
    public void testFilterOutRemovesMatchingElements() {
        Collection<Element> all = Arrays.asList(pFirst, pSecond, span);
        Collection<Element> outs = Collections.singleton(pSecond);
        Elements filtered = Selector.filterOut(all, outs);
        assertEquals(2, filtered.size());
        assertTrue(filtered.contains(pFirst));
        assertTrue(filtered.contains(span));
        assertFalse(filtered.contains(pSecond));
    }

    @Test
    public void testFilterOutWhenNoMatchReturnsOriginal() {
        Collection<Element> all = Arrays.asList(pFirst, span);
        Collection<Element> outs = Collections.singleton(pSecond); // not present
        Elements filtered = Selector.filterOut(all, outs);
        assertEquals(2, filtered.size());
        assertTrue(filtered.containsAll(all));
    }

    /*** selectFirst ***/
    @Test
    public void testSelectFirstReturnsFirstMatch() {
        Element first = Selector.selectFirst("p", divRoot);
        assertNotNull(first);
        // According to document order, pFirst appears before pSecond
        assertEquals(pFirst, first);
    }

    @Test
    public void testSelectFirstReturnsNullWhenNoMatch() {
        Element result = Selector.selectFirst("ul", divRoot);
        assertNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSelectFirstEmptyQueryThrows() {
        Selector.selectFirst("", divRoot);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSelectFirstNullQueryThrows() {
        Selector.selectFirst(null, divRoot);
    }

    /*** Additional sanity checks ***/
    @Test
    public void testSelectByQueryCaseInsensitive() {
        // Selector is case‑insensitive; "P.FIRST" should match the same element.
        Elements els = Selector.select("P.FIRST", divRoot);
        assertEquals(1, els.size());
        assertEquals(pFirst, els.first());
    }

    @Test
    public void testSelectByEvaluatorClass() {
        Evaluator evaluator = new Evaluator.Class("only");
        Elements els = Selector.select(evaluator, divRoot);
        assertEquals(1, els.size());
        assertEquals(span, els.first());
    }
}
###Test END##