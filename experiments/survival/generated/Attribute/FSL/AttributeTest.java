package org.jsoup.nodes;

import static org.junit.Assert.*;

import java.io.IOException;

import org.jsoup.SerializationException;
import org.jsoup.helper.Validate;
import org.jsoup.parser.Parser;
import org.junit.Test;

/**
 * Comprehensive JUnit4 tests for {@link Attribute}.
 */
public class AttributeTest {

    /* ---------- Constructor validation ---------- */

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullKey_throwsException() {
        new Attribute(null, "value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_blankKeyAfterTrim_throwsException() {
        new Attribute("   ", "value");
    }

    @Test
    public void constructor_validKeyAndValue_setsFields() {
        Attribute attr = new Attribute("  href  ", "index.html");
        assertEquals("href", attr.getKey());          // trimmed
        assertEquals("index.html", attr.getValue());
    }

    /* ---------- getKey / setKey ---------- */

    @Test(expected = IllegalArgumentException.class)
    public void setKey_null_throwsException() {
        Attribute attr = new Attribute("foo", "bar");
        attr.setKey(null);
    }

    @Test
    public void setKey_updatesParentWhenPresent() {
        Attributes parent = new Attributes();
        parent.put("foo", "val");
        Attribute attr = new Attribute("foo", "val", parent);

        // change key
        attr.setKey("bar");

        // old key should be gone, new key should map to the same value
        assertNull(parent.get("foo"));
        assertEquals("val", parent.get("bar"));
        assertEquals("bar", attr.getKey());
    }

    @Test
    public void setKey_withoutParentOnlyUpdatesSelf() {
        Attribute attr = new Attribute("a", "1");
        attr.setKey("b");
        assertEquals("b", attr.getKey());
    }

    /* ---------- getValue / setValue ---------- */

    @Test
    public void setValue_updatesParentWhenPresent() {
        Attributes parent = new Attributes();
        parent.put("key", "old");
        Attribute attr = new Attribute("key", "old", parent);

        String old = attr.setValue("new");

        assertEquals("old", old);
        assertEquals("new", attr.getValue());
        assertEquals("new", parent.get("key"));
    }

    @Test
    public void setValue_returnsOldWhenNoParent() {
        Attribute attr = new Attribute("k", "v");
        String old = attr.setValue("v2");
        assertNull(old); // parent is null, so oldVal is null
        assertEquals("v2", attr.getValue());
    }

    @Test
    public void setValue_allowsNullValue() {
        Attribute attr = new Attribute("k", "v");
        attr.setValue(null);
        assertNull(attr.getValue());
    }

    /* ---------- HTML output ---------- */

    @Test
    public void html_simpleAttribute() {
        Attribute attr = new Attribute("href", "index.html");
        assertEquals("href=\"index.html\"", attr.html());
        assertEquals(attr.html(), attr.toString());
    }

    @Test
    public void html_escapesSpecialCharacters() {
        Attribute attr = new Attribute("title", "Tom & Jerry \"Best\"");
        // Expected: title="Tom &amp; Jerry &quot;Best&quot;"
        assertEquals("title=\"Tom &amp; Jerry &quot;Best&quot;\"", attr.html());
    }

    @Test
    public void html_booleanAttributeCollapsesWhenEmpty() {
        Attribute attr = new Attribute("checked", "");
        // In HTML syntax a boolean attribute with empty value collapses to just "checked"
        assertEquals("checked", attr.html());
    }

    @Test
    public void html_booleanAttributeCollapsesWhenValueEqualsKey() {
        Attribute attr = new Attribute("checked", "checked");
        assertEquals("checked", attr.html());
    }

    @Test
    public void html_booleanAttributeDoesNotCollapseInXmlSyntax() {
        Document.OutputSettings xmlOut = new Document("").outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        StringBuilder accum = new StringBuilder();
        try {
            Attribute.html("checked", "checked", accum, xmlOut);
        } catch (IOException e) {
            throw new SerializationException(e);
        }
        // In XML syntax the attribute must be rendered with a value
        assertEquals("checked=\"checked\"", accum.toString());
    }

    @Test
    public void html_nonBooleanNeverCollapses() {
        Attribute attr = new Attribute("class", "myclass");
        assertEquals("class=\"myclass\"", attr.html());
    }

    /* ---------- createFromEncoded ---------- */

    @Test
    public void createFromEncoded_unescapesValue() {
        Attribute attr = Attribute.createFromEncoded("data", "Tom &amp; Jerry");
        assertEquals("data", attr.getKey());
        assertEquals("Tom & Jerry", attr.getValue());
    }

    /* ---------- Data attribute detection ---------- */

    @Test
    public void isDataAttribute_trueWhenStartsWithDataPrefix() {
        Attribute attr = new Attribute("data-test", "val");
        assertTrue(attr.isDataAttribute());
    }

    @Test
    public void isDataAttribute_falseWhenOnlyPrefix() {
        Attribute attr = new Attribute("data-", "val");
        assertFalse(attr.isDataAttribute());
    }

    @Test
    public void isDataAttribute_staticMethod() {
        assertTrue(Attribute.isDataAttribute("data-foo"));
        assertFalse(Attribute.isDataAttribute("href"));
        assertFalse(Attribute.isDataAttribute("data"));
    }

    /* ---------- Boolean attribute detection ---------- */

    @Test
    public void isBooleanAttribute_static_trueForKnownBoolean() {
        assertTrue(Attribute.isBooleanAttribute("checked"));
        assertTrue(Attribute.isBooleanAttribute("disabled"));
    }

    @Test
    public void isBooleanAttribute_static_falseForOther() {
        assertFalse(Attribute.isBooleanAttribute("href"));
        assertFalse(Attribute.isBooleanAttribute("custom"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void isBooleanAttribute_deprecatedMethod() {
        Attribute attr = new Attribute("checked", null);
        // The deprecated method also returns true when value is null
        assertTrue(attr.isBooleanAttribute());
    }

    /* ---------- shouldCollapseAttribute ---------- */

    @Test
    public void shouldCollapseAttribute_trueForCollapsibleBoolean() {
        Document.OutputSettings out = new Document("").outputSettings().syntax(Document.OutputSettings.Syntax.html);
        assertTrue(Attribute.shouldCollapseAttribute("checked", "", out));
        assertTrue(Attribute.shouldCollapseAttribute("checked", "checked", out));
    }

    @Test
    public void shouldCollapseAttribute_falseWhenNotBooleanOrNonHtmlSyntax() {
        Document.OutputSettings outHtml = new Document("").outputSettings().syntax(Document.OutputSettings.Syntax.html);
        Document.OutputSettings outXml = new Document("").outputSettings().syntax(Document.OutputSettings.Syntax.xml);

        // Not a boolean attribute
        assertFalse(Attribute.shouldCollapseAttribute("href", "", outHtml));

        // Boolean but non‑HTML syntax
        assertFalse(Attribute.shouldCollapseAttribute("checked", "", outXml));
    }

    /* ---------- equals and hashCode ---------- */

    @Test
    public void equals_sameKeyAndValue_true() {
        Attribute a1 = new Attribute("id", "123");
        Attribute a2 = new Attribute("id", "123");
        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    @Test
    public void equals_differentKey_false() {
        Attribute a1 = new Attribute("id", "123");
        Attribute a2 = new Attribute("class", "123");
        assertNotEquals(a1, a2);
    }

    @Test
    public void equals_differentValue_false() {
        Attribute a1 = new Attribute("id", "123");
        Attribute a2 = new Attribute("id", "456");
        assertNotEquals(a1, a2);
    }

    @Test
    public void equals_nullOrOtherClass_false() {
        Attribute attr = new Attribute("k", "v");
        assertNotEquals(attr, null);
        assertNotEquals(attr, "someString");
    }

    /* ---------- clone ---------- */

    @Test
    public void clone_createsEqualButDistinctObject() {
        Attribute original = new Attribute("src", "image.png");
        Attribute copy = original.clone();

        assertNotSame(original, copy);
        assertEquals(original, copy);
        // Changing the clone must not affect the original
        copy.setKey("data-src");
        copy.setValue("new.png");
        assertNotEquals(original.getKey(), copy.getKey());
        assertNotEquals(original.getValue(), copy.getValue());
    }

    /* ---------- parent reference edge cases ---------- */

    @Test
    public void setKey_andSetValue_withoutParent_doNotThrow() {
        Attribute attr = new Attribute("foo", "bar");
        // No parent – should just change internal state
        attr.setKey("baz");
        attr.setValue("qux");
        assertEquals("baz", attr.getKey());
        assertEquals("qux", attr.getValue());
    }
}
