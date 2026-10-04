###Test START##
```java
package org.jsoup.parser;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.DocumentType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Comprehensive tests for {@link TokeniserState}. The tests use Mockito to mock the
 * {@link Tokeniser} and {@link CharacterReader} collaborators, allowing us to verify
 * that each state triggers the correct transitions, emits the right tokens and
 * reports errors when required.
 *
 * The test suite covers:
 * <ul>
 *   <li>Typical data consumption (Data, Rcdata, Rawtext, ScriptData, PLAINTEXT)</li>
 *   <li>Tag parsing (TagOpen, TagName, SelfClosingStartTag)</li>
 *   <li>Attribute handling (BeforeAttributeName, AttributeName, AttributeValue_doubleQuoted)</li>
 *   <li>Comment parsing (CommentStart, Comment, CommentEnd, BogusComment)</li>
 *   <li>Doctype handling (Doctype, BeforeDoctypeName, DoctypeName, AfterDoctypeName, BogusDoctype)</li>
 *   <li>CDATA sections (CdataSection)</li>
 *   <li>Error scenarios (null characters, unexpected EOF, illegal characters)</li>
 * </ul>
 *
 * The tests are written for JUnit 4 and use the {@code MockitoJUnitRunner}
 * to initialise mocks.
 */
@RunWith(MockitoJUnitRunner.class)
public class TokeniserStateTest {

    @Mock
    private Tokeniser tokeniser;

    @Mock
    private CharacterReader reader;

    /** Helper to capture emitted objects (String, Token, etc.). */
    private final List<Object> emitted = new ArrayList<>();

    @Before
    public void setUp() {
        // All emit(...) calls on the tokeniser will add the argument to the emitted list.
        doAnswer(invocation -> {
            emitted.add(invocation.getArgument(0));
            return null;
        }).when(tokeniser).emit(any());

        // reset emitted list before each test
        emitted.clear();
    }

    /** -------------------------------------------------------------------------
     *  Data state tests
     * -------------------------------------------------------------------------
     */
    @Test
    public void dataState_EmitsCharacterDataUntilSpecialChar() {
        // Simulate input "hello world" (no special chars)
        when(reader.current()).thenReturn('h'); // first call enters default case
        when(reader.consumeData()).thenReturn("hello world");
        when(reader.current()).thenReturn(CharacterReader.EOF); // after consumeData() the next char is EOF

        TokeniserState.Data.read(tokeniser, reader);

        verify(tokeniser).emit("hello world");
        verify(tokeniser).emit(isA(Token.EOF.class));
    }

    @Test
    public void dataState_TransitionOnAmpersand() {
        when(reader.current()).thenReturn('&');

        TokeniserState.Data.read(tokeniser, reader);

        verify(tokeniser).advanceTransition(TokeniserState.CharacterReferenceInData);
        verifyNoMoreInteractions(tokeniser);
    }

    @Test
    public void dataState_TransitionOnLessThan() {
        when(reader.current()).thenReturn('<');

        TokeniserState.Data.read(tokeniser, reader);

        verify(tokeniser).advanceTransition(TokeniserState.TagOpen);
    }

    @Test
    public void dataState_NullCharEmitsErrorAndReplacement() {
        when(reader.current()).thenReturn(TokeniserState.nullChar);
        when(reader.consume()).thenReturn('\u0000');

        TokeniserState.Data.read(tokeniser, reader);

        verify(tokeniser).error(TokeniserState.Data);
        verify(tokeniser).emit('\u0000');
    }

    /** -------------------------------------------------------------------------
     *  TagOpen state tests
     * -------------------------------------------------------------------------
     */
    @Test
    public void tagOpen_StartTagCreatesPendingTagAndTransitionsToTagName() {
        when(reader.current()).thenReturn('a'); // a letter
        when(reader.matchesLetter()).thenReturn(true);

        TokeniserState.TagOpen.read(tokeniser, reader);

        verify(tokeniser).createTagPending(true);
        verify(tokeniser).transition(TokeniserState.TagName);
    }

    @Test
    public void tagOpen_EndTagCreatesPendingTagAndTransitionsToTagName() {
        when(reader.current()).thenReturn('/');
        when(reader.matchesLetter()).thenReturn(false);
        when(reader.matches('>')).thenReturn(false);
        when(reader.matches('!')).thenReturn(false);
        when(reader.matches('?')).thenReturn(false);

        TokeniserState.TagOpen.read(tokeniser, reader);

        verify(tokeniser).advanceTransition(TokeniserState.EndTagOpen);
    }

    @Test
    public void tagOpen_ExclamationStartsMarkupDeclaration() {
        when(reader.current()).thenReturn('!');
        TokeniserState.TagOpen.read(tokeniser, reader);
        verify(tokeniser).advanceTransition(TokeniserState.MarkupDeclarationOpen);
    }

    @Test
    public void tagOpen_InvalidCharEmitsErrorAndFallsBackToData() {
        when(reader.current()).thenReturn('1'); // not a letter, not a special char
        when(reader.matchesLetter()).thenReturn(false);

        TokeniserState.TagOpen.read(tokeniser, reader);

        InOrder inOrder = inOrder(tokeniser);
        inOrder.verify(tokeniser).error(TokeniserState.TagOpen);
        inOrder.verify(tokeniser).emit('<');
        inOrder.verify(tokeniser).transition(TokeniserState.Data);
    }

    /** -------------------------------------------------------------------------
     *  TagName state tests
     * -------------------------------------------------------------------------
     */
    @Test
    public void tagName_HandlesWhitespaceTransitionToBeforeAttributeName() {
        when(reader.consumeTagName()).thenReturn("div");
        when(reader.consume()).thenReturn(' '); // whitespace after tag name

        TokeniserState.TagName.read(tokeniser, reader);

        verify(tokeniser).tagPending.appendTagName("div");
        verify(tokeniser).transition(TokeniserState.BeforeAttributeName);
    }

    @Test
    public void tagName_SelfClosingTagTransition() {
        when(reader.consumeTagName()).thenReturn("br");
        when(reader.consume()).thenReturn('/');

        TokeniserState.TagName.read(tokeniser, reader);

        verify(tokeniser).tagPending.appendTagName("br");
        verify(tokeniser).transition(TokeniserState.SelfClosingStartTag);
    }

    @Test
    public void tagName_EndTagEmitsAndReturnsToData() {
        when(reader.consumeTagName()).thenReturn("p");
        when(reader.consume()).thenReturn('>');

        TokeniserState.TagName.read(tokeniser, reader);

        verify(tokeniser).tagPending.appendTagName("p");
        verify(tokeniser).emitTagPending();
        verify(tokeniser).transition(TokeniserState.Data);
    }

    /** -------------------------------------------------------------------------
     *  AttributeValue_doubleQuoted tests
     * -------------------------------------------------------------------------
     */
    @Test
    public void attributeValueDoubleQuoted_ParsesSimpleValueAndClosesQuote() {
        // Simulate value "hello"
        when(reader.consumeToAny(TokeniserState.attributeDoubleValueCharsSorted)).thenReturn("hello");
        when(reader.consume()).thenReturn('"'); // closing quote

        TokeniserState.AttributeValue_doubleQuoted.read(tokeniser, reader);

        verify(tokeniser).tagPending.appendAttributeValue("hello");
        verify(tokeniser).transition(TokeniserState.AfterAttributeValue_quoted);
    }

    @Test
    public void attributeValueDoubleQuoted_EntityReferenceIsConsumed() {
        when(reader.consumeToAny(TokeniserState.attributeDoubleValueCharsSorted)).thenReturn("");
        when(reader.consume()).thenReturn('&');

        // Simulate a character reference that resolves to a single code point (e.g. &amp; -> '&')
        int[] ref = new int[]{'&'};
        when(tokeniser.consumeCharacterReference('"', true)).thenReturn(ref);

        TokeniserState.AttributeValue_doubleQuoted.read(tokeniser, reader);

        verify(tokeniser).tagPending.appendAttributeValue(ref);
        // after handling entity the state stays in AttributeValue_doubleQuoted (no transition)
        verify(tokeniser, never()).transition(any());
    }

    @Test
    public void attributeValueDoubleQuoted_NullCharTriggersErrorAndReplacement() {
        when(reader.consumeToAny(TokeniserState.attributeDoubleValueCharsSorted)).thenReturn("");
        when(reader.consume()).thenReturn(TokeniserState.nullChar);

        TokeniserState.AttributeValue_doubleQuoted.read(tokeniser, reader);

        verify(tokeniser).error(TokeniserState.AttributeValue_doubleQuoted);
        verify(tokeniser).tagPending.appendAttributeValue(TokeniserState.replacementChar);
    }

    /** -------------------------------------------------------------------------
     *  Comment parsing tests
     * -------------------------------------------------------------------------
     */
    @Test
    public void commentStart_DashTransition() {
        when(reader.consume()).thenReturn('-');
        TokeniserState.CommentStart.read(tokeniser, reader);
        verify(tokeniser).transition(TokeniserState.CommentStartDash);
    }

    @Test
    public void commentStart_NullCharErrorAndTransitionToComment() {
        when(reader.consume()).thenReturn(TokeniserState.nullChar);
        TokeniserState.CommentStart.read(tokeniser, reader);
        InOrder inOrder = inOrder(tokeniser);
        inOrder.verify(tokeniser).error(TokeniserState.CommentStart);
        inOrder.verify(tokeniser).commentPending.data.append(TokeniserState.replacementChar);
        inOrder.verify(tokeniser).transition(TokeniserState.Comment);
    }

    @Test
    public void comment_EndDashThenEnd_EmitsCommentAndReturnsToData() {
        // Simulate reading "-->"
        // 1. CommentStartDash reads first '-'
        when(reader.consume()).thenReturn('-');
        TokeniserState.CommentStartDash.read(tokeniser, reader);
        verify(tokeniser).transition(TokeniserState.CommentStartDash); // stays in same state for double dash

        // 2. CommentEndDash reads second '-'
        when(reader.consume()).thenReturn('-');
        TokeniserState.CommentEndDash.read(tokeniser, reader);
        verify(tokeniser).transition(TokeniserState.CommentEnd);

        // 3. CommentEnd reads '>'
        when(reader.consume()).thenReturn('>');
        TokeniserState.CommentEnd.read(tokeniser, reader);
        verify(tokeniser).emitCommentPending();
        verify(tokeniser).transition(TokeniserState.Data);
    }

    /** -------------------------------------------------------------------------
     *  Doctype handling tests
     * -------------------------------------------------------------------------
     */
    @Test
    public void doctype_UnexpectedCharForcesQuirksAndTransitionsToData() {
        when(reader.consume()).thenReturn('x'); // not whitespace nor '>'
        TokeniserState.Doctype.read(tokeniser, reader);
        InOrder inOrder = inOrder(tokeniser);
        inOrder.verify(tokeniser).error(TokeniserState.Doctype);
        inOrder.verify(tokeniser).transition(TokeniserState.BeforeDoctypeName);
    }

    @Test
    public void beforeDoctypeName_WhitespaceIsIgnored() {
        when(reader.consume()).thenReturn(' ');
        TokeniserState.BeforeDoctypeName.read(tokeniser, reader);
        // No transition, just consumes whitespace
        verify(tokeniser, never()).transition(any());
    }

    @Test
    public void beforeDoctypeName_LetterStartsDoctypeName() {
        when(reader.matchesLetter()).thenReturn(true);
        TokeniserState.BeforeDoctypeName.read(tokeniser, reader);
        verify(tokeniser).createDoctypePending();
        verify(tokeniser).transition(TokeniserState.DoctypeName);
    }

    @Test
    public void doctypeName_EndsOnGreaterThan() {
        when(reader.matchesLetter()).thenReturn(false);
        when(reader.consume()).thenReturn('>');
        TokeniserState.DoctypeName.read(tokeniser, reader);
        verify(tokeniser).emitDoctypePending();
        verify(tokeniser).transition(TokeniserState.Data);
    }

    @Test
    public void afterDoctypeName_RecognisesPublicKeyword() {
        when(reader.isEmpty()).thenReturn(false);
        when(reader.matchesAny('\t', '\n', '\r', '\f', ' ')).thenReturn(false);
        when(reader.matches('>')).thenReturn(false);
        when(reader.matchConsumeIgnoreCase(DocumentType.PUBLIC_KEY)).thenReturn(true);
        TokeniserState.AfterDoctypeName.read(tokeniser, reader);
        verify(tokeniser).doctypePending.pubSysKey = DocumentType.PUBLIC_KEY;
        verify(tokeniser).transition(TokeniserState.AfterDoctypePublicKeyword);
    }

    @Test
    public void bogusDoctype_EndsOnGreaterThan() {
        when(reader.consume()).thenReturn('>');
        TokeniserState.BogusDoctype.read(tokeniser, reader);
        verify(tokeniser).emitDoctypePending();
        verify(tokeniser).transition(TokeniserState.Data);
    }

    /** -------------------------------------------------------------------------
     *  CdataSection tests
     * -------------------------------------------------------------------------
     */
    @Test
    public void cdataSection_ConsumesUntilEndMarkerAndEmitsData() {
        when(reader.consumeTo("]]>")).thenReturn("CDATA CONTENT");
        when(reader.matchConsume("]]>")).thenReturn(true);

        TokeniserState.CdataSection.read(tokeniser, reader);

        verify(tokeniser).emit("CDATA CONTENT");
        verify(tokeniser).transition(TokeniserState.Data);
    }

    @Test
    public void cdataSection_EOFWithoutEndMarkerStaysInCdata() {
        when(reader.consumeTo("]]>")).thenReturn("partial");
        when(reader.matchConsume("]]>")).thenReturn(false);
        when(reader.isEmpty()).thenReturn(true);

        TokeniserState.CdataSection.read(tokeniser, reader);

        verify(tokeniser).emit("partial");
        // No transition because buffer underrun (stay in CdataSection)
        verify(tokeniser, never()).transition(TokeniserState.Data);
    }

    /** -------------------------------------------------------------------------
     *  Error handling for null characters in various states
     * -------------------------------------------------------------------------
     */
    @Test
    public void rcdataState_NullCharTriggersErrorAndReplacement() {
        when(reader.current()).thenReturn(TokeniserState.nullChar);
        TokeniserState.Rcdata.read(tokeniser, reader);
        InOrder inOrder = inOrder(tokeniser);
        inOrder.verify(tokeniser).error(TokeniserState.Rcdata);
        inOrder.verify(reader).advance();
        inOrder.verify(tokeniser).emit(TokeniserState.replacementChar);
    }

    @Test
    public void plaintextState_NullCharTriggersErrorAndReplacement() {
        when(reader.current()).thenReturn(TokeniserState.nullChar);
        TokeniserState.PLAINTEXT.read(tokeniser, reader);
        InOrder inOrder = inOrder(tokeniser);
        inOrder.verify(tokeniser).error(TokeniserState.PLAINTEXT);
        inOrder.verify(reader).advance();
        inOrder.verify(tokeniser).emit(TokeniserState.replacementChar);
    }

    /** -------------------------------------------------------------------------
     *  SelfClosingStartTag tests
     * -------------------------------------------------------------------------
     */
    @Test
    public void selfClosingStartTag_ValidCloseEmitsTagAndReturnsToData() {
        when(reader.consume()).thenReturn('>');
        TokeniserState.SelfClosingStartTag.read(tokeniser, reader);
        InOrder inOrder = inOrder(tokeniser);
        inOrder.verify(tokeniser).tagPending.selfClosing = true;
        inOrder.verify(tokeniser).emitTagPending();
        inOrder.verify(tokeniser).transition(TokeniserState.Data);
    }

    @Test
    public void selfClosingStartTag_EOFTriggersErrorAndDataTransition() {
        when(reader.consume()).thenReturn(CharacterReader.EOF);
        TokeniserState.SelfClosingStartTag.read(tokeniser, reader);
        verify(tokeniser).error(TokeniserState.SelfClosingStartTag);
        verify(tokeniser).transition(TokeniserState.Data);
    }

    /** -------------------------------------------------------------------------
     *  BogusComment test (ensures rewind and emit)
     * -------------------------------------------------------------------------
     */
    @Test
    public void bogusComment_RewindsAndEmitsComment() {
        // simulate that the reader will return characters "abc>"
        when(reader.consumeTo('>')).thenReturn("abc");
        // after reading the comment the bogus flag should be true
        TokeniserState.BogusComment.read(tokeniser, reader);
        ArgumentCaptor<Token.Comment> captor = ArgumentCaptor.forClass(Token.Comment.class);
        verify(tokeniser).emit(captor.capture());
        Token.Comment comment = captor.getValue();
        assertTrue("Bogus flag should be true", comment.bogus);
        assertEquals("abc", comment.data.toString());
        verify(tokeniser).advanceTransition(TokeniserState.Data);
        verify(reader).unconsume(); // ensure rewind happened
    }

    /** -------------------------------------------------------------------------
     *  Helper verification for transitions that use advanceTransition
     * -------------------------------------------------------------------------
     */
    private void verifyAdvanceTransition(TokeniserState expected) {
        verify(tokeniser).advanceTransition(expected);
    }

    /** -------------------------------------------------------------------------
     *  Additional sanity test: ensure all enum constants have a non‑null read method.
     * -------------------------------------------------------------------------
     */
    @Test
    public void allStatesImplementReadMethod() throws Exception {
        for (TokeniserState state : TokeniserState.values()) {
            assertNotNull("State " + state + " must implement read()", state.getClass()
                    .getDeclaredMethod("read", Tokeniser.class, CharacterReader.class));
        }
    }
}
```
###Test END##