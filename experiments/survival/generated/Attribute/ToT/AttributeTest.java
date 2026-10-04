/****************************************************************************************
 * JUnit‑4 test suite for {@link org.jsoup.nodes.Attribute}
 *
 * The suite is written from the perspective of three “experts”.  Each expert contributed
 * one test per public method, covering the normal behaviour, edge cases and expected
 * error conditions.  The final file aggregates all those tests into a single,
 * comprehensive test class.
 *
 * Author:  Generated for the coding‑exercise
 * Date:    2026‑10‑04
 ****************************************************************************************/

package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.helper.Validate;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class AttributeTest {

    /* --------------------------------------------------------------
     *  Helper methods
     * -------------------------------------------------------------- */

    /** Creates a fresh {@link Attributes} container with a single entry. */
    private Attributes createParent(String key, String value) {
        Attributes attrs = new Attributes();
        attrs.put(key, value);
        return attrs;
    }

    /** Returns a {@link Document} with default output settings (HTML syntax). */
    private Document newDoc() {
        return new Document("");
    }

    /** Returns a {@link Document} configured for XML syntax (no attribute collapse). */
    private Document newXmlDoc() {
        Document doc = new Document("");
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        return doc;
    }

    /* --------------------------------------------------------------
     *  Constructor & basic getters
     * -------------------------------------------------------------- */

    @Test
    public void constructor_andGetters_normalCase() {
        Attribute a = new Attribute("href", "index.html");
        assertEquals("href", a.getKey());
        assertEquals("index.html", a.getValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullKey_throws() {
        new Attribute(null, "value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_emptyKeyAfterTrim_throws() {
        new Attribute("   ", "value");
    }

    /* --------------------------------------------------------------
     *  setKey – updates parent container and trims input
     * -------------------------------------------------------------- */

    @Test
    public void setKey_updatesParent_andTrims() {
        Attributes parent = createParent("  foo  ", "bar");
        Attribute a = new Attribute("foo", "bar", parent);

        // change key, expecting parent map to reflect the new key (trimmed)
        a.setKey("   baz   ");
        assertEquals("baz", a.getKey());
        assertTrue(parent.hasKey("baz"));
        assertFalse(parent.hasKey("foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setKey_nullKey_throws() {
        Attribute a = new Attribute("key", "val");
        a.setKey(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setKey_toEmptyAfterTrim_throws() {
        Attribute a = new Attribute("key", "val");
        a.setKey("   ");
    }

    /* --------------------------------------------------------------
     *  setValue – returns old value from parent, updates parent map
     * -------------------------------------------------------------- */

    @Test
    public void setValue_updatesParent_andReturnsOldValue() {
        Attributes parent = createParent("id", "old");
        Attribute a = new Attribute("id", "old", parent);
        String old = a.setValue("new");
        assertEquals("old", old);
        assertEquals("new", a.getValue());
        assertEquals("new", parent.get("id"));
    }

    @Test
    public void setValue_withoutParent_returnsNullOldValue() {
        Attribute a = new Attribute("name", "first");
        String old = a.setValue("second");
        assertNull(old);
        assertEquals("second", a.getValue());
    }

    /* --------------------------------------------------------------
     *  html() – string representation, attribute collapse handling
     * -------------------------------------------------------------- */

    @Test
    public void html_nonCollapsibleAttribute_isEscapedCorrectly() {
        Attribute a = new Attribute("title", "Tom & Jerry <Cartoon>");
        String html = a.html(); // defaults to HTML syntax
        // expected: title="Tom &amp; Jerry &lt;Cartoon&gt;"
        assertEquals("title=\"Tom &amp; Jerry &lt;Cartoon&gt;\"", html);
    }

    @Test
    public void html_collapsibleBooleanAttribute_isKeyOnly() {
        // "checked" is a boolean attribute (present in the static array)
        Attribute a = new Attribute("checked", "checked");
        String html = a.html();
        // In HTML syntax the attribute should collapse to just the key
        assertEquals("checked", html);
    }

    @Test
    public void html_booleanAttributeWithEmptyValue_collapses() {
        Attribute a = new Attribute("disabled", "");
        assertEquals("disabled", a.html());
    }

    @Test
    public void html_booleanAttributeInXmlSyntax_doesNotCollapse() {
        Document xmlDoc = newXmlDoc();
        Attribute a = new Attribute("readonly", "readonly");
        StringBuilder accum = new StringBuilder();
        try {
            a.html(accum, xmlDoc.outputSettings());
        } catch (IOException e) {
            fail("IOException should never happen here");
        }
        // In XML syntax the attribute must retain its value
        assertEquals("readonly=\"readonly\"", accum.toString());
    }

    @Test(expected = SerializationException.class)
    public void html_ioExceptionIsWrappedInSerializationException() throws IOException {
        // Force an IOException by passing a custom Appendable that throws
        Appendable throwingAppendable = new Appendable() {
            @Override public Appendable append(CharSequence csq) throws IOException { throw new IOException("boom"); }
            @Override public Appendable append(CharSequence csq, int start, int end) throws IOException { throw new IOException("boom"); }
            @Override public Appendable append(char c) throws IOException { throw new IOException("boom"); }
        };
        Attribute a = new Attribute("data-test", "value");
        // Directly invoke the protected method via reflection
        try {
            java.lang.reflect.Method m = Attribute.class.getDeclaredMethod("html", Appendable.class, Document.OutputSettings.class);
            m.setAccessible(true);
            m.invoke(a, throwingAppendable, newDoc().outputSettings());
        } catch (java.lang.reflect.InvocationTargetException e) {
            // unwrap the original exception
            throw (IOException) e.getCause();
        }
    }

    /* --------------------------------------------------------------
     *  createFromEncoded – unescaping of HTML encoded values
     * -------------------------------------------------------------- */

    @Test
    public void createFromEncoded_decodesEntities() {
        Attribute a = Attribute.createFromEncoded("title", "Tom &amp; Jerry &lt;Cartoon&gt;");
        assertEquals("title", a.getKey());
        assertEquals("Tom & Jerry <Cartoon>", a.getValue());
    }

    /* --------------------------------------------------------------
     *  isDataAttribute – both instance and static versions
     * -------------------------------------------------------------- */

    @Test
    public void isDataAttribute_trueWhenStartsWithDataPrefix() {
        Attribute a = new Attribute("data-user-id", "123");
        assertTrue(a.isDataAttribute());
        assertTrue(Attribute.isDataAttribute("data-foo"));
    }

    @Test
    public void isDataAttribute_falseWhenNotDataPrefixOrTooShort() {
        Attribute a = new Attribute("data-", "empty");
        assertFalse(a.isDataAttribute());
        assertFalse(Attribute.isDataAttribute("databank"));
    }

    /* --------------------------------------------------------------
     *  shouldCollapseAttribute – static helper logic
     * -------------------------------------------------------------- */

    @Test
    public void shouldCollapseAttribute_trueForBooleanWithEmptyOrSameValue_htmlSyntax() {
        Document out = newDoc().outputSettings(); // HTML syntax
        assertTrue(Attribute.shouldCollapseAttribute("checked", "", out));
        assertTrue(Attribute.shouldCollapseAttribute("checked", "checked", out));
    }

    @Test
    public void shouldCollapseAttribute_falseForNonBoolean_htmlSyntax() {
        Document out = newDoc().outputSettings();
        assertFalse(Attribute.shouldCollapseAttribute("href", "http://example.com", out));
    }

    @Test
    public void shouldCollapseAttribute_falseWhenXmlSyntaxEvenIfBoolean() {
        Document out = newXmlDoc().outputSettings(); // XML syntax
        assertFalse(Attribute.shouldCollapseAttribute("checked", "", out));
    }

    /* --------------------------------------------------------------
     *  Boolean attribute detection (binarySearch)
     * -------------------------------------------------------------- */

    @Test
    public void isBooleanAttribute_staticDetectsKnownBoolean() {
        assertTrue(Attribute.isBooleanAttribute("checked"));
        assertTrue(Attribute.isBooleanAttribute("disabled"));
    }

    @Test
    public void isBooleanAttribute_staticReturnsFalseForUnknown() {
        assertFalse(Attribute.isBooleanAttribute("href"));
        assertFalse(Attribute.isBooleanAttribute("custom"));
    }

    @Test
    public void isBooleanAttribute_instanceUsesStaticLogic() {
        Attribute a = new Attribute("checked", "checked");
        // The deprecated instance method simply forwards to the static version
        assertTrue(a.isBooleanAttribute());
    }

    /* --------------------------------------------------------------
     *  equals() and hashCode()
     * -------------------------------------------------------------- */

    @Test
    public void equalsAndHashCode_consistentForEqualObjects() {
        Attribute a1 = new Attribute("class", "button");
        Attribute a2 = new Attribute("class", "button");
        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    @Test
    public void equals_returnsFalseWhenKeyDiffers() {
        Attribute a1 = new Attribute("id", "123");
        Attribute a2 = new Attribute("name", "123");
        assertNotEquals(a1, a2);
    }

    @Test
    public void equals_returnsFalseWhenValueDiffers() {
        Attribute a1 = new Attribute("id", "123");
        Attribute a2 = new Attribute("id", "456");
        assertNotEquals(a1, a2);
    }

    @Test
    public void equals_isReflexiveAndHandlesNull() {
        Attribute a = new Attribute("src", "a.png");
        assertEquals(a, a);
        assertNotEquals(a, null);
    }

    /* --------------------------------------------------------------
     *  clone()
     * -------------------------------------------------------------- */

    @Test
    public void clone_createsEqualButDistinctInstance() {
        Attribute original = new Attribute("alt", "logo");
        Attribute copy = original.clone();
        assertNotSame(original, copy);
        assertEquals(original, copy);
        // Mutating the clone must not affect the original
        copy.setValue("new logo");
        assertEquals("logo", original.getValue());
        assertEquals("new logo", copy.getValue());
    }

    /* --------------------------------------------------------------
     *  toString() – delegated to html()
     * -------------------------------------------------------------- */

    @Test
    public void toString_delegatesToHtml() {
        Attribute a = new Attribute("src", "image.png");
        assertEquals(a.html(), a.toString());
    }
}
