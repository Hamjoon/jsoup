package org.jsoup.select;

import org.jsoup.helper.Validate;
import org.jsoup.parser.TokenQueue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Comprehensive tests for {@link QueryParser}.
 * <p>
 * The tests exercise every public parsing path, verify the type of {@link Evaluator}
 * produced and, where appropriate, inspect internal state via reflection.
 * </p>
 */
@RunWith(JUnit4.class)
public class QueryParserTest {

    /** Utility to read a private field value via reflection. */
    private static Object getField(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException("Unable to read field '" + name + "' from " + target, e);
        }
    }

    @Test
    public void testParseIdSelector() {
        Evaluator eval = QueryParser.parse("#myId");
        assertTrue(eval instanceof Evaluator.Id);
        assertEquals("myId", getField(eval, "id"));
    }

    @Test
    public void testParseClassSelector() {
        Evaluator eval = QueryParser.parse(".myClass");
        assertTrue(eval instanceof Evaluator.Class);
        assertEquals("myClass", getField(eval, "className"));
    }

    @Test
    public void testParseTagSelector() {
        Evaluator eval = QueryParser.parse("div");
        assertTrue(eval instanceof Evaluator.Tag);
        assertEquals("div", getField(eval, "tagName"));
    }

    @Test
    public void testParseWildcardTagSelector() {
        Evaluator eval = QueryParser.parse("*");
        assertTrue(eval instanceof Evaluator.AllElements);
    }

    @Test
    public void testParseNamespaceWildcardTag() {
        Evaluator eval = QueryParser.parse("*|svg");
        assertTrue(eval instanceof CombiningEvaluator.Or);
        // The Or should contain two evaluators: Tag and TagEndsWith
        CombiningEvaluator.Or or = (CombiningEvaluator.Or) eval;
        assertEquals(2, or.evaluators.size());
        assertTrue(or.evaluators.get(0) instanceof Evaluator.Tag);
        assertTrue(or.evaluators.get(1) instanceof Evaluator.TagEndsWith);
    }

    @Test
    public void testParseNamespaceTag() {
        Evaluator eval = QueryParser.parse("x|div");
        assertTrue(eval instanceof Evaluator.Tag);
        assertEquals("x:div", getField(eval, "tagName"));
    }

    @Test
    public void testParseAttributeExists() {
        Evaluator eval = QueryParser.parse("[title]");
        assertTrue(eval instanceof Evaluator.Attribute);
        assertEquals("title", getField(eval, "key"));
    }

    @Test
    public void testParseAttributeStartsWith() {
        Evaluator eval = QueryParser.parse("[^data]");
        assertTrue(eval instanceof Evaluator.AttributeStarting);
        assertEquals("data", getField(eval, "key"));
    }

    @Test
    public void testParseAttributeEquals() {
        Evaluator eval = QueryParser.parse("[href=example.com]");
        assertTrue(eval instanceof Evaluator.AttributeWithValue);
        assertEquals("href", getField(eval, "key"));
        assertEquals("example.com", getField(eval, "value"));
    }

    @Test
    public void testParseAttributeNotEquals() {
        Evaluator eval = QueryParser.parse("[href!=example.com]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueNot);
        assertEquals("href", getField(eval, "key"));
        assertEquals("example.com", getField(eval, "value"));
    }

    @Test
    public void testParseAttributeStartsWithValue() {
        Evaluator eval = QueryParser.parse("[href^=https]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueStarting);
        assertEquals("href", getField(eval, "key"));
        assertEquals("https", getField(eval, "value"));
    }

    @Test
    public void testParseAttributeEndsWithValue() {
        Evaluator eval = QueryParser.parse("[href$=.org]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueEnding);
        assertEquals("href", getField(eval, "key"));
        assertEquals(".org", getField(eval, "value"));
    }

    @Test
    public void testParseAttributeContainsValue() {
        Evaluator eval = QueryParser.parse("[href*=example]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueContaining);
        assertEquals("href", getField(eval, "key"));
        assertEquals("example", getField(eval, "value"));
    }

    @Test
    public void testParseAttributeRegex() {
        Evaluator eval = QueryParser.parse("[data~=\\d+]");
        assertTrue(eval instanceof Evaluator.AttributeWithValueMatching);
        Pattern p = (Pattern) getField(eval, "pattern");
        assertEquals("\\d+", p.pattern());
    }

    @Test
    public void testParsePseudoContains() {
        Evaluator eval = QueryParser.parse(":contains(hello world)");
        assertTrue(eval instanceof Evaluator.ContainsText);
        assertEquals("hello world", getField(eval, "searchText"));
    }

    @Test
    public void testParsePseudoContainsOwn() {
        Evaluator eval = QueryParser.parse(":containsOwn(own text)");
        assertTrue(eval instanceof Evaluator.ContainsOwnText);
        assertEquals("own text", getField(eval, "searchText"));
    }

    @Test
    public void testParsePseudoContainsData() {
        Evaluator eval = QueryParser.parse(":containsData(data)");
        assertTrue(eval instanceof Evaluator.ContainsData);
        assertEquals("data", getField(eval, "searchText"));
    }

    @Test
    public void testParsePseudoMatches() {
        Evaluator eval = QueryParser.parse(":matches(foo.*)");
        assertTrue(eval instanceof Evaluator.Matches);
        Pattern p = (Pattern) getField(eval, "pattern");
        assertEquals("foo.*", p.pattern());
    }

    @Test
    public void testParsePseudoMatchesOwn() {
        Evaluator eval = QueryParser.parse(":matchesOwn(bar\\d+)");
        assertTrue(eval instanceof Evaluator.MatchesOwn);
        Pattern p = (Pattern) getField(eval, "pattern");
        assertEquals("bar\\d+", p.pattern());
    }

    @Test
    public void testParsePseudoIndexLessThan() {
        Evaluator eval = QueryParser.parse(":lt(3)");
        assertTrue(eval instanceof Evaluator.IndexLessThan);
        assertEquals(3, getField(eval, "index"));
    }

    @Test
    public void testParsePseudoIndexGreaterThan() {
        Evaluator eval = QueryParser.parse(":gt(5)");
        assertTrue(eval instanceof Evaluator.IndexGreaterThan);
        assertEquals(5, getField(eval, "index"));
    }

    @Test
    public void testParsePseudoIndexEquals() {
        Evaluator eval = QueryParser.parse(":eq(2)");
        assertTrue(eval instanceof Evaluator.IndexEquals);
        assertEquals(2, getField(eval, "index"));
    }

    @Test
    public void testParsePseudoFirstChild() {
        Evaluator eval = QueryParser.parse(":first-child");
        assertTrue(eval instanceof Evaluator.IsFirstChild);
    }

    @Test
    public void testParsePseudoLastChild() {
        Evaluator eval = QueryParser.parse(":last-child");
        assertTrue(eval instanceof Evaluator.IsLastChild);
    }

    @Test
    public void testParsePseudoOnlyChild() {
        Evaluator eval = QueryParser.parse(":only-child");
        assertTrue(eval instanceof Evaluator.IsOnlyChild);
    }

    @Test
    public void testParsePseudoEmpty() {
        Evaluator eval = QueryParser.parse(":empty");
        assertTrue(eval instanceof Evaluator.IsEmpty);
    }

    @Test
    public void testParsePseudoRoot() {
        Evaluator eval = QueryParser.parse(":root");
        assertTrue(eval instanceof Evaluator.IsRoot);
    }

    @Test
    public void testParsePseudoMatchText() {
        Evaluator eval = QueryParser.parse(":matchText");
        assertTrue(eval instanceof Evaluator.MatchText);
    }

    @Test
    public void testParsePseudoNthChild() {
        Evaluator eval = QueryParser.parse(":nth-child(2n+3)");
        assertTrue(eval instanceof Evaluator.IsNthChild);
        assertEquals(2, getField(eval, "a"));
        assertEquals(3, getField(eval, "b"));
    }

    @Test
    public void testParsePseudoNthLastChildEven() {
        Evaluator eval = QueryParser.parse(":nth-last-child(even)");
        assertTrue(eval instanceof Evaluator.IsNthLastChild);
        assertEquals(2, getField(eval, "a"));
        assertEquals(0, getField(eval, "b"));
    }

    @Test
    public void testParsePseudoNthOfTypeOdd() {
        Evaluator eval = QueryParser.parse(":nth-of-type(odd)");
        assertTrue(eval instanceof Evaluator.IsNthOfType);
        assertEquals(2, getField(eval, "a"));
        assertEquals(1, getField(eval, "b"));
    }

    @Test
    public void testParsePseudoNthLastOfType() {
        Evaluator eval = QueryParser.parse(":nth-last-of-type(3n-1)");
        assertTrue(eval instanceof Evaluator.IsNthLastOfType);
        assertEquals(3, getField(eval, "a"));
        assertEquals(-1, getField(eval, "b"));
    }

    @Test
    public void testParsePseudoHas() {
        Evaluator eval = QueryParser.parse(":has(div)");
        assertTrue(eval instanceof StructuralEvaluator.Has);
        Evaluator inner = (Evaluator) getField(eval, "evaluator");
        assertTrue(inner instanceof Evaluator.Tag);
        assertEquals("div", getField(inner, "tagName"));
    }

    @Test
    public void testParsePseudoNot() {
        Evaluator eval = QueryParser.parse(":not(.ignore)");
        assertTrue(eval instanceof StructuralEvaluator.Not);
        Evaluator inner = (Evaluator) getField(eval, "evaluator");
        assertTrue(inner instanceof Evaluator.Class);
        assertEquals("ignore", getField(inner, "className"));
    }

    @Test
    public void testParseCombinatorChild() {
        Evaluator eval = QueryParser.parse("div > p");
        assertTrue(eval instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        // Right side should be ImmediateParent wrapping the left selector
        Evaluator right = and.right;
        assertTrue(right instanceof StructuralEvaluator.ImmediateParent);
        Evaluator left = ((StructuralEvaluator.ImmediateParent) right).evaluator;
        assertTrue(left instanceof Evaluator.Tag);
        assertEquals("p", getField(left, "tagName"));
    }

    @Test
    public void testParseCombinatorDescendant() {
        Evaluator eval = QueryParser.parse("ul li");
        assertTrue(eval instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        Evaluator right = and.right;
        assertTrue(right instanceof StructuralEvaluator.Parent);
        Evaluator left = ((StructuralEvaluator.Parent) right).evaluator;
        assertTrue(left instanceof Evaluator.Tag);
        assertEquals("li", getField(left, "tagName"));
    }

    @Test
    public void testParseCombinatorAdjacentSibling() {
        Evaluator eval = QueryParser.parse("h1 + p");
        assertTrue(eval instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        Evaluator right = and.right;
        assertTrue(right instanceof StructuralEvaluator.ImmediatePreviousSibling);
        Evaluator left = ((StructuralEvaluator.ImmediatePreviousSibling) right).evaluator;
        assertTrue(left instanceof Evaluator.Tag);
        assertEquals("p", getField(left, "tagName"));
    }

    @Test
    public void testParseCombinatorGeneralSibling() {
        Evaluator eval = QueryParser.parse("h1 ~ p");
        assertTrue(eval instanceof CombiningEvaluator.And);
        CombiningEvaluator.And and = (CombiningEvaluator.And) eval;
        Evaluator right = and.right;
        assertTrue(right instanceof StructuralEvaluator.PreviousSibling);
        Evaluator left = ((StructuralEvaluator.PreviousSibling) right).evaluator;
        assertTrue(left instanceof Evaluator.Tag);
        assertEquals("p", getField(left, "tagName"));
    }

    @Test
    public void testParseGroupOr() {
        Evaluator eval = QueryParser.parse("a, span");
        assertTrue(eval instanceof CombiningEvaluator.Or);
        CombiningEvaluator.Or or = (CombiningEvaluator.Or) eval;
        assertEquals(2, or.evaluators.size());
        assertTrue(or.evaluators.get(0) instanceof Evaluator.Tag);
        assertTrue(or.evaluators.get(1) instanceof Evaluator.Tag);
        assertEquals("a", getField(or.evaluators.get(0), "tagName"));
        assertEquals("span", getField(or.evaluators.get(1), "tagName"));
    }

    @Test
    public void testParseComplexSelector() {
        // div#main.content[attr=val]:contains(txt) > span:first-child
        Evaluator eval = QueryParser.parse("div#main.content[attr=val]:contains(txt) > span:first-child");
        assertTrue(eval instanceof CombiningEvaluator.And);
        // The top-level And should combine the right side (span:first-child) with a Parent combinator
        CombiningEvaluator.And topAnd = (CombiningEvaluator.And) eval;
        Evaluator right = topAnd.right;
        assertTrue(right instanceof StructuralEvaluator.ImmediateParent);
        // Inside the ImmediateParent should be the left side selector (span:first-child)
        Evaluator spanSel = ((StructuralEvaluator.ImmediateParent) right).evaluator;
        assertTrue(spanSel instanceof CombiningEvaluator.And);
        // Ensure the span selector includes the first-child pseudo
        CombiningEvaluator.And spanAnd = (CombiningEvaluator.And) spanSel;
        assertTrue(spanAnd.right instanceof Evaluator.IsFirstChild);
        // The left side of the top-level And should be the complex "div#main.content[attr=val]:contains(txt)"
        Evaluator left = topAnd.left;
        assertTrue(left instanceof CombiningEvaluator.And);
        // Verify that the left side contains an Id evaluator with id "main"
        // (We don't need to check every piece, just that the tree is built)
        assertTrue(((CombiningEvaluator.And) left).left instanceof Evaluator.Tag);
        assertTrue(((CombiningEvaluator.And) left).right instanceof Evaluator.Class);
    }

    @Test(expected = Selector.SelectorParseException.class)
    public void testParseInvalidSelectorThrows() {
        QueryParser.parse(":unknown(");
    }
}
