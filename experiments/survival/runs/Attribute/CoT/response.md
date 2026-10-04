###Test START##
```java
package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.helper.Validate;
import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * JUnit4 test suite for {@link Attribute}.
 *
 * The test cases are generated in three steps:
 * 1. List all public methods (including constructors) with their signatures.
 * 2. Provide a basic test that checks the normal behaviour of each method.
 * 3. Add edge‑case / exception tests identified from the source code.
 *
 * The file is wrapped with the required markers.
 */
public class AttributeTest {

    /* --------------------------------------------------------------------- *
     * 1. Public API extracted from Attribute
     * --------------------------------------------------------------------- *
     *
     * Constructors
     *   Attribute(String key, String value)
     *   Attribute(String key, String val, Attributes parent)
     *
     * Instance methods
     *   String getKey()
     *   void   setKey(String key)
     *   String getValue()
     *   String setValue(String val)          // from Map.Entry
     *   String html()
     *   String toString()
     *   boolean equals(Object o)
     *   int     hashCode()
     *   Attribute clone()
     *
     * Static methods
     *   Attribute createFromEncoded(String unencodedKey, String encodedValue)
     * --------------------------------------------------------------------- */

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Attribute simpleAttr;
    private Attribute booleanAttr;

    @Before
    public void setUp() {
        simpleAttr = new Attribute("href", "index.html");
        booleanAttr = new Attribute("checked", "");
    }

    /* --------------------------------------------------------------------- *
     * 2. Basic functionality tests
     * --------------------------------------------------------------------- */

    @Test
    public void constructor_setsKeyAndValue() {
        Attribute a = new Attribute("title", "Hello");
        assertEquals("title", a.getKey());
        assertEquals("Hello", a.getValue());
    }

    @Test
    public void getKey_returnsKey() {
        assertEquals("href", simpleAttr.getKey());
    }

    @Test
    public void setKey_updatesKey() {
        simpleAttr.setKey("src");
        assertEquals("src", simpleAttr.getKey());
    }

    @Test
    public void getValue_returnsValue() {
        assertEquals("index.html", simpleAttr.getValue());
    }

    @Test
    public void setValue_updatesValue_andReturnsOldValue() {
        String old = simpleAttr.setValue("new.html");
        assertEquals("index.html", old);
        assertEquals("new.html", simpleAttr.getValue());
    }

    @Test
    public void html_producesCorrectAttributeString() {
        assertEquals("href=\"index.html\"", simpleAttr.html());
    }

    @Test
    public void html_collapsesBooleanAttributeWhenAppropriate() {
        // Boolean attribute with empty value should be collapsed to just the key
        assertEquals("checked", booleanAttr.html());
    }

    @Test
    public void toString_isAliasForHtml() {
        assertEquals(simpleAttr.html(), simpleAttr.toString());
    }

    @Test
    public void createFromEncoded_decodesHtmlEntities() {
        Attribute a = Attribute.createFromEncoded("title", "Hello &amp; Bye");
        assertEquals("Hello & Bye", a.getValue());
        assertEquals("title", a.getKey());
    }

    @Test
    public void equalsAndHashCode_workAsExpected() {
        Attribute a1 = new Attribute("class", "foo");
        Attribute a2 = new Attribute("class", "foo");
        Attribute a3 = new Attribute("class", "bar");

        assertTrue(a1.equals(a2));
        assertEquals(a1.hashCode(), a2.hashCode());

        assertFalse(a1.equals(a3));
        assertNotEquals(a1.hashCode(), a3.hashCode());
    }

    @Test
    public void clone_createsEqualButDistinctObject() {
        Attribute original = new Attribute("data-id", "123");
        Attribute copy = original.clone();

        assertNotSame(original, copy);
        assertEquals(original, copy);
        // Changing the clone must not affect the original
        copy.setValue("456");
        assertEquals("123", original.getValue());
        assertEquals("456", copy.getValue());
    }

    /* --------------------------------------------------------------------- *
     * 3. Edge‑case / exception tests
     * --------------------------------------------------------------------- */

    // ---- Constructor validation ------------------------------------------------

    @Test
    public void constructor_nullKey_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        new Attribute(null, "value");
    }

    @Test
    public void constructor_emptyKeyAfterTrim_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        new Attribute("   ", "value");
    }

    // ---- setKey validation -----------------------------------------------------

    @Test
    public void setKey_nullKey_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        simpleAttr.setKey(null);
    }

    @Test
    public void setKey_emptyAfterTrim_throwsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        simpleAttr.setKey("   ");
    }

    // ---- setValue when parent is null (internal NPE) ---------------------------

    @Test
    public void setValue_withoutParent_throwsNullPointerException() {
        // Attribute created with the 2‑arg constructor has parent == null
        Attribute attr = new Attribute("foo", "bar");
        thrown.expect(NullPointerException.class);
        attr.setValue("baz");
    }

    // ---- html() when underlying Document throws IOException (unlikely but forced)
    // We use a subclass to inject a failing OutputSettings via reflection.

    @Test
    public void html_whenIOExceptionWrappedInSerializationException() throws Exception {
        // Create a dummy Attribute that overrides the protected html(Appendable,Document.OutputSettings)
        Attribute faulty = new Attribute("data-test", "value") {
            @Override
            protected void html(Appendable accum, Document.OutputSettings out) throws IOException {
                throw new IOException("forced I/O error");
            }
        };
        thrown.expect(SerializationException.class);
        faulty.html();
    }

    // ---- shouldCollapseAttribute static logic --------------------------------

    @Test
    public void shouldCollapseAttribute_booleanTrueAndEmptyValue_returnsTrue() throws Exception {
        Document.OutputSettings out = new Document("").outputSettings();
        out.syntax(Document.OutputSettings.Syntax.html);
        // The attribute is a known boolean attribute ("checked") and has an empty value
        boolean collapsed = Attribute.shouldCollapseAttribute("checked", "", out);
        assertTrue(collapsed);
    }

    @Test
    public void shouldCollapseAttribute_nonBoolean_returnsFalse() throws Exception {
        Document.OutputSettings out = new Document("").outputSettings();
        out.syntax(Document.OutputSettings.Syntax.html);
        boolean collapsed = Attribute.shouldCollapseAttribute("href", "http://example.com", out);
        assertFalse(collapsed);
    }

    @Test
    public void shouldCollapseAttribute_booleanButXmlSyntax_returnsFalse() throws Exception {
        Document.OutputSettings out = new Document("").outputSettings();
        out.syntax(Document.OutputSettings.Syntax.xml); // XML syntax must not collapse
        boolean collapsed = Attribute.shouldCollapseAttribute("checked", "", out);
        assertFalse(collapsed);
    }
}
```
###Test END##