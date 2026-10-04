package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link Collector}.
 *
 * The suite covers:
 * 1. Extraction of all public methods.
 * 2. Basic functional tests for each method.
 * 3. Edge‑case and exception handling tests.
 * 4. Comprehensive test file that can be run directly.
 *
 * The tests use simple HTML snippets parsed with jsoup and custom
 * {@link Evaluator} implementations (lambda expressions or anonymous
 * classes) to verify the behaviour of {@code collect} and {@code findFirst}.
 */
public class CollectorTest {

    /* --------------------------------------------------------------------
     *  PUBLIC METHOD LIST (extracted from Collector)
     * --------------------------------------------------------------------
     *
     *  public static Elements collect(Evaluator eval, Element root)
     *  public static Element  findFirst(Evaluator eval, Element root)
     *
     * --------------------------------------------------------------------
     */

    /*** BASIC FUNCTIONAL TESTS ***/

    @Test
    public void testCollectBasicMatch() {
        String html = "<div id='root'><p class='match'>One</p><p>Two</p></div>";
        Element root = Jsoup.parse(html).body().selectFirst("div");

        // Evaluator that matches elements having class "match"
        Evaluator eval = (r, el) -> el.hasClass("match");

        Elements result = Collector.collect(eval, root);
        assertEquals("Exactly one element should be collected", 1, result.size());
        Element matched = result.get(0);
        assertEquals("Tag name should be <p>", "p", matched.tagName());
        assertTrue("Element should have class \"match\"", matched.hasClass("match"));
    }

    @Test
    public void testCollectNoMatches() {
        String html = "<div><span></span></div>";
        Element root = Jsoup.parse(html).body().selectFirst("div");

        // Evaluator that never matches
        Evaluator eval = (r, el) -> false;

        Elements result = Collector.collect(eval, root);
        assertTrue("Result should be empty when no elements match", result.isEmpty());
    }

    @Test
    public void testCollectMatchesRootElement() {
        String html = "<section id='root'></section>";
        Element root = Jsoup.parse(html).body().selectFirst("section");

        // Evaluator that matches only the root element itself
        Evaluator eval = (r, el) -> el == r;

        Elements result = Collector.collect(eval, root);
        assertEquals("Root element should be collected", 1, result.size());
        assertSame("Collected element must be the root", root, result.get(0));
    }

    @Test
    public void testFindFirstBasicMatch() {
        String html = "<ul><li class='a'>A</li><li class='b'>B</li><li class='a'>C</li></ul>";
        Element root = Jsoup.parse(html).body().selectFirst("ul");

        // Evaluator that matches elements with class "a"
        Evaluator eval = (r, el) -> el.hasClass("a");

        Element match = Collector.findFirst(eval, root);
        assertNotNull("First matching element should be returned", match);
        assertEquals("Tag should be <li>", "li", match.tagName());
        assertEquals("First matched text should be \"A\"", "A", match.text());
    }

    @Test
    public void testFindFirstNoMatch() {
        String html = "<div><p></p></div>";
        Element root = Jsoup.parse(html).body().selectFirst("div");

        // Evaluator that never matches
        Evaluator eval = (r, el) -> false;

        Element match = Collector.findFirst(eval, root);
        assertNull("When there is no match, null should be returned", match);
    }

    @Test
    public void testFindFirstMatchesRootElement() {
        String html = "<article id='root'></article>";
        Element root = Jsoup.parse(html).body().selectFirst("article");

        // Evaluator that matches only the root element itself
        Evaluator eval = (r, el) -> el == r;

        Element match = Collector.findFirst(eval, root);
        assertNotNull("Root should be returned when it matches", match);
        assertSame("Returned element must be the root", root, match);
    }

    @Test
    public void testFindFirstStopsAfterFirstMatch() {
        String html = "<div>"
                    + "  <span class='stop'>First</span>"
                    + "  <span class='stop'>Second</span>"
                    + "</div>";
        Element root = Jsoup.parse(html).body().selectFirst("div");

        // Counter to verify that evaluator is invoked only until first match
        final int[] invocationCount = {0};
        Evaluator eval = (r, el) -> {
            if (el.hasClass("stop")) {
                invocationCount[0]++;   // count only when a match occurs
                return true;
            }
            return false;
        };

        Element match = Collector.findFirst(eval, root);
        assertNotNull("First matching element must be found", match);
        assertEquals("Returned element should be the first <span>", "First", match.text());
        assertEquals("Evaluator should be called only once (traversal stops)", 1, invocationCount[0]);
    }

    /*** EDGE‑CASE & EXCEPTION TESTS ***/

    @Test(expected = NullPointerException.class)
    public void testCollectNullEvaluator() {
        Element root = Jsoup.parse("<div></div>").body().selectFirst("div");
        Collector.collect(null, root);               // should throw NPE
    }

    @Test(expected = NullPointerException.class)
    public void testCollectNullRoot() {
        Evaluator eval = (r, el) -> true;
        Collector.collect(eval, null);               // should throw NPE
    }

    @Test(expected = NullPointerException.class)
    public void testFindFirstNullEvaluator() {
        Element root = Jsoup.parse("<div></div>").body().selectFirst("div");
        Collector.findFirst(null, root);              // should throw NPE
    }

    @Test(expected = NullPointerException.class)
    public void testFindFirstNullRoot() {
        Evaluator eval = (r, el) -> true;
        Collector.findFirst(eval, null);              // should throw NPE
    }

    /*** ADDITIONAL VALIDATION ***/

    @Test
    public void testCollectWithComplexEvaluator() {
        String html = "<div id='root'>"
                    + "  <p class='a'>A</p>"
                    + "  <p class='b'>B</p>"
                    + "  <span class='a'>C</span>"
                    + "</div>";
        Element root = Jsoup.parse(html).body().selectFirst("div");

        // Evaluator that matches elements that have class "a" AND are NOT <span>
        Evaluator eval = (r, el) -> el.hasClass("a") && !"span".equals(el.tagName());

        Elements result = Collector.collect(eval, root);
        assertEquals("Only one <p class='a'> should be collected", 1, result.size());
        assertEquals("Tag of collected element should be <p>", "p", result.get(0).tagName());
    }
}
