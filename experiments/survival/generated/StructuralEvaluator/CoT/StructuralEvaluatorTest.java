package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for the inner structural evaluator classes
 * defined in {@link StructuralEvaluator}.
 *
 * The tests cover:
 *   • Public constructors
 *   • {@code matches(..)} basic behaviour
 *   • Edge‑case handling (root == element, missing parents/siblings, null evaluator)
 *   • {@code toString()} formatting
 */
public class StructuralEvaluatorTest {

    /* --------------------------------------------------------------------- */
    /* Helper evaluator implementations                                      */
    /* --------------------------------------------------------------------- */

    /** An evaluator that matches every element. */
    private static class TrueEvaluator extends Evaluator {
        @Override
        public boolean matches(Element root, Element element) {
            return true;
        }

        @Override
        public String toString() {
            return ":true";
        }
    }

    /** An evaluator that matches only elements with a given tag name. */
    private static class TagEvaluator extends Evaluator {
        private final String tag;

        TagEvaluator(String tag) {
            this.tag = tag;
        }

        @Override
        public boolean matches(Element root, Element element) {
            return element.tagName().equals(tag);
        }

        @Override
        public String toString() {
            return ":tag(" + tag + ")";
        }
    }

    /** An evaluator that always returns false. */
    private static class FalseEvaluator extends Evaluator {
        @Override
        public boolean matches(Element root, Element element) {
            return false;
        }

        @Override
        public String toString() {
            return ":false";
        }
    }

    /* --------------------------------------------------------------------- */
    /* Sample DOM used by all tests                                          */
    /* --------------------------------------------------------------------- */

    /**
     * Creates a small document:
     *
     * <html>
     *   <body>
     *     <div id="a">
     *       <p id="p1">para</p>
     *       <span id="s1">span</span>
     *     </div>
     *     <div id="b">
     *       <p id="p2">para2</p>
     *     </div>
     *   </body>
     * </html>
     */
    private static Document sampleDocument() {
        String html = "<html><body>"
                + "<div id='a'><p id='p1'>para</p><span id='s1'>span</span></div>"
                + "<div id='b'><p id='p2'>para2</p></div>"
                + "</body></html>";
        return Jsoup.parse(html);
    }

    /* --------------------------------------------------------------------- */
    /* 1. StructuralEvaluator.Root                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testRootMatchesSameElement() {
        Document doc = sampleDocument();
        Element root = doc;                     // the Document itself is the root
        Element html = doc.selectFirst("html");

        StructuralEvaluator.Root rootEval = new StructuralEvaluator.Root();

        // root == element → true
        assertTrue(rootEval.matches(root, root));

        // different element → false
        assertFalse(rootEval.matches(root, html));
    }

    @Test
    public void testRootToString() {
        StructuralEvaluator.Root rootEval = new StructuralEvaluator.Root();
        // The class does not override toString(); default Object implementation is used.
        // We only assert that calling it does not throw.
        assertNotNull(rootEval.toString());
    }

    /* --------------------------------------------------------------------- */
    /* 2. StructuralEvaluator.Has                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testHasMatchesWhenDescendantMatches() {
        Document doc = sampleDocument();
        Element divA = doc.getElementById("a");
        Element p1 = doc.getElementById("p1");
        Element span1 = doc.getElementById("s1");

        // evaluator that matches any <span>
        StructuralEvaluator.Has hasEval = new StructuralEvaluator.Has(new TagEvaluator("span"));

        // divA contains a <span> as a descendant → true
        assertTrue(hasEval.matches(doc, divA));

        // p1 has no descendants → false
        assertFalse(hasEval.matches(doc, p1));

        // span1 (the element itself) should not cause a true result
        assertFalse(hasEval.matches(doc, span1));
    }

    @Test
    public void testHasReturnsFalseWhenNoDescendantsMatch() {
        Document doc = sampleDocument();
        Element divB = doc.getElementById("b");

        // evaluator that matches <span> – divB has only a <p>
        StructuralEvaluator.Has hasEval = new StructuralEvaluator.Has(new TagEvaluator("span"));
        assertFalse(hasEval.matches(doc, divB));
    }

    @Test
    public void testHasToStringFormatting() {
        Evaluator inner = new TagEvaluator("p");
        StructuralEvaluator.Has hasEval = new StructuralEvaluator.Has(inner);
        assertEquals(":has(:tag(p))", hasEval.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testHasWithNullEvaluatorThrowsNPE() {
        new StructuralEvaluator.Has(null).matches(sampleDocument(), sampleDocument().body());
    }

    /* --------------------------------------------------------------------- */
    /* 3. StructuralEvaluator.Not                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNotInvertsEvaluatorResult() {
        Document doc = sampleDocument();
        Element p1 = doc.getElementById("p1");

        StructuralEvaluator.Not notEval = new StructuralEvaluator.Not(new TagEvaluator("p"));
        // TagEvaluator matches <p>, so Not should be false for a <p>
        assertFalse(notEval.matches(doc, p1));

        // For a <span>, Not should be true
        Element span1 = doc.getElementById("s1");
        assertTrue(notEval.matches(doc, span1));
    }

    @Test
    public void testNotToStringFormatting() {
        Evaluator inner = new TagEvaluator("div");
        StructuralEvaluator.Not notEval = new StructuralEvaluator.Not(inner);
        assertEquals(":not:tag(div)", notEval.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testNotWithNullEvaluatorThrowsNPE() {
        new StructuralEvaluator.Not(null).matches(sampleDocument(), sampleDocument().body());
    }

    /* --------------------------------------------------------------------- */
    /* 4. StructuralEvaluator.Parent                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testParentMatchesWhenAncestorMatches() {
        Document doc = sampleDocument();
        Element p1 = doc.getElementById("p1"); // child of div#a
        Element divA = doc.getElementById("a"); // parent of p1
        Element body = doc.body();

        // evaluator that matches <div>
        StructuralEvaluator.Parent parentEval = new StructuralEvaluator.Parent(new TagEvaluator("div"));
        assertTrue(parentEval.matches(doc, p1)); // div#a is an ancestor

        // evaluator that matches <body>
        StructuralEvaluator.Parent bodyParentEval = new StructuralEvaluator.Parent(new TagEvaluator("body"));
        assertTrue(bodyParentEval.matches(doc, p1));

        // evaluator that matches <html> should also be true (ancestor chain up to root)
        StructuralEvaluator.Parent htmlParentEval = new StructuralEvaluator.Parent(new TagEvaluator("html"));
        assertTrue(htmlParentEval.matches(doc, p1));
    }

    @Test
    public void testParentReturnsFalseWhenNoMatchingAncestor() {
        Document doc = sampleDocument();
        Element p1 = doc.getElementById("p1");

        // evaluator that matches <span> – there is no <span> in the ancestor chain
        StructuralEvaluator.Parent parentEval = new StructuralEvaluator.Parent(new TagEvaluator("span"));
        assertFalse(parentEval.matches(doc, p1));
    }

    @Test
    public void testParentRootEqualsElementReturnsFalse() {
        Document doc = sampleDocument();
        StructuralEvaluator.Parent parentEval = new StructuralEvaluator.Parent(new TrueEvaluator());
        assertFalse(parentEval.matches(doc, doc)); // root == element
    }

    @Test
    public void testParentToStringFormatting() {
        Evaluator inner = new TagEvaluator("li");
        StructuralEvaluator.Parent parentEval = new StructuralEvaluator.Parent(inner);
        assertEquals(":parent:tag(li)", parentEval.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testParentWithNullEvaluatorThrowsNPE() {
        new StructuralEvaluator.Parent(null).matches(sampleDocument(), sampleDocument().body());
    }

    /* --------------------------------------------------------------------- */
    /* 5. StructuralEvaluator.ImmediateParent                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testImmediateParentMatchesWhenDirectParentMatches() {
        Document doc = sampleDocument();
        Element p1 = doc.getElementById("p1"); // parent is <div id="a">
        StructuralEvaluator.ImmediateParent ipEval = new StructuralEvaluator.ImmediateParent(new TagEvaluator("div"));
        assertTrue(ipEval.matches(doc, p1));

        // parent is <body> for the outer <div>
        Element divB = doc.getElementById("b");
        StructuralEvaluator.ImmediateParent ipBodyEval = new StructuralEvaluator.ImmediateParent(new TagEvaluator("body"));
        assertTrue(ipBodyEval.matches(doc, divB));
    }

    @Test
    public void testImmediateParentReturnsFalseWhenParentDoesNotMatch() {
        Document doc = sampleDocument();
        Element p1 = doc.getElementById("p1");
        StructuralEvaluator.ImmediateParent ipEval = new StructuralEvaluator.ImmediateParent(new TagEvaluator("span"));
        assertFalse(ipEval.matches(doc, p1));
    }

    @Test
    public void testImmediateParentWhenElementHasNoParent() {
        Document doc = sampleDocument();
        // the Document itself has no parent; passing it as element should return false
        StructuralEvaluator.ImmediateParent ipEval = new StructuralEvaluator.ImmediateParent(new TrueEvaluator());
        assertFalse(ipEval.matches(doc, doc));
    }

    @Test
    public void testImmediateParentToStringFormatting() {
        Evaluator inner = new TagEvaluator("a");
        StructuralEvaluator.ImmediateParent ipEval = new StructuralEvaluator.ImmediateParent(inner);
        assertEquals(":ImmediateParent:tag(a)", ipEval.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testImmediateParentWithNullEvaluatorThrowsNPE() {
        new StructuralEvaluator.ImmediateParent(null).matches(sampleDocument(), sampleDocument().body());
    }

    /* --------------------------------------------------------------------- */
    /* 6. StructuralEvaluator.PreviousSibling                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPreviousSiblingFindsMatchingSibling() {
        Document doc = sampleDocument();
        // In <div id='a'> the <span> follows the <p>
        Element span1 = doc.getElementById("s1");
        StructuralEvaluator.PreviousSibling prevEval = new StructuralEvaluator.PreviousSibling(new TagEvaluator("p"));
        assertTrue(prevEval.matches(doc, span1));
    }

    @Test
    public void testPreviousSiblingReturnsFalseWhenNoMatchingSibling() {
        Document doc = sampleDocument();
        Element p1 = doc.getElementById("p1");
        StructuralEvaluator.PreviousSibling prevEval = new StructuralEvaluator.PreviousSibling(new TagEvaluator("span"));
        // p1 is the first child → no previous sibling
        assertFalse(prevEval.matches(doc, p1));
    }

    @Test
    public void testPreviousSiblingStopsAtRoot() {
        Document doc = sampleDocument();
        Element body = doc.body();

        // evaluator that matches <html> – previous sibling chain never reaches root, should be false
        StructuralEvaluator.PreviousSibling prevEval = new StructuralEvaluator.PreviousSibling(new TagEvaluator("html"));
        assertFalse(prevEval.matches(doc, body));
    }

    @Test
    public void testPreviousSiblingToStringFormatting() {
        Evaluator inner = new TagEvaluator("li");
        StructuralEvaluator.PreviousSibling prevEval = new StructuralEvaluator.PreviousSibling(inner);
        assertEquals(":prev*:tag(li)", prevEval.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testPreviousSiblingWithNullEvaluatorThrowsNPE() {
        new StructuralEvaluator.PreviousSibling(null).matches(sampleDocument(), sampleDocument().body());
    }

    /* --------------------------------------------------------------------- */
    /* 7. StructuralEvaluator.ImmediatePreviousSibling                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testImmediatePreviousSiblingMatchesWhenImmediateMatches() {
        Document doc = sampleDocument();
        Element span1 = doc.getElementById("s1");
        StructuralEvaluator.ImmediatePreviousSibling iPrevEval =
                new StructuralEvaluator.ImmediatePreviousSibling(new TagEvaluator("p"));
        assertTrue(iPrevEval.matches(doc, span1));
    }

    @Test
    public void testImmediatePreviousSiblingReturnsFalseWhenNotImmediate() {
        Document doc = sampleDocument();
        // In <div id='b'> there is only one child <p>, so no previous sibling at all
        Element p2 = doc.getElementById("p2");
        StructuralEvaluator.ImmediatePreviousSibling iPrevEval =
                new StructuralEvaluator.ImmediatePreviousSibling(new TagEvaluator("div"));
        assertFalse(iPrevEval.matches(doc, p2));
    }

    @Test
    public void testImmediatePreviousSiblingToStringFormatting() {
        Evaluator inner = new TagEvaluator("a");
        StructuralEvaluator.ImmediatePreviousSibling iPrevEval =
                new StructuralEvaluator.ImmediatePreviousSibling(inner);
        assertEquals(":prev:tag(a)", iPrevEval.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testImmediatePreviousSiblingWithNullEvaluatorThrowsNPE() {
        new StructuralEvaluator.ImmediatePreviousSibling(null).matches(sampleDocument(), sampleDocument().body());
    }
}
