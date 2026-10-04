###Test START##
```java
package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.junit.*;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link NodeTraversor}.
 *
 * <p>The tests cover:</p>
 * <ul>
 *   <li>Depth‑first traversal using {@link NodeTraversor#traverse(NodeVisitor, Node)}.</li>
 *   <li>Traversal of an {@link Elements} collection.</li>
 *   <li>Depth‑first filtering using {@link NodeTraversor#filter(NodeFilter, Node)}.</li>
 *   <li>Filtering of an {@link Elements} collection.</li>
 *   <li>Behaviour of the deprecated constructor / instance method.</li>
 *   <li>Edge cases: empty root, null arguments, STOP, SKIP_CHILDREN and REMOVE actions.</li>
 * </ul>
 */
public class NodeTraversorTest {

    /** Simple visitor that records the order of {@code head} and {@code tail} calls together with depth. */
    private static class RecordingVisitor implements NodeVisitor {
        final List<String> log = new ArrayList<>();

        @Override
        public void head(Node node, int depth) {
            log.add("head:" + node.nodeName() + ":" + depth);
        }

        @Override
        public void tail(Node node, int depth) {
            log.add("tail:" + node.nodeName() + ":" + depth);
        }
    }

    /** Visitor that throws an exception when a node with a specific name is visited – used to test STOP handling. */
    private static class StoppingVisitor implements NodeVisitor {
        private final String stopAt;
        final List<String> visited = new ArrayList<>();

        StoppingVisitor(String stopAt) {
            this.stopAt = stopAt;
        }

        @Override
        public void head(Node node, int depth) {
            visited.add("head:" + node.nodeName());
            if (node.nodeName().equals(stopAt)) {
                throw new StopTraversalException();
            }
        }

        @Override
        public void tail(Node node, int depth) {
            visited.add("tail:" + node.nodeName());
        }
    }

    /** Runtime exception used only to abort traversal in the {@link StoppingVisitor}. */
    private static class StopTraversalException extends RuntimeException {}

    /** Simple filter that records calls and can be configured to return a specific {@link FilterResult}. */
    private static class ConfigurableFilter implements NodeFilter {
        final List<String> log = new ArrayList<>();
        FilterResult headResult = FilterResult.CONTINUE;
        FilterResult tailResult = FilterResult.CONTINUE;
        final Set<String> removeTags = new HashSet<>();

        @Override
        public FilterResult head(Node node, int depth) {
            log.add("head:" + node.nodeName() + ":" + depth);
            if (removeTags.contains(node.nodeName())) {
                return FilterResult.REMOVE;
            }
            return headResult;
        }

        @Override
        public FilterResult tail(Node node, int depth) {
            log.add("tail:" + node.nodeName() + ":" + depth);
            return tailResult;
        }

        /** Convenience: make the filter return {@code STOP} when a node with the given name is encountered. */
        void stopWhen(String nodeName) {
            headResult = (node, depth) -> node.nodeName().equals(nodeName) ? FilterResult.STOP : FilterResult.CONTINUE;
        }
    }

    /** Helper to build a small deterministic DOM used by many tests. */
    private Document buildSampleDocument() {
        // <html>
        //   <head></head>
        //   <body>
        //     <div id="a">
        //       <p id="b">text</p>
        //       <span id="c"></span>
        //     </div>
        //     <ul id="d">
        //       <li id="e"></li>
        //       <li id="f"></li>
        //     </ul>
        //   </body>
        // </html>
        return Jsoup.parse("<html><head></head><body>"
                + "<div id='a'><p id='b'>text</p><span id='c'></span></div>"
                + "<ul id='d'><li id='e'></li><li id='f'></li></ul>"
                + "</body></html>");
    }

    /** --------------------------------------------------------------------- */
    /**                       TRAVERSE (NodeVisitor, Node)                    */
    /** --------------------------------------------------------------------- */

    @Test
    public void traverseSimpleTree_visitsAllNodesInDepthFirstOrder() {
        Document doc = buildSampleDocument();
        Node root = doc.body(); // start at <body>
        RecordingVisitor visitor = new RecordingVisitor();

        NodeTraversor.traverse(visitor, root);

        // Expected depth‑first order (head before children, tail after children)
        List<String> expected = Arrays.asList(
                "head:body:0",
                "head:div:1", "head:p:2", "tail:p:2", "head:span:2", "tail:span:2",
                "tail:div:1",
                "head:ul:1", "head:li:2", "tail:li:2", "head:li:2", "tail:li:2",
                "tail:ul:1",
                "tail:body:0"
        );
        assertEquals(expected, visitor.log);
    }

    @Test
    public void traverseSingleNode_callsHeadAndTailExactlyOnce() {
        Document doc = buildSampleDocument();
        Node root = doc.selectFirst("p"); // <p id="b">
        RecordingVisitor visitor = new RecordingVisitor();

        NodeTraversor.traverse(visitor, root);

        List<String> expected = Arrays.asList(
                "head:p:0",
                "tail:p:0"
        );
        assertEquals(expected, visitor.log);
    }

    @Test(expected = NullPointerException.class)
    public void traverse_nullRoot_throwsNPE() {
        RecordingVisitor visitor = new RecordingVisitor();
        NodeTraversor.traverse(visitor, null);
    }

    /** --------------------------------------------------------------------- */
    /**                     TRAVERSE (NodeVisitor, Elements)                   */
    /** --------------------------------------------------------------------- */

    @Test
    public void traverseElements_iteratesEachElementIndependently() {
        Document doc = buildSampleDocument();
        Elements els = doc.select("div, ul"); // two top‑level elements under body
        RecordingVisitor visitor = new RecordingVisitor();

        NodeTraversor.traverse(visitor, els);

        // Each element's subtree should be visited fully, in the order they appear in the Elements list
        List<String> expected = new ArrayList<>();
        // <div> subtree
        expected.addAll(Arrays.asList(
                "head:div:0", "head:p:1", "tail:p:1", "head:span:1", "tail:span:1", "tail:div:0"
        ));
        // <ul> subtree
        expected.addAll(Arrays.asList(
                "head:ul:0", "head:li:1", "tail:li:1", "head:li:1", "tail:li:1", "tail:ul:0"
        ));
        assertEquals(expected, visitor.log);
    }

    @Test(expected = IllegalArgumentException.class)
    public void traverseElements_nullVisitor_throwsIAE() {
        Document doc = buildSampleDocument();
        Elements els = doc.select("div");
        NodeTraversor.traverse(null, els);
    }

    @Test(expected = IllegalArgumentException.class)
    public void traverseElements_nullElements_throwsIAE() {
        RecordingVisitor visitor = new RecordingVisitor();
        NodeTraversor.traverse(visitor, null);
    }

    /** --------------------------------------------------------------------- */
    /**                         FILTER (NodeFilter, Node)                     */
    /** --------------------------------------------------------------------- */

    @Test
    public void filter_continueVisitsAllNodesAndReturnsContinueAtRoot() {
        Document doc = buildSampleDocument();
        ConfigurableFilter filter = new ConfigurableFilter();

        FilterResult result = NodeTraversor.filter(filter, doc.body());

        // The filter should have visited every node in depth‑first order (head then tail)
        List<String> expected = Arrays.asList(
                "head:body:0", "head:div:1", "head:p:2", "tail:p:2",
                "head:span:2", "tail:span:2", "tail:div:1",
                "head:ul:1", "head:li:2", "tail:li:2", "head:li:2", "tail:li:2",
                "tail:ul:1", "tail:body:0"
        );
        assertEquals(expected, filter.log);
        assertEquals(FilterResult.CONTINUE, result);
    }

    @Test
    public void filter_stopAtSpecificNodeHaltsImmediately() {
        Document doc = buildSampleDocument();
        ConfigurableFilter filter = new ConfigurableFilter();
        filter.stopWhen("ul"); // stop when <ul> is encountered

        FilterResult result = NodeTraversor.filter(filter, doc.body());

        // Expect only nodes up to (and including head of) <ul>
        List<String> expected = Arrays.asList(
                "head:body:0",
                "head:div:1", "head:p:2", "tail:p:2",
                "head:span:2", "tail:span:2", "tail:div:1",
                "head:ul:1" // STOP occurs here, no tail for <ul> nor further nodes
        );
        assertEquals(expected, filter.log);
        assertEquals(FilterResult.STOP, result);
    }

    @Test
    public void filter_skipChildrenDoesNotDescendIntoSubtree() {
        Document doc = buildSampleDocument();
        ConfigurableFilter filter = new ConfigurableFilter() {
            @Override
            public FilterResult head(Node node, int depth) {
                log.add("head:" + node.nodeName() + ":" + depth);
                // Skip children of <div>
                if (node.nodeName().equals("div")) {
                    return FilterResult.SKIP_CHILDREN;
                }
                return FilterResult.CONTINUE;
            }
        };

        FilterResult result = NodeTraversor.filter(filter, doc.body());

        // The <p> and <span> under <div> are not visited
        List<String> expected = Arrays.asList(
                "head:body:0", "head:div:1", "tail:div:1",
                "head:ul:1", "head:li:2", "tail:li:2", "head:li:2", "tail:li:2",
                "tail:ul:1", "tail:body:0"
        );
        assertEquals(expected, filter.log);
        assertEquals(FilterResult.CONTINUE, result);
    }

    @Test
    public void filter_removeNodeDeletesItFromTheDOM() {
        Document doc = buildSampleDocument();
        ConfigurableFilter filter = new ConfigurableFilter() {
            {
                // Mark <span> for removal
                removeTags.add("span");
            }
        };

        FilterResult result = NodeTraversor.filter(filter, doc.body());

        // After traversal, the <span id='c'> should no longer be present
        assertNull(doc.selectFirst("span#c"));
        // Ensure traversal still completed normally
        assertEquals(FilterResult.CONTINUE, result);
    }

    @Test
    public void filter_removeDuringTailDoesNotAffectParentTraversal() {
        Document doc = buildSampleDocument();
        ConfigurableFilter filter = new ConfigurableFilter() {
            @Override
            public FilterResult tail(Node node, int depth) {
                log.add("tail:" + node.nodeName() + ":" + depth);
                // Remove the <li> after its tail has been called
                if (node.nodeName().equals("li") && node.attr("id").equals("e")) {
                    return FilterResult.REMOVE;
                }
                return FilterResult.CONTINUE;
            }
        };

        FilterResult result = NodeTraversor.filter(filter, doc.body());

        // <li id='e'> should be gone, <li id='f'> remains
        assertNull(doc.selectFirst("li#e"));
        assertNotNull(doc.selectFirst("li#f"));
        assertEquals(FilterResult.CONTINUE, result);
    }

    @Test(expected = NullPointerException.class)
    public void filter_nullRoot_throwsNPE() {
        ConfigurableFilter filter = new ConfigurableFilter();
        NodeTraversor.filter(filter, null);
    }

    /** --------------------------------------------------------------------- */
    /**                    FILTER (NodeFilter, Elements)                      */
    /** --------------------------------------------------------------------- */

    @Test
    public void filterElementsStopsWhenFilterReturnsStop() {
        Document doc = buildSampleDocument();
        Elements els = doc.select("div, ul");
        ConfigurableFilter filter = new ConfigurableFilter() {
            @Override
            public FilterResult head(Node node, int depth) {
                log.add("head:" + node.nodeName() + ":" + depth);
                if (node.nodeName().equals("ul")) {
                    return FilterResult.STOP;
                }
                return FilterResult.CONTINUE;
            }
        };

        NodeTraversor.filter(filter, els);

        // Only the <div> subtree should be processed.
        List<String> expected = Arrays.asList(
                "head:div:0", "head:p:1", "tail:p:1", "head:span:1", "tail:span:1", "tail:div:0",
                "head:ul:0" // STOP occurs on ul head; its children are not visited.
        );
        assertEquals(expected, filter.log);
    }

    @Test(expected = IllegalArgumentException.class)
    public void filterElements_nullFilter_throwsIAE() {
        Document doc = buildSampleDocument();
        Elements els = doc.select("p");
        NodeTraversor.filter(null, els);
    }

    @Test(expected = IllegalArgumentException.class)
    public void filterElements_nullElements_throwsIAE() {
        ConfigurableFilter filter = new ConfigurableFilter();
        NodeTraversor.filter(filter, null);
    }

    /** --------------------------------------------------------------------- */
    /**                DEPRECATED CONSTRUCTOR & INSTANCE METHOD               */
    /** --------------------------------------------------------------------- */

    @Test
    public void deprecatedConstructorAndTraverseDelegateToStaticImplementation() {
        Document doc = buildSampleDocument();
        RecordingVisitor visitor = new RecordingVisitor();

        // Use the deprecated constructor + instance method
        NodeTraversor traversor = new NodeTraversor(visitor);
        traversor.traverse(doc.body());

        // Expected log identical to static traverse test
        List<String> expected = Arrays.asList(
                "head:body:0",
                "head:div:1", "head:p:2", "tail:p:2", "head:span:2", "tail:span:2",
                "tail:div:1",
                "head:ul:1", "head:li:2", "tail:li:2", "head:li:2", "tail:li:2",
                "tail:ul:1",
                "tail:body:0"
        );
        assertEquals(expected, visitor.log);
    }

    /** --------------------------------------------------------------------- */
    /**                         INTERNAL Helper Assertions                    */
    /** --------------------------------------------------------------------- */

    /** Helper to compare two lists ignoring order of tail calls for nodes that have been removed.
     *  In our tests the order is deterministic, so we simply use {@link Assert#assertEquals}.
     */
    private static void assertListEquals(List<String> expected, List<String> actual) {
        assertEquals("Expected traversal log does not match", expected, actual);
    }
}
```
###Test END##