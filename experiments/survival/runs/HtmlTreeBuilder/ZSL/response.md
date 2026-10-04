###Test START##
package org.jsoup.parser;

import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.List;

/**
 * Comprehensive JUnit4 tests for {@link HtmlTreeBuilder}.
 * The tests exercise public and package‑private behaviour by using the
 * {@code parseFragment} entry point to initialise the builder and then
 * calling the various manipulation methods directly.
 */
public class HtmlTreeBuilderTest {

    private HtmlTreeBuilder tb;
    private Document doc;

    @Before
    public void setUp() {
        tb = new HtmlTreeBuilder();
        // Initialise the builder with a minimal fragment – this creates the Document,
        // the stack (with an <html> root) and sets the default settings.
        List<Node> nodes = tb.parseFragment("", null, "", new ParseErrorList(0), ParseSettings.htmlDefault);
        doc = tb.getDocument(); // the document created by initialiseParse
        // sanity check that the stack has at least the root element
        assertNotNull(doc);
        assertEquals(1, tb.getStack().size());
        assertEquals("html", tb.currentElement().tagName());
    }

    // -----------------------------------------------------------------------
    // Simple state handling
    // -----------------------------------------------------------------------
    @Test
    public void testStateTransitions() {
        tb.transition(HtmlTreeBuilderState.InBody);
        assertEquals(HtmlTreeBuilderState.InBody, tb.state());

        tb.markInsertionMode();
        tb.transition(HtmlTreeBuilderState.InTable);
        assertEquals(HtmlTreeBuilderState.InTable, tb.state());

        tb.transition(tb.originalState());
        assertEquals(HtmlTreeBuilderState.InBody, tb.state());
    }

    @Test
    public void testFramesetFlag() {
        assertTrue(tb.framesetOk()); // default true
        tb.framesetOk(false);
        assertFalse(tb.framesetOk());
    }

    @Test
    public void testFragmentParsingFlag() {
        // parseFragment with a non‑null context forces fragment mode
        Element context = new Element(Tag.valueOf("div"), "");
        tb.parseFragment("<p>hi</p>", context, "", new ParseErrorList(0), ParseSettings.htmlDefault);
        assertTrue(tb.isFragmentParsing());

        // reset with null context – not a fragment
        tb = new HtmlTreeBuilder();
        tb.parseFragment("", null, "", new ParseErrorList(0), ParseSettings.htmlDefault);
        assertFalse(tb.isFragmentParsing());
    }

    // -----------------------------------------------------------------------
    // Base URI handling
    // -----------------------------------------------------------------------
    @Test
    public void testMaybeSetBaseUri() {
        Element base = new Element(Tag.valueOf("base"), "http://example.com/");
        base.attr("href", "http://example.org/");
        tb.maybeSetBaseUri(base);
        assertEquals("http://example.org/", tb.getBaseUri());
        assertEquals("http://example.org/", doc.baseUri());

        // second call must be ignored
        Element base2 = new Element(Tag.valueOf("base"), "http://example.com/");
        base2.attr("href", "http://example.net/");
        tb.maybeSetBaseUri(base2);
        assertEquals("http://example.org/", tb.getBaseUri());
    }

    // -----------------------------------------------------------------------
    // Stack manipulation
    // -----------------------------------------------------------------------
    @Test
    public void testPushPopAndOnStack() {
        Element div = new Element(Tag.valueOf("div"), "");
        tb.push(div);
        assertTrue(tb.onStack(div));
        assertEquals(div, tb.currentElement());

        Element popped = tb.pop();
        assertEquals(div, popped);
        assertFalse(tb.onStack(div));
    }

    @Test
    public void testGetFromAndRemoveFromStack() {
        Element body = new Element(Tag.valueOf("body"), "");
        tb.push(body);
        Element head = new Element(Tag.valueOf("head"), "");
        tb.push(head);

        assertEquals(head, tb.getFromStack("head"));
        assertEquals(body, tb.getFromStack("body"));

        assertTrue(tb.removeFromStack(head));
        assertNull(tb.getFromStack("head"));
        assertFalse(tb.onStack(head));
    }

    @Test
    public void testPopStackToCloseSingleAndMultiple() {
        tb.push(new Element(Tag.valueOf("div"), ""));
        tb.push(new Element(Tag.valueOf("p"), ""));
        tb.push(new Element(Tag.valueOf("span"), ""));

        tb.popStackToClose("p");
        assertNull(tb.getFromStack("p"));
        assertNotNull(tb.getFromStack("div"));

        // multiple names (sorted array)
        tb.push(new Element(Tag.valueOf("li"), ""));
        tb.push(new Element(Tag.valueOf("ul"), ""));
        tb.popStackToClose("li", "ul");
        assertNull(tb.getFromStack("li"));
        assertNull(tb.getFromStack("ul"));
        assertNotNull(tb.getFromStack("div"));
    }

    @Test
    public void testPopStackToBefore() {
        tb.push(new Element(Tag.valueOf("div"), ""));
        tb.push(new Element(Tag.valueOf("p"), ""));
        tb.push(new Element(Tag.valueOf("span"), ""));

        tb.popStackToBefore("p");
        assertNull(tb.getFromStack("span"));
        assertNotNull(tb.getFromStack("p"));
        assertNotNull(tb.getFromStack("div"));
    }

    @Test
    public void testClearStackToTableContexts() {
        tb.push(new Element(Tag.valueOf("html"), ""));
        tb.push(new Element(Tag.valueOf("body"), ""));
        tb.push(new Element(Tag.valueOf("table"), ""));
        tb.push(new Element(Tag.valueOf("tbody"), ""));
        tb.push(new Element(Tag.valueOf("tr"), ""));
        tb.push(new Element(Tag.valueOf("td"), ""));

        tb.clearStackToTableContext();
        assertNotNull(tb.getFromStack("table"));
        assertNull(tb.getFromStack("tbody"));
        assertNull(tb.getFromStack("tr"));
        assertNull(tb.getFromStack("td"));

        tb.clearStackToTableBodyContext();
        // after previous clear, only html, body, table remain – body is not a table‑body tag,
        // so nothing is removed
        assertNotNull(tb.getFromStack("table"));

        tb.clearStackToTableRowContext();
        // no row context present, nothing removed
        assertNotNull(tb.getFromStack("table"));
    }

    @Test
    public void testAboveOnStack() {
        Element html = tb.currentElement(); // <html>
        Element body = new Element(Tag.valueOf("body"), "");
        tb.push(body);
        Element div = new Element(Tag.valueOf("div"), "");
        tb.push(div);

        assertEquals(body, tb.aboveOnStack(div));
        assertEquals(html, tb.aboveOnStack(body));
    }

    @Test
    public void testInsertOnStackAfterAndReplaceOnStack() {
        Element html = tb.currentElement();
        Element head = new Element(Tag.valueOf("head"), "");
        tb.push(head);
        Element body = new Element(Tag.valueOf("body"), "");
        tb.push(body);

        Element title = new Element(Tag.valueOf("title"), "");
        tb.insertOnStackAfter(head, title);

        // title should be after head, before body
        List<Element> stack = tb.getStack();
        assertEquals(head, stack.get(1));
        assertEquals(title, stack.get(2));
        assertEquals(body, stack.get(3));

        Element newBody = new Element(Tag.valueOf("body"), "");
        tb.replaceOnStack(body, newBody);
        assertEquals(newBody, tb.getFromStack("body"));
    }

    // -----------------------------------------------------------------------
    // Insertion helpers
    // -----------------------------------------------------------------------
    @Test
    public void testInsertStartTagAndEmpty() {
        Token.StartTag start = new Token.StartTag();
        start.name("p");
        Element el = tb.insert(start);
        assertEquals("p", el.tagName());
        assertTrue(tb.onStack(el));

        // self‑closing tag should become empty and emit a fake end tag
        Token.StartTag br = new Token.StartTag();
        br.name("br");
        br.selfClosing(true);
        Element brEl = tb.insert(br);
        assertEquals("br", brEl.tagName());
        assertTrue(tb.onStack(brEl));
        // The empty element should have been added; the emitted end tag is processed internally,
        // we just verify that the element exists on the stack.
    }

    @Test
    public void testInsertForm() {
        Token.StartTag formTag = new Token.StartTag();
        formTag.name("form");
        FormElement form = tb.insertForm(formTag, true);
        assertNotNull(form);
        assertTrue(tb.onStack(form));
        assertEquals(form, tb.getFormElement());
    }

    @Test
    public void testInsertCommentAndCharacter() {
        Token.Comment comment = new Token.Comment();
        comment.data("hello");
        tb.insert(comment);
        assertTrue(doc.childNodeSize() > 0);
        assertTrue(doc.childNode(0) instanceof Comment);
        assertEquals("hello", ((Comment) doc.childNode(0)).getData());

        Token.Character ch = new Token.Character();
        ch.data("text");
        // current element is <html>
        tb.insert(ch);
        assertEquals(1, tb.currentElement().childNodeSize());
        assertTrue(tb.currentElement().childNode(0) instanceof TextNode);
        assertEquals("text", ((TextNode) tb.currentElement().childNode(0)).text());
    }

    @Test
    public void testInsertNodeFosterInserts() {
        // set up a table context so foster insertion can be exercised
        tb.push(new Element(Tag.valueOf("html"), ""));
        tb.push(new Element(Tag.valueOf("body"), ""));
        tb.push(new Element(Tag.valueOf("table"), ""));
        tb.setFosterInserts(true);

        Token.Character ch = new Token.Character();
        ch.data("fostered");
        tb.insert(ch); // should go to foster parent (the body)

        Element body = tb.getFromStack("body");
        assertEquals(1, body.childNodeSize());
        assertTrue(body.childNode(0) instanceof TextNode);
        assertEquals("fostered", ((TextNode) body.childNode(0)).text());
    }

    // -----------------------------------------------------------------------
    // Scope checks
    // -----------------------------------------------------------------------
    @Test
    public void testInScopeVariations() {
        // Build a stack with html > body > div > p
        tb.push(new Element(Tag.valueOf("body"), ""));
        tb.push(new Element(Tag.valueOf("div"), ""));
        tb.push(new Element(Tag.valueOf("p"), ""));

        assertTrue(tb.inScope("p"));
        assertTrue(tb.inScope("div"));
        assertTrue(tb.inScope("body"));
        assertTrue(tb.inScope("html")); // always true

        // list‑item scope
        assertTrue(tb.inListItemScope("p"));
        // button scope (no button on stack)
        assertFalse(tb.inButtonScope("button"));
    }

    @Test
    public void testInTableAndSelectScope() {
        tb.push(new Element(Tag.valueOf("html"), ""));
        tb.push(new Element(Tag.valueOf("body"), ""));
        tb.push(new Element(Tag.valueOf("table"), ""));
        tb.push(new Element(Tag.valueOf("tr"), ""));
        tb.push(new Element(Tag.valueOf("td"), ""));

        assertTrue(tb.inTableScope("table"));
        assertTrue(tb.inTableScope("tr"));
        assertTrue(tb.inTableScope("td"));
        assertFalse(tb.inTableScope("body"));

        tb = new HtmlTreeBuilder();
        tb.parseFragment("", null, "", new ParseErrorList(0), ParseSettings.htmlDefault);
        tb.push(new Element(Tag.valueOf("select"), ""));
        tb.push(new Element(Tag.valueOf("option"), ""));
        assertTrue(tb.inSelectScope("option"));
        assertFalse(tb.inSelectScope("div"));
    }

    // -----------------------------------------------------------------------
    // Implied end tags generation
    // -----------------------------------------------------------------------
    @Test
    public void testGenerateImpliedEndTags() {
        // Stack: html > body > p > li > p
        tb.push(new Element(Tag.valueOf("body"), ""));
        tb.push(new Element(Tag.valueOf("p"), ""));
        tb.push(new Element(Tag.valueOf("li"), ""));
        tb.push(new Element(Tag.valueOf("p"), "")); // current

        // generate implied end tags (no exclude)
        tb.generateImpliedEndTags();
        // The algorithm should pop p, li, and p (since they are in TagSearchEndTags)
        assertNull(tb.getFromStack("p"));
        assertNull(tb.getFromStack("li"));
        // body should still be present
        assertNotNull(tb.getFromStack("body"));
    }

    @Test
    public void testGenerateImpliedEndTagsWithExclude() {
        tb.push(new Element(Tag.valueOf("body"), ""));
        tb.push(new Element(Tag.valueOf("p"), ""));
        tb.push(new Element(Tag.valueOf("li"), ""));
        tb.push(new Element(Tag.valueOf("p"), "")); // current

        tb.generateImpliedEndTags("li"); // exclude li
        // current p should be popped, li stays, previous p stays
        assertNull(tb.getFromStack("p")); // topmost p popped
        assertNotNull(tb.getFromStack("li"));
        assertNotNull(tb.getFromStack("p")); // the earlier p still there
    }

    // -----------------------------------------------------------------------
    // Special element detection
    // -----------------------------------------------------------------------
    @Test
    public void testIsSpecial() {
        Element div = new Element(Tag.valueOf("div"), "");
        assertFalse(tb.isSpecial(div));

        Element img = new Element(Tag.valueOf("img"), "");
        assertTrue(tb.isSpecial(img));

        Element script = new Element(Tag.valueOf("script"), "");
        assertTrue(tb.isSpecial(script));
    }

    // -----------------------------------------------------------------------
    // Active formatting elements handling
    // -----------------------------------------------------------------------
    @Test
    public void testPushActiveFormattingElementsAndReconstruct() {
        // Create three formatting elements with same tag/attributes
        Element b1 = new Element(Tag.valueOf("b"), "");
        Element b2 = new Element(Tag.valueOf("b"), "");
        Element i = new Element(Tag.valueOf("i"), "");

        tb.pushActiveFormattingElements(b1);
        tb.pushActiveFormattingElements(b2);
        tb.pushActiveFormattingElements(i);

        // The list should contain b1, b2, i (no markers)
        assertEquals(3, tb.formattingElements.size());

        // Now push a duplicate of b1 three times – the third push should evict the oldest
        tb.pushActiveFormattingElements(b1);
        tb.pushActiveFormattingElements(b1);
        tb.pushActiveFormattingElements(b1); // this should cause removal of the first b1

        // Verify that the list still has at most 3 b’s (the earlier one removed)
        int bCount = 0;
        for (Element el : tb.formattingElements) {
            if (el != null && el.tagName().equals("b")) bCount++;
        }
        assertEquals(3, bCount); // max three b's allowed

        // Reconstruct formatting elements when none are on stack
        // Ensure stack only has <html> root
        while (tb.getStack().size() > 1) tb.pop();

        tb.reconstructFormattingElements(); // should create missing elements on stack
        // The last formatting element (i) should now be on the stack
        assertTrue(tb.onStack(i));
        // The order on stack should be html -> b -> b -> i (or similar)
        assertEquals("i", tb.currentElement().tagName());
    }

    @Test
    public void testFormattingElementMarkersAndClear() {
        tb.insertMarkerToFormattingElements();
        tb.pushActiveFormattingElements(new Element(Tag.valueOf("b"), ""));
        tb.pushActiveFormattingElements(new Element(Tag.valueOf("i"), ""));
        // List now: marker, b, i
        assertEquals(3, tb.formattingElements.size());

        tb.clearFormattingElementsToLastMarker();
        // All elements after the marker should be removed, leaving only the marker
        assertEquals(1, tb.formattingElements.size());
        assertNull(tb.formattingElements.get(0)); // the marker itself
    }

    @Test
    public void testActiveFormattingElementQueries() {
        Element b = new Element(Tag.valueOf("b"), "");
        Element i = new Element(Tag.valueOf("i"), "");
        tb.pushActiveFormattingElements(b);
        tb.pushActiveFormattingElements(i);

        assertTrue(tb.isInActiveFormattingElements(i));
        assertEquals(i, tb.getActiveFormattingElement("i"));
        assertNull(tb.getActiveFormattingElement("u"));

        tb.removeFromActiveFormattingElements(i);
        assertFalse(tb.isInActiveFormattingElements(i));
    }

    @Test
    public void testReplaceActiveFormattingElement() {
        Element b1 = new Element(Tag.valueOf("b"), "");
        Element b2 = new Element(Tag.valueOf("b"), "");
        tb.pushActiveFormattingElements(b1);
        tb.replaceActiveFormattingElement(b1, b2);
        assertTrue(tb.isInActiveFormattingElements(b2));
        assertFalse(tb.isInActiveFormattingElements(b1));
    }

    // -----------------------------------------------------------------------
    // Reset insertion mode logic
    // -----------------------------------------------------------------------
    @Test
    public void testResetInsertionMode() {
        // Build a stack: html > head > body > table > tr > td
        tb.push(new Element(Tag.valueOf("head"), ""));
        tb.push(new Element(Tag.valueOf("body"), ""));
        tb.push(new Element(Tag.valueOf("table"), ""));
        tb.push(new Element(Tag.valueOf("tr"), ""));
        tb.push(new Element(Tag.valueOf("td"), ""));

        tb.resetInsertionMode();
        // The deepest relevant element is <td>, which should set state to InCell
        assertEquals(HtmlTreeBuilderState.InCell, tb.state());

        // Remove td and test again – should become InRow
        tb.pop();
        tb.resetInsertionMode();
        assertEquals(HtmlTreeBuilderState.InRow, tb.state());

        // Remove tr and test again – should become InTable
        tb.pop();
        tb.resetInsertionMode();
        assertEquals(HtmlTreeBuilderState.InTable, tb.state());

        // Remove table – should become InBody (because <body> is present)
        tb.pop();
        tb.resetInsertionMode();
        assertEquals(HtmlTreeBuilderState.InBody, tb.state());
    }

    // -----------------------------------------------------------------------
    // InsertInFosterParent edge cases
    // -----------------------------------------------------------------------
    @Test
    public void testInsertInFosterParentWhenNoTable() {
        // No table on stack – foster parent should be the root (<html>)
        tb.setFosterInserts(true);
        Token.Character ch = new Token.Character();
        ch.data("fostered");
        tb.insert(ch);
        Element html = tb.getFromStack("html");
        assertEquals(1, html.childNodeSize());
        assertEquals("fostered", ((TextNode) html.childNode(0)).text());
    }

    // -----------------------------------------------------------------------
    // toString sanity check (does not assert but ensures no exception)
    // -----------------------------------------------------------------------
    @Test
    public void testToString() {
        String s = tb.toString();
        assertNotNull(s);
        assertTrue(s.contains("state="));
    }
}
###Test END##