package org.jsoup.parser;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal stub of {@link Tokeniser} that records the actions performed by {@link TokeniserState#read}.
 * Only the methods that are exercised by the tests are implemented.
 */
class TokeniserStub extends Tokeniser {
    // records
    TokeniserState lastAdvancedState = null;
    TokeniserState lastTransitionState = null;
    List<Object> emitted = new ArrayList<>();
    List<String> errors = new ArrayList<>();

    // simplified tag pending representation
    static class TagStub {
        boolean startTag;
        StringBuilder tagName = new StringBuilder();
        boolean selfClosing = false;
        List<String> attributes = new ArrayList<>();

        TagStub(boolean startTag) {
            this.startTag = startTag;
        }

        void appendTagName(String s) { tagName.append(s); }
        void appendTagName(char c) { tagName.append(c); }
    }

    TagStub tagPending = null;
    StringBuilder dataBuffer = new StringBuilder();

    TokeniserStub() {
        super(null, null, null);
    }

    @Override
    void advanceTransition(TokeniserState state) {
        lastAdvancedState = state;
    }

    @Override
    void transition(TokeniserState state) {
        lastTransitionState = state;
    }

    @Override
    void emit(Object o) {
        emitted.add(o);
    }

    @Override
    void error(TokeniserState state) {
        errors.add(state.name());
    }

    @Override
    void eofError(TokeniserState state) {
        errors.add("EOF:" + state.name());
    }

    @Override
    void createTagPending(boolean start) {
        tagPending = new TagStub(start);
    }

    @Override
    Tag createTagPending(boolean start) { // for calls that expect a Tag return
        createTagPending(start);
        return null; // not used in tests
    }

    // helper to reset between tests
    void reset() {
        lastAdvancedState = null;
        lastTransitionState = null;
        emitted.clear();
        errors.clear();
        tagPending = null;
        dataBuffer.setLength(0);
    }
}

/**
 * Very small {@link CharacterReader} implementation that works on a mutable String.
 * Only the methods exercised by the tests are implemented.
 */
class CharacterReaderStub extends CharacterReader {
    private final String input;
    private int pos = 0;

    CharacterReaderStub(String input) {
        super(input);
        this.input = input;
    }

    @Override
    public char current() {
        if (pos >= input.length())
            return eof;
        return input.charAt(pos);
    }

    @Override
    public char consume() {
        char c = current();
        pos++;
        return c;
    }

    @Override
    public void advance() {
        if (pos < input.length())
            pos++;
    }

    @Override
    public boolean matches(char c) {
        return current() == c;
    }

    @Override
    public boolean matchesLetter() {
        char c = current();
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    @Override
    public boolean isEmpty() {
        return pos >= input.length();
    }

    @Override
    public String consumeData() {
        int start = pos;
        while (!isEmpty()) {
            char c = current();
            if (c == '&' || c == '<' || c == TokeniserState.nullChar)
                break;
            pos++;
        }
        return input.substring(start, pos);
    }

    @Override
    public String consumeToAny(char... chars) {
        int start = pos;
        outer:
        while (!isEmpty()) {
            char c = current();
            for (char stop : chars) {
                if (c == stop) break outer;
            }
            pos++;
        }
        return input.substring(start, pos);
    }

    @Override
    public String consumeTagName() {
        // consume consecutive letters/digits/hyphens (simplified)
        int start = pos;
        while (!isEmpty()) {
            char c = current();
            if (Character.isLetterOrDigit(c) || c == '-' || c == ':')
                pos++;
            else
                break;
        }
        return input.substring(start, pos);
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
        if (input.regionMatches(true, pos, seq, 0, seq.length())) {
            pos += seq.length();
            return true;
        }
        return false;
    }

    @Override
    public boolean matches(char... vals) {
        char cur = current();
        for (char v : vals) if (cur == v) return true;
        return false;
    }

    @Override
    public void unconsume() {
        if (pos > 0) pos--;
    }
}

/**
 * JUnit‑4 test suite for {@link TokeniserState}.
 */
public class TokeniserStateTest {

    private TokeniserStub tokeniser;
    private CharacterReaderStub reader;

    @Before
    public void setUp() {
        tokeniser = new TokeniserStub();
    }

    /** -----------------------------------------------------------------------
     *  List of public methods (generated from the enum source)
     *  -----------------------------------------------------------------------
     *  public static TokeniserState[] values()
     *  public static TokeniserState valueOf(String name)
     *  public final String name()
     *  public final int ordinal()
     *  public int compareTo(TokeniserState o)
     *  public boolean equals(Object other)
     *  public int hashCode()
     *  public String toString()
     *  -----------------------------------------------------------------------
     */

    @Test
    public void testValuesAndValueOf() {
        TokeniserState[] vals = TokeniserState.values();
        assertTrue(vals.length > 0);
        for (TokeniserState s : vals) {
            assertEquals(s, TokeniserState.valueOf(s.name()));
        }
    }

    /** -------------------  Data state basic behaviour ------------------- */
    @Test
    public void dataState_ampsymbolTriggersCharacterReferenceInData() {
        reader = new CharacterReaderStub("&foo");
        TokeniserState.Data.read(tokeniser, reader);
        assertEquals(TokeniserState.CharacterReferenceInData, tokeniser.lastAdvancedState);
    }

    @Test
    public void dataState_lessThanTriggersTagOpen() {
        reader = new CharacterReaderStub("<div");
        TokeniserState.Data.read(tokeniser, reader);
        assertEquals(TokeniserState.TagOpen, tokeniser.lastAdvancedState);
    }

    @Test
    public void dataState_nullCharEmitsErrorAndReplacement() {
        // construct a string containing the actual null char
        reader = new CharacterReaderStub("\u0000abc");
        TokeniserState.Data.read(tokeniser, reader);
        assertTrue(tokeniser.errors.contains("Data"));
        assertEquals(1, tokeniser.emitted.size());
        assertEquals('\uFFFD', tokeniser.emitted.get(0)); // replacementChar is '\uFFFD'
    }

    @Test
    public void dataState_eofEmitsEOFToken() {
        reader = new CharacterReaderStub("");
        TokeniserState.Data.read(tokeniser, reader);
        assertEquals(1, tokeniser.emitted.size());
        assertTrue(tokeniser.emitted.get(0) instanceof Token.EOF);
    }

    @Test
    public void dataState_defaultConsumesData() {
        reader = new CharacterReaderStub("hello world");
        TokeniserState.Data.read(tokeniser, reader);
        assertEquals(1, tokeniser.emitted.size());
        assertEquals("hello world", tokeniser.emitted.get(0));
    }

    /** -------------------  TagOpen state ------------------- */
    @Test
    public void tagOpen_exclamationStartsMarkupDeclaration() {
        reader = new CharacterReaderStub("!DOCTYPE html");
        TokeniserState.TagOpen.read(tokeniser, reader);
        assertEquals(TokeniserState.MarkupDeclarationOpen, tokeniser.lastAdvancedState);
    }

    @Test
    public void tagOpen_slashStartsEndTagOpen() {
        reader = new CharacterReaderStub("/p>");
        TokeniserState.TagOpen.read(tokeniser, reader);
        assertEquals(TokeniserState.EndTagOpen, tokeniser.lastAdvancedState);
    }

    @Test
    public void tagOpen_questionStartsBogusComment() {
        reader = new CharacterReaderStub("?xml");
        TokeniserState.TagOpen.read(tokeniser, reader);
        assertEquals(TokeniserState.BogusComment, tokeniser.lastAdvancedState);
    }

    @Test
    public void tagOpen_letterCreatesStartTagAndTransitionsToTagName() {
        reader = new CharacterReaderStub("a");
        TokeniserState.TagOpen.read(tokeniser, reader);
        assertNotNull(tokeniser.tagPending);
        assertTrue(tokeniser.tagPending.startTag);
        assertEquals(TokeniserState.TagName, tokeniser.lastTransitionState);
    }

    @Test
    public void tagOpen_invalidCharEmitsErrorAndFallsBackToData() {
        reader = new CharacterReaderStub("1");
        TokeniserState.TagOpen.read(tokeniser, reader);
        assertTrue(tokeniser.errors.contains("TagOpen"));
        assertEquals(1, tokeniser.emitted.size());
        assertEquals('<', tokeniser.emitted.get(0));
        assertEquals(TokeniserState.Data, tokeniser.lastTransitionState);
    }

    /** -------------------  EndTagOpen edge cases ------------------- */
    @Test
    public void endTagOpen_emptyReaderTriggersEofErrorAndEmitsSlash() {
        reader = new CharacterReaderStub("");
        TokeniserState.EndTagOpen.read(tokeniser, reader);
        assertTrue(tokeniser.errors.contains("EOF:EndTagOpen"));
        assertEquals(1, tokeniser.emitted.size());
        assertEquals("</", tokeniser.emitted.get(0));
        assertEquals(TokeniserState.Data, tokeniser.lastTransitionState);
    }

    @Test
    public void endTagOpen_letterCreatesEndTagAndTransitionsToTagName() {
        reader = new CharacterReaderStub("p");
        TokeniserState.EndTagOpen.read(tokeniser, reader);
        assertNotNull(tokeniser.tagPending);
        assertFalse(tokeniser.tagPending.startTag);
        assertEquals(TokeniserState.TagName, tokeniser.lastTransitionState);
    }

    @Test
    public void endTagOpen_greaterThanEmitsErrorAndAdvancesToData() {
        reader = new CharacterReaderStub(">");
        TokeniserState.EndTagOpen.read(tokeniser, reader);
        assertTrue(tokeniser.errors.contains("EndTagOpen"));
        assertEquals(TokeniserState.Data, tokeniser.lastAdvancedState);
    }

    @Test
    public void endTagOpen_otherCharTriggersBogusComment() {
        reader = new CharacterReaderStub("!");
        TokeniserState.EndTagOpen.read(tokeniser, reader);
        assertTrue(tokeniser.errors.contains("EndTagOpen"));
        assertEquals(TokeniserState.BogusComment, tokeniser.lastAdvancedState);
    }

    /** -------------------  TagName handling of '>' ------------------- */
    @Test
    public void tagName_onGreaterThanEmitsTagAndReturnsToData() {
        // simulate reader positioned after "<div"
        reader = new CharacterReaderStub(">rest");
        // first we need a pending tag (created by previous state)
        tokeniser.createTagPending(true);
        TokeniserState.TagName.read(tokeniser, reader);
        // after reading tag name, the '>' should be consumed
        assertEquals(1, tokeniser.emitted.size());
        // the emitted object is a Tag – we only verify that transition happened
        assertEquals(TokeniserState.Data, tokeniser.lastTransitionState);
    }

    /** -------------------  BeforeAttributeName whitespace handling ------------------- */
    @Test
    public void beforeAttributeName_ignoresWhitespace() {
        reader = new CharacterReaderStub("   a");
        TokeniserState.BeforeAttributeName.read(tokeniser, reader);
        // no transition should happen because whitespace is ignored
        assertNull(tokeniser.lastTransitionState);
        // next char is 'a' which should cause transition to AttributeName
        TokeniserState.BeforeAttributeName.read(tokeniser, reader);
        assertEquals(TokeniserState.AttributeName, tokeniser.lastTransitionState);
    }

    /** -------------------  AttributeValue_doubleQuoted empty value ------------------- */
    @Test
    public void attributeValueDoubleQuoted_emptyValueSetsEmptyAndStopsAtQuote() {
        // Input: "" (immediately closing quote)
        reader = new CharacterReaderStub("\"");
        // Need a pending tag
        tokeniser.createTagPending(true);
        TokeniserState.AttributeValue_doubleQuoted.read(tokeniser, reader);
        // The stub tag does not have a concrete method to check empty flag,
        // but we can verify that transition moves to AfterAttributeValue_quoted
        assertEquals(TokeniserState.AfterAttributeValue_quoted, tokeniser.lastTransitionState);
    }

    /** -------------------  CdataSection consumes until delimiter ------------------- */
    @Test
    public void cdataSection_consumesUntilDelimiterAndReturnsToData() {
        reader = new CharacterReaderStub("some data]]>more");
        TokeniserState.CdataSection.read(tokeniser, reader);
        // first emitted token should be the data before delimiter
        assertEquals(1, tokeniser.emitted.size());
        assertEquals("some data", tokeniser.emitted.get(0));
        // after consuming delimiter, state should be Data
        assertEquals(TokeniserState.Data, tokeniser.lastTransitionState);
    }

    /** -------------------  Edge case: null character in ScriptDataEscaped ------------------- */
    @Test
    public void scriptDataEscaped_nullCharTriggersErrorAndReplacement() {
        reader = new CharacterReaderStub("\u0000-");
        TokeniserState.ScriptDataEscaped.read(tokeniser, reader);
        assertTrue(tokeniser.errors.contains("ScriptDataEscaped"));
        assertEquals('\uFFFD', tokeniser.emitted.get(0));
        // after handling null, the next character '-' should be emitted in the next state,
        // but we only verify that no exception was thrown and error recorded.
    }

    /** -------------------  Edge case: EOF in SelfClosingStartTag ------------------- */
    @Test
    public void selfClosingStartTag_EOFTriggersErrorAndReturnsToData() {
        reader = new CharacterReaderStub("");
        TokeniserState.SelfClosingStartTag.read(tokeniser, reader);
        assertTrue(tokeniser.errors.contains("EOF:SelfClosingStartTag"));
        assertEquals(TokeniserState.Data, tokeniser.lastTransitionState);
    }

    /** -------------------  Helper to reset tokeniser between tests ------------------- */
    private void resetTokeniser() {
        tokeniser.reset();
    }
}
