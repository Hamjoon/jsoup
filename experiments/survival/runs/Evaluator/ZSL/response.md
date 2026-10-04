###Test START##
package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Tag;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class EvaluatorTest {

    /** Helper to parse a small HTML snippet */
    private Document parse(String html) {
        return Jsoup.parse(html);
    }

    @Test
    public void testTagEvaluator() {
        Element div = new Element(Tag.valueOf("DIV"), "");
        Evaluator.Tag eval = new Evaluator.Tag("div");
        assertTrue(eval.matches(div, div));
        assertEquals("div", eval.toString());

        Evaluator.Tag evalUpper = new Evaluator.Tag("DIV");
        assertTrue(evalUpper.matches(div, div));
    }

    @Test
    public void testTagEndsWithEvaluator() {
        Element custom = new Element(Tag.valueOf("my-custom-tag"), "");
        Evaluator.TagEndsWith eval = new Evaluator.TagEndsWith("-tag");
        assertTrue(eval.matches(custom, custom));
        assertFalse(eval.matches(custom, new Element(Tag.valueOf("other"), "")));
        assertEquals("-tag", eval.toString());
    }

    @Test
    public void testIdEvaluator() {
        Element el = new Element(Tag.valueOf("p"), "").attr("id", "myId");
        Evaluator.Id eval = new Evaluator.Id("myId");
        assertTrue(eval.matches(el, el));
        assertFalse(eval.matches(el, new Element(Tag.valueOf("p"), "")));
        assertEquals("#myId", eval.toString());
    }

    @Test
    public void testClassEvaluator() {
        Element el = new Element(Tag.valueOf("span"), "").addClass("highlight");
        Evaluator.Class eval = new Evaluator.Class("highlight");
        assertTrue(eval.matches(el, el));
        assertFalse(eval.matches(el, new Element(Tag.valueOf("span"), "")));
        assertEquals(".highlight", eval.toString());
    }

    @Test
    public void testAttributeEvaluator() {
        Element el = new Element(Tag.valueOf("a"), "").attr("href", "http://example.com");
        Evaluator.Attribute eval = new Evaluator.Attribute("href");
        assertTrue(eval.matches(el, el));
        assertFalse(eval.matches(el, new Element(Tag.valueOf("a"), "")));
        assertEquals("[href]", eval.toString());
    }

    @Test
    public void testAttributeStartingEvaluator() {
        Element el = new Element(Tag.valueOf("div"), "").attr("data-value", "123");
        Evaluator.AttributeStarting eval = new Evaluator.AttributeStarting("data-");
        assertTrue(eval.matches(el, el));

        Evaluator.AttributeStarting evalNeg = new Evaluator.AttributeStarting("src");
        assertFalse(evalNeg.matches(el, el));
        assertEquals("[^data-]", evalNeg.toString());
    }

    @Test
    public void testAttributeWithValueEvaluator() {
        Element el = new Element(Tag.valueOf("input"), "").attr("type", "text");
        Evaluator.AttributeWithValue eval = new Evaluator.AttributeWithValue("type", "text");
        assertTrue(eval.matches(el, el));

        // case‑insensitive match and trimming
        el.attr("type", "  TeXt  ");
        assertTrue(eval.matches(el, el));

        assertEquals("[type=text]", eval.toString());
    }

    @Test
    public void testAttributeWithValueNotEvaluator() {
        Element el = new Element(Tag.valueOf("input"), "").attr("type", "password");
        Evaluator.AttributeWithValueNot eval = new Evaluator.AttributeWithValueNot("type", "text");
        assertTrue(eval.matches(el, el));
        assertEquals("[type!=text]", eval.toString());
    }

    @Test
    public void testAttributeWithValueStartingEvaluator() {
        Element el = new Element(Tag.valueOf("img"), "").attr("src", "http://example.com/image.png");
        Evaluator.AttributeWithValueStarting eval = new Evaluator.AttributeWithValueStarting("src", "http://example.com");
        assertTrue(eval.matches(el, el));
        assertEquals("[src^=http://example.com]", eval.toString());
    }

    @Test
    public void testAttributeWithValueEndingEvaluator() {
        Element el = new Element(Tag.valueOf("a"), "").attr("href", "page.html");
        Evaluator.AttributeWithValueEnding eval = new Evaluator.AttributeWithValueEnding("href", ".html");
        assertTrue(eval.matches(el, el));
        assertEquals("[href$=.html]", eval.toString());
    }

    @Test
    public void testAttributeWithValueContainingEvaluator() {
        Element el = new Element(Tag.valueOf("div"), "").attr("class", "my-special-class");
        Evaluator.AttributeWithValueContaining eval = new Evaluator.AttributeWithValueContaining("class", "special");
        assertTrue(eval.matches(el, el));
        assertEquals("[class*=special]", eval.toString());
    }

    @Test
    public void testAttributeWithValueMatchingEvaluator() {
        Element el = new Element(Tag.valueOf("p"), "").attr("data-num", "12345");
        Pattern pattern = Pattern.compile("^\\d{5}$");
        Evaluator.AttributeWithValueMatching eval = new Evaluator.AttributeWithValueMatching("data-num", pattern);
        assertTrue(eval.matches(el, el));
        assertEquals("[data-num~=^\\d{5}$]", eval.toString());
    }

    @Test
    public void testAllElementsEvaluator() {
        Element el = new Element(Tag.valueOf("div"), "");
        Evaluator.AllElements eval = new Evaluator.AllElements();
        assertTrue(eval.matches(el, el));
        assertEquals("*", eval.toString());
    }

    @Test
    public void testIndexEvaluators() {
        Document doc = parse("<div><p id='a'></p><p id='b'></p><p id='c'></p></div>");
        Element a = doc.selectFirst("#a");
        Element b = doc.selectFirst("#b");
        Element c = doc.selectFirst("#c");

        Evaluator.IndexLessThan lt = new Evaluator.IndexLessThan(2);
        assertTrue(lt.matches(a, b)); // b index 1 < 2
        assertFalse(lt.matches(a, c)); // c index 2 !< 2

        Evaluator.IndexGreaterThan gt = new Evaluator.IndexGreaterThan(0);
        assertTrue(gt.matches(a, b)); // index 1 > 0
        assertTrue(gt.matches(a, c)); // index 2 > 0
        assertFalse(gt.matches(a, a)); // index 0 > 0 false

        Evaluator.IndexEquals eq = new Evaluator.IndexEquals(0);
        assertTrue(eq.matches(a, a));
        assertFalse(eq.matches(a, b));
        assertEquals(":eq(0)", eq.toString());
    }

    @Test
    public void testFirstAndLastChildEvaluators() {
        Document doc = parse("<ul><li id='first'></li><li id='mid'></li><li id='last'></li></ul>");
        Element first = doc.selectFirst("#first");
        Element mid = doc.selectFirst("#mid");
        Element last = doc.selectFirst("#last");

        Evaluator.IsFirstChild firstEval = new Evaluator.IsFirstChild();
        assertTrue(firstEval.matches(first, first));
        assertFalse(firstEval.matches(first, mid));

        Evaluator.IsLastChild lastEval = new Evaluator.IsLastChild();
        assertTrue(lastEval.matches(last, last));
        assertFalse(lastEval.matches(last, mid));

        assertEquals(":first-child", firstEval.toString());
        assertEquals(":last-child", lastEval.toString());
    }

    @Test
    public void testRootEvaluator() {
        Document doc = parse("<html><body><div id='root'></div></body></html>");
        Element rootDiv = doc.getElementById("root");
        // According to implementation, root is the first child of the document (the <html> element)
        Evaluator.IsRoot eval = new Evaluator.IsRoot();
        assertTrue(eval.matches(doc.child(0), doc.child(0))); // html element is root
        assertFalse(eval.matches(doc.child(0), rootDiv));
        assertEquals(":root", eval.toString());
    }

    @Test
    public void testOnlyChildAndOnlyOfTypeEvaluators() {
        Document doc = parse("<div><p id='only'></p></div>");
        Element only = doc.selectFirst("#only");
        Evaluator.IsOnlyChild onlyChildEval = new Evaluator.IsOnlyChild();
        assertTrue(onlyChildEval.matches(only, only));

        Document doc2 = parse("<div><p class='a'></p><p class='b'></p><span></span></div>");
        Element pA = doc2.selectFirst("p.a");
        Evaluator.IsOnlyOfType onlyOfTypeEval = new Evaluator.IsOnlyOfType();
        assertFalse(onlyOfTypeEval.matches(pA, pA)); // two <p> siblings

        Document doc3 = parse("<div><p class='single'></p></div>");
        Element singleP = doc3.selectFirst("p.single");
        assertTrue(onlyOfTypeEval.matches(singleP, singleP));
    }

    @Test
    public void testEmptyEvaluator() {
        Document doc = parse("<div><!-- comment --><!-- another --></div>");
        Element div = doc.selectFirst("div");
        Evaluator.IsEmpty emptyEval = new Evaluator.IsEmpty();
        assertTrue(emptyEval.matches(div, div));

        div.appendElement("span").text("not empty");
        assertFalse(emptyEval.matches(div, div));
    }

    @Test
    public void testContainsTextEvaluators() {
        Document doc = parse("<p>Hello <b>World</b></p>");
        Element p = doc.selectFirst("p");
        Evaluator.ContainsText contains = new Evaluator.ContainsText("hello world");
        assertTrue(contains.matches(p, p));

        Evaluator.ContainsOwnText own = new Evaluator.ContainsOwnText("hello");
        assertTrue(own.matches(p, p));

        Evaluator.ContainsData dataEval = new Evaluator.ContainsData("var");
        Document scriptDoc = parse("<script>var x = 10;</script>");
        Element script = scriptDoc.selectFirst("script");
        assertTrue(dataEval.matches(script, script));
    }

    @Test
    public void testMatchesEvaluators() {
        Document doc = parse("<p>Foo123Bar</p>");
        Element p = doc.selectFirst("p");
        Evaluator.Matches matches = new Evaluator.Matches(Pattern.compile("\\d{3}"));
        assertTrue(matches.matches(p, p));

        Evaluator.MatchesOwn matchesOwn = new Evaluator.MatchesOwn(Pattern.compile("Foo\\d+"));
        assertTrue(matchesOwn.matches(p, p));
    }

    @Test
    public void testNthChildEvaluators() {
        Document doc = parse("<ul><li id='a'></li><li id='b'></li><li id='c'></li></ul>");
        Element a = doc.selectFirst("#a");
        Element b = doc.selectFirst("#b");
        Element c = doc.selectFirst("#c");

        // nth-child(2) => second child (b)
        Evaluator.IsNthChild nth2 = new Evaluator.IsNthChild(0, 2);
        assertTrue(nth2.matches(a, b));
        assertFalse(nth2.matches(a, a));

        // nth-last-child(1) => last child (c)
        Evaluator.IsNthLastChild nthLast1 = new Evaluator.IsNthLastChild(0, 1);
        assertTrue(nthLast1.matches(a, c));

        // nth-of-type(2) => second <li>
        Evaluator.IsNthOfType nthOfType2 = new Evaluator.IsNthOfType(0, 2);
        assertTrue(nthOfType2.matches(a, b));

        // nth-last-of-type(1) => last <li>
        Evaluator.IsNthLastOfType nthLastOfType1 = new Evaluator.IsNthLastOfType(0, 1);
        assertTrue(nthLastOfType1.matches(a, c));
    }

    @Test
    public void testFirstAndLastOfTypeEvaluators() {
        Document doc = parse("<div><p id='first'></p><span></span><p id='second'></p></div>");
        Element firstP = doc.selectFirst("#first");
        Element secondP = doc.selectFirst("#second");

        Evaluator.IsFirstOfType firstOfType = new Evaluator.IsFirstOfType();
        assertTrue(firstOfType.matches(firstP, firstP));
        assertFalse(firstOfType.matches(firstP, secondP));

        Evaluator.IsLastOfType lastOfType = new Evaluator.IsLastOfType();
        assertTrue(lastOfType.matches(secondP, secondP));
        assertFalse(lastOfType.matches(secondP, firstP));
    }

    @Test
    public void testMatchTextEvaluator() {
        Document doc = parse("<p>Some <b>bold</b> text</p>");
        Element p = doc.selectFirst("p");

        Evaluator.MatchText matchText = new Evaluator.MatchText();

        // Not a PseudoTextElement => should return false and replace text nodes
        assertFalse(matchText.matches(p, p));
        // After matching, the first child should be a PseudoTextElement
        assertTrue(p.childNode(0) instanceof PseudoTextElement);

        // When the element itself is a PseudoTextElement, matches returns true
        PseudoTextElement pseudo = new PseudoTextElement(Tag.valueOf("span"), "", new Attributes());
        assertTrue(matchText.matches(pseudo, pseudo));
    }

    @Test
    public void testToStringRepresentations() {
        assertEquals(":first-child", new Evaluator.IsFirstChild().toString());
        assertEquals(":last-child", new Evaluator.IsLastChild().toString());
        assertEquals(":root", new Evaluator.IsRoot().toString());
        assertEquals(":only-child", new Evaluator.IsOnlyChild().toString());
        assertEquals(":only-of-type", new Evaluator.IsOnlyOfType().toString());
        assertEquals(":empty", new Evaluator.IsEmpty().toString());
        assertEquals(":contains(hello)", new Evaluator.ContainsText("hello").toString());
        assertEquals(":containsData(data)", new Evaluator.ContainsData("data").toString());
        assertEquals(":containsOwn(own)", new Evaluator.ContainsOwnText("own").toString());
        assertEquals(":matches(java.util.regex.Pattern: java.util.regex.Pattern)", new Evaluator.Matches(Pattern.compile("x")).toString());
        assertEquals(":matchesOwn(java.util.regex.Pattern: java.util.regex.Pattern)", new Evaluator.MatchesOwn(Pattern.compile("y")).toString());
        assertEquals(":matchText", new Evaluator.MatchText().toString());
    }
}
###Test END##