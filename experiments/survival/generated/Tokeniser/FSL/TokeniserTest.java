package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Entities;
import org.jsoup.parser.TokeniserState;
import org.jsoup.parser.Token;
import org.jsoup.parser.CharacterReader;
import org.jsoup.parser.ParseErrorList;
import org.jsoup.parser.ParseError;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

/**
 * Comprehensive JUnit4 tests for {@link Tokeniser}.
 * The tests cover:
 * <ul>
 *   <li>state transitions</li>
 *   <li>emit overloads (Token, String, char, char[], int[])</li>
 *   <li>character reference consumption (numeric, hex, named, errors)</li>
 *   <li>temporary buffer handling</li>
 *   <li>appropriate end‑tag logic</li>
 *   <li>error recording</li>
 *   <li>unescapeEntities utility</li>
 * </ul>
 */
public class TokeniserTest {

    /** Helper to create a fresh Tokeniser with a given input string. */
    private Tokeniser newTokeniser(String input) {
        CharacterReader reader = new CharacterReader(input);
        ParseErrorList errors = ParseErrorList.tracking(10);
        return new Tokeniser(reader, errors);
    }

    /** Helper to retrieve the internal error list from a tokeniser. */
    private List<ParseError> errors(Tokeniser t) {
        // ParseErrorList extends ArrayList<ParseError>, so cast works.
        return ((ParseErrorList) t.errors);
    }

    // ----------------------------------------------------------------------
    // 1. State handling
    // ----------------------------------------------------------------------
    @Test
    public void testInitialStateAndTransition() {
        Tokeniser t = newTokeniser("");
        assertEquals(TokeniserState.Data, t.getState());

        t.transition(TokeniserState.TagOpen);
        assertEquals(TokeniserState.TagOpen, t.getState());

        // advanceTransition should move the reader one char forward and change state
        Tokeniser t2 = newTokeniser("abc");
        int posBefore = t2.reader.pos();
        t2.advanceTransition(TokeniserState.TagName);
        assertEquals(posBefore + 1, t2.reader.pos());
        assertEquals(TokeniserState.TagName, t2.getState());
    }

    // ----------------------------------------------------------------------
    // 2. emit overloads
    // ----------------------------------------------------------------------
    @Test
    public void testEmitTokenAndRead() {
        Tokeniser t = newTokeniser("");
        Token.Comment comment = new Token.Comment();
        comment.reset();
        comment.data("hello");

        t.emit(comment);
        Token out = t.read();
        assertSame(comment, out);
        assertEquals(Token.TokenType.Comment, out.type);
        assertEquals("hello", ((Token.Comment) out).getData());
    }

    @Test
    public void testEmitStringAndReadCharacterToken() {
        Tokeniser t = newTokeniser("");
        t.emit("abc");
        Token token = t.read();
        assertEquals(Token.TokenType.Character, token.type);
        assertEquals("abc", ((Token.Character) token).getData());
    }

    @Test
    public void testEmitCharAndReadCharacterToken() {
        Tokeniser t = newTokeniser("");
        t.emit('x');
        Token token = t.read();
        assertEquals(Token.TokenType.Character, token.type);
        assertEquals("x", ((Token.Character) token).getData());
    }

    @Test
    public void testEmitCharArrayAndReadCharacterToken() {
        Tokeniser t = newTokeniser("");
        char[] chars = new char[] {'J', 'S', 'O', 'U', 'P'};
        t.emit(chars);
        Token token = t.read();
        assertEquals(Token.TokenType.Character, token.type);
        assertEquals("JSOUP", ((Token.Character) token).getData());
    }

    @Test
    public void testEmitIntArrayAndReadCharacterToken() {
        Tokeniser t = newTokeniser("");
        int[] codepoints = new int[] {0x41, 0x42, 0x43}; // A B C
        t.emit(codepoints);
        Token token = t.read();
        assertEquals(Token.TokenType.Character, token.type);
        assertEquals("ABC", ((Token.Character) token).getData());
    }

    // ----------------------------------------------------------------------
    // 3. consumeCharacterReference – numeric (decimal & hex) and named
    // ----------------------------------------------------------------------
    @Test
    public void testConsumeDecimalNumericReference() {
        Tokeniser t = newTokeniser("&#65;"); // 'A'
        // simulate that the '&' has already been consumed by the caller
        t.reader.consume(); // consume '&'
        int[] out = t.consumeCharacterReference(null, false);
        assertNotNull(out);
        assertEquals(1, out.length);
        assertEquals('A', out[0]);
    }

    @Test
    public void testConsumeHexNumericReferenceMissingSemicolon() {
        Tokeniser t = newTokeniser("&#x41"); // missing ';'
        t.reader.consume(); // consume '&'
        int[] out = t.consumeCharacterReference(null, false);
        assertNotNull(out);
        assertEquals(1, out.length);
        assertEquals('A', out[0]); // still resolves
        // an error should have been recorded for missing semicolon
        assertFalse(errors(t).isEmpty());
        assertTrue(errors(t).get(0).getErrorMessage().contains("missing semicolon"));
    }

    @Test
    public void testConsumeInvalidNumericReferenceProducesReplacementChar() {
        // 0x110000 is beyond Unicode max (0x10FFFF)
        Tokeniser t = newTokeniser("&#x110000;");
        t.reader.consume(); // consume '&'
        int[] out = t.consumeCharacterReference(null, false);
        assertNotNull(out);
        assertEquals(1, out.length);
        assertEquals(Tokeniser.replacementChar, out[0]);
        assertFalse(errors(t).isEmpty());
        assertTrue(errors(t).get(0).getErrorMessage().contains("character outside of valid range"));
    }

    @Test
    public void testConsumeNamedEntityWithSemicolon() {
        Tokeniser t = newTokeniser("&amp;");
        t.reader.consume(); // consume '&'
        int[] out = t.consumeCharacterReference(null, false);
        assertNotNull(out);
        assertEquals(1, out.length);
        assertEquals('&', out[0]); // '&' is 0x26
    }

    @Test
    public void testConsumeNamedEntityWithoutSemicolonWhenAllowed() {
        // "nbsp" is a base named entity; missing semicolon is allowed in some contexts
        Tokeniser t = newTokeniser("&nbsp");
        t.reader.consume(); // consume '&'
        int[] out = t.consumeCharacterReference(null, false);
        assertNotNull(out);
        assertEquals(1, out.length);
        assertEquals(0x00A0, out[0]); // non‑breaking space
    }

    @Test
    public void testConsumeNamedEntityInvalid() {
        Tokeniser t = newTokeniser("&notanentity;");
        t.reader.consume(); // consume '&'
        int[] out = t.consumeCharacterReference(null, false);
        assertNull(out);
        // No error is added for an invalid named reference (per spec), just null.
        assertTrue(errors(t).isEmpty());
    }

    @Test
    public void testConsumeCharacterReferenceAdditionalAllowedCharacter() {
        Tokeniser t = newTokeniser("&");
        // The next character after '&' is the end of input, so we set additionalAllowedCharacter to null.
        // Pass a character that matches the current (which is EOF) to verify early null return.
        // To force the condition, we need a non‑empty reader:
        Tokeniser t2 = newTokeniser("&x");
        t2.reader.consume(); // consume '&'
        // The next char is 'x'. Provide additionalAllowedCharacter='x' to trigger early return.
        int[] out = t2.consumeCharacterReference('x', false);
        assertNull(out);
    }

    // ----------------------------------------------------------------------
    // 4. Tag handling and appropriate end‑tag checks
    // ----------------------------------------------------------------------
    @Test
    public void testCreateTagPendingAndEmitTagPending() {
        Tokeniser t = newTokeniser("");
        // Create a start tag and set its name
        Token.Tag start = t.createTagPending(true);
        start.tagName("div");
        t.emitTagPending(); // should set lastStartTag to "div"

        // Now create an end tag with the same name and test appropriateness
        Token.Tag end = t.createTagPending(false);
        end.tagName("div");
        assertTrue(t.isAppropriateEndTagToken());
        assertEquals("div", t.appropriateEndTagName());

        // Change name to something else – should be false
        end.tagName("span");
        assertFalse(t.isAppropriateEndTagToken());
    }

    // ----------------------------------------------------------------------
    // 5. Error handling
    // ----------------------------------------------------------------------
    @Test
    public void testErrorMethodsAddParseError() {
        Tokeniser t = newTokeniser("abc");
        t.error("Custom error message");
        assertEquals(1, errors(t).size());
        assertTrue(errors(t).get(0).getErrorMessage().contains("Custom error message"));

        t.error(TokeniserState.Comment);
        assertEquals(2, errors(t).size());
        assertTrue(errors(t).get(1).getErrorMessage().contains("Unexpected character"));
        assertTrue(errors(t).get(1).getErrorMessage().contains("Comment"));
    }

    @Test
    public void testEofErrorAddsParseError() {
        Tokeniser t = newTokeniser("");
        t.eofError(TokeniserState.Data);
        assertEquals(1, errors(t).size());
        assertTrue(errors(t).get(0).getErrorMessage().contains("Unexpectedly reached end of file"));
    }

    // ----------------------------------------------------------------------
    // 6. unescapeEntities
    // ----------------------------------------------------------------------
    @Test
    public void testUnescapeEntitiesSimple() {
        Tokeniser t = newTokeniser("A &amp; B &lt; C");
        String out = t.unescapeEntities(false);
        assertEquals("A & B < C", out);
    }

    @Test
    public void testUnescapeEntitiesWithNumericRefs() {
        Tokeniser t = newTokeniser("&#65; &#x42;");
        String out = t.unescapeEntities(false);
        assertEquals("AB", out);
    }

    @Test
    public void testUnescapeEntitiesLeavesInvalidRefsUntouched() {
        Tokeniser t = newTokeniser("Hello &unknown; World");
        String out = t.unescapeEntities(false);
        // Unknown entity should be left as literal "&unknown;"
        assertEquals("Hello &unknown; World", out);
    }

    // ----------------------------------------------------------------------
    // 7. Miscellaneous utilities
    // ----------------------------------------------------------------------
    @Test
    public void testCurrentNodeInHtmlNSAlwaysTrue() {
        Tokeniser t = newTokeniser("");
        assertTrue(t.currentNodeInHtmlNS());
    }

    @Test
    public void testCreateTempBufferResetsDataBuffer() {
        Tokeniser t = newTokeniser("");
        t.dataBuffer.append("some data");
        assertTrue(t.dataBuffer.length() > 0);
        t.createTempBuffer();
        assertEquals(0, t.dataBuffer.length());
    }
}
