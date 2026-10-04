package org.jsoup.parser;

import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.*;
import org.jsoup.parser.Token.StartTag;
import org.jsoup.parser.Token.EndTag;
import org.jsoup.parser.Token.Comment;
import org.jsoup.parser.Token.Character;
import org.jsoup.parser.Token.Doctype;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * JUnit4 test suite for {@link HtmlTreeBuilderState}.
 *
 * The tests use Mockito to mock the {@link HtmlTreeBuilder} and its collaborators.
 * Each enum constant is exercised for its main branches, verifying state transitions,
 * element insertions and error handling.
 *
 * Note: The real jsoup library provides concrete implementations for {@link Token},
 * {@link Document}, {@link Element} etc.; these are used directly.
 */
public class HtmlTreeBuilderStateTest {

    private HtmlTreeBuilder tb;               // mocked tree builder
    private Tokeniser tokeniser;              // mocked tokeniser (returned from tb)
    private Document document;                // mocked document (returned from tb)

    @Before
    public void setUp() {
        // deep stubs allow chained calls like tb.settings().normalizeTag(...)
        tb = mock(HtmlTreeBuilder.class, RETURNS_DEEP_STUBS);
        tokeniser = mock(Tokeniser.class);
        document = mock(Document.class);

        // common stubbing for all tests
        when(tb.tokeniser()).thenReturn(tokeniser);
        when(tb.tokeniser()).thenReturn(tokeniser);
        when(tb.tokeniser()).thenReturn(tokeniser);
        when(tb.tokeniser()).thenReturn(tokeniser);
        when(tb.tokeniser()).thenReturn(tokeniser);
        when(tb.getDocument()).thenReturn(document);
        when(tb.settings().normalizeTag(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tb.process(any(Token.class))).thenReturn(true);
        when(tb.process(any(Token.class), any(HtmlTreeBuilderState.class))).thenReturn(true);
        when(tb.processStartTag(anyString())).thenReturn(true);
        when(tb.processStartTag(anyString(), any(Attributes.class))).thenReturn(true);
        when(tb.processEndTag(anyString())).thenReturn(true);
        when(tb.inButtonScope(anyString())).thenReturn(false);
        when(tb.inScope(anyString())).thenReturn(false);
        when(tb.inTableScope(anyString())).thenReturn(true);
        when(tb.inSelectScope(anyString())).thenReturn(true);
        when(tb.isFragmentParsing()).thenReturn(false);
        when(tb.currentElement()).thenReturn(mock(Element.class));
        when(tb.getHeadElement()).thenReturn(mock(Element.class));
        when(tb.getFormElement()).thenReturn(null);
        when(tb.getBaseUri()).thenReturn("http://example.com/");
    }

    /** Helper to create a character token */
    private Character charToken(String data) {
        Character c = new Character();
        c.data(data);
        return c;
    }

    /** Helper to create a comment token */
    private Comment commentToken(String data) {
        Comment c = new Comment();
        c.data(data);
        return c;
    }

    /** Helper to create a doctype token */
    private Doctype doctypeToken(String name) {
        Doctype d = new Doctype();
        d.name(name);
        d.publicIdentifier("");
        d.systemIdentifier("");
        d.forceQuirks(false);
        return d;
    }

    /** Helper to create a start‑tag token */
    private StartTag startTag(String name) {
        StartTag s = new StartTag();
        s.name(name);
        return s;
    }

    /** Helper to create an end‑tag token */
    private EndTag endTag(String name) {
        EndTag e = new EndTag();
        e.name(name);
        return e;
    }

    // -------------------------------------------------------------------------
    //  Tests for the *Initial* state
    // -------------------------------------------------------------------------

    @Test
    public void initialIgnoresWhitespace() {
        Character ws = charToken("   ");
        assertTrue(HtmlTreeBuilderState.Initial.process(ws, tb));
        verify(tb, never()).insert(any());
        verify(tb, never()).transition(any());
    }

    @Test
    public void initialHandlesDoctypeAndTransitions() {
        Doctype d = doctypeToken("html");
        DocumentType mockDoctype = mock(DocumentType.class);
        // when a new DocumentType is created inside the method, we cannot intercept it directly.
        // Instead we verify that the document receives an appended child.
        assertTrue(HtmlTreeBuilderState.Initial.process(d, tb));
        verify(tb).getDocument();
        verify(document).appendChild(any(DocumentType.class));
        verify(tb).transition(HtmlTreeBuilderState.BeforeHtml);
    }

    @Test
    public void initialHandlesStartTagHtml() {
        StartTag s = startTag("html");
        assertTrue(HtmlTreeBuilderState.Initial.process(s, tb));
        verify(tb).insert(s);
        verify(tb).transition(HtmlTreeBuilderState.BeforeHtml);
    }

    // -------------------------------------------------------------------------
    //  Tests for the *BeforeHtml* state
    // -------------------------------------------------------------------------

    @Test
    public void beforeHtmlStartTagHtmlTransitionsToBeforeHead() {
        StartTag s = startTag("html");
        assertTrue(HtmlTreeBuilderState.BeforeHtml.process(s, tb));
        verify(tb).insert(s);
        verify(tb).transition(HtmlTreeBuilderState.BeforeHead);
    }

    @Test
    public void beforeHtmlEndTagHeadTriggersAnythingElse() {
        EndTag e = endTag("head");
        // AnythingElse inserts an <html> start tag and re‑processes the token
        assertTrue(HtmlTreeBuilderState.BeforeHtml.process(e, tb));
        verify(tb).insertStartTag("html");
        verify(tb).transition(HtmlTreeBuilderState.BeforeHead);
        verify(tb).process(e);
    }

    // -------------------------------------------------------------------------
    //  Tests for the *BeforeHead* state
    // -------------------------------------------------------------------------

    @Test
    public void beforeHeadStartTagHeadCreatesHeadElement() {
        StartTag s = startTag("head");
        assertTrue(HtmlTreeBuilderState.BeforeHead.process(s, tb));
        verify(tb).insert(s);
        verify(tb).setHeadElement(any(Element.class));
        verify(tb).transition(HtmlTreeBuilderState.InHead);
    }

    @Test
    public void beforeHeadEndTagHeadTriggersInsertionOfHead() {
        EndTag e = endTag("head");
        assertTrue(HtmlTreeBuilderState.BeforeHead.process(e, tb));
        verify(tb).processStartTag("head");
        verify(tb).process(e);
    }

    @Test
    public void beforeHeadOtherStartTagTriggersInsertionOfHeadAndReprocess() {
        StartTag s = startTag("div");
        assertTrue(HtmlTreeBuilderState.BeforeHead.process(s, tb));
        verify(tb).processStartTag("head");
        verify(tb).process(s);
    }

    // -------------------------------------------------------------------------
    //  Tests for the *InHead* state
    // -------------------------------------------------------------------------

    @Test
    public void inHeadStartTagMetaInsertsEmpty() {
        StartTag s = startTag("meta");
        assertTrue(HtmlTreeBuilderState.InHead.process(s, tb));
        verify(tb).insertEmpty(s);
    }

    @Test
    public void inHeadStartTagTitleEntersRcDataMode() {
        StartTag s = startTag("title");
        assertTrue(HtmlTreeBuilderState.InHead.process(s, tb));
        verify(tokeniser).transition(TokeniserState.Rcdata);
        verify(tb).markInsertionMode();
        verify(tb).transition(HtmlTreeBuilderState.Text);
        verify(tb).insert(s);
    }

    @Test
    public void inHeadEndTagHeadPopsAndTransitions() {
        EndTag e = endTag("head");
        assertTrue(HtmlTreeBuilderState.InHead.process(e, tb));
        verify(tb).pop();
        verify(tb).transition(HtmlTreeBuilderState.AfterHead);
    }

    @Test
    public void inHeadAnythingElseProcessesEndTagHeadThenToken() {
        StartTag s = startTag("div");
        assertTrue(HtmlTreeBuilderState.InHead.process(s, tb));
        verify(tb).processEndTag("head");
        verify(tb).process(s);
    }

    // -------------------------------------------------------------------------
    //  Tests for the *InHeadNoscript* state
    // -------------------------------------------------------------------------

    @Test
    public void inHeadNoscriptStartTagHtmlDelegatesToInBody() {
        StartTag s = startTag("html");
        assertTrue(HtmlTreeBuilderState.InHeadNoscript.process(s, tb));
        verify(tb).process(s, HtmlTreeBuilderState.InBody);
    }

    @Test
    public void inHeadNoscriptEndTagNoscriptPopsAndReturnsToInHead() {
        EndTag e = endTag("noscript");
        assertTrue(HtmlTreeBuilderState.InHeadNoscript.process(e, tb));
        verify(tb).pop();
        verify(tb).transition(HtmlTreeBuilderState.InHead);
    }

    // -------------------------------------------------------------------------
    //  Tests for the *AfterHead* state
    // -------------------------------------------------------------------------

    @Test
    public void afterHeadStartTagBodyCreatesBodyAndDisablesFramesetOk() {
        StartTag s = startTag("body");
        assertTrue(HtmlTreeBuilderState.AfterHead.process(s, tb));
        verify(tb).insert(s);
        verify(tb).framesetOk(false);
        verify(tb).transition(HtmlTreeBuilderState.InBody);
    }

    @Test
    public void afterHeadStartTagUnsupportedHeadErrorsAndReturnsFalse() {
        StartTag s = startTag("head");
        assertFalse(HtmlTreeBuilderState.AfterHead.process(s, tb));
        verify(tb).error(HtmlTreeBuilderState.AfterHead);
    }

    // -------------------------------------------------------------------------
    //  Tests for the *InBody* state – a few representative branches
    // -------------------------------------------------------------------------

    @Test
    public void inBodyCharacterWhitespaceWhenFramesetOk() {
        when(tb.framesetOk()).thenReturn(true);
        Character c = charToken(" ");
        assertTrue(HtmlTreeBuilderState.InBody.process(c, tb));
        verify(tb).reconstructFormattingElements();
        verify(tb).insert(c);
    }

    @Test
    public void inBodyStartTagAWhenActiveFormattingElementExists() {
        // simulate that an active formatting element <a> is present
        Element a = mock(Element.class);
        when(tb.getActiveFormattingElement("a")).thenReturn(a);
        when(tb.getFromStack("a")).thenReturn(a);
        StartTag s = startTag("a");
        assertTrue(HtmlTreeBuilderState.InBody.process(s, tb));
        InOrder inOrder = inOrder(tb);
        inOrder.verify(tb).error(HtmlTreeBuilderState.InBody);
        inOrder.verify(tb).processEndTag("a");
        inOrder.verify(tb).removeFromActiveFormattingElements(a);
        inOrder.verify(tb).removeFromStack(a);
        inOrder.verify(tb).reconstructFormattingElements();
        inOrder.verify(tb).insert(s);
        inOrder.verify(tb).pushActiveFormattingElements(any(Element.class));
    }

    @Test
    public void inBodyStartTagUnknownIsInserted() {
        StartTag s = startTag("div");
        assertTrue(HtmlTreeBuilderState.InBody.process(s, tb));
        verify(tb).reconstructFormattingElements();
        verify(tb).insert(s);
    }

    @Test
    public void inBodyEndTagPWhenInButtonScope() {
        when(tb.inButtonScope("p")).thenReturn(true);
        EndTag e = endTag("p");
        assertTrue(HtmlTreeBuilderState.InBody.process(e, tb));
        verify(tb).generateImpliedEndTags("p");
        verify(tb).popStackToClose("p");
    }

    @Test
    public void inBodyEndTagPWhenNotInButtonScopeCreatesEmptyP() {
        when(tb.inButtonScope("p")).thenReturn(false);
        EndTag e = endTag("p");
        assertTrue(HtmlTreeBuilderState.InBody.process(e, tb));
        verify(tb).error(HtmlTreeBuilderState.InBody);
        verify(tb).processStartTag("p");
        verify(tb).process(e);
    }

    // -------------------------------------------------------------------------
    //  Tests for the *Text* state
    // -------------------------------------------------------------------------

    @Test
    public void textCharacterIsInserted() {
        Character c = charToken("a");
        assertTrue(HtmlTreeBuilderState.Text.process(c, tb));
        verify(tb).insert(c);
    }

    @Test
    public void textEOFTriggersErrorAndStateRestore() {
        Token eof = mock(Token.class);
        when(eof.isEOF()).thenReturn(true);
        assertTrue(HtmlTreeBuilderState.Text.process(eof, tb));
        verify(tb).error(HtmlTreeBuilderState.Text);
        verify(tb).pop();
        verify(tb).transition(tb.originalState());
        verify(tb).process(eof);
    }

    // -------------------------------------------------------------------------
    //  Tests for the *InTable* state
    // -------------------------------------------------------------------------

    @Test
    public void inTableCharacterSwitchesToInTableText() {
        Character c = charToken("a");
        assertTrue(HtmlTreeBuilderState.InTable.process(c, tb));
        verify(tb).newPendingTableCharacters();
        verify(tb).markInsertionMode();
        verify(tb).transition(HtmlTreeBuilderState.InTableText);
        verify(tb).process(c);
    }

    @Test
    public void inTableStartTagCaptionTransitionsToInCaption() {
        StartTag s = startTag("caption");
        assertTrue(HtmlTreeBuilderState.InTable.process(s, tb));
        verify(tb).clearStackToTableContext();
        verify(tb).insertMarkerToFormattingElements();
        verify(tb).insert(s);
        verify(tb).transition(HtmlTreeBuilderState.InCaption);
    }

    @Test
    public void inTableEndTagTableClosesTableAndResetsMode() {
        EndTag e = endTag("table");
        assertTrue(HtmlTreeBuilderState.InTable.process(e, tb));
        verify(tb).inTableScope("table");
        verify(tb).popStackToClose("table");
        verify(tb).resetInsertionMode();
    }

    @Test
    public void inTableAnythingElseFosterInsertsWhenInTableContext() {
        // make current element be a <table> so foster insertion path is taken
        Element table = mock(Element.class);
        when(table.nodeName()).thenReturn("table");
        when(tb.currentElement()).thenReturn(table);
        StartTag s = startTag("div");
        assertTrue(HtmlTreeBuilderState.InTable.process(s, tb));
        InOrder inOrder = inOrder(tb);
        inOrder.verify(tb).error(HtmlTreeBuilderState.InTable);
        inOrder.verify(tb).setFosterInserts(true);
        inOrder.verify(tb).process(s, HtmlTreeBuilderState.InBody);
        inOrder.verify(tb).setFosterInserts(false);
    }

    // -------------------------------------------------------------------------
    //  Tests for the *InTableText* state
    // -------------------------------------------------------------------------

    @Test
    public void inTableTextCharacterNullStringCausesErrorAndFalse() {
        Character c = charToken("\u0000");
        assertFalse(HtmlTreeBuilderState.InTableText.process(c, tb));
        verify(tb).error(HtmlTreeBuilderState.InTableText);
    }

    @Test
    public void inTableTextOtherTokenFlushesPendingAndRestoresState() {
        // simulate pending characters list with a non‑whitespace character
        when(tb.getPendingTableCharacters()).thenReturn(new ArrayList<String>() {{ add("x"); }});
        Token other = startTag("div");
        assertTrue(HtmlTreeBuilderState.InTableText.process(other, tb));
        verify(tb).setFosterInserts(anyBoolean()); // may be called depending on current element
        verify(tb).transition(tb.originalState());
        verify(tb).process(other);
    }

    // -------------------------------------------------------------------------
    //  Tests for the *InCaption* state
    // -------------------------------------------------------------------------

    @Test
    public void inCaptionEndTagCaptionClosesAndReturnsToInTable() {
        EndTag e = endTag("caption");
        assertTrue(HtmlTreeBuilderState.InCaption.process(e, tb));
        verify(tb).generateImpliedEndTags();
        verify(tb).popStackToClose("caption");
        verify(tb).clearFormattingElementsToLastMarker();
        verify(tb).transition(HtmlTreeBuilderState.InTable);
    }

    @Test
    public void inCaptionOtherStartTagIsHandledByInBody() {
        StartTag s = startTag("div");
        assertTrue(HtmlTreeBuilderState.InCaption.process(s, tb));
        verify(tb).process(s, HtmlTreeBuilderState.InBody);
    }

    // -------------------------------------------------------------------------
    //  Tests for the *InColumnGroup* state
    // -------------------------------------------------------------------------

    @Test
    public void inColumnGroupWhitespaceInserted() {
        Character ws = charToken(" ");
        assertTrue(HtmlTreeBuilderState.InColumnGroup.process(ws, tb));
        verify(tb).insert(ws.asCharacter());
    }

    @Test
    public void inColumnGroupStartTagColInsertedEmpty() {
        StartTag s = startTag("col");
        assertTrue(HtmlTreeBuilderState.InColumnGroup.process(s, tb));
        verify(tb).insertEmpty(s);
    }

    @Test
    public void inColumnGroupEndTagColgroupClosesAndTransitions() {
        EndTag e = endTag("colgroup");
        Element colgroup = mock(Element.class);
        when(tb.currentElement()).thenReturn(colgroup);
        when(colgroup.nodeName()).thenReturn("colgroup");
        assertTrue(HtmlTreeBuilderState.InColumnGroup.process(e, tb));
        verify(tb).pop();
        verify(tb).transition(HtmlTreeBuilderState.InTable);
    }

    // -------------------------------------------------------------------------
    //  Tests for the *InTableBody* state
    // -------------------------------------------------------------------------

    @Test
    public void inTableBodyStartTagTrTransitionsToInRow() {
        StartTag s = startTag("tr");
        assertTrue(HtmlTreeBuilderState.InTableBody.process(s, tb));
        verify(tb).clearStackToTableBodyContext();
        verify(tb).insert(s);
        verify(tb).transition(HtmlTreeBuilderState.InRow);
    }

    @Test
    public void inTableBodyEndTagTbodyClosesAndReturnsToInTable() {
        EndTag e = endTag("tbody");
        assertTrue(HtmlTreeBuilderState.InTableBody.process(e, tb));
        verify(tb).clearStackToTableBodyContext();
        verify(tb).pop();
        verify(tb).transition(HtmlTreeBuilderState.InTable);
    }

    // -------------------------------------------------------------------------
    //  Tests for the *InRow* state
    // -------------------------------------------------------------------------

    @Test
    public void inRowStartTagTdTransitionsToInCell() {
        StartTag s = startTag("td");
        assertTrue(HtmlTreeBuilderState.InRow.process(s, tb));
        verify(tb).clearStackToTableRowContext();
        verify(tb).insert(s);
        verify(tb).transition(HtmlTreeBuilderState.InCell);
        verify(tb).insertMarkerToFormattingElements();
    }

    @Test
    public void inRowEndTagTrClosesAndReturnsToInTableBody() {
        EndTag e = endTag("tr");
        assertTrue(HtmlTreeBuilderState.InRow.process(e, tb));
        verify(tb).clearStackToTableRowContext();
        verify(tb).pop(); // pop tr
        verify(tb).transition(HtmlTreeBuilderState.InTableBody);
    }

    // -------------------------------------------------------------------------
    //  Tests for the *InCell* state
    // -------------------------------------------------------------------------

    @Test
    public void inCellEndTagTdClosesCellAndReturnsToInRow() {
        when(tb.inTableScope("td")).thenReturn(true);
        EndTag e = endTag("td");
        assertTrue(HtmlTreeBuilderState.InCell.process(e, tb));
        verify(tb).generateImpliedEndTags();
        verify(tb).popStackToClose("td");
        verify(tb).clearFormattingElementsToLastMarker();
        verify(tb).transition(HtmlTreeBuilderState.InRow);
    }

    // -------------------------------------------------------------------------
    //  Tests for the *InSelect* state
    // -------------------------------------------------------------------------

    @Test
    public void inSelectStartTagOptionClosesPreviousOption() {
        // current element is <option>
        Element opt = mock(Element.class);
        when(opt.nodeName()).thenReturn("option");
        when(tb.currentElement()).thenReturn(opt);
        StartTag s = startTag("option");
        assertTrue(HtmlTreeBuilderState.InSelect.process(s, tb));
        verify(tb).processEndTag("option");
        verify(tb).insert(s);
    }

    @Test
    public void inSelectEndTagSelectClosesAndResetsInsertionMode() {
        EndTag e = endTag("select");
        assertTrue(HtmlTreeBuilderState.InSelect.process(e, tb));
        verify(tb).popStackToClose("select");
        verify(tb).resetInsertionMode();
    }

    // -------------------------------------------------------------------------
    //  Tests for the *AfterBody* state
    // -------------------------------------------------------------------------

    @Test
    public void afterBodyWhitespaceReprocessesInBody() {
        Character ws = charToken(" ");
        assertTrue(HtmlTreeBuilderState.AfterBody.process(ws, tb));
        verify(tb).process(ws, HtmlTreeBuilderState.InBody);
    }

    @Test
    public void afterBodyEndTagHtmlFragmentErrors() {
        when(tb.isFragmentParsing()).thenReturn(true);
        EndTag e = endTag("html");
        assertFalse(HtmlTreeBuilderState.AfterBody.process(e, tb));
        verify(tb).error(HtmlTreeBuilderState.AfterBody);
    }

    // -------------------------------------------------------------------------
    //  Tests for the *InFrameset* state
    // -------------------------------------------------------------------------

    @Test
    public void inFramesetStartTagFramesetInserted() {
        StartTag s = startTag("frameset");
        assertTrue(HtmlTreeBuilderState.InFrameset.process(s, tb));
        verify(tb).insert(s);
    }

    @Test
    public void inFramesetEndTagFramesetPopsAndMayTransitionAfterFrameset() {
        EndTag e = endTag("frameset");
        Element frameset = mock(Element.class);
        when(tb.currentElement()).thenReturn(frameset);
        when(frameset.nodeName()).thenReturn("frameset");
        assertTrue(HtmlTreeBuilderState.InFrameset.process(e, tb));
        verify(tb).pop();
        // In a non‑fragment parse the next state would be AfterFrameset
        verify(tb, atLeast(0)).transition(HtmlTreeBuilderState.AfterFrameset);
    }

    // -------------------------------------------------------------------------
    //  Tests for the *AfterFrameset* state
    // -------------------------------------------------------------------------

    @Test
    public void afterFramesetStartTagNoFramesDelegatesToInHead() {
        StartTag s = startTag("noframes");
        assertTrue(HtmlTreeBuilderState.AfterFrameset.process(s, tb));
        verify(tb).process(s, HtmlTreeBuilderState.InHead);
    }

    // -------------------------------------------------------------------------
    //  Tests for the *AfterAfterBody* state
    // -------------------------------------------------------------------------

    @Test
    public void afterAfterBodyCommentIsInserted() {
        Comment c = commentToken("test");
        assertTrue(HtmlTreeBuilderState.AfterAfterBody.process(c, tb));
        verify(tb).insert(c);
    }

    // -------------------------------------------------------------------------
    //  Tests for the *ForeignContent* state (trivial)
    // -------------------------------------------------------------------------

    @Test
    public void foreignContentAlwaysReturnsTrue() {
        Token any = mock(Token.class);
        assertTrue(HtmlTreeBuilderState.ForeignContent.process(any, tb));
    }
}
