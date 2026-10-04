package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link StructuralEvaluator} and its concrete subclasses.
 *
 * The tests construct a small DOM tree using Jsoup and use simple
 * {@link Evaluator} implementations that match based on tag name.
 * Each structural evaluator is exercised for both positive and negative
 * scenarios, including edge‑cases such as the root element being the same
 * as the target element.
 */
public class StructuralEvaluatorTest {

    private Document doc;
    private Element root;          // <html>
    private Element div;           // <div id="div1">
    private Element span;          // <span id="span1">
    private Element p;             // <p id="p1">
    private Element sibling1;      // <a id="a1">
    private Element sibling2;      // <b id="b1">
    private Element sibling3;      // <c id="c1">

    /**
     * Simple evaluator that matches elements having a specific tag name.
     */
    private static class TagEvaluator extends Evaluator {
        private final String tag;

        TagEvaluator(String tag) {
            this.tag = tag;
        }

        @Override
        public boolean matches(Element root, Element element) {
            return element.tagName().equalsIgnoreCase(tag);
        }

        @Override
        public String toString() {
            return ":" + tag;
        }
    }

    @Before
    public void setUp() {
        // Build a deterministic DOM tree:
        // <html>
        //   <body>
        //     <div id="div1">
        //       <span id="span1"></span>
        //       <p id="p1"></p>
        //       <a id="a1"></a>
        //       <b id="b1"></b>
        //       <c id="c1"></c>
        //     </div>
        //   </body>
        // </html>
        String html = "<html><body>"
                + "<div id='div1'>"
                + "  <span id='span1'></span>"
                + "  <p id='p1'></p>"
                + "  <a id='a1'></a>"
                + "  <b id='b1'></b>"
                + "  <c id='c1'></c>"
                + "</div>"
                + "</body></html>";
        doc = Jsoup.parse(html);
        root = doc;                     // the Document is the root for Jsoup selectors
        div = doc.selectFirst("div#div1");
        span = doc.selectFirst("span#span1");
        p = doc.selectFirst("p#p1");
        sibling1 = doc.selectFirst("a#a1");
        sibling2 = doc.selectFirst("b#b1");
        sibling3 = doc.selectFirst("c#c1");
    }

    /* --------------------------------------------------------------------- *
     *  StructuralEvaluator.Root
     * --------------------------------------------------------------------- */

    @Test
    public void testRootMatchesWhenSameInstance() {
        StructuralEvaluator.Root evaluator = new StructuralEvaluator.Root();
        assertTrue("Root should match when element equals root", evaluator.matches(root, root));
    }

    @Test
    public void testRootDoesNotMatchDifferentElement() {
        StructuralEvaluator.Root evaluator = new StructuralEvaluator.Root();
        assertFalse("Root should not match a different element", evaluator.matches(root, div));
    }

    @Test
    public void testRootToString() {
        StructuralEvaluator.Root evaluator = new StructuralEvaluator.Root();
        assertEquals(":root", evaluator.toString()); // default Evaluator toString is ":root"
    }

    /* --------------------------------------------------------------------- *
     *  StructuralEvaluator.Has
     * --------------------------------------------------------------------- */

    @Test
    public void testHasMatchesWhenDescendantMatches() {
        Evaluator spanEval = new TagEvaluator("span");
        StructuralEvaluator.Has has = new StructuralEvaluator.Has(spanEval);

        // 'div' contains a 'span' descendant
        assertTrue("Has should match when a descendant satisfies the evaluator", has.matches(root, div));
    }

    @Test
    public void testHasDoesNotMatchWhenNoDescendantMatches() {
        Evaluator imgEval = new TagEvaluator("img"); // no img in our tree
        StructuralEvaluator.Has has = new StructuralEvaluator.Has(imgEval);
        assertFalse("Has should not match when no descendant satisfies the evaluator", has.matches(root, div));
    }

    @Test
    public void testHasDoesNotConsiderSelf() {
        // The element itself is a <span>, but Has must ignore self.
        Evaluator spanEval = new TagEvaluator("span");
        StructuralEvaluator.Has has = new StructuralEvaluator.Has(spanEval);
        assertFalse("Has must ignore the element itself", has.matches(root, span));
    }

    @Test
    public void testHasToString() {
        Evaluator eval = new TagEvaluator("foo");
        StructuralEvaluator.Has has = new StructuralEvaluator.Has(eval);
        assertEquals(":has(:foo)", has.toString());
    }

    /* --------------------------------------------------------------------- *
     *  StructuralEvaluator.Not
     * --------------------------------------------------------------------- */

    @Test
    public void testNotInvertsEvaluatorResult() {
        Evaluator divEval = new TagEvaluator("div");
        StructuralEvaluator.Not not = new StructuralEvaluator.Not(divEval);

        assertFalse("Not should be false when inner evaluator matches", not.matches(root, div));
        assertTrue("Not should be true when inner evaluator does not match", not.matches(root, span));
    }

    @Test
    public void testNotToString() {
        Evaluator eval = new TagEvaluator("bar");
        StructuralEvaluator.Not not = new StructuralEvaluator.Not(eval);
        assertEquals(":not:bar", not.toString());
    }

    /* --------------------------------------------------------------------- *
     *  StructuralEvaluator.Parent
     * --------------------------------------------------------------------- */

    @Test
    public void testParentMatchesWhenAncestorMatches() {
        // Evaluator looks for a <div> ancestor
        Evaluator divEval = new TagEvaluator("div");
        StructuralEvaluator.Parent parent = new StructuralEvaluator.Parent(divEval);

        // <span> is inside <div>, so should match
        assertTrue("Parent should match when an ancestor satisfies the evaluator", parent.matches(root, span));
    }

    @Test
    public void testParentDoesNotMatchWhenNoAncestorMatches() {
        Evaluator imgEval = new TagEvaluator("img");
        StructuralEvaluator.Parent parent = new StructuralEvaluator.Parent(imgEval);
        assertFalse("Parent should not match when no ancestor satisfies the evaluator", parent.matches(root, span));
    }

    @Test
    public void testParentStopsAtRoot() {
        // Root is the document; evaluator matches the document itself.
        Evaluator rootEval = new Evaluator() {
            @Override
            public boolean matches(Element root, Element element) {
                return element == root; // true only for the root element
            }

            @Override
            public String toString() {
                return ":rootEvaluator";
            }
        };
        StructuralEvaluator.Parent parent = new StructuralEvaluator.Parent(rootEval);
        // 'div' is a direct child of the body (which is under the Document root)
        // The loop should stop when parent becomes the root; it should NOT count the root itself.
        assertFalse("Parent must not consider the root element as a match", parent.matches(root, div));
    }

    @Test
    public void testParentToString() {
        Evaluator eval = new TagEvaluator("baz");
        StructuralEvaluator.Parent parent = new StructuralEvaluator.Parent(eval);
        assertEquals(":parent:baz", parent.toString());
    }

    /* --------------------------------------------------------------------- *
     *  StructuralEvaluator.ImmediateParent
     * --------------------------------------------------------------------- */

    @Test
    public void testImmediateParentMatchesWhenDirectParentMatches() {
        Evaluator divEval = new TagEvaluator("div");
        StructuralEvaluator.ImmediateParent ip = new StructuralEvaluator.ImmediateParent(divEval);
        assertTrue("ImmediateParent should match when direct parent satisfies evaluator", ip.matches(root, span));
    }

    @Test
    public void testImmediateParentDoesNotMatchWhenIndirectParentMatches() {
        // Create a deeper hierarchy: <div><section><p></p></section></div>
        Element section = doc.createElement("section");
        Element deepP = doc.createElement("p");
        div.appendChild(section);
        section.appendChild(deepP);

        Evaluator divEval = new TagEvaluator("div");
        StructuralEvaluator.ImmediateParent ip = new StructuralEvaluator.ImmediateParent(divEval);
        assertFalse("ImmediateParent should not match when only a grand‑parent satisfies evaluator", ip.matches(root, deepP));
    }

    @Test
    public void testImmediateParentToString() {
        Evaluator eval = new TagEvaluator("qux");
        StructuralEvaluator.ImmediateParent ip = new StructuralEvaluator.ImmediateParent(eval);
        assertEquals(":ImmediateParent:qux", ip.toString());
    }

    /* --------------------------------------------------------------------- *
     *  StructuralEvaluator.PreviousSibling
     * --------------------------------------------------------------------- */

    @Test
    public void testPreviousSiblingMatchesWhenAnyPreviousSiblingMatches() {
        // sibling3 (<c>) follows sibling2 (<b>) which follows sibling1 (<a>)
        // Evaluator matches tag "a" (the first sibling)
        Evaluator aEval = new TagEvaluator("a");
        StructuralEvaluator.PreviousSibling prev = new StructuralEvaluator.PreviousSibling(aEval);

        // Check on sibling3: there is an earlier sibling <a> that matches
        assertTrue("PreviousSibling should be true when any earlier sibling matches", prev.matches(root, sibling3));
    }

    @Test
    public void testPreviousSiblingDoesNotMatchWhenNoPreviousSiblingMatches() {
        Evaluator imgEval = new TagEvaluator("img");
        StructuralEvaluator.PreviousSibling prev = new StructuralEvaluator.PreviousSibling(imgEval);
        assertFalse("PreviousSibling should be false when no earlier sibling matches", prev.matches(root, sibling1));
    }

    @Test
    public void testPreviousSiblingStopsAtRoot() {
        // Ensure that the loop terminates without NPE when element has no previous sibling.
        Evaluator divEval = new TagEvaluator("div");
        StructuralEvaluator.PreviousSibling prev = new StructuralEvaluator.PreviousSibling(divEval);
        assertFalse("PreviousSibling should safely return false when element has no previous sibling", prev.matches(root, sibling1));
    }

    @Test
    public void testPreviousSiblingToString() {
        Evaluator eval = new TagEvaluator("fooPrev");
        StructuralEvaluator.PreviousSibling prev = new StructuralEvaluator.PreviousSibling(eval);
        assertEquals(":prev* :fooPrev", prev.toString());
    }

    /* --------------------------------------------------------------------- *
     *  StructuralEvaluator.ImmediatePreviousSibling
     * --------------------------------------------------------------------- */

    @Test
    public void testImmediatePreviousSiblingMatchesWhenImmediateMatches() {
        Evaluator bEval = new TagEvaluator("b");
        StructuralEvaluator.ImmediatePreviousSibling iprev = new StructuralEvaluator.ImmediatePreviousSibling(bEval);
        // sibling3 (<c>) has immediate previous sibling <b>
        assertTrue("ImmediatePreviousSibling should be true when immediate previous sibling matches", iprev.matches(root, sibling3));
    }

    @Test
    public void testImmediatePreviousSiblingDoesNotMatchWhenOnlyEarlierSiblingMatches() {
        Evaluator aEval = new TagEvaluator("a");
        StructuralEvaluator.ImmediatePreviousSibling iprev = new StructuralEvaluator.ImmediatePreviousSibling(aEval);
        // sibling3's immediate previous sibling is <b>, not <a>
        assertFalse("ImmediatePreviousSibling should be false when only a non‑immediate previous sibling matches", iprev.matches(root, sibling3));
    }

    @Test
    public void testImmediatePreviousSiblingToString() {
        Evaluator eval = new TagEvaluator("barPrev");
        StructuralEvaluator.ImmediatePreviousSibling iprev = new StructuralEvaluator.ImmediatePreviousSibling(eval);
        assertEquals(":prev:barPrev", iprev.toString());
    }

    /* --------------------------------------------------------------------- *
     *  Miscellaneous edge cases
     * --------------------------------------------------------------------- */

    @Test
    public void testAllEvaluatorsReturnFalseWhenRootEqualsElement() {
        // For every evaluator except Root, the contract says root == element => false.
        Evaluator anyEval = new TagEvaluator("any");
        StructuralEvaluator.Has has = new StructuralEvaluator.Has(anyEval);
        StructuralEvaluator.Not not = new StructuralEvaluator.Not(anyEval);
        StructuralEvaluator.Parent parent = new StructuralEvaluator.Parent(anyEval);
        StructuralEvaluator.ImmediateParent ip = new StructuralEvaluator.ImmediateParent(anyEval);
        StructuralEvaluator.PreviousSibling prev = new StructuralEvaluator.PreviousSibling(anyEval);
        StructuralEvaluator.ImmediatePreviousSibling iprev = new StructuralEvaluator.ImmediatePreviousSibling(anyEval);

        assertFalse("Has should be false when root == element", has.matches(root, root));
        assertFalse("Not should be false when root == element (negated inner false)", not.matches(root, root));
        assertFalse("Parent should be false when root == element", parent.matches(root, root));
        assertFalse("ImmediateParent should be false when root == element", ip.matches(root, root));
        assertFalse("PreviousSibling should be false when root == element", prev.matches(root, root));
        assertFalse("ImmediatePreviousSibling should be false when root == element", iprev.matches(root, root));
    }
}
