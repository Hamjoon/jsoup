package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeFilter.FilterResult;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link NodeTraversor}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors and methods.</li>
 *   <li>Normal behaviour (correct ordering of callbacks, handling of empty collections, etc.).</li>
 *   <li>Edge‑cases and exception handling (null arguments, early STOP, REMOVE, SKIP_CHILDREN, etc.).</li>
 * </ul>
 */
public class NodeTraversorTest {

    /** Simple HTML used for all tests. */
    private Document doc;
    private Element divA;
    private Element pInDiv;
    private Element spanB;

    @Before
    public void setUp() {
        doc = Jsoup.parse(
                "<html>" +
                        "<body>" +
                        "  <div id='a'><p>text</p></div>" +
                        "  <span>hi</span>" +
                        "</body>" +
                        "</html>");
        divA = doc.selectFirst("div#a");
        pInDiv = divA.selectFirst("p");
        spanB = doc.selectFirst("span");
    }

    /* --------------------------------------------------------------------- *
     *  Helper classes that record callbacks for verification
     * --------------------------------------------------------------------- */

    /** Records head/tail calls together with node name and depth. */
    private static class RecordingVisitor implements NodeVisitor {
        final List<String> events = new ArrayList<>();

        @Override
        public void head(Node node, int depth) {
            events.add("head:" + node.nodeName() + ":" + depth);
        }

        @Override
        public void tail(Node node, int depth) {
            events.add("tail:" + node.nodeName() + ":" + depth);
        }
    }

    /**
     * A {@link NodeFilter} that can be configured to return a specific
     * {@link FilterResult} for a node with a given tag name.
     */
    private static class ConfigurableFilter implements NodeFilter {
        final List<String> events = new ArrayList<>();
        final String stopTag;
        final String removeTag;
        final String skipTag;

        ConfigurableFilter(String stopTag, String removeTag, String skipTag) {
            this.stopTag = stopTag;
            this.removeTag = removeTag;
            this.skipTag = skipTag;
        }

        @Override
        public FilterResult head(Node node, int depth) {
            events.add("head:" + node.nodeName());
            if (stopTag != null && node.nodeName().equals(stopTag))
                return FilterResult.STOP;
            if (removeTag != null && node.nodeName().equals(removeTag))
                return FilterResult.REMOVE;
            if (skipTag != null && node.nodeName().equals(skipTag))
                return FilterResult.SKIP_CHILDREN;
            return FilterResult.CONTINUE;
        }

        @Override
        public FilterResult tail(Node node, int depth) {
            events.add("tail:" + node.nodeName());
            return FilterResult.CONTINUE;
        }
    }

    /* --------------------------------------------------------------------- *
     *  1.  Public constructor & instance method
     * --------------------------------------------------------------------- */

    @Test
    public void testConstructorAndInstanceTraverse() {
        RecordingVisitor visitor = new RecordingVisitor();
        NodeTraversor traversor = new NodeTraversor(visitor);
        traversor.traverse(doc);          // instance method delegates to static version

        // Verify that the visitor saw the root (document) and then the body hierarchy.
        assertTrue(visitor.events.get(0).startsWith("head:#document"));
        assertTrue(visitor.events.get(visitor.events.size() - 1).startsWith("tail:#document"));
    }

    @Test(expected = NullPointerException.class)
    public void testInstanceTraverse_NullRoot_ShouldThrowNPE() {
        RecordingVisitor visitor = new RecordingVisitor();
        NodeTraversor traversor = new NodeTraversor(visitor);
        traversor.traverse(null);          // root is null → NPE inside while‑loop
    }

    /* --------------------------------------------------------------------- *
     *  2.  Static traverse(NodeVisitor, Node)
     * --------------------------------------------------------------------- */

    @Test
    public void testStaticTraverse_VisitorOrder() {
        RecordingVisitor visitor = new RecordingVisitor();
        NodeTraversor.traverse(visitor, doc);

        // Expected depth‑first order (head before children, tail after children)
        List<String> expected = new ArrayList<>();
        expected.add("head:#document:0");
        expected.add("head:html:1");
        expected.add("head:head:2");
        expected.add("tail:head:2");
        expected.add("head:body:2");
        expected.add("head:div:3");
        expected.add("head:p:4");
        expected.add("tail:p:4");
        expected.add("tail:div:3");
        expected.add("head:span:3");
        expected.add("tail:span:3");
        expected.add("tail:body:2");
        expected.add("tail:html:1");
        expected.add("tail:#document:0");

        assertEquals("Visitor callbacks order differs", expected, visitor.events);
    }

    @Test(expected = NullPointerException.class)
    public void testStaticTraverse_NullVisitor_ShouldThrowNPE() {
        NodeTraversor.traverse(null, doc);
    }

    /* --------------------------------------------------------------------- *
     *  3.  Static traverse(NodeVisitor, Elements)
     * --------------------------------------------------------------------- */

    @Test
    public void testTraverseElements_EmptyList_NoCalls() {
        RecordingVisitor visitor = new RecordingVisitor();
        Elements empty = new Elements();               // empty collection
        NodeTraversor.traverse(visitor, empty);
        assertTrue("Visitor should not be called for empty Elements", visitor.events.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void testTraverseElements_NullVisitor_ShouldThrowNPE() {
        Elements els = new Elements(divA, spanB);
        NodeTraversor.traverse(null, els);
    }

    @Test(expected = NullPointerException.class)
    public void testTraverseElements_NullElements_ShouldThrowNPE() {
        RecordingVisitor visitor = new RecordingVisitor();
        NodeTraversor.traverse(visitor, null);
    }

    @Test
    public void testTraverseElements_MultipleElements() {
        RecordingVisitor visitor = new RecordingVisitor();
        Elements els = new Elements(divA, spanB);
        NodeTraversor.traverse(visitor, els);

        // The first element visited should be the div and its subtree,
        // followed by the span element.
        assertTrue(visitor.events.get(0).startsWith("head:div"));
        assertTrue(visitor.events.get(visitor.events.size() - 1).startsWith("tail:span"));
    }

    /* --------------------------------------------------------------------- *
     *  4.  Static filter(NodeFilter, Node)
     * --------------------------------------------------------------------- */

    @Test
    public void testFilter_StopEarly() {
        ConfigurableFilter filter = new ConfigurableFilter("p", null, null); // STOP on <p>
        FilterResult result = NodeTraversor.filter(filter, doc);
        assertEquals(FilterResult.STOP, result);

        // The <p> node should appear in head events, but its tail (and any later
        // nodes) must not be visited.
        assertTrue(filter.events.contains("head:p"));
        assertFalse(filter.events.contains("tail:p"));
        assertFalse(filter.events.contains("head:span")); // span comes after <p>
    }

    @Test
    public void testFilter_RemoveNode() {
        ConfigurableFilter filter = new ConfigurableFilter(null, "div", null); // REMOVE <div>
        FilterResult result = NodeTraversor.filter(filter, doc);
        assertEquals(FilterResult.CONTINUE, result);

        // After traversal the <div id='a'> must be gone.
        assertNull(doc.selectFirst("div#a"));
        // The <p> that was inside the removed <div> must also be gone.
        assertNull(doc.selectFirst("p"));
    }

    @Test
    public void testFilter_SkipChildren() {
        ConfigurableFilter filter = new ConfigurableFilter(null, null, "div"); // SKIP_CHILDREN on <div>
        NodeTraversor.filter(filter, doc);

        // The <p> inside the div must never appear in the event list.
        assertFalse(filter.events.contains("head:p"));
        assertFalse(filter.events.contains("tail:p"));
        // The div itself must still have both head and tail.
        assertTrue(filter.events.contains("head:div"));
        assertTrue(filter.events.contains("tail:div"));
    }

    @Test(expected = NullPointerException.class)
    public void testFilter_NullFilter_ShouldThrowNPE() {
        NodeTraversor.filter(null, doc);
    }

    /* --------------------------------------------------------------------- *
     *  5.  Static filter(NodeFilter, Elements)
     * --------------------------------------------------------------------- */

    @Test
    public void testFilterElements_StopStopsProcessingRemainingElements() {
        ConfigurableFilter filter = new ConfigurableFilter("div", null, null); // STOP on <div>
        Elements els = new Elements(divA, spanB);
        NodeTraversor.filter(filter, els);

        // The filter should stop after handling the div; span must not be visited.
        assertTrue(filter.events.contains("head:div"));
        assertFalse(filter.events.contains("head:span"));
    }

    @Test(expected = NullPointerException.class)
    public void testFilterElements_NullFilter_ShouldThrowNPE() {
        Elements els = new Elements(divA);
        NodeTraversor.filter(null, els);
    }

    @Test(expected = NullPointerException.class)
    public void testFilterElements_NullElements_ShouldThrowNPE() {
        ConfigurableFilter filter = new ConfigurableFilter(null, null, null);
        NodeTraversor.filter(filter, null);
    }

    @Test
    public void testFilterElements_EmptyElements_NoException() {
        ConfigurableFilter filter = new ConfigurableFilter(null, null, null);
        Elements empty = new Elements();
        NodeTraversor.filter(filter, empty);
        assertTrue(filter.events.isEmpty());
    }
}
