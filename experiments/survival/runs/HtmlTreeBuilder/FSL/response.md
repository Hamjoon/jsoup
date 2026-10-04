###Test START##
package org.jsoup.parser;

import static org.junit.Assert.*;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.List;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.*;
import org.jsoup.parser.ParseErrorList;
import org.jsoup.parser.ParseSettings;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link HtmlTreeBuilder}.
 *
 * The tests are placed in the same package as {@code HtmlTreeBuilder} so they can
 * access package‑private members.  Where a method is {@code private} we use
 * reflection to initialise the builder (the {@code initialiseParse} method is
 * {@code protected} in the real library).
 */
public class HtmlTreeBuilderTest {

    private HtmlTreeBuilder builder;

    /** Initialise a fresh {@link HtmlTreeBuilder} before each test. */
    @Before
    public void setUp() throws Exception {
        builder = new HtmlTreeBuilder();

        // invoke the protected initialiseParse(...) method so the internal fields are reset
        Method init = HtmlTreeBuilder.class.getDeclaredMethod(
                "initialiseParse", Reader.class, String.class, ParseErrorList.class, ParseSettings.class);
        init.setAccessible(true);
        init.invoke(builder, new StringReader(""), "", ParseErrorList.noTracking(), ParseSettings.htmlDefault);
    }

    /** Helper to create an element with the given tag name. */
    private static Element el(String tag) {
        return new Element(Tag.valueOf(tag), "");
    }

    /* --------------------------------------------------------------------- *
     *  Stack manipulation tests
     * --------------------------------------------------------------------- */

    @Test
    public void testPushAndPop() {
        Element div = el("div");
        Element span = el("span");
        builder.push(div);
        builder.push(span);

        assertEquals(span, builder.currentElement());
        assertEquals(span, builder.pop());
        assertEquals(div, builder.currentElement());
    }

    @Test
    public void testGetFromStackAndRemoveFromStack() {
        Element a = el("a");
        Element b = el("b");
        Element c = el("c");
        builder.push(a);
        builder.push(b);
        builder.push(c);

        assertEquals(b, builder.getFromStack("b"));
        assertTrue(builder.removeFromStack(b));
        assertNull(builder.getFromStack("b"));
        // stack should now be a, c (c is on top)
        assertEquals(c, builder.currentElement());
    }

    @Test
    public void testPopStackToCloseSingleName() {
        builder.push(el("html"));
        builder.push(el("body"));
        builder.push(el("div"));
        builder.push(el("p"));
        builder.push(el("span"));

        builder.popStackToClose("div"); // should pop span, p, div
        assertEquals("body", builder.currentElement().tagName());
        assertNull(builder.getFromStack("span"));
        assertNull(builder.getFromStack("p"));
        assertNull(builder.getFromStack("div"));
    }

    @Test
    public void testPopStackToCloseMultipleNames() {
        builder.push(el("html"));
        builder.push(el("body"));
        builder.push(el("ul"));
        builder.push(el("li"));
        builder.push(el("p"));

        // pop until we encounter either "ul" or "li"
        builder.popStackToClose("ul", "li");
        assertEquals("body", builder.currentElement().tagName());
        assertNull(builder.getFromStack("ul"));
        assertNull(builder.getFromStack("li"));
        assertNull(builder.getFromStack("p"));
    }

    @Test
    public void testPopStackToBefore() {
        builder.push(el("html"));
        builder.push(el("body"));
        builder.push(el("div"));
        builder.push(el("p"));
        builder.push(el("span"));

        builder.popStackToBefore("div"); // should remove span and p, keep div on stack
        assertEquals("div", builder.currentElement().tagName());
        assertNull(builder.getFromStack("span"));
        assertNull(builder.getFromStack("p"));
    }

    @Test
    public void testClearStackToTableContext() {
        builder.push(el("html"));
        builder.push(el("body"));
        builder.push(el("div"));
        builder.push(el("table"));
        builder.push(el("tbody"));
        builder.push(el("tr"));
        builder.push(el("td"));
        builder.push(el("p"));

        builder.clearStackToTableContext(); // should stop at the first "table"
        assertEquals("table", builder.currentElement().tagName());
        assertNull(builder.getFromStack("tbody"));
        assertNull(builder.getFromStack("tr"));
        assertNull(builder.getFromStack("td"));
        assertNull(builder.getFromStack("p"));
    }

    /* --------------------------------------------------------------------- *
     *  Scope checking tests
     * --------------------------------------------------------------------- */

    @Test
    public void testInScope() {
        // Build stack: html > body > div > p
        builder.push(el("html"));
        builder.push(el("body"));
        builder.push(el("div"));
        builder.push(el("p"));

        // "p" is in the generic scope (TagsSearchInScope contains "td","th"... not "p")
        assertTrue(builder.inScope("p"));
        // "div" is also in scope
        assertTrue(builder.inScope("div"));
        // a tag not present should be false
        assertFalse(builder.inScope("section"));
    }

    @Test
    public void testInButtonScope() {
        // Stack: html > body > button > span
        builder.push(el("html"));
        builder.push(el("body"));
        builder.push(el("button"));
        builder.push(el("span"));

        assertTrue(builder.inButtonScope("button"));
        // "span" is not a button‑scope element, but it is still in scope because the search
        // includes the generic list plus the button list.
        assertTrue(builder.inButtonScope("span"));
        // An element not on the stack is false
        assertFalse(builder.inButtonScope("div"));
    }

    @Test
    public void testInTableScope() {
        // Stack: html > body > table > tr > td
        builder.push(el("html"));
        builder.push(el("body"));
        builder.push(el("table"));
        builder.push(el("tr"));
        builder.push(el("td"));

        assertTrue(builder.inTableScope("table"));
        assertTrue(builder.inTableScope("td"));
        assertFalse(builder.inTableScope("div"));
    }

    @Test
    public void testInSelectScope() {
        // Stack: html > body > select > option
        builder.push(el("html"));
        builder.push(el("body"));
        builder.push(el("select"));
        builder.push(el("option"));

        assertTrue(builder.inSelectScope("option"));
        // "optgroup" is also allowed in the select scope list
        builder.push(el("optgroup"));
        assertTrue(builder.inSelectScope("optgroup"));
        // a tag that breaks the select scope should return false
        builder.push(el("div"));
        assertFalse(builder.inSelectScope("div"));
    }

    /* --------------------------------------------------------------------- *
     *  Implied end‑tag generation
     * --------------------------------------------------------------------- */

    @Test
    public void testGenerateImpliedEndTagsWithoutExclude() {
        // Build stack: html > body > p > li > dd
        builder.push(el("html"));
        builder.push(el("body"));
        builder.push(el("p"));
        builder.push(el("li"));
        builder.push(el("dd"));

        builder.generateImpliedEndTags(); // should pop dd, li, p (all are in TagSearchEndTags)
        assertEquals("body", builder.currentElement().tagName());
        assertNull(builder.getFromStack("p"));
        assertNull(builder.getFromStack("li"));
        assertNull(builder.getFromStack("dd"));
    }

    @Test
    public void testGenerateImpliedEndTagsWithExclude() {
        // Build stack: html > body > p > li > dd
        builder.push(el("html"));
        builder.push(el("body"));
        builder.push(el("p"));
        builder.push(el("li"));
        builder.push(el("dd"));

        // Exclude "li" – it should stay on the stack
        builder.generateImpliedEndTags("li");
        assertEquals("li", builder.currentElement().tagName());
        assertNull(builder.getFromStack("dd"));
        // "p" is still there because we stopped before "li"
        assertNotNull(builder.getFromStack("p"));
    }

    /* --------------------------------------------------------------------- *
     *  Special‑tag detection
     * --------------------------------------------------------------------- */

    @Test
    public void testIsSpecial() {
        assertTrue(builder.isSpecial(el("script"))); // script is in TagSearchSpecial
        assertTrue(builder.isSpecial(el("br")));     // br is a void element and special
        assertFalse(builder.isSpecial(el("div")));  // div is not special
        assertFalse(builder.isSpecial(el("custom"))); // unknown tag
    }

    /* --------------------------------------------------------------------- *
     *  Active formatting element handling
     * --------------------------------------------------------------------- */

    @Test
    public void testPushActiveFormattingElementsLimitsDuplicates() {
        Element b1 = el("b");
        Element b2 = el("b");
        Element b3 = el("b");
        Element b4 = el("b");

        builder.pushActiveFormattingElements(b1);
        builder.pushActiveFormattingElements(b2);
        builder.pushActiveFormattingElements(b3);
        // At this point there are three identical <b> elements – the spec says the earliest
        // one is removed when a fourth is added.
        builder.pushActiveFormattingElements(b4);

        // The formatting list should contain exactly three elements, the first one removed.
        assertEquals(3, builder.formattingElements.size());
        assertSame(b2, builder.formattingElements.get(0));
        assertSame(b3, builder.formattingElements.get(1));
        assertSame(b4, builder.formattingElements.get(2));
    }

    @Test
    public void testReconstructFormattingElements() {
        // Create a formatting element that is NOT on the stack
        Element i = el("i");
        builder.pushActiveFormattingElements(i);
        // Ensure the element is not on the stack yet
        assertFalse(builder.onStack(i));

        // Reconstruct – should create a new <i> element, insert it onto the stack,
        // and replace the entry in the formatting list with the newly created element.
        builder.reconstructFormattingElements();

        // The stack now contains the new <i>
        Element onStack = builder.currentElement();
        assertEquals("i", onStack.tagName());

        // The formatting list should contain the *same* instance that is on the stack
        Element fmt = builder.lastFormattingElement();
        assertSame(onStack, fmt);
    }

    @Test
    public void testFormattingElementMarkersAndClear() {
        // Insert a marker and a couple of elements
        builder.insertMarkerToFormattingElements();
        Element b = el("b");
        Element i = el("i");
        builder.pushActiveFormattingElements(b);
        builder.pushActiveFormattingElements(i);

        // The list now: null, b, i
        assertEquals(3, builder.formattingElements.size());
        assertNull(builder.formattingElements.get(0));

        // clearFormattingElementsToLastMarker should remove b and i but keep the marker
        builder.clearFormattingElementsToLastMarker();
        assertEquals(1, builder.formattingElements.size());
        assertNull(builder.formattingElements.get(0));
    }

    @Test
    public void testActiveFormattingElementLookupAndRemoval() {
        Element u = el("u");
        builder.pushActiveFormattingElements(u);
        assertTrue(builder.isInActiveFormattingElements(u));
        assertSame(u, builder.getActiveFormattingElement("u"));

        builder.removeFromActiveFormattingElements(u);
        assertFalse(builder.isInActiveFormattingElements(u));
        assertNull(builder.getActiveFormattingElement("u"));
    }

    @Test
    public void testReplaceActiveFormattingElement() {
        Element s1 = el("s");
        Element s2 = el("s");
        builder.pushActiveFormattingElements(s1);
        assertSame(s1, builder.lastFormattingElement());

        builder.replaceActiveFormattingElement(s1, s2);
        assertSame(s2, builder.lastFormattingElement());
    }

    /* --------------------------------------------------------------------- *
     *  Foster parenting tests
     * --------------------------------------------------------------------- */

    @Test
    public void testInsertInFosterParentWhenTableExists() {
        // Build a simple tree: html > body > table > tbody
        Element html = el("html");
        Element body = el("body");
        Element table = el("table");
        Element tbody = el("tbody");
        builder.push(html);
        builder.push(body);
        builder.push(table);
        builder.push(tbody); // current element is tbody

        // Insert a text node via foster parenting
        TextNode txt = new TextNode("fostered");
        builder.insertInFosterParent(txt);

        // Because the last table has a parent (body), the node should be inserted *before* the table
        // (i.e., as a sibling of the table, preceding it)
        assertEquals(txt, body.childNode(0)); // first child of body is the fostered text node
        // The table should now be the second child
        assertEquals(table, body.childNode(1));
    }

    @Test
    public void testInsertInFosterParentWhenNoTable() {
        // No table on stack – foster parent should be the root element (first in stack)
        Element html = el("html");
        builder.push(html);

        TextNode txt = new TextNode("no-table");
        builder.insertInFosterParent(txt);

        assertEquals(txt, html.childNode(0));
    }

    /* --------------------------------------------------------------------- *
     *  State handling tests
     * --------------------------------------------------------------------- */

    @Test
    public void testTransitionAndMarkInsertionMode() {
        // Initial state is Initial
        assertEquals(HtmlTreeBuilderState.Initial, builder.state());

        builder.transition(HtmlTreeBuilderState.InBody);
        assertEquals(HtmlTreeBuilderState.InBody, builder.state());

        builder.markInsertionMode();
        assertEquals(HtmlTreeBuilderState.InBody, builder.originalState());

        // Change state again – originalState should stay the same
        builder.transition(HtmlTreeBuilderState.InTable);
        assertEquals(HtmlTreeBuilderState.InTable, builder.state());
        assertEquals(HtmlTreeBuilderState.InBody, builder.originalState());
    }

    @Test
    public void testFramesetOkFlag() {
        assertTrue(builder.framesetOk()); // default true
        builder.framesetOk(false);
        assertFalse(builder.framesetOk());
    }

    @Test
    public void testFosterInsertsFlag() {
        assertFalse(builder.isFosterInserts());
        builder.setFosterInserts(true);
        assertTrue(builder.isFosterInserts());
    }

    @Test
    public void testFragmentParsingFlag() {
        // By default parsing a full document
        assertFalse(builder.isFragmentParsing());

        // Simulate a fragment parse by calling parseFragment (public)
        Document doc = new Document("");
        List<Node> nodes = builder.parseFragment("<p>frag</p>", null, "", ParseErrorList.noTracking(), ParseSettings.htmlDefault);
        assertTrue(builder.isFragmentParsing());
        // The fragment should contain a single <p> element
        assertEquals(1, nodes.size());
        assertTrue(nodes.get(0) instanceof Element);
        assertEquals("p", ((Element) nodes.get(0)).tagName());
    }

    /* --------------------------------------------------------------------- *
     *  Miscellaneous getters / setters
     * --------------------------------------------------------------------- */

    @Test
    public void testBaseUriSetting() {
        // Base URI is initially empty
        assertEquals("", builder.getBaseUri());

        // Simulate a <base href="http://example.com/"> element
        Element base = new Element(Tag.valueOf("base"), "");
        base.attr("href", "http://example.com/");
        builder.maybeSetBaseUri(base);

        assertEquals("http://example.com/", builder.getBaseUri());
        // Subsequent calls should be ignored
        Element base2 = new Element(Tag.valueOf("base"), "");
        base2.attr("href", "http://ignored.com/");
        builder.maybeSetBaseUri(base2);
        assertEquals("http://example.com/", builder.getBaseUri());
    }

    @Test
    public void testHeadElementSetterGetter() {
        Element head = el("head");
        builder.setHeadElement(head);
        assertSame(head, builder.getHeadElement());
    }

    @Test
    public void testPendingTableCharacters() {
        builder.newPendingTableCharacters();
        List<String> pending = builder.getPendingTableCharacters();
        assertNotNull(pending);
        assertTrue(pending.isEmpty());

        pending.add("text");
        assertEquals(1, builder.getPendingTableCharacters().size());
        assertEquals("text", builder.getPendingTableCharacters().get(0));

        // replace the list
        builder.setPendingTableCharacters(java.util.Collections.emptyList());
        assertTrue(builder.getPendingTableCharacters().isEmpty());
    }

    @Test
    public void testFormElementAssociation() {
        FormElement form = new FormElement(Tag.valueOf("form"), "", new Attributes());
        builder.setFormElement(form);
        assertSame(form, builder.getFormElement());

        // Insert an input element – it should be added to the form's controls list
        Element input = new Element(Tag.valueOf("input"), "");
        builder.insert(input);
        assertTrue(form.elements().contains(input));
    }
}
###Test END##