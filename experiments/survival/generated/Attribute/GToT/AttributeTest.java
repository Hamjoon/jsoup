package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.helper.Validate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.junit.Assert.*;

public class AttributeTest {

    private Document.OutputSettings htmlOut;
    private Document.OutputSettings xmlOut;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        Document doc = new Document("");
        htmlOut = doc.outputSettings();               // default is HTML syntax
        xmlOut = new Document.OutputSettings();
        xmlOut.syntax(Document.OutputSettings.Syntax.xml);
    }

    // -----------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------
    @Test
    public void constructor_TrimsKeyAndStoresValue() {
        Attribute a = new Attribute("  href  ", "index.html");
        assertEquals("href", a.getKey());
        assertEquals("index.html", a.getValue());
        assertNull(a.parent);
    }

    @Test
    public void constructor_WithParent_StoresParentReference() {
        Attributes parent = new Attributes();
        parent.put("src", "image.png");
        Attribute a = new Attribute("src", "image.png", parent);
        assertSame(parent, a.parent);
    }

    @Test
    public void constructor_NullKey_ThrowsException() {
        thrown.expect(IllegalArgumentException.class);
        new Attribute(null, "val");
    }

    @Test
    public void constructor_EmptyAfterTrim_ThrowsException() {
        thrown.expect(IllegalArgumentException.class);
        new Attribute("   ", "val");
    }

    // -----------------------------------------------------------------
    // getKey / setKey
    // -----------------------------------------------------------------
    @Test
    public void getKey_ReturnsTrimmedKey() {
        Attribute a = new Attribute("  title ", "Hello");
        assertEquals("title", a.getKey());
    }

    @Test
    public void setKey_UpdatesKeyAndParentArray() {
        Attributes parent = new Attributes();
        parent.put("href", "old.html");
        Attribute a = new Attribute("href", "old.html", parent);
        a.setKey("src");
        assertEquals("src", a.getKey());
        // parent should have been updated
        assertEquals("src", parent.getKey(0));
        assertEquals("old.html", parent.get("src"));
    }

    @Test
    public void setKey_Null_ThrowsException() {
        Attribute a = new Attribute("foo", "bar");
        thrown.expect(IllegalArgumentException.class);
        a.setKey(null);
    }

    @Test
    public void setKey_TrimmedToEmpty_ThrowsException() {
        Attribute a = new Attribute("foo", "bar");
        thrown.expect(IllegalArgumentException.class);
        a.setKey("   ");
    }

    // -----------------------------------------------------------------
    // getValue / setValue
    // -----------------------------------------------------------------
    @Test
    public void getValue_ReturnsStoredValue() {
        Attribute a = new Attribute("alt", "image");
        assertEquals("image", a.getValue());
    }

    @Test
    public void setValue_UpdatesValueAndReturnsOldParentValue() {
        Attributes parent = new Attributes();
        parent.put("class", "old");
        Attribute a = new Attribute("class", "old", parent);
        String old = a.setValue("new");
        assertEquals("old", old);
        assertEquals("new", a.getValue());
        // parent must reflect change
        assertEquals("new", parent.get("class"));
    }

    @Test
    public void setValue_NoParent_ReturnsNullAndUpdatesLocalValue() {
        Attribute a = new Attribute("id", "first");
        String old = a.setValue("second");
        assertNull(old);
        assertEquals("second", a.getValue());
    }

    @Test
    public void setValue_NullIsAllowed() {
        Attribute a = new Attribute("data", "value");
        a.setValue(null);
        assertNull(a.getValue());
    }

    // -----------------------------------------------------------------
    // html / toString
    // -----------------------------------------------------------------
    @Test
    public void html_NormalAttribute_IsEscapedAndQuoted() {
        Attribute a = new Attribute("title", "Tom & Jerry");
        String html = a.html();
        assertEquals("title=\"Tom &amp; Jerry\"", html);
    }

    @Test
    public void html_BooleanAttribute_CollapsesWhenAppropriate() {
        Attribute a = new Attribute("checked", "");
        assertEquals("checked", a.html());   // empty value → collapsed
        // also test when value equals key (case‑insensitive)
        a = new Attribute("disabled", "DISABLED");
        assertEquals("disabled", a.html());
    }

    @Test
    public void html_BooleanAttribute_DoesNotCollapseInXmlSyntax() {
        Attribute a = new Attribute("checked", "");
        // force XML output
        StringBuilder accum = new StringBuilder();
        try {
            Attribute.html("checked", "", accum, new Document("").outputSettings().syntax(Document.OutputSettings.Syntax.xml));
        } catch (IOException e) {
            fail("IOException should not be thrown");
        }
        assertEquals("checked=\"\"", accum.toString());
    }

    @Test
    public void toString_DelegatesToHtml() {
        Attribute a = new Attribute("href", "a?b=c&d=e");
        assertEquals(a.html(), a.toString());
    }

    // -----------------------------------------------------------------
    // createFromEncoded
    // -----------------------------------------------------------------
    @Test
    public void createFromEncoded_DecodesEntities() {
        Attribute a = Attribute.createFromEncoded("title", "Tom &amp; Jerry");
        assertEquals("Tom & Jerry", a.getValue());
    }

    // -----------------------------------------------------------------
    // Data attribute detection
    // -----------------------------------------------------------------
    @Test
    public void isDataAttribute_Positive() {
        Attribute a = new Attribute("data-user", "bob");
        assertTrue(a.isDataAttribute());
        assertTrue(Attribute.isDataAttribute("data-foo"));
    }

    @Test
    public void isDataAttribute_Negative() {
        Attribute a = new Attribute("data", "val");
        assertFalse(a.isDataAttribute());
        assertFalse(Attribute.isDataAttribute("data-"));
    }

    // -----------------------------------------------------------------
    // shouldCollapseAttribute logic
    // -----------------------------------------------------------------
    @Test
    public void shouldCollapseAttribute_BooleanTrueInHtmlSyntax() {
        Attribute a = new Attribute("checked", "");
        assertTrue(a.shouldCollapseAttribute(htmlOut));
    }

    @Test
    public void shouldCollapseAttribute_BooleanFalseInXmlSyntax() {
        Attribute a = new Attribute("checked", "");
        assertFalse(a.shouldCollapseAttribute(xmlOut));
    }

    @Test
    public void shouldCollapseAttribute_NonBooleanNeverCollapses() {
        Attribute a = new Attribute("href", "index.html");
        assertFalse(a.shouldCollapseAttribute(htmlOut));
    }

    // -----------------------------------------------------------------
    // Boolean attribute detection
    // -----------------------------------------------------------------
    @Test
    public void isBooleanAttribute_StaticPositive() {
        assertTrue(Attribute.isBooleanAttribute("checked"));
        assertTrue(Attribute.isBooleanAttribute("disabled"));
    }

    @Test
    public void isBooleanAttribute_StaticNegative() {
        assertFalse(Attribute.isBooleanAttribute("href"));
        assertFalse(Attribute.isBooleanAttribute("custom"));
    }

    // -----------------------------------------------------------------
    // equals & hashCode
    // -----------------------------------------------------------------
    @Test
    public void equals_SameKeyAndValue_ReturnsTrue() {
        Attribute a1 = new Attribute("class", "top");
        Attribute a2 = new Attribute("class", "top");
        assertTrue(a1.equals(a2));
        assertTrue(a2.equals(a1));
    }

    @Test
    public void equals_DifferentKeyOrValue_ReturnsFalse() {
        Attribute a1 = new Attribute("class", "top");
        Attribute a2 = new Attribute("id", "top");
        Attribute a3 = new Attribute("class", "bottom");
        assertFalse(a1.equals(a2));
        assertFalse(a1.equals(a3));
    }

    @Test
    public void equals_NullOrOtherClass_ReturnsFalse() {
        Attribute a = new Attribute("foo", "bar");
        assertFalse(a.equals(null));
        assertFalse(a.equals("some string"));
    }

    @Test
    public void hashCode_ConsistentWithEquals() {
        Attribute a1 = new Attribute("style", "color:red");
        Attribute a2 = new Attribute("style", "color:red");
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    // -----------------------------------------------------------------
    // clone
    // -----------------------------------------------------------------
    @Test
    public void clone_CreatesEqualButDistinctInstance() {
        Attribute original = new Attribute("src", "image.png");
        Attribute copy = original.clone();
        assertNotSame(original, copy);
        assertEquals(original, copy);
        // modify copy – original must stay unchanged
        copy.setKey("data-src");
        copy.setValue("new.png");
        assertEquals("src", original.getKey());
        assertEquals("image.png", original.getValue());
        assertEquals("data-src", copy.getKey());
        assertEquals("new.png", copy.getValue());
    }

    // -----------------------------------------------------------------
    // html() error handling – ensure SerializationException is wrapped
    // -----------------------------------------------------------------
    @Test(expected = SerializationException.class)
    public void html_WhenAppendableThrowsIOException_ThrowsSerializationException() throws IOException {
        // Create a dummy Appendable that always throws IOException
        Appendable failing = new Appendable() {
            @Override public Appendable append(CharSequence csq) throws IOException { throw new IOException("boom"); }
            @Override public Appendable append(CharSequence csq, int start, int end) throws IOException { throw new IOException("boom"); }
            @Override public Appendable append(char c) throws IOException { throw new IOException("boom"); }
        };
        // Use reflection to invoke the protected html(Appendable, Document.OutputSettings) method
        Attribute a = new Attribute("test", "val");
        try {
            java.lang.reflect.Method m = Attribute.class.getDeclaredMethod("html", Appendable.class, Document.OutputSettings.class);
            m.setAccessible(true);
            m.invoke(a, failing, htmlOut);
        } catch (java.lang.reflect.InvocationTargetException e) {
            // unwrap and re‑throw the cause – the test expects SerializationException
            throw (RuntimeException) e.getCause();
        }
    }
}
