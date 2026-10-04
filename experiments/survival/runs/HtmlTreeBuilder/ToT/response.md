###Test START##
```java
package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link HtmlTreeBuilder}.
 *
 * The tests cover:
 * <ul>
 *   <li>state management (transition, markInsertionMode, etc.)</li>
 *   <li>stack manipulation (push, pop, getFromStack, etc.)</li>
 *   <li>scope checks (inScope, inSelectScope, inTableScope, …)</li>
 *   <li>element insertion helpers (insert, insertEmpty, insertForm, insertStartTag, …)</li>
 *   <li>formatting‑elements handling (pushActiveFormattingElements, reconstructFormattingElements, …)</li>
 *   <li>special‑element detection, implied‑end‑tag generation and foster‑parent insertion</li>
 *   <li>error handling and base‑uri handling</li>
 * </ul>
 *
 * The test class lives in the same package as {@code HtmlTreeBuilder} so that package‑private
 * members can be accessed directly.
 */
public class HtmlTreeBuilderTest {

    private HtmlTreeBuilder builder;
    private ParseErrorList errors;

    @Before
    public void setUp() {
        builder = new HtmlTreeBuilder();
        errors = ParseErrorList.tracking(10);
        // initialise with an empty input – we will manipulate the builder directly in most tests
        builder.initialiseParse(new StringReader(""), "http://example.com", errors, ParseSettings.htmlDefault);
    }

    /*** -----------------------------------------------------------------------
     *  Basic state / configuration helpers
     *  --------------------------------------------------------------------- */

    @Test
    public void defaultSettings_isHtmlDefault() {
        assertSame(ParseSettings.htmlDefault, builder.defaultSettings());
    }

    @Test
    public void transition_andStateTracking() {
        builder.transition(HtmlTreeBuilderState.InBody);
        assertEquals(HtmlTreeBuilderState.InBody, builder.state());

        builder.markInsertionMode();
        builder.transition(HtmlTreeBuilderState.InTable);
        assertEquals(HtmlTreeBuilderState.InTable, builder.state());

        // originalState should still be the state that was marked
        assertEquals(HtmlTreeBuilderState.InBody, builder.originalState());
    }

    @Test
    public void framesetOkFlag() {
        assertTrue(builder.framesetOk());
        builder.framesetOk(false);
        assertFalse(builder.framesetOk());
    }

    @Test
    public void maybeSetBaseUri_respectsFirstBaseOnly() {
        Element base1 = new Element(Tag.valueOf("base"), "http://example.com");
        base1.attr("href", "http://first.com");
        builder.maybeSetBaseUri(base1);
        assertEquals("http://first.com", builder.getBaseUri());

        // second <base> must be ignored
        Element base2 = new Element(Tag.valueOf("base"), "http://example.com");
        base2.attr("href", "http://second.com");
        builder.maybeSetBaseUri(base2);
        assertEquals("http://first.com", builder.getBaseUri());
    }

    @Test
    public void errorAddsParseErrorWhenAllowed() {
        // force a token that will generate an error
        Token.StartTag token = new Token.StartTag();
        token.name("bogus");
        builder.error(HtmlTreeBuilderState.InBody);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getErrorMessage().contains("Unexpected token"));
    }

    /*** -----------------------------------------------------------------------
     *  Stack manipulation
     *  --------------------------------------------------------------------- */

    @Test
    public void pushPopAndOnStackBehaviour() {
        Element div = new Element(Tag.valueOf("div"), "http://example.com");
        builder.push(div);
        assertTrue(builder.onStack(div));
        assertEquals(div, builder.pop());
        assertFalse(builder.onStack(div));
    }

    @Test
    public void getFromStack_andRemoveFromStack() {
        Element a = new Element(Tag.valueOf("a"), "http://example.com");
        Element b = new Element(Tag.valueOf("b"), "http://example.com");
        builder.push(a);
        builder.push(b);
        assertEquals(b, builder.getFromStack("b"));
        assertTrue(builder.removeFromStack(a));
        assertNull(builder.getFromStack("a"));
    }

    @Test
    public void popStackToCloseSingleAndMultipleNames() {
        // build stack: <html><body><div><p>
        builder.push(new Element(Tag.valueOf("html"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("body"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("div"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("p"), "http://example.com"));

        // pop to close "div"
        builder.popStackToClose("div");
        assertNull(builder.getFromStack("div"));
        assertNotNull(builder.getFromStack("body"));

        // push again and pop using var‑args (close on first match)
        builder.push(new Element(Tag.valueOf("div"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("ul"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("li"), "http://example.com"));
        builder.popStackToClose("ul", "li"); // should stop at "ul"
        assertNull(builder.getFromStack("ul"));
        assertNotNull(builder.getFromStack("div"));
    }

    @Test
    public void clearStackToTableContexts() {
        // stack: html > body > table > tbody > tr > td
        builder.push(new Element(Tag.valueOf("html"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("body"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("table"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("tbody"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("tr"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("td"), "http://example.com"));

        // clear to table body context, should leave table, tbody, html, body
        builder.clearStackToTableBodyContext();
        assertNull(builder.getFromStack("tr"));
        assertNull(builder.getFromStack("td"));
        assertNotNull(builder.getFromStack("tbody"));
        assertNotNull(builder.getFromStack("table"));
    }

    @Test
    public void aboveOnStack_andInsertOnStackAfter() {
        Element html = new Element(Tag.valueOf("html"), "http://example.com");
        Element body = new Element(Tag.valueOf("body"), "http://example.com");
        Element div = new Element(Tag.valueOf("div"), "http://example.com");
        builder.push(html);
        builder.push(body);
        builder.push(div);

        // aboveOnStack(div) should be body
        assertEquals(body, builder.aboveOnStack(div));

        // insert a new element after body
        Element span = new Element(Tag.valueOf("span"), "http://example.com");
        builder.insertOnStackAfter(body, span);
        // order should be: html, body, span, div
        List<Element> stack = builder.getStack();
        assertEquals(html, stack.get(0));
        assertEquals(body, stack.get(1));
        assertEquals(span, stack.get(2));
        assertEquals(div, stack.get(3));
    }

    @Test
    public void replaceOnStack() {
        Element p1 = new Element(Tag.valueOf("p"), "http://example.com");
        Element p2 = new Element(Tag.valueOf("p"), "http://example.com");
        Element strong = new Element(Tag.valueOf("strong"), "http://example.com");
        builder.push(p1);
        builder.push(p2);
        builder.replaceOnStack(p2, strong);
        assertEquals(strong, builder.getFromStack("strong"));
        assertNull(builder.getFromStack("p"));
    }

    /*** -----------------------------------------------------------------------
     *  Scope checks
     *  --------------------------------------------------------------------- */

    @Test
    public void inScope_basic() {
        // stack: html > body > div > p
        builder.push(new Element(Tag.valueOf("html"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("body"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("div"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("p"), "http://example.com"));

        // "div" is in scope (searches TagsSearchInScope which includes "html")
        assertTrue(builder.inScope("div"));
        // "table" is not in scope
        assertFalse(builder.inScope("table"));
    }

    @Test
    public void inSelectScope_trueAndFalse() {
        // stack: html > select > option
        builder.push(new Element(Tag.valueOf("html"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("select"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("option"), "http://example.com"));

        assertTrue(builder.inSelectScope("option"));
        // "optgroup" not present – should be false
        assertFalse(builder.inSelectScope("optgroup"));
    }

    @Test
    public void inTableScope_andButtonScope() {
        // stack: html > body > table > tr > td
        builder.push(new Element(Tag.valueOf("html"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("body"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("table"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("tr"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("td"), "http://example.com"));

        assertTrue(builder.inTableScope("table"));
        assertFalse(builder.inTableScope("div"));

        // button scope: only "button" is a stopper
        builder.push(new Element(Tag.valueOf("button"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("span"), "http://example.com"));
        assertTrue(builder.inButtonScope("span"));
        assertFalse(builder.inButtonScope("button")); // because button itself is a stopper
    }

    /*** -----------------------------------------------------------------------
     *  Element insertion helpers
     *  --------------------------------------------------------------------- */

    @Test
    public void insertEmpty_selfClosingKnownTag() {
        Token.StartTag start = new Token.StartTag();
        start.name("br");
        start.selfClosing(true);
        Element el = builder.insert(start);
        // <br> should be on the stack and be a void element (no further processing needed)
        assertEquals("br", el.tagName());
        assertTrue(builder.onStack(el));
        // the tokeniser should have been set to Data state (cannot verify directly,
        // but no exception means the transition call succeeded)
    }

    @Test
    public void insertEmpty_selfClosingUnknownTag_marksSelfClosing() {
        Token.StartTag start = new Token.StartTag();
        start.name("my-unknown");
        start.selfClosing(true);
        Element el = builder.insert(start);
        assertEquals("my-unknown", el.tagName());
        // unknown tags are marked as self‑closing internally
        assertTrue(el.tag().isSelfClosing());
    }

    @Test
    public void insertForm_setsFormElement() {
        Token.StartTag start = new Token.StartTag();
        start.name("form");
        FormElement form = builder.insertForm(start, true);
        assertSame(form, builder.getFormElement());
        assertTrue(builder.onStack(form));
    }

    @Test
    public void insertComment_createsCommentNode() {
        Token.Comment comment = new Token.Comment();
        comment.data("hello world");
        builder.insert(comment);
        // The comment should be the first child of the document (stack empty at this point)
        Node first = builder.getDocument().childNode(0);
        assertTrue(first instanceof Comment);
        assertEquals("hello world", ((Comment) first).getData());
    }

    @Test
    public void insertCharacter_intoScriptCreatesDataNode() {
        // push a <script> element as current element
        Element script = new Element(Tag.valueOf("script"), "http://example.com");
        builder.push(script);
        Token.Character ch = new Token.Character();
        ch.data("var a = 1;");
        builder.insert(ch);
        Node child = script.childNode(0);
        assertTrue(child instanceof DataNode);
        assertEquals("var a = 1;", ((DataNode) child).getWholeData());
    }

    @Test
    public void insertCharacter_intoNormalElementCreatesTextNode() {
        Element p = new Element(Tag.valueOf("p"), "http://example.com");
        builder.push(p);
        Token.Character ch = new Token.Character();
        ch.data("Hello");
        builder.insert(ch);
        Node child = p.childNode(0);
        assertTrue(child instanceof TextNode);
        assertEquals("Hello", ((TextNode) child).text());
    }

    /*** -----------------------------------------------------------------------
     *  Implied‑end‑tag generation
     *  --------------------------------------------------------------------- */

    @Test
    public void generateImpliedEndTags_popsCorrectElements() {
        // stack: html > body > p > li > dd
        builder.push(new Element(Tag.valueOf("html"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("body"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("p"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("li"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("dd"), "http://example.com"));

        // generate implied end tags (no exclude)
        builder.generateImpliedEndTags();
        // dd, li, p should be popped (they are in TagSearchEndTags)
        assertNull(builder.getFromStack("dd"));
        assertNull(builder.getFromStack("li"));
        assertNull(builder.getFromStack("p"));
        // body should still be present
        assertNotNull(builder.getFromStack("body"));
    }

    @Test
    public void generateImpliedEndTags_withExclude() {
        // stack: html > body > p > li > dd
        builder.push(new Element(Tag.valueOf("html"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("body"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("p"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("li"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("dd"), "http://example.com"));

        // exclude "li" – it should stay on the stack
        builder.generateImpliedEndTags("li");
        assertNull(builder.getFromStack("dd"));
        assertNotNull(builder.getFromStack("li"));
        assertNotNull(builder.getFromStack("p"));
    }

    /*** -----------------------------------------------------------------------
     *  Special element detection
     *  --------------------------------------------------------------------- */

    @Test
    public void isSpecial_knownSpecialAndNonSpecial() {
        Element img = new Element(Tag.valueOf("img"), "http://example.com");
        Element div = new Element(Tag.valueOf("div"), "http://example.com");
        assertTrue(builder.isSpecial(img));
        assertFalse(builder.isSpecial(div));
    }

    /*** -----------------------------------------------------------------------
     *  Active formatting elements handling
     *  --------------------------------------------------------------------- */

    @Test
    public void pushActiveFormattingElements_limitsToThreeIdentical() {
        Element b1 = new Element(Tag.valueOf("b"), "http://example.com");
        Element b2 = new Element(Tag.valueOf("b"), "http://example.com");
        Element b3 = new Element(Tag.valueOf("b"), "http://example.com");
        Element b4 = new Element(Tag.valueOf("b"), "http://example.com");

        builder.pushActiveFormattingElements(b1);
        builder.pushActiveFormattingElements(b2);
        builder.pushActiveFormattingElements(b3);
        // after three identical, the earliest one should be removed when adding the fourth
        builder.pushActiveFormattingElements(b4);
        List<Element> fmt = builder.formattingElements;
        assertEquals(3, fmt.size());
        // the remaining elements should be b2, b3, b4 (b1 removed)
        assertSame(b2, fmt.get(0));
        assertSame(b3, fmt.get(1));
        assertSame(b4, fmt.get(2));
    }

    @Test
    public void reconstructFormattingElements_createsMissingElements() {
        // Simulate a situation where a formatting element is in the list but not on the stack
        Element i = new Element(Tag.valueOf("i"), "http://example.com");
        builder.formattingElements.add(i); // list contains <i>
        // stack currently empty (no <i>)
        builder.reconstructFormattingElements();

        // after reconstruction, <i> should be on the stack and also be the last formatting element
        Element onStack = builder.getFromStack("i");
        assertNotNull(onStack);
        assertSame(onStack, builder.lastFormattingElement());
    }

    @Test
    public void clearFormattingElementsToLastMarker_andMarkerBehaviour() {
        Element b = new Element(Tag.valueOf("b"), "http://example.com");
        builder.pushActiveFormattingElements(b);
        builder.insertMarkerToFormattingElements(); // null marker
        Element i = new Element(Tag.valueOf("i"), "http://example.com");
        builder.pushActiveFormattingElements(i);

        // clear up to last marker – should remove i and the marker, leaving only b
        builder.clearFormattingElementsToLastMarker();
        assertTrue(builder.isInActiveFormattingElements(b));
        assertFalse(builder.isInActiveFormattingElements(i));
        // the marker itself should have been removed; formattingElements size == 1
        assertEquals(1, builder.formattingElements.size());
    }

    @Test
    public void replaceActiveFormattingElement_updatesList() {
        Element bOld = new Element(Tag.valueOf("b"), "http://example.com");
        Element bNew = new Element(Tag.valueOf("b"), "http://example.com");
        builder.formattingElements.add(bOld);
        builder.replaceActiveFormattingElement(bOld, bNew);
        assertFalse(builder.isInActiveFormattingElements(bOld));
        assertTrue(builder.isInActiveFormattingElements(bNew));
    }

    @Test
    public void getActiveFormattingElement_returnsLastMatching() {
        Element b1 = new Element(Tag.valueOf("b"), "http://example.com");
        Element i = new Element(Tag.valueOf("i"), "http://example.com");
        Element b2 = new Element(Tag.valueOf("b"), "http://example.com");
        builder.formattingElements.add(b1);
        builder.formattingElements.add(i);
        builder.formattingElements.add(b2);

        // should return the later <b> (b2)
        Element result = builder.getActiveFormattingElement("b");
        assertSame(b2, result);
    }

    /*** -----------------------------------------------------------------------
     *  Foster parent insertion
     *  --------------------------------------------------------------------- */

    @Test
    public void insertInFosterParent_withTable() {
        // Build a minimal stack with a table that has a parent (html)
        Element html = new Element(Tag.valueOf("html"), "http://example.com");
        Element body = new Element(Tag.valueOf("body"), "http://example.com");
        Element table = new Element(Tag.valueOf("table"), "http://example.com");
        Element tr = new Element(Tag.valueOf("tr"), "http://example.com");
        builder.push(html);
        builder.push(body);
        builder.push(table);
        builder.push(tr);

        // Insert a stray text node while in foster mode
        builder.setFosterInserts(true);
        TextNode stray = new TextNode("fostered");
        builder.insertInFosterParent(stray);

        // Because table has a parent (body), the node should be inserted before the table element
        assertEquals(stray, body.childNode(body.childNodeSize() - 1)); // last child of body
        assertEquals(stray, table.previousSibling());
    }

    @Test
    public void insertInFosterParent_withoutTable_fallsBackToRoot() {
        // No table in stack – fragment scenario
        Element html = new Element(Tag.valueOf("html"), "http://example.com");
        builder.push(html);
        builder.setFosterInserts(true);
        TextNode txt = new TextNode("fallback");
        builder.insertInFosterParent(txt);
        // Should be appended to the root (html) element
        assertEquals(txt, html.childNode(0));
    }

    /*** -----------------------------------------------------------------------
     *  resetInsertionMode logic – verifies state selection based on stack content
     *  --------------------------------------------------------------------- */

    @Test
    public void resetInsertionMode_selectsCorrectState() {
        // Build a stack that ends with <select>
        builder.push(new Element(Tag.valueOf("html"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("body"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("select"), "http://example.com"));
        builder.resetInsertionMode();
        assertEquals(HtmlTreeBuilderState.InSelect, builder.state());

        // Change stack to end with <td> (not the last element)
        builder = new HtmlTreeBuilder();
        builder.initialiseParse(new StringReader(""), "http://example.com", errors, ParseSettings.htmlDefault);
        builder.push(new Element(Tag.valueOf("html"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("body"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("table"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("tr"), "http://example.com"));
        builder.push(new Element(Tag.valueOf("td"), "http://example.com"));
        builder.resetInsertionMode();
        assertEquals(HtmlTreeBuilderState.InCell, builder.state());
    }

    /*** -----------------------------------------------------------------------
     *  Miscellaneous getters / setters
     *  --------------------------------------------------------------------- */

    @Test
    public void gettersAndSetters_workAsExpected() {
        // head element
        Element head = new Element(Tag.valueOf("head"), "http://example.com");
        builder.setHeadElement(head);
        assertSame(head, builder.getHeadElement());

        // fragment parsing flag
        assertFalse(builder.isFragmentParsing());
        // simulate fragment parsing
        builder.fragmentParsing = true;
        assertTrue(builder.isFragmentParsing());

        // pending table characters list
        List<String> chars = new ArrayList<>();
        chars.add("a");
        builder.setPendingTableCharacters(chars);
        assertSame(chars, builder.getPendingTableCharacters());
    }
}
```
###Test END##