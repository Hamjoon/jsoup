###Test START##
```java
package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Entities;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link Tokeniser}.
 *
 * The tests are written in the same package {@code org.jsoup.parser}
 * so that package‑private members of {@link Tokeniser} can be accessed.
 *
 * Each public (actually package‑private) method of {@code Tokeniser}
 * is exercised at least once – a “basic” test that checks the normal
 * behaviour and an “edge‑case” test that exercises the special
 * conditions documented in the source code.
 */
public class TokeniserTest {

    private Tokeniser tokeniser;
    private ParseErrorList errors;

    /** Helper to create a fresh Tokeniser for each test. */
    private Tokeniser newTokeniser(String data) {
        CharacterReader reader = new CharacterReader(data);
        errors = ParseErrorList.tracking(10);
        return new Tokeniser(reader, errors);
    }

    /* --------------------------------------------------------------------- *
     *  1. read() – basic use via emit(String)                              *
     * --------------------------------------------------------------------- */
    @Test
    public void read_EmittedString_ReturnsCharacterToken() {
        tokeniser = newTokeniser("");
        tokeniser.emit("hello");
        Token token = tokeniser.read();

        // The token returned by read() after an emit(String) must be a character token
        assertNotNull("Token must not be null", token);
        assertTrue("Token should be a character token", token instanceof Token.Character);
        // The data stored in the character token should match the emitted string
        Token.Character charToken = (Token.Character) token;
        assertEquals("hello", charToken.getData());   // getData() is the public accessor in jsoup
    }

    /* --------------------------------------------------------------------- *
     *  2. getState() / transition() – state handling                        *
     * --------------------------------------------------------------------- */
    @Test
    public void transition_ChangesState_GetStateReflectsIt() {
        tokeniser = newTokeniser("");
        // initial state is Data
        assertEquals(TokeniserState.Data, tokeniser.getState());

        tokeniser.transition(TokeniserState.Rcdata);
        assertEquals(TokeniserState.Rcdata, tokeniser.getState());
    }

    /* --------------------------------------------------------------------- *
     *  3. advanceTransition() – advances the underlying reader            *
     * --------------------------------------------------------------------- */
    @Test
    public void advanceTransition_AdvancesReaderPosition() {
        tokeniser = newTokeniser("ABC");
        // start at position 0
        assertEquals(0, tokeniser.reader.pos());

        tokeniser.advanceTransition(TokeniserState.Rcdata);
        // The reader should have consumed one character
        assertEquals(1, tokeniser.reader.pos());
        assertEquals(TokeniserState.Rcdata, tokeniser.getState());
    }

    /* --------------------------------------------------------------------- *
     *  4. consumeCharacterReference() – numeric reference (valid)          *
     * --------------------------------------------------------------------- */
    @Test
    public void consumeCharacterReference_NumericValid_ReturnsCodepoint() {
        tokeniser = newTokeniser("&#65;"); // 65 == 'A'
        int[] result = tokeniser.consumeCharacterReference(null, false);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(65, result[0]);
    }

    /* --------------------------------------------------------------------- *
     *  5. consumeCharacterReference() – numeric reference (out of range)   *
     * --------------------------------------------------------------------- */
    @Test
    public void consumeCharacterReference_NumericInvalid_ReturnsReplacementChar() {
        tokeniser = newTokeniser("&#x110000;"); // > 0x10FFFF
        int[] result = tokeniser.consumeCharacterReference(null, false);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(Tokeniser.replacementChar, (char) result[0]);
    }

    /* --------------------------------------------------------------------- *
     *  6. consumeCharacterReference() – named base entity (with ';')       *
     * --------------------------------------------------------------------- */
    @Test
    public void consumeCharacterReference_NamedBase_WithSemicolon() {
        tokeniser = newTokeniser("&amp;");
        int[] result = tokeniser.consumeCharacterReference(null, false);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals('&', (char) result[0]);
    }

    /* --------------------------------------------------------------------- *
     *  7. consumeCharacterReference() – named base entity (missing ';')   *
     * --------------------------------------------------------------------- */
    @Test
    public void consumeCharacterReference_NamedBase_MissingSemicolon() {
        tokeniser = newTokeniser("&amp");
        int[] result = tokeniser.consumeCharacterReference(null, false);
        // Base named entities are allowed without a semicolon – the parser still resolves them
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals('&', (char) result[0]);
    }

    /* --------------------------------------------------------------------- *
     *  8. unescapeEntities() – simple entity conversion                     *
     * --------------------------------------------------------------------- */
    @Test
    public void unescapeEntities_SimpleConversion() {
        tokeniser = newTokeniser("a &amp; b");
        String out = tokeniser.unescapeEntities(false);
        assertEquals("a & b", out);
    }

    /* --------------------------------------------------------------------- *
     *  9. unescapeEntities() – missing semicolon on base entity            *
     * --------------------------------------------------------------------- */
    @Test
    public void unescapeEntities_MissingSemicolonBaseEntity() {
        tokeniser = newTokeniser("x &amp y");
        String out = tokeniser.unescapeEntities(false);
        // Base entity without semicolon should still be resolved to '&'
        assertEquals("x & y", out);
    }

    /* --------------------------------------------------------------------- *
     * 10. unescapeEntities() – invalid numeric reference                     *
     * --------------------------------------------------------------------- */
    @Test
    public void unescapeEntities_InvalidNumericReference() {
        tokeniser = newTokeniser("z &#x110000; q");
        String out = tokeniser.unescapeEntities(false);
        // Invalid numeric reference is replaced by the replacement character ()
        assertEquals("z \uFFFD q", out);
    }

    /* --------------------------------------------------------------------- *
     * 11. error(String) – adds a ParseError to the supplied list           *
     * --------------------------------------------------------------------- */
    @Test
    public void error_StringAddsParseError() {
        tokeniser = newTokeniser("");
        assertEquals(0, errors.size());

        tokeniser.error("custom error message");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getErrorMessage().contains("custom error message"));
    }

    /* --------------------------------------------------------------------- *
     * 12. isAppropriateEndTagToken() – no start tag emitted yet            *
     * --------------------------------------------------------------------- */
    @Test
    public void isAppropriateEndTagToken_NoStartTag_ReturnsFalse() throws Exception {
        tokeniser = newTokeniser("");
        // Ensure the internal lastStartTag is null via reflection (new tokeniser guarantees this)
        Field lastStartTagField = Tokeniser.class.getDeclaredField("lastStartTag");
        lastStartTagField.setAccessible(true);
        lastStartTagField.set(tokeniser, null);

        // Create an end‑tag pending with a dummy name
        Token.Tag endTag = tokeniser.createTagPending(false);
        // The Tag class stores the name in a field called "name".  We set it via reflection.
        Field nameField = Token.Tag.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(endTag, "div");

        assertFalse(tokeniser.isAppropriateEndTagToken());
    }

    /* --------------------------------------------------------------------- *
     * 13. appropriateEndTagName() – returns the last start‑tag name (null) *
     * --------------------------------------------------------------------- */
    @Test
    public void appropriateEndTagName_InitialValueIsNull() {
        tokeniser = newTokeniser("");
        assertNull(tokeniser.appropriateEndTagName());
    }

    /* --------------------------------------------------------------------- *
     * 14. createTempBuffer() – clears the data buffer                        *
     * --------------------------------------------------------------------- */
    @Test
    public void createTempBuffer_ClearsDataBuffer() throws Exception {
        tokeniser = newTokeniser("abc");
        // Fill the buffer manually
        tokeniser.dataBuffer.append("something");
        assertTrue(tokeniser.dataBuffer.length() > 0);

        tokeniser.createTempBuffer();
        assertEquals(0, tokeniser.dataBuffer.length());
    }

    /* --------------------------------------------------------------------- *
     * 15. emit(Tag) – validates that start‑tag updates lastStartTag           *
     * --------------------------------------------------------------------- */
    @Test
    public void emit_StartTag_UpdatesLastStartTag() throws Exception {
        tokeniser = newTokeniser("");
        Token.StartTag startTag = (Token.StartTag) tokeniser.createTagPending(true);
        // set the tag name to "p"
        Field nameField = Token.Tag.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(startTag, "p");
        // Emit the start tag – this should store "p" as lastStartTag
        tokeniser.emit(startTag);

        // Verify via appropriateEndTagName()
        assertEquals("p", tokeniser.appropriateEndTagName());
    }

    /* --------------------------------------------------------------------- *
     * 16. emit(Token) – error on emitting an EndTag with attributes           *
     * --------------------------------------------------------------------- */
    @Test
    public void emit_EndTagWithAttributes_TriggersError() throws Exception {
        tokeniser = newTokeniser("");
        Token.EndTag endTag = (Token.EndTag) tokeniser.createTagPending(false);
        // Simulate an attribute map (non‑null) – the actual type is Token.Attributes
        Field attrsField = Token.EndTag.class.getDeclaredField("attributes");
        attrsField.setAccessible(true);
        attrsField.set(endTag, new Token.Attributes()); // non‑null triggers error

        tokeniser.emit(endTag);
        // An error should have been recorded
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getErrorMessage().contains("Attributes incorrectly present on end tag"));
    }

    /* --------------------------------------------------------------------- *
     * 17. emit(String) followed by multiple emits – uses StringBuilder      *
     * --------------------------------------------------------------------- */
    @Test
    public void emit_MultipleStrings_BuffersCorrectly() {
        tokeniser = newTokeniser("");
        tokeniser.emit("first");
        tokeniser.emit("second");
        tokeniser.emit("third");

        // The first read should return a character token containing the concatenated string.
        Token token = tokeniser.read();
        assertTrue(token instanceof Token.Character);
        Token.Character charToken = (Token.Character) token;
        assertEquals("firstsecondthird", charToken.getData());
    }

    /* --------------------------------------------------------------------- *
     * 18. emit(char[]) and emit(int[]) – delegate to emit(String)            *
     * --------------------------------------------------------------------- */
    @Test
    public void emit_CharArrayAndIntArray_DelegateToStringEmit() {
        tokeniser = newTokeniser("");
        tokeniser.emit(new char[] {'a', 'b', 'c'});
        tokeniser.emit(new int[] {'x', 'y', 'z'}); // code points for 'x','y','z'

        Token token = tokeniser.read();
        assertTrue(token instanceof Token.Character);
        Token.Character charToken = (Token.Character) token;
        // The char array and int array are concatenated in order of emission
        assertEquals("abcxyz", charToken.getData());
    }

    /* --------------------------------------------------------------------- *
     * 19. currentNodeInHtmlNS() – always true (as per implementation)      *
     * --------------------------------------------------------------------- */
    @Test
    public void currentNodeInHtmlNS_AlwaysTrue() {
        tokeniser = newTokeniser("");
        assertTrue(tokeniser.currentNodeInHtmlNS());
    }
}
```
###Test END##