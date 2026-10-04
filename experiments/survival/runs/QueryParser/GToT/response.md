###Test START##
```java
package org.jsoup.select;

import org.jsoup.helper.Validate;
import org.jsoup.parser.TokenQueue;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link QueryParser}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public parsing entry points.</li>
 *   <li>Typical selector usage (tag, id, class, attribute, pseudo‑classes, combinators, groups, namespaces).</li>
 *   <li>Edge‑case handling and exception scenarios (empty query, malformed selectors, illegal numbers, bad regexes, etc.).</li>
 * </ul>
 *
 * The test class lives in the same package as {@code QueryParser} so that it can
 * reference package‑private inner classes (e.g. {@code StructuralEvaluator}).
 */
public class QueryParserTest {

    /* --------------------------------------------------------------------- */
    /* 1.  Simple selectors                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void parsesSimpleTag() {
        Evaluator eval = QueryParser.parse("div");
        assertTrue(eval instanceof Evaluator.Tag);
        assertEquals("div", ((Evaluator.Tag) eval).getTagName());
    }

    @Test
    public void parsesIdSelector() {
        Evaluator eval = QueryParser.parse("#myId");
        assertTrue(eval instanceof Evaluator.Id);
        assertEquals("myId", ((Evaluator.Id) eval).getId());
    }

    @Test
    public void parsesClassSelector() {
        Evaluator eval = QueryParser.parse(".myClass");
        assertTrue(eval instanceof Evaluator.Class);
        assertEquals("myClass", ((Evaluator.Class) eval).getClassName());
    }

    @Test
    public void parsesAllElementsWildcard() {
        Evaluator eval = QueryParser.parse("*");
        assertTrue(eval instanceof Evaluator.AllElements);
    }

    /* --------------------------------------------------------------------- */
    /* 2.  Attribute selectors                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void parsesAttributeExists() {
        Evaluator eval = QueryParser.parse("[href]");
        assertTrue(eval instanceof Evaluator.Attribute);
    }

    @Test
    public void parsesAttributeEquals() {
        Evaluator eval = QueryParser.parse("[href=example.com]");
        assertTrue(eval instanceof Evaluator.AttributeWithValue);
        Evaluator.AttributeWithValue a = (Evaluator.AttributeWithValue) eval;
        assertEquals("href", a.getKey());
        assertEquals("example.com", a.getValue());
    }

    @Test
    public void parsesAttributeNotEquals() {
        Evaluator eval = QueryParser.parse("[href!=example.com]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueNot);
    }

    @Test
    public void parsesAttributeStartsWith() {
        Evaluator eval = QueryParser.parse("[href^=http]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueStarting);
    }

    @Test
    public void parsesAttributeEndsWith() {
        Evaluator eval = QueryParser.parse("[href$=.com]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueEnding);
    }

    @Test
    public void parsesAttributeContains() {
        Evaluator eval = QueryParser.parse("[href*=example]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueContaining);
    }

    @Test
    public void parsesAttributeRegex() {
        Evaluator eval = QueryParser.parse("[href~=^https?://]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueMatching);
        Pattern p = ((Evaluator.AttributeWithValueMatching) eval).getPattern();
        assertTrue(p.matcher("https://jsoup.org").matches());
    }

    /* --------------------------------------------------------------------- */
    /* 3.  Pseudo‑class selectors                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void parsesIndexLessThan() {
        Evaluator eval = QueryParser.parse(":lt(3)");
        assertTrue(eval instanceof Evaluator.IndexLessThan);
        assertEquals(3, ((Evaluator.IndexLessThan) eval).getIndex());
    }

    @Test
    public void parsesIndexGreaterThan() {
        Evaluator eval = QueryParser.parse(":gt(1)");
        assertTrue(eval instanceof Evaluator.IndexGreaterThan);
        assertEquals(1, ((Evaluator.IndexGreaterThan) eval).getIndex());
    }

    @Test
    public void parsesIndexEquals() {
        Evaluator eval = QueryParser.parse(":eq(2)");
        assertTrue(eval instanceof Evaluator.IndexEquals);
        assertEquals(2, ((Evaluator.IndexEquals) eval).getIndex());
    }

    @Test
    public void parsesNthChildOdd() {
        Evaluator eval = QueryParser.parse(":nth-child(odd)");
        assertTrue(eval instanceof Evaluator.IsNthChild);
        Evaluator.IsNthChild nth = (Evaluator.IsNthChild) eval;
        assertEquals(2, nth.getA());
        assertEquals(1, nth.getB());
    }

    @Test
    public void parsesNthChildExpression() {
        Evaluator eval = QueryParser.parse(":nth-child(2n+3)");
        assertTrue(eval instanceof Evaluator.IsNthChild);
        Evaluator.IsNthChild nth = (Evaluator.IsNthChild) eval;
        assertEquals(2, nth.getA());
        assertEquals(3, nth.getB());
    }

    @Test
    public void parsesFirstChild() {
        Evaluator eval = QueryParser.parse(":first-child");
        assertTrue(eval instanceof Evaluator.IsFirstChild);
    }

    @Test
    public void parsesLastChild() {
        Evaluator eval = QueryParser.parse(":last-child");
        assertTrue(eval instanceof Evaluator.IsLastChild);
    }

    @Test
    public void parsesOnlyChild() {
        Evaluator eval = QueryParser.parse(":only-child");
        assertTrue(eval instanceof Evaluator.IsOnlyChild);
    }

    @Test
    public void parsesEmpty() {
        Evaluator eval = QueryParser.parse(":empty");
        assertTrue(eval instanceof Evaluator.IsEmpty);
    }

    @Test
    public void parsesRoot() {
        Evaluator eval = QueryParser.parse(":root");
        assertTrue(eval instanceof Evaluator.IsRoot);
    }

    @Test
    public void parsesHas() {
        Evaluator eval = QueryParser.parse("div:has(p)");
        assertTrue(eval instanceof StructuralEvaluator.Has);
        StructuralEvaluator.Has has = (StructuralEvaluator.Has) eval;
        assertTrue(has.getEvaluator() instanceof Evaluator.Tag);
        assertEquals("p", ((Evaluator.Tag) has.getEvaluator()).getTagName());
    }

    @Test
    public void parsesContains() {
        Evaluator eval = QueryParser.parse("p:contains(hello)");
        assertTrue(eval instanceof Evaluator.ContainsText);
        assertEquals("hello", ((Evaluator.ContainsText) eval).getSearchText());
    }

    @Test
    public void parsesContainsOwn() {
        Evaluator eval = QueryParser.parse("p:containsOwn(world)");
        assertTrue(eval instanceof Evaluator.ContainsOwnText);
        assertEquals("world", ((Evaluator.ContainsOwnText) eval).getSearchText());
    }

    @Test
    public void parsesContainsData() {
        Evaluator eval = QueryParser.parse("p:containsData(data)");
        assertTrue(eval instanceof Evaluator.ContainsData);
        assertEquals("data", ((Evaluator.ContainsData) eval).getSearchText());
    }

    @Test
    public void parsesMatches() {
        Evaluator eval = QueryParser.parse("p:matches(\\d+)");
        assertTrue(eval instanceof Evaluator.Matches);
        Pattern p = ((Evaluator.Matches) eval).getPattern();
        assertTrue(p.matcher("123").matches());
    }

    @Test
    public void parsesMatchesOwn() {
        Evaluator eval = QueryParser.parse("p:matchesOwn(\\w+)");
        assertTrue(eval instanceof Evaluator.MatchesOwn);
        Pattern p = ((Evaluator.MatchesOwn) eval).getPattern();
        assertTrue(p.matcher("abc").matches());
    }

    @Test
    public void parsesNot() {
        Evaluator eval = QueryParser.parse("div:not(.ignore)");
        assertTrue(eval instanceof StructuralEvaluator.Not);
        StructuralEvaluator.Not not = (StructuralEvaluator.Not) eval;
        assertTrue(not.getEvaluator() instanceof Evaluator.Class);
    }

    /* --------------------------------------------------------------------- */
    /* 4.  Combinators (>, space, +, ~) and grouping (,)                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void parsesChildCombinator() {
        Evaluator eval = QueryParser.parse("div > p");
        assertTrue(eval instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        // left side = tag "div", right side = ImmediateParent(p, div)
        assertTrue(and.getLeft() instanceof Evaluator.Tag);
        assertTrue(and.getRight() instanceof StructuralEvaluator.ImmediateParent);
    }

    @Test
    public void parsesDescendantCombinator() {
        Evaluator eval = QueryParser.parse("div p");
        assertTrue(eval instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        assertTrue(and.getLeft() instanceof Evaluator.Tag);
        assertTrue(and.getRight() instanceof StructuralEvaluator.Parent);
    }

    @Test
    public void parsesAdjacentSiblingCombinator() {
        Evaluator eval = QueryParser.parse("div + p");
        assertTrue(eval instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        assertTrue(and.getRight() instanceof StructuralEvaluator.ImmediatePreviousSibling);
    }

    @Test
    public void parsesGeneralSiblingCombinator() {
        Evaluator eval = QueryParser.parse("div ~ p");
        assertTrue(eval instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        assertTrue(and.getRight() instanceof StructuralEvaluator.PreviousSibling);
    }

    @Test
    public void parsesGroupComma() {
        Evaluator eval = QueryParser.parse("div, p");
        assertTrue(eval instanceof CombiningEvaluator.Or);
        CombiningEvaluator.Or or = (CombiningEvaluator.Or) eval;
        assertEquals(2, or.getEvaluators().size());
        assertTrue(or.getEvaluators().get(0) instanceof Evaluator.Tag);
        assertTrue(or.getEvaluators().get(1) instanceof Evaluator.Tag);
    }

    /* --------------------------------------------------------------------- */
    /* 5.  Namespace handling                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void parsesWildcardNamespace() {
        Evaluator eval = QueryParser.parse("*|div");
        assertTrue(eval instanceof CombiningEvaluator.Or);
        CombiningEvaluator.Or or = (CombiningEvaluator.Or) eval;
        // first part is a plain Tag, second part a TagEndsWith
        assertTrue(or.getEvaluators().get(0) instanceof Evaluator.Tag);
        assertTrue(or.getEvaluators().get(1) instanceof Evaluator.TagEndsWith);
    }

    @Test
    public void parsesExplicitNamespace() {
        Evaluator eval = QueryParser.parse("ns|div");
        assertTrue(eval instanceof Evaluator.Tag);
        assertEquals("ns:div", ((Evaluator.Tag) eval).getTagName());
    }

    /* --------------------------------------------------------------------- */
    /* 6.  Edge‑case and error handling                                      */
    /* --------------------------------------------------------------------- */

    @Test(expected = Selector.SelectorParseException.class)
    public void emptyQueryThrows() {
        QueryParser.parse("");
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void unknownTokenThrows() {
        QueryParser.parse("??");
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void unclosedAttributeThrows() {
        QueryParser.parse("[href");
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void unclosedPseudoThrows() {
        QueryParser.parse(":lt(");
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void invalidNthThrows() {
        QueryParser.parse(":nth-child(abc)");
    }

    @Test(expected = java.util.regex.PatternSyntaxException.class)
    public void invalidRegexInAttributeThrows() {
        QueryParser.parse("[href~=(*]");
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void nonNumericIndexThrows() {
        QueryParser.parse(":eq(notnum)");
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void emptyHasSubqueryThrows() {
        QueryParser.parse(":has()");
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void emptyNotSubqueryThrows() {
        QueryParser.parse(":not()");
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void emptyContainsThrows() {
        QueryParser.parse(":contains()");
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void emptyMatchesThrows() {
        QueryParser.parse(":matches()");
    }

    /* --------------------------------------------------------------------- */
    /* 7.  Additional sanity checks                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void parsesComplexSelectorWithMultipleCombinators() {
        // "ul > li:first-child + a[href^=http]"
        Evaluator eval = QueryParser.parse("ul > li:first-child + a[href^=http]");
        assertTrue(eval instanceof CombiningEvaluator.And);
        // The top‑level should be an AND combining the "+ a[...]" part with the left side.
        // We only verify the tree depth is as expected.
        CombiningEvaluator.And top = (CombiningEvaluator.And) eval;
        assertTrue(top.getRight() instanceof StructuralEvaluator.ImmediatePreviousSibling);
        StructuralEvaluator.ImmediatePreviousSibling sibling = (StructuralEvaluator.ImmediatePreviousSibling) top.getRight();
        assertTrue(sibling.getEvaluator() instanceof Evaluator.Tag);
        assertEquals("a", ((Evaluator.Tag) sibling.getEvaluator()).getTagName());
    }
}
```
###Test END##