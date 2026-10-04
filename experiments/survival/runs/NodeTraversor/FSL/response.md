###Test START##
package org.jsoup.select;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeFilter.FilterResult;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class NodeTraversorTest {

    /** Helper visitor that records head/tail callbacks. */
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

        List<String> getEvents() {
            return events;
        }
    }

    /** Helper filter that can be configured to return different results per node. */
    private static abstract class ConfigurableFilter implements NodeFilter {
        final List<String> events = new ArrayList<>();

        @Override
        public FilterResult head(Node node, int depth) {
            events.add("head:" + node.nodeName() + ":" + depth);
            return headResult(node, depth);
        }

        @Override
        public FilterResult tail(Node node, int depth) {
            events.add("tail:" + node.nodeName() + ":" + depth);
            return tailResult(node, depth);
        }

        protected abstract FilterResult headResult(Node node, int depth);
        protected abstract FilterResult tailResult(Node node, int depth);

        List<String> getEvents() {
            return events;
        }
    }

    /** Builds a small DOM tree used by many tests.
     *
     *  <div id="root">
     *      <p id="p1">Hello</p>
     *      <span id="span1">
     *          <a id="a1"></a>
     *      </span>
     *  </div>
     */
    private Element buildTestTree() {
        Element root = new Element("div").attr("id", "root");
        Element p = new Element("p").attr("id", "p1").appendChild(new TextNode("Hello"));
        Element span = new Element("span").attr("id", "span1");
        Element a = new Element("a").attr("id", "a1");
        span.appendChild(a);
        root.appendChild(p);
        root.appendChild(span);
        return root;
    }

    @Test
    public void testStaticTraverseNodeVisitor() {
        Element root = buildTestTree();
        RecordingVisitor visitor = new RecordingVisitor();

        NodeTraversor.traverse(visitor, root);

        List<String> expected = new ArrayList<>();
        // head events
        expected.add("head:div:0");
        expected.add("head:p:1");
        // p has no children → tail p
        expected.add("tail:p:1");
        expected.add("head:span:1");
        expected.add("head:a:2");
        expected.add("tail:a:2");
        expected.add("tail:span:1");
        expected.add("tail:div:0");

        assertEquals("Traversal order (head/tail) does not match expected depth‑first order", expected, visitor.getEvents());
    }

    @Test
    public void testInstanceTraverseUsesVisitor() {
        Element root = buildTestTree();
        RecordingVisitor visitor = new RecordingVisitor();
        NodeTraversor traversor = new NodeTraversor(visitor);

        traversor.traverse(root); // deprecated instance method

        // Expect same order as static version
        List<String> expected = new ArrayList<>();
        expected.add("head:div:0");
        expected.add("head:p:1");
        expected.add("tail:p:1");
        expected.add("head:span:1");
        expected.add("head:a:2");
        expected.add("tail:a:2");
        expected.add("tail:span:1");
        expected.add("tail:div:0");

        assertEquals(expected, visitor.getEvents());
    }

    @Test
    public void testTraverseElements() {
        Element root1 = buildTestTree();               // first tree
        Element root2 = new Element("section").attr("id", "sec");
        root2.appendChild(new Element("h1").attr("id", "h1"));

        Elements elements = new Elements(root1, root2);
        RecordingVisitor visitor = new RecordingVisitor();

        NodeTraversor.traverse(visitor, elements);

        // Expected events: first tree then second tree
        List<String> expected = new ArrayList<>();
        // first tree
        expected.add("head:div:0");
        expected.add("head:p:1");
        expected.add("tail:p:1");
        expected.add("head:span:1");
        expected.add("head:a:2");
        expected.add("tail:a:2");
        expected.add("tail:span:1");
        expected.add("tail:div:0");
        // second tree
        expected.add("head:section:0");
        expected.add("head:h1:1");
        expected.add("tail:h1:1");
        expected.add("tail:section:0");

        assertEquals(expected, visitor.getEvents());
    }

    @Test
    public void testFilterContinueVisitsAllNodes() {
        Element root = buildTestTree();

        ConfigurableFilter filter = new ConfigurableFilter() {
            @Override
            protected FilterResult headResult(Node node, int depth) {
                return FilterResult.CONTINUE;
            }

            @Override
            protected FilterResult tailResult(Node node, int depth) {
                return FilterResult.CONTINUE;
            }
        };

        FilterResult result = NodeTraversor.filter(filter, root);
        assertEquals(FilterResult.CONTINUE, result);

        List<String> expected = new ArrayList<>();
        // head events
        expected.add("head:div:0");
        expected.add("head:p:1");
        expected.add("head:span:1");
        expected.add("head:a:2");
        // tail events (order same as traverse)
        expected.add("tail:a:2");
        expected.add("tail:span:1");
        expected.add("tail:p:1");
        expected.add("tail:div:0");

        // Note: The filter implementation records head before tail for each node.
        // The order of tail events for p occurs after span processing according to algorithm.
        assertEquals(expected, filter.getEvents());
    }

    @Test
    public void testFilterSkipChildren() {
        Element root = buildTestTree();

        ConfigurableFilter filter = new ConfigurableFilter() {
            @Override
            protected FilterResult headResult(Node node, int depth) {
                // Skip children of the <span> element
                if ("span".equals(node.nodeName())) {
                    return FilterResult.SKIP_CHILDREN;
                }
                return FilterResult.CONTINUE;
            }

            @Override
            protected FilterResult tailResult(Node node, int depth) {
                return FilterResult.CONTINUE;
            }
        };

        FilterResult result = NodeTraversor.filter(filter, root);
        assertEquals(FilterResult.CONTINUE, result);

        // Expected order: div head, p head/tail, span head/tail (no a), then div tail
        List<String> expected = new ArrayList<>();
        expected.add("head:div:0");
        expected.add("head:p:1");
        expected.add("tail:p:1");
        expected.add("head:span:1");
        expected.add("tail:span:1");
        expected.add("tail:div:0");

        assertEquals(expected, filter.getEvents());
    }

    @Test
    public void testFilterStopStopsImmediately() {
        Element root = buildTestTree();

        ConfigurableFilter filter = new ConfigurableFilter() {
            @Override
            protected FilterResult headResult(Node node, int depth) {
                // Stop when we encounter the <p> element
                if ("p".equals(node.nodeName())) {
                    return FilterResult.STOP;
                }
                return FilterResult.CONTINUE;
            }

            @Override
            protected FilterResult tailResult(Node node, int depth) {
                return FilterResult.CONTINUE;
            }
        };

        FilterResult result = NodeTraversor.filter(filter, root);
        assertEquals(FilterResult.STOP, result);

        // Only the root head and the p head should have been recorded.
        List<String> expected = new ArrayList<>();
        expected.add("head:div:0");
        expected.add("head:p:1");

        assertEquals(expected, filter.getEvents());
    }

    @Test
    public void testFilterRemoveNode() {
        Element root = buildTestTree();

        ConfigurableFilter filter = new ConfigurableFilter() {
            @Override
            protected FilterResult headResult(Node node, int depth) {
                return FilterResult.CONTINUE;
            }

            @Override
            protected FilterResult tailResult(Node node, int depth) {
                // Remove the <a> element after its tail is called
                if ("a".equals(node.nodeName())) {
                    return FilterResult.REMOVE;
                }
                return FilterResult.CONTINUE;
            }
        };

        FilterResult result = NodeTraversor.filter(filter, root);
        assertEquals(FilterResult.CONTINUE, result);

        // Verify that the <a> element has been removed from the DOM.
        Element span = root.getElementById("span1");
        assertNotNull(span);
        assertTrue("The <a> child should have been removed", span.childNodeSize() == 0);

        // Expected events include head/tail for <a> and a REMOVE result after tail.
        List<String> expected = new ArrayList<>();
        expected.add("head:div:0");
        expected.add("head:p:1");
        expected.add("head:span:1");
        expected.add("head:a:2");
        expected.add("tail:a:2"); // after this the filter returns REMOVE
        expected.add("tail:span:1");
        expected.add("tail:p:1");
        expected.add("tail:div:0");

        assertEquals(expected, filter.getEvents());
    }

    @Test
    public void testFilterElementsStopsOnStopResult() {
        Element root1 = buildTestTree(); // first tree
        Element root2 = new Element("section").attr("id", "sec");
        root2.appendChild(new Element("h1").attr("id", "h1"));
        Elements elements = new Elements(root1, root2);

        ConfigurableFilter filter = new ConfigurableFilter() {
            @Override
            protected FilterResult headResult(Node node, int depth) {
                // Stop as soon as we encounter the first <span>
                if ("span".equals(node.nodeName())) {
                    return FilterResult.STOP;
                }
                return FilterResult.CONTINUE;
            }

            @Override
            protected FilterResult tailResult(Node node, int depth) {
                return FilterResult.CONTINUE;
            }
        };

        NodeTraversor.filter(filter, elements);

        // The filter should have processed root1 up to the <span> and then stopped.
        List<String> expected = new ArrayList<>();
        expected.add("head:div:0");
        expected.add("head:p:1");
        expected.add("tail:p:1");
        expected.add("head:span:1"); // STOP returned here, traversal ends immediately

        assertEquals(expected, filter.getEvents());
    }
}
###Test END##