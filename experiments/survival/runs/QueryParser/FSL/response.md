###Test START##
```java
package org.jsoup.select;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.jsoup.helper.Validate;
import org.jsoup.parser.TokenQueue;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link QueryParser}.
 *
 * The tests cover every parsing branch in {@code QueryParser}:
 * <ul>
 *   <li>Simple selectors: id, class, tag (including namespace wild‑cards)</li>
 *   <li>Attribute selectors with all supported operators</li>
 *   <li>Pseudo‑classes: :lt, :gt, :eq, :first‑child, :last‑of‑type, :nth‑child, …</li>
 *   <li>Structural pseudo‑selectors: :has, :contains, :containsOwn, :containsData,
 *       :matches, :matchesOwn, :not</li>
 *   <li>Combinators: > + ~ , and whitespace (descendant)</li>
 *   <li>Grouping with commas and precedence handling</li>
 *   <li>Error paths: unknown combinator, malformed attribute, malformed nth‑child,
 *       empty sub‑query for :has / :not</li>
 * </ul>
 *
 * Assertions are based on the concrete {@link Evaluator} implementation that
 * {@link QueryParser#parse(String)} returns.  Where a selector creates a
 * {@link CombiningEvaluator.And} or {@link CombiningEvaluator.Or}, the test
 * verifies the exact type hierarchy to ensure the parser built the correct tree.
 */
public class QueryParserTest {

    /* --------------------------------------------------------------------- */
    /*  Simple selectors                                                    */
    /* --------------------------------------------------------------------- */

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
    public void parsesTagSelector() {
        Evaluator eval = QueryParser.parse("div");
        assertTrue(eval instanceof Evaluator.Tag);
        assertEquals("div", ((Evaluator.Tag) eval).getTagName());
    }

    @Test
    public void parsesTagSelectorWithNamespaceWildcard() {
        Evaluator eval = QueryParser.parse("*|svg");
        assertTrue(eval instanceof CombiningEvaluator.Or);
        CombiningEvaluator.Or or = (CombiningEvaluator.Or) eval;
        assertEquals(2, or.evaluatorsSize());

        // first part – exact match on the qualified name
        assertTrue(or.get(0) instanceof Evaluator.Tag);
        assertEquals("*|svg", ((Evaluator.Tag) or.get(0)).getTagName());

        // second part – suffix match after the wildcard namespace
        assertTrue(or.get(1) instanceof Evaluator.TagEndsWith);
        assertEquals(":svg", ((Evaluator.TagEndsWith) or.get(1)).getSuffix());
    }

    @Test
    public void parsesTagSelectorWithNamespacePipe() {
        Evaluator eval = QueryParser.parse("xhtml|p");
        assertTrue(eval instanceof Evaluator.Tag);
        assertEquals("xhtml:p", ((Evaluator.Tag) eval).getTagName());
    }

    @Test
    public void parsesAllElementsSelector() {
        Evaluator eval = QueryParser.parse("*");
        assertTrue(eval instanceof Evaluator.AllElements);
    }

    /* --------------------------------------------------------------------- */
    /*  Attribute selectors                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void parsesAttributeExists() {
        Evaluator eval = QueryParser.parse("[href]");
        assertTrue(eval instanceof Evaluator.Attribute);
        assertEquals("href", ((Evaluator.Attribute) eval).getKey());
    }

    @Test
    public void parsesAttributeStartsWithCaret() {
        Evaluator eval = QueryParser.parse("[^src]");
        assertTrue(eval instanceof Evaluator.AttributeStarting);
        assertEquals("src", ((Evaluator.AttributeStarting) eval).getKey());
    }

    @Test
    public void parsesAttributeEquals() {
        Evaluator eval = QueryParser.parse("[title=Hello]");
        assertTrue(eval instanceof Evaluator.AttributeWithValue);
        Evaluator.AttributeWithValue a = (Evaluator.AttributeWithValue) eval;
        assertEquals("title", a.getKey());
        assertEquals("Hello", a.getValue());
    }

    @Test
    public void parsesAttributeNotEquals() {
        Evaluator eval = QueryParser.parse("[title!=World]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueNot);
        Evaluator.AttributeWithValueNot a = (Evaluator.AttributeWithValueNot) eval;
        assertEquals("title", a.getKey());
        assertEquals("World", a.getValue());
    }

    @Test
    public void parsesAttributeStartsWith() {
        Evaluator eval = QueryParser.parse("[data^=foo]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueStarting);
        Evaluator.AttributeWithValueStarting a = (Evaluator.AttributeWithValueStarting) eval;
        assertEquals("data", a.getKey());
        assertEquals("foo", a.getValue());
    }

    @Test
    public void parsesAttributeEndsWith() {
        Evaluator eval = QueryParser.parse("[data$=bar]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueEnding);
        Evaluator.AttributeWithValueEnding a = (Evaluator.AttributeWithValueEnding) eval;
        assertEquals("data", a.getKey());
        assertEquals("bar", a.getValue());
    }

    @Test
    public void parsesAttributeContains() {
        Evaluator eval = QueryParser.parse("[data*=mid]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueContaining);
        Evaluator.AttributeWithValueContaining a = (Evaluator.AttributeWithValueContaining) eval;
        assertEquals("data", a.getKey());
        assertEquals("mid", a.getValue());
    }

    @Test
    public void parsesAttributeRegexMatch() {
        Evaluator eval = QueryParser.parse("[data~=\\d{3}]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueMatching);
        Evaluator.AttributeWithValueMatching a = (Evaluator.AttributeWithValueMatching) eval;
        assertEquals("data", a.getKey());
        assertEquals("\\d{3}", a.getPattern().pattern());
    }

    /* --------------------------------------------------------------------- */
    /*  Pseudo‑class selectors                                                */
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
        Evaluator eval = QueryParser.parse(":eq(0)");
        assertTrue(eval instanceof Evaluator.IndexEquals);
        assertEquals(0, ((Evaluator.IndexEquals) eval).getIndex());
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

    @Test
    public void parsesNthChildOdd() {
        Evaluator eval = QueryParser.parse(":nth-child(odd)");
        assertTrue(eval instanceof Evaluator.IsNthChild);
        Evaluator.IsNthChild nth = (Evaluator.IsNthChild) eval;
        assertEquals(2, nth.getA());
        assertEquals(1, nth.getB());
    }

    @Test
    public void parsesNthLastOfTypeComplex() {
        Evaluator eval = QueryParser.parse(":nth-last-of-type(3n-2)");
        assertTrue(eval instanceof Evaluator.IsNthLastOfType);
        Evaluator.IsNthLastOfType nth = (Evaluator.IsNthLastOfType) eval;
        assertEquals(3, nth.getA());
        assertEquals(-2, nth.getB());
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
    public void parsesMatchText() {
        Evaluator eval = QueryParser.parse(":matchText");
        assertTrue(eval instanceof Evaluator.MatchText);
    }

    /* --------------------------------------------------------------------- */
    /*  Structural pseudo‑selectors                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void parsesHas() {
        Evaluator eval = QueryParser.parse(":has(.child)");
        assertTrue(eval instanceof StructuralEvaluator.Has);
        StructuralEvaluator.Has has = (StructuralEvaluator.Has) eval;
        assertTrue(has.getEvaluator() instanceof Evaluator.Class);
        assertEquals("child", ((Evaluator.Class) has.getEvaluator()).getClassName());
    }

    @Test
    public void parsesContains() {
        Evaluator eval = QueryParser.parse(":contains(Hello World)");
        assertTrue(eval instanceof Evaluator.ContainsText);
        assertEquals("Hello World", ((Evaluator.ContainsText) eval).getSearchText());
    }

    @Test
    public void parsesContainsOwn() {
        Evaluator eval = QueryParser.parse(":containsOwn(OwnText)");
        assertTrue(eval instanceof Evaluator.ContainsOwnText);
        assertEquals("OwnText", ((Evaluator.ContainsOwnText) eval).getSearchText());
    }

    @Test
    public void parsesContainsData() {
        Evaluator eval = QueryParser.parse(":containsData(someData)");
        assertTrue(eval instanceof Evaluator.ContainsData);
        assertEquals("someData", ((Evaluator.ContainsData) eval).getSearchText());
    }

    @Test
    public void parsesMatches() {
        Evaluator eval = QueryParser.parse(":matches(\\d+)");
        assertTrue(eval instanceof Evaluator.Matches);
        Pattern p = ((Evaluator.Matches) eval).getPattern();
        assertEquals("\\d+", p.pattern());
    }

    @Test
    public void parsesMatchesOwn() {
        Evaluator eval = QueryParser.parse(":matchesOwn(foo|bar)");
        assertTrue(eval instanceof Evaluator.MatchesOwn);
        Pattern p = ((Evaluator.MatchesOwn) eval).getPattern();
        assertEquals("foo|bar", p.pattern());
    }

    @Test
    public void parsesNot() {
        Evaluator eval = QueryParser.parse(":not(#skip)");
        assertTrue(eval instanceof StructuralEvaluator.Not);
        StructuralEvaluator.Not not = (StructuralEvaluator.Not) eval;
        assertTrue(not.getEvaluator() instanceof Evaluator.Id);
        assertEquals("skip", ((Evaluator.Id) not.getEvaluator()).getId());
    }

    /* --------------------------------------------------------------------- */
    /*  Combinators and grouping                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void parsesDescendantCombinator() {
        Evaluator eval = QueryParser.parse("div span");
        assertTrue(eval instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        assertEquals(2, and.evaluatorsSize());
        assertTrue(and.get(0) instanceof Evaluator.Tag);
        assertTrue(and.get(1) instanceof Evaluator.Tag);
        assertEquals("div", ((Evaluator.Tag) and.get(0)).getTagName());
        assertEquals("span", ((Evaluator.Tag) and.get(1)).getTagName());
    }

    @Test
    public void parsesChildCombinator() {
        Evaluator eval = QueryParser.parse("ul > li");
        assertTrue(eval instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        // Right‑most part is ImmediateParent of the left part
        assertTrue(and.get(1) instanceof StructuralEvaluator.ImmediateParent);
    }

    @Test
    public void parsesAdjacentSiblingCombinator() {
        Evaluator eval = QueryParser.parse("h1 + p");
        assertTrue(eval instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        assertTrue(and.get(1) instanceof StructuralEvaluator.ImmediatePreviousSibling);
    }

    @Test
    public void parsesGeneralSiblingCombinator() {
        Evaluator eval = QueryParser.parse("h1 ~ p");
        assertTrue(eval instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        assertTrue(and.get(1) instanceof StructuralEvaluator.PreviousSibling);
    }

    @Test
    public void parsesGroupCombinatorWithPrecedence() {
        // "a,b > c" → OR( a , (b > c) )
        Evaluator eval = QueryParser.parse("a,b > c");
        assertTrue(eval instanceof CombiningEvaluator.Or);
        CombiningEvaluator.Or or = (CombiningEvaluator.Or) eval;
        assertEquals(2, or.evaluatorsSize());

        // first branch is just "a"
        assertTrue(or.get(0) instanceof Evaluator.Tag);
        assertEquals("a", ((Evaluator.Tag) or.get(0)).getTagName());

        // second branch must be an AND that represents "b > c"
        assertTrue(or.get(1) instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) or.get(1);
        // verify that the right side of the AND is an ImmediateParent (b is parent of c)
        assertTrue(and.get(1) instanceof StructuralEvaluator.ImmediateParent);
    }

    /* --------------------------------------------------------------------- */
    /*  Error handling                                                       */
    /* --------------------------------------------------------------------- */

    @Test(expected = Selector.SelectorParseException.class)
    public void throwsOnUnknownCombinator() {
        // The parser only knows > + ~ , and whitespace.  "!" is not a valid combinator.
        QueryParser.parse("div!p");
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void throwsOnMalformedAttribute() {
        // Missing closing bracket
        QueryParser.parse("[href");
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void throwsOnInvalidNthExpression() {
        QueryParser.parse(":nth-child(2x+1)");
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void throwsOnEmptyHasSubquery() {
        QueryParser.parse(":has()");
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void throwsOnEmptyNotSubquery() {
        QueryParser.parse(":not()");
    }

    /* --------------------------------------------------------------------- */
    /*  Utility methods for reflection (to read private fields)            */
    /* --------------------------------------------------------------------- */

    /**
     * Helper that uses reflection to read the private {@code id} field from
     * {@link Evaluator.Id}.  This is only used in tests that need to verify the
     * parsed value without exposing a public getter in the library.
     */
    private static String getId(Evaluator.Id eval) {
        try {
            java.lang.reflect.Field f = Evaluator.Id.class.getDeclaredField("id");
            f.setAccessible(true);
            return (String) f.get(eval);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper that uses reflection to read the private {@code className} field
     * from {@link Evaluator.Class}.
     */
    private static String getClassName(Evaluator.Class eval) {
        try {
            java.lang.reflect.Field f = Evaluator.Class.class.getDeclaredField("className");
            f.setAccessible(true);
            return (String) f.get(eval);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper that uses reflection to read the private {@code tagName} field
     * from {@link Evaluator.Tag}.
     */
    private static String getTagName(Evaluator.Tag eval) {
        try {
            java.lang.reflect.Field f = Evaluator.Tag.class.getDeclaredField("tagName");
            f.setAccessible(true);
            return (String) f.get(eval);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // The above reflection helpers are exercised in the first three tests:
    @Test
    public void verifiesReflectionHelpers() {
        Evaluator.Id idEval = (Evaluator.Id) QueryParser.parse("#myId");
        assertEquals("myId", getId(idEval));

        Evaluator.Class classEval = (Evaluator.Class) QueryParser.parse(".myClass");
        assertEquals("myClass", getClassName(classEval));

        Evaluator.Tag tagEval = (Evaluator.Tag) QueryParser.parse("section");
        assertEquals("section", getTagName(tagEval));
    }
}
```
###Test END##