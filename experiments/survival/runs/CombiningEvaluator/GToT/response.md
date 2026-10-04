###Test START##
```java
package org.jsoup.select;

import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test suite for {@link CombiningEvaluator} and its concrete subclasses {@link CombiningEvaluator.And}
 * and {@link CombiningEvaluator.Or}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public (package‑private) API of the base class: {@code rightMostEvaluator()},
 *       {@code replaceRightMostEvaluator(Evaluator)} and {@code updateNumEvaluators()}.</li>
 *   <li>Behaviour of {@code And.matches(...)} and {@code Or.matches(...)} with various
 *       numbers of inner evaluators (0, 1, many).</li>
 *   <li>String representation via {@code toString()} for both combinators.</li>
 *   <li>Edge cases such as empty evaluator collections, replacement of the right‑most evaluator,
 *       and illegal operations that should throw {@link IndexOutOfBoundsException}.</li>
 * </ul>
 *
 * The test class resides in the same package as {@code CombiningEvaluator} so that it can
 * access the package‑private members.
 */
public class CombiningEvaluatorTest {

    /* ---------------------------------------------------------------------- */
    /* Helper stub evaluators                                                */
    /* ---------------------------------------------------------------------- */

    /** Always returns {@code true}. */
    private static class TrueEvaluator extends Evaluator {
        @Override public boolean matches(Element root, Element node) { return true; }
        @Override public String toString() { return "true"; }
    }

    /** Always returns {@code false}. */
    private static class FalseEvaluator extends Evaluator {
        @Override public boolean matches(Element root, Element node) { return false; }
        @Override public String toString() { return "false"; }
    }

    /** Returns the value supplied at construction time. */
    private static class FixedEvaluator extends Evaluator {
        private final boolean result;
        private final String name;
        FixedEvaluator(boolean result, String name) {
            this.result = result;
            this.name = name;
        }
        @Override public boolean matches(Element root, Element node) { return result; }
        @Override public String toString() { return name; }
    }

    /** Simple {@link Element} used for all match calls – the concrete element does not matter. */
    private static Element dummyElement() {
        return new Element(Tag.valueOf("div"), "");
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for the base class (package‑private methods)                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testRightMostEvaluator_Empty() {
        CombiningEvaluator.And empty = new CombiningEvaluator.And(); // no args -> empty list
        assertNull("Right‑most evaluator should be null when none are present",
                empty.rightMostEvaluator());
    }

    @Test
    public void testRightMostEvaluator_NonEmpty() {
        Evaluator a = new TrueEvaluator();
        Evaluator b = new FalseEvaluator();
        CombiningEvaluator.And combo = new CombiningEvaluator.And(a, b);
        assertSame("Right‑most evaluator should be the last added evaluator",
                b, combo.rightMostEvaluator());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReplaceRightMostEvaluator_EmptyThrows() {
        CombiningEvaluator.And empty = new CombiningEvaluator.And(); // empty list
        empty.replaceRightMostEvaluator(new TrueEvaluator()); // should throw
    }

    @Test
    public void testReplaceRightMostEvaluator_Succeeds() {
        Evaluator first = new TrueEvaluator();
        Evaluator second = new FalseEvaluator();
        CombiningEvaluator.And combo = new CombiningEvaluator.And(first, second);
        // replace second with a true evaluator
        combo.replaceRightMostEvaluator(new TrueEvaluator());
        assertTrue("After replacement, matches should succeed (both true)",
                combo.matches(dummyElement(), dummyElement()));
    }

    @Test
    public void testUpdateNumEvaluators_ReflectsSize() {
        // Directly use the protected method via a subclass
        CombiningEvaluator.Or or = new CombiningEvaluator.Or();
        // initially empty
        assertEquals(0, or.num);
        or.add(new TrueEvaluator());
        assertEquals(1, or.num);
        or.add(new FalseEvaluator());
        assertEquals(2, or.num);
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for And evaluator                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAndMatches_AllTrue() {
        CombiningEvaluator.And and = new CombiningEvaluator.And(
                new TrueEvaluator(),
                new TrueEvaluator(),
                new TrueEvaluator()
        );
        assertTrue("And should return true when all inner evaluators are true",
                and.matches(dummyElement(), dummyElement()));
    }

    @Test
    public void testAndMatches_OneFalse() {
        CombiningEvaluator.And and = new CombiningEvaluator.And(
                new TrueEvaluator(),
                new FalseEvaluator(),
                new TrueEvaluator()
        );
        assertFalse("And should return false if any inner evaluator is false",
                and.matches(dummyElement(), dummyElement()));
    }

    @Test
    public void testAndMatches_EmptyList() {
        // An And with no evaluators should vacuously be true (no condition to fail)
        CombiningEvaluator.And and = new CombiningEvaluator.And();
        assertTrue("And with no evaluators should match any element", 
                and.matches(dummyElement(), dummyElement()));
    }

    @Test
    public void testAndToString_JoinsWithSpace() {
        Evaluator a = new FixedEvaluator(true, "A");
        Evaluator b = new FixedEvaluator(false, "B");
        CombiningEvaluator.And and = new CombiningEvaluator.And(a, b);
        assertEquals("String representation should join with a single space",
                "A B", and.toString());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for Or evaluator                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testOrConstructor_WrapsMultipleIntoAnd() {
        // >1 evaluators: constructor should wrap them into a single And clause
        Evaluator a = new FixedEvaluator(true, "A");
        Evaluator b = new FixedEvaluator(false, "B");
        CombiningEvaluator.Or or = new CombiningEvaluator.Or(a, b);
        // The internal list should contain exactly one evaluator (the And)
        assertEquals("Or with >1 initial evaluators should contain a single inner And",
                1, or.evaluators.size());
        // The inner evaluator must be an instance of And
        assertTrue(or.evaluators.get(0) instanceof CombiningEvaluator.And);
        // Matching should be false because the And (A && B) is false
        assertFalse(or.matches(dummyElement(), dummyElement()));
    }

    @Test
    public void testOrConstructor_SingleEvaluator() {
        Evaluator a = new FixedEvaluator(true, "A");
        CombiningEvaluator.Or or = new CombiningEvaluator.Or(a);
        assertEquals("Or with a single evaluator should store it directly",
                1, or.evaluators.size());
        assertSame(a, or.evaluators.get(0));
        assertTrue(or.matches(dummyElement(), dummyElement()));
    }

    @Test
    public void testOrConstructor_Empty() {
        CombiningEvaluator.Or or = new CombiningEvaluator.Or();
        assertEquals("Empty Or should have zero inner evaluators", 0, or.evaluators.size());
        assertFalse("Or with no clauses should never match", 
                or.matches(dummyElement(), dummyElement()));
    }

    @Test
    public void testOrAdd_IncreasesNumAndMatches() {
        CombiningEvaluator.Or or = new CombiningEvaluator.Or();
        assertEquals(0, or.num);
        or.add(new FalseEvaluator());
        assertEquals(1, or.num);
        assertFalse(or.matches(dummyElement(), dummyElement()));
        or.add(new TrueEvaluator());
        assertEquals(2, or.num);
        assertTrue("Or should match if any inner evaluator returns true",
                or.matches(dummyElement(), dummyElement()));
    }

    @Test
    public void testOrMatches_EarlyReturn() {
        // Verify that evaluation stops after the first true evaluator
        Evaluator trueEval = new FixedEvaluator(true, "T");
        Evaluator falseEval = new FixedEvaluator(false, "F");
        CountingEvaluator countingFalse = new CountingEvaluator(falseEval);
        CombiningEvaluator.Or or = new CombiningEvaluator.Or(trueEval, countingFalse);
        assertTrue(or.matches(dummyElement(), dummyElement()));
        assertEquals("Second evaluator should not be invoked after first true",
                0, countingFalse.invocations);
    }

    @Test
    public void testOrToString_JoinsWithCommaSpace() {
        Evaluator a = new FixedEvaluator(true, "A");
        Evaluator b = new FixedEvaluator(false, "B");
        CombiningEvaluator.Or or = new CombiningEvaluator.Or();
        or.add(a);
        or.add(b);
        assertEquals("String representation should join with \", \"",
                "A, B", or.toString());
    }

    /* ---------------------------------------------------------------------- */
    /* Helper class to verify call counts (used for early‑return test)        */
    /* ---------------------------------------------------------------------- */
    private static class CountingEvaluator extends Evaluator {
        private final Evaluator delegate;
        int invocations = 0;
        CountingEvaluator(Evaluator delegate) { this.delegate = delegate; }
        @Override public boolean matches(Element root, Element node) {
            invocations++;
            return delegate.matches(root, node);
        }
        @Override public String toString() { return delegate.toString(); }
    }
}
```
###Test END##