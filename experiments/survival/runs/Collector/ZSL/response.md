###Test START##
```java
package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Collector;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;
import org.jsoup.select.NodeFilter.FilterResult;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit4 test suite for {@link Collector}.
 *
 * The tests cover:
 * <ul>
 *   <li>Correct collection of all matching elements.</li>
 *   <li>Behaviour when no elements match.</li>
 *   <li>Finding the first matching element.</li>
 *   <li>Root element being correctly passed to {@link Evaluator#matches}.</li>
 *   <li>Early termination of traversal in {@code findFirst}.</li>
 *   <li>Null‑root handling (expected to throw NPE).</li>
 * </ul>
 */
public class CollectorTest {

    /** Simple HTML used by most tests. */
    private static final String HTML = ""
            + "<html>"
            + " <head><title>Test</title></head>"
            + " <body>"
            + "   <div id='root'>"
            + "     <p class='first'>Paragraph 1</p>"
            + "     <section>"
            + "       <p class='second'>Paragraph 2</p>"
            + "       <span class='target'>Span 1</span>"
            + "     </section>"
            + "     <p class='third'>Paragraph 3</p>"
            + "     <span class='target'>Span 2</span>"
            + "   </div>"
            + " </body>"
            + "</html>";

    /**
     * Helper to parse the HTML and return the element with id {@code root}.
     */
    private Element getRootElement() {
        Document doc = Jsoup.parse(HTML);
        return doc.getElementById("root");
    }

    /** --------------------------------------------------------------------- *
     *  Test {@link Collector#collect(Evaluator, Element)} returns all matches.
     *  --------------------------------------------------------------------- */
    @Test
    public void testCollectReturnsAllMatchingElements() {
        Element root = getRootElement();

        // Use the built‑in Tag evaluator to match all <p> elements.
        Evaluator pTagEval = new Evaluator.Tag("p");
        Elements result = Collector.collect(pTagEval, root);

        // Expect three <p> elements.
        assertEquals("Should collect exactly three <p> elements", 3, result.size());
        assertEquals("Paragraph 1", result.get(0).text());
        assertEquals("Paragraph 2", result.get(1).text());
        assertEquals("Paragraph 3", result.get(2).text());
    }

    /** --------------------------------------------------------------------- *
     *  Test {@link Collector#collect(Evaluator, Element)} when no element matches.
     *  --------------------------------------------------------------------- */
    @Test
    public void testCollectReturnsEmptyWhenNoMatch() {
        Element root = getRootElement();

        // No <ul> tags exist in the sample HTML.
        Evaluator ulEval = new Evaluator.Tag("ul");
        Elements result = Collector.collect(ulEval, root);

        assertTrue("Result should be empty when no matches are found", result.isEmpty());
    }

    /** --------------------------------------------------------------------- *
     *  Test {@link Collector#findFirst(Evaluator, Element)} returns the first match.
     *  --------------------------------------------------------------------- */
    @Test
    public void testFindFirstReturnsFirstMatch() {
        Element root = getRootElement();

        // Use a class evaluator that matches elements with class "target".
        Evaluator targetClassEval = new Evaluator.Class("target");
        Element first = Collector.findFirst(targetClassEval, root);

        // The first element with class "target" is the <span> inside <section>.
        assertNotNull("First matching element should not be null", first);
        assertEquals("span", first.tagName());
        assertEquals("Span 1", first.text());
    }

    /** --------------------------------------------------------------------- *
     *  Test {@link Collector#findFirst(Evaluator, Element)} returns {@code null}
     *  when no element matches.
     *  --------------------------------------------------------------------- */
    @Test
    public void testFindFirstReturnsNullWhenNoMatch() {
        Element root = getRootElement();

        Evaluator nonExistingEval = new Evaluator.Tag("nonexistent");
        Element result = Collector.findFirst(nonExistingEval, root);

        assertNull("Result should be null when no matching element exists", result);
    }

    /** --------------------------------------------------------------------- *
     *  Verify that the {@code root} argument supplied to {@link Evaluator#matches}
     *  is exactly the root element passed to Collector methods.
     *  --------------------------------------------------------------------- */
    @Test
    public void testEvaluatorReceivesCorrectRoot() {
        final Element root = getRootElement();

        class RootRecordingEvaluator implements Evaluator {
            final List<Element> recordedRoots = new ArrayList<>();

            @Override
            public boolean matches(Element root, Element element) {
                recordedRoots.add(root);
                // Accept every element so that traversal proceeds.
                return true;
            }
        }

        RootRecordingEvaluator eval = new RootRecordingEvaluator();

        // Invoke collect – this will call matches for every element in the tree.
        Collector.collect(eval, root);
        // The root should have been recorded once for each element visited.
        // The HTML under <div id='root'> contains 7 elements (div, 3 p, section, span, span).
        assertEquals("Matches should have been called for each visited element",
                7, eval.recordedRoots.size());

        // Verify that every recorded root is the same instance we passed.
        for (Element recordedRoot : eval.recordedRoots) {
            assertSame("Root passed to matches must be the original root element", root, recordedRoot);
        }
    }

    /** --------------------------------------------------------------------- *
     *  Ensure that {@code findFirst} stops traversal after the first match.
     *  --------------------------------------------------------------------- */
    @Test
    public void testFindFirstStopsAfterFirstMatch() {
        final Element root = getRootElement();

        class CountingEvaluator implements Evaluator {
            int callCount = 0;
            boolean matched = false;

            @Override
            public boolean matches(Element root, Element element) {
                callCount++;
                // Match the very first <p> element only.
                if (!matched && element.tagName().equals("p")) {
                    matched = true;
                    return true;
                }
                return false;
            }
        }

        CountingEvaluator eval = new CountingEvaluator();

        Element first = Collector.findFirst(eval, root);
        assertNotNull("First match should be found", first);
        assertEquals("p", first.tagName());

        // Since traversal is depth‑first, the first <p> is visited before any other <p>.
        // After a match, traversal must stop, so callCount should be exactly 1.
        assertEquals("Evaluator should be called only once because traversal stops after first match",
                1, eval.callCount);
    }

    /** --------------------------------------------------------------------- *
     *  Verify that passing a {@code null} root results in a {@link NullPointerException}.
     *  The Collector API does not guard against null, so this is the expected behaviour.
     *  --------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testCollectWithNullRootThrowsNPE() {
        Evaluator anyEval = new Evaluator.Tag("p");
        Collector.collect(anyEval, null);
    }

    /** --------------------------------------------------------------------- *
     *  Verify that passing a {@code null} root to {@code findFirst} also throws NPE.
     *  --------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testFindFirstWithNullRootThrowsNPE() {
        Evaluator anyEval = new Evaluator.Tag("p");
        Collector.findFirst(anyEval, null);
    }
}
```
###Test END##