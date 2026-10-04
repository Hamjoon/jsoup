package org.jsoup.parser;

import org.junit.Test;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Minimal stubs for the parser infrastructure that the {@link TokeniserState} enum
 * depends on.  They record the interactions that the state logic performs so that
 * the tests can assert the correct behaviour (transitions, emitted tokens, errors,
 * etc.) without pulling in the full jsoup library.
 */
public class TokeniserStateTest {

    /** Simple token hierarchy used by the tests. */
    static class Token {
        static class EOF { }
        static class Comment {
            boolean bogus = false;
            final StringBuilder data = new StringBuilder();
        }
    }

    /** Minimal {@link CharacterReader} implementation sufficient for the tests. */
    static class MockCharacterReader extends CharacterReader {
        private final String input;
        private int pos = 0;

        MockCharacterReader(String input) {
            super(input);
            this.input = input;
        }

        @Override
        public char current() {
            return pos >= input.length() ? eof : input.charAt(pos);
        }

        @Override
        public char consume() {
            char c = current();
            pos++;
            return c;
        }

        @Override
        public void advance() {
            pos++;
        }

        @Override
        public boolean matches(char c) {
            return current() == c;
        }

        @Override
        public boolean matchesLetter() {
            char c = current();
            return Character.isLetter(c);
        }

        @Override
        public boolean matchesAny(char... cs) {
            char c = current();
            for (char ch : cs) {
                if (c == ch) return true;
            }
            return false;
        }

        @Override
        public boolean matchConsume(String seq) {
            if (input.startsWith(seq, pos)) {
                pos += seq.length();
                return true;
            }
            return false;
        }

        @Override
        public boolean matchConsumeIgnoreCase(String seq) {
            int len = seq.length();
            if (pos + len > input.length()) return false;
            String sub = input.substring(pos, pos + len);
            if (sub.equalsIgnoreCase(seq)) {
                pos += len;
                return true;
            }
            return false;
        }

        @Override
        public boolean isEmpty() {
            return pos >= input.length();
        }

        @Override
        public String consumeData() {
            String data = input.substring(pos);
            pos = input.length();
            return data;
        }

        @Override
        public String consumeToAny(char... cs) {
            int start = pos;
            while (!isEmpty() && !contains(current(), cs)) {
                pos++;
            }
            return input.substring(start, pos);
        }

        @Override
        public String consumeTo(String seq) {
            int start = pos;
            while (!isEmpty() && !input.startsWith(seq, pos)) {
                pos++;
            }
            return input.substring(start, pos);
        }

        @Override
        public String consumeTagName() {
            int start = pos;
            while (!isEmpty() && (Character.isLetterOrDigit(current()) || current() == '-')) {
                pos++;
            }
            return input.substring(start, pos);
        }

        @Override
        public String consumeLetterSequence() {
            int start = pos;
            while (!isEmpty() && Character.isLetter(current())) {
                pos++;
            }
            return input.substring(start, pos);
        }

        @Override
        public String consumeToAnySorted(char[] sorted) {
            // For the purposes of the test the sorted variant behaves the same as consumeToAny.
            return consumeToAny(sorted);
        }

        private boolean contains(char c, char[] arr) {
            for (char ch : arr) {
                if (c == ch) return true;
            }
            return false;
        }
    }

    /** Minimal {@link Tokeniser} implementation that records what the state machine does. */
    static class MockTokeniser extends Tokeniser {
        final List<Object> emitted = new ArrayList<>();
        final List<TokeniserState> errors = new ArrayList<>();
        final List<TokeniserState> eofErrors = new ArrayList<>();
        TokeniserState lastTransition = null;
        final StringBuilder dataBuffer = new StringBuilder();

        MockTag tagPending = null;
        Token.Comment commentPending = null;
        DocumentType doctypePending = null; // not used directly in the tests

        MockTokeniser() {
            super(null);
        }

        @Override
        void advanceTransition(TokeniserState state) {
            lastTransition = state;
        }

        @Override
        void transition(TokeniserState state) {
            lastTransition = state;
        }

        @Override
        void emit(Object o) {
            emitted.add(o);
        }

        @Override
        void error(TokeniserState state) {
            errors.add(state);
        }

        @Override
        void eofError(TokeniserState state) {
            eofErrors.add(state);
        }

        @Override
        void createTagPending(boolean startTag) {
            tagPending = new MockTag(startTag);
        }

        @Override
        Tag createTagPending(boolean startTag) {
            tagPending = new MockTag(startTag);
            return tagPending;
        }

        @Override
        void emitTagPending() {
            emitted.add(tagPending);
        }

        @Override
        void createCommentPending() {
            commentPending = new Token.Comment();
        }

        @Override
        void emitCommentPending() {
            emitted.add(commentPending);
        }

        @Override
        void createDoctypePending() {
            // No‑op for this test suite – doctype handling is not exercised directly.
        }

        @Override
        void emitDoctypePending() {
            // No‑op for this test suite.
        }

        @Override
        void createTempBuffer() {
            // No‑op – the tests do not inspect the temporary buffer.
        }

        @Override
        int[] consumeCharacterReference(char end, boolean inAttribute) {
            // Simplify: always return null so the state logic falls back to emitting '&'.
            return null;
        }

        // --------------------------------------------------------------------
        // Minimal Tag implementation used by the mock tokeniser.
        // --------------------------------------------------------------------
        static class MockTag extends Tokeniser.Tag {
            final boolean startTag;
            final StringBuilder tagName = new StringBuilder();
            boolean selfClosing = false;

            MockTag(boolean startTag) {
                super(null);
                this.startTag = startTag;
            }

            @Override
            void appendTagName(String s) {
                tagName.append(s);
            }

            @Override
            void appendTagName(char c) {
                tagName.append(c);
            }

            @Override
            void newAttribute() {
                // No‑op – attribute handling is not verified in these tests.
            }

            @Override
            void appendAttributeName(String s) { }

            @Override
            void appendAttributeName(char c) { }

            @Override
            void setEmptyAttributeValue() { }

            @Override
            void appendAttributeValue(String s) { }

            @Override
            void appendAttributeValue(char[] chars) { }

            @Override
            void appendAttributeValue(int[] codepoints) { }

            @Override
            public String toString() {
                return (startTag ? "<" : "</") + tagName + (selfClosing ? "/>" : ">");
            }
        }
    }

    private MockTokeniser tokeniser;
    private MockCharacterReader reader;

    @Before
    public void setUp() {
        tokeniser = new MockTokeniser();
    }

    // --------------------------------------------------------------------
    // Data state tests
    // --------------------------------------------------------------------
    @Test
    public void dataState_EmitCharacters() {
        reader = new MockCharacterReader("hello");
        TokeniserState.Data.read(tokeniser, reader);
        assertEquals(1, tokeniser.emitted.size());
        assertEquals("hello", tokeniser.emitted.get(0));
        assertNull(tokeniser.lastTransition);
    }

    @Test
    public void dataState_CharacterReferenceTransition() {
        reader = new MockCharacterReader("&");
        TokeniserState.Data.read(tokeniser, reader);
        assertEquals(TokeniserState.CharacterReferenceInData, tokeniser.lastTransition);
    }

    @Test
    public void dataState_TagOpenTransition() {
        reader = new MockCharacterReader("<");
        TokeniserState.Data.read(tokeniser, reader);
        assertEquals(TokeniserState.TagOpen, tokeniser.lastTransition);
    }

    @Test
    public void dataState_NullCharErrorAndEmit() {
        reader = new MockCharacterReader("\u0000");
        TokeniserState.Data.read(tokeniser, reader);
        assertTrue(tokeniser.errors.contains(TokeniserState.Data));
        assertEquals(1, tokeniser.emitted.size());
        assertEquals('\u0000', tokeniser.emitted.get(0));
    }

    @Test
    public void dataState_EOF() {
        reader = new MockCharacterReader("");
        TokeniserState.Data.read(tokeniser, reader);
        assertEquals(1, tokeniser.emitted.size());
        assertTrue(tokeniser.emitted.get(0) instanceof Token.EOF);
    }

    // --------------------------------------------------------------------
    // TagOpen state tests
    // --------------------------------------------------------------------
    @Test
    public void tagOpen_LetterStartsTag() {
        reader = new MockCharacterReader("a");
        TokeniserState.TagOpen.read(tokeniser, reader);
        assertNotNull(tokeniser.tagPending);
        assertTrue(tokeniser.tagPending.startTag);
        assertEquals(TokeniserState.TagName, tokeniser.lastTransition);
    }

    @Test
    public void tagOpen_ExclamationMark() {
        reader = new MockCharacterReader("!");
        TokeniserState.TagOpen.read(tokeniser, reader);
        assertEquals(TokeniserState.MarkupDeclarationOpen, tokeniser.lastTransition);
    }

    @Test
    public void tagOpen_SlashStartsEndTag() {
        reader = new MockCharacterReader("/");
        TokeniserState.TagOpen.read(tokeniser, reader);
        assertEquals(TokeniserState.EndTagOpen, tokeniser.lastTransition);
    }

    @Test
    public void tagOpen_QuestionMarkBogusComment() {
        reader = new MockCharacterReader("?");
        TokeniserState.TagOpen.read(tokeniser, reader);
        assertEquals(TokeniserState.BogusComment, tokeniser.lastTransition);
    }

    @Test
    public void tagOpen_OtherCharacterErrorAndEmit() {
        reader = new MockCharacterReader("1");
        TokeniserState.TagOpen.read(tokeniser, reader);
        assertTrue(tokeniser.errors.contains(TokeniserState.TagOpen));
        assertEquals(1, tokeniser.emitted.size());
        assertEquals('<', tokeniser.emitted.get(0));
        assertEquals(TokeniserState.Data, tokeniser.lastTransition);
    }

    // --------------------------------------------------------------------
    // EndTagOpen state tests
    // --------------------------------------------------------------------
    @Test
    public void endTagOpen_EOF() {
        reader = new MockCharacterReader("");
        TokeniserState.EndTagOpen.read(tokeniser, reader);
        assertTrue(tokeniser.eofErrors.contains(TokeniserState.EndTagOpen));
        assertEquals(1, tokeniser.emitted.size());
        assertEquals("</", tokeniser.emitted.get(0));
        assertEquals(TokeniserState.Data, tokeniser.lastTransition);
    }

    @Test
    public void endTagOpen_LetterCreatesTagAndGoesToTagName() {
        reader = new MockCharacterReader("div");
        TokeniserState.EndTagOpen.read(tokeniser, reader);
        assertNotNull(tokeniser.tagPending);
        assertFalse(tokeniser.tagPending.startTag);
        assertEquals(TokeniserState.TagName, tokeniser.lastTransition);
    }

    @Test
    public void endTagOpen_GreaterThanErrorAndAdvance() {
        reader = new MockCharacterReader(">");
        TokeniserState.EndTagOpen.read(tokeniser, reader);
        assertTrue(tokeniser.errors.contains(TokeniserState.EndTagOpen));
        assertEquals(TokeniserState.Data, tokeniser.lastTransition);
    }

    @Test
    public void endTagOpen_OtherStartsBogusComment() {
        reader = new MockCharacterReader("!");
        TokeniserState.EndTagOpen.read(tokeniser, reader);
        assertTrue(tokeniser.errors.contains(TokeniserState.EndTagOpen));
        assertEquals(TokeniserState.BogusComment, tokeniser.lastTransition);
    }

    // --------------------------------------------------------------------
    // TagName state tests
    // --------------------------------------------------------------------
    @Test
    public void tagName_RegularTagCloses() {
        reader = new MockCharacterReader("span>");
        tokeniser.createTagPending(true);
        TokeniserState.TagName.read(tokeniser, reader);
        // after consumeTagName we have "span", then the '>' character triggers emitTagPending()
        assertEquals(1, tokeniser.emitted.size());
        assertEquals("<span>", tokeniser.emitted.get(0).toString());
        assertEquals(TokeniserState.Data, tokeniser.lastTransition);
    }

    @Test
    public void tagName_SelfClosingTagTransition() {
        reader = new MockCharacterReader("br/");
        tokeniser.createTagPending(true);
        TokeniserState.TagName.read(tokeniser, reader);
        // The '/' should cause a transition to SelfClosingStartTag
        assertEquals(TokeniserState.SelfClosingStartTag, tokeniser.lastTransition);
    }

    @Test
    public void tagName_WhitespaceTransitionToBeforeAttributeName() {
        reader = new MockCharacterReader("img ");
        tokeniser.createTagPending(true);
        TokeniserState.TagName.read(tokeniser, reader);
        assertEquals(TokeniserState.BeforeAttributeName, tokeniser.lastTransition);
    }

    // --------------------------------------------------------------------
    // Rcdata state tests (null character handling)
    // --------------------------------------------------------------------
    @Test
    public void rcdataState_NullCharErrorAndEmitReplacement() {
        reader = new MockCharacterReader("\u0000");
        TokeniserState.Rcdata.read(tokeniser, reader);
        assertTrue(tokeniser.errors.contains(TokeniserState.Rcdata));
        assertEquals(1, tokeniser.emitted.size());
        assertEquals('\uFFFD', tokeniser.emitted.get(0)); // replacement character
    }

    // --------------------------------------------------------------------
    // PLAINTEXT state tests (null character handling)
    // --------------------------------------------------------------------
    @Test
    public void plaintextState_NullCharErrorAndEmitReplacement() {
        reader = new MockCharacterReader("\u0000");
        TokeniserState.PLAINTEXT.read(tokeniser, reader);
        assertTrue(tokeniser.errors.contains(TokeniserState.PLAINTEXT));
        assertEquals(1, tokeniser.emitted.size());
        assertEquals('\uFFFD', tokeniser.emitted.get(0));
    }

    // --------------------------------------------------------------------
    // BeforeAttributeName state tests
    // --------------------------------------------------------------------
    @Test
    public void beforeAttributeName_WhitespaceIsIgnored() {
        reader = new MockCharacterReader("   ");
        TokeniserState.BeforeAttributeName.read(tokeniser, reader);
        // No transition change because whitespace is simply ignored.
        assertNull(tokeniser.lastTransition);
    }

    @Test
    public void beforeAttributeName_SlashGoesToSelfClosingStartTag() {
        reader = new MockCharacterReader("/");
        TokeniserState.BeforeAttributeName.read(tokeniser, reader);
        assertEquals(TokeniserState.SelfClosingStartTag, tokeniser.lastTransition);
    }

    @Test
    public void beforeAttributeName_GreaterThanEmitsTagAndReturnsToData() {
        // Prepare a pending tag so that emitTagPending() has something to emit.
        tokeniser.createTagPending(true);
        reader = new MockCharacterReader(">");
        TokeniserState.BeforeAttributeName.read(tokeniser, reader);
        assertEquals(1, tokeniser.emitted.size());
        assertEquals(tokeniser.tagPending, tokeniser.emitted.get(0));
        assertEquals(TokeniserState.Data, tokeniser.lastTransition);
    }

    // --------------------------------------------------------------------
    // BogusComment state test (basic comment capture)
    // --------------------------------------------------------------------
    @Test
    public void bogusComment_CapturesUntilGreaterThan() {
        tokeniser.createCommentPending();
        reader = new MockCharacterReader("abc>");
        TokeniserState.BogusComment.read(tokeniser, reader);
        assertEquals(1, tokeniser.emitted.size());
        Token.Comment comment = (Token.Comment) tokeniser.emitted.get(0);
        assertTrue(comment.bogus);
        assertEquals("abc", comment.data.toString());
        assertEquals(TokeniserState.Data, tokeniser.lastTransition);
    }

    // --------------------------------------------------------------------
    // CdataSection state test (simple capture)
    // --------------------------------------------------------------------
    @Test
    public void cdataSection_ConsumesUntilTerminator() {
        reader = new MockCharacterReader("some data]]>more");
        TokeniserState.CdataSection.read(tokeniser, reader);
        assertEquals(1, tokeniser.emitted.size());
        assertEquals("some data", tokeniser.emitted.get(0));
        // After the terminator the state should have moved back to Data.
        assertEquals(TokeniserState.Data, tokeniser.lastTransition);
    }

    // --------------------------------------------------------------------
    // Helper to expose the private static replacement character for assertions.
    // --------------------------------------------------------------------
    private static char replacementChar() {
        // The HTML parser uses U+FFFD as the replacement character.
        return '\uFFFD';
    }
}
