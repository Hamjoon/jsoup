package org.jsoup.parser;

import org.jsoup.nodes.Attributes;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Token} and its concrete subclasses.
 *
 * The tests cover:
 * <ul>
 *   <li>All accessible (package‑private) public‑like methods.</li>
 *   <li>Typical usage scenarios.</li>
 *   <li>Edge cases, null handling and exception paths.</li>
 * </ul>
 */
public class TokenTest {

    /* --------------------------------------------------------------------- */
    /*  Helper methods to create concrete token instances                  */
    /* --------------------------------------------------------------------- */

    private Token.Doctype newDoctype() {
        return new Token.Doctype();
    }

    private Token.StartTag newStartTag() {
        return new Token.StartTag();
    }

    private Token.EndTag newEndTag() {
        return new Token.EndTag();
    }

    private Token.Comment newComment() {
        return new Token.Comment();
    }

    private Token.Character newCharacter() {
        return new Token.Character();
    }

    private Token.EOF newEOF() {
        return new Token.EOF();
    }

    /* --------------------------------------------------------------------- */
    /*  Token type‑identification methods                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsAndAsMethods() {
        Token.Doctype doctype = newDoctype();
        Token.StartTag start = newStartTag();
        Token.EndTag end = newEndTag();
        Token.Comment comment = newComment();
        Token.Character character = newCharacter();
        Token.EOF eof = newEOF();

        // ---- isXxx() ----
        assertTrue(doctype.isDoctype());
        assertFalse(doctype.isStartTag());
        assertFalse(doctype.isEndTag());
        assertFalse(doctype.isComment());
        assertFalse(doctype.isCharacter());
        assertFalse(doctype.isEOF());

        assertTrue(start.isStartTag());
        assertFalse(start.isDoctype());
        assertFalse(start.isEndTag());
        assertFalse(start.isComment());
        assertFalse(start.isCharacter());
        assertFalse(start.isEOF());

        assertTrue(end.isEndTag());
        assertTrue(comment.isComment());
        assertTrue(character.isCharacter());
        assertTrue(eof.isEOF());

        // ---- asXxx() ----
        assertSame(doctype, doctype.asDoctype());
        assertSame(start, start.asStartTag());
        assertSame(end, end.asEndTag());
        assertSame(comment, comment.asComment());
        assertSame(character, character.asCharacter());

        // Wrong cast should throw ClassCastException
        try {
            doctype.asStartTag();
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {
        }
        try {
            start.asDoctype();
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {
        }
    }

    @Test
    public void testTokenTypeName() {
        Token start = newStartTag();
        Token end = newEndTag();
        Token comment = newComment();
        Token character = newCharacter();
        Token doctype = newDoctype();
        Token eof = newEOF();

        assertEquals("StartTag", start.tokenType());
        assertEquals("EndTag", end.tokenType());
        assertEquals("Comment", comment.tokenType());
        assertEquals("Character", character.tokenType());
        assertEquals("Doctype", doctype.tokenType());
        assertEquals("EOF", eof.tokenType());
    }

    /* --------------------------------------------------------------------- */
    /*  Doctype specific tests                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDoctypeGettersAndReset() {
        Token.Doctype doctype = newDoctype();
        doctype.name.append("html");
        doctype.pubSysKey = "PUBLIC";
        doctype.publicIdentifier.append("-//W3C//DTD HTML 4.01//EN");
        doctype.systemIdentifier.append("http://www.w3.org/TR/html4/strict.dtd");
        doctype.forceQuirks = true;

        assertEquals("html", doctype.getName());
        assertEquals("PUBLIC", doctype.getPubSysKey());
        assertEquals("-//W3C//DTD HTML 4.01//EN", doctype.getPublicIdentifier());
        assertEquals("http://www.w3.org/TR/html4/strict.dtd", doctype.getSystemIdentifier());
        assertTrue(doctype.isForceQuirks());

        // reset must clear everything
        doctype.reset();
        assertEquals("", doctype.getName());
        assertNull(doctype.getPubSysKey());
        assertEquals("", doctype.getPublicIdentifier());
        assertEquals("", doctype.getSystemIdentifier());
        assertFalse(doctype.isForceQuirks());
    }

    /* --------------------------------------------------------------------- */
    /*  Tag (StartTag) attribute handling tests                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testStartTagAttributeCreationAndRetrieval() {
        Token.StartTag tag = newStartTag();
        tag.name("a"); // set a tag name for later toString checks

        // ---- simple attribute with a value ----
        tag.appendAttributeName("href");
        tag.appendAttributeValue("http://example.com");
        tag.newAttribute(); // commit attribute
        assertEquals("http://example.com", tag.getAttributes().get("href"));

        // ---- boolean attribute (empty string) ----
        tag.appendAttributeName("disabled");
        tag.setEmptyAttributeValue();
        tag.newAttribute();
        assertEquals("", tag.getAttributes().get("disabled"));

        // ---- attribute built via multiple append calls (StringBuilder path) ----
        tag.appendAttributeName("data");
        tag.appendAttributeValue("val");
        tag.appendAttributeValue("ue");
        tag.appendAttributeValue(new char[]{'A', 'B'});
        tag.appendAttributeValue(new int[]{0x20AC}); // Euro sign
        tag.newAttribute();
        // Expected composed value: "valueAB€"
        assertEquals("valueAB€", tag.getAttributes().get("data"));
    }

    @Test
    public void testFinaliseTagTriggersPendingAttribute() {
        Token.StartTag tag = newStartTag();
        tag.appendAttributeName("id");
        tag.appendAttributeValue("test");
        // No explicit newAttribute() – finaliseTag should do it
        tag.finaliseTag();
        assertEquals("test", tag.getAttributes().get("id"));
    }

    @Test
    public void testAppendTagNameWhenInitiallyNull() {
        Token.StartTag tag = newStartTag();
        // tagName is null initially; we append pieces
        tag.appendTagName("di");
        tag.appendTagName('v');
        tag.appendTagName("Tag");
        assertEquals("divTag", tag.name()); // name() returns the raw tagName
        assertEquals("divtag", tag.normalName()); // lower‑cased version
    }

    /* --------------------------------------------------------------------- */
    /*  toString() implementations                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testStartTagToStringWithAndWithoutAttributes() {
        Token.StartTag empty = newStartTag();
        empty.name("br");
        // No attributes
        assertEquals("<br>", empty.toString());

        // With attributes
        Token.StartTag withAttrs = newStartTag();
        withAttrs.name("a");
        withAttrs.appendAttributeName("href");
        withAttrs.appendAttributeValue("http://example.com");
        withAttrs.newAttribute();
        // Attributes.toString() prints in the form "href=http://example.com"
        assertEquals("<a href=http://example.com>", withAttrs.toString());
    }

    @Test
    public void testEndTagToString() {
        Token.EndTag end = newEndTag();
        end.name("p");
        assertEquals("</p>", end.toString());
    }

    @Test
    public void testCommentToStringAndData() {
        Token.Comment comment = newComment();
        comment.data.append("Hello");
        comment.data.append(" World");
        assertEquals("Hello World", comment.getData());
        assertEquals("<!--Hello World-->", comment.toString());
    }

    @Test
    public void testCharacterToStringAndData() {
        Token.Character ch = newCharacter();
        ch.data("x");
        assertEquals("x", ch.getData());
        assertEquals("x", ch.toString());
    }

    @Test
    public void testEOFBehaviour() {
        Token.EOF eof = newEOF();
        assertTrue(eof.isEOF());
        // reset on EOF returns the same instance (no state to clear)
        Token.EOF afterReset = (Token.EOF) eof.reset();
        assertSame(eof, afterReset);
    }

    /* --------------------------------------------------------------------- */
    /*  Static utility method reset(StringBuilder) edge cases                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testStaticResetStringBuilder() {
        // Normal case
        StringBuilder sb = new StringBuilder("temp");
        Token.reset(sb);
        assertEquals(0, sb.length());

        // Null argument must be ignored, not throw NPE
        Token.reset(null);
    }

    /* --------------------------------------------------------------------- */
    /*  Edge case: calling newAttribute() when no pending name/value        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNewAttributeWithNoPendingNameDoesNothing() {
        Token.StartTag tag = newStartTag();
        // No pending attribute name/value – should simply reset internal flags
        tag.newAttribute(); // should not throw
        assertNull(tag.getAttributes()); // attributes are still null until first attribute added
    }

    /* --------------------------------------------------------------------- */
    /*  Edge case: attribute name consisting only of whitespace          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAttributeNameWhitespaceIsIgnored() {
        Token.StartTag tag = newStartTag();
        tag.appendAttributeName("   "); // whitespace only
        tag.appendAttributeValue("value");
        tag.newAttribute(); // should ignore the attribute
        assertTrue(tag.getAttributes() == null || tag.getAttributes().size() == 0);
    }
}
