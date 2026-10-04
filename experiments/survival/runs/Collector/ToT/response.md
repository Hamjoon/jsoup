###Test START##
```java
package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Collector.FirstFinder;
import org.jsoup.select.Collector.Accumulator;
import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Collector}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical usage – collecting all matching elements.</li>
 *   <li>Edge cases – root matches, no matches, null arguments.</li>
 *   <li>Behaviour of the early‑stop logic in {@code findFirst}.</li>
 *   <li>Verification that only {@link Element}s are added (non‑element nodes are ignored).</li>
 *   <li>Propagation of runtime exceptions thrown by an {@link Evaluator}.</li>
 * </ul>
 */
public class CollectorTest {

    private Document doc;
    private Element root;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // Simple HTML structure used by most tests
        String html = "<html>" +
                "<head><title>Test</title></head>" +
                "<body>" +
                "  <div id='a' class='box'>A</div>" +
                "  <div id='b' class='box'>B</p>" +   // mismatched closing tag on purpose – Jsoup will fix it
                "  <span id='c'>C</span>" +
                "  <p id='d'>D</p>" +
                "</body>" +
                "</html>";
        doc = Jsoup.parse(html);
        root = doc.body(); // use the <body> element as the traversal root
    }

    /**
     * Helper evaluator that matches elements with a given tag name.
     */
    private static class TagNameEvaluator implements Evaluator {
        private final String tagName;

        TagNameEvaluator(String tagName) {
            this.tagName = tagName;
        }

        @Override
        public boolean matches(Element root, Element element) {
            return element.tagName().equalsIgnoreCase(tagName);
        }
    }

    /**
     * Helper evaluator that matches elements having a specific attribute/value pair.
     */
    private static class AttrEvaluator implements Evaluator {
        private final String attrKey;
        private final String attrValue;

        AttrEvaluator(String key, String value) {
            this.attrKey = key;
            this.attrValue = value;
        }

        @Override
        public boolean matches(Element root, Element element) {
            return attrValue.equals(element.attr(attrKey));
        }
    }

    /**
     * Evaluator that always returns {@code false}.
     */
    private static final Evaluator NEVER_MATCH = (root, el) -> false;

    /**
     * Evaluator that throws a RuntimeException when invoked.
     * Used to test exception propagation.
     */
    private static final Evaluator THROWING_EVAL = (root, el) -> {
        throw new IllegalStateException("Evaluator failure");
    };

    /** --------------------------------------------------------------
     *  1. Typical use – collect all elements with tag name "div".
     *  -------------------------------------------------------------- */
    @Test
    public void collect_AllDivs_ReturnsTwoElements() {
        Elements result = Collector.collect(new TagNameEvaluator("div"), root);
        assertEquals("Should find exactly two <div> elements", 2, result.size());

        // Verify that the correct elements were collected (order matters)
        assertEquals("a", result.get(0).id());
        assertEquals("b", result.get(1).id());
    }

    /** --------------------------------------------------------------
     *  2. Edge case – root element itself matches the evaluator.
     *  -------------------------------------------------------------- */
    @Test
    public void collect_RootMatches_IncludesRootInResult() {
        // Root is <body>; evaluator matches tag "body"
        Elements result = Collector.collect(new TagNameEvaluator("body"), root);
        assertEquals(1, result.size());
        assertSame(root, result.get(0));
    }

    /** --------------------------------------------------------------
     *  3. Edge case – no element matches (evaluator always false).
     *  -------------------------------------------------------------- */
    @Test
    public void collect_NoMatches_ReturnsEmptyElements() {
        Elements result = Collector.collect(NEVER_MATCH, root);
        assertTrue("Result should be empty when nothing matches", result.isEmpty());
    }

    /** --------------------------------------------------------------
     *  4. Error scenario – passing a null root should throw NPE.
     *  -------------------------------------------------------------- */
    @Test
    public void collect_NullRoot_ThrowsNullPointerException() {
        thrown.expect(NullPointerException.class);
        Collector.collect(new TagNameEvaluator("div"), null);
    }

    /** --------------------------------------------------------------
     *  5. Typical use – findFirst returns the first <div> in document order.
     *  -------------------------------------------------------------- */
    @Test
    public void findFirst_FirstDiv_ReturnsFirstDivElement() {
        Element firstDiv = Collector.findFirst(new TagNameEvaluator("div"), root);
        assertNotNull("First matching element should not be null", firstDiv);
        assertEquals("a", firstDiv.id()); // <div id='a'> appears before <div id='b'>
    }

    /** --------------------------------------------------------------
     *  6. Edge case – findFirst when root itself matches.
     *  -------------------------------------------------------------- */
    @Test
    public void findFirst_RootMatches_ReturnsRoot() {
        Element found = Collector.findFirst(new TagNameEvaluator("body"), root);
        assertSame("When root matches, it should be returned", root, found);
    }

    /** --------------------------------------------------------------
     *  7. Edge case – findFirst with no matches returns null.
     *  -------------------------------------------------------------- */
    @Test
    public void findFirst_NoMatches_ReturnsNull() {
        Element found = Collector.findFirst(NEVER_MATCH, root);
        assertNull("Should return null when nothing matches", found);
    }

    /** --------------------------------------------------------------
     *  8. Verify that traversal stops after the first match.
     *  -------------------------------------------------------------- */
    @Test
    public void findFirst_StopsTraversalAfterFirstMatch() {
        // Counter evaluator – increments a counter each time matches() is called.
        class CountingEvaluator implements Evaluator {
            int calls = 0;
            @Override
            public boolean matches(Element root, Element element) {
                calls++;
                // Only match <div> elements
                return element.tagName().equals("div");
            }
        }
        CountingEvaluator eval = new CountingEvaluator();

        // First <div> is id='a', after that traversal must stop.
        Element first = Collector.findFirst(eval, root);
        assertNotNull(first);
        assertEquals("a", first.id());

        // Because traversal stops, matches() should have been called only for:
        //  - <body> (depth 0)
        //  - <div id='a'> (depth 1) -> match -> stop
        // Hence exactly 2 calls.
        assertEquals("Traversal should stop after first match", 2, eval.calls);
    }

    /** --------------------------------------------------------------
     *  9. Ensure that non‑Element nodes (e.g., TextNode) are ignored by collect().
     *  -------------------------------------------------------------- */
    @Test
    public void collect_IgnoresNonElementNodes() {
        // Insert a raw TextNode directly under <body>
        TextNode txt = new TextNode("plain text", "");
        root.appendChild(txt);

        // Evaluator that matches everything (always true)
        Evaluator alwaysTrue = (root, el) -> true;

        Elements result = Collector.collect(alwaysTrue, root);
        // Should contain only Elements, never the TextNode
        for (Element el : result) {
            assertFalse("Result must never contain a TextNode", el instanceof TextNode);
        }

        // Verify count: original elements (4) + added TextNode (ignored) = 4
        assertEquals(4, result.size());
    }

    /** --------------------------------------------------------------
     * 10. Propagation of runtime exceptions thrown by an Evaluator.
     * -------------------------------------------------------------- */
    @Test
    public void collect_EvaluatorThrows_ExceptionPropagates() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Evaluator failure");
        Collector.collect(THROWING_EVAL, root);
    }

    /** --------------------------------------------------------------
     * 11. Propagation of runtime exceptions thrown by an Evaluator in findFirst().
     * -------------------------------------------------------------- */
    @Test
    public void findFirst_EvaluatorThrows_ExceptionPropagates() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Evaluator failure");
        Collector.findFirst(THROWING_EVAL, root);
    }

    /** --------------------------------------------------------------
     * 12. Verify that a custom evaluator based on attributes works correctly.
     * -------------------------------------------------------------- */
    @Test
    public void collect_AttributeEvaluator_MatchesCorrectElements() {
        // Match elements that have class="box"
        Evaluator classBox = new AttrEvaluator("class", "box");
        Elements result = Collector.collect(classBox, root);
        assertEquals(2, result.size());
        assertEquals("a", result.get(0).id());
        assertEquals("b", result.get(1).id());
    }
}
```
###Test END##