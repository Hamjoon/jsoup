###Test START##
package org.jsoup.select;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Tag;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class NodeTraversorTest {

    /**
     * Helper visitor that records the order of head/tail calls together with node name and depth.
     */
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

    /**
     * Helper filter that records calls and can be configured to return specific {@link FilterResult}
     * for each node (by node name). If a node name is not configured, {@link FilterResult#CONTINUE}
     * is returned.
     */
    private static class ConfigurableFilter implements NodeFilter {
        private final Map<String, FilterResult> headResultMap = new HashMap<>();
        private final Map<String, FilterResult> tailResultMap = new HashMap<>();
        final List<String> log = new ArrayList<>();

        void setHeadResult(String nodeName, FilterResult result) {
            headResultMap.put(nodeName, result);
        }

        void setTailResult(String nodeName, FilterResult result) {
            tailResultMap.put(nodeName, result);
        }

        @Override
        public FilterResult head(Node node, int depth) {
            log.add("head:" + node.nodeName() + ":" + depth);
            return headResultMap.getOrDefault(node.nodeName(), FilterResult.CONTINUE);
        }

        @Override
        public FilterResult tail(Node node, int depth) {
            log.add("tail:" + node.nodeName() + ":" + depth);
            return tailResultMap.getOrDefault(node.nodeName(), FilterResult.CONTINUE);
        }
    }

    /**
     * Builds a small DOM tree:
     *
     * <root>
     *   <a>
     *     <c/>
     *   </a>
     *   <b/>
     * </root>
     *
     * plus a text node under <c> to ensure non‑Element nodes are also visited.
     */
    private Element buildSampleTree() {
        Element root = new Element(Tag.valueOf("root"), "");
        Element a = new Element(Tag.valueOf("a"), "");
        Element b = new Element(Tag.valueOf("b"), "");
        Element c = new Element(Tag.valueOf("c"), "");
        TextNode txt = new TextNode("text", "");

        root.appendChild(a);
        root.appendChild(b);
        a.appendChild(c);
        c.appendChild(txt);
        return root;
    }

    @Test
    public void testTraverseStaticVisitor() {
        Element root = buildSampleTree();
        RecordingVisitor visitor = new RecordingVisitor();

        NodeTraversor.traverse(visitor, root);

        List<String> expected = Arrays.asList(
                "head:root:0",
                "head:a:1",
                "head:c:2",
                "head:#text:3",
                "tail:#text:3",
                "tail:c:2",
                "tail:a:1",
                "head:b:1",
                "tail:b:1",
                "tail:root:0"
        );
        assertEquals(expected, visitor.log);
    }

    @Test
    public void testTraverseInstanceMethodDelegatesToStatic() {
        Element root = buildSampleTree();
        RecordingVisitor visitor = new RecordingVisitor();

        // Use the deprecated constructor + instance method.
        NodeTraversor traversor = new NodeTraversor(visitor);
        traversor.traverse(root);

        List<String> expected = Arrays.asList(
                "head:root:0",
                "head:a:1",
                "head:c:2",
                "head:#text:3",
                "tail:#text:3",
                "tail:c:2",
                "tail:a:1",
                "head:b:1",
                "tail:b:1",
                "tail:root:0"
        );
        assertEquals(expected, visitor.log);
    }

    @Test
    public void testTraverseElementsCollection() {
        Element root1 = new Element(Tag.valueOf("x"), "");
        Element root2 = new Element(Tag.valueOf("y"), "");
        root1.appendChild(new Element(Tag.valueOf("c1"), ""));
        root2.appendChild(new Element(Tag.valueOf("c2"), ""));

        Elements elements = new Elements();
        elements.add(root1);
        elements.add(root2);

        RecordingVisitor visitor = new RecordingVisitor();
        NodeTraversor.traverse(visitor, elements);

        List<String> expected = Arrays.asList(
                // first element tree
                "head:x:0",
                "head:c1:1",
                "tail:c1:1",
                "tail:x:0",
                // second element tree
                "head:y:0",
                "head:c2:1",
                "tail:c2:1",
                "tail:y:0"
        );
        assertEquals(expected, visitor.log);
    }

    @Test
    public void testFilterContinueBehavesLikeTraverse() {
        Element root = buildSampleTree();
        ConfigurableFilter filter = new ConfigurableFilter(); // default CONTINUE for all nodes

        FilterResult result = NodeTraversor.filter(filter, root);
        assertEquals(FilterResult.CONTINUE, result);

        // The log should be identical to a normal traversal (head/tail order)
        List<String> expected = Arrays.asList(
                "head:root:0",
                "head:a:1",
                "head:c:2",
                "head:#text:3",
                "tail:#text:3",
                "tail:c:2",
                "tail:a:1",
                "head:b:1",
                "tail:b:1",
                "tail:root:0"
        );
        assertEquals(expected, filter.log);
    }

    @Test
    public void testFilterSkipChildren() {
        Element root = buildSampleTree();
        ConfigurableFilter filter = new ConfigurableFilter();
        // Instruct the filter to skip children of node "a"
        filter.setHeadResult("a", FilterResult.SKIP_CHILDREN);

        FilterResult result = NodeTraversor.filter(filter, root);
        assertEquals(FilterResult.CONTINUE, result);

        // Expected: children of <a> (c and its text) are not visited.
        List<String> expected = Arrays.asList(
                "head:root:0",
                "head:a:1",
                "tail:a:1",          // tail called even though children were skipped
                "head:b:1",
                "tail:b:1",
                "tail:root:0"
        );
        assertEquals(expected, filter.log);
    }

    @Test
    public void testFilterRemoveNode() {
        Element root = buildSampleTree();
        // Remove node "c" during traversal.
        ConfigurableFilter filter = new ConfigurableFilter();
        filter.setHeadResult("c", FilterResult.REMOVE);

        FilterResult result = NodeTraversor.filter(filter, root);
        assertEquals(FilterResult.CONTINUE, result);

        // After removal, <a> should have no children.
        Element a = root.selectFirst("a");
        assertNotNull(a);
        assertEquals(0, a.childNodeSize());

        // Log should contain head/tail for root, a, (c head only, tail not called because removed before tail)
        List<String> expected = Arrays.asList(
                "head:root:0",
                "head:a:1",
                "head:c:2",
                // tail for c is never called because result == REMOVE causes early removal
                "tail:a:1",
                "head:b:1",
                "tail:b:1",
                "tail:root:0"
        );
        assertEquals(expected, filter.log);
    }

    @Test
    public void testFilterStopFromHead() {
        Element root = buildSampleTree();
        ConfigurableFilter filter = new ConfigurableFilter();
        // Stop when we reach node "c"
        filter.setHeadResult("c", FilterResult.STOP);

        FilterResult result = NodeTraversor.filter(filter, root);
        assertEquals(FilterResult.STOP, result);

        // Traversal should stop immediately after head:c, no tail:c and no further nodes.
        List<String> expected = Arrays.asList(
                "head:root:0",
                "head:a:1",
                "head:c:2"
                // No further calls
        );
        assertEquals(expected, filter.log);
    }

    @Test
    public void testFilterStopFromTail() {
        Element root = buildSampleTree();
        ConfigurableFilter filter = new ConfigurableFilter();
        // Return STOP from tail of node "a"
        filter.setTailResult("a", FilterResult.STOP);

        FilterResult result = NodeTraversor.filter(filter, root);
        assertEquals(FilterResult.STOP, result);

        // Expected order: traverse root, a, c, text, tail:text, tail:c, tail:a (STOP), then stop before b.
        List<String> expected = Arrays.asList(
                "head:root:0",
                "head:a:1",
                "head:c:2",
                "head:#text:3",
                "tail:#text:3",
                "tail:c:2",
                "tail:a:1"   // STOP here
        );
        assertEquals(expected, filter.log);
    }

    @Test
    public void testFilterElementsStopsOnStopResult() {
        Element root1 = new Element(Tag.valueOf("x"), "");
        Element root2 = new Element(Tag.valueOf("y"), "");
        root1.appendChild(new Element(Tag.valueOf("c1"), ""));
        root2.appendChild(new Element(Tag.valueOf("c2"), ""));

        Elements elements = new Elements();
        elements.add(root1);
        elements.add(root2);

        ConfigurableFilter filter = new ConfigurableFilter();
        // Stop when processing root2
        filter.setHeadResult("y", FilterResult.STOP);

        NodeTraversor.filter(filter, elements);

        // Expect traversal of root1 completely, then head:y (stop) and nothing after.
        List<String> expected = Arrays.asList(
                "head:x:0",
                "head:c1:1",
                "tail:c1:1",
                "tail:x:0",
                "head:y:0"   // STOP here, no tail:y, no children of y visited
        );
        assertEquals(expected, filter.log);
    }

    @Test
    public void testTraverseDoesNotBlowStackOnDeepTree() {
        // Build a deep chain of 10,000 nested <div> elements.
        final int depth = 10000;
        Element root = new Element(Tag.valueOf("div"), "");
        Element parent = root;
        for (int i = 0; i < depth; i++) {
            Element child = new Element(Tag.valueOf("div"), "");
            parent.appendChild(child);
            parent = child;
        }

        RecordingVisitor visitor = new RecordingVisitor();
        // Should complete without StackOverflowError.
        NodeTraversor.traverse(visitor, root);

        // Verify that the first and last entries are correct.
        assertEquals("head:div:0", visitor.log.get(0));
        assertEquals("tail:div:0", visitor.log.get(visitor.log.size() - 1));
        // Total log entries = 2 * (depth+1) because each node has head and tail.
        assertEquals(2 * (depth + 1), visitor.log.size());
    }
}
###Test END##