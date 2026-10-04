###Test START##
package org.jsoup.select;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.Test;

/**
 * Unit tests for {@link CombiningEvaluator} and its concrete subclasses {@link CombiningEvaluator.And}
 * and {@link CombiningEvaluator.Or}.
 *
 * The tests use simple stub {@link Evaluator} implementations that return deterministic
 * results so that the behaviour of the combining logic can be verified in isolation.
 */
public class CombiningEvaluatorTest {

    /** Simple evaluator that always returns {@code true}. */
    private static class TrueEval extends Evaluator {
        @Override
        public boolean matches(Element root, Element node) {
            return true;
        }

        @Override
        public String toString() {
            return "True";
        }
    }

    /** Simple evaluator that always returns {@code false}. */
    private static class FalseEval extends Evaluator {
        @Override
        public boolean matches(Element root, Element node) {
            return false;
        }

        @Override
        public String toString() {
            return "False";
        }
    }

    /** Evaluator that matches a node whose tag name equals the supplied tag. */
    private static class TagEval extends Evaluator {
        private final String tag;

        TagEval(String tag) {
            this.tag = tag;
        }

        @Override
        public boolean matches(Element root, Element node) {
            return node != null && tag.equalsIgnoreCase(node.tagName());
        }

        @Override
        public String toString() {
            return "Tag(" + tag + ")";
        }
    }

    /** Helper to create a dummy element used as both root and node in matcher calls. */
    private static Element dummyElement() {
        return Jsoup.parse("<div id='root'><p>text</p></div>").selectFirst("div");
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for the base class behaviour (rightMostEvaluator, replace, etc.) */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testRightMostEvaluator_Empty() {
        CombiningEvaluator.And and = new CombiningEvaluator.And();
        assertNull("rightMostEvaluator() should return null when no evaluators are present", and.rightMostEvaluator());
    }

    @Test
    public void testRightMostEvaluator_NonEmpty() {
        CombiningEvaluator.And and = new CombiningEvaluator.And(new TrueEval(), new FalseEval(), new TagEval("p"));
        assertNotNull(and.rightMostEvaluator());
        assertEquals("The right‑most evaluator should be the last one supplied (Tag(p))",
                new TagEval("p").toString(),
                and.rightMostEvaluator().toString());
    }

    @Test
    public void testReplaceRightMostEvaluator() {
        TrueEval trueEval = new TrueEval();
        FalseEval falseEval = new FalseEval();

        CombiningEvaluator.And and = new CombiningEvaluator.And(trueEval, falseEval);
        // initially matches should be false because the second evaluator is false
        assertFalse(and.matches(dummyElement(), dummyElement()));

        // replace the right‑most (false) evaluator with a true one
        and.replaceRightMostEvaluator(trueEval);
        assertTrue("After replacement the AND should evaluate to true", and.matches(dummyElement(), dummyElement()));
    }

    @Test
    public void testUpdateNumEvaluators_InternalConsistency() {
        CombiningEvaluator.And and = new CombiningEvaluator.And();
        assertEquals(0, and.num);
        and.evaluators.add(new TrueEval());
        and.updateNumEvaluators();
        assertEquals(1, and.num);
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for CombiningEvaluator.And */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAndMatches_AllTrue() {
        CombiningEvaluator.And and = new CombiningEvaluator.And(new TrueEval(), new TrueEval(), new TagEval("div"));
        assertTrue("AND of all true evaluators must return true", and.matches(dummyElement(), dummyElement()));
    }

    @Test
    public void testAndMatches_OneFalse() {
        CombiningEvaluator.And and = new CombiningEvaluator.And(new TrueEval(), new FalseEval(), new TagEval("div"));
        assertFalse("AND should short‑circuit to false when any evaluator is false", and.matches(dummyElement(), dummyElement()));
    }

    @Test
    public void testAndToString_JoinsWithSpace() {
        CombiningEvaluator.And and = new CombiningEvaluator.And(new TrueEval(), new TagEval("p"));
        String expected = "True Tag(p)";
        assertEquals("And.toString() should join evaluator strings with a space", expected, and.toString());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for CombiningEvaluator.Or constructors and internal handling */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testOrConstructor_Empty() {
        CombiningEvaluator.Or or = new CombiningEvaluator.Or();
        assertEquals("Empty OR should have zero internal evaluators", 0, or.num);
        assertFalse("Empty OR should never match", or.matches(dummyElement(), dummyElement()));
    }

    @Test
    public void testOrConstructor_SingleEvaluator() {
        TrueEval trueEval = new TrueEval();
        CombiningEvaluator.Or or = new CombiningEvaluator.Or(trueEval);
        assertEquals("OR with a single evaluator should contain exactly that evaluator", 1, or.num);
        assertSame("The stored evaluator should be the one passed in", trueEval, or.evaluators.get(0));
        assertTrue(or.matches(dummyElement(), dummyElement()));
    }

    @Test
    public void testOrConstructor_MultipleEvaluators_WrapsInAnd() {
        TrueEval trueEval = new TrueEval();
        TagEval tagEval = new TagEval("div");
        Collection<Evaluator> coll = Arrays.asList(trueEval, tagEval);
        CombiningEvaluator.Or or = new CombiningEvaluator.Or(coll);

        // Because we passed >1 evaluator, the constructor should have wrapped them in a single And.
        assertEquals("OR constructed with >1 evaluators should contain a single And", 1, or.num);
        assertTrue("The sole inner evaluator must be an instance of And", or.evaluators.get(0) instanceof CombiningEvaluator.And);

        // The And inside should evaluate to true only when *both* inner evaluators are true.
        assertTrue("OR should match when the wrapped AND matches (both true)", or.matches(dummyElement(), dummyElement()));

        // Replace the wrapped AND with one that always returns false and verify behaviour.
        or.evaluators.set(0, new FalseEval());
        or.updateNumEvaluators(); // sync internal counter
        assertFalse("After replacing the inner evaluator with false, OR must not match", or.matches(dummyElement(), dummyElement()));
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for Or.add, matches, and toString */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testOrAdd_EvaluatorsAccumulate() {
        CombiningEvaluator.Or or = new CombiningEvaluator.Or();
        assertEquals(0, or.num);

        or.add(new FalseEval());
        assertEquals(1, or.num);
        assertFalse(or.matches(dummyElement(), dummyElement()));

        or.add(new TrueEval());
        assertEquals(2, or.num);
        assertTrue("OR should be true if any added evaluator matches", or.matches(dummyElement(), dummyElement()));
    }

    @Test
    public void testOrMatches_ComplexScenario() {
        // Build an OR with three evaluators: false, false, true
        CombiningEvaluator.Or or = new CombiningEvaluator.Or();
        or.add(new FalseEval());
        or.add(new FalseEval());
        or.add(new TrueEval());

        assertTrue("OR should return true when at least one evaluator matches", or.matches(dummyElement(), dummyElement()));

        // Replace the true evaluator with a false one and verify the result flips.
        or.replaceRightMostEvaluator(new FalseEval()); // right‑most is the TrueEval we just added
        assertFalse("After replacement with false, OR should return false", or.matches(dummyElement(), dummyElement()));
    }

    @Test
    public void testOrToString_FormatsWithComma() {
        CombiningEvaluator.Or or = new CombiningEvaluator.Or();
        or.add(new TrueEval());
        or.add(new TagEval("p"));
        String expected = "True, Tag(p)";
        assertEquals("Or.toString() should join evaluator strings with a comma and space", expected, or.toString());
    }

    /* ---------------------------------------------------------------------- */
    /* Additional sanity checks for internal counters after mutations */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNumUpdatesAfterReplaceRightMost() {
        CombiningEvaluator.And and = new CombiningEvaluator.And(new TrueEval(), new TrueEval());
        assertEquals(2, and.num);
        and.replaceRightMostEvaluator(new FalseEval());
        // replace does not change size, so num stays the same
        assertEquals(2, and.num);
        assertFalse(and.matches(dummyElement(), dummyElement()));
    }
}
###Test END##