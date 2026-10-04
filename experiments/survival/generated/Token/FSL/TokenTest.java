package org.jsoup.parser;

import org.jsoup.nodes.Attributes;
import org.junit.Test;

import static org.junit.Assert.*;

public class TokenTest {

    // ----------------------------------------------------------------------
    // Token utility methods
    // ----------------------------------------------------------------------
    @Test
    public void tokenType_returnsSimpleClassName() {
        Token.Doctype doctype = new Token.Doctype();
        Token.StartTag start = new Token.StartTag();
        Token.EndTag end = new Token.EndTag();
        Token.Comment comment = new Token.Comment();
        Token.Character character = new Token.Character();
        Token.EOF eof = new Token.EOF();

        assertEquals("Doctype", doctype.tokenType());
        assertEquals("StartTag", start.tokenType());
        assertEquals("EndTag", end.tokenType());
        assertEquals("Comment", comment.tokenType());
        assertEquals("Character", character.tokenType());
        assertEquals("EOF", eof.tokenType());
    }

    @Test
    public void resetStringBuilder_clearsContent() {
        StringBuilder sb = new StringBuilder("some content");
        Token.reset(sb);
        assertEquals(0, sb.length());
    }

    @Test
    public void resetStringBuilder_nullDoesNothing() {
        // should not throw NPE
        Token.reset(null);
    }

    // ----------------------------------------------------------------------
    // Doctype tests
    // ----------------------------------------------------------------------
    @Test
    public void doctype_gettersAndReset() {
        Token.Doctype doctype = new Token.Doctype();
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

        // reset should clear everything
        doctype.reset();
        assertEquals("", doctype.getName());
        assertNull(doctype.getPubSysKey());
        assertEquals("", doctype.getPublicIdentifier());
        assertEquals("", doctype.getSystemIdentifier());
        assertFalse(doctype.isForceQuirks());
    }

    // ----------------------------------------------------------------------
    // StartTag / EndTag (Tag) tests
    // ----------------------------------------------------------------------
    @Test
    public void startTag_nameAndNormalName() {
        Token.StartTag tag = new Token.StartTag();
        tag.name("DiV");
        assertEquals("DiV", tag.name());
        assertEquals("div", tag.normalName());
    }

    @Test
    public void startTag_appendTagName() {
        Token.StartTag tag = new Token.StartTag();
        tag.appendTagName("sp");
        tag.appendTagName('a');
        tag.appendTagName('n');
        // now tag name should be "span"
        assertEquals("span", tag.name());
        assertEquals("span", tag.normalName());
    }

    @Test
    public void startTag_selfClosingFlag() {
        Token.StartTag tag = new Token.StartTag();
        assertFalse(tag.isSelfClosing());
        tag.selfClosing = true; // same package, can access directly
        assertTrue(tag.isSelfClosing());
    }

    @Test
    public void startTag_attributeLifecycle() {
        Token.StartTag tag = new Token.StartTag();

        // attribute with a value built via two calls (first uses pendingAttributeValueS)
        tag.appendAttributeName("href");
        tag.appendAttributeValue("http://");
        tag.appendAttributeValue("example.com");
        tag.newAttribute(); // commit

        // attribute with empty value
        tag.appendAttributeName("disabled");
        tag.setEmptyAttributeValue();
        tag.newAttribute();

        // attribute with no value (boolean attribute)
        tag.appendAttributeName("checked");
        tag.newAttribute();

        Attributes attrs = tag.getAttributes();
        assertEquals(3, attrs.size());
        assertEquals("http://example.com", attrs.get("href"));
        assertEquals("", attrs.get("disabled"));
        assertNull(attrs.get("checked"));
    }

    @Test
    public void startTag_finaliseTagCreatesPendingAttribute() {
        Token.StartTag tag = new Token.StartTag();

        tag.appendAttributeName("title");
        tag.appendAttributeValue("Hello");
        // do NOT call newAttribute(); let finaliseTag handle it
        tag.finaliseTag();

        Attributes attrs = tag.getAttributes();
        assertEquals(1, attrs.size());
        assertEquals("Hello", attrs.get("title"));
    }

    @Test
    public void startTag_toStringWithAndWithoutAttributes() {
        Token.StartTag empty = new Token.StartTag().name("br");
        assertEquals("<br>", empty.toString());

        Token.StartTag withAttrs = new Token.StartTag().name("img");
        withAttrs.appendAttributeName("src");
        withAttrs.appendAttributeValue("a.png");
        withAttrs.newAttribute();
        assertTrue(withAttrs.toString().contains("<img"));
        assertTrue(withAttrs.toString().contains("src"));
        assertTrue(withAttrs.toString().contains("a.png"));
    }

    @Test
    public void endTag_toString() {
        Token.EndTag end = new Token.EndTag().name("p");
        assertEquals("</p>", end.toString());
    }

    // ----------------------------------------------------------------------
    // Comment tests
    // ----------------------------------------------------------------------
    @Test
    public void comment_getDataResetAndToString() {
        Token.Comment comment = new Token.Comment();
        comment.data.append("hello");
        comment.bogus = true;

        assertEquals("hello", comment.getData());
        assertTrue(comment.bogus);
        assertEquals("<!--hello-->", comment.toString());

        comment.reset();
        assertEquals("", comment.getData());
        assertFalse(comment.bogus);
    }

    // ----------------------------------------------------------------------
    // Character tests
    // ----------------------------------------------------------------------
    @Test
    public void character_dataAndReset() {
        Token.Character ch = new Token.Character().data("x");
        assertEquals("x", ch.getData());
        assertEquals("x", ch.toString());

        ch.reset();
        assertNull(ch.getData());
    }

    // ----------------------------------------------------------------------
    // EOF tests
    // ----------------------------------------------------------------------
    @Test
    public void eof_isEOF() {
        Token.EOF eof = new Token.EOF();
        assertTrue(eof.isEOF());
        // reset does nothing but should return same instance
        assertSame(eof, eof.reset());
    }

    // ----------------------------------------------------------------------
    // Type checking helpers
    // ----------------------------------------------------------------------
    @Test
    public void typeCheckMethodsAndCasts() {
        Token.Doctype doctype = new Token.Doctype();
        Token.StartTag start = new Token.StartTag();
        Token.EndTag end = new Token.EndTag();
        Token.Comment comment = new Token.Comment();
        Token.Character character = new Token.Character();
        Token.EOF eof = new Token.EOF();

        assertTrue(doctype.isDoctype());
        assertSame(doctype, doctype.asDoctype());

        assertTrue(start.isStartTag());
        assertSame(start, start.asStartTag());

        assertTrue(end.isEndTag());
        assertSame(end, end.asEndTag());

        assertTrue(comment.isComment());
        assertSame(comment, comment.asComment());

        assertTrue(character.isCharacter());
        assertSame(character, character.asCharacter());

        assertTrue(eof.isEOF());
    }
}
