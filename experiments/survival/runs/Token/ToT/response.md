###Test START##
```java
/**
 * Comprehensive JUnit‑4 test suite for {@code org.jsoup.parser.Token}.
 *
 * The tests are written in the same package as the class under test so that
 * package‑private members (constructors, inner classes, and methods) are
 * directly accessible.
 *
 * The suite follows the “three‑expert” discussion model described in the
 * prompt: each expert contributed a test case for every public‑or‑package
 * method, covering typical usage, edge cases and error scenarios.
 */
package org.jsoup.parser;

import org.jsoup.nodes.Attributes;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class TokenTest {

    /* --------------------------------------------------------------------- */
    /*  Token type helpers                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void tokenType_returnsSimpleClassName() {
        Token.Doctype doctype = new Token.Doctype();
        assertEquals("Doctype", doctype.tokenType());

        Token.StartTag start = new Token.StartTag();
        assertEquals("StartTag", start.tokenType());

        Token.EndTag end = new Token.EndTag();
        assertEquals("EndTag", end.tokenType());

        Token.Comment comment = new Token.Comment();
        assertEquals("Comment", comment.tokenType());

        Token.Character character = new Token.Character();
        assertEquals("Character", character.tokenType());

        Token.EOF eof = new Token.EOF();
        assertEquals("EOF", eof.tokenType());
    }

    @Test
    public void staticReset_clearsStringBuilder() {
        StringBuilder sb = new StringBuilder("already there");
        Token.reset(sb);
        assertEquals(0, sb.length());

        // null argument must be ignored (no NPE)
        Token.reset(null);
    }

    /* --------------------------------------------------------------------- */
    /*  Doctype                                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void doctype_gettersAndReset() {
        Token.Doctype dt = new Token.Doctype();

        // populate fields
        dt.name.append("html");
        dt.pubSysKey = "SYSTEM";
        dt.publicIdentifier.append("-//W3C//DTD HTML 4.01//EN");
        dt.systemIdentifier.append("http://www.w3.org/TR/html4/strict.dtd");
        dt.forceQuirks = true;

        // getters
        assertEquals("html", dt.getName());
        assertEquals("SYSTEM", dt.getPubSysKey());
        assertEquals("-//W3C//DTD HTML 4.01//EN", dt.getPublicIdentifier());
        assertEquals("http://www.w3.org/TR/html4/strict.dtd", dt.getSystemIdentifier());
        assertTrue(dt.isForceQuirks());

        // reset must clear everything
        dt.reset();
        assertEquals("", dt.getName());
        assertNull(dt.getPubSysKey());
        assertEquals("", dt.getPublicIdentifier());
        assertEquals("", dt.getSystemIdentifier());
        assertFalse(dt.isForceQuirks());
    }

    /* --------------------------------------------------------------------- */
    /*  Tag (via StartTag – concrete subclass)                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void tag_nameAndNormalName() {
        Token.StartTag tag = new Token.StartTag();
        tag.name("DiV");
        assertEquals("DiV", tag.name());
        assertEquals("div", tag.normalName());
    }

    @Test
    public void tag_selfClosingFlag() {
        Token.StartTag tag = new Token.StartTag();
        assertFalse(tag.isSelfClosing());
        // manipulate directly – the flag is package‑private
        tag.selfClosing = true;
        assertTrue(tag.isSelfClosing());
    }

    @Test
    public void tag_appendTagName_buildsCorrectly() {
        Token.StartTag tag = new Token.StartTag();
        tag.appendTagName("h");
        tag.appendTagName('1');
        assertEquals("h1", tag.name());
        assertEquals("h1", tag.normalName()); // lower‑casing already applied
    }

    @Test
    public void tag_appendAttributeName_andTrimOnNewAttribute() {
        Token.StartTag tag = new Token.StartTag();

        // name with surrounding whitespace – should be trimmed and then dropped
        tag.appendAttributeName("  ");
        tag.newAttribute();                       // no attribute should be added
        assertNull(tag.attributes);

        // valid name
        tag.appendAttributeName("  href ");
        tag.appendAttributeValue("http://example.com");
        tag.newAttribute();

        assertNotNull(tag.attributes);
        assertEquals("http://example.com", tag.attributes.get("href"));
    }

    @Test
    public void tag_attributeValues_singleAndMultipleAppends() {
        Token.StartTag tag = new Token.StartTag();

        // first value goes to pendingAttributeValueS
        tag.appendAttributeName("data");
        tag.appendAttributeValue("a");
        tag.newAttribute();
        assertEquals("a", tag.attributes.get("data"));

        // second value forces builder conversion
        tag.appendAttributeName("data2");
        tag.appendAttributeValue("b");   // pendingAttributeValueS = "b"
        tag.appendAttributeValue("c");   // triggers conversion, result "bc"
        tag.newAttribute();
        assertEquals("bc", tag.attributes.get("data2"));
    }

    @Test
    public void tag_emptyAttributeValue_isStoredAsEmptyString() {
        Token.StartTag tag = new Token.StartTag();

        tag.appendAttributeName("checked");
        tag.setEmptyAttributeValue();   // indicates boolean attribute with empty string
        tag.newAttribute();

        assertEquals("", tag.attributes.get("checked"));
    }

    @Test
    public void tag_attributeWithoutValue_isStoredAsNull() {
        Token.StartTag tag = new Token.StartTag();

        tag.appendAttributeName("novalue");
        // do not call any value‑related methods
        tag.newAttribute();

        assertTrue(tag.attributes.hasKey("novalue"));
        assertNull(tag.attributes.get("novalue"));
    }

    @Test
    public void tag_finaliseTag_triggersPendingAttribute() {
        Token.StartTag tag = new Token.StartTag();

        tag.appendAttributeName("title");
        tag.appendAttributeValue("Test");
        tag.finaliseTag();   // should call newAttribute internally

        assertNotNull(tag.attributes);
        assertEquals("Test", tag.attributes.get("title"));
    }

    @Test
    public void tag_reset_clearsAllFields() {
        Token.StartTag tag = new Token.StartTag();
        tag.name("p");
        tag.appendAttributeName("class");
        tag.appendAttributeValue("intro");
        tag.newAttribute();
        tag.selfClosing = true;

        tag.reset();

        assertNull(tag.tagName);
        assertNull(tag.normalName);
        assertNull(tag.pendingAttributeName);
        assertEquals(0, tag.pendingAttributeValue.length());
        assertNull(tag.pendingAttributeValueS);
        assertFalse(tag.hasEmptyAttributeValue);
        assertFalse(tag.hasPendingAttributeValue);
        assertFalse(tag.selfClosing);
        assertNull(tag.attributes);
    }

    /* --------------------------------------------------------------------- */
    /*  StartTag specific behaviour                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void startTag_nameAttr_setsBothNameAndAttributes() {
        Attributes attrs = new Attributes();
        attrs.put("id", "main");
        Token.StartTag tag = new Token.StartTag();
        tag.nameAttr("section", attrs);

        assertEquals("section", tag.name());
        assertEquals("section", tag.normalName());
        assertEquals(attrs, tag.attributes);
    }

    @Test
    public void startTag_toString_withAndWithoutAttributes() {
        Token.StartTag empty = new Token.StartTag();
        empty.name("br");
        assertEquals("<br>", empty.toString());

        Token.StartTag withAttrs = new Token.StartTag();
        withAttrs.name("a");
        withAttrs.attributes = new Attributes();
        withAttrs.attributes.put("href", "http://example.com");
        withAttrs.attributes.put("rel", "nofollow");
        // order of attributes in Attributes.toString() is undefined – compare using contains
        String result = withAttrs.toString();
        assertTrue(result.startsWith("<a "));
        assertTrue(result.endsWith(">"));
        assertTrue(result.contains("href=http://example.com"));
        assertTrue(result.contains("rel=nofollow"));
    }

    /* --------------------------------------------------------------------- */
    /*  EndTag                                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void endTag_toString() {
        Token.EndTag end = new Token.EndTag();
        end.name("div");
        assertEquals("</div>", end.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  Comment                                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void comment_getData_andToString() {
        Token.Comment comment = new Token.Comment();
        comment.data.append("Hello");
        comment.data.append(" World");
        assertEquals("Hello World", comment.getData());
        assertEquals("<!--Hello World-->", comment.toString());

        // reset must clear data and bogus flag
        comment.bogus = true;
        comment.reset();
        assertEquals("", comment.getData());
        assertFalse(comment.bogus);
    }

    /* --------------------------------------------------------------------- */
    /*  Character                                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void character_data_andToString() {
        Token.Character ch = new Token.Character();
        ch.data("x");
        assertEquals("x", ch.getData());
        assertEquals("x", ch.toString());

        ch.reset();
        assertNull(ch.getData());
    }

    /* --------------------------------------------------------------------- */
    /*  EOF                                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void eof_resetReturnsSameInstance() {
        Token.EOF eof = new Token.EOF();
        Token.EOF afterReset = (Token.EOF) eof.reset();
        assertSame(eof, afterReset);
    }

    /* --------------------------------------------------------------------- */
    /*  Type‑checking convenience methods                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void typeCheckMethods_behaveAsExpected() {
        Token.Doctype dt = new Token.Doctype();
        assertTrue(dt.isDoctype());
        assertFalse(dt.isStartTag());
        assertFalse(dt.isEndTag());
        assertFalse(dt.isComment());
        assertFalse(dt.isCharacter());
        assertFalse(dt.isEOF());
        assertSame(dt, dt.asDoctype());

        Token.StartTag st = new Token.StartTag();
        assertTrue(st.isStartTag());
        assertFalse(st.isDoctype());
        assertSame(st, st.asStartTag());

        Token.EndTag et = new Token.EndTag();
        assertTrue(et.isEndTag());
        assertSame(et, et.asEndTag());

        Token.Comment cm = new Token.Comment();
        assertTrue(cm.isComment());
        assertSame(cm, cm.asComment());

        Token.Character ch = new Token.Character();
        assertTrue(ch.isCharacter());
        assertSame(ch, ch.asCharacter());

        Token.EOF eof = new Token.EOF();
        assertTrue(eof.isEOF());
        assertSame(eof, eof.asEOF());
    }

    /* --------------------------------------------------------------------- */
    /*  Helper for EOF – expose asEOF() (not originally present)             */
    /* --------------------------------------------------------------------- */

    // The original Token class does not expose asEOF(); we add this tiny
    // helper via a subclass in the test package to keep the compilation
    // happy while still testing the semantics of the EOF token.
    private static final class EOFExposer extends Token.EOF {
        EOFExposer() { super(); }
        Token asEOF() { return this; }
    }

    @Test
    public void eof_exposerWorks() {
        EOFExposer eof = new EOFExposer();
        assertTrue(eof.isEOF());
        assertSame(eof, eof.asEOF());
    }
}
```
###Test END##