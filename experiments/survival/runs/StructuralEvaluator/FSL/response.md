**###Test START##**  
```java
package org.jsoup.select;

import static org.junit.Assert.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;

/**
 * Unit tests for the {@link StructuralEvaluator} inner classes.
 * The tests build small HTML fragments with Jsoup and verify the
 * semantics of each structural evaluator (Root, Has, Not, Parent,
 * ImmediateParent, PreviousSibling and ImmediatePreviousSibling).
 */
public class StructuralEvaluatorTest {

    /** Simple evaluator that matches an element by its tag name. */
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
            return tag;
        }
    }

    /** Simple evaluator that matches an element by an attribute value. */
    private static class AttrEvaluator extends Evaluator {
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

        @Override
        public String toString() {
            return String.format("[%s=%s]", attrKey, attrValue);
        }
    }

    /* -------------------------------------------------------------------- */
    /* Root evaluator                                                       */
    /* -------------------------------------------------------------------- */
    @Test
    public void testRootMatches() {
        Document doc = Jsoup.parse("<div id='root'><p>text</p></div>");
        Element root = doc.selectFirst("#root");
        Element p = doc.selectFirst("p");

        Evaluator rootEval = new StructuralEvaluator.Root();

        // root matches itself
        assertTrue(rootEval.matches(root, root));

        // root does NOT match a different element
        assertFalse(rootEval.matches(root, p));
    }

    /* -------------------------------------------------------------------- */
    /* Has evaluator                                                         */
    /* -------------------------------------------------------------------- */
    @Test
    public void testHasMatchesWhenDescendantExists() {
        Document doc = Jsoup.parse(
                "<div id='container'>" +
                        "  <section>" +
                        "    <p>Paragraph</p>" +
                        "    <b>Bold</b>" +
                        "  </section>" +
                        "</div>");
        Element container = doc.selectFirst("#container");
        Element section = doc.selectFirst("section");
        Evaluator hasBold = new StructuralEvaluator.Has(new TagEvaluator("b"));

        // section contains a <b> descendant (not itself) → true
        assertTrue(hasBold.matches(container, section));

        // container also contains a <b> descendant → true
        assertTrue(hasBold.matches(container, container));

        // element without such descendant → false
        Evaluator hasImg = new StructuralEvaluator.Has(new TagEvaluator("img"));
        assertFalse(hasImg.matches(container, section));
    }

    @Test
    public void testHasDoesNotCountSelf() {
        Document doc = Jsoup.parse("<div id='self'><span>inner</span></div>");
        Element div = doc.selectFirst("#self");
        Evaluator hasDiv = new StructuralEvaluator.Has(new TagEvaluator("div"));

        // The element is a <div> but should not be considered a descendant of itself
        assertFalse(hasDiv.matches(div, div));
    }

    /* -------------------------------------------------------------------- */
    /* Not evaluator                                                         */
    /* -------------------------------------------------------------------- */
    @Test
    public void testNotInvertsResult() {
        Document doc = Jsoup.parse("<ul><li class='item'>One</li></ul>");
        Element li = doc.selectFirst("li");
        Evaluator notItemClass = new StructuralEvaluator.Not(new AttrEvaluator("class", "item"));

        // The element matches the attribute evaluator, so Not should be false
        assertFalse(notItemClass.matches(li, li));

        // Change attribute so inner evaluator returns false → Not should be true
        li.removeAttr("class");
        assertTrue(notItemClass.matches(li, li));
    }

    /* -------------------------------------------------------------------- */
    /* Parent evaluator                                                      */
    /* -------------------------------------------------------------------- */
    @Test
    public void testParentMatchesAncestor() {
        Document doc = Jsoup.parse(
                "<html>" +
                        " <body>" +
                        "   <section id='sec'>" +
                        "     <div>" +
                        "       <p id='target'>Text</p>" +
                        "     </div>" +
                        "   </section>" +
                        " </body>" +
                        "</html>");
        Element root = doc; // the Document itself is the root
        Element target = doc.getElementById("target");
        Evaluator parentIsSection = new StructuralEvaluator.Parent(new AttrEvaluator("id", "sec"));

        // The <p> has <section id='sec'> as an ancestor → true
        assertTrue(parentIsSection.matches(root, target));

        // Direct parent is a <div>, not a <section> → still true because ancestor chain is checked
        Evaluator parentIsDiv = new StructuralEvaluator.Parent(new TagEvaluator("div"));
        assertTrue(parentIsDiv.matches(root, target));

        // No matching ancestor → false
        Evaluator parentIsHeader = new StructuralEvaluator.Parent(new TagEvaluator("header"));
        assertFalse(parentIsHeader.matches(root, target));
    }

    @Test
    public void testParentRootEqualityReturnsFalse() {
        Document doc = Jsoup.parse("<div></div>");
        Evaluator parentAny = new StructuralEvaluator.Parent(new TagEvaluator("div"));
        // root == element, should be false irrespective of evaluator
        assertFalse(parentAny.matches(doc, doc));
    }

    /* -------------------------------------------------------------------- */
    /* ImmediateParent evaluator                                             */
    /* -------------------------------------------------------------------- */
    @Test
    public void testImmediateParentMatchesDirectParentOnly() {
        Document doc = Jsoup.parse(
                "<div id='grand'>" +
                        "  <section id='parent'>" +
                        "    <p id='child'>Hello</p>" +
                        "  </section>" +
                        "</div>");
        Element child = doc.getElementById("child");
        Element root = doc; // root is the Document

        // Direct parent is <section id='parent'>
        Evaluator immediateParentIsSection = new StructuralEvaluator.ImmediateParent(new AttrEvaluator("id", "parent"));
        assertTrue(immediateParentIsSection.matches(root, child));

        // Direct parent is NOT <div id='grand'>, even though it is an ancestor
        Evaluator immediateParentIsDiv = new StructuralEvaluator.ImmediateParent(new AttrEvaluator("id", "grand"));
        assertFalse(immediateParentIsDiv.matches(root, child));
    }

    @Test
    public void testImmediateParentRootEqualityReturnsFalse() {
        Document doc = Jsoup.parse("<span></span>");
        Evaluator ip = new StructuralEvaluator.ImmediateParent(new TagEvaluator("span"));
        assertFalse(ip.matches(doc, doc));
    }

    /* -------------------------------------------------------------------- */
    /* PreviousSibling evaluator                                            */
    /* -------------------------------------------------------------------- */
    @Test
    public void testPreviousSiblingMatchesAnyEarlierSibling() {
        Document doc = Jsoup.parse(
                "<ul>" +
                        "  <li id='first'>1</li>" +
                        "  <li id='second'>2</li>" +
                        "  <li id='third'>3</li>" +
                        "</ul>");
        Element third = doc.getElementById("third");

        // Look for any previous <li> with id='first'
        Evaluator prevHasFirst = new StructuralEvaluator.PreviousSibling(new AttrEvaluator("id", "first"));
        assertTrue(prevHasFirst.matches(doc, third));

        // Look for a previous <li> with class='missing' → should be false
        Evaluator prevHasMissing = new StructuralEvaluator.PreviousSibling(new AttrEvaluator("class", "missing"));
        assertFalse(prevHasMissing.matches(doc, third));
    }

    @Test
    public void testPreviousSiblingIgnoresSelfAndStopsAtRoot() {
        Document doc = Jsoup.parse("<div><p id='solo'>Solo</p></div>");
        Element solo = doc.getElementById("solo");

        // No previous sibling exists
        Evaluator anyPrev = new StructuralEvaluator.PreviousSibling(new TagEvaluator("p"));
        assertFalse(anyPrev.matches(doc, solo));
    }

    /* -------------------------------------------------------------------- */
    /* ImmediatePreviousSibling evaluator                                   */
    /* -------------------------------------------------------------------- */
    @Test
    public void testImmediatePreviousSiblingMatchesOnlyDirectSibling() {
        Document doc = Jsoup.parse(
                "<ol>" +
                        "  <li id='a'>A</li>" +
                        "  <li id='b'>B</li>" +
                        "  <li id='c'>C</li>" +
                        "</ol>");
        Element c = doc.getElementById("c");

        // Direct previous sibling is <li id='b'>
        Evaluator immediatePrevIsB = new StructuralEvaluator.ImmediatePreviousSibling(new AttrEvaluator("id", "b"));
        assertTrue(immediatePrevIsB.matches(doc, c));

        // Direct previous sibling is NOT <li id='a'> (it's two steps away)
        Evaluator immediatePrevIsA = new StructuralEvaluator.ImmediatePreviousSibling(new AttrEvaluator("id", "a"));
        assertFalse(immediatePrevIsA.matches(doc, c));
    }

    @Test
    public void testImmediatePreviousSiblingRootEqualityReturnsFalse() {
        Document doc = Jsoup.parse("<div></div>");
        Evaluator iprev = new StructuralEvaluator.ImmediatePreviousSibling(new TagEvaluator("div"));
        assertFalse(iprev.matches(doc, doc));
    }

    /* -------------------------------------------------------------------- */
    /* toString() verification for each structural evaluator                */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToStringFormats() {
        Evaluator tagA = new TagEvaluator("a");
        assertEquals(":has(a)", new StructuralEvaluator.Has(tagA).toString());
        assertEquals(":nota", new StructuralEvaluator.Not(tagA).toString());
        assertEquals(":parenta", new StructuralEvaluator.Parent(tagA).toString());
        assertEquals(":ImmediateParenta", new StructuralEvaluator.ImmediateParent(tagA).toString());
        assertEquals(":prev*a", new StructuralEvaluator.PreviousSibling(tagA).toString());
        assertEquals(":preva", new StructuralEvaluator.ImmediatePreviousSibling(tagA).toString());
    }
}
```
**###Test END##**