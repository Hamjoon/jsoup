package org.jsoup.parser;

import org.jsoup.nodes.Attributes;
import org.junit.Test;

import static org.junit.Assert.*;

public class TokenTest {

    /* ---------- Token type and tokenType() ---------- */

    @Test
    public void testTokenTypeNames() {
        Token.Doctype doctype = new Token.Doctype();
        assertEquals("Doctype", doctype.tokenType());

        Token.StartTag startTag = new Token.StartTag();
        assertEquals("StartTag", startTag.tokenType());

        Token.EndTag endTag = new Token.EndTag();
        assertEquals("EndTag", endTag.tokenType());

        Token.Comment comment = new Token.Comment();
        assertEquals("Comment", comment.tokenType());

        Token.Character character = new Token.Character();
        assertEquals("Character", character.tokenType());

        Token.EOF eof = new Token.EOF();
        assertEquals("EOF", eof.tokenType());
    }

    /* ---------- Doctype ---------- */

    @Test
    public void testDoctypeGettersAndReset() {
        Token.Doctype dt = new Token.Doctype();
        dt.name.append("html");
        dt.pubSysKey = "PUBLIC";
        dt.publicIdentifier.append("-//W3C//DTD HTML 4.01//EN");
        dt.systemIdentifier.append("http://www.w3.org/TR/html4/strict.dtd");
        dt.forceQuirks = true;

        assertEquals("html", dt.getName());
        assertEquals("PUBLIC", dt.getPubSysKey());
        assertEquals("-//W3C//DTD HTML 4.01//EN", dt.getPublicIdentifier());
        assertEquals("http://www.w3.org/TR/html4/strict.dtd", dt.getSystemIdentifier());
        assertTrue(dt.isForceQuirks());

        // reset should clear everything
        dt.reset();
        assertEquals("", dt.getName());
        assertNull(dt.getPubSysKey());
        assertEquals("", dt.getPublicIdentifier());
        assertEquals("", dt.getSystemIdentifier());
        assertFalse(dt.isForceQuirks());
    }

    /* ---------- Comment ---------- */

    @Test
    public void testCommentDataAndReset() {
        Token.Comment comment = new Token.Comment();
        comment.data.append("Hello");
        comment.data.append(" World");
        comment.bogus = true;

        assertEquals("Hello World", comment.getData());
        assertEquals("<!--Hello World-->", comment.toString());

        comment.reset();
        assertEquals("", comment.getData());
        assertFalse(comment.bogus);
    }

    /* ---------- Character ---------- */

    @Test
    public void testCharacterDataAndReset() {
        Token.Character ch = new Token.Character();
        ch.data("A");
        assertEquals("A", ch.getData());
        assertEquals("A", ch.toString());

        ch.reset();
        assertNull(ch.getData());
    }

    /* ---------- EOF ---------- */

    @Test
    public void testEOFIsSingletonLike() {
        Token.EOF eof = new Token.EOF();
        assertEquals(Token.TokenType.EOF, eof.type);
        // reset does nothing but return itself
        assertSame(eof, eof.reset());
    }

    /* ---------- Type checking helpers ---------- */

    @Test
    public void testTypeCheckingMethods() {
        Token token = new Token.StartTag();
        assertTrue(token.isStartTag());
        assertFalse(token.isEndTag());
        assertFalse(token.isDoctype());
        assertFalse(token.isComment());
        assertFalse(token.isCharacter());
        assertFalse(token.isEOF());

        assertSame(token, token.asStartTag());

        token = new Token.EndTag();
        assertTrue(token.isEndTag());
        assertSame(token, token.asEndTag());

        token = new Token.Doctype();
        assertTrue(token.isDoctype());
        assertSame(token, token.asDoctype());

        token = new Token.Comment();
        assertTrue(token.isComment());
        assertSame(token, token.asComment());

        token = new Token.Character();
        assertTrue(token.isCharacter());
        assertSame(token, token.asCharacter());

        token = new Token.EOF();
        assertTrue(token.isEOF());
    }

    /* ---------- StartTag attribute handling ---------- */

    @Test
    public void testStartTagAttributesSingleValue() {
        Token.StartTag tag = new Token.StartTag();
        tag.name("img");
        // simulate parser building attribute name/value
        tag.appendAttributeName("src");
        tag.appendAttributeValue("http://example.com/");
        tag.newAttribute(); // commit attribute

        Attributes attrs = tag.getAttributes();
        assertEquals(1, attrs.size());
        assertEquals("http://example.com/", attrs.get("src"));
    }

    @Test
    public void testStartTagAttributeValueBuilderTransition() {
        Token.StartTag tag = new Token.StartTag();
        tag.name("a");
        tag.appendAttributeName("href");
        // first value piece stored in pendingAttributeValueS
        tag.appendAttributeValue("http://");
        // second piece forces transition to StringBuilder
        tag.appendAttributeValue("example.com");
        tag.newAttribute();

        assertEquals("http://example.com", tag.getAttributes().get("href"));
    }

    @Test
    public void testStartTagEmptyAttributeValue() {
        Token.StartTag tag = new Token.StartTag();
        tag.name("input");
        tag.appendAttributeName("disabled");
        tag.setEmptyAttributeValue();
        tag.newAttribute();

        assertEquals("", tag.getAttributes().get("disabled"));
    }

    @Test
    public void testStartTagBooleanAttributeNoValue() {
        Token.StartTag tag = new Token.StartTag();
        tag.name("input");
        tag.appendAttributeName("required");
        // no value, no empty flag
        tag.newAttribute();

        assertNull(tag.getAttributes().get("required"));
    }

    @Test
    public void testStartTagAttributeNameTrimAndIgnoreEmpty() {
        Token.StartTag tag = new Token.StartTag();
        tag.name("div");
        // whitespace around name should be trimmed
        tag.appendAttributeName("  data-test  ");
        tag.appendAttributeValue("value");
        tag.newAttribute();
        // empty name after trim should be ignored
        tag.appendAttributeName("   ");
        tag.appendAttributeValue("ignore");
        tag.newAttribute();

        assertEquals(1, tag.getAttributes().size());
        assertEquals("value", tag.getAttributes().get("data-test"));
    }

    @Test
    public void testStartTagFinaliseTagCommitsPendingAttribute() {
        Token.StartTag tag = new Token.StartTag();
        tag.name("meta");
        tag.appendAttributeName("charset");
        tag.appendAttributeValue("utf-8");
        // do not call newAttribute explicitly
        tag.finaliseTag();

        assertEquals("utf-8", tag.getAttributes().get("charset"));
    }

    @Test
    public void testStartTagSelfClosingFlagAndReset() {
        Token.StartTag tag = new Token.StartTag();
        tag.selfClosing = true;
        assertTrue(tag.isSelfClosing());

        tag.appendTagName("div");
        tag.appendAttributeName("id");
        tag.appendAttributeValue("main");
        tag.newAttribute();

        tag.reset();

        assertFalse(tag.isSelfClosing());
        assertNull(tag.name());
        assertNull(tag.normalName());
        assertNull(tag.getAttributes());
    }

    @Test
    public void testStartTagToStringWithAttributes() {
        Token.StartTag tag = new Token.StartTag();
        tag.nameAttr("p", new Attributes().add("class", "intro"));
        assertEquals("<p class=intro>", tag.toString());

        Token.StartTag emptyTag = new Token.StartTag();
        emptyTag.name("br");
        assertEquals("<br>", emptyTag.toString());
    }

    /* ---------- EndTag basic behavior ---------- */

    @Test
    public void testEndTagNameAndToString() {
        Token.EndTag endTag = new Token.EndTag();
        endTag.name("div");
        assertEquals("div", endTag.name());
        assertEquals("div", endTag.normalName());
        assertEquals("</div>", endTag.toString());
    }

    /* ---------- Tag name and normalName helpers ---------- */

    @Test
    public void testTagNameAndNormalNameCaseHandling() {
        Token.StartTag tag = new Token.StartTag();
        tag.name("DiV");
        assertEquals("DiV", tag.name());
        // normalName should be lower‑cased
        assertEquals("div", tag.normalName());
    }

    @Test
    public void testAppendTagName() {
        Token.StartTag tag = new Token.StartTag();
        tag.appendTagName("sp");
        tag.appendTagName('a');
        tag.appendTagName("n");
        assertEquals("span", tag.name());
        assertEquals("span", tag.normalName());
    }

    /* ---------- Static reset(StringBuilder) ---------- */

    @Test
    public void testStaticStringBuilderReset() {
        StringBuilder sb = new StringBuilder("content");
        Token.reset(sb);
        assertEquals(0, sb.length());

        // null should be safely ignored
        Token.reset(null);
    }
}
