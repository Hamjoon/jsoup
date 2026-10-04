###Test START##
```java
package org.jsoup.select;

import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link CombiningEvaluator} and its concrete subclasses {@link CombiningEvaluator.And}
 * and {@link CombiningEvaluator.Or}.
 *
 * The test class lives in the same package as the classes under test so that it can
 * directly access the package‑private fields (e.g., {@code evaluators}) and methods.
 */
public class CombiningEvaluatorTest {

    /** Simple stub evaluator that always returns a predefined boolean result. */
    private static class StubEvaluator extends Evaluator {
        private final boolean result;
        private final String name;

        StubEvaluator(boolean result, String name) {
            this.result = result;
            this.name = name;
        }

        @Override
        public boolean matches(Element root, Element node) {
            return result;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private Element dummyRoot;
    private Element dummyNode;

    @Before
    public void setUp() {
        // jsoup Element can be instantiated directly with a tag name.
        dummyRoot = new Element("html");
        dummyNode = new Element("div");
        dummyRoot.appendChild(dummyNode);
    }

    /* -------------------------------------------------
     *  Tests for the protected helper methods of CombiningEvaluator
     * ------------------------------------------------- */

    @Test
    public void testRightMostEvaluator_Empty() {
        CombiningEvaluator.And and = new CombiningEvaluator.And();
        assertNull("rightMostEvaluator should be null when no evaluators are present", and.rightMostEvaluator());
    }

    @Test
    public void testRightMostEvaluator_NonEmpty() {
        StubEvaluator a = new StubEvaluator(true, "A");
        StubEvaluator b = new StubEvaluator(false, "B");
        CombiningEvaluator.And and = new CombiningEvaluator.And(a, b);
        assertSame("rightMostEvaluator should return the last evaluator added", b, and.rightMostEvaluator());
    }

    @Test
    public void testReplaceRightMostEvaluator() {
        StubEvaluator first = new StubEvaluator(true, "first");
        StubEvaluator second = new StubEvaluator(false, "second");
        StubEvaluator replacement = new StubEvaluator(true, "replacement");

        CombiningEvaluator.And and = new CombiningEvaluator.And(first, second);
        // initially matches should be false because second returns false
        assertFalse(and.matches(dummyRoot, dummyNode));

        // replace the right‑most (second) evaluator with one that returns true
        and.replaceRightMostEvaluator(replacement);
        assertTrue(and.matches(dummyRoot, dummyNode));
    }

    @Test
    public void testUpdateNumEvaluators_ReflectedInBehavior() {
        StubEvaluator a = new StubEvaluator(true, "A");
        StubEvaluator b = new StubEvaluator(true, "B");
        // Build an Or with a single evaluator – internal num should be 1
        CombiningEvaluator.Or or = new CombiningEvaluator.Or(a);
        assertEquals(1, or.num);
        // Add another evaluator – num should become 2
        or.add(b);
        assertEquals(2, or.num);
        // Both evaluators return true, so matches must be true
        assertTrue(or.matches(dummyRoot, dummyNode));
    }

    /* -------------------------------------------------
     *  Tests for the And subclass
     * ------------------------------------------------- */

    @Test
    public void testAndMatches_AllTrue() {
        StubEvaluator a = new StubEvaluator(true, "A");
        StubEvaluator b = new StubEvaluator(true, "B");
        CombiningEvaluator.And and = new CombiningEvaluator.And(a, b);
        assertTrue("And should match when all child evaluators return true", and.matches(dummyRoot, dummyNode));
    }

    @Test
    public void testAndMatches_OneFalse() {
        StubEvaluator a = new StubEvaluator(true, "A");
        StubEvaluator b = new StubEvaluator(false, "B");
        CombiningEvaluator.And and = new CombiningEvaluator.And(a, b);
        assertFalse("And should not match when any child evaluator returns false", and.matches(dummyRoot, dummyNode));
    }

    @Test
    public void testAndToString_JoinWithSpace() {
        StubEvaluator a = new StubEvaluator(true, "first");
        StubEvaluator b = new StubEvaluator(true, "second");
        StubEvaluator c = new StubEvaluator(true, "third");
        CombiningEvaluator.And and = new CombiningEvaluator.And(a, b, c);
        String expected = "first second third";
        assertEquals("And.toString should join evaluator strings with a space", expected, and.toString());
    }

    /* -------------------------------------------------
     *  Tests for the Or subclass
     * ------------------------------------------------- */

    @Test
    public void testOrConstructor_WithMultipleEvaluators_CombinesIntoAnd() {
        StubEvaluator a = new StubEvaluator(true, "A");
        StubEvaluator b = new StubEvaluator(true, "B");
        List<Evaluator> list = Arrays.asList(a, b);
        CombiningEvaluator.Or or = new CombiningEvaluator.Or(list);

        // When more than one evaluator is supplied, Or should wrap them in a single And evaluator.
        assertEquals("Or should contain exactly one internal evaluator (the And wrapper)", 1, or.evaluators.size());
        assertTrue("Internal evaluator should be an instance of And", or.evaluators.get(0) instanceof CombiningEvaluator.And);

        // The And wrapper must evaluate both child evaluators, therefore the overall result is true.
        assertTrue("Or should match when the wrapped And matches", or.matches(dummyRoot, dummyNode));
    }

    @Test
    public void testOrConstructor_WithSingleEvaluator_NoAndWrap() {
        StubEvaluator a = new StubEvaluator(true, "A");
        CombiningEvaluator.Or or = new CombiningEvaluator.Or(a);

        assertEquals("Or with a single evaluator should store it directly", 1, or.evaluators.size());
        assertSame("Stored evaluator should be the one passed in", a, or.evaluators.get(0));
    }

    @Test
    public void testOrAdd_EvaluatorIsConsideredInMatches() {
        StubEvaluator a = new StubEvaluator(false, "A");
        StubEvaluator b = new StubEvaluator(true, "B");
        CombiningEvaluator.Or or = new CombiningEvaluator.Or();
        or.add(a);
        or.add(b);

        // OR should be true because the second evaluator returns true
        assertTrue("Or should match when any added evaluator returns true", or.matches(dummyRoot, dummyNode));
    }

    @Test
    public void testOrMatches_LogicalOrBehavior() {
        StubEvaluator falseEval = new StubEvaluator(false, "F");
        StubEvaluator trueEval = new StubEvaluator(true, "T");
        CombiningEvaluator.Or or = new CombiningEvaluator.Or(falseEval, trueEval);
        // Since at least one evaluator (the second) returns true, the OR should succeed.
        assertTrue(or.matches(dummyRoot, dummyNode));

        // Flip results: both false -> overall false
        StubEvaluator falseEval2 = new StubEvaluator(false, "F2");
        CombiningEvaluator.Or orAllFalse = new CombiningEvaluator.Or(falseEval, falseEval2);
        assertFalse(orAllFalse.matches(dummyRoot, dummyNode));
    }

    @Test
    public void testOrToString_CommaSeparated() {
        StubEvaluator a = new StubEvaluator(true, "first");
        StubEvaluator b = new StubEvaluator(true, "second");
        StubEvaluator c = new StubEvaluator(true, "third");
        CombiningEvaluator.Or or = new CombiningEvaluator.Or(a, b, c);
        String expected = "first, second, third";
        assertEquals("Or.toString should join evaluator strings with a comma and space", expected, or.toString());
    }

    /* -------------------------------------------------
     *  Additional sanity checks for internal state after manipulations
     * ------------------------------------------------- */

    @Test
    public void testOrInternalStateAfterAdd() {
        StubEvaluator a = new StubEvaluator(true, "A");
        CombiningEvaluator.Or or = new CombiningEvaluator.Or();
        assertEquals("Initially Or should have zero evaluators", 0, or.evaluators.size());

        or.add(a);
        assertEquals("After adding one evaluator, size should be 1", 1, or.evaluators.size());
        assertSame("The stored evaluator must be the one added", a, or.evaluators.get(0));
    }

    @Test
    public void testAndInternalStateAfterConstruction() {
        StubEvaluator a = new StubEvaluator(true, "A");
        StubEvaluator b = new StubEvaluator(true, "B");
        CombiningEvaluator.And and = new CombiningEvaluator.And(a, b);
        assertEquals("And should store both evaluators", 2, and.evaluators.size());
        assertTrue(and.evaluators.containsAll(Arrays.asList(a, b)));
    }
}
```
###Test END##