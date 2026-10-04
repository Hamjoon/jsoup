**###Test START##**  

```java
package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Token;
import org.jsoup.parser.CharacterReader;
import org.jsoup.parser.ParseErrorList;
import org.jsoup.parser.TokeniserState;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Tokeniser}.
 *
 * The tests are written in the same package as {@code Tokeniser} so that
 * package‑private members (e.g. {@code consumeCharacterReference}) can be accessed
 * directly.
 *
 * The suite covers:
 * <ul>
 *   <li>All public (and package‑private) API methods.</li>
 *   <li>Typical use cases, edge cases and error handling.</li>
 *   <li>Interaction with the {@link CharacterReader} and {@link ParseErrorList}.</li>
 * </ul>
 */
public class TokeniserTest {

    private CharacterReader reader;
    private ParseErrorList errors;
    private Tokeniser tokeniser;

    @Before
    public void setUp() {
        // a fresh reader / error list for every test
        reader = new CharacterReader("");
        errors = ParseErrorList.tracking(10);   // allow up to 10 errors
        tokeniser = new Tokeniser(reader, errors);
    }

    /* --------------------------------------------------------------------- *
     *  Tests for emit(String) buffering logic                               *
     * --------------------------------------------------------------------- */

    @Test
    public void emitStringBuffersUntilRead() {
        tokeniser.emit("a");
        tokeniser.emit("b");
        tokeniser.emit("c");

        Token token = tokeniser.read();               // should flush the buffer as a Character token
        assertEquals(Token.TokenType.Character, token.type);
        assertEquals("abc", ((Token.Character) token).getData());
    }

    @Test
    public void emitStringThenEmitToken_CharacterTokenEmittedFirst() {
        // Emit a non‑character token first
        Token.Comment comment = new Token.Comment().data("my comment");
        tokeniser.emit(comment);

        // Followed by a buffered string
        tokeniser.emit("xyz");

        // The first read must return the comment token, not the string buffer
        Token t1 = tokeniser.read();
        assertEquals(Token.TokenType.Comment, t1.type);
        assertEquals("my comment", ((Token.Comment) t1).getData());

        // The second read must now return the buffered characters
        Token t2 = tokeniser.read();
        assertEquals(Token.TokenType.Character, t2.type);
        assertEquals("xyz", ((Token.Character) t2).getData());
    }

    /* --------------------------------------------------------------------- *
     *  State handling                                                       *
     * --------------------------------------------------------------------- */

    @Test
    public void transitionChangesState() {
        tokeniser.transition(TokeniserState.Rcdata);
        assertEquals(TokeniserState.Rcdata, tokeniser.getState());
    }

    @Test
    public void advanceTransitionAdvancesReaderAndChangesState() {
        // Use a spy so we can verify that reader.advance() is called
        CharacterReader spyReader = spy(new CharacterReader("abc"));
        Tokeniser t = new Tokeniser(spyReader, errors);

        t.advanceTransition(TokeniserState.CData);
        verify(spyReader, times(1)).advance();
        assertEquals(TokeniserState.CData, t.getState());
    }

    /* --------------------------------------------------------------------- *
     *  Character reference consumption (numeric & named)                    *
     * --------------------------------------------------------------------- */

    @Test
    public void consumeNumericCharacterReference_ValidDecimal() {
        tokeniser = new Tokeniser(new CharacterReader("&#65;"), errors);
        int[] ref = tokeniser.consumeCharacterReference(null, false);
        assertNotNull(ref);
        assertEquals(1, ref.length);
        assertEquals('A', ref[0]);
    }

    @Test
    public void consumeNumericCharacterReference_ValidHex() {
        tokeniser = new Tokeniser(new CharacterReader("&#x41;"), errors);
        int[] ref = tokeniser.consumeCharacterReference(null, false);
        assertNotNull(ref);
        assertEquals('A', ref[0]);
    }

    @Test
    public void consumeNumericCharacterReference_MissingSemicolon() {
        tokeniser = new Tokeniser(new CharacterReader("&#65"), errors);
        int[] ref = tokeniser.consumeCharacterReference(null, false);
        // Missing semicolon is a parse error but the reference is still resolved
        assertNotNull(ref);
        assertEquals('A', ref[0]);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getErrorMessage().contains("missing semicolon"));
    }

    @Test
    public void consumeNumericCharacterReference_InvalidNumber_ReturnsReplacementChar() {
        tokeniser = new Tokeniser(new CharacterReader("&#x110000;"), errors); // > 0x10FFFF
        int[] ref = tokeniser.consumeCharacterReference(null, false);
        assertNotNull(ref);
        assertEquals(Tokeniser.replacementChar, ref[0]);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getErrorMessage().contains("character outside of valid range"));
    }

    @Test
    public void consumeNamedCharacterReference_ValidEntity() {
        tokeniser = new Tokeniser(new CharacterReader("&amp;"), errors);
        int[] ref = tokeniser.consumeCharacterReference(null, false);
        assertNotNull(ref);
        assertEquals(1, ref.length);
        assertEquals('&', ref[0]);
    }

    @Test
    public void consumeNamedCharacterReference_InvalidEntity_ReturnsNullAndLogsError() {
        tokeniser = new Tokeniser(new CharacterReader("&invalid;"), errors);
        int[] ref = tokeniser.consumeCharacterReference(null, false);
        assertNull(ref);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getErrorMessage().contains("invalid named"));
    }

    @Test
    public void consumeNamedCharacterReference_MissingSemicolon_LogsErrorButReturnsCodepoint() {
        tokeniser = new Tokeniser(new CharacterReader("&amp"), errors);
        int[] ref = tokeniser.consumeCharacterReference(null, false);
        // Entity is recognised even without ';' (base named entity)
        assertNotNull(ref);
        assertEquals('&', ref[0]);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getErrorMessage().contains("missing semicolon"));
    }

    /* --------------------------------------------------------------------- *
     *  Tag handling & end‑tag validation                                    *
     * --------------------------------------------------------------------- */

    @Test
    public void createAndEmitTagPending_StartTag() {
        Token.Tag tag = tokeniser.createTagPending(true); // start tag
        tag.name("div");
        tokeniser.emitTagPending();

        Token emitted = tokeniser.read();
        assertEquals(Token.TokenType.StartTag, emitted.type);
        assertEquals("div", ((Token.StartTag) emitted).tagName);
        // lastStartTag should now be "div"
        assertEquals("div", tokeniser.appropriateEndTagName());
    }

    @Test
    public void isAppropriateEndTagToken_MatchesLastStartTag() {
        // Emit a start tag <span>
        tokeniser.createTagPending(true).name("span");
        tokeniser.emitTagPending();
        tokeniser.read(); // consume the start tag

        // Build an end tag </span>
        tokeniser.createTagPending(false).name("SPAN"); // case‑insensitive
        assertTrue(tokeniser.isAppropriateEndTagToken());
    }

    @Test
    public void isAppropriateEndTagToken_DoesNotMatch() {
        // Emit a start tag <p>
        tokeniser.createTagPending(true).name("p");
        tokeniser.emitTagPending();
        tokeniser.read();

        // End tag </div>
        tokeniser.createTagPending(false).name("div");
        assertFalse(tokeniser.isAppropriateEndTagToken());
    }

    /* --------------------------------------------------------------------- *
     *  Doctype and comment helpers                                          *
     * --------------------------------------------------------------------- */

    @Test
    public void createAndEmitDoctypePending() {
        tokeniser.createDoctypePending();
        tokeniser.doctypePending.name("html");
        tokeniser.emitDoctypePending();

        Token t = tokeniser.read();
        assertEquals(Token.TokenType.Doctype, t.type);
        assertEquals("html", ((Token.Doctype) t).getName());
    }

    @Test
    public void createAndEmitCommentPending() {
        tokeniser.createCommentPending();
        tokeniser.commentPending.data("a comment");
        tokeniser.emitCommentPending();

        Token t = tokeniser.read();
        assertEquals(Token.TokenType.Comment, t.type);
        assertEquals("a comment", ((Token.Comment) t).getData());
    }

    /* --------------------------------------------------------------------- *
     *  Buffer utilities                                                     *
     * --------------------------------------------------------------------- */

    @Test
    public void createTempBufferResetsDataBuffer() {
        tokeniser.dataBuffer.append("some data");
        tokeniser.createTempBuffer();
        assertEquals(0, tokeniser.dataBuffer.length());
    }

    /* --------------------------------------------------------------------- *
     *  Error handling                                                       *
     * --------------------------------------------------------------------- */

    @Test
    public void errorAddsParseError() {
        tokeniser.error("custom error message");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getErrorMessage().contains("custom error message"));
    }

    @Test
    public void eofErrorAddsParseError() {
        tokeniser.eofError(TokeniserState.Data);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getErrorMessage().contains("Unexpectedly reached end of file"));
    }

    @Test
    public void characterReferenceErrorAddsParseError() {
        // Trigger a character reference error via an invalid numeric reference
        tokeniser = new Tokeniser(new CharacterReader("&#xXYZ;"), errors);
        tokeniser.consumeCharacterReference(null, false);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getErrorMessage().contains("Invalid character reference"));
    }

    /* --------------------------------------------------------------------- *
     *  Miscellaneous helpers                                                *
     * --------------------------------------------------------------------- */

    @Test
    public void currentNodeInHtmlNSAlwaysTrue() {
        assertTrue(tokeniser.currentNodeInHtmlNS());
    }

    @Test
    public void unescapeEntitiesHandlesAmpersandAndNamedRefs() {
        tokeniser = new Tokeniser(new CharacterReader("a&amp;b &copy;"), errors);
        String out = tokeniser.unescapeEntities(false);
        assertEquals("a&b ©", out);
    }

    @Test
    public void unescapeEntitiesInAttributeLeavesInvalidRefs() {
        tokeniser = new Tokeniser(new CharacterReader("a&bogus;z"), errors);
        String out = tokeniser.unescapeEntities(true); // in attribute => invalid refs are left untouched
        assertEquals("a&bogus;z", out);
        // An error should have been recorded for the invalid reference
        assertEquals(1, errors.size());
    }
}
```

**###Test END##**