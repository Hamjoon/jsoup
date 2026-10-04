package org.jsoup.parser;

import org.jsoup.helper.CharacterReader;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Entities;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Tokeniser}.
 *
 * The tests are grouped by the logical area they verify:
 *
 * 1.  Emission buffering (emit(String) / read()).
 * 2.  State transitions (getState / transition / advanceTransition).
 * 3.  Character reference consumption (numeric & named, error cases, attribute context).
 * 4.  Tag handling (createTagPending, emitTagPending, isAppropriateEndTagToken).
 * 5.  Doctype / comment handling (creation & emission).
 * 6.  Error reporting (error, eofError, characterReferenceError).
 * 7.  Utility methods (unescapeEntities, currentNodeInHtmlNS, appropriateEndTagName).
 *
 * The {@link ParseErrorList} used by the Tokeniser is configured with a
 * capacity large enough to capture all generated errors, allowing us to
 * assert on its contents.
 */
public class TokeniserTest {

    private static final int ERROR_CAPACITY = 100;

    private CharacterReader reader;
    private ParseErrorList errors;
    private Tokeniser tokeniser;

    /** Helper to create a fresh Tokeniser for each test. */
    private void initTokeniser(String data) {
        reader = new CharacterReader(data);
        errors = new ParseErrorList(ERROR_CAPACITY);
        tokeniser = new Tokeniser(reader, errors);
    }

    @Before
    public void setUp() {
        // default empty initialisation – individual tests will call initTokeniser(...)
    }

    /* --------------------------------------------------------------
     * 1. Emission buffering (emit(String) / read())
     * -------------------------------------------------------------- */
    @Test
    public void emitStringBuffersUntilReadReturnsCharacterToken() {
        initTokeniser("abc");
        // No token emitted yet – call emit() directly
        tokeniser.emit("a");
        tokeniser.emit("b");
        tokeniser.emit("c");

        // The first call to read() should return a Character token containing "abc"
        Token token = tokeniser.read();
        assertEquals(Token.TokenType.Character, token.type);
        assertEquals("abc", token.asCharacter().getData());

        // Subsequent read() should return EOF (null) because no more input
        assertNull(tokeniser.read());
    }

    @Test
    public void emitStringSwitchesToBuilderWhenMoreThanOneString() {
        initTokeniser("xy");
        tokeniser.emit("x"); // first string -> charsString
        tokeniser.emit("y"); // second string -> switches to charsBuilder

        // charsBuilder now contains "xy"
        Token token = tokeniser.read();
        assertEquals(Token.TokenType.Character, token.type);
        assertEquals("xy", token.asCharacter().getData());
    }

    /* --------------------------------------------------------------
     * 2. State transition methods
     * -------------------------------------------------------------- */
    @Test
    public void transitionAndAdvanceTransitionChangeStateCorrectly() {
        initTokeniser("");
        assertEquals(TokeniserState.Data, tokeniser.getState());

        tokeniser.transition(TokeniserState.TagOpen);
        assertEquals(TokeniserState.TagOpen, tokeniser.getState());

        // advanceTransition should also advance the reader (even if empty)
        tokeniser.advanceTransition(TokeniserState.EndTagOpen);
        assertEquals(TokeniserState.EndTagOpen, tokeniser.getState());
        // reader position should be 0 (no characters) – just ensures no exception
        assertEquals(0, reader.pos());
    }

    /* --------------------------------------------------------------
     * 3. Character reference consumption
     * -------------------------------------------------------------- */
    @Test
    public void consumeDecimalNumericReference() {
        initTokeniser("&#65;"); // Unicode code point 65 -> 'A'
        int[] cp = tokeniser.consumeCharacterReference(null, false);
        assertNotNull(cp);
        assertEquals(1, cp.length);
        assertEquals('A', cp[0]);
    }

    @Test
    public void consumeHexNumericReference() {
        initTokeniser("&#x41;"); // hexadecimal 0x41 -> 'A'
        int[] cp = tokeniser.consumeCharacterReference(null, false);
        assertNotNull(cp);
        assertEquals(1, cp.length);
        assertEquals('A', cp[0]);
    }

    @Test
    public void numericReferenceMissingSemicolonAddsParseError() {
        initTokeniser("&#65"); // missing ';'
        tokeniser.consumeCharacterReference(null, false);
        assertFalse(errors.isEmpty());
        ParseError err = errors.get(0);
        assertTrue(err.getErrorMessage().contains("missing semicolon"));
    }

    @Test
    public void numericReferenceInvalidNumberIsReplacedByReplacementChar() {
        // 0x110000 is beyond Unicode max (0x10FFFF)
        initTokeniser("&#x110000;");
        int[] cp = tokeniser.consumeCharacterReference(null, false);
        assertNotNull(cp);
        assertEquals(1, cp.length);
        assertEquals(Tokeniser.replacementChar, cp[0]);
        // error should be recorded
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).getErrorMessage().contains("character outside of valid range"));
    }

    @Test
    public void namedReferenceWithSemicolon() {
        initTokeniser("&amp;"); // should resolve to '&'
        int[] cp = tokeniser.consumeCharacterReference(null, false);
        assertNotNull(cp);
        assertEquals(1, cp.length);
        assertEquals('&', cp[0]);
    }

    @Test
    public void baseNamedReferenceWithoutSemicolonIsAccepted() {
        initTokeniser("&amp"); // base named entity does not require ';'
        int[] cp = tokeniser.consumeCharacterReference(null, false);
        assertNotNull(cp);
        assertEquals(1, cp.length);
        assertEquals('&', cp[0]);
    }

    @Test
    public void namedReferenceInvalidAndMissingSemicolonAddsError() {
        initTokeniser("&unknown;"); // not a known entity
        tokeniser.consumeCharacterReference(null, false);
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).getErrorMessage().contains("invalid named referenece"));
    }

    @Test
    public void consumeCharacterReferenceInAttributeStopsOnAlphanumAfterEntity() {
        // In attribute context, "&ampx" should NOT be consumed as an entity
        initTokeniser("&ampx");
        int[] cp = tokeniser.consumeCharacterReference(null, true);
        assertNull(cp);
        // The reader should still be positioned after the '&' (i.e., at 'a')
        assertEquals('&', reader.current());
    }

    /* --------------------------------------------------------------
     * 4. Tag handling and appropriate end‑tag logic
     * -------------------------------------------------------------- */
    @Test
    public void createAndEmitStartTagSetsLastStartTag() {
        initTokeniser("");
        Token.Tag start = tokeniser.createTagPending(true);
        start.tagName("div");
        tokeniser.emitTagPending();

        // The token emitted should be a StartTag token with name "div"
        Token emitted = tokeniser.read();
        assertEquals(Token.TokenType.StartTag, emitted.type);
        assertEquals("div", emitted.asStartTag().tagName());

        // isAppropriateEndTagToken should now be true for a matching EndTag
        Token.Tag end = tokeniser.createTagPending(false);
        end.tagName("DIV"); // case‑insensitive match
        assertTrue(tokeniser.isAppropriateEndTagToken());

        // mismatching tag name should be false
        end.tagName("span");
        assertFalse(tokeniser.isAppropriateEndTagToken());
    }

    @Test
    public void appropriateEndTagNameReturnsLastStartTagOrNull() {
        initTokeniser("");
        assertNull(tokeniser.appropriateEndTagName());

        Token.Tag start = tokeniser.createTagPending(true);
        start.tagName("p");
        tokeniser.emitTagPending(); // emit start tag
        tokeniser.read(); // consume it

        assertEquals("p", tokeniser.appropriateEndTagName());
    }

    /* --------------------------------------------------------------
     * 5. Doctype and comment creation / emission
     * -------------------------------------------------------------- */
    @Test
    public void createAndEmitDoctypeToken() {
        initTokeniser("");
        tokeniser.createDoctypePending();
        tokeniser.doctypePending.name("html");
        tokeniser.doctypePending.publicId("PUBLIC");
        tokeniser.doctypePending.systemId("SYSTEM");
        tokeniser.emitDoctypePending();

        Token token = tokeniser.read();
        assertEquals(Token.TokenType.Doctype, token.type);
        Token.Doctype doctype = token.asDoctype();
        assertEquals("html", doctype.name());
        assertEquals("PUBLIC", doctype.publicId());
        assertEquals("SYSTEM", doctype.systemId());
    }

    @Test
    public void createAndEmitCommentToken() {
        initTokeniser("");
        tokeniser.createCommentPending();
        tokeniser.commentPending.data("my comment");
        tokeniser.emitCommentPending();

        Token token = tokeniser.read();
        assertEquals(Token.TokenType.Comment, token.type);
        assertEquals("my comment", token.asComment().getData());
    }

    /* --------------------------------------------------------------
     * 6. Error handling
     * -------------------------------------------------------------- */
    @Test
    public void errorWithStateAddsParseError() {
        initTokeniser("<");
        tokeniser.error(TokeniserState.TagOpen);
        assertEquals(1, errors.size());
        ParseError err = errors.get(0);
        assertTrue(err.getErrorMessage().contains("Unexpected character"));
        assertTrue(err.getErrorMessage().contains("TagOpen"));
    }

    @Test
    public void eofErrorAddsParseError() {
        initTokeniser("");
        tokeniser.eofError(TokeniserState.Data);
        assertEquals(1, errors.size());
        ParseError err = errors.get(0);
        assertTrue(err.getErrorMessage().contains("Unexpectedly reached end of file"));
        assertTrue(err.getErrorMessage().contains("Data"));
    }

    @Test
    public void genericErrorStringAddsParseError() {
        initTokeniser("");
        tokeniser.error("Custom error message");
        assertEquals(1, errors.size());
        assertEquals("Custom error message", errors.get(0).getErrorMessage());
    }

    /* --------------------------------------------------------------
     * 7. Utility methods
     * -------------------------------------------------------------- */
    @Test
    public void unescapeEntitiesHandlesMixOfTextAndEntities() {
        initTokeniser("a &amp; b &#x41; c &unknown; d");
        String result = tokeniser.unescapeEntities(false);
        // Expected: a & b A c &unknown; d   (unknown stays as literal)
        assertEquals("a & b A c &unknown; d", result);
    }

    @Test
    public void currentNodeInHtmlNSAlwaysTrue() {
        initTokeniser("");
        assertTrue(tokeniser.currentNodeInHtmlNS());
    }

    @Test
    public void createTempBufferResetsDataBuffer() {
        initTokeniser("script</script>");
        // Fill the dataBuffer manually
        tokeniser.dataBuffer.append("partial");
        tokeniser.createTempBuffer();
        assertEquals(0, tokeniser.dataBuffer.length());
    }

    /* --------------------------------------------------------------
     * Helper assertions for Token type casts (to keep test code tidy)
     * -------------------------------------------------------------- */
    private static class TokenExtensions {
        static Token.StartTag asStartTag(Token token) {
            return token.asStartTag();
        }

        static Token.Character asCharacter(Token token) {
            return token.asCharacter();
        }

        static Token.Comment asComment(Token token) {
            return token.asComment();
        }

        static Token.Doctype asDoctype(Token token) {
            return token.asDoctype();
        }
    }
}
