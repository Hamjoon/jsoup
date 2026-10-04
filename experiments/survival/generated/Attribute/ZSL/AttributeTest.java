package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.helper.Validate;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

public class AttributeTest {

    private Attributes parent;

    @Before
    public void setUp() {
        parent = new Attributes();
    }

    /*** Constructor validation ***/
    @Test(expected = IllegalArgumentException.class)
    public void constructorThrowsWhenKeyIsNull() {
        new Attribute(null, "value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorThrowsWhenKeyIsEmptyAfterTrim() {
        new Attribute("   ", "value");
    }

    @Test
    public void constructorCreatesAttributeWithTrimmedKey() {
        Attribute attr = new Attribute("  foo  ", "bar");
        assertEquals("foo", attr.getKey());
        assertEquals("bar", attr.getValue());
    }

    /*** getKey / setKey ***/
    @Test
    public void setKeyUpdatesParentAttributes() {
        // put attribute into parent so the parent knows about the key/value
        parent.put("foo", "bar");
        // retrieve the underlying Attribute instance (Attributes stores keys/vals in parallel arrays)
        // For testing we construct an Attribute with the same parent reference.
        Attribute attr = new Attribute("foo", "bar", parent);
        attr.setKey("baz");

        // Parent should now contain the new key and not the old one
        assertNull(parent.get("foo"));
        assertEquals("bar", parent.get("baz"));
        assertEquals("baz", attr.getKey());
    }

    @Test
    public void setKeyTrimsAndValidates() {
        Attribute attr = new Attribute("key", "val");
        attr.setKey("   newKey   ");
        assertEquals("newKey", attr.getKey());
    }

    /*** getValue / setValue ***/
    @Test
    public void setValueUpdatesParentAttributes() {
        parent.put("myKey", "oldVal");
        Attribute attr = new Attribute("myKey", "oldVal", parent);
        String old = attr.setValue("newVal");

        // old value returned should be the previous value from the parent
        assertEquals("oldVal", old);
        // parent now reflects the new value
        assertEquals("newVal", parent.get("myKey"));
        // attribute instance also updated
        assertEquals("newVal", attr.getValue());
    }

    @Test
    public void setValueAcceptsNull() {
        Attribute attr = new Attribute("k", "v");
        attr.setValue(null);
        assertNull(attr.getValue());
    }

    /*** HTML rendering ***/
    @Test
    public void htmlEscapesValue() {
        Attribute attr = new Attribute("title", "Tom & Jerry \"Great\"");
        String html = attr.html();
        // Expected: title="Tom &amp; Jerry &quot;Great&quot;"
        assertEquals("title=\"Tom &amp; Jerry &quot;Great&quot;\"", html);
    }

    @Test
    public void toStringDelegatesToHtml() {
        Attribute attr = new Attribute("href", "a?b=c&d=e");
        assertEquals(attr.html(), attr.toString());
    }

    @Test
    public void htmlCollapsesBooleanAttributeWhenEmptyValue() {
        Attribute attr = new Attribute("checked", "");
        assertEquals("checked", attr.html());
    }

    @Test
    public void htmlCollapsesBooleanAttributeWhenValueEqualsKey() {
        Attribute attr = new Attribute("disabled", "disabled");
        assertEquals("disabled", attr.html());
    }

    @Test
    public void htmlDoesNotCollapseWhenSyntaxIsXml() throws IOException {
        StringBuilder sb = new StringBuilder();
        Document.OutputSettings out = new Document("").outputSettings();
        out.syntax(Document.OutputSettings.Syntax.xml);
        Attribute.html("checked", "checked", sb, out);
        assertEquals("checked=\"checked\"", sb.toString());
    }

    /*** createFromEncoded ***/
    @Test
    public void createFromEncodedUnescapesValue() {
        // encoded value contains an entity for ampersand
        Attribute attr = Attribute.createFromEncoded("data", "Tom &amp; Jerry");
        assertEquals("Tom & Jerry", attr.getValue());
        assertEquals("data", attr.getKey());
    }

    /*** Data attribute detection ***/
    @Test
    public void isDataAttributeDetectsCorrectly() {
        Attribute attr = new Attribute("data-user-id", "123");
        assertTrue(attr.isDataAttribute());

        Attribute nonData = new Attribute("class", "btn");
        assertFalse(nonData.isDataAttribute());
    }

    @Test
    public void staticIsDataAttributeWorks() {
        assertTrue(Attribute.isDataAttribute("data-foo"));
        assertFalse(Attribute.isDataAttribute("data"));
        assertFalse(Attribute.isDataAttribute("foo-data"));
    }

    /*** Boolean attribute utilities ***/
    @Test
    public void isBooleanAttributeStaticWorks() {
        assertTrue(Attribute.isBooleanAttribute("checked"));
        assertTrue(Attribute.isBooleanAttribute("disabled"));
        assertFalse(Attribute.isBooleanAttribute("href"));
    }

    @Test
    public void deprecatedIsBooleanAttributeMatchesStaticBehaviour() {
        Attribute attr = new Attribute("checked", null);
        // deprecated method checks both array and null value
        assertTrue(attr.isBooleanAttribute());
        Attribute nonBool = new Attribute("href", "http://example.com");
        assertFalse(nonBool.isBooleanAttribute());
    }

    /*** shouldCollapseAttribute logic ***/
    @Test
    public void shouldCollapseAttributeTrueForBooleanHtmlSyntax() {
        Document.OutputSettings out = new Document("").outputSettings();
        out.syntax(Document.OutputSettings.Syntax.html);
        assertTrue(Attribute.shouldCollapseAttribute("checked", "", out));
        assertTrue(Attribute.shouldCollapseAttribute("checked", "checked", out));
    }

    @Test
    public void shouldCollapseAttributeFalseForNonBooleanOrXmlSyntax() {
        Document.OutputSettings outHtml = new Document("").outputSettings();
        outHtml.syntax(Document.OutputSettings.Syntax.html);
        Document.OutputSettings outXml = new Document("").outputSettings();
        outXml.syntax(Document.OutputSettings.Syntax.xml);

        assertFalse(Attribute.shouldCollapseAttribute("href", "http://example.com", outHtml));
        assertFalse(Attribute.shouldCollapseAttribute("checked", "checked", outXml));
    }

    /*** Equality and hashCode ***/
    @Test
    public void equalsAndHashCodeRespectKeyAndValue() {
        Attribute a1 = new Attribute("id", "123");
        Attribute a2 = new Attribute("id", "123");
        Attribute a3 = new Attribute("id", "456");
        Attribute a4 = new Attribute("class", "123");

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());

        assertNotEquals(a1, a3);
        assertNotEquals(a1, a4);
    }

    @Test
    public void equalsHandlesNullsProperly() {
        Attribute a = new Attribute("key", null);
        Attribute b = new Attribute("key", null);
        Attribute c = new Attribute("key", "value");
        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    /*** Clone ***/
    @Test
    public void cloneCreatesEqualButDistinctInstance() {
        Attribute original = new Attribute("src", "image.png");
        Attribute copy = original.clone();

        assertEquals(original, copy);
        assertNotSame(original, copy);

        // Changing the clone must not affect the original
        copy.setKey("data-src");
        copy.setValue("new.png");

        assertNotEquals(original.getKey(), copy.getKey());
        assertNotEquals(original.getValue(), copy.getValue());
    }

    /*** Internal helper test for binarySearch pre‑condition ***/
    @Test
    public void booleanAttributesArrayIsSorted() {
        // Ensure the static array is sorted for binarySearch to work correctly.
        String[] arr = Attribute.booleanAttributes.clone();
        String[] sorted = arr.clone();
        Arrays.sort(sorted);
        assertArrayEquals(sorted, arr);
    }
}
