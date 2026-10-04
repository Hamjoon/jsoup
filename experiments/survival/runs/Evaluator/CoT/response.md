###Test START##
```java
package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Tag;
import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Evaluator} and all of its
 * public nested evaluator classes.
 *
 * The tests are organised in three sections:
 *   1.  Extraction of public method signatures (documented in comments).
 *   2.  Basic functionality tests for each evaluator.
 *   3.  Edge‑case / exception tests (null arguments, empty strings,
 *       case‑insensitivity, regex handling, sibling‑index rules, etc.).
 *
 * The test file is deliberately self‑contained – it builds the minimal
 * HTML DOM required for each test using Jsoup’s parsing facilities.
 */
public class EvaluatorTest {

    private Document doc;
    private Element root;
    private Element child1;
    private Element child2;
    private Element child3;
    private Element span;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // Simple DOM used by many tests
        // <html><body>
        //   <div id="root">
        //     <p class="alpha">First</p>
        //     <p class="beta" data-val="  VALUE  ">Second</p>
        //     <span>Third</span>
        //   </div>
        // </body></html>
        doc = Jsoup.parse("<div id='root'>" +
                "<p class='alpha'>First</p>" +
                "<p class='beta' data-val='  VALUE  '>Second</p>" +
                "<span>Third</span>" +
                "</div>");
        root = doc.selectFirst("div#root");
        child1 = root.selectFirst("p.alpha");
        child2 = root.selectFirst("p.beta");
        child3 = root.selectFirst("span");
        span = child3; // alias for readability
    }

    /* ----------------------------------------------------------------------
     * 1.  PUBLIC METHOD SIGNATURES (extracted from the source)
     * ----------------------------------------------------------------------
     *
     * Evaluator (abstract)
     *   public abstract boolean matches(Element root, Element element);
     *
     * Tag
     *   public Tag(String tagName)
     *   public boolean matches(Element root, Element element)
     *   public String toString()
     *
     * TagEndsWith
     *   public TagEndsWith(String tagName)
     *   public boolean matches(Element root, Element element)
     *   public String toString()
     *
     * Id
     *   public Id(String id)
     *   public boolean matches(Element root, Element element)
     *   public String toString()
     *
     * Class
     *   public Class(String className)
     *   public boolean matches(Element root, Element element)
     *   public String toString()
     *
     * Attribute
     *   public Attribute(String key)
     *   public boolean matches(Element root, Element element)
     *   public String toString()
     *
     * AttributeStarting
     *   public AttributeStarting(String keyPrefix)               // validates not empty
     *   public boolean matches(Element root, Element element)
     *   public String toString()
     *
     * AttributeWithValue
     *   public AttributeWithValue(String key, String value)
     *   public boolean matches(Element root, Element element)
     *   public String toString()
     *
     * AttributeWithValueNot
     *   public AttributeWithValueNot(String key, String value)
     *   public boolean matches(Element root, Element element)
     *   public String toString()
     *
     * AttributeWithValueStarting
     *   public AttributeWithValueStarting(String key, String value)
     *   public boolean matches(Element root, Element element)
     *   public String toString()
     *
     * AttributeWithValueEnding
     *   public AttributeWithValueEnding(String key, String value)
     *   public boolean matches(Element root, Element element)
     *   public String toString()
     *
     * AttributeWithValueContaining
     *   public AttributeWithValueContaining(String key, String value)
     *   public boolean matches(Element root, Element element)
     *   public String toString()
     *
     * AttributeWithValueMatching
     *   public AttributeWithValueMatching(String key, Pattern pattern)
     *   public boolean matches(Element root, Element element)
     *   public String toString()
     *
     * AllElements
     *   public AllElements()
     *   public boolean matches(Element root, Element element)
     *   public String toString()
     *
     * IndexLessThan
     *   public IndexLessThan(int index)
     *   public boolean matches(Element root, Element element)
     *   public String toString()
     *
     * IndexGreaterThan
     *   public IndexGreaterThan(int index)
     *   public boolean matches(Element root, Element element)
     *   public String toString()
     *
     * IndexEquals
     *   public IndexEquals(int index)
     *   public boolean matches(Element root, Element element)
     *   public String toString()
     *
     * IsLastChild, IsFirstChild, IsFirstOfType, IsLastOfType,
     * IsNthChild, IsNthLastChild, IsNthOfType, IsNthLastOfType,
     * IsRoot, IsOnlyChild, IsOnlyOfType, IsEmpty, MatchText,
     * ContainsText, ContainsData, ContainsOwnText, Matches,
     * MatchesOwn
     *   public <ClassName>()
     *   public boolean matches(Element root, Element element)
     *   public String toString()
     *
     * ----------------------------------------------------------------------
     * 2.  BASIC FUNCTIONALITY TESTS
     * ----------------------------------------------------------------------
     */

    // ----------------------------------------------------------------------
    // Tag / TagEndsWith
    // ----------------------------------------------------------------------
    @Test
    public void testTagMatchesExactly() {
        Evaluator.Tag eval = new Evaluator.Tag("p");
        assertTrue(eval.matches(root, child1));
        assertTrue(eval.matches(root, child2));
        assertFalse(eval.matches(root, span));
    }

    @Test
    public void testTagIsCaseInsensitive() {
        Evaluator.Tag eval = new Evaluator.Tag("P");
        assertTrue(eval.matches(root, child1));
    }

    @Test
    public void testTagToString() {
        Evaluator.Tag eval = new Evaluator.Tag("div");
        assertEquals("div", eval.toString());
    }

    @Test
    public void testTagEndsWithMatches() {
        Evaluator.TagEndsWith eval = new Evaluator.TagEndsWith("div");
        assertTrue(eval.matches(root, root));               // <div>
        assertFalse(eval.matches(root, child1));            // <p>
    }

    @Test
    public void testTagEndsWithToString() {
        Evaluator.TagEndsWith eval = new Evaluator.TagEndsWith("span");
        assertEquals("span", eval.toString());
    }

    // ----------------------------------------------------------------------
    // Id
    // ----------------------------------------------------------------------
    @Test
    public void testIdMatches() {
        Evaluator.Id eval = new Evaluator.Id("root");
        assertTrue(eval.matches(root, root));
        assertFalse(eval.matches(root, child1));
    }

    @Test
    public void testIdToString() {
        Evaluator.Id eval = new Evaluator.Id("myId");
        assertEquals("#myId", eval.toString());
    }

    // ----------------------------------------------------------------------
    // Class
    // ----------------------------------------------------------------------
    @Test
    public void testClassMatches() {
        Evaluator.Class eval = new Evaluator.Class("alpha");
        assertTrue(eval.matches(root, child1));
        assertFalse(eval.matches(root, child2));
    }

    @Test
    public void testClassToString() {
        Evaluator.Class eval = new Evaluator.Class("someClass");
        assertEquals(".someClass", eval.toString());
    }

    // ----------------------------------------------------------------------
    // Attribute
    // ----------------------------------------------------------------------
    @Test
    public void testAttributeExists() {
        Evaluator.Attribute eval = new Evaluator.Attribute("data-val");
        assertTrue(eval.matches(root, child2));
        assertFalse(eval.matches(root, child1));
    }

    @Test
    public void testAttributeToString() {
        Evaluator.Attribute eval = new Evaluator.Attribute("href");
        assertEquals("[href]", eval.toString());
    }

    // ----------------------------------------------------------------------
    // AttributeStarting (prefix)
    // ----------------------------------------------------------------------
    @Test
    public void testAttributeStartingMatchesPrefix() {
        Evaluator.AttributeStarting eval = new Evaluator.AttributeStarting("data-");
        assertTrue(eval.matches(root, child2));
    }

    @Test
    public void testAttributeStartingIsCaseInsensitive() {
        Evaluator.AttributeStarting eval = new Evaluator.AttributeStarting("DATA-");
        assertTrue(eval.matches(root, child2));
    }

    @Test
    public void testAttributeStartingToString() {
        Evaluator.AttributeStarting eval = new Evaluator.AttributeStarting("aria-");
        assertEquals("[^aria-]", eval.toString());
    }

    // ----------------------------------------------------------------------
    // AttributeWithValue (exact match, trimmed, case‑insensitive)
    // ----------------------------------------------------------------------
    @Test
    public void testAttributeWithValueExactMatch() {
        Evaluator.AttributeWithValue eval = new Evaluator.AttributeWithValue("data-val", "VALUE");
        assertTrue(eval.matches(root, child2));
    }

    @Test
    public void testAttributeWithValueIgnoresWhitespaceAndCase() {
        Evaluator.AttributeWithValue eval = new Evaluator.AttributeWithValue("data-val", "  value  ");
        assertTrue(eval.matches(root, child2));
    }

    @Test
    public void testAttributeWithValueToString() {
        Evaluator.AttributeWithValue eval = new Evaluator.AttributeWithValue("title", "Hello");
        assertEquals("[title=Hello]", eval.toString());
    }

    // ----------------------------------------------------------------------
    // AttributeWithValueNot
    // ----------------------------------------------------------------------
    @Test
    public void testAttributeWithValueNotMatchesWhenDifferent() {
        Evaluator.AttributeWithValueNot eval = new Evaluator.AttributeWithValueNot("data-val", "something");
        assertTrue(eval.matches(root, child2));
    }

    @Test
    public void testAttributeWithValueNotMatchesWhenSame() {
        Evaluator.AttributeWithValueNot eval = new Evaluator.AttributeWithValueNot("data-val", "VALUE");
        assertFalse(eval.matches(root, child2));
    }

    // ----------------------------------------------------------------------
    // AttributeWithValueStarting
    // ----------------------------------------------------------------------
    @Test
    public void testAttributeWithValueStartingMatchesPrefix() {
        Evaluator.AttributeWithValueStarting eval = new Evaluator.AttributeWithValueStarting("data-val", "val");
        assertTrue(eval.matches(root, child2));
    }

    @Test
    public void testAttributeWithValueStartingIsCaseInsensitive() {
        Evaluator.AttributeWithValueStarting eval = new Evaluator.AttributeWithValueStarting("data-val", "VaL");
        assertTrue(eval.matches(root, child2));
    }

    // ----------------------------------------------------------------------
    // AttributeWithValueEnding
    // ----------------------------------------------------------------------
    @Test
    public void testAttributeWithValueEndingMatchesSuffix() {
        Evaluator.AttributeWithValueEnding eval = new Evaluator.AttributeWithValueEnding("data-val", "lue");
        assertTrue(eval.matches(root, child2));
    }

    // ----------------------------------------------------------------------
    // AttributeWithValueContaining
    // ----------------------------------------------------------------------
    @Test
    public void testAttributeWithValueContainingMatchesSubstring() {
        Evaluator.AttributeWithValueContaining eval = new Evaluator.AttributeWithValueContaining("data-val", "alu");
        assertTrue(eval.matches(root, child2));
    }

    // ----------------------------------------------------------------------
    // AttributeWithValueMatching (regex)
    // ----------------------------------------------------------------------
    @Test
    public void testAttributeWithValueMatchingRegex() {
        Pattern p = Pattern.compile("^\\s*VALUE\\s*$", Pattern.CASE_INSENSITIVE);
        Evaluator.AttributeWithValueMatching eval = new Evaluator.AttributeWithValueMatching("data-val", p);
        assertTrue(eval.matches(root, child2));
    }

    @Test
    public void testAttributeWithValueMatchingToString() {
        Pattern p = Pattern.compile(".*");
        Evaluator.AttributeWithValueMatching eval = new Evaluator.AttributeWithValueMatching("href", p);
        assertEquals("[href~=" + p.toString() + "]", eval.toString());
    }

    // ----------------------------------------------------------------------
    // AllElements
    // ----------------------------------------------------------------------
    @Test
    public void testAllElementsAlwaysTrue() {
        Evaluator.AllElements eval = new Evaluator.AllElements();
        assertTrue(eval.matches(root, child1));
        assertTrue(eval.matches(root, child2));
        assertTrue(eval.matches(root, span));
    }

    @Test
    public void testAllElementsToString() {
        Evaluator.AllElements eval = new Evaluator.AllElements();
        assertEquals("*", eval.toString());
    }

    // ----------------------------------------------------------------------
    // IndexLessThan, IndexGreaterThan, IndexEquals
    // ----------------------------------------------------------------------
    @Test
    public void testIndexLessThan() {
        Evaluator.IndexLessThan eval = new Evaluator.IndexLessThan(2);
        // child1 index = 0, child2 = 1, span = 2
        assertTrue(eval.matches(root, child1));
        assertTrue(eval.matches(root, child2));
        assertFalse(eval.matches(root, span));
    }

    @Test
    public void testIndexLessThanRootEqualsElement() {
        Evaluator.IndexLessThan eval = new Evaluator.IndexLessThan(5);
        // root != element condition must be true; when we pass root as element it should be false
        assertFalse(eval.matches(root, root));
    }

    @Test
    public void testIndexGreaterThan() {
        Evaluator.IndexGreaterThan eval = new Evaluator.IndexGreaterThan(0);
        assertTrue(eval.matches(root, child2));
        assertTrue(eval.matches(root, span));
        assertFalse(eval.matches(root, child1));
    }

    @Test
    public void testIndexEquals() {
        Evaluator.IndexEquals eval = new Evaluator.IndexEquals(1);
        assertTrue(eval.matches(root, child2));
        assertFalse(eval.matches(root, child1));
        assertFalse(eval.matches(root, span));
    }

    // ----------------------------------------------------------------------
    // IsFirstChild / IsLastChild
    // ----------------------------------------------------------------------
    @Test
    public void testIsFirstChild() {
        Evaluator.IsFirstChild eval = new Evaluator.IsFirstChild();
        assertTrue(eval.matches(root, child1));
        assertFalse(eval.matches(root, child2));
    }

    @Test
    public void testIsLastChild() {
        Evaluator.IsLastChild eval = new Evaluator.IsLastChild();
        assertTrue(eval.matches(root, span));
        assertFalse(eval.matches(root, child2));
    }

    // ----------------------------------------------------------------------
    // IsRoot
    // ----------------------------------------------------------------------
    @Test
    public void testIsRootMatchesDocumentRoot() {
        Evaluator.IsRoot eval = new Evaluator.IsRoot();
        // In our document the first child of the Document is the <html> element,
        // but the evaluator is used with the element that was passed as root.
        // We call matches(root, root) – should be true.
        assertTrue(eval.matches(root, root));
        assertFalse(eval.matches(root, child1));
    }

    // ----------------------------------------------------------------------
    // IsOnlyChild
    // ----------------------------------------------------------------------
    @Test
    public void testIsOnlyChildTrue() {
        // Build a parent with a single child
        Document d = Jsoup.parse("<div id='single'><p>solo</p></div>");
        Element singleParent = d.selectFirst("div#single");
        Element solo = singleParent.selectFirst("p");
        Evaluator.IsOnlyChild eval = new Evaluator.IsOnlyChild();
        assertTrue(eval.matches(singleParent, solo));
    }

    @Test
    public void testIsOnlyChildFalseWhenMultiple() {
        Evaluator.IsOnlyChild eval = new Evaluator.IsOnlyChild();
        assertFalse(eval.matches(root, child1)); // root has three children
    }

    // ----------------------------------------------------------------------
    // IsOnlyOfType
    // ----------------------------------------------------------------------
    @Test
    public void testIsOnlyOfTypeTrue() {
        Document d = Jsoup.parse("<div><p>only p</p><span>span</span></div>");
        Element div = d.selectFirst("div");
        Element onlyP = div.selectFirst("p");
        Evaluator.IsOnlyOfType eval = new Evaluator.IsOnlyOfType();
        assertTrue(eval.matches(div, onlyP));
    }

    @Test
    public void testIsOnlyOfTypeFalseWhenMultipleSameTag() {
        Evaluator.IsOnlyOfType eval = new Evaluator.IsOnlyOfType();
        assertFalse(eval.matches(root, child1)); // two <p> siblings
    }

    // ----------------------------------------------------------------------
    // IsEmpty
    // ----------------------------------------------------------------------
    @Test
    public void testIsEmptyTrueWhenOnlyComments() {
        Document d = Jsoup.parse("<div><!-- comment --></div>");
        Element div = d.selectFirst("div");
        Evaluator.IsEmpty eval = new Evaluator.IsEmpty();
        assertTrue(eval.matches(div, div));
    }

    @Test
    public void testIsEmptyFalseWhenTextNodePresent() {
        Evaluator.IsEmpty eval = new Evaluator.IsEmpty();
        assertFalse(eval.matches(root, child1));
    }

    // ----------------------------------------------------------------------
    // ContainsText / ContainsOwnText / ContainsData
    // ----------------------------------------------------------------------
    @Test
    public void testContainsTextCaseInsensitive() {
        Evaluator.ContainsText eval = new Evaluator.ContainsText("first");
        assertTrue(eval.matches(root, child1));
    }

    @Test
    public void testContainsOwnTextMatchesOwnTextOnly() {
        Evaluator.ContainsOwnText eval = new Evaluator.ContainsOwnText("second");
        assertTrue(eval.matches(root, child2));
        // child2 contains its own text "Second"
        // child1 contains "First", but also its descendant text (none) – should be false
        assertFalse(eval.matches(root, child1));
    }

    @Test
    public void testContainsDataMatchesData() {
        Evaluator.ContainsData eval = new Evaluator.ContainsData("value");
        assertTrue(eval.matches(root, child2));
    }

    // ----------------------------------------------------------------------
    // Matches / MatchesOwn (regex)
    // ----------------------------------------------------------------------
    @Test
    public void testMatchesRegexOnFullText() {
        Pattern p = Pattern.compile("First|Second|Third");
        Evaluator.Matches eval = new Evaluator.Matches(p);
        assertTrue(eval.matches(root, child1));
        assertTrue(eval.matches(root, child2));
        assertTrue(eval.matches(root, span));
    }

    @Test
    public void testMatchesOwnRegex() {
        Pattern p = Pattern.compile("^Second$");
        Evaluator.MatchesOwn eval = new Evaluator.MatchesOwn(p);
        assertTrue(eval.matches(root, child2));
        assertFalse(eval.matches(root, child1));
    }

    // ----------------------------------------------------------------------
    // MatchText (converts TextNode to PseudoTextElement)
    // ----------------------------------------------------------------------
    @Test
    public void testMatchTextReplacesTextNode() {
        // Create a simple element with a single text node
        Document d = Jsoup.parse("<p>Hello</p>");
        Element p = d.selectFirst("p");
        assertEquals(1, p.textNodes().size());
        Evaluator.MatchText eval = new Evaluator.MatchText();
        assertFalse(eval.matches(p, p)); // the method always returns false after conversion
        // After invocation the original TextNode should have been replaced by a PseudoTextElement
        assertEquals(0, p.textNodes().size());
        assertEquals(1, p.children().size());
        assertTrue(p.child(0) instanceof PseudoTextElement);
    }

    // ----------------------------------------------------------------------
    // 3.  EDGE‑CASE / EXCEPTION TESTS
    // ----------------------------------------------------------------------
    @Test
    public void testAttributeStartingConstructorRejectsEmptyPrefix() {
        thrown.expect(IllegalArgumentException.class);
        new Evaluator.AttributeStarting("");
    }

    @Test
    public void testAttributeKeyPairConstructorRejectsEmptyKey() {
        thrown.expect(IllegalArgumentException.class);
        // Directly instantiate a concrete subclass that extends AttributeKeyPair
        new Evaluator.AttributeWithValue("", "value");
    }

    @Test
    public void testAttributeKeyPairConstructorRejectsEmptyValue() {
        thrown.expect(IllegalArgumentException.class);
        new Evaluator.AttributeWithValue("key", "");
    }

    @Test
    public void testMatchesMethodHandlesNullRoot() {
        Evaluator.Tag eval = new Evaluator.Tag("p");
        thrown.expect(NullPointerException.class);
        eval.matches(null, child1);
    }

    @Test
    public void testMatchesMethodHandlesNullElement() {
        Evaluator.Tag eval = new Evaluator.Tag("p");
        thrown.expect(NullPointerException.class);
        eval.matches(root, null);
    }

    @Test
    public void testContainsTextWithEmptySearchStringMatchesAll() {
        Evaluator.ContainsText eval = new Evaluator.ContainsText("");
        assertTrue(eval.matches(root, child1));
        assertTrue(eval.matches(root, child2));
        assertTrue(eval.matches(root, span));
    }

    @Test
    public void testIsNthChildSimpleCases() {
        Evaluator.IsNthChild eval = new Evaluator.IsNthChild(0, 2); // equivalent to :nth-child(2)
        assertTrue(eval.matches(root, child2)); // second child
        assertFalse(eval.matches(root, child1));
        assertFalse(eval.matches(root, span));
    }

    @Test
    public void testIsNthChildWithFormulaAandB() {
        // :nth-child(2n+1) should match odd positions (1,3,5,…)
        Evaluator.IsNthChild eval = new Evaluator.IsNthChild(2, 1);
        assertTrue(eval.matches(root, child1)); // index 0 -> position 1
        assertFalse(eval.matches(root, child2)); // index 1 -> position 2
        assertTrue(eval.matches(root, span));   // index 2 -> position 3
    }

    @Test
    public void testIsNthLastChild() {
        // :nth-last-child(1) matches the last child
        Evaluator.IsNthLastChild eval = new Evaluator.IsNthLastChild(0, 1);
        assertTrue(eval.matches(root, span));
        assertFalse(eval.matches(root, child2));
    }

    @Test
    public void testIsNthOfType() {
        // In our root there are two <p> elements, third child is <span>.
        // :nth-of-type(2) should match the second <p>
        Evaluator.IsNthOfType eval = new Evaluator.IsNthOfType(0, 2);
        assertTrue(eval.matches(root, child2));
        assertFalse(eval.matches(root, child1));
        assertFalse(eval.matches(root, span));
    }

    @Test
    public void testIsNthLastOfType() {
        // :nth-last-of-type(1) matches the last <p> (which is child2)
        Evaluator.IsNthLastOfType eval = new Evaluator.IsNthLastOfType(0, 1);
        assertTrue(eval.matches(root, child2));
        assertFalse(eval.matches(root, child1));
    }

    @Test
    public void testIsFirstOfTypeAndIsLastOfTypeAliases() {
        Evaluator.IsFirstOfType first = new Evaluator.IsFirstOfType();
        Evaluator.IsLastOfType last = new Evaluator.IsLastOfType();
        // First of type <p> is child1, last of type <p> is child2
        assertTrue(first.matches(root, child1));
        assertFalse(first.matches(root, child2));
        assertTrue(last.matches(root, child2));
        assertFalse(last.matches(root, child1));
    }

    @Test
    public void testToStringImplementationsAreNonNull() {
        Evaluator[] evals = new Evaluator[] {
                new Evaluator.Tag("a"),
                new Evaluator.TagEndsWith("b"),
                new Evaluator.Id("c"),
                new Evaluator.Class("d"),
                new Evaluator.Attribute("e"),
                new Evaluator.AttributeStarting("f"),
                new Evaluator.AttributeWithValue("g", "h"),
                new Evaluator.AttributeWithValueNot("i", "j"),
                new Evaluator.AttributeWithValueStarting("k", "l"),
                new Evaluator.AttributeWithValueEnding("m", "n"),
                new Evaluator.AttributeWithValueContaining("o", "p"),
                new Evaluator.AttributeWithValueMatching("q", Pattern.compile(".*")),
                new Evaluator.AllElements(),
                new Evaluator.IndexLessThan(1),
                new Evaluator.IndexGreaterThan(1),
                new Evaluator.IndexEquals(1),
                new Evaluator.IsFirstChild(),
                new Evaluator.IsLastChild(),
                new Evaluator.IsRoot(),
                new Evaluator.IsOnlyChild(),
                new Evaluator.IsOnlyOfType(),
                new Evaluator.IsEmpty(),
                new Evaluator.ContainsText("txt"),
                new Evaluator.ContainsData("data"),
                new Evaluator.ContainsOwnText("own"),
                new Evaluator.Matches(Pattern.compile(".*")),
                new Evaluator.MatchesOwn(Pattern.compile(".*")),
                new Evaluator.MatchText(),
                new Evaluator.IsNthChild(1, 0),
                new Evaluator.IsNthLastChild(1, 0),
                new Evaluator.IsNthOfType(1, 0),
                new Evaluator.IsNthLastOfType(1, 0),
                new Evaluator.IsFirstOfType(),
                new Evaluator.IsLastOfType()
        };
        for (Evaluator e : evals) {
            assertNotNull("toString() returned null for " + e.getClass().getSimpleName(), e.toString());
        }
    }
}
```
###Test END##