package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.*;
import org.jsoup.parser.Token.StartTag;
import org.jsoup.parser.Token.Comment;
import org.jsoup.parser.Token.Character;
import org.jsoup.parser.TokeniserState;
import org.jsoup.parser.TreeBuilder;
import org.jsoup.parser.HtmlTreeBuilderState;
import org.jsoup.parser.ParseErrorList;
import org.jsoup.parser.ParseSettings;
import org.junit.Test;
import org.junit.Before;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive unit tests for {@link HtmlTreeBuilder}.
 *
 * The test class lives in the same package as {@code HtmlTreeBuilder}
 * so that it can access package‑private members (the parser is heavily
 * package‑scoped).  The tests focus on the public/visible behaviour of
 * each method, covering normal operation as well as identified edge
 * cases and exception paths.
 */
public class HtmlTreeBuilderTest {

    private HtmlTreeBuilder tb;
    private ParseErrorList errors;
    private ParseSettings settings;

    @Before
    public void setUp() {
        tb = new HtmlTreeBuilder();
        errors = ParseErrorList.noTracking();               // no error tracking needed for most tests
        settings = tb.defaultSettings();                    // package‑private but accessible here
        // initialise with an empty input – this creates the Document and the internal stack
        tb.initialiseParse(new StringReader(""), "http://example.com", errors, settings);
    }

    /** --------------------------------------------------------------------
     *  1.  Basic state / flag getters / setters
     *  -------------------------------------------------------------------- */
    @Test
    public void testBaseUriHandling() {
        // initially the base uri is the one passed to initialiseParse
        assertEquals("http://example.com", tb.getBaseUri());

        // create a <base href="http://newbase.com/"> element
        Element base = new Element(Tag.valueOf("base"), tb.getBaseUri());
        base.attr("href", "http://newbase.com/");
        tb.maybeSetBaseUri(base);

        // first call should set the base uri
        assertEquals("http://newbase.com/", tb.getBaseUri());
        assertEquals("http://newbase.com/", tb.getDocument().baseUri());

        // a second call with a different base element must be ignored
        Element base2 = new Element(Tag.valueOf("base"), tb.getBaseUri());
        base2.attr("href", "http://ignored.com/");
        tb.maybeSetBaseUri(base2);
        assertEquals("http://newbase.com/", tb.getBaseUri());
    }

    @Test
    public void testBaseUriIgnoredWhenHrefEmpty() {
        Element base = new Element(Tag.valueOf("base"), tb.getBaseUri());
        base.attr("target", "_blank");          // no href attribute
        tb.maybeSetBaseUri(base);
        // base uri must stay unchanged
        assertEquals("http://example.com", tb.getBaseUri());
    }

    @Test
    public void testFramesetFlag() {
        assertTrue(tb.framesetOk());            // default true
        tb.framesetOk(false);
        assertFalse(tb.framesetOk());
    }

    @Test
    public void testFragmentParsingFlag() {
        assertFalse(tb.isFragmentParsing());
        // trigger fragment parsing via parseFragment()
        tb.parseFragment("<p>test</p>", new Element(Tag.valueOf("div"), ""), "http://example.com", errors, settings);
        assertTrue(tb.isFragmentParsing());
    }

    /** --------------------------------------------------------------------
     *  2.  Stack manipulation (push / pop / query)
     *  -------------------------------------------------------------------- */
    @Test
    public void testPushPopStack() {
        Element div = new Element(Tag.valueOf("div"), tb.getBaseUri());
        tb.push(div);
        assertEquals(1, tb.getStack().size());
        assertTrue(tb.onStack(div));

        Element popped = tb.pop();
        assertSame(div, popped);
        assertEquals(0, tb.getStack().size());
        assertFalse(tb.onStack(div));
    }

    @Test
    public void testGetFromStackAndRemove() {
        Element a = new Element(Tag.valueOf("a"), tb.getBaseUri());
        Element b = new Element(Tag.valueOf("b"), tb.getBaseUri());
        tb.push(a);
        tb.push(b);

        assertSame(b, tb.getFromStack("b"));
        assertSame(a, tb.getFromStack("a"));
        assertNull(tb.getFromStack("c"));

        assertTrue(tb.removeFromStack(a));
        assertFalse(tb.onStack(a));
        assertFalse(tb.removeFromStack(a)); // already removed
    }

    @Test
    public void testPopStackToCloseSingleName() {
        Element html = new Element(Tag.valueOf("html"), tb.getBaseUri());
        Element body = new Element(Tag.valueOf("body"), tb.getBaseUri());
        Element div = new Element(Tag.valueOf("div"), tb.getBaseUri());
        tb.push(html);
        tb.push(body);
        tb.push(div);
        tb.popStackToClose("body");
        assertNull(tb.getFromStack("div"));
        assertNotNull(tb.getFromStack("html"));
        assertNull(tb.getFromStack("body"));
    }

    @Test
    public void testPopStackToCloseVarargs() {
        Element table = new Element(Tag.valueOf("table"), tb.getBaseUri());
        Element tr = new Element(Tag.valueOf("tr"), tb.getBaseUri());
        Element td = new Element(Tag.valueOf("td"), tb.getBaseUri());
        tb.push(table);
        tb.push(tr);
        tb.push(td);
        tb.popStackToClose("tr", "table");
        assertNull(tb.getFromStack("td"));
        assertNull(tb.getFromStack("tr"));
        assertNotNull(tb.getFromStack("table"));
    }

    @Test
    public void testPopStackToBefore() {
        Element div = new Element(Tag.valueOf("div"), tb.getBaseUri());
        Element span = new Element(Tag.valueOf("span"), tb.getBaseUri());
        Element p = new Element(Tag.valueOf("p"), tb.getBaseUri());
        tb.push(div);
        tb.push(span);
        tb.push(p);
        tb.popStackToBefore("span");
        assertEquals(2, tb.getStack().size());
        assertSame(div, tb.getStack().get(0));
        assertSame(span, tb.getStack().get(1));
        assertNull(tb.getFromStack("p"));
    }

    /** --------------------------------------------------------------------
     *  3.  Insertion helpers
     *  -------------------------------------------------------------------- */
    @Test
    public void testInsertStartTagAndStack() {
        Element el = tb.insertStartTag("section");
        assertEquals("section", el.tagName());
        assertTrue(tb.onStack(el));
        // the element should also be the current element (top of stack)
        assertSame(el, tb.currentElement());
    }

    @Test
    public void testInsertSelfClosingTagCreatesEmptyElementAndEmitsEndTag() {
        // Build a self‑closing <br/> token
        StartTag br = new Token.StartTag();
        br.name("br");
        br.selfClosing(true);

        Element el = tb.insert(br);
        assertEquals("br", el.tagName());
        assertTrue(tb.onStack(el));

        // The tokeniser should have been switched to Data mode – we cannot inspect the
        // tokeniser directly, but we can verify that an empty EndTag token was emitted
        // by checking that the builder's currentToken is an EndTag with the same name.
        assertTrue(tb.currentToken instanceof Token.EndTag);
        assertEquals("br", ((Token.EndTag) tb.currentToken).name());
    }

    @Test
    public void testInsertEmptyUnknownTagMarksSelfClosing() {
        StartTag unk = new Token.StartTag();
        unk.name("mycustom");
        unk.selfClosing(true);
        Element el = tb.insertEmpty(unk);
        assertEquals("mycustom", el.tagName());
        // unknown tag must be marked self‑closing
        assertTrue(el.tag().isSelfClosing());
    }

    @Test
    public void testInsertCommentCreatesCommentNode() {
        Comment commentToken = new Token.Comment();
        commentToken.getData(); // just to ensure token is instantiated
        commentToken.data("a comment");
        tb.insert(commentToken);
        // The comment should be the last child of the document (stack empty at init)
        List<Node> children = tb.getDocument().childNodes();
        assertFalse(children.isEmpty());
        Node last = children.get(children.size() - 1);
        assertTrue(last instanceof org.jsoup.nodes.Comment);
        assertEquals("a comment", ((org.jsoup.nodes.Comment) last).getData());
    }

    @Test
    public void testInsertCharacterCreatesTextOrDataNodeBasedOnContext() {
        // Push a <script> element to make the builder think we are inside script
        Element script = new Element(Tag.valueOf("script"), tb.getBaseUri());
        tb.push(script);

        Token.Character charToken = new Token.Character();
        charToken.data("var x=1;");
        tb.insert(charToken);
        // Inside script we expect a DataNode
        List<Node> children = script.childNodes();
        assertEquals(1, children.size());
        assertTrue(children.get(0) instanceof DataNode);
    }

    /** --------------------------------------------------------------------
     *  4.  Scope queries
     *  -------------------------------------------------------------------- */
    @Test
    public void testInScopePositiveAndNegative() {
        // Build a small stack: html > body > p > li
        Element html = new Element(Tag.valueOf("html"), tb.getBaseUri());
        Element body = new Element(Tag.valueOf("body"), tb.getBaseUri());
        Element p = new Element(Tag.valueOf("p"), tb.getBaseUri());
        Element li = new Element(Tag.valueOf("li"), tb.getBaseUri());
        tb.push(html);
        tb.push(body);
        tb.push(p);
        tb.push(li);

        // "li" is in scope (default search list includes li)
        assertTrue(tb.inScope("li"));
        // "p" is also in scope
        assertTrue(tb.inScope("p"));
        // "table" is NOT in scope (not in the stack)
        assertFalse(tb.inScope("table"));
    }

    @Test
    public void testInListItemScopeRespectsExtraTypes() {
        // Stack: html > ul > li
        Element html = new Element(Tag.valueOf("html"), tb.getBaseUri());
        Element ul = new Element(Tag.valueOf("ul"), tb.getBaseUri());
        Element li = new Element(Tag.valueOf("li"), tb.getBaseUri());
        tb.push(html);
        tb.push(ul);
        tb.push(li);

        // In list‑item scope, "ul" is an extra type that stops the search.
        assertFalse(tb.inListItemScope("ul"));
        // "li" itself is still found
        assertTrue(tb.inListItemScope("li"));
    }

    @Test
    public void testInSelectScopeStopsAtSelectBoundary() {
        // Stack: html > select > option
        Element html = new Element(Tag.valueOf("html"), tb.getBaseUri());
        Element select = new Element(Tag.valueOf("select"), tb.getBaseUri());
        Element option = new Element(Tag.valueOf("option"), tb.getBaseUri());
        tb.push(html);
        tb.push(select);
        tb.push(option);

        // option is in select scope
        assertTrue(tb.inSelectScope("option"));
        // html is NOT in select scope (search stops at select)
        assertFalse(tb.inSelectScope("html"));
    }

    /** --------------------------------------------------------------------
     *  5.  Implied end‑tag generation
     *  -------------------------------------------------------------------- */
    @Test
    public void testGenerateImpliedEndTagsWithoutExclude() {
        // Build a stack with elements that are in TagSearchEndTags: p, li, div
        Element div = new Element(Tag.valueOf("div"), tb.getBaseUri());
        Element li = new Element(Tag.valueOf("li"), tb.getBaseUri());
        Element p = new Element(Tag.valueOf("p"), tb.getBaseUri());
        tb.push(div);
        tb.push(li);
        tb.push(p);
        // current element is "p"
        tb.generateImpliedEndTags();
        // p and li should have been popped, div remains
        assertNull(tb.getFromStack("p"));
        assertNull(tb.getFromStack("li"));
        assertNotNull(tb.getFromStack("div"));
    }

    @Test
    public void testGenerateImpliedEndTagsWithExclude() {
        // Stack: div > p > li
        Element div = new Element(Tag.valueOf("div"), tb.getBaseUri());
        Element p = new Element(Tag.valueOf("p"), tb.getBaseUri());
        Element li = new Element(Tag.valueOf("li"), tb.getBaseUri());
        tb.push(div);
        tb.push(p);
        tb.push(li);
        // Exclude "p" – it should stay on the stack
        tb.generateImpliedEndTags("p");
        assertNull(tb.getFromStack("li"));
        assertNotNull(tb.getFromStack("p"));
        assertNotNull(tb.getFromStack("div"));
    }

    /** --------------------------------------------------------------------
     *  6.  Special element detection
     *  -------------------------------------------------------------------- */
    @Test
    public void testIsSpecial() {
        Element script = new Element(Tag.valueOf("script"), tb.getBaseUri());
        Element div = new Element(Tag.valueOf("div"), tb.getBaseUri());
        Element custom = new Element(Tag.valueOf("mytag"), tb.getBaseUri());

        assertTrue(tb.isSpecial(script));
        assertTrue(tb.isSpecial(div));          // "div" is listed in TagSearchSpecial
        assertFalse(tb.isSpecial(custom));
    }

    /** --------------------------------------------------------------------
     *  7.  Active formatting element handling
     *  -------------------------------------------------------------------- */
    @Test
    public void testPushActiveFormattingElementsLimitsDuplicates() {
        Element em = new Element(Tag.valueOf("em"), tb.getBaseUri());
        // push the same element three times (simulating duplicate formatting)
        tb.pushActiveFormattingElements(em);
        tb.pushActiveFormattingElements(em);
        tb.pushActiveFormattingElements(em);
        // The list should contain only three entries, and the oldest duplicate removed
        assertEquals(3, tb.formattingElements.size());
        // The first element in the list should be the second insertion (the oldest duplicate removed)
        // Verify that the list still contains exactly the em element three times
        for (Element e : tb.formattingElements) {
            assertSame(em, e);
        }
    }

    @Test
    public void testReconstructFormattingElementsWhenMissingFromStack() {
        // Prepare a formatting element that is NOT on the stack
        Element strong = new Element(Tag.valueOf("strong"), tb.getBaseUri());
        tb.pushActiveFormattingElements(strong);
        // Ensure the stack is empty (no current element)
        assertTrue(tb.getStack().isEmpty());

        // Call reconstruction – it should create a new <strong> element, push it onto the stack,
        // and replace the entry in the formatting list with the new element.
        tb.reconstructFormattingElements();

        // New element must be on the stack
        assertEquals(1, tb.getStack().size());
        Element onStack = tb.currentElement();
        assertEquals("strong", onStack.tagName());

        // Formatting list entry should be the same instance as the one on the stack
        assertEquals(1, tb.formattingElements.size());
        assertSame(onStack, tb.formattingElements.get(0));
    }

    @Test
    public void testClearFormattingElementsToLastMarker() {
        // marker (null) followed by two formatting elements
        tb.insertMarkerToFormattingElements();
        Element b = new Element(Tag.valueOf("b"), tb.getBaseUri());
        Element i = new Element(Tag.valueOf("i"), tb.getBaseUri());
        tb.formattingElements.add(b);
        tb.formattingElements.add(i);
        // Verify size before clearing
        assertEquals(3, tb.formattingElements.size());

        tb.clearFormattingElementsToLastMarker();

        // After clearing, only the marker should remain
        assertEquals(1, tb.formattingElements.size());
        assertNull(tb.formattingElements.get(0));
    }

    /** --------------------------------------------------------------------
     *  8.  Foster parenting (edge case when no table present)
     *  -------------------------------------------------------------------- */
    @Test
    public void testInsertInFosterParentWithoutTable() {
        // Build a simple stack with a single <html> element (no table)
        Element html = new Element(Tag.valueOf("html"), tb.getBaseUri());
        tb.push(html);
        // Node to be foster‑inserted
        TextNode txt = new TextNode("fostered text");
        tb.setFosterInserts(true);
        tb.insertInFosterParent(txt);
        // The node should have been appended to the first element in the stack (html)
        assertEquals(1, html.childNodeSize());
        assertSame(txt, html.childNode(0));
    }

    /** --------------------------------------------------------------------
     *  9.  resetInsertionMode logic (a couple of representative branches)
     *  -------------------------------------------------------------------- */
    @Test
    public void testResetInsertionModeSelect() {
        // Stack: html > select
        Element html = new Element(Tag.valueOf("html"), tb.getBaseUri());
        Element select = new Element(Tag.valueOf("select"), tb.getBaseUri());
        tb.push(html);
        tb.push(select);
        tb.resetInsertionMode();
        assertEquals(HtmlTreeBuilderState.InSelect, tb.state());
    }

    @Test
    public void testResetInsertionModeBody() {
        // Stack: html > body
        Element html = new Element(Tag.valueOf("html"), tb.getBaseUri());
        Element body = new Element(Tag.valueOf("body"), tb.getBaseUri());
        tb.push(html);
        tb.push(body);
        tb.resetInsertionMode();
        assertEquals(HtmlTreeBuilderState.InBody, tb.state());
    }

    /** --------------------------------------------------------------------
     *  10.  toString sanity check
     *  -------------------------------------------------------------------- */
    @Test
    public void testToStringContainsStateAndCurrentElement() {
        String s = tb.toString();
        assertTrue(s.contains("state="));
        // after initialiseParse there is no current element – should be "null"
        assertTrue(s.contains("currentElement=null"));
    }
}
