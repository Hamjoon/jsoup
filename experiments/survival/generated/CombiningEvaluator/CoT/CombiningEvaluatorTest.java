package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CombiningEvaluator} and its concrete subclasses
 * {@link CombiningEvaluator.And} and {@link CombiningEvaluator.Or}.
 *
 * The tests are written in the same package (org.jsoup.select) so that they can
 * access the package‑private members (e.g. {@code rightMostEvaluator()},
 * {@code replaceRightMostEvaluator(...)} and {@code updateNumEvaluators()}).
 */
public class CombiningEvaluatorTest {

    /* --------------------------------------------------------------------- *
     *  Helper evaluator implementations used throughout the test suite.   *
     * --------------------------------------------------------------------- */

    /** Always returns {@code true}. */
    private static class AlwaysTrue implements Evaluator {
        @Override public boolean matches(Element root, Element node) { return true; }
        @Override public String toString() { return "true"; }
    }

    /** Always returns {@code false}. */
    private static class AlwaysFalse implements Evaluator {
        @Override public boolean matches(Element root, Element node) { return false; }
        @Override public String toString() { return "false"; }
    }

    /** Matches when the element’s tag name equals the supplied name. */
    private static class TagNameEvaluator implements Evaluator {
        private final String tagName;
        TagNameEvaluator(String tagName) { this.tagName = tagName; }
        @Override public boolean matches(Element root, Element node) {
            return node != null && tagName.equals(node.tagName());
        }
        @Override public String toString() { return "tag=" + tagName; }
    }

    /** Small utility to obtain an {@link Element} from an HTML fragment. */
    private static Element elementFromHtml(String html) {
        // Jsoup parses the fragment and returns the first element.
        return Jsoup.parseBodyFragment(html).body().child(0);
    }

    /* --------------------------------------------------------------------- *
     *  Tests for the public API of the AND evaluator.                       *
     * --------------------------------------------------------------------- */

    @Test
    public void andMatchesWithNoEvaluatorsShouldReturnTrue() {
        CombiningEvaluator.And and = new CombiningEvaluator.And(Collections.emptyList());
        Element root = elementFromHtml("<div></div>");
        Element node = elementFromHtml("<p></p>");
        assertTrue("AND with no sub‑evaluators must match any node", and.matches(root, node));
    }

    @Test
    public void andMatchesWithSingleTrueEvaluator() {
        CombiningEvaluator.And and = new CombiningEvaluator.And(new AlwaysTrue());
        Element root = elementFromHtml("<div></div>");
        Element node = elementFromHtml("<span></span>");
        assertTrue(and.matches(root, node));
    }

    @Test
    public void andMatchesWithSingleFalseEvaluator() {
        CombiningEvaluator.And and = new CombiningEvaluator.And(new AlwaysFalse());
        Element root = elementFromHtml("<div></div>");
        Element node = elementFromHtml("<span></span>");
        assertFalse(and.matches(root, node));
    }

    @Test
    public void andMatchesWithMultipleEvaluators() {
        CombiningEvaluator.And and = new CombiningEvaluator.And(
                new TagNameEvaluator("div"),
                new TagNameEvaluator("p"));
        Element root = elementFromHtml("<html></html>");
        Element div = elementFromHtml("<div></div>");
        Element p   = elementFromHtml("<p></p>");

        // Both must match → false because the node cannot be both a div and a p.
        assertFalse(and.matches(root, div));
        assertFalse(and.matches(root, p));

        // Using a custom evaluator that always returns true makes the whole AND true.
        CombiningEvaluator.And alwaysTrueAnd = new CombiningEvaluator.And(
                new AlwaysTrue(),
                new AlwaysTrue());
        assertTrue(alwaysTrueAnd.matches(root, div));
    }

    @Test
    public void andReplaceRightMostEvaluatorChangesBehaviour() {
        // Build an AND with two evaluators: true + false → overall false.
        CombiningEvaluator.And and = new CombiningEvaluator.And(
                new AlwaysTrue(),
                new AlwaysFalse());

        Element root = elementFromHtml("<html></html>");
        Element node = elementFromHtml("<div></div>");

        assertFalse("initial AND should be false (true && false)", and.matches(root, node));

        // Replace the right‑most (false) evaluator with a true evaluator.
        and.replaceRightMostEvaluator(new AlwaysTrue());

        // Verify internal state via the protected helper.
        Evaluator rightMost = and.rightMostEvaluator();
        assertTrue("right‑most evaluator after replacement must be an AlwaysTrue instance",
                rightMost instanceof AlwaysTrue);

        // Now the AND should evaluate to true.
        assertTrue("AND should become true after replacement (true && true)", and.matches(root, node));
    }

    @Test
    public void andToStringJoinsWithSpace() {
        CombiningEvaluator.And and = new CombiningEvaluator.And(
                new TagNameEvaluator("div"),
                new TagNameEvaluator("p"));
        String expected = "tag=div tag=p";
        assertEquals(expected, and.toString());
    }

    /* --------------------------------------------------------------------- *
     *  Tests for the public API of the OR evaluator.                        *
     * --------------------------------------------------------------------- */

    @Test
    public void orMatchesWithNoEvaluatorsShouldReturnFalse() {
        CombiningEvaluator.Or or = new CombiningEvaluator.Or();
        Element root = elementFromHtml("<div></div>");
        Element node = elementFromHtml("<span></span>");
        assertFalse("OR with no sub‑evaluators must not match any node", or.matches(root, node));
    }

    @Test
    public void orMatchesWithSingleTrueEvaluator() {
        CombiningEvaluator.Or or = new CombiningEvaluator.Or(new AlwaysTrue());
        Element root = elementFromHtml("<div></div>");
        Element node = elementFromHtml("<span></span>");
        assertTrue(or.matches(root, node));
    }

    @Test
    public void orMatchesWithSingleFalseEvaluator() {
        CombiningEvaluator.Or or = new CombiningEvaluator.Or(new AlwaysFalse());
        Element root = elementFromHtml("<div></div>");
        Element node = elementFromHtml("<span></span>");
        assertFalse(or.matches(root, node));
    }

    @Test
    public void orMatchesWithMultipleEvaluatorsStopsAtFirstMatch() {
        CombiningEvaluator.Or or = new CombiningEvaluator.Or(
                new AlwaysFalse(),
                new AlwaysTrue(),
                new AlwaysFalse()); // third should never be evaluated
        Element root = elementFromHtml("<div></div>");
        Element node = elementFromHtml("<span></span>");

        assertTrue("OR should return true as soon as a sub‑evaluator matches", or.matches(root, node));
    }

    @Test
    public void orConstructorWithCollectionGreaterThanOneWrapsInAnd() {
        // Two evaluators passed to the collection‑constructor → they are wrapped in an AND.
        CombiningEvaluator.Or or = new CombiningEvaluator.Or(
                Arrays.asList(new TagNameEvaluator("div"), new TagNameEvaluator("p")));

        Element root = elementFromHtml("<html></html>");
        Element div = elementFromHtml("<div></div>");
        Element p   = elementFromHtml("<p></p>");

        // The internal first clause is an AND (div && p) → never true.
        assertFalse(or.matches(root, div));
        assertFalse(or.matches(root, p));

        // Adding a true evaluator creates an additional OR clause.
        or.add(new AlwaysTrue());
        assertTrue(or.matches(root, div));
    }

    @Test
    public void orAddIncrementsNumEvaluatorsAndAffectsMatching() {
        CombiningEvaluator.Or or = new CombiningEvaluator.Or();
        assertFalse(or.matches(null, null)); // empty → false

        // Add a false evaluator → still false.
        or.add(new AlwaysFalse());
        assertFalse(or.matches(null, null));

        // Add a true evaluator → now true.
        or.add(new AlwaysTrue());
        assertTrue(or.matches(null, null));
    }

    @Test
    public void orToStringJoinsWithCommaAndSpace() {
        CombiningEvaluator.Or or = new CombiningEvaluator.Or(
                new TagNameEvaluator("div"),
                new TagNameEvaluator("p"));
        String expected = "tag=div, tag=p";
        assertEquals(expected, or.toString());
    }

    @Test(expected = NullPointerException.class)
    public void orAddNullEvaluatorShouldThrowWhenMatching() {
        CombiningEvaluator.Or or = new CombiningEvaluator.Or();
        or.add(null); // null is accepted by the list, but a NPE will be thrown on match
        Element root = elementFromHtml("<div></div>");
        Element node = elementFromHtml("<span></span>");
        // The following call should trigger NPE because the stored evaluator is null.
        or.matches(root, node);
    }

    @Test(expected = NullPointerException.class)
    public void andConstructorWithNullCollectionShouldThrow() {
        // Passing a null collection to the AND constructor will cause a NPE when
        // the constructor tries to addAll(null).
        new CombiningEvaluator.And((java.util.Collection<Evaluator>) null);
    }
}
