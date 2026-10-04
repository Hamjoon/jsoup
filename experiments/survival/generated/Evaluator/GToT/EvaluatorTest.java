/**
 * Comprehensive JUnit‑4 test suite for {@link org.jsoup.select.Evaluator}
 *
 * The tests cover:
 *   1. Extraction of every public evaluator class and its {@code matches} method.
 *   2. Normal behaviour (positive & negative matches).
 *   3. Edge‑cases such as null arguments, empty strings, case‑insensitivity,
 *      whitespace handling and malformed input that should raise exceptions.
 *
 * The test file can be compiled and run with JUnit‑4 and Jsoup‑1.17+ on the
 * class‑path.
 */
package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class EvaluatorTest {

    private Document doc;
    private Element root;          // <html>
    private Element body;          // <body>
    private Element div;           // <div id="main" class="content wrapper" data-info="Hello">
    private Element span;          // <span class="highlight">World</span>
    private Element emptyDiv;      // <div></div>

    @Before
    public void setUp() {
        String html = "<!DOCTYPE html>" +
                "<html>" +
                " <head><title>Test</title></head>" +
                " <body>" +
                "   <div id='main' class='content wrapper' data-info='Hello'>" +
                "       <p>Paragraph <span class='highlight'>World</span></p>" +
                "       <!-- comment -->" +
                "   </div>" +
                "   <div></div>" +               // emptyDiv
                " </body>" +
                "</html>";
        doc = Jsoup.parse(html);
        root = doc;                  // root for most tests
        body = doc.body();
        div = doc.selectFirst("div#main");
        span = doc.selectFirst("span.highlight");
        emptyDiv = doc.select("div").get(1);
    }

    /* --------------------------------------------------------------------- *
     *  1. Simple tag based evaluators
     * --------------------------------------------------------------------- */

    @Test
    public void testTagMatches() {
        Evaluator.Tag eval = new Evaluator.Tag("div");
        assertTrue(eval.matches(root, div));
        assertFalse(eval.matches(root, span));
    }

    @Test
    public void testTagMatchesCaseInsensitive() {
        Evaluator.Tag eval = new Evaluator.Tag("DIV");
        assertTrue(eval.matches(root, div));
    }

    @Test
    public void testTagEndsWithMatches() {
        Evaluator.TagEndsWith eval = new Evaluator.TagEndsWith("iv");
        assertTrue(eval.matches(root, div));
        assertFalse(eval.matches(root, span));
    }

    @Test
    public void testTagEndsWithCaseSensitive() {
        Evaluator.TagEndsWith eval = new Evaluator.TagEndsWith("IV");
        // endsWith is case‑sensitive
        assertFalse(eval.matches(root, div));
    }

    /* --------------------------------------------------------------------- *
     *  2. Id & class evaluators
     * --------------------------------------------------------------------- */

    @Test
    public void testIdMatches() {
        Evaluator.Id eval = new Evaluator.Id("main");
        assertTrue(eval.matches(root, div));
        assertFalse(eval.matches(root, span));
    }

    @Test
    public void testClassMatches() {
        Evaluator.Class eval = new Evaluator.Class("highlight");
        assertTrue(eval.matches(root, span));
        assertFalse(eval.matches(root, div));
    }

    /* --------------------------------------------------------------------- *
     *  3. Attribute based evaluators
     * --------------------------------------------------------------------- */

    @Test
    public void testAttributeExists() {
        Evaluator.Attribute eval = new Evaluator.Attribute("data-info");
        assertTrue(eval.matches(root, div));
        assertFalse(eval.matches(root, span));
    }

    @Test
    public void testAttributeStartingMatches() {
        Evaluator.AttributeStarting eval = new Evaluator.AttributeStarting("data-");
        assertTrue(eval.matches(root, div));
        assertFalse(eval.matches(root, span));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttributeStartingEmptyKey() {
        new Evaluator.AttributeStarting(""); // Validate.notEmpty should fire
    }

    @Test
    public void testAttributeWithValueExactMatch() {
        Evaluator.AttributeWithValue eval = new Evaluator.AttributeWithValue("data-info", "Hello");
        assertTrue(eval.matches(root, div));
        assertFalse(eval.matches(root, span));
    }

    @Test
    public void testAttributeWithValueExactMatchIgnoresCaseAndWhitespace() {
        // value stored with surrounding spaces and mixed case
        div.attr("data-info", "  HeLLo  ");
        Evaluator.AttributeWithValue eval = new Evaluator.AttributeWithValue("data-info", "\"Hello\"");
        assertTrue(eval.matches(root, div));
    }

    @Test
    public void testAttributeWithValueNot() {
        Evaluator.AttributeWithValueNot eval = new Evaluator.AttributeWithValueNot("data-info", "Bye");
        assertTrue(eval.matches(root, div)); // attribute present and not equal
    }

    @Test
    public void testAttributeWithValueStarting() {
        Evaluator.AttributeWithValueStarting eval = new Evaluator.AttributeWithValueStarting("data-info", "he");
        assertTrue(eval.matches(root, div));
    }

    @Test
    public void testAttributeWithValueEnding() {
        Evaluator.AttributeWithValueEnding eval = new Evaluator.AttributeWithValueEnding("data-info", "lo");
        assertTrue(eval.matches(root, div));
    }

    @Test
    public void testAttributeWithValueContaining() {
        Evaluator.AttributeWithValueContaining eval = new Evaluator.AttributeWithValueContaining("data-info", "ell");
        assertTrue(eval.matches(root, div));
    }

    @Test
    public void testAttributeWithValueMatchingRegex() {
        Pattern pattern = Pattern.compile("^He.*o$");
        Evaluator.AttributeWithValueMatching eval = new Evaluator.AttributeWithValueMatching("data-info", pattern);
        assertTrue(eval.matches(root, div));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttributeKeyPairEmptyKey() {
        // AttributeKeyPair is abstract, but its constructor is exercised via a concrete subclass
        new Evaluator.AttributeWithValue("", "value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttributeKeyPairEmptyValue() {
        new Evaluator.AttributeWithValue("key", "");
    }

    /* --------------------------------------------------------------------- *
     *  4. Universal evaluator
     * --------------------------------------------------------------------- */

    @Test
    public void testAllElementsAlwaysTrue() {
        Evaluator.AllElements eval = new Evaluator.AllElements();
        assertTrue(eval.matches(root, div));
        assertTrue(eval.matches(root, span));
        assertTrue(eval.matches(root, emptyDiv));
    }

    /* --------------------------------------------------------------------- *
     *  5. Index based evaluators
     * --------------------------------------------------------------------- */

    @Test
    public void testIndexLessThan() {
        // The first <div> (id=main) has sibling index 0, second emptyDiv has index 1
        Evaluator.IndexLessThan eval = new Evaluator.IndexLessThan(1);
        assertTrue(eval.matches(root, div));
        assertFalse(eval.matches(root, emptyDiv));
    }

    @Test
    public void testIndexGreaterThan() {
        Evaluator.IndexGreaterThan eval = new Evaluator.IndexGreaterThan(0);
        assertTrue(eval.matches(root, emptyDiv));
        assertFalse(eval.matches(root, div));
    }

    @Test
    public void testIndexEquals() {
        Evaluator.IndexEquals eval = new Evaluator.IndexEquals(0);
        assertTrue(eval.matches(root, div));
        assertFalse(eval.matches(root, emptyDiv));
    }

    /* --------------------------------------------------------------------- *
     *  6. Structural pseudo‑classes
     * --------------------------------------------------------------------- */

    @Test
    public void testIsFirstChild() {
        Evaluator.IsFirstChild eval = new Evaluator.IsFirstChild();
        assertTrue(eval.matches(root, body)); // <body> is first child of <html>
        assertFalse(eval.matches(root, div)); // <div> is not first child of <body>
    }

    @Test
    public void testIsLastChild() {
        Evaluator.IsLastChild eval = new Evaluator.IsLastChild();
        assertTrue(eval.matches(root, emptyDiv)); // last child of <body>
        assertFalse(eval.matches(root, div));
    }

    @Test
    public void testIsRoot() {
        Evaluator.IsRoot eval = new Evaluator.IsRoot();
        // root of the selector tree is the document itself; the first element under the document is <html>
        Element html = doc.selectFirst("html");
        assertTrue(eval.matches(doc, html));
        assertFalse(eval.matches(doc, body));
    }

    @Test
    public void testIsOnlyChild() {
        // Create a parent with a single child to test the selector
        Element parent = new Element(Tag.valueOf("section"));
        Element child = new Element(Tag.valueOf("p"));
        parent.appendChild(child);

        Evaluator.IsOnlyChild eval = new Evaluator.IsOnlyChild();
        assertTrue(eval.matches(parent, child));

        // Add another sibling – now it should be false
        parent.appendChild(new Element(Tag.valueOf("p")));
        assertFalse(eval.matches(parent, child));
    }

    @Test
    public void testIsOnlyOfType() {
        // <ul><li>A</li><li>B</li></ul>
        Element ul = new Element(Tag.valueOf("ul"));
        Element li1 = new Element(Tag.valueOf("li")).text("A");
        Element li2 = new Element(Tag.valueOf("li")).text("B");
        ul.appendChild(li1);
        ul.appendChild(li2);
        Evaluator.IsOnlyOfType eval = new Evaluator.IsOnlyOfType();

        // Neither <li> is the only of its type
        assertFalse(eval.matches(ul, li1));

        // Add a different tag – <li> is still not unique
        ul.appendChild(new Element(Tag.valueOf("p")));
        assertFalse(eval.matches(ul, li1));

        // New structure where a type appears only once
        Element onlyDiv = new Element(Tag.valueOf("div"));
        ul.appendChild(onlyDiv);
        assertTrue(eval.matches(ul, onlyDiv));
    }

    @Test
    public void testIsEmpty() {
        Evaluator.IsEmpty eval = new Evaluator.IsEmpty();

        // emptyDiv has no child nodes → true
        assertTrue(eval.matches(root, emptyDiv));

        // div contains a <p> element → false
        assertFalse(eval.matches(root, div));

        // Element containing only comments should be considered empty
        Element commentOnly = new Element(Tag.valueOf("section"));
        commentOnly.appendChild(new Comment("just a comment", ""));
        assertTrue(eval.matches(root, commentOnly));
    }

    /* --------------------------------------------------------------------- *
     *  7. Text based evaluators
     * --------------------------------------------------------------------- */

    @Test
    public void testContainsText() {
        Evaluator.ContainsText eval = new Evaluator.ContainsText("world");
        assertTrue(eval.matches(root, div)); // "World" appears in descendant <span>
        assertFalse(eval.matches(root, emptyDiv));
    }

    @Test
    public void testContainsOwnText() {
        Evaluator.ContainsOwnText eval = new Evaluator.ContainsOwnText("Paragraph");
        // <p> element's own text is "Paragraph " (the span text is not own text)
        Element p = doc.selectFirst("p");
        assertTrue(eval.matches(root, p));
        assertFalse(eval.matches(root, div));
    }

    @Test
    public void testContainsData() {
        // data() returns the combined data of all child TextNodes (including script/style)
        Evaluator.ContainsData eval = new Evaluator.ContainsData("Hello");
        assertTrue(eval.matches(root, div));
    }

    @Test
    public void testMatchesRegex() {
        Evaluator.Matches eval = new Evaluator.Matches(Pattern.compile("World$"));
        assertTrue(eval.matches(root, div)); // text() of div ends with "World"
    }

    @Test
    public void testMatchesOwnRegex() {
        Evaluator.MatchesOwn eval = new Evaluator.MatchesOwn(Pattern.compile("^Paragraph"));
        Element p = doc.selectFirst("p");
        assertTrue(eval.matches(root, p));
    }

    /* --------------------------------------------------------------------- *
     *  8. Pseudo‑text handling (MatchText)
     * --------------------------------------------------------------------- */

    @Test
    public void testMatchTextReplacesTextNodesWithPseudoTextElement() {
        Evaluator.MatchText eval = new Evaluator.MatchText();

        // Before evaluation the <p> has a TextNode and a <span>
        Element p = doc.selectFirst("p");
        assertEquals(2, p.childNodeSize()); // TextNode + Element

        // The evaluator returns false (it never matches) but performs replacement
        assertFalse(eval.matches(root, p));

        // After evaluation the original TextNode should be wrapped in a PseudoTextElement
        assertEquals(2, p.childNodeSize()); // still two children
        assertTrue(p.childNode(0) instanceof PseudoTextElement);
        assertTrue(p.childNode(1) instanceof Element); // the <span>
    }

    /* --------------------------------------------------------------------- *
     *  9. Null argument safety (should throw NPE)
     * --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testTagMatchesNullElement() {
        new Evaluator.Tag("div").matches(root, null);
    }

    @Test(expected = NullPointerException.class)
    public void testTagMatchesNullRoot() {
        new Evaluator.Tag("div").matches(null, div);
    }

    /* --------------------------------------------------------------------- *
     *  10. toString() sanity checks (useful for debugging)
     * --------------------------------------------------------------------- */

    @Test
    public void testToStringOutputsExpectedFormat() {
        assertEquals("div", new Evaluator.Tag("div").toString());
        assertEquals("#main", new Evaluator.Id("main").toString());
        assertEquals(".highlight", new Evaluator.Class("highlight").toString());
        assertEquals("[data-info]", new Evaluator.Attribute("data-info").toString());
        assertEquals("[^data-]", new Evaluator.AttributeStarting("data-").toString());
        assertEquals("[data-info=Hello]", new Evaluator.AttributeWithValue("data-info", "Hello").toString());
        assertEquals("[data-info!=Bye]", new Evaluator.AttributeWithValueNot("data-info", "Bye").toString());
        assertEquals("[data-info^=he]", new Evaluator.AttributeWithValueStarting("data-info", "he").toString());
        assertEquals("[data-info$=lo]", new Evaluator.AttributeWithValueEnding("data-info", "lo").toString());
        assertEquals("[data-info*=%s]".replace("%s", "ell"), new Evaluator.AttributeWithValueContaining("data-info", "ell").toString());
        assertEquals("*", new Evaluator.AllElements().toString());
        assertEquals(":lt(1)", new Evaluator.IndexLessThan(1).toString());
        assertEquals(":gt(0)", new Evaluator.IndexGreaterThan(0).toString());
        assertEquals(":eq(0)", new Evaluator.IndexEquals(0).toString());
        assertEquals(":first-child", new Evaluator.IsFirstChild().toString());
        assertEquals(":last-child", new Evaluator.IsLastChild().toString());
        assertEquals(":root", new Evaluator.IsRoot().toString());
    }
}
