package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link StructuralEvaluator} and its
 * nested static classes.  The tests cover:
 * <ul>
 *   <li>Normal behaviour (typical use cases)</li>
 *   <li>Edge cases such as root element, missing parents/siblings, etc.</li>
 *   <li>String representations via {@code toString()}</li>
 * </ul>
 *
 * The tests use a tiny HTML fragment and a simple custom {@link Evaluator}
 * implementation that matches elements by tag name or by an attribute value.
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
            return element != null && tag.equalsIgnoreCase(element.tagName());
        }

        @Override
        public String toString() {
            return "tag=" + tag;
        }
    }

    /** Simple evaluator that matches an element by a specific attribute/value pair. */
    private static class AttrEvaluator extends Evaluator {
        private final String attr;
        private final String value;

        AttrEvaluator(String attr, String value) {
            this.attr = attr;
            this.value = value;
        }

        @Override
        public boolean matches(Element root, Element element) {
            return element != null && value.equals(element.attr(attr));
        }

        @Override
        public String toString() {
            return "attr[" + attr + "=" + value + "]";
        }
    }

    /* --------------------------------------------------------------------- */
    /* --------------------------- ROOT ------------------------------------ */
    /* --------------------------------------------------------------------- */

    @Test
    public void testRootMatches_SameElement() {
        Document doc = Jsoup.parse("<div id='root'></div>");
        Element root = doc.selectFirst("div");
        StructuralEvaluator.Root evaluator = new StructuralEvaluator.Root();

        assertTrue("Root should match itself", evaluator.matches(root, root));
    }

    @Test
    public void testRootMatches_DifferentElement() {
        Document doc = Jsoup.parse("<div id='root'><p id='child'></p></div>");
        Element root = doc.selectFirst("div");
        Element child = doc.selectFirst("p");
        StructuralEvaluator.Root evaluator = new StructuralEvaluator.Root();

        assertFalse("Root must not match a different element", evaluator.matches(root, child));
    }

    @Test
    public void testRootToString() {
        StructuralEvaluator.Root evaluator = new StructuralEvaluator.Root();
        assertEquals("Root toString should be empty (inherits from Evaluator)", "", evaluator.toString());
    }

    /* --------------------------------------------------------------------- */
    /* --------------------------- HAS ------------------------------------- */
    /* --------------------------------------------------------------------- */

    @Test
    public void testHasMatches_WithMatchingDescendant() {
        Document doc = Jsoup.parse("<div><span></span><p></p></div>");
        Element root = doc.selectFirst("div");
        StructuralEvaluator.Has evaluator = new StructuralEvaluator.Has(new TagEvaluator("p"));

        // The element itself is the root; it contains a <p> descendant.
        assertTrue(evaluator.matches(root, root));
    }

    @Test
    public void testHasMatches_NoMatchingDescendant() {
        Document doc = Jsoup.parse("<div><span></span></div>");
        Element root = doc.selectFirst("div");
        StructuralEvaluator.Has evaluator = new StructuralEvaluator.Has(new TagEvaluator("p"));

        assertFalse(evaluator.matches(root, root));
    }

    @Test
    public void testHasMatches_ElementIsRootButHasNoDescendants() {
        Document doc = Jsoup.parse("<div></div>");
        Element root = doc.selectFirst("div");
        StructuralEvaluator.Has evaluator = new StructuralEvaluator.Has(new TagEvaluator("p"));

        assertFalse(evaluator.matches(root, root));
    }

    @Test
    public void testHasToString() {
        StructuralEvaluator.Has evaluator = new StructuralEvaluator.Has(new TagEvaluator("p"));
        assertEquals(":has(tag=p)", evaluator.toString());
    }

    /* --------------------------------------------------------------------- */
    /* --------------------------- NOT ------------------------------------- */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNotMatches_NegatesUnderlyingEvaluator() {
        Document doc = Jsoup.parse("<div><p></p></div>");
        Element root = doc.selectFirst("div");
        Element p = doc.selectFirst("p");
        StructuralEvaluator.Not evaluator = new StructuralEvaluator.Not(new TagEvaluator("p"));

        // Underlying evaluator matches <p>, so Not should return false.
        assertFalse(evaluator.matches(root, p));

        // For the <div> element, underlying evaluator is false -> Not true.
        assertTrue(evaluator.matches(root, root));
    }

    @Test
    public void testNotToString() {
        StructuralEvaluator.Not evaluator = new StructuralEvaluator.Not(new TagEvaluator("p"));
        assertEquals(":nottag=p", evaluator.toString());
    }

    /* --------------------------------------------------------------------- */
    /* --------------------------- PARENT ---------------------------------- */
    /* --------------------------------------------------------------------- */

    @Test
    public void testParentMatches_AncestorMatches() {
        Document doc = Jsoup.parse("<section><div><p></p></div></section>");
        Element root = doc.selectFirst("section");
        Element p = doc.selectFirst("p");
        StructuralEvaluator.Parent evaluator = new StructuralEvaluator.Parent(new TagEvaluator("div"));

        // The direct parent of <p> is <div>, which matches.
        assertTrue(evaluator.matches(root, p));
    }

    @Test
    public void testParentMatches_NoAncestorMatches() {
        Document doc = Jsoup.parse("<section><div><p></p></div></section>");
        Element root = doc.selectFirst("section");
        Element p = doc.selectFirst("p");
        StructuralEvaluator.Parent evaluator = new StructuralEvaluator.Parent(new TagEvaluator("section"));

        // The root element is excluded from matching; thus should be false.
        assertFalse(evaluator.matches(root, p));
    }

    @Test
    public void testParentMatches_ElementIsRoot() {
        Document doc = Jsoup.parse("<div></div>");
        Element root = doc.selectFirst("div");
        StructuralEvaluator.Parent evaluator = new StructuralEvaluator.Parent(new TagEvaluator("div"));

        assertFalse(evaluator.matches(root, root));
    }

    @Test
    public void testParentToString() {
        StructuralEvaluator.Parent evaluator = new StructuralEvaluator.Parent(new TagEvaluator("div"));
        assertEquals(":parenttag=div", evaluator.toString());
    }

    /* --------------------------------------------------------------------- */
    /* --------------------- IMMEDIATE PARENT ------------------------------- */
    /* --------------------------------------------------------------------- */

    @Test
    public void testImmediateParentMatches_DirectParentMatches() {
        Document doc = Jsoup.parse("<ul><li><span></span></li></ul>");
        Element root = doc.selectFirst("ul");
        Element span = doc.selectFirst("span");
        StructuralEvaluator.ImmediateParent evaluator = new StructuralEvaluator.ImmediateParent(new TagEvaluator("li"));

        assertTrue(evaluator.matches(root, span));
    }

    @Test
    public void testImmediateParentMatches_DirectParentDoesNotMatch() {
        Document doc = Jsoup.parse("<ul><li><span></span></li></ul>");
        Element root = doc.selectFirst("ul");
        Element span = doc.selectFirst("span");
        StructuralEvaluator.ImmediateParent evaluator = new StructuralEvaluator.ImmediateParent(new TagEvaluator("ul"));

        // Direct parent is <li>, not <ul>.
        assertFalse(evaluator.matches(root, span));
    }

    @Test
    public void testImmediateParentMatches_NoParent() {
        Document doc = Jsoup.parse("<div></div>");
        Element root = doc.selectFirst("div");
        StructuralEvaluator.ImmediateParent evaluator = new StructuralEvaluator.ImmediateParent(new TagEvaluator("div"));

        // The element has no parent (apart from the implicit document root) – should be false.
        assertFalse(evaluator.matches(root, root));
    }

    @Test
    public void testImmediateParentToString() {
        StructuralEvaluator.ImmediateParent evaluator = new StructuralEvaluator.ImmediateParent(new TagEvaluator("li"));
        assertEquals(":ImmediateParenttag=li", evaluator.toString());
    }

    /* --------------------------------------------------------------------- */
    /* --------------------- PREVIOUS SIBLING ------------------------------ */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPreviousSiblingMatches_AnyPreviousMatches() {
        Document doc = Jsoup.parse("<ul><li id='a'></li><li id='b'></li><li id='c'></li></ul>");
        Element root = doc.selectFirst("ul");
        Element c = doc.selectFirst("li[id=c]");
        StructuralEvaluator.PreviousSibling evaluator = new StructuralEvaluator.PreviousSibling(new AttrEvaluator("id", "a"));

        // 'a' is a previous sibling of 'c' (two steps away) – should be true.
        assertTrue(evaluator.matches(root, c));
    }

    @Test
    public void testPreviousSiblingMatches_NoneMatch() {
        Document doc = Jsoup.parse("<ul><li id='a'></li><li id='b'></li></ul>");
        Element root = doc.selectFirst("ul");
        Element b = doc.selectFirst("li[id=b]");
        StructuralEvaluator.PreviousSibling evaluator = new StructuralEvaluator.PreviousSibling(new AttrEvaluator("id", "c"));

        assertFalse(evaluator.matches(root, b));
    }

    @Test
    public void testPreviousSiblingMatches_ElementIsRoot() {
        Document doc = Jsoup.parse("<ul></ul>");
        Element root = doc.selectFirst("ul");
        StructuralEvaluator.PreviousSibling evaluator = new StructuralEvaluator.PreviousSibling(new TagEvaluator("li"));

        assertFalse(evaluator.matches(root, root));
    }

    @Test
    public void testPreviousSiblingToString() {
        StructuralEvaluator.PreviousSibling evaluator = new StructuralEvaluator.PreviousSibling(new TagEvaluator("li"));
        assertEquals(":prev*tag=li", evaluator.toString());
    }

    /* --------------------------------------------------------------------- */
    /* ----------- IMMEDIATE PREVIOUS SIBLING ------------------------------- */
    /* --------------------------------------------------------------------- */

    @Test
    public void testImmediatePreviousSiblingMatches_DirectPreviousMatches() {
        Document doc = Jsoup.parse("<ul><li id='first'></li><li id='second'></li></ul>");
        Element root = doc.selectFirst("ul");
        Element second = doc.selectFirst("li[id=second]");
        StructuralEvaluator.ImmediatePreviousSibling evaluator =
                new StructuralEvaluator.ImmediatePreviousSibling(new AttrEvaluator("id", "first"));

        assertTrue(evaluator.matches(root, second));
    }

    @Test
    public void testImmediatePreviousSiblingMatches_DirectPreviousDoesNotMatch() {
        Document doc = Jsoup.parse("<ul><li id='first'></li><li id='second'></li></ul>");
        Element root = doc.selectFirst("ul");
        Element second = doc.selectFirst("li[id=second]");
        StructuralEvaluator.ImmediatePreviousSibling evaluator =
                new StructuralEvaluator.ImmediatePreviousSibling(new AttrEvaluator("id", "nonexistent"));

        assertFalse(evaluator.matches(root, second));
    }

    @Test
    public void testImmediatePreviousSiblingMatches_NoPreviousSibling() {
        Document doc = Jsoup.parse("<ul><li id='only'></li></ul>");
        Element root = doc.selectFirst("ul");
        Element only = doc.selectFirst("li[id=only]");
        StructuralEvaluator.ImmediatePreviousSibling evaluator =
                new StructuralEvaluator.ImmediatePreviousSibling(new TagEvaluator("li"));

        assertFalse(evaluator.matches(root, only));
    }

    @Test
    public void testImmediatePreviousSiblingToString() {
        StructuralEvaluator.ImmediatePreviousSibling evaluator =
                new StructuralEvaluator.ImmediatePreviousSibling(new TagEvaluator("li"));
        assertEquals(":prevtag=li", evaluator.toString());
    }
}
