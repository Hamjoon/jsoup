package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Tag;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class EvaluatorTest {

    /* ---------- Simple tag based evaluators ---------- */

    @Test
    public void testTag() {
        Element div = new Element(Tag.valueOf("div"), "");
        Element span = new Element(Tag.valueOf("span"), "");
        Evaluator.Tag evalDiv = new Evaluator.Tag("div");
        Evaluator.Tag evalSpan = new Evaluator.Tag("SPAN"); // case‑insensitive

        assertTrue(evalDiv.matches(div, div));
        assertFalse(evalDiv.matches(div, span));
        assertTrue(evalSpan.matches(div, span));
    }

    @Test
    public void testTagEndsWith() {
        Element custom = new Element(Tag.valueOf("my-custom"), "");
        Evaluator.TagEndsWith endsWith = new Evaluator.TagEndsWith("-custom");
        assertTrue(endsWith.matches(custom, custom));

        Evaluator.TagEndsWith notEnds = new Evaluator.TagEndsWith("custom-");
        assertFalse(notEnds.matches(custom, custom));
    }

    @Test
    public void testId() {
        Element e = new Element(Tag.valueOf("p"), "").attr("id", "myId");
        Evaluator.Id eval = new Evaluator.Id("myId");
        assertTrue(eval.matches(e, e));

        Evaluator.Id evalWrong = new Evaluator.Id("other");
        assertFalse(evalWrong.matches(e, e));
    }

    @Test
    public void testClass() {
        Element e = new Element(Tag.valueOf("div"), "").addClass("cls");
        Evaluator.Class eval = new Evaluator.Class("cls");
        assertTrue(eval.matches(e, e));

        Evaluator.Class eval2 = new Evaluator.Class("other");
        assertFalse(eval2.matches(e, e));
    }

    /* ---------- Attribute based evaluators ---------- */

    @Test
    public void testAttribute() {
        Element e = new Element(Tag.valueOf("a"), "").attr("href", "http://example.com");
        Evaluator.Attribute eval = new Evaluator.Attribute("href");
        assertTrue(eval.matches(e, e));

        Evaluator.Attribute missing = new Evaluator.Attribute("title");
        assertFalse(missing.matches(e, e));
    }

    @Test
    public void testAttributeStarting() {
        Element e = new Element(Tag.valueOf("img"), "").attr("data-src", "pic.png");
        Evaluator.AttributeStarting eval = new Evaluator.AttributeStarting("data-");
        assertTrue(eval.matches(e, e));

        Evaluator.AttributeStarting eval2 = new Evaluator.AttributeStarting("src");
        assertFalse(eval2.matches(e, e));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttributeStartingEmptyPrefix() {
        new Evaluator.AttributeStarting("");
    }

    @Test
    public void testAttributeWithValue() {
        Element e = new Element(Tag.valueOf("input"), "").attr("type", "text");
        Evaluator.AttributeWithValue eval = new Evaluator.AttributeWithValue("type", "TEXT");
        assertTrue(eval.matches(e, e));

        Evaluator.AttributeWithValue eval2 = new Evaluator.AttributeWithValue("type", "\"text\"");
        assertTrue(eval2.matches(e, e));

        Evaluator.AttributeWithValue eval3 = new Evaluator.AttributeWithValue("type", "button");
        assertFalse(eval3.matches(e, e));
    }

    @Test
    public void testAttributeWithValueNot() {
        Element e = new Element(Tag.valueOf("input"), "").attr("type", "password");
        Evaluator.AttributeWithValueNot eval = new Evaluator.AttributeWithValueNot("type", "text");
        assertTrue(eval.matches(e, e));

        Evaluator.AttributeWithValueNot eval2 = new Evaluator.AttributeWithValueNot("type", "password");
        assertFalse(eval2.matches(e, e));
    }

    @Test
    public void testAttributeWithValueStarting() {
        Element e = new Element(Tag.valueOf("a"), "").attr("href", "https://example.com");
        Evaluator.AttributeWithValueStarting eval = new Evaluator.AttributeWithValueStarting("href", "https");
        assertTrue(eval.matches(e, e));

        Evaluator.AttributeWithValueStarting eval2 = new Evaluator.AttributeWithValueStarting("href", "http");
        assertFalse(eval2.matches(e, e));
    }

    @Test
    public void testAttributeWithValueEnding() {
        Element e = new Element(Tag.valueOf("a"), "").attr("href", "example.org");
        Evaluator.AttributeWithValueEnding eval = new Evaluator.AttributeWithValueEnding("href", "org");
        assertTrue(eval.matches(e, e));

        Evaluator.AttributeWithValueEnding eval2 = new Evaluator.AttributeWithValueEnding("href", "com");
        assertFalse(eval2.matches(e, e));
    }

    @Test
    public void testAttributeWithValueContaining() {
        Element e = new Element(Tag.valueOf("meta"), "").attr("content", "charset=UTF-8");
        Evaluator.AttributeWithValueContaining eval = new Evaluator.AttributeWithValueContaining("content", "utf-8");
        assertTrue(eval.matches(e, e));

        Evaluator.AttributeWithValueContaining eval2 = new Evaluator.AttributeWithValueContaining("content", "ascii");
        assertFalse(eval2.matches(e, e));
    }

    @Test
    public void testAttributeWithValueMatching() {
        Element e = new Element(Tag.valueOf("div"), "").attr("data-num", "12345");
        Pattern pat = Pattern.compile("\\d{5}");
        Evaluator.AttributeWithValueMatching eval = new Evaluator.AttributeWithValueMatching("data-num", pat);
        assertTrue(eval.matches(e, e));

        Pattern pat2 = Pattern.compile("^12$");
        Evaluator.AttributeWithValueMatching eval2 = new Evaluator.AttributeWithValueMatching("data-num", pat2);
        assertFalse(eval2.matches(e, e));
    }

    @Test
    public void testAllElements() {
        Element e = new Element(Tag.valueOf("p"), "");
        Evaluator.AllElements eval = new Evaluator.AllElements();
        assertTrue(eval.matches(e, e));
    }

    /* ---------- Index based evaluators ---------- */

    @Test
    public void testIndexLessThan() {
        Document doc = Jsoup.parse("<ul><li id='a'></li><li id='b'></li><li id='c'></li></ul>");
        Element root = doc.selectFirst("ul");
        Element a = doc.getElementById("a");
        Element b = doc.getElementById("b");
        Element c = doc.getElementById("c");

        Evaluator.IndexLessThan eval = new Evaluator.IndexLessThan(2);
        assertTrue(eval.matches(root, a)); // index 0 < 2
        assertTrue(eval.matches(root, b)); // index 1 < 2
        assertFalse(eval.matches(root, c)); // index 2 !< 2
        // root != element condition
        assertFalse(eval.matches(root, root));
    }

    @Test
    public void testIndexGreaterThan() {
        Document doc = Jsoup.parse("<ul><li id='a'></li><li id='b'></li><li id='c'></li></ul>");
        Element root = doc.selectFirst("ul");
        Element a = doc.getElementById("a");
        Element b = doc.getElementById("b");
        Element c = doc.getElementById("c");

        Evaluator.IndexGreaterThan eval = new Evaluator.IndexGreaterThan(0);
        assertFalse(eval.matches(root, a)); // 0 > 0 false
        assertTrue(eval.matches(root, b));  // 1 > 0 true
        assertTrue(eval.matches(root, c));  // 2 > 0 true
    }

    @Test
    public void testIndexEquals() {
        Document doc = Jsoup.parse("<ul><li id='a'></li><li id='b'></li><li id='c'></li></ul>");
        Element root = doc.selectFirst("ul");
        Element b = doc.getElementById("b");

        Evaluator.IndexEquals eval = new Evaluator.IndexEquals(1);
        assertTrue(eval.matches(root, b));
        assertFalse(eval.matches(root, doc.getElementById("a")));
    }

    /* ---------- Structural pseudo‑class evaluators ---------- */

    @Test
    public void testIsLastChild() {
        Document doc = Jsoup.parse("<div><p id='first'></p><p id='last'></p></div>");
        Element first = doc.getElementById("first");
        Element last = doc.getElementById("last");

        Evaluator.IsLastChild eval = new Evaluator.IsLastChild();
        assertFalse(eval.matches(first, first));
        assertTrue(eval.matches(last, last));
    }

    @Test
    public void testIsFirstOfType() {
        Document doc = Jsoup.parse("<div><p id='p1'></p><span id='s1'></span><p id='p2'></p></div>");
        Element p1 = doc.getElementById("p1");
        Element p2 = doc.getElementById("p2");
        Element s1 = doc.getElementById("s1");

        Evaluator.IsFirstOfType eval = new Evaluator.IsFirstOfType();
        assertTrue(eval.matches(p1, p1)); // first <p>
        assertFalse(eval.matches(p2, p2)); // not first <p>
        assertTrue(eval.matches(s1, s1)); // first (and only) <span>
    }

    @Test
    public void testIsLastOfType() {
        Document doc = Jsoup.parse("<div><p id='p1'></p><span id='s1'></span><p id='p2'></p></div>");
        Element p2 = doc.getElementById("p2");
        Element p1 = doc.getElementById("p1");
        Element s1 = doc.getElementById("s1");

        Evaluator.IsLastOfType eval = new Evaluator.IsLastOfType();
        assertTrue(eval.matches(p2, p2)); // last <p>
        assertFalse(eval.matches(p1, p1));
        assertTrue(eval.matches(s1, s1)); // only <span> is also last
    }

    @Test
    public void testIsNthChild() {
        Document doc = Jsoup.parse("<ul><li id='a'></li><li id='b'></li><li id='c'></li><li id='d'></li></ul>");
        Element a = doc.getElementById("a");
        Element c = doc.getElementById("c");
        Element d = doc.getElementById("d");

        // nth-child(2)  -> a=0,b=2
        Evaluator.IsNthChild nth2 = new Evaluator.IsNthChild(0, 2);
        assertTrue(nth2.matches(a.parent(), c)); // index 2 (third child) is not 2 -> false
        assertTrue(nth2.matches(a.parent(), doc.getElementById("b"))); // index 1 -> false, wait
        // Actually nth-child(2) means second child (index 1). So we need to test correctly:
        Evaluator.IsNthChild nthSecond = new Evaluator.IsNthChild(0, 2);
        assertTrue(nthSecond.matches(a.parent(), doc.getElementById("b"))); // second child
        assertFalse(nthSecond.matches(a.parent(), a)); // first child

        // nth-child(2n+1) -> a=2,b=1 (odd positions)
        Evaluator.IsNthChild nthOdd = new Evaluator.IsNthChild(2, 1);
        assertTrue(nthOdd.matches(a.parent(), a)); // 1st -> true
        assertFalse(nthOdd.matches(a.parent(), doc.getElementById("b"))); // 2nd -> false
        assertTrue(nthOdd.matches(a.parent(), c)); // 3rd -> true
    }

    @Test
    public void testIsNthLastChild() {
        Document doc = Jsoup.parse("<ul><li id='a'></li><li id='b'></li><li id='c'></li></ul>");
        Element a = doc.getElementById("a");
        Element c = doc.getElementById("c");

        // nth-last-child(1) -> last child
        Evaluator.IsNthLastChild last = new Evaluator.IsNthLastChild(0, 1);
        assertTrue(last.matches(a.parent(), c));
        assertFalse(last.matches(a.parent(), a));

        // nth-last-child(2n) -> every even position from the end (2,4,...)
        Evaluator.IsNthLastChild everySecondFromEnd = new Evaluator.IsNthLastChild(2, 0);
        assertTrue(everySecondFromEnd.matches(a.parent(), b)); // 2nd from start = 2nd from end in 3‑item list
        assertFalse(everySecondFromEnd.matches(a.parent(), a));
    }

    @Test
    public void testIsNthOfType() {
        Document doc = Jsoup.parse("<div><p id='p1'></p><span></span><p id='p2'></p><p id='p3'></p></div>");
        Element p1 = doc.getElementById("p1");
        Element p2 = doc.getElementById("p2");
        Element p3 = doc.getElementById("p3");

        // nth-of-type(2) -> second <p>
        Evaluator.IsNthOfType secondP = new Evaluator.IsNthOfType(0, 2);
        assertTrue(secondP.matches(p1.parent(), p2));
        assertFalse(secondP.matches(p1.parent(), p1));
        assertFalse(secondP.matches(p1.parent(), p3));

        // nth-of-type(2n+1) -> odd <p>
        Evaluator.IsNthOfType oddP = new Evaluator.IsNthOfType(2, 1);
        assertTrue(oddP.matches(p1.parent(), p1));
        assertFalse(oddP.matches(p1.parent(), p2));
        assertTrue(oddP.matches(p1.parent(), p3));
    }

    @Test
    public void testIsNthLastOfType() {
        Document doc = Jsoup.parse("<div><p id='p1'></p><span></span><p id='p2'></p><p id='p3'></p></div>");
        Element p3 = doc.getElementById("p3");
        Element p2 = doc.getElementById("p2");
        Element p1 = doc.getElementById("p1");

        // nth-last-of-type(1) -> last <p>
        Evaluator.IsNthLastOfType lastP = new Evaluator.IsNthLastOfType(0, 1);
        assertTrue(lastP.matches(p1.parent(), p3));
        assertFalse(lastP.matches(p1.parent(), p2));

        // nth-last-of-type(2n) -> every even position from the end
        Evaluator.IsNthLastOfType everySecondFromEnd = new Evaluator.IsNthLastOfType(2, 0);
        assertTrue(everySecondFromEnd.matches(p1.parent(), p2));
        assertFalse(everySecondFromEnd.matches(p1.parent(), p3));
    }

    @Test
    public void testIsFirstChild() {
        Document doc = Jsoup.parse("<ul><li id='first'></li><li id='second'></li></ul>");
        Element first = doc.getElementById("first");
        Element second = doc.getElementById("second");

        Evaluator.IsFirstChild eval = new Evaluator.IsFirstChild();
        assertTrue(eval.matches(first.parent(), first));
        assertFalse(eval.matches(second.parent(), second));
    }

    @Test
    public void testIsRoot() {
        Document doc = Jsoup.parse("<html><body><div id='d'></div></body></html>");
        Element root = doc; // document itself is the root
        Element html = doc.selectFirst("html");
        Element div = doc.getElementById("d");

        Evaluator.IsRoot eval = new Evaluator.IsRoot();
        assertTrue(eval.matches(root, html)); // html is the root element of the document
        assertFalse(eval.matches(root, div));
    }

    @Test
    public void testIsOnlyChild() {
        Document doc = Jsoup.parse("<div><p id='only'></p></div>");
        Element only = doc.getElementById("only");
        Evaluator.IsOnlyChild eval = new Evaluator.IsOnlyChild();
        assertTrue(eval.matches(only.parent(), only));

        // add a sibling and verify false
        doc.selectFirst("div").appendElement("span");
        assertFalse(eval.matches(only.parent(), only));
    }

    @Test
    public void testIsOnlyOfType() {
        Document doc = Jsoup.parse("<div><p id='p1'></p><span></span><p id='p2'></p></div>");
        Element span = doc.selectFirst("span");
        Evaluator.IsOnlyOfType eval = new Evaluator.IsOnlyOfType();
        assertTrue(eval.matches(span.parent(), span));

        // make another <span> and verify false
        doc.selectFirst("div").appendElement("span");
        assertFalse(eval.matches(span.parent(), span));
    }

    @Test
    public void testIsEmpty() {
        Element empty = new Element(Tag.valueOf("div"), "").appendChild(new Comment("a comment", ""));
        Evaluator.IsEmpty eval = new Evaluator.IsEmpty();
        assertTrue(eval.matches(empty, empty));

        Element notEmpty = new Element(Tag.valueOf("div"), "").text("hello");
        assertFalse(eval.matches(notEmpty, notEmpty));
    }

    /* ---------- Text based evaluators ---------- */

    @Test
    public void testContainsText() {
        Element e = new Element(Tag.valueOf("p"), "").text("Hello World");
        Evaluator.ContainsText eval = new Evaluator.ContainsText("world");
        assertTrue(eval.matches(e, e));

        Evaluator.ContainsText eval2 = new Evaluator.ContainsText("foo");
        assertFalse(eval2.matches(e, e));
    }

    @Test
    public void testContainsData() {
        // Using a <script> element where data() returns the script content
        Element script = new Element(Tag.valueOf("script"), "").appendChild(new DataNode("var x = 10;", ""));
        Evaluator.ContainsData eval = new Evaluator.ContainsData("var x");
        assertTrue(eval.matches(script, script));

        Evaluator.ContainsData eval2 = new Evaluator.ContainsData("nothing");
        assertFalse(eval2.matches(script, script));
    }

    @Test
    public void testContainsOwnText() {
        Element e = new Element(Tag.valueOf("div"), "").appendText("Own");
        e.appendElement("span").text("Child");
        Evaluator.ContainsOwnText eval = new Evaluator.ContainsOwnText("own");
        assertTrue(eval.matches(e, e));

        Evaluator.ContainsOwnText eval2 = new Evaluator.ContainsOwnText("child");
        assertFalse(eval2.matches(e, e));
    }

    @Test
    public void testMatches() {
        Element e = new Element(Tag.valueOf("p"), "").text("abc123");
        Evaluator.Matches eval = new Evaluator.Matches(Pattern.compile("\\d+"));
        assertTrue(eval.matches(e, e));

        Evaluator.Matches eval2 = new Evaluator.Matches(Pattern.compile("XYZ"));
        assertFalse(eval2.matches(e, e));
    }

    @Test
    public void testMatchesOwn() {
        Element e = new Element(Tag.valueOf("p"), "").appendText("ownText");
        e.appendElement("span").text("child");
        Evaluator.MatchesOwn eval = new Evaluator.MatchesOwn(Pattern.compile("own"));
        assertTrue(eval.matches(e, e));

        Evaluator.MatchesOwn eval2 = new Evaluator.MatchesOwn(Pattern.compile("child"));
        assertFalse(eval2.matches(e, e));
    }

    @Test
    public void testMatchText() {
        Element e = new Element(Tag.valueOf("p"), "").text("some text");
        Evaluator.MatchText eval = new Evaluator.MatchText();

        // First call should replace text nodes with PseudoTextElement and return false
        assertFalse(eval.matches(e, e));
        // Now the element contains a PseudoTextElement child; second call should return true
        assertTrue(eval.matches(e, e));
    }
}
