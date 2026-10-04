###Test START##
```java
/********************************************************************
 *  JUnit‑4 test suite for {@link org.jsoup.parser.HtmlTreeBuilder}
 *
 *  The tests are written in the same package as the class under test
 *  so that package‑private members can be accessed directly.
 *
 *  The suite covers:
 *   – extraction of all public (or package‑private) methods,
 *   – a basic “happy‑path” test for each method,
 *   – edge‑case and exception scenarios,
 *   – integration style checks that combine several behaviours.
 *
 *  The tests rely only on the public Jsoup API (Elements, Tags,
 *  Tokens, etc.) and on JUnit‑4 assertions.
 ********************************************************************/
package org.jsoup.parser;

import org.jsoup.ParseErrorList;
import org.jsoup.parser.Token.Comment;
import org.jsoup.parser.Token.Character;
import org.jsoup.parser.Token.StartTag;
import org.jsoup.parser.TokeniserState;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class HtmlTreeBuilderTest {

    private HtmlTreeBuilder builder;
    private static final String BASE_URI = "http://example.com/";

    /**
     * Helper: creates a fresh {@link HtmlTreeBuilder} and runs the
     * protected {@code initialiseParse} through {@code parseFragment}
     * so that {@code doc}, {@code stack} and other internal fields are
     * ready for the individual tests.
     */
    @Before
    public void setUp() {
        builder = new HtmlTreeBuilder();
        // initialise by parsing an empty fragment – this calls initialiseParse()
        // and gives us a non‑null Document and an empty stack.
        builder.parseFragment("", null, BASE_URI,
                new ParseErrorList(0, true), builder.defaultSettings());
    }

    /* -----------------------------------------------------------------
     *  1.  State handling
     * ----------------------------------------------------------------- */

    @Test
    public void transition_and_state() {
        builder.transition(HtmlTreeBuilderState.InBody);
        assertEquals(HtmlTreeBuilderState.InBody, builder.state());

        builder.transition(HtmlTreeBuilderState.InTable);
        assertEquals(HtmlTreeBuilderState.InTable, builder.state());
    }

    @Test
    public void markInsertionMode_and_originalState() {
        builder.transition(HtmlTreeBuilderState.InBody);
        builder.markInsertionMode();                     // remembers current state
        assertEquals(HtmlTreeBuilderState.InBody, builder.originalState());

        builder.transition(HtmlTreeBuilderState.InSelect);
        assertEquals(HtmlTreeBuilderState.InSelect, builder.state());
        // originalState must stay unchanged
        assertEquals(HtmlTreeBuilderState.InBody, builder.originalState());
    }

    /* -----------------------------------------------------------------
     *  2.  Frameset flag
     * ----------------------------------------------------------------- */

    @Test
    public void framesetOk_flag() {
        assertTrue(builder.framesetOk());                // default true
        builder.framesetOk(false);
        assertFalse(builder.framesetOk());
    }

    /* -----------------------------------------------------------------
     *  3.  Document / Base URI handling
     * ----------------------------------------------------------------- */

    @Test
    public void getDocument_and_getBaseUri() {
        Document doc = builder.getDocument();
        assertNotNull(doc);
        assertEquals(BASE_URI, builder.getBaseUri());
    }

    @Test
    public void maybeSetBaseUri_respectsFirstBaseTagOnly() {
        // first <base href>
        Element base1 = new Element(Tag.valueOf("base"), BASE_URI);
        base1.attr("href", "http://example.com/first/");
        builder.maybeSetBaseUri(base1);
        assertEquals("http://example.com/first/", builder.getBaseUri());

        // second <base href> must be ignored
        Element base2 = new Element(Tag.valueOf("base"), BASE_URI);
        base2.attr("href", "http://example.com/second/");
        builder.maybeSetBaseUri(base2);
        assertEquals("http://example.com/first/", builder.getBaseUri());
    }

    /* -----------------------------------------------------------------
     *  4.  Stack manipulation
     * ----------------------------------------------------------------- */

    @Test
    public void push_pop_and_onStack_behaviour() {
        Element div = new Element(Tag.valueOf("div"), BASE_URI);
        builder.push(div);
        assertTrue(builder.onStack(div));
        assertEquals(div, builder.pop());
        assertFalse(builder.onStack(div));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void pop_onEmptyStack_throwsException() {
        // stack is empty after setUp()
        builder.pop();   // should throw
    }

    @Test
    public void getFromStack_and_removeFromStack() {
        Element p = new Element(Tag.valueOf("p"), BASE_URI);
        Element span = new Element(Tag.valueOf("span"), BASE_URI);
        builder.push(p);
        builder.push(span);

        assertEquals(span, builder.getFromStack("span"));
        assertTrue(builder.removeFromStack(p));
        assertNull(builder.getFromStack("p"));
        assertFalse(builder.removeFromStack(p)); // already removed
    }

    @Test
    public void popStackToClose_singleName_and_multiNames() {
        // stack: html > body > div > p > li
        builder.push(new Element(Tag.valueOf("html"), BASE_URI));
        builder.push(new Element(Tag.valueOf("body"), BASE_URI));
        builder.push(new Element(Tag.valueOf("div"), BASE_URI));
        builder.push(new Element(Tag.valueOf("p"), BASE_URI));
        builder.push(new Element(Tag.valueOf("li"), BASE_URI));

        // pop to close "p"
        builder.popStackToClose("p");
        assertNull(builder.getFromStack("p"));
        assertNotNull(builder.getFromStack("li")); // li is still there (was removed after p)

        // pop to close either "div" or "body"
        builder.popStackToClose("div", "body");
        assertNull(builder.getFromStack("div"));
        // "body" is the first of the supplied names, so it remains on stack
        assertNotNull(builder.getFromStack("body"));
    }

    @Test
    public void popStackToBefore() {
        builder.push(new Element(Tag.valueOf("html"), BASE_URI));
        builder.push(new Element(Tag.valueOf("head"), BASE_URI));
        builder.push(new Element(Tag.valueOf("title"), BASE_URI));
        builder.push(new Element(Tag.valueOf("meta"), BASE_URI));

        // remove everything before "title" (i.e. html & head)
        builder.popStackToBefore("title");
        assertNull(builder.getFromStack("head"));
        assertNull(builder.getFromStack("html"));
        assertNotNull(builder.getFromStack("title"));
        assertNotNull(builder.getFromStack("meta"));
    }

    @Test
    public void aboveOnStack_returnsCorrectElement() {
        Element html = new Element(Tag.valueOf("html"), BASE_URI);
        Element body = new Element(Tag.valueOf("body"), BASE_URI);
        Element div  = new Element(Tag.valueOf("div"), BASE_URI);
        builder.push(html);
        builder.push(body);
        builder.push(div);

        assertEquals(body, builder.aboveOnStack(div));
        assertEquals(html, builder.aboveOnStack(body));
    }

    @Test
    public void insertOnStackAfter_and_replaceOnStack() {
        Element a = new Element(Tag.valueOf("a"), BASE_URI);
        Element b = new Element(Tag.valueOf("b"), BASE_URI);
        Element c = new Element(Tag.valueOf("c"), BASE_URI);
        builder.push(a);
        builder.push(c);
        builder.insertOnStackAfter(a, b); // stack should be a,b,c
        assertEquals(b, builder.getStack().get(1));

        Element d = new Element(Tag.valueOf("d"), BASE_URI);
        builder.replaceOnStack(c, d);
        assertEquals(d, builder.getStack().get(2));
    }

    /* -----------------------------------------------------------------
     *  5.  In‑scope checks (edge cases)
     * ----------------------------------------------------------------- */

    @Test
    public void inScope_basic_trueAndFalse() {
        // Build a small stack: html > body > ul > li
        builder.push(new Element(Tag.valueOf("html"), BASE_URI));
        builder.push(new Element(Tag.valueOf("body"), BASE_URI));
        builder.push(new Element(Tag.valueOf("ul"), BASE_URI));
        Element li = new Element(Tag.valueOf("li"), BASE_URI);
        builder.push(li);

        // “li” is in list‑item scope
        assertTrue(builder.inListItemScope("li"));
        // “p” is not in scope
        assertFalse(builder.inScope("p"));
    }

    @Test
    public void inSelectScope_edgeCase() {
        // Stack: html > select > option
        builder.push(new Element(Tag.valueOf("html"), BASE_URI));
        builder.push(new Element(Tag.valueOf("select"), BASE_URI));
        builder.push(new Element(Tag.valueOf("option"), BASE_URI));

        assertTrue(builder.inSelectScope("option"));
        // a tag not in the select‑specific set should break the search
        builder.push(new Element(Tag.valueOf("div"), BASE_URI));
        assertFalse(builder.inSelectScope("option"));
    }

    /* -----------------------------------------------------------------
     *  6.  Head / Form handling
     * ----------------------------------------------------------------- */

    @Test
    public void set_and_get_headElement() {
        Element head = new Element(Tag.valueOf("head"), BASE_URI);
        builder.setHeadElement(head);
        assertSame(head, builder.getHeadElement());
    }

    @Test
    public void set_and_get_formElement() {
        FormElement form = new FormElement(Tag.valueOf("form"), BASE_URI);
        builder.setFormElement(form);
        assertSame(form, builder.getFormElement());
    }

    /* -----------------------------------------------------------------
     *  7.  Pending table characters handling
     * ----------------------------------------------------------------- */

    @Test
    public void pendingTableCharactersLifecycle() {
        builder.newPendingTableCharacters();
        List<String> list = builder.getPendingTableCharacters();
        assertNotNull(list);
        assertTrue(list.isEmpty());

        List<String> custom = new ArrayList<>();
        custom.add("cell");
        builder.setPendingTableCharacters(custom);
        assertSame(custom, builder.getPendingTableCharacters());
    }

    /* -----------------------------------------------------------------
     *  8.  Implied end‑tag generation
     * ----------------------------------------------------------------- */

    @Test
    public void generateImpliedEndTags_popsCorrectElements() {
        // Stack: html > body > p > li
        builder.push(new Element(Tag.valueOf("html"), BASE_URI));
        builder.push(new Element(Tag.valueOf("body"), BASE_URI));
        Element p  = new Element(Tag.valueOf("p"), BASE_URI);
        Element li = new Element(Tag.valueOf("li"), BASE_URI);
        builder.push(p);
        builder.push(li);

        // “li” is in TagSearchEndTags, so generateImpliedEndTags() should pop it
        builder.generateImpliedEndTags();
        assertNull(builder.getFromStack("li"));
        // “p” is also an end‑tag element; it should be popped as well
        assertNull(builder.getFromStack("p"));
    }

    @Test
    public void generateImpliedEndTags_excludeTag() {
        // Stack: html > body > p > li
        builder.push(new Element(Tag.valueOf("html"), BASE_URI));
        builder.push(new Element(Tag.valueOf("body"), BASE_URI));
        Element p  = new Element(Tag.valueOf("p"), BASE_URI);
        Element li = new Element(Tag.valueOf("li"), BASE_URI);
        builder.push(p);
        builder.push(li);

        // Exclude “li” – only “p” should be removed
        builder.generateImpliedEndTags("li");
        assertNotNull(builder.getFromStack("li"));   // still there
        assertNull(builder.getFromStack("p"));      // removed
    }

    /* -----------------------------------------------------------------
     *  9.  Special element detection
     * ----------------------------------------------------------------- */

    @Test
    public void isSpecial_knownTag() {
        Element br = new Element(Tag.valueOf("br"), BASE_URI);
        assertTrue(builder.isSpecial(br));
    }

    @Test
    public void isSpecial_unknownTag() {
        Element custom = new Element(Tag.valueOf("custom-tag"), BASE_URI);
        assertFalse(builder.isSpecial(custom));
    }

    /* -----------------------------------------------------------------
     * 10.  Active Formatting Elements handling
     * ----------------------------------------------------------------- */

    @Test
    public void pushActiveFormattingElements_limitsToThreeIdentical() {
        Element b = new Element(Tag.valueOf("b"), BASE_URI);
        // push the same element four times – the 4th push should cause the
        // earliest of the three identical elements to be removed.
        builder.pushActiveFormattingElements(b);
        builder.pushActiveFormattingElements(b);
        builder.pushActiveFormattingElements(b);
        builder.pushActiveFormattingElements(b);
        // there must be exactly three instances in the list
        assertEquals(3, builder.formattingElements.size());
    }

    @Test
    public void reconstructFormattingElements_createsMissingElements() {
        // Build a formatting list: <b>, <i>, (no <b> on stack)
        Element b = new Element(Tag.valueOf("b"), BASE_URI);
        Element i = new Element(Tag.valueOf("i"), BASE_URI);
        builder.pushActiveFormattingElements(b);
        builder.pushActiveFormattingElements(i);

        // Ensure the stack does NOT contain the <b> element
        assertFalse(builder.onStack(b));

        // Reconstruct – this should insert a new <b> element onto the stack
        builder.reconstructFormattingElements();

        // The newly created element must now be on the stack
        Element onStack = builder.getStack().get(builder.getStack().size() - 1);
        assertEquals("b", onStack.tagName());
    }

    @Test
    public void clearFormattingElementsToLastMarker_and_removeFromActiveFormattingElements() {
        Element b = new Element(Tag.valueOf("b"), BASE_URI);
        Element i = new Element(Tag.valueOf("i"), BASE_URI);
        builder.pushActiveFormattingElements(b);
        builder.insertMarkerToFormattingElements();   // marker
        builder.pushActiveFormattingElements(i);

        // remove i explicitly
        builder.removeFromActiveFormattingElements(i);
        assertFalse(builder.isInActiveFormattingElements(i));

        // clear up to marker – should remove the marker and everything before it (i.e. b)
        builder.clearFormattingElementsToLastMarker();
        assertTrue(builder.formattingElements.isEmpty());
    }

    @Test
    public void getActiveFormattingElement_findsMostRecent() {
        Element b1 = new Element(Tag.valueOf("b"), BASE_URI);
        Element b2 = new Element(Tag.valueOf("b"), BASE_URI);
        builder.pushActiveFormattingElements(b1);
        builder.pushActiveFormattingElements(b2);

        Element found = builder.getActiveFormattingElement("b");
        assertSame(b2, found);   // most recent one
    }

    @Test
    public void replaceActiveFormattingElement_updatesList() {
        Element b1 = new Element(Tag.valueOf("b"), BASE_URI);
        Element b2 = new Element(Tag.valueOf("b"), BASE_URI);
        Element strong = new Element(Tag.valueOf("strong"), BASE_URI);
        builder.pushActiveFormattingElements(b1);
        builder.pushActiveFormattingElements(b2);

        builder.replaceActiveFormattingElement(b2, strong);
        assertSame(strong, builder.getActiveFormattingElement("strong"));
        assertNull(builder.getActiveFormattingElement("b"));
    }

    /* -----------------------------------------------------------------
     * 11.  Foster parenting (no table present)
     * ----------------------------------------------------------------- */

    @Test
    public void insertInFosterParent_withoutTable_appendsToRoot() {
        // Ensure there is at least one element on the stack to act as the root
        Element root = new Element(Tag.valueOf("html"), BASE_URI);
        builder.push(root);

        TextNode txt = new TextNode("fostered");
        builder.insertInFosterParent(txt);

        // The node should be a child of the root element (the first element on stack)
        assertEquals(1, root.childNodeSize());
        assertSame(txt, root.childNode(0));
    }

    /* -----------------------------------------------------------------
     * 12.  resetInsertionMode – verifies a few typical contexts
     * ----------------------------------------------------------------- */

    @Test
    public void resetInsertionMode_selectContext() {
        builder.push(new Element(Tag.valueOf("html"), BASE_URI));
        builder.push(new Element(Tag.valueOf("select"), BASE_URI));
        builder.resetInsertionMode();
        assertEquals(HtmlTreeBuilderState.InSelect, builder.state());
    }

    @Test
    public void resetInsertionMode_bodyContext() {
        builder.push(new Element(Tag.valueOf("html"), BASE_URI));
        builder.push(new Element(Tag.valueOf("body"), BASE_URI));
        builder.resetInsertionMode();
        assertEquals(HtmlTreeBuilderState.InBody, builder.state());
    }

    /* -----------------------------------------------------------------
     * 13.  toString() – sanity check that it does not throw
     * ----------------------------------------------------------------- */

    @Test
    public void toString_returnsNonNull() {
        String s = builder.toString();
        assertNotNull(s);
        assertTrue(s.contains("TreeBuilder"));
    }

    /* -----------------------------------------------------------------
     * 14.  process(Token) – basic state transition via a StartTag token
     * ----------------------------------------------------------------- */

    @Test
    public void process_startTagToken_advancesState() {
        // Build a simple start‑tag token (e.g. <body>)
        StartTag token = new Token.StartTag();
        token.name("body");
        token.selfClosing(false);

        // The builder starts in the Initial state
        assertEquals(HtmlTreeBuilderState.Initial, builder.state());

        // Process the token – the state machine will transition to InBody (as per spec)
        boolean consumed = builder.process(token);
        assertTrue(consumed);
        assertEquals(HtmlTreeBuilderState.InBody, builder.state());
    }

    /* -----------------------------------------------------------------
     * 15.  error handling – ensure errors are recorded when allowed
     * ----------------------------------------------------------------- */

    @Test
    public void error_recordsParseErrorWhenAllowed() {
        // Force an error condition by calling error() directly (package‑private)
        // The ParseErrorList we have tracks errors (max 10)
        ParseErrorList errors = new ParseErrorList(10, true);
        builder.errors = errors; // direct field access (same package)

        // Simulate an unexpected token in a given state
        builder.error(HtmlTreeBuilderState.InBody);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getErrorMessage().contains("Unexpected token"));
    }
}
```
###Test END##