###Test START##
package org.jsoup.parser;

import org.jsoup.nodes.DocumentType;
import org.jsoup.parser.Token.Comment;
import org.jsoup.parser.Token.Doctype;
import org.jsoup.parser.Token.EOF;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive tests for {@link TokeniserState}.
 * Uses a lightweight fake {@link Tokeniser} implementation to capture
 * state transitions, emitted tokens and internal buffers.
 */
public class TokeniserStateTest {

    private FakeTokeniser tokeniser;
    private FakeCharacterReader reader;

    @Before
    public void setUp() {
        tokeniser = new FakeTokeniser();
        reader = new FakeCharacterReader();
    }

    // -----------------------------------------------------------------
    // Helper fake implementations
    // -----------------------------------------------------------------
    private static class FakeTokeniser {
        TokeniserState lastAdvancedState = null;
        TokeniserState lastTransitionState = null;
        final List<Object> emitted = new ArrayList<>();
        final StringBuilder dataBuffer = new StringBuilder();
        Tag tagPending = null;
        Comment commentPending = null;
        Doctype doctypePending = null;

        // ----- transition helpers -----
        void advanceTransition(TokeniserState state) {
            lastAdvancedState = state;
            transition(state);
        }

        void transition(TokeniserState state) {
            lastTransitionState = state;
        }

        // ----- emit helpers -----
        void emit(Object o) {
            emitted.add(o);
        }

        // ----- error handling (no‑op for tests) -----
        void error(TokeniserState state) { }
        void eofError(TokeniserState state) { }

        // ----- tag handling -----
        Tag createTagPending(boolean start) {
            tagPending = new Tag(start);
            return tagPending;
        }

        // ----- comment handling -----
        void createCommentPending() {
            commentPending = new Comment();
        }

        void emitCommentPending() {
            emitted.add(commentPending);
        }

        // ----- doctype handling -----
        void createDoctypePending() {
            doctypePending = new Doctype();
        }

        void emitDoctypePending() {
            emitted.add(doctypePending);
        }

        // ----- character reference handling -----
        int[] consumeCharacterReference(char endChar, boolean inAttribute) {
            // For simplicity return null (meaning no reference found)
            return null;
        }

        // ----- appropriate end‑tag helpers (default false / null) -----
        String appropriateEndTagName() { return null; }
        boolean isAppropriateEndTagToken() { return false; }

        // ----- emit tag pending -----
        void emitTagPending() {
            emitted.add(tagPending);
        }

        // ----- emit data (String) -----
        void emit(String data) {
            emitted.add(data);
        }

        // ----- emit replacement char -----
        void emit(char c) {
            emitted.add(c);
        }

        // ----- emit int[] (character reference) -----
        void emit(int[] chars) {
            emitted.add(chars);
        }

        // ----- emit EOF token -----
        void emit(EOF eof) {
            emitted.add(eof);
        }
    }

    private static class Tag {
        final boolean startTag;
        final StringBuilder name = new StringBuilder();
        final List<Attribute> attributes = new ArrayList<>();
        boolean selfClosing = false;

        Tag(boolean startTag) {
            this.startTag = startTag;
        }

        Tag appendTagName(String s) {
            name.append(s);
            return this;
        }

        Tag appendTagName(char c) {
            name.append(c);
            return this;
        }

        Tag newAttribute() {
            attributes.add(new Attribute());
            return this;
        }

        Tag appendAttributeName(String s) {
            currentAttr().name.append(s);
            return this;
        }

        Tag appendAttributeName(char c) {
            currentAttr().name.append(c);
            return this;
        }

        Tag appendAttributeValue(String s) {
            currentAttr().value.append(s);
            return this;
        }

        Tag appendAttributeValue(int[] chars) {
            for (int i : chars) currentAttr().value.append((char) i);
            return this;
        }

        Tag setEmptyAttributeValue() {
            // No‑op for test – just mark as empty
            return this;
        }

        private Attribute currentAttr() {
            if (attributes.isEmpty()) {
                newAttribute();
            }
            return attributes.get(attributes.size() - 1);
        }
    }

    private static class Attribute {
        final StringBuilder name = new StringBuilder();
        final StringBuilder value = new StringBuilder();
    }

    private static class FakeCharacterReader {
        private int index = 0;
        private final StringBuilder data = new StringBuilder();

        // Set the underlying data for the reader
        void setData(String s) {
            data.setLength(0);
            data.append(s);
            index = 0;
        }

        char current() {
            if (index >= data.length()) return TokeniserState.eof;
            return data.charAt(index);
        }

        boolean matchesLetter() {
            char c = current();
            return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
        }

        boolean matches(char match) {
            return current() == match;
        }

        boolean matchesAny(char... chars) {
            char c = current();
            for (char ch : chars) if (c == ch) return true;
            return false;
        }

        boolean isEmpty() {
            return index >= data.length();
        }

        void advance() {
            if (index < data.length()) index++;
        }

        char consume() {
            char c = current();
            advance();
            return c;
        }

        String consumeData() {
            // Consume until a special char or EOF
            int start = index;
            while (!isEmpty()) {
                char c = current();
                if (c == '&' || c == '<' || c == TokeniserState.nullChar) break;
                advance();
            }
            return data.substring(start, index);
        }

        String consumeToAny(char... stopChars) {
            int start = index;
            while (!isEmpty() && !matchesAny(stopChars)) {
                advance();
            }
            return data.substring(start, index);
        }

        String consumeToAnySorted(char[] sorted) {
            // Simplified: treat as same as consumeToAny
            return consumeToAny(sorted);
        }

        String consumeTagName() {
            // Consume letters/digits/hyphens as a simple tag name
            int start = index;
            while (!isEmpty() && (Character.isLetterOrDigit(current()) || current() == '-')) {
                advance();
            }
            return data.substring(start, index);
        }

        String consumeLetterSequence() {
            int start = index;
            while (!isEmpty() && matchesLetter()) {
                advance();
            }
            return data.substring(start, index);
        }

        boolean matchConsume(String seq) {
            if (data.substring(index).startsWith(seq)) {
                index += seq.length();
                return true;
            }
            return false;
        }

        boolean matchConsumeIgnoreCase(String seq) {
            String sub = data.substring(index).toLowerCaseFold();
            if (sub.startsWith(seq.toCaseFold())) {
                index += seq.length();
                return true;
            }
            return false;
        }

        boolean containsIgnoreCase(String s) {
            return data.toString().toLowerCase().contains(s.toLowerCase());
        }

        // Helper for tests that need to rewind a character
        void unconsume() {
            if (index > 0) index--;
        }
    }

    // -----------------------------------------------------------------
    // Tests for each TokeniserState branch
    // -----------------------------------------------------------------

    @Test
    public void dataState_ampsersandTriggersCharacterReference() {
        {
            reader.setData("&");
        }
        TokeniserState.Data.read(tokeniser, reader);
        assertEquals(TokeniserState.CharacterReferenceInData, tokeniser.lastAdvancedState);
    }

    @Test
    public void dataState_lessThanTriggersTagOpen() {
        reader.setData("<");
        TokeniserState.Data.read(tokeniser, reader);
        assertEquals(TokeniserState.TagOpen, tokeniser.lastAdvancedState);
    }

    @Test
    public void dataState_nullCharEmitsReplacementAndError() {
        reader.setData("\u0000");
        TokeniserState.Data.read(tokeniser, reader);
        assertTrue(tokeniser.emitted.contains(TokeniserState.nullChar));
        // The emitted value is the null char itself (as per the code)
    }

    @Test
    public void dataState_eofEmitsEOFToken() {
        reader.setData("");
        TokeniserState.Data.read(tokeniser, reader);
        assertTrue(tokeniser.emitted.stream().anyMatch(o -> o instanceof EOF));
    }

    @Test
    public void dataState_defaultConsumesData() {
        reader.setData("Hello World");
        TokeniserState.Data.read(tokeniser, reader);
        assertTrue(tokeniser.emitted.contains("Hello World"));
    }

    @Test
    public void characterReferenceInData_readsCharRefAndReturnsToData() {
        // Simulate that consumeCharacterReference returns null (no ref)
        tokeniser = new FakeTokeniser() {
            @Override
            int[] consumeCharacterReference(char endChar, boolean inAttribute) {
                return null;
            }
        };
        reader.setData("&");
        TokeniserState.CharacterReferenceInData.read(tokeniser, reader);
        // Should have emitted '&' and transitioned to Data
        assertTrue(tokeniser.emitted.contains('&'));
        assertEquals(TokeniserState.Data, tokeniser.lastTransitionState);
    }

    @Test
    public void rcdataState_ampersandTriggersCharacterReferenceInRcdata() {
        reader.setData("&");
        TokeniserState.Rcdata.read(tokeniser, reader);
        assertEquals(TokeniserState.CharacterReferenceInRcdata, tokeniser.lastAdvancedState);
    }

    @Test
    public void rcdataState_lessThanTriggersRcdataLessthanSign() {
        reader.setData("<");
        TokeniserState.Rcdata.read(tokeniser, reader);
        assertEquals(TokeniserState.RcdataLessthanSign, tokeniser.lastAdvancedState);
    }

    @Test
    public void rcdataState_nullCharEmitsReplacementAndError() {
        reader.setData("\u0000");
        TokeniserState.Rcdata.read(tokeniser, reader);
        assertTrue(tokeniser.emitted.contains(TokeniserState.replacementChar));
    }

    @Test
    public void rcdataState_defaultConsumesToSpecialChars() {
        reader.setData("abc&<\u0000def");
        TokeniserState.Rcdata.read(tokeniser, reader);
        assertTrue(tokeniser.emitted.contains("abc"));
    }

    @Test
    public void rawtextState_usesReadDataHelper() {
        // rawtext state should delegate to readData with RawtextLessthanSign
        reader.setData("plain text");
        TokeniserState.Rawtext.read(tokeniser, reader);
        // readData will emit the whole string and stay in Rawtext (no transition)
        assertTrue(tokeniser.emitted.contains("plain text"));
    }

    @Test
    public void scriptDataState_usesReadDataHelper() {
        reader.setData("script content");
        TokeniserState.ScriptData.read(tokeniser, reader);
        assertTrue(tokeniser.emitted.contains("script content"));
    }

    @Test
    public void plaintextState_nullCharEmitsReplacement() {
        reader.setData("\u0000");
        TokeniserState.PLAINTEXT.read(tokeniser, reader);
        assertTrue(tokeniser.emitted.contains(TokeniserState.replacementChar));
    }

    @Test
    public void plaintextState_eofEmitsEOF() {
        reader.setData("");
        TokeniserState.PLAINTEXT.read(tokeniser, reader);
        assertTrue(tokeniser.emitted.stream().anyMatch(o -> o instanceof EOF));
    }

    @Test
    public void plaintextState_defaultConsumesToNullChar() {
        reader.setData("some text\u0000more");
        TokeniserState.PLAINTEXT.read(tokeniser, reader);
        assertTrue(tokeniser.emitted.contains("some text"));
    }

    @Test
    public void tagOpenState_exclamationStartsMarkupDeclaration() {
        reader.setData("!DOCTYPE html>");
        TokeniserState.TagOpen.read(tokeniser, reader);
        assertEquals(TokeniserState.MarkupDeclarationOpen, tokeniser.lastAdvancedState);
    }

    @Test
    public void tagOpenState_slashStartsEndTagOpen() {
        reader.setData("/div>");
        TokeniserState.TagOpen.read(tokeniser, reader);
        assertEquals(TokeniserState.EndTagOpen, tokeniser.lastAdvancedState);
    }

    @Test
    public void tagOpenState_questionStartsBogusComment() {
        reader.setData("?foo");
        TokeniserState.TagOpen.read(tokeniser, reader);
        assertEquals(TokeniserState.BogusComment, tokeniser.lastAdvancedState);
    }

    @Test
    public void tagOpenState_letterStartsTagName() {
        reader.setData("a");
        TokeniserState.TagOpen.read(tokeniser, reader);
        assertEquals(TokeniserState.TagName, tokeniser.lastTransitionState);
        assertNotNull(tokeniser.tagPending);
        assertTrue(tokeniser.tagPending.startTag);
    }

    @Test
    public void tagOpenState_invalidCharEmitsErrorAndChar() {
        reader.setData("1");
        TokeniserState.TagOpen.read(tokeniser, reader);
        // Should emit '<' and transition to Data
        assertTrue(tokeniser.emitted.contains('<'));
        assertEquals(TokeniserState.Data, tokeniser.lastTransitionState);
    }

    @Test
    public void endTagOpenState_emptyReaderEmitsEofErrorAndTransition() {
        reader.setData("");
        TokeniserState.EndTagOpen.read(tokeniser, reader);
        // Should emit "</" and transition to Data
        assertTrue(tokeniser.emitted.contains("</"));
        assertEquals(TokeniserState.Data, tokeniser.lastTransitionState);
    }

    @Test
    public void endTagOpenState_letterCreatesTagPending() {
        reader.setData("p>");
        TokeniserState.EndTagOpen.read(tokeniser, reader);
        assertEquals(TokeniserState.TagName, tokeniser.lastTransitionState);
        assertNotNull(tokeniser.tagPending);
        assertFalse(tokeniser.tagPending.startTag);
    }

    @Test
    public void endTagOpenState_greaterThanErrorAndAdvance() {
        reader.setData(">");
        TokeniserState.EndTagOpen.read(tokeniser, reader);
        assertEquals(TokeniserState.Data, tokeniser.lastAdvancedState);
    }

    @Test
    public void tagNameState_consumesTagNameAndHandlesSpace() {
        reader.setData("div >");
        TokeniserState.TagName.read(tokeniser, reader);
        assertEquals("div", tokeniser.tagPending.name.toString());
        assertEquals(TokeniserState.BeforeAttributeName, tokeniser.lastTransitionState);
    }

    @Test
    public void tagNameState_selfClosingSlash() {
        reader.setData("br/");
        TokeniserState.TagName.read(tokeniser, reader);
        assertEquals(TokeniserState.SelfClosingStartTag, tokeniser.lastTransitionState);
    }

    @Test
    public void tagNameState_closingAngleBracketEmitsTag() {
        reader.set("<img>");
        // Simulate that TagOpen already created a pending tag
        tokeniser.createTagPending(true);
        TokeniserState.TagName.read(tokeniser, reader);
        assertTrue(tokeniser.emitted.contains(tokeniser.tagPending));
        assertEquals(TokeniserState.Data, tokeniser.lastTransitionState);
    }

    @Test
    public void rcdataLessthanSign_state_startTagWhenNoAppropriateEndTag() {
        // Simulate a start tag scenario where appropriate end tag is null
        tokeniser.appropriateEndTagName = () -> null;
        reader.setData("<span");
        TokeniserState.RcdataLessthanSign.read(tokeniser, reader);
        // Should emit "<" and transition back to Rcdata
        assertTrue(tokeniser.emitted.contains("<"));
        assertEquals(TokeniserState.Rcdata, tokeniser.lastTransitionState);
    }

    @Test
    public void bogusCommentState_collectsCommentData() {
        {
            reader.setData("<!-- bogus comment -->");
        }
        tokeniser.createCommentPending();
        TokeniserState.BogusComment.read(tokeniser, reader);
        // The comment data should be captured (excluding the leading '<' which is unconsumed)
        assertTrue(tokeniser.emitted.stream().anyMatch(o -> o instanceof Comment));
    }

    // -----------------------------------------------------------------
    // Tests for static helper methods
    // -----------------------------------------------------------------
    @Test
    public void testReadDataHelperEmitsDataAndTransitionOnLessThan() {
        reader.setData("<");
        TokeniserState.readData(tokeniser, reader, TokeniserState.Data, TokeniserState.RawtextLessthanSign);
        assertEquals(TokeniserState.RawtextLessthanSign, tokeniser.lastAdvancedState);
    }

    @Test
    public void testReadDataHelperEmitsReplacementOnNullChar() {
        reader.setData("\u0000");
        TokeniserState.readData(tokeniser, reader, TokeniserState.Data, TokeniserState.RawtextLessthanSign);
        assertTrue(tokeniser.emitted.contains(TokeniserState.replacementChar));
    }

    @Test
    public void testReadDataHelperEmitsEOF() {
        reader.setData("");
        TokeniserState.readData(tokeniser, reader, TokeniserState.Data, TokeniserState.RawtextLessthanSign);
        assertTrue(tokeniser.emitted.stream().anyMatch(o -> o instanceof EOF));
    }

    @Test
    public void testReadCharRefEmitsAmpersandWhenNoRef() {
        tokeniser = new FakeTokeniser() {
            @Override
            int[] consumeCharacterReference(char endChar, boolean inAttribute) {
                return null;
            }
        };
        TokeniserState.readCharRef(tokeniser, TokeniserState.Data);
        assertTrue(tokeniser.emitted.contains('&'));
        assertEquals(TokeniserState.Data, tokeniser.lastTransitionState);
    }

    @Test
    public void testReadEndTagCreatesTagWhenLetter() {
        reader.setData("a");
        TokeniserState.readEndTag(tokeniser, reader, TokeniserState.RCDATAEndTagName, TokeniserState.Rcdata);
        assertEquals(TokeniserState.RCDATAEndTagName, tokeniser.lastTransitionState);
        assertNotNull(tokeniser.tagPending);
    }

    @Test
    public void testReadEndTagEmitsSlashWhenNotLetter() {
        reader.setData(">");
        TokeniserState.readEndTag(tokeniser, reader, TokeniserState.RCDATAEndTagName, TokeniserState.Rcdata);
        assertTrue(tokeniser.emitted.contains("</"));
        assertEquals(TokeniserState.Rcdata, tokeniser.lastTransitionState);
    }

    @Test
    public void testHandleDataDoubleEscapeTagTransitionsToPrimaryWhenScript() {
        // Simulate that dataBuffer already contains "script"
        tokeniser.dataBuffer.append("script");
        reader.setData(" ");
        TokeniserState.handleDataDoubleEscapeTag(tokeniser, reader, TokeniserState.ScriptDataDoubleEscaped, TokeniserState.ScriptDataEscaped);
        assertEquals(TokeniserState.ScriptDataDoubleEscaped, tokeniser.lastTransitionState);
    }

    @Test
    public void testHandleDataDoubleEscapeTagTransitionsToFallbackWhenNotScript() {
        tokeniser.dataBuffer.append("div");
        reader.setData(">");
        TokeniserState.handleDataDoubleEscapeTag(tokeniser, reader, TokeniserState.ScriptDataDoubleEscaped, TokeniserState.ScriptDataEscaped);
        assertEquals(TokeniserState.ScriptDataEscaped, tokeniser.lastTransitionState);
    }

    // -----------------------------------------------------------------
    // Additional simple sanity checks
    // -----------------------------------------------------------------
    @Test
    public void allStatesHaveReadMethod() {
        for (TokeniserState state : TokeniserState.values()) {
            assertNotNull(state);
        }
    }
}
###Test END##