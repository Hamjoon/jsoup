package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link QueryParser}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic selectors (tag, id, class, universal)</li>
 *   <li>attribute selectors (existence, equality, prefix, suffix, contains, regex)</li>
 *   <li>pseudo‑selectors (:contains, :containsOwn, :matches, :matchesOwn, :not, :lt, :gt, :eq, :nth‑child, :first‑child, …)</li>
 *   <li>combinators (descendant, child, adjacent sibling, general sibling, group/or)</li>
 *   <li>edge cases – whitespace handling, empty parts, malformed queries</li>
 *   <li>error paths – malformed attribute, unknown combinator, empty sub‑query</li>
 * </ul>
 *
 * The test class uses the public {@link QueryParser#parse(String)} entry point.
 * The resulting {@link Evaluator} objects are inspected via {@code instanceof}
 * and, where possible, via their public getters (e.g. {@code Evaluator.Tag#getTagName()}).
 */
public class QueryParserTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /** Helper to assert that the evaluator is a Tag with the expected name. */
    private static void assertTag(Evaluator eval, String expectedTag) {
        assertTrue("Expected a Tag evaluator", eval instanceof Evaluator.Tag);
        assertEquals(expectedTag, ((Evaluator.Tag) eval).getTagName());
    }

    /** Helper to assert that the evaluator is an Id selector with the expected id. */
    private static void assertId(Evaluator eval, String expectedId) {
        assertTrue("Expected an Id evaluator", eval instanceof Evaluator.Id);
        assertEquals(expectedId, ((Evaluator.Id) eval).getId());
    }

    /** Helper to assert that the evaluator is a Class selector with the expected class name. */
    private static void assertClass(Evaluator eval, String expectedClass) {
        assertTrue("Expected a Class evaluator", eval instanceof Evaluator.Class);
        assertEquals(expectedClass, ((Evaluator.Class) eval).getClassName());
    }

    /** Helper to assert that the evaluator is an Attribute selector with the expected key. */
    private static void assertAttribute(Evaluator eval, String expectedKey) {
        assertTrue("Expected an Attribute evaluator", eval instanceof Evaluator.Attribute);
        assertEquals(expectedKey, ((Evaluator.Attribute) eval).getKey());
    }

    /** --------------------------------------------------------------------- **/
    /** BASIC SELECTORS                                                       **/
    /** --------------------------------------------------------------------- **/

    @Test
    public void parsesSimpleTag() {
        Evaluator eval = QueryParser.parse("div");
        assertTag(eval, "div");
    }

    @Test
    public void parsesIdSelector() {
        Evaluator eval = QueryParser.parse("#myId");
        assertId(eval, "myId");
    }

    @Test
    public void parsesClassSelector() {
        Evaluator eval = QueryParser.parse(".myClass");
        assertClass(eval, "myClass");
    }

    @Test
    public void parsesUniversalSelector() {
        Evaluator eval = QueryParser.parse("*");
        assertTrue("Expected AllElements evaluator", eval instanceof Evaluator.AllElements);
    }

    /** --------------------------------------------------------------------- **/
    /** ATTRIBUTE SELECTORS                                                   **/
    /** --------------------------------------------------------------------- **/

    @Test
    public void parsesAttributeExists() {
        Evaluator eval = QueryParser.parse("[href]");
        assertTrue("Expected an Attribute existence evaluator", eval instanceof Evaluator.Attribute);
        assertEquals("href", ((Evaluator.Attribute) eval).getKey());
    }

    @Test
    public void parsesAttributeEquals() {
        Evaluator eval = QueryParser.parse("[title=example]");
        assertTrue(eval instanceof Evaluator.AttributeWithValue);
        Evaluator.AttributeWithValue av = (Evaluator.AttributeWithValue) eval;
        assertEquals("title", av.getKey());
        assertEquals("example", av.getValue());
    }

    @Test
    public void parsesAttributeStartsWith() {
        Evaluator eval = QueryParser.parse("[data^=val]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueStarting);
        Evaluator.AttributeWithValueStarting avs = (Evaluator.AttributeWithValueStarting) eval;
        assertEquals("data", avs.getKey());
        assertEquals("val", avs.getValuePrefix());
    }

    @Test
    public void parsesAttributeEndsWith() {
        Evaluator eval = QueryParser.parse("[data$=end]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueEnding);
        Evaluator.AttributeWithValueEnding ave = (Evaluator.AttributeWithValueEnding) eval;
        assertEquals("data", ave.getKey());
        assertEquals("end", ave.getValueSuffix());
    }

    @Test
    public void parsesAttributeContains() {
        Evaluator eval = QueryParser.parse("[data*=mid]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueContaining);
        Evaluator.AttributeWithValueContaining avc = (Evaluator.AttributeWithValueContaining) eval;
        assertEquals("data", avc.getKey());
        assertEquals("mid", avc.getContained());
    }

    @Test
    public void parsesAttributeRegex() {
        Evaluator eval = QueryParser.parse("[data~=\\d{3}]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueMatching);
        Evaluator.AttributeWithValueMatching avm = (Evaluator.AttributeWithValueMatching) eval;
        assertEquals("data", avm.getKey());
        assertEquals("\\d{3}", avm.getPattern().pattern());
    }

    @Test
    public void attributeSelectorInvalidOperatorThrows() {
        thrown.expect(Selector.SelectorParseException.class);
        QueryParser.parse("[href??]");
    }

    /** --------------------------------------------------------------------- **/
    /** PSEUDO SELECTORS                                                      **/
    /** --------------------------------------------------------------------- **/

    @Test
    public void parsesContains() {
        Evaluator eval = QueryParser.parse(":contains(hello world)");
        assertTrue(eval instanceof Evaluator.ContainsText);
        assertEquals("hello world", ((Evaluator.ContainsText) eval).getSearchText());
    }

    @Test
    public void parsesContainsOwn() {
        Evaluator eval = QueryParser.parse(":containsOwn(foo)");
        assertTrue(eval instanceof Evaluator.ContainsOwnText);
        assertEquals("foo", ((Evaluator.ContainsOwnText) eval).getSearchText());
    }

    @Test
    public void parsesMatches() {
        Evaluator eval = QueryParser.parse(":matches(^\\d+$)");
        assertTrue(eval instanceof Evaluator.Matches);
        Pattern p = ((Evaluator.Matches) eval).getPattern();
        assertEquals("^\\d+$", p.pattern());
    }

    @Test
    public void parsesMatchesOwn() {
        Evaluator eval = QueryParser.parse(":matchesOwn([A-Z]+)");
        assertTrue(eval instanceof Evaluator.MatchesOwn);
        Pattern p = ((Evaluator.MatchesOwn) eval).getPattern();
        assertEquals("[A-Z]+", p.pattern());
    }

    @Test
    public void parsesNotPseudo() {
        Evaluator eval = QueryParser.parse("div:not(.ignore)");
        assertTrue(eval instanceof StructuralEvaluator.Not);
        StructuralEvaluator.Not not = (StructuralEvaluator.Not) eval;
        // The inner evaluator should be a Class selector for "ignore"
        assertClass(not.getEvaluator(), "ignore");
    }

    @Test
    public void parsesIndexLessThan() {
        Evaluator eval = QueryParser.parse(":lt(5)");
        assertTrue(eval instanceof Evaluator.IndexLessThan);
        assertEquals(5, ((Evaluator.IndexLessThan) eval).getIndex());
    }

    @Test
    public void parsesIndexGreaterThan() {
        Evaluator eval = QueryParser.parse(":gt(2)");
        assertTrue(eval instanceof Evaluator.IndexGreaterThan);
        assertEquals(2, ((Evaluator.IndexGreaterThan) eval).getIndex());
    }

    @Test
    public void parsesIndexEquals() {
        Evaluator eval = QueryParser.parse(":eq(3)");
        assertTrue(eval instanceof Evaluator.IndexEquals);
        assertEquals(3, ((Evaluator.IndexEquals) eval).getIndex());
    }

    @Test
    public void parsesNthChildOdd() {
        Evaluator eval = QueryParser.parse("li:nth-child(odd)");
        assertTrue(eval instanceof Evaluator.IsNthChild);
        Evaluator.IsNthChild nth = (Evaluator.IsNthChild) eval;
        assertEquals(2, nth.getA());
        assertEquals(1, nth.getB());
    }

    @Test
    public void parsesNthChildExpression() {
        Evaluator eval = QueryParser.parse("li:nth-child(2n+3)");
        assertTrue(eval instanceof Evaluator.IsNthChild);
        Evaluator.IsNthChild nth = (Evaluator.IsNthChild) eval;
        assertEquals(2, nth.getA());
        assertEquals(3, nth.getB());
    }

    @Test
    public void parsesNthLastOfTypeEven() {
        Evaluator eval = QueryParser.parse("li:nth-last-of-type(even)");
        assertTrue(eval instanceof Evaluator.IsNthLastOfType);
        Evaluator.IsNthLastOfType nth = (Evaluator.IsNthLastOfType) eval;
        assertEquals(2, nth.getA());
        assertEquals(0, nth.getB());
    }

    @Test
    public void parsesFirstChild() {
        Evaluator eval = QueryParser.parse(":first-child");
        assertTrue(eval instanceof Evaluator.IsFirstChild);
    }

    @Test
    public void parsesLastOfType() {
        Evaluator eval = QueryParser.parse(":last-of-type");
        assertTrue(eval instanceof Evaluator.IsLastOfType);
    }

    /** --------------------------------------------------------------------- **/
    /** COMBINATORS                                                            **/
    /** --------------------------------------------------------------------- **/

    @Test
    public void parsesDescendantCombinator() {
        Evaluator eval = QueryParser.parse("ul li");
        assertTrue(eval instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        // The right‑most evaluator should be a Parent structural evaluator wrapping the “li” tag.
        Evaluator right = and.rightMostEvaluator();
        assertTrue(right instanceof StructuralEvaluator.Parent);
        StructuralEvaluator.Parent parent = (StructuralEvaluator.Parent) right;
        assertTrue(parent.getEvaluator() instanceof Evaluator.Tag);
        assertTag(parent.getEvaluator(), "li");
    }

    @Test
    public void parsesChildCombinator() {
        Evaluator eval = QueryParser.parse("div > p");
        assertTrue(eval instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        Evaluator right = and.rightMostEvaluator();
        assertTrue(right instanceof StructuralEvaluator.ImmediateParent);
        StructuralEvaluator.ImmediateParent ip = (StructuralEvaluator.ImmediateParent) right;
        assertTag(ip.getEvaluator(), "p");
    }

    @Test
    public void parsesAdjacentSiblingCombinator() {
        Evaluator eval = QueryParser.parse("h1 + p");
        assertTrue(eval instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        Evaluator right = and.rightMostEvaluator();
        assertTrue(right instanceof StructuralEvaluator.ImmediatePreviousSibling);
        StructuralEvaluator.ImmediatePreviousSibling ips = (StructuralEvaluator.ImmediatePreviousSibling) right;
        assertTag(ips.getEvaluator(), "p");
    }

    @Test
    public void parsesGeneralSiblingCombinator() {
        Evaluator eval = QueryParser.parse("h1 ~ p");
        assertTrue(eval instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        Evaluator right = and.rightMostEvaluator();
        assertTrue(right instanceof StructuralEvaluator.PreviousSibling);
        StructuralEvaluator.PreviousSibling ps = (StructuralEvaluator.PreviousSibling) right;
        assertTag(ps.getEvaluator(), "p");
    }

    @Test
    public void parsesGroupOrCombinator() {
        Evaluator eval = QueryParser.parse("a, b, .c");
        assertTrue(eval instanceof CombiningEvaluator.Or);
        CombiningEvaluator.Or or = (CombiningEvaluator.Or) eval;
        assertEquals(3, or.evaluators().size());
        assertTrue(or.evaluators().get(0) instanceof Evaluator.Tag);
        assertTrue(or.evaluators().get(1) instanceof Evaluator.Tag);
        assertTrue(or.evaluators().get(2) instanceof Evaluator.Class);
    }

    @Test
    public void leadingCombinatorIsTreatedAsRoot() {
        // Query starts with '>' – parser should inject a Root evaluator first.
        Evaluator eval = QueryParser.parse("> p");
        assertTrue(eval instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        Evaluator left = and.leftMostEvaluator(); // root part
        assertTrue(left instanceof StructuralEvaluator.Root);
    }

    /** --------------------------------------------------------------------- **/
    /** EDGE CASES & ERROR HANDLING                                            **/
    /** --------------------------------------------------------------------- **/

    @Test
    public void emptyQueryThrows() {
        thrown.expect(Selector.SelectorParseException.class);
        QueryParser.parse("");
    }

    @Test
    public void malformedSelectorThrows() {
        thrown.expect(Selector.SelectorParseException.class);
        QueryParser.parse("div..class");
    }

    @Test
    public void notSelectorWithEmptySubQueryThrows() {
        thrown.expect(Selector.SelectorParseException.class);
        QueryParser.parse("div:not()");
    }

    @Test
    public void hasSelectorWithEmptySubQueryThrows() {
        thrown.expect(Selector.SelectorParseException.class);
        QueryParser.parse("div:has()");
    }

    @Test
    public void containsSelectorWithEmptyTextThrows() {
        thrown.expect(Selector.SelectorParseException.class);
        QueryParser.parse(":contains()");
    }

    @Test
    public void matchesSelectorWithEmptyRegexThrows() {
        thrown.expect(Selector.SelectorParseException.class);
        QueryParser.parse(":matches()");
    }

    @Test
    public void unknownCombinatorThrows() {
        // The parser only knows ",>+~ " – injecting a bogus char forces the error path.
        thrown.expect(Selector.SelectorParseException.class);
        QueryParser.parse("div ? p");
    }

    /** --------------------------------------------------------------------- **/
    /** INTERNAL BEHAVIOUR (consumeSubQuery) – verified via public API          **/
    /** --------------------------------------------------------------------- **/

    @Test
    public void consumeSubQueryHandlesBalancedParentheses() {
        // The sub‑query contains a nested selector with its own parentheses.
        Evaluator eval = QueryParser.parse("div:has(span[title='[test]']) > a");
        // Ensure parsing succeeded and the top evaluator is an AND combining the Has‑evaluator
        // with a child combinator.
        assertTrue(eval instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        Evaluator right = and.rightMostEvaluator();
        assertTrue(right instanceof StructuralEvaluator.ImmediateParent);
        StructuralEvaluator.ImmediateParent ip = (StructuralEvaluator.ImmediateParent) right;
        assertTag(ip.getEvaluator(), "a");
    }

    /** --------------------------------------------------------------------- **/
    /** Real‑world integration – applying the parsed evaluator to a document   **/
    /** --------------------------------------------------------------------- **/

    @Test
    public void parsedEvaluatorSelectsCorrectElements() {
        String html = "<html><body>"
                + "<div id='one' class='alpha'>A</div>"
                + "<div id='two' class='beta'>B</div>"
                + "<p class='alpha'>C</p>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);
        Evaluator eval = QueryParser.parse("div.alpha, p.alpha");
        assertTrue(eval instanceof CombiningEvaluator.Or);
        // Verify that the evaluator picks exactly the two elements with class 'alpha'
        assertEquals(2, doc.select(eval).size());
    }
}
