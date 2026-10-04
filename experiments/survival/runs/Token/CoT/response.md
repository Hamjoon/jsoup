**1.  Public (or package‑visible) methods of `org.jsoup.parser.Token`**

| Class / Interface | Method signature | Description |
|-------------------|------------------|-------------|
| **Token** (abstract) | `String tokenType()` | Returns the simple class name of the concrete token implementation. |
|  | `static void reset(StringBuilder sb)` | Clears the supplied `StringBuilder` (no‑op if `null`). |
| **Token.Doctype** | `String getName()` | Returns the doctype name as a `String`. |
|  | `String getPubSysKey()` | Returns the public‑system key (`null` if none). |
|  | `String getPublicIdentifier()` | Returns the public identifier. |
|  | `String getSystemIdentifier()` | Returns the system identifier. |
|  | `boolean isForceQuirks()` | Returns the *force quirks* flag. |
| **Token.Tag** (abstract) | `Tag reset()` | Resets the tag fields for reuse. |
|  | `void newAttribute()` | Finalises the pending attribute and stores it in the `Attributes` map. |
|  | `void finaliseTag()` | Calls `newAttribute()` if an attribute is still pending. |
|  | `String name()` | Returns the tag name (throws if not set). |
|  | `String normalName()` | Returns the lower‑cased copy of the tag name. |
|  | `Tag name(String name)` | Sets the tag name and normalised name, returns the tag. |
|  | `boolean isSelfClosing()` | Returns the self‑closing flag. |
|  | `Attributes getAttributes()` | Returns the `Attributes` object (may be `null`). |
|  | `void appendTagName(String append)` | Appends to the tag name. |
|  | `void appendTagName(char append)` | Same as above for a single char. |
|  | `void appendAttributeName(String append)` | Appends to the pending attribute name. |
|  | `void appendAttributeName(char append)` | Same as above for a single char. |
|  | `void appendAttributeValue(String append)` | Appends to the pending attribute value (optimised for a single token). |
|  | `void appendAttributeValue(char append)` | Appends a single character to the pending attribute value. |
|  | `void appendAttributeValue(char[] append)` | Appends a character array to the pending attribute value. |
|  | `void appendAttributeValue(int[] appendCodepoints)` | Appends Unicode code‑points to the pending attribute value. |
|  | `void setEmptyAttributeValue()` | Marks the pending attribute as an empty‑string attribute. |
| **Token.StartTag** | `StartTag nameAttr(String name, Attributes attributes)` | Shortcut to set tag name and its attributes. |
|  | `String toString()` | Human readable representation (`<tag attr=val>`). |
| **Token.EndTag** | `String toString()` | Human readable representation (`</tag>`). |
| **Token.Comment** | `String getData()` | Returns the comment data. |
|  | `String toString()` | Human readable representation (`<!--data-->`). |
| **Token.Character** | `Character data(String data)` | Sets the character data. |
|  | `String getData()` | Returns the character data. |
|  | `String toString()` | Returns the character data. |
| **Token.EOF** | *(no public API other than the constructor)* |
| **Token** (type‑query helpers) | `boolean isDoctype()` | true if token is a Doctype. |
|  | `Doctype asDoctype()` | Casts to `Doctype`. |
|  | `boolean isStartTag()` | true if token is a StartTag. |
|  | `StartTag asStartTag()` | Casts to `StartTag`. |
|  | `boolean isEndTag()` | true if token is an EndTag. |
|  | `EndTag asEndTag()` | Casts to `EndTag`. |
|  | `boolean isComment()` | true if token is a Comment. |
|  | `Comment asComment()` | Casts to `Comment`. |
|  | `boolean isCharacter()` | true if token is a Character. |
|  | `Character asCharacter()` | Casts to `Character`. |
|  | `boolean isEOF()` | true if token is EOF. |

---

**2.  Basic JUnit 4 test cases (functionality)**  

Below are minimal tests that verify the *happy‑path* behaviour of each method.

```java
// TokenBasicTests.java  (package‑visible, same package as Token)
package org.jsoup.parser;

import org.jsoup.nodes.Attributes;
import org.junit.Test;
import static org.junit.Assert.*;

public class TokenBasicTests {

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
        c.getData(); // initially empty
        c.reset();   // ensure clean state
        // simulate adding data
        Token.reset(c.data); // data is package‑visible; using reflection is not needed because we are in same package
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
}
```

---

**3.  Edge‑case & exception tests**

| Edge case | Reason | Test |
|-----------|--------|------|
| `Tag.name()` called before a name is set – should throw `IllegalArgumentException` (via `Validate.isFalse`). | Defensive programming. | Expect exception. |
| `Tag.newAttribute()` when `pendingAttributeName` is only whitespace – should be ignored. | Trimming logic. | Verify no attribute added. |
| `Tag.appendAttributeValue` first call stores value in `pendingAttributeValueS`; second call forces conversion to `StringBuilder`. | Internal optimisation path. | Verify final value is concatenated correctly. |
| `Tag.appendAttributeValue` with an empty string – should keep `pendingAttributeValueS` unchanged. | No‑op handling. | Verify attribute value after `newAttribute`. |
| `Token.Doctype.reset()` clears all builders and flags. | Re‑use of token objects. | Populate fields, call `reset()`, assert cleared. |
| `Token.Comment.reset()` clears data and bogus flag. | Re‑use of comment token. | Populate, reset, assert empty and bogus `false`. |
| `Token.Character.reset()` clears data to `null`. | Re‑use of character token. | Set data, reset, assert `null`. |
| `Token.EOF.reset()` returns itself unchanged (no side‑effects). | Simple token. | Call reset and assert same instance. |
| `Token.reset(null)` is a no‑op (already covered, but keep as edge). | Null safety. | Already in basic tests. |

```java
// TokenEdgeCaseTests.java
package org.jsoup.parser;

import org.jsoup.nodes.Attributes;
import org.junit.Test;
import static org.junit.Assert.*;

public class TokenEdgeCaseTests {

    @Test(expected = IllegalArgumentException.class)
    public void tag_nameThrowsIfNotSet() {
        Token.StartTag st = new Token.StartTag();
        st.name(); // Validate.isFalse should fire
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
        st.appendAttributeValue("first"); // stored in pendingAttributeValueS
        st.appendAttributeValue("second"); // now moves to StringBuilder
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
```

---

**4.  Complete JUnit 4 test file (`TokenTest.java`)**

```java
###Test START##
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
###Test END##
```