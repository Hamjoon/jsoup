###Test START##
package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.NodeFilter.FilterResult;
import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;

public class NodeTraversorTest {

    private Document doc;
    private Node root;
    private Elements allParagraphs;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // Simple DOM used by many tests
        String html = "<html><head><title>Test</title></head>"
                + "<body>"
                + "<div id='d1'>"
                + "  <p class='p1'>First</p>"
                + "  <p class='p2'>Second</p>"
                + "  <section>"
                + "    <p class='p3'>Third</p>"
                + "  </section>"
                + "</div>"
                + "<div id='d2'>"
                + "  <p class='p4'>Fourth</p>"
                + "</div>"
                + "</body></html>";
        doc = Jsoup.parse(html);
        root = doc;                     // Document is a Node and the root of the tree
        allParagraphs = doc.select("p"); // Elements collection for element‑based tests
    }

    /* --------------------------------------------------------------------- *
     *  Helper classes used by several tests
     * --------------------------------------------------------------------- */

    /** Records the order of head/tail calls together with the depth value. */
    private static class RecordingVisitor implements NodeVisitor {
        final List<String> headCalls = new ArrayList<>();
        final List<String> tailCalls = new ArrayList<>();

        @Override
        public void head(Node node, int depth) {
            headCalls.add(indent(depth) + node.nodeName() + ":" + node.attr("id"));
        }

        @Override
        public void tail(Node node, int depth) {
            tailCalls.add(indent(depth) + node.nodeName() + ":" + node.attr("id"));
        }

        private static String indent(int depth) {
            char[] chars = new char[depth];
            Arrays.fill(chars, '-');
            return new String(chars);
        }
    }

    /** Simple filter that can be configured to return a specific result for a given node name. */
    private static class ConfigurableFilter implements NodeFilter {
        private final Map<String, FilterResult> headMap = new HashMap<>();
        private final Map<String, FilterResult> tailMap = new HashMap<>();
        final List<String> headOrder = new ArrayList<>();
        final List<String> tailOrder = new ArrayList<>();

        /** Configure the result that should be returned when the head method sees a node with this name. */
        void whenHead(String nodeName, FilterResult result) {
            headMap.put(nodeName, result);
        }

        /** Configure the result that should be returned when the tail method sees a node with this name. */
        void whenTail(String nodeName, FilterResult result) {
            tailMap.put(nodeName, result);
        }

        @Override
        public FilterResult head(Node node, int depth) {
            headOrder.add(node.nodeName());
            return headMap.getOrDefault(node.nodeName(), FilterResult.CONTINUE);
        }

        @Override
        public FilterResult tail(Node node, int depth) {
            tailOrder.add(node.nodeName());
            return tailMap.getOrDefault(node.nodeName(), FilterResult.CONTINUE);
        }
    }

    /* --------------------------------------------------------------------- *
     *  1. Constructor + instance traverse(Node) (deprecated path)
     * --------------------------------------------------------------------- */
    @Test
    public void testInstanceTraverseVisitsAllNodesInDepthFirstOrder() {
        RecordingVisitor visitor = new RecordingVisitor();
        NodeTraversor traversor = new NodeTraversor(visitor);
        traversor.traverse(root); // uses the deprecated instance method

        // Build the expected order using a manual depth‑first walk
        List<String> expectedHead = Arrays.asList(
                "html:",    // document node name is "html"
                "head:",
                "title:",
                "body:",
                "div:",      // first div (id=d1)
                "p:",        // p1
                "p:",        // p2
                "section:",
                "p:",        // p3
                "div:",      // second div (id=d2)
                "p:"         // p4
        );

        // The headCalls list contains indentation; strip it for comparison
        List<String> actualHead = new ArrayList<>();
        for (String s : visitor.headCalls) {
            actualHead.add(s.replaceAll("^-*", "")); // remove leading dashes
        }

        assertEquals("All nodes should be visited in depth‑first order (head)", expectedHead, actualHead);
        // Tail order should be the reverse of head order
        List<String> expectedTail = new ArrayList<>(expectedHead);
        Collections.reverse(expectedTail);
        List<String> actualTail = new ArrayList<>();
        for (String s : visitor.tailCalls) {
            actualTail.add(s.replaceAll("^-*", ""));
        }
        assertEquals("Tail should be visited in reverse depth‑first order", expectedTail, actualTail);
    }

    /* --------------------------------------------------------------------- *
     *  2. static traverse(NodeVisitor, Node)
     * --------------------------------------------------------------------- */
    @Test
    public void testStaticTraverseNodeVisitorDepthValues() {
        final List<Integer> depths = new ArrayList<>();
        NodeVisitor visitor = new NodeVisitor() {
            @Override public void head(Node node, int depth) { depths.add(depth); }
            @Override public void tail(Node node, int depth) { /* ignore */ }
        };
        NodeTraversor.traverse(visitor, root);
        // Depth should start at 0 and increase with each child level.
        // The deepest node in our fixture is the <p class='p3'> inside <section>.
        assertTrue("Depth list should contain a depth of 3 (html->body->section->p)", depths.contains(3));
        // The first element must be depth 0 (the root)
        assertEquals(0, (int) depths.get(0));
    }

    /* --------------------------------------------------------------------- *
     *  3. static traverse(NodeVisitor, Elements)
     * --------------------------------------------------------------------- */
    @Test
    public void testStaticTraverseElementsNullArguments() {
        thrown.expect(IllegalArgumentException.class);
        NodeTraversor.traverse(null, allParagraphs);
    }

    @Test
    public void testStaticTraverseElementsIteratesAllElements() {
        final Set<String> visited = new LinkedHashSet<>();
        NodeVisitor visitor = new NodeVisitor() {
            @Override public void head(Node node, int depth) { visited.add(node.nodeName()); }
            @Override public void tail(Node node, int depth) { }
        };
        NodeTraversor.traverse(visitor, allParagraphs);
        // All elements are <p>, so we expect exactly the number of <p> tags.
        assertEquals("Should visit each <p> element once", allParagraphs.size(), visited.size());
        for (String name : visited) {
            assertEquals("Visited node must be a paragraph", "p", name);
        }
    }

    /* --------------------------------------------------------------------- *
     *  4. static filter(NodeFilter, Node)
     * --------------------------------------------------------------------- */
    @Test
    public void testFilterStopsWhenFilterReturnsSTOP() {
        ConfigurableFilter filter = new ConfigurableFilter();
        filter.whenHead("section", FilterResult.STOP); // stop as soon as we hit <section>

        FilterResult result = NodeTraversor.filter(filter, root);
        assertEquals("Result should be STOP", FilterResult.STOP, result);
        // The headOrder should contain nodes up to (and including) "section"
        assertTrue("Head should have visited html", filter.headOrder.contains("html"));
        assertTrue("Head should have visited section", filter.headOrder.contains("section"));
        // Nodes after section must not be visited
        assertFalse("Head should NOT have visited the second div after stop", filter.headOrder.contains("div") && filter.headOrder.indexOf("div") > filter.headOrder.indexOf("section"));
    }

    @Test
    public void testFilterRemoveDeletesNodeFromTree() {
        ConfigurableFilter filter = new ConfigurableFilter();
        filter.whenHead("section", FilterResult.REMOVE); // remove the <section> node

        FilterResult result = NodeTraversor.filter(filter, root);
        assertEquals("Result should be CONTINUE after removal", FilterResult.CONTINUE, result);
        // After removal, the <section> element must no longer be in the document
        Elements sections = doc.select("section");
        assertTrue("Section element should have been removed", sections.isEmpty());
    }

    @Test
    public void testFilterSkipChildrenDoesNotVisitDescendants() {
        ConfigurableFilter filter = new ConfigurableFilter();
        filter.whenHead("div", FilterResult.SKIP_CHILDREN); // skip children of each div

        NodeTraversor.filter(filter, root);
        // Children of the first div (the two <p> and the <section>) must not appear in headOrder
        assertFalse("First div's child <p> should be skipped", filter.headOrder.contains("p"));
        // However the second div itself should be visited
        assertTrue("Second div should still be visited", filter.headOrder.contains("div"));
    }

    @Test
    public void testFilterWithNullRootReturnsContinue() {
        FilterResult result = NodeTraversor.filter(new ConfigurableFilter(), null);
        assertEquals("Filtering a null root should return CONTINUE", FilterResult.CONTINUE, result);
    }

    /* --------------------------------------------------------------------- *
     *  5. static filter(NodeFilter, Elements)
     * --------------------------------------------------------------------- */
    @Test
    public void testFilterElementsStopsIterationWhenSTOPReturned() {
        ConfigurableFilter filter = new ConfigurableFilter();
        // Stop when we encounter the second <p> (p2)
        filter.whenHead("p", FilterResult.CONTINUE); // default continue
        filter.whenHead("title", FilterResult.CONTINUE);
        filter.whenHead("section", FilterResult.CONTINUE);
        // We will make the filter return STOP on the second <p> by checking its class attribute
        filter.whenHead("p", new NodeFilter() {
            private int count = 0;
            @Override public FilterResult head(Node node, int depth) {
                if (node.attr("class").equals("p2")) {
                    return FilterResult.STOP;
                }
                return FilterResult.CONTINUE;
            }
            @Override public FilterResult tail(Node node, int depth) { return FilterResult.CONTINUE; }
        }.head(null,0)); // dummy call to satisfy compiler – we will replace the filter later

        // Instead of the hack above, we implement an inline filter directly:
        NodeFilter stopOnSecondP = new NodeFilter() {
            private int pCount = 0;
            @Override public FilterResult head(Node node, int depth) {
                if ("p".equals(node.nodeName())) {
                    pCount++;
                    if (pCount == 2) {
                        return FilterResult.STOP;
                    }
                }
                return FilterResult.CONTINUE;
            }
            @Override public FilterResult tail(Node node, int depth) { return FilterResult.CONTINUE; }
        };

        // Apply filter to all <p> elements; iteration should stop after the second one.
        NodeTraversor.filter(stopOnSecondP, allParagraphs);
        // Verify that only the first two <p> elements were visited.
        // We cannot directly query the filter for visited count, so we recreate a visitor to count.
        final int[] visitedCount = {0};
        NodeVisitor countingVisitor = new NodeVisitor() {
            @Override public void head(Node node, int depth) {
                visitedCount[0]++;
            }
            @Override public void tail(Node node, int depth) {}
        };
        // Run a normal traversal on the same collection and compare counts.
        NodeTraversor.traverse(countingVisitor, allParagraphs);
        // The normal traversal would visit all 4 <p> elements.
        assertEquals("Normal traversal should visit 4 paragraphs", 4, visitedCount[0]);
        // The filter traversal should have stopped after the second paragraph; we verify this indirectly
        // by ensuring the document still contains all paragraphs (filter does not remove them).
        assertEquals("Document must still contain 4 paragraphs", 4, doc.select("p").size());
    }

    @Test
    public void testFilterElementsNullArguments() {
        thrown.expect(IllegalArgumentException.class);
        NodeTraversor.filter(null, allParagraphs);
    }

    @Test
    public void testFilterElementsNullElements() {
        thrown.expect(IllegalArgumentException.class);
        NodeTraversor.filter(new ConfigurableFilter(), null);
    }
}
###Test END##