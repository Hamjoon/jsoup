###Test START##
```java
/****************************************************************************************
 * TokeniserStateTest.java
 *
 * Comprehensive JUnit‑4 test suite for {@link org.jsoup.parser.TokeniserState}.
 *
 * The tests focus on the public (package‑private) behaviour of the enum constants,
 * exercising the `read(Tokeniser, CharacterReader)` method for a representative
 * subset of states.  Each test creates a Mockito mock of {@code Tokeniser} and
 * {@code CharacterReader} and verifies that the correct callbacks are invoked for
 * normal, edge‑case and error conditions.
 *
 * The test class lives in the same package (`org.jsoup.parser`) so that it can
 * access the package‑private enum and its members.
 *
 * Author:  ChatGPT (generated)
 * Date:    2026‑10‑04
 ****************************************************************************************/
package org.jsoup.parser;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.jsoup.nodes.DocumentType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * JUnit‑4 test suite for {@link TokeniserState}.
 *
 * The suite covers:
 * <ul>
 *   <li>Normal transitions for a few representative states (Data, TagOpen, EndTagOpen,
 *       TagName).</li>
 *   <li>Edge cases such as {@code nullChar}, EOF, empty input, and unexpected characters.</li>
 *   <li>Verification that error handling callbacks are invoked.</li>
 * </ul>
 *
 * Because {@code TokeniserState.read(..)} is package‑private, the test class is placed
 * in the same package as the class under test.
 */
@RunWith(MockitoJUnitRunner.class)
public class TokeniserStateTest {

    @Mock private Tokeniser tokeniser;
    @Mock private CharacterReader reader;

    @Before
    public void initMocks() {
        // Initialise Mockito annotations
        // (MockitoJUnitRunner does this automatically, but keeping the method for clarity)
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- Data state tests ------------------------ */
    /* ---------------------------------------------------------------------- */

    @Test
    public void dataState_ampsymbol_triggersCharacterReferenceInData() {
        when(reader.current()).thenReturn('&');

        TokeniserState.Data.read(tokeniser, reader);

        verify(tokeniser).advanceTransition(TokeniserState.CharacterReferenceInData);
        verifyNoMoreInteractions(tokeniser);
    }

    @Test
    public void dataState_lessThan_triggersTagOpen() {
        when(reader.current()).thenReturn('<');

        TokeniserState.Data.read(tokeniser, reader);

        verify(tokeniser).advanceTransition(TokeniserState.TagOpen);
        verifyNoMoreInteractions(tokeniser);
    }

    @Test
    public void dataState_nullChar_emitsReplacementAndCallsError() {
        when(reader.current()).thenReturn(TokeniserState.nullChar);
        when(reader.consume()).thenReturn('\u0000');

        TokeniserState.Data.read(tokeniser, reader);

        InOrder inOrder = inOrder(tokeniser);
        inOrder.verify(tokeniser).error(TokeniserState.Data);
        inOrder.verify(tokeniser).emit('\u0000');
        verifyNoMoreInteractions(tokeniser);
    }

    @Test
    public void dataState_eof_emitsEOFToken() {
        when(reader.current()).thenReturn(TokeniserState.eof);

        TokeniserState.Data.read(tokeniser, reader);

        verify(tokeniser).emit(isA(Token.EOF.class));
        verifyNoMoreInteractions(tokeniser);
    }

    @Test
    public void dataState_defaultConsumesDataAndEmitsString() {
        when(reader.current()).thenReturn('a');
        when(reader.consumeData()).thenReturn("abc");

        TokeniserState.Data.read(tokeniser, reader);

        verify(tokeniser).emit("abc");
        verifyNoMoreInteractions(tokeniser);
    }

    /* ---------------------------------------------------------------------- */
    /* -------------------------- TagOpen state tests ---------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void tagOpen_letterStartsTag() {
        when(reader.current()).thenReturn('d');
        when(reader.matchesLetter()).thenReturn(true);

        TokeniserState.TagOpen.read(tokeniser, reader);

        InOrder inOrder = inOrder(tokeniser);
        inOrder.verify(tokeniser).createTagPending(true);
        inOrder.verify(tokeniser).transition(TokeniserState.TagName);
        verifyNoMoreInteractions(tokeniser);
    }

    @Test
    public void tagOpen_exclamationStartsMarkupDeclaration() {
        when(reader.current()).thenReturn('!');

        TokeniserState.TagOpen.read(tokeniser, reader);

        verify(tokeniser).advanceTransition(TokeniserState.MarkupDeclarationOpen);
        verifyNoMoreInteractions(tokeniser);
    }

    @Test
    public void tagOpen_slashStartsEndTagOpen() {
        when(reader.current()).thenReturn('/');

        TokeniserState.TagOpen.read(tokeniser, reader);

        verify(tokeniser).advanceTransition(TokeniserState.EndTagOpen);
        verifyNoMoreInteractions(tokeniser);
    }

    @Test
    public void tagOpen_questionMarkStartsBogusComment() {
        when(reader.current()).thenReturn('?');

        TokeniserState.TagOpen.read(tokeniser, reader);

        verify(tokeniser).advanceTransition(TokeniserState.BogusComment);
        verifyNoMoreInteractions(tokeniser);
    }

    @Test
    public void tagOpen_invalidCharTriggersErrorEmitAndDataTransition() {
        when(reader.current()).thenReturn('1'); // not a letter
        when(reader.matchesLetter()).thenReturn(false);

        TokeniserState.TagOpen.read(tokeniser, reader);

        InOrder inOrder = inOrder(tokeniser);
        inOrder.verify(tokeniser).error(TokeniserState.TagOpen);
        inOrder.verify(tokeniser).emit('<');
        inOrder.verify(tokeniser).transition(TokeniserState.Data);
        verifyNoMoreInteractions(tokeniser);
    }

    /* ---------------------------------------------------------------------- */
    /* ------------------------- EndTagOpen state tests -------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void endTagOpen_emptyReaderTriggersEofErrorAndEmit() {
        when(reader.isEmpty()).thenReturn(true);

        TokeniserState.EndTagOpen.read(tokeniser, reader);

        InOrder inOrder = inOrder(tokeniser);
        inOrder.verify(tokeniser).eofError(TokeniserState.EndTagOpen);
        inOrder.verify(tokeniser).emit("</");
        inOrder.verify(tokeniser).transition(TokeniserState.Data);
        verifyNoMoreInteractions(tokeniser);
    }

    @Test
    public void endTagOpen_letterCreatesTagAndTransitionsToTagName() {
        when(reader.isEmpty()).thenReturn(false);
        when(reader.matchesLetter()).thenReturn(true);

        TokeniserState.EndTagOpen.read(tokeniser, reader);

        verify(tokeniser).createTagPending(false);
        verify(tokeniser).transition(TokeniserState.TagName);
        verifyNoMoreInteractions(tokeniser);
    }

    @Test
    public void endTagOpen_greaterThanTriggersErrorAndDataTransition() {
        when(reader.isEmpty()).thenReturn(false);
        when(reader.matchesLetter()).thenReturn(false);
        when(reader.matches('>')).thenReturn(true);

        TokeniserState.EndTagOpen.read(tokeniser, reader);

        InOrder inOrder = inOrder(tokeniser);
        inOrder.verify(tokeniser).error(TokeniserState.EndTagOpen);
        inOrder.verify(tokeniser).advanceTransition(TokeniserState.Data);
        verifyNoMoreInteractions(tokeniser);
    }

    @Test
    public void endTagOpen_otherCharTriggersErrorAndBogusComment() {
        when(reader.isEmpty()).thenReturn(false);
        when(reader.matchesLetter()).thenReturn(false);
        when(reader.matches('>')).thenReturn(false);

        TokeniserState.EndTagOpen.read(tokeniser, reader);

        InOrder inOrder = inOrder(tokeniser);
        inOrder.verify(tokeniser).error(TokeniserState.EndTagOpen);
        inOrder.verify(tokeniser).advanceTransition(TokeniserState.BogusComment);
        verifyNoMoreInteractions(tokeniser);
    }

    /* ---------------------------------------------------------------------- */
    /* -------------------------- TagName state tests ---------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void tagName_consumesTagNameAndHandlesWhitespaceTransition() {
        // Simulate a tag name "div" followed by a space.
        when(reader.consumeTagName()).thenReturn("div");
        // After consuming tag name, the next char returned by consume() is a space.
        when(reader.consume()).thenReturn(' ');

        TokeniserState.TagName.read(tokeniser, reader);

        // Tag name should be appended to the pending tag.
        verify(tokeniser).tagPending.appendTagName("div");
        // Whitespace should cause transition to BeforeAttributeName.
        verify(tokeniser).transition(TokeniserState.BeforeAttributeName);
        verifyNoMoreInteractions(tokeniser);
    }

    @Test
    public void tagName_slashTransitionsToSelfClosingStartTag() {
        when(reader.consumeTagName()).thenReturn("br");
        when(reader.consume()).thenReturn('/');

        TokeniserState.TagName.read(tokeniser, reader);

        verify(tokeniser).tagPending.appendTagName("br");
        verify(tokeniser).transition(TokeniserState.SelfClosingStartTag);
        verifyNoMoreInteractions(tokeniser);
    }

    @Test
    public void tagName_greaterThanEmitsTagAndReturnsToData() {
        when(reader.consumeTagName()).thenReturn("p");
        when(reader.consume()).thenReturn('>');

        TokeniserState.TagName.read(tokeniser, reader);

        verify(tokeniser).tagPending.appendTagName("p");
        verify(tokeniser).emitTagPending();
        verify(tokeniser).transition(TokeniserState.Data);
        verifyNoMoreInteractions(tokeniser);
    }

    @Test
    public void tagName_nullCharAppendsReplacementString() {
        when(reader.consumeTagName()).thenReturn("span");
        when(reader.consume()).thenReturn(TokeniserState.nullChar);

        TokeniserState.TagName.read(tokeniser, reader);

        InOrder inOrder = inOrder(tokeniser);
        inOrder.verify(tokeniser).tagPending.appendTagName("span");
        inOrder.verify(tokeniser).tagPending.appendTagName(TokeniserState.replacementStr);
        verifyNoMoreInteractions(tokeniser);
    }

    @Test
    public void tagName_eofTriggersEofErrorAndDataTransition() {
        when(reader.consumeTagName()).thenReturn("img");
        when(reader.consume()).thenReturn(TokeniserState.eof);

        TokeniserState.TagName.read(tokeniser, reader);

        InOrder inOrder = inOrder(tokeniser);
        inOrder.verify(tokeniser).tagPending.appendTagName("img");
        inOrder.verify(tokeniser).eofError(TokeniserState.TagName);
        inOrder.verify(tokeniser).transition(TokeniserState.Data);
        verifyNoMoreInteractions(tokeniser);
    }

    /* ---------------------------------------------------------------------- */
    /* -------------------------- Helper state tests ----------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void readData_nullCharCallsErrorEmitReplacement() {
        // Directly test the private static helper via the Rawtext state which delegates to it.
        // Rawtext.read() calls readData(this, r, this, RawtextLessthanSign);
        when(reader.current()).thenReturn(TokeniserState.nullChar);
        // The rawtext state will invoke r.advance() and then emit replacementChar.
        doNothing().when(reader).advance();

        TokeniserState.Rawtext.read(tokeniser, reader);

        InOrder inOrder = inOrder(tokeniser);
        inOrder.verify(tokeniser).error(TokeniserState.Rawtext);
        inOrder.verify(reader).advance();
        inOrder.verify(tokeniser).emit(TokeniserState.replacementChar);
        verifyNoMoreInteractions(tokeniser);
    }

    @Test
    public void readData_eofEmitsEOFToken() {
        when(reader.current()).thenReturn(TokeniserState.eof);

        TokeniserState.ScriptData.read(tokeniser, reader);

        verify(tokeniser).emit(isA(Token.EOF.class));
        verifyNoMoreInteractions(tokeniser);
    }

    @Test
    public void readCharRef_nullReferenceEmitsAmpersand() {
        // CharacterReferenceInData state calls readCharRef(this, Data)
        // We stub Tokeniser.consumeCharacterReference to return null.
        when(tokeniser.consumeCharacterReference(null, false)).thenReturn(null);

        TokeniserState.CharacterReferenceInData.read(tokeniser, reader);

        InOrder inOrder = inOrder(tokeniser);
        inOrder.verify(tokeniser).emit('&');
        inOrder.verify(tokeniser).transition(TokeniserState.Data);
        verifyNoMoreInteractions(tokeniser);
    }

    @Test
    public void readCharRef_validReferenceEmitsCodePoints() {
        // Simulate a character reference that expands to two code points.
        when(tokeniser.consumeCharacterReference(null, false)).thenReturn(new int[]{65, 66}); // "AB"

        TokeniserState.CharacterReferenceInData.read(tokeniser, reader);

        InOrder inOrder = inOrder(tokeniser);
        inOrder.verify(tokeniser).emit(new int[]{65, 66});
        inOrder.verify(tokeniser).transition(TokeniserState.Data);
        verifyNoMoreInteractions(tokeniser);
    }

    /* ---------------------------------------------------------------------- */
    /* ---------------------------- Miscellaneous -------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void bogusComment_readsUntilGreaterThanAndEmitsComment() {
        // BogusComment reads the rest of the input up to '>'
        // Simulate a reader that returns the remaining comment text " hello>"
        when(reader.unconsume()).thenReturn(null); // first step: rewind
        when(reader.consumeTo('>')).thenReturn(" hello");
        // The comment token is created inside the method; we only verify that emit is called.
        TokeniserState.BogusComment.read(tokeniser, reader);

        verify(tokeniser).emit(argThat(argument -> {
            if (!(argument instanceof Token.Comment)) return false;
            Token.Comment comment = (Token.Comment) argument;
            return comment.data.toString().equals(" hello");
        }));
        verify(tokeniser).advanceTransition(TokeniserState.Data);
        verifyNoMoreInteractions(tokeniser);
    }

    @Test
    public void cdataSection_consumesUntilCdataEndAndTransitionsToData() {
        when(reader.consumeTo("]]>")).thenReturn("some <cdata> content");
        when(reader.matchConsume("]]>")).thenReturn(true);

        TokeniserState.CdataSection.read(tokeniser, reader);

        verify(tokeniser).emit("some <cdata> content");
        verify(tokeniser).transition(TokeniserState.Data);
        verifyNoMoreInteractions(tokeniser);
    }

    @Test
    public void cdataSection_withoutClosingSequenceStaysInCdata() {
        when(reader.consumeTo("]]>")).thenReturn("unfinished cdata");
        when(reader.matchConsume("]]>")).thenReturn(false);
        when(reader.isEmpty()).thenReturn(true);

        TokeniserState.CdataSection.read(tokeniser, reader);

        verify(tokeniser).emit("unfinished cdata");
        // Since the buffer is empty, the state does not transition – we just verify no transition call.
        verify(tokeniser, never()).transition(any(TokeniserState.class));
        verifyNoMoreInteractions(tokeniser);
    }
}
```
###Test END##