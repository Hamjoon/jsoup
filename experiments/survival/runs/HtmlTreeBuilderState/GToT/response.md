###Test START##
```java
package org.jsoup.parser;

import org.jsoup.nodes.*;
import org.jsoup.parser.Token.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link HtmlTreeBuilderState}.
 *
 * The tests are written as three “experts” would collaborate:
 * <ul>
 *   <li>Extract public (or package‑private) methods</li>
 *   <li>Write a basic test for each method</li>
 *   <li>Identify edge‑cases / exception scenarios</li>
 *   <li>Write additional tests for those scenarios</li>
 *   <li>Merge everything into a single test class</li>
 * </ul>
 *
 * The enum lives in the same package as the test, so we can call the
 * {@code process(Token, HtmlTreeBuilder)} method directly.  All interactions
 * with {@link HtmlTreeBuilder} are verified with Mockito – the real parser is
 * not required for unit testing the state logic.
 */
@RunWith(MockitoJUnitRunner.class)
public class HtmlTreeBuilderStateTest {

    @Mock
    private HtmlTreeBuilder tb;               // mock of the tree‑builder used by the states

    @Before
    public void setUp() {
        // Most HtmlTreeBuilder methods used by the states return a boolean.
        // By default we let them return true so that the state can continue.
        when(tb.process(any(Token.class))).thenReturn(true);
        when(tb.process(any(Token.class), any(HtmlTreeBuilderState.class))).thenReturn(true);
        when(tb.transition(any(HtmlTreeBuilderState.class))).thenReturn(true);
        when(tb.error(any())).thenReturn(true);
        when(tb.insert(any())).thenReturn(new Element(Tag.valueOf("dummy"), ""));
        when(tb.insertEmpty(any())).thenReturn(new Element(Tag.valueOf("empty"), ""));
        when(tb.getDocument()).thenReturn(new Document(""));
        when(tb.settings).thenReturn(new ParseSettings(true));
        when(tb.isFragmentParsing()).thenReturn(false);
        when(tb.currentElement()).thenReturn(new Element(Tag.valueOf("html"), ""));
        when(tb.inScope(anyString())).thenReturn(true);
        when(tb.inTableScope(anyString())).thenReturn(true);
        when(tb.inButtonScope(anyString())).thenReturn(true);
        when(tb.isSpecial(any(Element.class))).thenReturn(false);
        when(tb.getStack()).thenReturn(new ArrayList<>());
        when(tb.getHeadElement()).thenReturn(new Element(Tag.valueOf("head"), ""));
        when(tb.getBaseUri()).thenReturn("");
    }

    /* ----------------------------------------------------------------------
     *  1.  Public / package‑private methods of HtmlTreeBuilderState
     * ---------------------------------------------------------------------- */
    // The enum defines one abstract package‑private method:
    //   boolean process(Token t, HtmlTreeBuilder tb)
    // plus a few private static helpers (isWhitespace, handleRcData, handleRawtext).
    // The tests below target the public behaviour of each enum constant via the
    // process() method and the helpers through reflection where needed.

    /* ----------------------------------------------------------------------
     *  2.  Basic functionality tests – one per state
     * ---------------------------------------------------------------------- */

    @Test
    public void initialState_ignoresWhitespace() {
        Token.Character ws = new Token.Character().data("   \n\t");
        boolean result = HtmlTreeBuilderState.Initial.process(ws, tb);
        assertTrue("Initial state should ignore whitespace and return true", result);
        verify(tb, never()).insert(any());          // no insertion
        verify(tb, never()).transition(any());      // no transition
    }

    @Test
    public void initialState_processesDoctype_andTransitions() {
        Token.Doctype doctype = new Token.Doctype()
                .name("html")
                .publicIdentifier("")
                .systemIdentifier("")
                .forceQuirks(false);
        boolean result = HtmlTreeBuilderState.Initial.process(doctype, tb);
        assertTrue(result);
        // The state should have appended a DocumentType node and transitioned.
        verify(tb).getDocument();
        verify(tb).transition(HtmlTreeBuilderState.BeforeHtml);
    }

    @Test
    public void beforeHtmlState_unexpectedDoctype_returnsFalseAndErrors() {
        Token.Doctype d = new Token.Doctype().name("html");
        when(tb.error(any())).thenReturn(false);
        boolean result = HtmlTreeBuilderState.BeforeHtml.process(d, tb);
        assertFalse(result);
        verify(tb).error(HtmlTreeBuilderState.BeforeHtml);
    }

    @Test
    public void beforeHtmlState_startHtmlTagInsertsAndTransitions() {
        Token.StartTag start = new Token.StartTag().name("html");
        boolean result = HtmlTreeBuilderState.BeforeHtml.process(start, tb);
        assertTrue(result);
        verify(tb).insert(start);
        verify(tb).transition(HtmlTreeBuilderState.BeforeHead);
    }

    @Test
    public void beforeHeadState_startHeadTagInsertsAndTransitions() {
        Token.StartTag head = new Token.StartTag().name("head");
        boolean result = HtmlTreeBuilderState.BeforeHead.process(head, tb);
        assertTrue(result);
        verify(tb).insert(head);
        verify(tb).setHeadElement(any(Element.class));
        verify(tb).transition(HtmlTreeBuilderState.InHead);
    }

    @Test
    public void inHeadState_startTitleTriggersRcDataHandling() {
        // Spy on the private static method handleRcData via reflection.
        Token.StartTag title = new Token.StartTag().name("title");
        HtmlTreeBuilderState.InHead.process(title, tb);
        // The state should have called tb.tokeniser.transition(Rcdata) and
        // transitioned to Text.
        verify(tb).tokeniser; // just ensures we touched the tokeniser (no direct verify)
        // Because the tokeniser is a mock, we verify the transition call indirectly.
        verify(tb).markInsertionMode();
        verify(tb).transition(HtmlTreeBuilderState.Text);
    }

    @Test
    public void inHeadState_endHeadTagPopsAndTransitionsToAfterHead() {
        Token.EndTag endHead = new Token.EndTag().name("head");
        boolean result = HtmlTreeBuilderState.InHead.process(endHead, tb);
        assertTrue(result);
        verify(tb).pop();
        verify(tb).transition(HtmlTreeBuilderState.AfterHead);
    }

    @Test
    public void inBodyState_characterNullString_returnsFalseAndErrors() {
        Token.Character nullChar = new Token.Character().data("\u0000");
        boolean result = HtmlTreeBuilderState.InBody.process(nullChar, tb);
        assertFalse(result);
        verify(tb).error(HtmlTreeBuilderState.InBody);
    }

    @Test
    public void inBodyState_startTagAWhenActiveFormattingElementExists() {
        // Simulate an existing active formatting element <a>
        Element existingA = new Element(Tag.valueOf("a"), "");
        when(tb.getActiveFormattingElement("a")).thenReturn(existingA);
        when(tb.getFromStack("a")).thenReturn(existingA);
        Token.StartTag aTag = new Token.StartTag().name("a");
        boolean result = HtmlTreeBuilderState.InBody.process(aTag, tb);
        assertTrue(result);
        // Expect an error and processing of an implicit end tag.
        verify(tb).error(HtmlTreeBuilderState.InBody);
        verify(tb).processEndTag("a");
        // The existing <a> should be removed from both active list and stack.
        verify(tb).removeFromActiveFormattingElements(existingA);
        verify(tb).removeFromStack(existingA);
    }

    @Test
    public void textState_characterTokenInserts() {
        Token.Character ch = new Token.Character().data("x");
        boolean result = HtmlTreeBuilderState.Text.process(ch, tb);
        assertTrue(result);
        verify(tb).insert(ch);
    }

    @Test
    public void textState_eofTriggersErrorPopAndRestoreOriginalState() {
        // Mock originalState() to return InBody for verification.
        when(tb.originalState()).thenReturn(HtmlTreeBuilderState.InBody);
        Token eof = new Token.EOF();
        boolean result = HtmlTreeBuilderState.Text.process(eof, tb);
        assertTrue(result);
        verify(tb).error(HtmlTreeBuilderState.Text);
        verify(tb).pop();
        verify(tb).transition(HtmlTreeBuilderState.InBody);
        verify(tb).process(eof);
    }

    @Test
    public void inTableState_characterTokenTriggersTransitionToInTableText() {
        Token.Character ch = new Token.Character().data("a");
        boolean result = HtmlTreeBuilderState.InTable.process(ch, tb);
        assertTrue(result);
        verify(tb).newPendingTableCharacters();
        verify(tb).markInsertionMode();
        verify(tb).transition(HtmlTreeBuilderState.InTableText);
        // After transition the same character token is re‑processed.
        verify(tb).process(ch);
    }

    @Test
    public void inTableTextState_characterNullString_returnsFalseAndErrors() {
        Token.Character nullChar = new Token.Character().data("\u0000");
        boolean result = HtmlTreeBuilderState.InTableText.process(nullChar, tb);
        assertFalse(result);
        verify(tb).error(HtmlTreeBuilderState.InTableText);
    }

    @Test
    public void afterBodyState_unexpectedStartTagTriggersErrorAndSwitchesToInBody() {
        Token.StartTag div = new Token.StartTag().name("div");
        when(tb.error(any())).thenReturn(true);
        boolean result = HtmlTreeBuilderState.AfterBody.process(div, tb);
        assertTrue(result);
        verify(tb).error(HtmlTreeBuilderState.AfterBody);
        verify(tb).transition(HtmlTreeBuilderState.InBody);
        verify(tb).process(div);
    }

    @Test
    public void inFramesetState_startNoframesTransitionsToInHead() {
        Token.StartTag noframes = new Token.StartTag().name("noframes");
        boolean result = HtmlTreeBuilderState.InFrameset.process(noframes, tb);
        assertTrue(result);
        verify(tb).process(noframes, HtmlTreeBuilderState.InHead);
    }

    @Test
    public void afterFramesetState_endHtmlTransitionsToAfterAfterFrameset() {
        Token.EndTag htmlEnd = new Token.EndTag().name("html");
        boolean result = HtmlTreeBuilderState.AfterFrameset.process(htmlEnd, tb);
        assertTrue(result);
        verify(tb).transition(HtmlTreeBuilderState.AfterAfterFrameset);
    }

    /* ----------------------------------------------------------------------
     *  3.  Edge‑case / exception scenario tests (identified from source)
     * ---------------------------------------------------------------------- */

    @Test
    public void inHeadNoscriptState_unexpectedDoctypeErrorsAndContinues() {
        Token.Doctype d = new Token.Doctype().name("html");
        HtmlTreeBuilderState.InHeadNoscript.process(d, tb);
        verify(tb).error(HtmlTreeBuilderState.InHeadNoscript);
        // No transition – the state stays the same.
        verify(tb, never()).transition(any());
    }

    @Test
    public void inSelectState_startTagSelectWhenAlreadyInSelectErrorsAndCloses() {
        Token.StartTag select = new Token.StartTag().name("select");
        boolean result = HtmlTreeBuilderState.InSelect.process(select, tb);
        assertTrue(result);
        verify(tb).error(HtmlTreeBuilderState.InSelect);
        verify(tb).processEndTag("select");
    }

    @Test
    public void inSelectState_endTagOptionWhenNotCurrentOptionErrors() {
        Token.EndTag endOption = new Token.EndTag().name("option");
        when(tb.currentElement().nodeName()).thenReturn("div"); // not an <option>
        boolean result = HtmlTreeBuilderState.InSelect.process(endOption, tb);
        assertTrue(result);
        verify(tb).error(HtmlTreeBuilderState.InSelect);
    }

    @Test
    public void foreignContentState_alwaysReturnsTrue() {
        Token.Comment comment = new Token.Comment().data("test");
        assertTrue(HtmlTreeBuilderState.ForeignContent.process(comment, tb));
    }

    /* ----------------------------------------------------------------------
     *  4.  Tests for the private static helper methods via reflection
     * ---------------------------------------------------------------------- */

    @Test
    public void isWhitespaceHelperDetectsOnlyWhitespace() throws Exception {
        java.lang.reflect.Method m = HtmlTreeBuilderState.class.getDeclaredMethod("isWhitespace", String.class);
        m.setAccessible(true);
        assertTrue((Boolean) m.invoke(null, " \t\n\r\f"));
        assertFalse((Boolean) m.invoke(null, " a "));
    }

    @Test
    public void handleRcDataTransitionsToRcdataAndText() throws Exception {
        Token.StartTag start = new Token.StartTag().name("title");
        java.lang.reflect.Method m = HtmlTreeBuilderState.class.getDeclaredMethod("handleRcData", Token.StartTag.class, HtmlTreeBuilder.class);
        m.setAccessible(true);
        m.invoke(null, start, tb);
        verify(tb).tokeniser;
        verify(tb).markInsertionMode();
        verify(tb).transition(HtmlTreeBuilderState.Text);
        verify(tb).insert(start);
    }

    @Test
    public void handleRawtextTransitionsToRawtextAndText() throws Exception {
        Token.StartTag start = new Token.StartTag().name("style");
        java.lang.reflect.Method m = HtmlTreeBuilderState.class.getDeclaredMethod("handleRawtext", Token.StartTag.class, HtmlTreeBuilder.class);
        m.setAccessible(true);
        m.invoke(null, start, tb);
        verify(tb).tokeniser;
        verify(tb).markInsertionMode();
        verify(tb).transition(HtmlTreeBuilderState.Text);
        verify(tb).insert(start);
    }

    /* ----------------------------------------------------------------------
     *  5.  Utility methods used by multiple tests
     * ---------------------------------------------------------------------- */

    /**
     * Helper to create a {@link Token.StartTag} with the given tag name.
     */
    private Token.StartTag startTag(String name) {
        return new Token.StartTag().name(name);
    }

    /**
     * Helper to create a {@link Token.EndTag} with the given tag name.
     */
    private Token.EndTag endTag(String name) {
        return new Token.EndTag().name(name);
    }
}
```
###Test END##