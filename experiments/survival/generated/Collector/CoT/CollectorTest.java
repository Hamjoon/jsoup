package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.junit.Assert;

/**
 * JUnit 4 test cases for {@link Collector}.
 *
 * The tests cover:
 * <ul>
 *   <li>Basic functionality of {@code collect} and {@code findFirst}</li>
 *   <li>Edge‑cases such as null arguments, empty matches, and root element matching</li>
 *   <li>Verification that {@code findFirst} stops after the first match</li>
 * </ul>
 */
public class CollectorTest {

    /** Simple evaluator that matches elements by a CSS class name. */
    private static class ClassMatcher implements Evaluator {
        private final String className;

        ClassMatcher(String className) {
            this.className = className;
        }

        @Override
        public boolean matches(Element root, Element element) {
            return element.hasClass(className);
        }

        @Override
        public String toString() {
            return "ClassMatcher(" + className + ")";
        }
    }

    /** Evaluator that matches elements by tag name. */
    private static class TagMatcher implements Evaluator {
        private final String tagName;

        TagMatcher(String tagName) {
            this.tagName = tagName;
        }

        @Override
        public boolean matches(Element root, Element element) {
            return element.tagName().equalsIgnoreCase(tagName);
        }

        @Override
        public String toString() {
            return "TagMatcher(" + tagName + ")";
        }
    }

    /** Helper to build a simple document used by many tests. */
    private Element buildSampleRoot() {
        String html = "<div id='root'>"
                + "  <p class='match'>Paragraph 1</p>"
                + "  <span class='match'>Span 1</span>"
                + "  <p>Paragraph 2</p>"
                + "  <div class='inner'>"
                + "      <p class='match'>Paragraph 3</p>"
                + "  </div>"
                + "</div>";
        Document doc = Jsoup.parse(html);
        // The element with id='root' is the entry point for traversal.
        return doc.getElementById("root");
    }

    /** -------------------------------------------------------------------- *
     *  BASIC FUNCTIONALITY TESTS                                            *
     *  -------------------------------------------------------------------- */

    @Test
    public void testCollectReturnsAllMatchingElements() {
        Element root = buildSampleRoot();
        Evaluator eval = new ClassMatcher("match");

        Elements result = Collector.collect(eval, root);

        // Expect 4 elements: three <p> and one <span> having class="match"
        Assert.assertEquals("Number of matched elements", 4, result.size());
        for (Element el : result) {
            Assert.assertTrue("Each element must have class 'match'",
                    el.hasClass("match"));
        }
    }

    @Test
    public void testFindFirstReturnsFirstMatchInDocumentOrder() {
        Element root = buildSampleRoot();
        Evaluator eval = new ClassMatcher("match");

        Element first = Collector.findFirst(eval, root);

        // The first element with class="match" is the first <p>
        Assert.assertNotNull("First match should not be null", first);
        Assert.assertEquals("First matched element tag", "p", first.tagName());
        Assert.assertEquals("First matched element text", "Paragraph 1", first.text());
    }

    /** -------------------------------------------------------------------- *
     *  EDGE CASES                                                          *
     *  -------------------------------------------------------------------- */

    @Test
    public void testCollectWhenNoElementsMatchReturnsEmptyElements() {
        Element root = buildSampleRoot();
        Evaluator eval = new ClassMatcher("nonexistent");

        Elements result = Collector.collect(eval, root);
        Assert.assertTrue("Result should be empty when no matches", result.isEmpty());
    }

    @Test
    public void testFindFirstWhenNoElementsMatchReturnsNull() {
        Element root = buildSampleRoot();
        Evaluator eval = new ClassMatcher("nonexistent");

        Element first = Collector.findFirst(eval, root);
        Assert.assertNull("findFirst should return null when nothing matches", first);
    }

    @Test
    public void testCollectRootMatchesItself() {
        Element root = buildSampleRoot();
        // The root <div id='root'> has class 'inner' on a child, but we match tag "div"
        Evaluator eval = new TagMatcher("div");

        Elements result = Collector.collect(eval, root);

        // Both the root <div> and the inner <div class='inner'> should be collected
        Assert.assertEquals("Two <div> elements expected (root + inner)", 2, result.size());
        Assert.assertTrue("Result must contain the root element", result.contains(root));
    }

    @Test
    public void testFindFirstRootMatchesItself() {
        Element root = buildSampleRoot();
        Evaluator eval = new TagMatcher("div");

        Element first = Collector.findFirst(eval, root);

        // The root element is the first <div> encountered in depth‑first order
        Assert.assertNotNull(first);
        Assert.assertSame("findFirst should return the root element when it matches", root, first);
    }

    @Test
    public void testCollectWithSingleElementTree() {
        Element single = new Element("span");
        single.addClass("solo");

        Evaluator eval = new ClassMatcher("solo");
        Elements result = Collector.collect(eval, single);

        Assert.assertEquals("Single matching element should be collected", 1, result.size());
        Assert.assertSame("Collected element must be the original element", single, result.get(0));
    }

    @Test
    public void testFindFirstWithSingleElementTree() {
        Element single = new Element("span");
        single.addClass("solo");

        Evaluator eval = new ClassMatcher("solo");
        Element first = Collector.findFirst(eval, single);

        Assert.assertNotNull(first);
        Assert.assertSame(single, first);
    }

    /** -------------------------------------------------------------------- *
     *  EXCEPTION HANDLING                                                   *
     *  -------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testCollectThrowsNPEWhenEvaluatorIsNull() {
        Element root = buildSampleRoot();
        Collector.collect(null, root);
    }

    @Test(expected = NullPointerException.class)
    public void testCollectThrowsNPEWhenRootIsNull() {
        Evaluator eval = new ClassMatcher("match");
        Collector.collect(eval, null);
    }

    @Test(expected = NullPointerException.class)
    public void testFindFirstThrowsNPEWhenEvaluatorIsNull() {
        Element root = buildSampleRoot();
        Collector.findFirst(null, root);
    }

    @Test(expected = NullPointerException.class)
    public void testFindFirstThrowsNPEWhenRootIsNull() {
        Evaluator eval = new ClassMatcher("match");
        Collector.findFirst(eval, null);
    }

    /** -------------------------------------------------------------------- *
     *  VERIFY STOP BEHAVIOUR OF FirstFinder (first match stops traversal)   *
     *  -------------------------------------------------------------------- */

    @Test
    public void testFindFirstStopsAfterFirstMatch() {
        // Build a tree where two elements match; we verify that the second one is never visited.
        String html = "<div id='root'>"
                + "  <p class='match'>First</p>"
                + "  <p class='match'>Second</p>"
                + "</div>";
        Document doc = Jsoup.parse(html);
        Element root = doc.getElementById("root");

        // A custom evaluator that records every element it inspects.
        class RecordingEvaluator implements Evaluator {
            final StringBuilder visited = new StringBuilder();

            @Override
            public boolean matches(Element root, Element element) {
                visited.append(element.text()).append("|");
                // Match all elements with class 'match'
                return element.hasClass("match");
            }
        }

        RecordingEvaluator eval = new RecordingEvaluator();
        Element first = Collector.findFirst(eval, root);

        Assert.assertNotNull(first);
        Assert.assertEquals("First", first.text());

        // The second matching element ("Second") must not have been visited because traversal stopped.
        Assert.assertFalse("Second element should not have been visited after STOP",
                eval.visited.toString().contains("Second"));
    }
}
