###Test START##
```java
package org.jsoup.select;

import org.jsoup.helper.Validate;
import org.jsoup.parser.TokenQueue;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 tests for {@link QueryParser}.
 *
 * The tests are grouped as follows:
 * <ul>
 *   <li>Basic public API extraction</li>
 *   <li>Normal functionality – each selector branch is exercised and the returned {@link Evaluator}
 *       type is verified.</li>
 *   <li>Edge‑case & exception handling – empty input, null input, malformed selectors,
 *       invalid nth‑child syntax, etc.</li>
 * </ul>
 *
 * The test suite can be run with any JUnit‑4 runner.
 */
public class QueryParserTest {

    /* ---------------------------------------------------------------------- */
    /* 1.  Public method signatures (extracted from QueryParser)               */
    /* ---------------------------------------------------------------------- */
    // public static Evaluator parse(String query)

    /* ---------------------------------------------------------------------- */
    /* 2.  Basic functionality tests                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void parse_simpleTag() {
        Evaluator eval = QueryParser.parse("div");
        assertTrue("Expected Evaluator.Tag for simple tag selector", eval instanceof Evaluator.Tag);
        assertEquals("div", ((Evaluator.Tag) eval).getTagName());
    }

    @Test
    public void parse_idSelector() {
        Evaluator eval = QueryParser.parse("#myId");
        assertTrue(eval instanceof Evaluator.Id);
        assertEquals("myId", ((Evaluator.Id) eval).getId());
    }

    @Test
    public void parse_classSelector() {
        Evaluator eval = QueryParser.parse(".myClass");
        assertTrue(eval instanceof Evaluator.Class);
        assertEquals("myClass", ((Evaluator.Class) eval).getClassName());
    }

    @Test
    public void parse_allElementsSelector() {
        Evaluator eval = QueryParser.parse("*");
        assertTrue(eval instanceof Evaluator.AllElements);
    }

    @Test
    public void parse_attributeExists() {
        Evaluator eval = QueryParser.parse("[href]");
        assertTrue(eval instanceof Evaluator.Attribute);
        assertEquals("href", ((Evaluator.Attribute) eval).getKey());
    }

    @Test
    public void parse_attributeEquals() {
        Evaluator eval = QueryParser.parse("[data=val]");
        assertTrue(eval instanceof Evaluator.AttributeWithValue);
        Evaluator.AttributeWithValue ev = (Evaluator.AttributeWithValue) eval;
        assertEquals("data", ev.getKey());
        assertEquals("val", ev.getValue());
    }

    @Test
    public void parse_attributeNotEquals() {
        Evaluator eval = QueryParser.parse("[data!=val]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueNot);
        Evaluator.AttributeWithValueNot ev = (Evaluator.AttributeWithValueNot) eval;
        assertEquals("data", ev.getKey());
        assertEquals("val", ev.getValue());
    }

    @Test
    public void parse_attributeStartsWith() {
        Evaluator eval = QueryParser.parse("[data^=val]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueStarting);
        Evaluator.AttributeWithValueStarting ev = (Evaluator.AttributeWithValueStarting) eval;
        assertEquals("data", ev.getKey());
        assertEquals("val", ev.getPrefix());
    }

    @Test
    public void parse_attributeEndsWith() {
        Evaluator eval = QueryParser.parse("[data$=val]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueEnding);
        Evaluator.AttributeWithValueEnding ev = (Evaluator.AttributeWithValueEnding) eval;
        assertEquals("data", ev.getKey());
        assertEquals("val", ev.getSuffix());
    }

    @Test
    public void parse_attributeContains() {
        Evaluator eval = QueryParser.parse("[data*=val]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueContaining);
        Evaluator.AttributeWithValueContaining ev = (Evaluator.AttributeWithValueContaining) eval;
        assertEquals("data", ev.getKey());
        assertEquals("val", ev.getContained());
    }

    @Test
    public void parse_attributeRegex() {
        Evaluator eval = QueryParser.parse("[data~=\\d+]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueMatching);
        Evaluator.AttributeWithValueMatching ev = (Evaluator.AttributeWithValueMatching) eval;
        assertEquals("data", ev.getKey());
        assertEquals(Pattern.compile("\\d+").pattern(), ev.getPattern().pattern());
    }

    @Test
    public void parse_pseudoContains() {
        Evaluator eval = QueryParser.parse(":contains(hello world)");
        assertTrue(eval instanceof Evaluator.ContainsText);
        assertEquals("hello world", ((Evaluator.ContainsText) eval).getSearchText());
    }

    @Test
    public void parse_pseudoContainsOwn() {
        Evaluator eval = QueryParser.parse(":containsOwn(hello)");
        assertTrue(eval instanceof Evaluator.ContainsOwnText);
        assertEquals("hello", ((Evaluator.ContainsOwnText) eval).getSearchText());
    }

    @Test
    public void parse_pseudoMatches() {
        Evaluator eval = QueryParser.parse(":matches(foo.*)");
        assertTrue(eval instanceof Evaluator.Matches);
        assertEquals("foo.*", ((Evaluator.Matches) eval).getPattern().pattern());
    }

    @Test
    public void parse_pseudoMatchesOwn() {
        Evaluator eval = QueryParser.parse(":matchesOwn(bar\\d+)");
        assertTrue(eval instanceof Evaluator.MatchesOwn);
        assertEquals("bar\\d+", ((Evaluator.MatchesOwn) eval).getPattern().pattern());
    }

    @Test
    public void parse_notPseudo() {
        Evaluator eval = QueryParser.parse(":not(.excluded)");
        assertTrue(eval instanceof StructuralEvaluator.Not);
        // Inside the Not evaluator there should be a Class evaluator for ".excluded"
        Evaluator inner = ((StructuralEvaluator.Not) eval).getEvaluator();
        assertTrue(inner instanceof Evaluator.Class);
        assertEquals("excluded", ((Evaluator.Class) inner).getClassName());
    }

    @Test
    public void parse_childCombinator() {
        Evaluator eval = QueryParser.parse("div > p");
        assertTrue(eval instanceof CombiningEvaluator.And);
        // The top‑level AND should contain a ImmediateParent evaluator
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        boolean hasImmediateParent = and.getEvaluators().stream()
                .anyMatch(e -> e instanceof StructuralEvaluator.ImmediateParent);
        assertTrue("Expected ImmediateParent inside AND for child combinator", hasImmediateParent);
    }

    @Test
    public void parse_descendantCombinator() {
        Evaluator eval = QueryParser.parse("ul li");
        assertTrue(eval instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        boolean hasParent = and.getEvaluators().stream()
                .anyMatch(e -> e instanceof StructuralEvaluator.Parent);
        assertTrue("Expected Parent inside AND for descendant combinator", hasParent);
    }

    @Test
    public void parse_adjacentSiblingCombinator() {
        Evaluator eval = QueryParser.parse("h1 + p");
        assertTrue(eval instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        boolean hasImmediatePrev = and.getEvaluators().stream()
                .anyMatch(e -> e instanceof StructuralEvaluator.ImmediatePreviousSibling);
        assertTrue("Expected ImmediatePreviousSibling inside AND for adjacent sibling combinator", hasImmediatePrev);
    }

    @Test
    public void parse_generalSiblingCombinator() {
        Evaluator eval = QueryParser.parse("h1 ~ p");
        assertTrue(eval instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        boolean hasPrev = and.getEvaluators().stream()
                .anyMatch(e -> e instanceof StructuralEvaluator.PreviousSibling);
        assertTrue("Expected PreviousSibling inside AND for general sibling combinator", hasPrev);
    }

    @Test
    public void parse_groupingCombinator() {
        Evaluator eval = QueryParser.parse("div, span");
        assertTrue(eval instanceof CombiningEvaluator.Or);
        CombiningEvaluator.Or or = (CombiningEvaluator.Or) eval;
        assertEquals(2, or.getEvaluators().size());
        assertTrue(or.getEvaluators().get(0) instanceof Evaluator.Tag);
        assertTrue(or.getEvaluators().get(1) instanceof Evaluator.Tag);
    }

    @Test
    public void parse_nthChildOdd() {
        Evaluator eval = QueryParser.parse(":nth-child(odd)");
        assertTrue(eval instanceof Evaluator.IsNthChild);
        Evaluator.IsNthChild nth = (Evaluator.IsNthChild) eval;
        assertEquals(2, nth.getA());
        assertEquals(1, nth.getB());
    }

    @Test
    public void parse_nthChildEven() {
        Evaluator eval = QueryParser.parse(":nth-child(even)");
        assertTrue(eval instanceof Evaluator.IsNthChild);
        Evaluator.IsNthChild nth = (Evaluator.IsNthChild) eval;
        assertEquals(2, nth.getA());
        assertEquals(0, nth.getB());
    }

    @Test
    public void parse_nthChildFormula() {
        Evaluator eval = QueryParser.parse(":nth-child(3n+2)");
        assertTrue(eval instanceof Evaluator.IsNthChild);
        Evaluator.IsNthChild nth = (Evaluator.IsNthChild) eval;
        assertEquals(3, nth.getA());
        assertEquals(2, nth.getB());
    }

    /* ---------------------------------------------------------------------- */
    /* 3.  Edge‑case & exception handling tests                               */
    /* ---------------------------------------------------------------------- */

    @Test(expected = Selector.SelectorParseException.class)
    public void parse_nullQueryThrows() {
        QueryParser.parse(null);
    }

    @Test
    public void parse_emptyStringReturnsAndEvaluatorWithNoChildren() {
        Evaluator eval = QueryParser.parse("");
        // An empty query results in an AND evaluator with an empty internal list.
        assertTrue(eval instanceof CombiningEvaluator.And);
        assertEquals(0, ((CombiningEvaluator.And) eval).getEvaluators().size());
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void parse_invalidSyntaxThrows() {
        QueryParser.parse("??");
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void parse_invalidAttributeThrows() {
        QueryParser.parse("[data~=]");
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void parse_invalidNthChildThrows() {
        QueryParser.parse(":nth-child(abc)");
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void parse_missingClosingParenInPseudoThrows() {
        QueryParser.parse(":contains(hello");
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void parse_notWithEmptySubselectorThrows() {
        QueryParser.parse(":not()");
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void parse_hasWithEmptySubselectorThrows() {
        QueryParser.parse(":has()");
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void parse_attributeWithInvalidOperatorThrows() {
        QueryParser.parse("[data??=val]");
    }
}
```
###Test END##