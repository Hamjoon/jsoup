###Test START##
```java
package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link StructuralEvaluator} and its
 * concrete inner classes.  The tests cover normal behaviour, edge cases and
 * a few error scenarios (e.g. {@code null} evaluator).
 *
 * The test class lives in the same package as {@code StructuralEvaluator}
 * so that the package‑private classes can be accessed directly.
 */
public class StructuralEvaluatorTest {

    /** Simple evaluator that matches elements by tag name. */
    private static class TagEvaluator implements Evaluator {
        private final String tag;

        TagEvaluator(String tag) {
            this.tag = tag;
        }

        @Override
        public boolean matches(Element root, Element element) {
            return element != null && element.tagName().equals(tag);
        }

        @Override
        public String toString() {
            return ":" + tag;
        }
    }

    /** Evaluator that always returns true (used for NOT tests). */
    private static final Evaluator ALWAYS_TRUE = new Evaluator() {
        @Override public boolean matches(Element root, Element element) { return true; }
        @Override public String toString() { return ":*"; }
    };

    /** Evaluator that always returns false (used for NOT tests). */
    private static final Evaluator ALWAYS_FALSE = new Evaluator() {
        @Override public boolean matches(Element root, Element element) { return false; }
        @Override public String toString() { return ":!"; }
    };

    /** Helper to build a tiny DOM used by many tests. */
    private Document parse(String html) {
        return Jsoup.parse(html);
    }

    /* --------------------------------------------------------------------- *
     *  ROOT
     * --------------------------------------------------------------------- */

    @Test
    public void rootMatchesOnlyItself() {
        Document doc = parse("<div id='root'><p>text</p></div>");
        Element root = doc.selectFirst("div");
        Element child = doc.selectFirst("p");

        StructuralEvaluator.Root rootEval = new StructuralEvaluator.Root();

        assertTrue("Root must match itself", rootEval.matches(root, root));
        assertFalse("Root must not match a different element", rootEval.matches(root, child));
    }

    /* --------------------------------------------------------------------- *
     *  HAS
     * --------------------------------------------------------------------- */

    @Test
    public void hasMatchesWhenDescendantSatisfiesEvaluator() {
        Document doc = parse("<div><span class='target'></span><p></p></div>");
        Element div = doc.selectFirst("div");
        Evaluator spanEval = new TagEvaluator("span");
        StructuralEvaluator.Has hasEval = new StructuralEvaluator.Has(spanEval);

        assertTrue("Has should succeed when a descendant matches", hasEval.matches(div, div));
        // The element itself should never be considered – ensure it does not affect result
        assertTrue("Has must still succeed when called on the matching element itself",
                hasEval.matches(div, div));
    }

    @Test
    public void hasDoesNotMatchIfNoDescendantMatches() {
        Document doc = parse("<div><p></p></div>");
        Element div = doc.selectFirst("div");
        Evaluator spanEval = new TagEvaluator("span");
        StructuralEvaluator.Has hasEval = new StructuralEvaluator.Has(spanEval);

        assertFalse("Has must fail when none of the descendants match", hasEval.matches(div, div));
    }

    @Test
    public void hasExcludesSelfFromEvaluation() {
        Document doc = parse("<div class='target'></div>");
        Element div = doc.selectFirst("div");
        Evaluator classEval = new Evaluator() {
            @Override public boolean matches(Element root, Element el) {
                return el.hasClass("target");
            }
            @Override public String toString() { return ":hasClass(target)"; }
        };
        StructuralEvaluator.Has hasEval = new StructuralEvaluator.Has(classEval);
        // Even though the element has the class, it must not be counted as a descendant.
        assertFalse("Has must not count the element itself", hasEval.matches(div, div));
    }

    @Test
    public void hasToStringFormatting() {
        StructuralEvaluator.Has hasEval = new StructuralEvaluator.Has(new TagEvaluator("span"));
        assertEquals(":has(:span)", hasEval.toString());
    }

    /* --------------------------------------------------------------------- *
     *  NOT
     * --------------------------------------------------------------------- */

    @Test
    public void notInvertsEvaluatorResult() {
        Document doc = parse("<div><span></span></div>");
        Element div = doc.selectFirst("div");
        Element span = doc.selectFirst("span");

        StructuralEvaluator.Not notTrue = new StructuralEvaluator.Not(ALWAYS_TRUE);
        StructuralEvaluator.Not notFalse = new StructuralEvaluator.Not(ALWAYS_FALSE);

        assertFalse("NOT of always‑true must be false", notTrue.matches(div, div));
        assertTrue("NOT of always‑false must be true", notFalse.matches(div, div));

        // also test with a real evaluator
        StructuralEvaluator.Not notSpan = new StructuralEvaluator.Not(new TagEvaluator("span"));
        assertTrue("Div is not a span", notSpan.matches(div, div));
        assertFalse("Span is a span, therefore NOT should be false", notSpan.matches(div, span));
    }

    @Test
    public void notToStringFormatting() {
        StructuralEvaluator.Not notEval = new StructuralEvaluator.Not(new TagEvaluator("p"));
        assertEquals(":not:p", notEval.toString());
    }

    /* --------------------------------------------------------------------- *
     *  PARENT
     * --------------------------------------------------------------------- */

    @Test
    public void parentMatchesAnyAncestor() {
        Document doc = parse("<html><body><section><article id='a'></article></section></body></html>");
        Element article = doc.getElementById("a");
        Element body = doc.selectFirst("body");
        Evaluator bodyEval = new Evaluator() {
            @Override public boolean matches(Element root, Element el) { return el == body; }
            @Override public String toString() { return ":body"; }
        };
        StructuralEvaluator.Parent parentEval = new StructuralEvaluator.Parent(bodyEval);
        assertTrue("Parent should match when body is an ancestor of article", parentEval.matches(doc, article));
    }

    @Test
    public void parentStopsAtRootAndReturnsFalseIfRootIsNotMatched() {
        Document doc = parse("<div><p id='p'></p></div>");
        Element p = doc.getElementById("p");
        Element div = doc.selectFirst("div");
        Evaluator divEval = new Evaluator() {
            @Override public boolean matches(Element root, Element el) { return el == div; }
            @Override public String toString() { return ":div"; }
        };
        StructuralEvaluator.Parent parentEval = new StructuralEvaluator.Parent(divEval);
        // root (document) is not the div, so it should return false.
        assertFalse("Parent must return false when the only ancestor is the root and does not match",
                parentEval.matches(doc, p));
    }

    @Test
    public void parentReturnsFalseWhenElementIsRoot() {
        Document doc = parse("<html></html>");
        StructuralEvaluator.Parent parentEval = new StructuralEvaluator.Parent(ALWAYS_TRUE);
        assertFalse("Parent should never match when element == root", parentEval.matches(doc, doc));
    }

    @Test
    public void parentToStringFormatting() {
        StructuralEvaluator.Parent parentEval = new StructuralEvaluator.Parent(new TagEvaluator("section"));
        assertEquals(":parent:section", parentEval.toString());
    }

    /* --------------------------------------------------------------------- *
     *  IMMEDIATE PARENT
     * --------------------------------------------------------------------- */

    @Test
    public void immediateParentMatchesDirectParentOnly() {
        Document doc = parse("<div><section id='s'><p id='p'></p></section></div>");
        Element p = doc.getElementById("p");
        Element s = doc.getElementById("s");
        Evaluator secEval = new Evaluator() {
            @Override public boolean matches(Element root, Element el) { return el == s; }
            @Override public String toString() { return ":section#s"; }
        };
        StructuralEvaluator.ImmediateParent immParent = new StructuralEvaluator.ImmediateParent(secEval);
        assertTrue("Should match when direct parent satisfies evaluator", immParent.matches(doc, p));
    }

    @Test
    public void immediateParentDoesNotMatchGrandParent() {
        Document doc = parse("<div><section><p id='p'></p></section></div>");
        Element p = doc.getElementById("p");
        StructuralEvaluator.ImmediateParent immParent = new StructuralEvaluator.ImmediateParent(new TagEvaluator("div"));
        assertFalse("ImmediateParent must not climb more than one level", immParent.matches(doc, p));
    }

    @Test
    public void immediateParentReturnsFalseWhenNoParent() {
        Document doc = parse("<p id='solo'></p>");
        Element p = doc.getElementById("solo");
        StructuralEvaluator.ImmediateParent immParent = new StructuralEvaluator.ImmediateParent(ALWAYS_TRUE);
        assertFalse("Element without a parent should return false", immParent.matches(doc, p));
    }

    @Test
    public void immediateParentToStringFormatting() {
        StructuralEvaluator.ImmediateParent immParent = new StructuralEvaluator.ImmediateParent(new TagEvaluator("ul"));
        assertEquals(":ImmediateParent:ul", immParent.toString());
    }

    /* --------------------------------------------------------------------- *
     *  PREVIOUS SIBLING
     * --------------------------------------------------------------------- */

    @Test
    public void previousSiblingMatchesAnyEarlierSibling() {
        Document doc = parse("<ul><li id='a'></li><li id='b'></li><li id='c'></li></ul>");
        Element c = doc.getElementById("c");
        Evaluator aEval = new Evaluator() {
            @Override public boolean matches(Element root, Element el) { return el.id().equals("a"); }
            @Override public String toString() { return ":id(a)"; }
        };
        StructuralEvaluator.PreviousSibling prevSibling = new StructuralEvaluator.PreviousSibling(aEval);
        assertTrue("c should match because an earlier sibling (a) satisfies evaluator", prevSibling.matches(doc, c));
    }

    @Test
    public void previousSiblingReturnsFalseWhenNoEarlierSiblingMatches() {
        Document doc = parse("<ul><li id='a'></li><li id='b'></li></ul>");
        Element b = doc.getElementById("b");
        StructuralEvaluator.PreviousSibling prevSibling = new StructuralEvaluator.PreviousSibling(new TagEvaluator("span"));
        assertFalse("b has a previous sibling but it is not a span", prevSibling.matches(doc, b));
    }

    @Test
    public void previousSiblingReturnsFalseWhenElementIsFirstChild() {
        Document doc = parse("<ul><li id='first'></li></ul>");
        Element first = doc.getElementById("first");
        StructuralEvaluator.PreviousSibling prevSibling = new StructuralEvaluator.PreviousSibling(ALWAYS_TRUE);
        assertFalse("First child has no previous sibling", prevSibling.matches(doc, first));
    }

    @Test
    public void previousSiblingToStringFormatting() {
        StructuralEvaluator.PreviousSibling prevSibling = new StructuralEvaluator.PreviousSibling(new TagEvaluator("div"));
        assertEquals(":prev*div", prevSibling.toString());
    }

    /* --------------------------------------------------------------------- *
     *  IMMEDIATE PREVIOUS SIBLING
     * --------------------------------------------------------------------- */

    @Test
    public void immediatePreviousSiblingMatchesDirectPreviousOnly() {
        Document doc = parse("<ul><li id='x'></li><li id='y'></li><li id='z'></li></ul>");
        Element y = doc.getElementById("y");
        StructuralEvaluator.ImmediatePreviousSibling immPrev = new StructuralEvaluator.ImmediatePreviousSibling(new TagEvaluator("li"));
        assertTrue("y has an immediate previous sibling (x) that is a li", immPrev.matches(doc, y));
    }

    @Test
    public void immediatePreviousSiblingDoesNotMatchIfDirectPreviousFails() {
        Document doc = parse("<ul><li id='x'></li><p id='y'></p></ul>");
        Element y = doc.getElementById("y");
        StructuralEvaluator.ImmediatePreviousSibling immPrev = new StructuralEvaluator.ImmediatePreviousSibling(new TagEvaluator("li"));
        assertFalse("Direct previous sibling is a p, not a li", immPrev.matches(doc, y));
    }

    @Test
    public void immediatePreviousSiblingReturnsFalseWhenNoPreviousSibling() {
        Document doc = parse("<ul><li id='only'></li></ul>");
        Element only = doc.getElementById("only");
        StructuralEvaluator.ImmediatePreviousSibling immPrev = new StructuralEvaluator.ImmediatePreviousSibling(ALWAYS_TRUE);
        assertFalse("Only element has no previous sibling", immPrev.matches(doc, only));
    }

    @Test
    public void immediatePreviousSiblingToStringFormatting() {
        StructuralEvaluator.ImmediatePreviousSibling immPrev = new StructuralEvaluator.ImmediatePreviousSibling(new TagEvaluator("a"));
        assertEquals(":prev:a", immPrev.toString());
    }

    /* --------------------------------------------------------------------- *
     *  ERROR SCENARIOS
     * --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void hasThrowsNPEWhenEvaluatorIsNull() {
        new StructuralEvaluator.Has(null).matches(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void notThrowsNPEWhenEvaluatorIsNull() {
        new StructuralEvaluator.Not(null).matches(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void parentThrowsNPEWhenEvaluatorIsNull() {
        new StructuralEvaluator.Parent(null).matches(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void immediateParentThrowsNPEWhenEvaluatorIsNull() {
        new StructuralEvaluator.ImmediateParent(null).matches(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void previousSiblingThrowsNPEWhenEvaluatorIsNull() {
        new StructuralEvaluator.PreviousSibling(null).matches(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void immediatePreviousSiblingThrowsNPEWhenEvaluatorIsNull() {
        new StructuralEvaluator.ImmediatePreviousSibling(null).matches(null, null);
    }
}
```
###Test END##