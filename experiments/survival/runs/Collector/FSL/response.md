###Test START##
```java
package org.jsoup.select;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;

/**
 * Unit tests for {@link Collector}.
 *
 * The tests cover:
 * <ul>
 *   <li>collect(...) – gathering all matching elements.</li>
 *   <li>findFirst(...) – locating the first matching element and ensuring traversal stops.</li>
 *   <li>Edge cases such as no matches and an evaluator that matches every element.</li>
 * </ul>
 */
public class CollectorTest {

    /** Simple evaluator that matches elements by tag name. */
    private static class TagEvaluator implements Evaluator {
        private final String tag;

        TagEvaluator(String tag) {
            this.tag = tag;
        }

        @Override
        public boolean matches(Element root, Element element) {
            return element.tagName().equals(tag);
        }
    }

    /** Evaluator that always returns true – useful for counting total elements visited. */
    private static class AllEvaluator implements Evaluator {
        @Override
        public boolean matches(Element root, Element element) {
            return true;
        }
    }

    /** Evaluator that counts how many times {@code matches} is invoked. */
    private static class CountingEvaluator implements Evaluator {
        private final Evaluator delegate;
        private final AtomicInteger callCount = new AtomicInteger(0);

        CountingEvaluator(Evaluator delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean matches(Element root, Element element) {
            callCount.incrementAndGet();
            return delegate.matches(root, element);
        }

        int getCallCount() {
            return callCount.get();
        }
    }

    /** Helper to parse a small HTML snippet used by several tests. */
    private static Element parse(String html) {
        Document doc = Jsoup.parse(html);
        // Jsoup's Document root element is <html>; we use its body as the test root.
        return doc.body();
    }

    @Test
    public void testCollect_RootMatches() {
        Element root = parse("<section id='root'>root</section>");
        Evaluator eval = new TagEvaluator("section");

        Elements result = Collector.collect(eval, root);
        assertEquals("Should contain exactly the root element", 1, result.size());
        assertSame("Returned element must be the root itself", root, result.get(0));
    }

    @Test
    public void testCollect_MultipleMatchesInDepthFirstOrder() {
        Element root = parse(
                "<div>" +
                "  <p id='p1'>first</p>" +
                "  <span>" +
                "    <p id='p2'>second</p>" +
                "    <article>" +
                "      <p id='p3'>third</p>" +
                "    </article>" +
                "  </span>" +
                "</div>"
        );

        Evaluator eval = new TagEvaluator("p");
        Elements result = Collector.collect(eval, root);

        assertEquals("Three <p> elements expected", 3, result.size());
        assertEquals("p1", result.get(0).id()); // depth‑first: first <p>
        assertEquals("p2", result.get(1).id());
        assertEquals("p3", result.get(2).id());
    }

    @Test
    public void testCollect_NoMatchesReturnsEmpty() {
        Element root = parse("<ul><li>Item</li></ul>");
        Evaluator eval = new TagEvaluator("div"); // no <div> in the snippet

        Elements result = Collector.collect(eval, root);
        assertTrue("Result should be empty when there are no matches", result.isEmpty());
    }

    @Test
    public void testCollect_MatcherMatchesAllElements() {
        Element root = parse(
                "<html>" +
                "  <head><title>Title</title></head>" +
                "  <body>" +
                "    <div><p>Text</p></div>" +
                "    <footer>Footer</footer>" +
                "  </body>" +
                "</html>"
        );

        // Count total elements in the parsed tree (including the synthetic <html> and <head>/<body>)
        int expectedElementCount = root.getAllElements().size(); // getAllElements includes the root itself

        Elements result = Collector.collect(new AllEvaluator(), root);
        assertEquals("All elements should be collected", expectedElementCount, result.size());
    }

    @Test
    public void testFindFirst_ReturnsFirstMatch() {
        Element root = parse(
                "<section>" +
                "  <p id='first'>A</p>" +
                "  <div>" +
                "    <p id='second'>B</p>" +
                "  </div>" +
                "</section>"
        );

        Element first = Collector.findFirst(new TagEvaluator("p"), root);
        assertNotNull("First match should not be null", first);
        assertEquals("first", first.id()); // the first <p> encountered in depth‑first traversal
    }

    @Test
    public void testFindFirst_StopsAfterFirstMatch() {
        Element root = parse(
                "<ul>" +
                "  <li id='li1'>One</li>" +
                "  <li id='li2'>Two</li>" +
                "  <li id='li3'>Three</li>" +
                "</ul>"
        );

        CountingEvaluator countingEval = new CountingEvaluator(new TagEvaluator("li"));
        Element first = Collector.findFirst(countingEval, root);

        assertNotNull(first);
        assertEquals("li1", first.id());
        // The evaluator should have been called exactly once – traversal stops on the first match.
        assertEquals("Evaluator.matches should be invoked once", 1, countingEval.getCallCount());
    }

    @Test
    public void testFindFirst_NoMatchReturnsNull() {
        Element root = parse("<div><span>Text</span></div>");
        Element result = Collector.findFirst(new TagEvaluator("h1"), root);
        assertNull("When no element matches, findFirst must return null", result);
    }

    @Test
    public void testCollect_IgnoresNonElementNodes() {
        // The HTML contains a comment and a text node; only the <p> element should be collected.
        Element root = parse(
                "<div>" +
                "  <!-- a comment -->" +
                "  Text before<p id='p'>Paragraph</p>Text after" +
                "</div>"
        );

        Evaluator eval = new TagEvaluator("p");
        Elements result = Collector.collect(eval, root);
        assertEquals(1, result.size());
        assertEquals("p", result.get(0).id());

        // Ensure that the comment or text nodes never trigger matches (they are not Elements).
        // This is indirectly verified because the result size is exactly 1.
    }
}
```
###Test END##