###Test START##
```java
package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Tag;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Evaluator} and all of its
 * concrete subclasses.  The tests cover:
 * <ul>
 *   <li>Typical use‑cases (matching expected elements)</li>
 *   <li>Edge‑cases (case‑insensitivity, whitespace, sibling‑index boundaries)</li>
 *   <li>Error scenarios (invalid constructor arguments, null handling)</li>
 *   <li>toString() contracts for debugging output</li>
 * </ul>
 */
public class EvaluatorTest {

    private Document doc;

    /** Build a small HTML document used by many tests. */
    @Before
    public void setUp() {
        String html = ""
                + "<!DOCTYPE html>"
                + "<html>"
                + " <head><title>Test</title></head>"
                + " <body>"
                + "   <div id='root' class='container'>"
                + "     <p class='para' data-info='  FooBar  ' custom-attr='value1'>Hello <span>World</span></p>"
                + "     <p class='para' data-info='baz'>Second paragraph</p>"
                + "     <section id='sec' data-number='12345'>"
                + "       <article class='article' data-number='abc123'>Article text</article>"
                + "       <article class='article' data-number='xyz456'>Another article</article>"
                + "     </section>"
                + "     <ul>"
                + "       <li>Item 1</li>"
                + "       <li class='selected'>Item 2</li>"
                + "       <li>Item 3</li>"
                + "     </ul>"
                + "     <!-- a comment -->"
                + "     <?xml version='1.0'?>"
                + "   </div>"
                + "   <empty></empty>"
                + " </body>"
                + "</html>";
        doc = Jsoup.parse(html);
    }

    /* --------------------------------------------------------------------- */
    /*  Simple tag based evaluators                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTagEvaluator() {
        Evaluator.Tag eval = new Evaluator.Tag("p");
        Element p = doc.selectFirst("p");
        assertTrue(eval.matches(doc, p));
        assertFalse(eval.matches(doc, doc.selectFirst("div")));

        // case‑insensitive match
        Evaluator.Tag upper = new Evaluator.Tag("DIV");
        assertTrue(upper.matches(doc, doc.selectFirst("div")));

        assertEquals("p", eval.toString());
    }

    @Test
    public void testTagEndsWithEvaluator() {
        Evaluator.TagEndsWith eval = new Evaluator.TagEndsWith("icle");
        Element article = doc.selectFirst("article");
        assertTrue(eval.matches(doc, article));

        Element div = doc.selectFirst("div");
        assertFalse(eval.matches(doc, div));

        assertEquals("icle", eval.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  Id / Class evaluators                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIdEvaluator() {
        Evaluator.Id eval = new Evaluator.Id("root");
        Element div = doc.selectFirst("div#root");
        assertTrue(eval.matches(doc, div));

        Evaluator.Id missing = new Evaluator.Id("nonexistent");
        assertFalse(missing.matches(doc, div));

        assertEquals("#root", eval.toString());
    }

    @Test
    public void testClassEvaluator() {
        Evaluator.Class eval = new Evaluator.Class("selected");
        Element li = doc.selectFirst("li.selected");
        assertTrue(eval.matches(doc, li));

        Evaluator.Class missing = new Evaluator.Class("missing");
        assertFalse(missing.matches(doc, li));

        assertEquals(".selected", eval.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  Attribute based evaluators                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAttributeEvaluator() {
        Evaluator.Attribute eval = new Evaluator.Attribute("custom-attr");
        Element p = doc.selectFirst("p[custom-attr]");
        assertTrue(eval.matches(doc, p));

        Evaluator.Attribute missing = new Evaluator.Attribute("no-such-attr");
        assertFalse(missing.matches(doc, p));

        assertEquals("[custom-attr]", eval.toString());
    }

    @Test
    public void testAttributeStartingEvaluator() {
        Evaluator.AttributeStarting eval = new Evaluator.AttributeStarting("data-");
        Element p = doc.selectFirst("p[data-info]");
        assertTrue(eval.matches(doc, p));

        // attribute key prefix must be case‑insensitive
        Evaluator.AttributeStarting mixed = new Evaluator.AttributeStarting("DaTa-");
        assertTrue(mixed.matches(doc, p));

        // empty prefix should throw
        try {
            new Evaluator.AttributeStarting("");
            fail("Expected IllegalArgumentException for empty prefix");
        } catch (IllegalArgumentException e) {
            // expected
        }

        assertEquals("[^data-]", eval.toString());
    }

    @Test
    public void testAttributeWithValueEvaluator() {
        Evaluator.AttributeWithValue eval = new Evaluator.AttributeWithValue("custom-attr", "value1");
        Element p = doc.selectFirst("p[custom-attr]");
        assertTrue(eval.matches(doc, p));

        // value comparison is case‑insensitive and trimmed
        Evaluator.AttributeWithValue spaced = new Evaluator.AttributeWithValue("data-info", "\"FooBar\"");
        assertTrue(spaced.matches(doc, p));

        Evaluator.AttributeWithValue missing = new Evaluator.AttributeWithValue("custom-attr", "nope");
        assertFalse(missing.matches(doc, p));

        assertEquals("[custom-attr=value1]", eval.toString());
    }

    @Test
    public void testAttributeWithValueNotEvaluator() {
        Evaluator.AttributeWithValueNot eval = new Evaluator.AttributeWithValueNot("custom-attr", "different");
        Element p = doc.selectFirst("p[custom-attr]");
        assertTrue(eval.matches(doc, p));

        Evaluator.AttributeWithValueNot falseEval = new Evaluator.AttributeWithValueNot("custom-attr", "value1");
        assertFalse(falseEval.matches(doc, p));

        assertEquals("[custom-attr!=different]", eval.toString());
    }

    @Test
    public void testAttributeWithValueStartingEvaluator() {
        Evaluator.AttributeWithValueStarting eval = new Evaluator.AttributeWithValueStarting("data-number", "abc");
        Element article = doc.selectFirst("article.article[data-number=abc123]");
        assertTrue(eval.matches(doc, article));

        Evaluator.AttributeWithValueStarting falseEval = new Evaluator.AttributeWithValueStarting("data-number", "123");
        assertFalse(falseEval.matches(doc, article));

        assertEquals("[data-number^=abc]", eval.toString());
    }

    @Test
    public void testAttributeWithValueEndingEvaluator() {
        Evaluator.AttributeWithValueEnding eval = new Evaluator.AttributeWithValueEnding("data-number", "456");
        Element article = doc.selectFirst("article.article[data-number=xyz456]");
        assertTrue(eval.matches(doc, article));

        Evaluator.AttributeWithValueEnding falseEval = new Evaluator.AttributeWithValueEnding("data-number", "xyz");
        assertFalse(falseEval.matches(doc, article));

        assertEquals("[data-number$=456]", eval.toString());
    }

    @Test
    public void testAttributeWithValueContainingEvaluator() {
        Evaluator.AttributeWithValueContaining eval = new Evaluator.AttributeWithValueContaining("data-number", "123");
        Element article = doc.selectFirst("article.article[data-number=abc123]");
        assertTrue(eval.matches(doc, article));

        Evaluator.AttributeWithValueContaining falseEval = new Evaluator.AttributeWithValueContaining("data-number", "999");
        assertFalse(falseEval.matches(doc, article));

        assertEquals("[data-number*=123]", eval.toString());
    }

    @Test
    public void testAttributeWithValueMatchingEvaluator() {
        Pattern pattern = Pattern.compile("^abc\\d+$");
        Evaluator.AttributeWithValueMatching eval = new Evaluator.AttributeWithValueMatching("data-number", pattern);
        Element article = doc.selectFirst("article.article[data-number=abc123]");
        assertTrue(eval.matches(doc, article));

        Evaluator.AttributeWithValueMatching falseEval = new Evaluator.AttributeWithValueMatching("data-number", Pattern.compile("zzz"));
        assertFalse(falseEval.matches(doc, article));

        assertEquals("[data-number~=abc\\d+]", eval.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttributeKeyPairValidationEmptyKey() {
        // Directly instantiate a subclass to trigger validation in AttributeKeyPair
        new Evaluator.AttributeWithValue("", "value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttributeKeyPairValidationEmptyValue() {
        new Evaluator.AttributeWithValue("key", "");
    }

    /* --------------------------------------------------------------------- */
    /*  Miscellaneous evaluators                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAllElementsEvaluator() {
        Evaluator.AllElements eval = new Evaluator.AllElements();
        Element any = doc.selectFirst("article");
        assertTrue(eval.matches(doc, any));
        assertEquals("*", eval.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  Index based evaluators                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIndexLessThanEvaluator() {
        Elements lis = doc.select("ul > li");
        Evaluator.IndexLessThan eval = new Evaluator.IndexLessThan(2);
        // sibling index of first <li> is 0, second is 1, third is 2
        assertTrue(eval.matches(doc, lis.get(0)));
        assertTrue(eval.matches(doc, lis.get(1)));
        assertFalse(eval.matches(doc, lis.get(2)));

        // root == element case must be false (guard against self‑match)
        assertFalse(eval.matches(lis.get(0), lis.get(0)));

        assertEquals(":lt(2)", eval.toString());
    }

    @Test
    public void testIndexGreaterThanEvaluator() {
        Elements lis = doc.select("ul > li");
        Evaluator.IndexGreaterThan eval = new Evaluator.IndexGreaterThan(0);
        assertTrue(eval.matches(doc, lis.get(1)));
        assertTrue(eval.matches(doc, lis.get(2)));
        assertFalse(eval.matches(doc, lis.get(0)));
        assertEquals(":gt(0)", eval.toString());
    }

    @Test
    public void testIndexEqualsEvaluator() {
        Elements lis = doc.select("ul > li");
        Evaluator.IndexEquals eval = new Evaluator.IndexEquals(1);
        assertTrue(eval.matches(doc, lis.get(1)));
        assertFalse(eval.matches(doc, lis.get(0)));
        assertEquals(":eq(1)", eval.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  Positional / pseudo‑class evaluators                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsLastChildEvaluator() {
        Element lastLi = doc.select("ul > li").last();
        Evaluator.IsLastChild eval = new Evaluator.IsLastChild();
        assertTrue(eval.matches(doc, lastLi));

        Element firstLi = doc.select("ul > li").first();
        assertFalse(eval.matches(doc, firstLi));

        assertEquals(":last-child", eval.toString());
    }

    @Test
    public void testIsFirstChildEvaluator() {
        Element firstLi = doc.select("ul > li").first();
        Evaluator.IsFirstChild eval = new Evaluator.IsFirstChild();
        assertTrue(eval.matches(doc, firstLi));

        Element secondLi = doc.select("ul > li").get(1);
        assertFalse(eval.matches(doc, secondLi));

        assertEquals(":first-child", eval.toString());
    }

    @Test
    public void testIsFirstOfTypeEvaluator() {
        Evaluator.IsFirstOfType eval = new Evaluator.IsFirstOfType();
        Element firstArticle = doc.selectFirst("section > article");
        assertTrue(eval.matches(doc, firstArticle));

        // second article is same type, not first‑of‑type
        Element secondArticle = doc.select("section > article").get(1);
        assertFalse(eval.matches(doc, secondArticle));

        assertEquals(":first-of-type", eval.toString());
    }

    @Test
    public void testIsLastOfTypeEvaluator() {
        Evaluator.IsLastOfType eval = new Evaluator.IsLastOfType();
        Element lastArticle = doc.select("section > article").last();
        assertTrue(eval.matches(doc, lastArticle));

        Element firstArticle = doc.selectFirst("section > article");
        assertFalse(eval.matches(doc, firstArticle));

        assertEquals(":last-of-type", eval.toString());
    }

    @Test
    public void testIsNthChildEvaluator() {
        // 2n+1 selects 1st, 3rd, 5th … child
        Evaluator.IsNthChild eval = new Evaluator.IsNthChild(2, 1);
        Elements lis = doc.select("ul > li");
        assertTrue(eval.matches(doc, lis.get(0))); // 1st
        assertFalse(eval.matches(doc, lis.get(1))); // 2nd
        assertTrue(eval.matches(doc, lis.get(2))); // 3rd

        assertEquals(":nth-child(2n+1)", eval.toString());
    }

    @Test
    public void testIsNthLastChildEvaluator() {
        // :nth-last-child(1) == last child
        Evaluator.IsNthLastChild eval = new Evaluator.IsNthLastChild(0, 1);
        Element lastLi = doc.select("ul > li").last();
        assertTrue(eval.matches(doc, lastLi));

        Element firstLi = doc.select("ul > li").first();
        assertFalse(eval.matches(doc, firstLi));

        assertEquals(":nth-last-child(1)", eval.toString());
    }

    @Test
    public void testIsNthOfTypeEvaluator() {
        // For <li>, 2n selects every second <li> (2nd, 4th,…)
        Evaluator.IsNthOfType eval = new Evaluator.IsNthOfType(2, 0);
        Elements lis = doc.select("ul > li");
        assertFalse(eval.matches(doc, lis.get(0))); // 1st
        assertTrue(eval.matches(doc, lis.get(1)));  // 2nd
        assertFalse(eval.matches(doc, lis.get(2))); // 3rd

        assertEquals(":nth-of-type(2n)", eval.toString());
    }

    @Test
    public void testIsNthLastOfTypeEvaluator() {
        // For <li>, nth-last-of-type(1) == last <li> of its type
        Evaluator.IsNthLastOfType eval = new Evaluator.IsNthLastOfType(0, 1);
        Element lastLi = doc.select("ul > li").last();
        assertTrue(eval.matches(doc, lastLi));

        Element middleLi = doc.select("ul > li").get(1);
        assertFalse(eval.matches(doc, middleLi));

        assertEquals(":nth-last-of-type(1)", eval.toString());
    }

    @Test
    public void testIsRootEvaluator() {
        Evaluator.IsRoot eval = new Evaluator.IsRoot();
        Element rootDiv = doc.selectFirst("div#root");
        // root for the selector engine is the document element (html)
        assertTrue(eval.matches(doc, doc)); // when root is the Document itself
        assertFalse(eval.matches(doc, rootDiv));
        assertEquals(":root", eval.toString());
    }

    @Test
    public void testIsOnlyChildEvaluator() {
        // <empty></empty> has no siblings
        Element empty = doc.selectFirst("empty");
        Evaluator.IsOnlyChild eval = new Evaluator.IsOnlyChild();
        assertTrue(eval.matches(doc, empty));

        // <p> inside <div> has siblings
        Element p = doc.selectFirst("div > p");
        assertFalse(eval.matches(doc, p));

        assertEquals(":only-child", eval.toString());
    }

    @Test
    public void testIsOnlyOfTypeEvaluator() {
        // Only <section> under <div>
        Element section = doc.selectFirst("section");
        Evaluator.IsOnlyOfType eval = new Evaluator.IsOnlyOfType();
        assertTrue(eval.matches(doc, section));

        // Multiple <p> under <div> → not only‑of‑type
        Element p = doc.selectFirst("div > p");
        assertFalse(eval.matches(doc, p));

        assertEquals(":only-of-type", eval.toString());
    }

    @Test
    public void testIsEmptyEvaluator() {
        // <empty> has no child nodes → should be true
        Element empty = doc.selectFirst("empty");
        Evaluator.IsEmpty eval = new Evaluator.IsEmpty();
        assertTrue(eval.matches(doc, empty));

        // <p> contains text nodes → not empty
        Element p = doc.selectFirst("p");
        assertFalse(eval.matches(doc, p));

        // Element containing only comments / XML declaration is considered empty
        Element div = doc.selectFirst("div#root");
        // Manually add a comment node only
        div.empty();
        div.appendChild(new Comment("just a comment", ""));
        assertTrue(eval.matches(doc, div));

        assertEquals(":empty", eval.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  Text based evaluators                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testContainsTextEvaluator() {
        Evaluator.ContainsText eval = new Evaluator.ContainsText("hello world");
        Element p = doc.selectFirst("p");
        assertTrue(eval.matches(doc, p)); // text "Hello World" (case‑insensitive)

        Evaluator.ContainsText missing = new Evaluator.ContainsText("nonexistent");
        assertFalse(missing.matches(doc, p));

        assertEquals(":contains(hello world)", eval.toString());
    }

    @Test
    public void testContainsDataEvaluator() {
        // data() returns the combined data of the element (for script/style etc.)
        // Here we use a custom attribute as "data" via element.attr()
        Element p = doc.selectFirst("p[data-info]");
        p.attr("data", "some data value");
        Evaluator.ContainsData eval = new Evaluator.ContainsData("some DATA");
        assertTrue(eval.matches(doc, p));

        assertEquals(":containsData(some DATA)", eval.toString());
    }

    @Test
    public void testContainsOwnTextEvaluator() {
        Evaluator.ContainsOwnText eval = new Evaluator.ContainsOwnText("hello");
        Element p = doc.selectFirst("p");
        // ownText() of <p> is "Hello " (excluding the <span>World</span>)
        assertTrue(eval.matches(doc, p));

        Evaluator.ContainsOwnText missing = new Evaluator.ContainsOwnText("world");
        assertFalse(missing.matches(doc, p));

        assertEquals(":containsOwn(hello)", eval.toString());
    }

    @Test
    public void testMatchesEvaluator() {
        Pattern pattern = Pattern.compile("Hello\\s+World");
        Evaluator.Matches eval = new Evaluator.Matches(pattern);
        Element p = doc.selectFirst("p");
        assertTrue(eval.matches(doc, p));

        Pattern falsePattern = Pattern.compile("Goodbye");
        Evaluator.Matches falseEval = new Evaluator.Matches(falsePattern);
        assertFalse(falseEval.matches(doc, p));

        assertEquals(":matches(Hello\\s+World)", eval.toString());
    }

    @Test
    public void testMatchesOwnEvaluator() {
        Pattern pattern = Pattern.compile("^Hello\\s*$");
        Evaluator.MatchesOwn eval = new Evaluator.MatchesOwn(pattern);
        Element p = doc.selectFirst("p");
        assertTrue(eval.matches(doc, p));

        Pattern falsePattern = Pattern.compile("World$");
        Evaluator.MatchesOwn falseEval = new Evaluator.MatchesOwn(falsePattern);
        assertFalse(falseEval.matches(doc, p));

        assertEquals(":matchesOwn(^Hello\\s*$)", eval.toString());
    }

    @Test
    public void testMatchTextEvaluator() {
        // The evaluator should replace each TextNode with a PseudoTextElement
        // and return false for the first invocation (as per implementation).
        Evaluator.MatchText eval = new Evaluator.MatchText();
        Element p = doc.selectFirst("p");
        assertFalse(eval.matches(doc, p));

        // After the call, each original TextNode should have been replaced.
        // Verify that the children of <p> now include a PseudoTextElement.
        boolean containsPseudo = false;
        for (Node n : p.childNodes()) {
            if (n instanceof PseudoTextElement) {
                containsPseudo = true;
                break;
            }
        }
        assertTrue("PseudoTextElement should have been inserted", containsPseudo);
        assertEquals(":matchText", eval.toString());
    }
}
```
###Test END##