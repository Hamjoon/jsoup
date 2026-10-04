package org.jsoup.parser;

import org.jsoup.nodes.Attributes;
import org.junit.Test;
import static org.junit.Assert.*;

public class TokenTest {

    /* -------------------------------------------------
       Basic functionality tests
    --------------------------------------------------- */

    @Test public void tokenType_returnsSimpleClassName() {
        Token.Doctype doctype = new Token.Doctype();
        assertEquals("Doctype", doctype.tokenType());
    }

    @Test public void staticReset_clearsStringBuilder() {
        StringBuilder sb = new StringBuilder("abc");
        Token.reset(sb);
        assertEquals(0, sb.length());
    }

    @Test public void staticReset_nullIsNoOp() {
        Token.reset(null); // should not throw
    }

    @Test public void doctype_gettersReturnInitialValues() {
        Token.Doctype d = new Token.Doctype();
        assertEquals("", d.getName());
        assertNull(d.getPubSysKey());
        assertEquals("", d.getPublicIdentifier());
        assertEquals("", d.getSystemIdentifier());
        assertFalse(d.isForceQuirks());
    }

    @Test public void startTag_toStringWithAttributes() {
        Token.StartTag st = new Token.StartTag();
        Attributes attrs = new Attributes();
        attrs.put("href", "http://example.com");
        st.nameAttr("a", attrs);
        assertEquals("<a href=\"http://example.com\">", st.toString());
    }

    @Test public void startTag_toStringWithoutAttributes() {
        Token.StartTag st = new Token.StartTag();
        st.name("br");
        assertEquals("<br>", st.toString());
    }

    @Test public void endTag_toString() {
        Token.EndTag et = new Token.EndTag();
        et.name("div");
        assertEquals("</div>", et.toString());
    }

    @Test public void comment_toString() {
        Token.Comment c = new Token.Comment();
        // simulate adding data (we are in the same package, so data is accessible)
        c.data.append("hello");
        assertEquals("<!--hello-->", c.toString());
    }

    @Test public void character_dataAndToString() {
        Token.Character ch = new Token.Character();
        ch.data("x");
        assertEquals("x", ch.getData());
        assertEquals("x", ch.toString());
    }

    @Test public void eof_isEOF() {
        Token.EOF eof = new Token.EOF();
        assertTrue(eof.isEOF());
    }

    @Test public void tag_nameAndNormalName() {
        Token.StartTag st = new Token.StartTag();
        st.name("DiV");
        assertEquals("DiV", st.name());
        assertEquals("div", st.normalName());
    }

    @Test public void tag_appendTagName() {
        Token.StartTag st = new Token.StartTag();
        st.appendTagName("h");
        st.appendTagName('1');
        assertEquals("h1", st.name());
        assertEquals("h1", st.normalName());
    }

    @Test public void tag_attributeLifecycle() {
        Token.StartTag st = new Token.StartTag();
        st.appendAttributeName("href");
        st.appendAttributeValue("http://example.com");
        st.newAttribute();               // finalise attribute
        assertEquals("http://example.com", st.getAttributes().get("href"));
    }

    @Test public void tag_setEmptyAttributeValue() {
        Token.StartTag st = new Token.StartTag();
        st.appendAttributeName("checked");
        st.setEmptyAttributeValue();
        st.newAttribute();
        assertEquals("", st.getAttributes().get("checked"));
    }

    @Test public void tag_finaliseTagFlushesPendingAttribute() {
        Token.StartTag st = new Token.StartTag();
        st.appendAttributeName("id");
        st.appendAttributeValue("test");
        st.finaliseTag();                // should call newAttribute internally
        assertEquals("test", st.getAttributes().get("id"));
    }

    @Test public void token_isTypeHelpers() {
        Token.Doctype d = new Token.Doctype();
        assertTrue(d.isDoctype());
        assertFalse(d.isStartTag());

        Token.StartTag st = new Token.StartTag();
        assertTrue(st.isStartTag());
        assertFalse(st.isEndTag());

        Token.EndTag et = new Token.EndTag();
        assertTrue(et.isEndTag());

        Token.Comment c = new Token.Comment();
        assertTrue(c.isComment());

        Token.Character ch = new Token.Character();
        assertTrue(ch.isCharacter());

        Token.EOF eof = new Token.EOF();
        assertTrue(eof.isEOF());
    }

    /* -------------------------------------------------
       Edge‑case / exception tests
    --------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void tag_nameThrowsIfNotSet() {
        Token.StartTag st = new Token.StartTag();
        st.name(); // should trigger Validate.isFalse
    }

    @Test
    public void tag_newAttributeIgnoresWhitespaceName() {
        Token.StartTag st = new Token.StartTag();
        st.appendAttributeName("   "); // only spaces
        st.appendAttributeValue("value");
        st.newAttribute(); // should not add anything
        assertTrue(st.getAttributes() == null || st.getAttributes().size() == 0);
    }

    @Test
    public void tag_appendAttributeValueOptimisationPath() {
        Token.StartTag st = new Token.StartTag();
        st.appendAttributeName("data");
        st.appendAttributeValue("first");   // stored in pendingAttributeValueS
        st.appendAttributeValue("second"); // forces conversion to StringBuilder
        st.newAttribute();
        assertEquals("firstsecond", st.getAttributes().get("data"));
    }

    @Test
    public void tag_appendAttributeValueEmptyStringStaysEmpty() {
        Token.StartTag st = new Token.StartTag();
        st.appendAttributeName("empty");
        st.appendAttributeValue(""); // empty string, pendingAttributeValueS becomes ""
        st.newAttribute();
        assertEquals("", st.getAttributes().get("empty"));
    }

    @Test
    public void doctype_resetClearsAllFields() {
        Token.Doctype d = new Token.Doctype();
        d.name.append("html");
        d.pubSysKey = "something";
        d.publicIdentifier.append("pubId");
        d.systemIdentifier.append("sysId");
        d.forceQuirks = true;

        d.reset();

        assertEquals("", d.getName());
        assertNull(d.getPubSysKey());
        assertEquals("", d.getPublicIdentifier());
        assertEquals("", d.getSystemIdentifier());
        assertFalse(d.isForceQuirks());
    }

    @Test
    public void comment_resetClearsDataAndBogusFlag() {
        Token.Comment c = new Token.Comment();
        c.data.append("some comment");
        c.bogus = true;

        c.reset();

        assertEquals("", c.getData());
        assertFalse(c.bogus);
    }

    @Test
    public void character_resetClearsData() {
        Token.Character ch = new Token.Character();
        ch.data("abc");
        ch.reset();
        assertNull(ch.getData());
    }

    @Test
    public void eof_resetReturnsSameInstance() {
        Token.EOF eof = new Token.EOF();
        Token.EOF after = (Token.EOF) eof.reset();
        assertSame(eof, after);
    }
}
