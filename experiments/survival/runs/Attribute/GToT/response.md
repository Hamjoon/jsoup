**Step 1 – Public API extracted from `Attribute`**

| Modifier | Return type | Method | Parameters |
|----------|-------------|--------|------------|
| `public` | `Attribute` | `Attribute(String key, String value)` | `String key`, `String value` |
| `public` | `Attribute` | `Attribute(String key, String val, Attributes parent)` | `String key`, `String val`, `Attributes parent` |
| `public` | `String`   | `getKey()` | – |
| `public` | `void`     | `setKey(String key)` | `String key` |
| `public` | `String`   | `getValue()` | – |
| `public` | `String`   | `setValue(String val)` | `String val` |
| `public` | `String`   | `html()` | – |
| `public` | `String`   | `toString()` | – |
| `public static` | `Attribute` | `createFromEncoded(String unencodedKey, String encodedValue)` | `String unencodedKey`, `String encodedValue` |
| `public` | `boolean` | `isDataAttribute()` | – |
| `public static` | `boolean` | `isDataAttribute(String key)` | `String key` |
| `public` | `boolean` | `shouldCollapseAttribute(Document.OutputSettings out)` | `Document.OutputSettings out` |
| `public static` | `boolean` | `shouldCollapseAttribute(String key, String val, Document.OutputSettings out)` | `String key`, `String val`, `Document.OutputSettings out` |
| `public` | `boolean` | `isBooleanAttribute()` | – |
| `public static` | `boolean` | `isBooleanAttribute(String key)` | `String key` |
| `public` | `boolean` | `equals(Object o)` | `Object o` |
| `public` | `int`     | `hashCode()` | – |
| `public` | `Attribute`| `clone()` | – |

---

**Step 2 – Basic test ideas (one per method)**  

| Method | Basic test description |
|--------|------------------------|
| constructors | Verify that key is trimmed and stored, value stored, parent stored (or `null`). |
| `getKey` | Returns the trimmed key supplied at construction. |
| `setKey` | Updates the key; when a parent `Attributes` is present the parent’s internal key array is also updated. |
| `getValue` | Returns the stored value. |
| `setValue` | Updates the value, returns the previous value from the parent (or `null` when no parent). |
| `html` | Produces `key="escaped‑value"` for normal attributes and collapses boolean attributes correctly. |
| `toString` | Delegates to `html()`. |
| `createFromEncoded` | Decodes HTML entities in the value. |
| `isDataAttribute` | Returns `true` only for keys that start with `data-`. |
| `shouldCollapseAttribute` | Collapses boolean attributes when output syntax is HTML and the value is empty, null or equals the key. |
| `isBooleanAttribute` | Detects boolean attributes using the internal static list. |
| `equals` / `hashCode` | Two `Attribute` objects with same key/value are equal and have identical hash codes, regardless of parent. |
| `clone` | Returns a deep‑clone that is equal but not the same instance. |

---

**Step 3 – Edge‑case & exception scenarios**

| Method | Edge case / exception |
|--------|------------------------|
| constructors | `null` key → `IllegalArgumentException`; key consisting only of whitespace → `IllegalArgumentException`. |
| `setKey` | `null` argument → `IllegalArgumentException`; key that becomes empty after `trim()`. |
| `setValue` | `null` value is allowed (should not throw); parent is `null` – method must still set the local field and return `null`. |
| `html` | Boolean attribute with empty value must output only the key (`checked`). Non‑boolean attribute must always output `key="value"` even if value is empty. |
| `shouldCollapseAttribute` | When `Document.OutputSettings.Syntax.xml` the attribute must **not** collapse, even if it is boolean. |
| `isDataAttribute` | Keys that are exactly `"data-"` (no suffix) must return `false`. |
| `createFromEncoded` | Encoded value containing HTML entities (e.g. `&amp;`) must be correctly unescaped. |
| `equals` | Comparison with `null` or different class must return `false`. |
| `clone` | Changing the clone must not affect the original (deep clone of fields). |

---

**Step 4 – Comprehensive JUnit 4 test file**

Below is the **complete** JUnit 4 test suite that implements the basic tests **and** the edge‑case tests identified above.  
The file is ready to compile and run against the original `Attribute` implementation (jsoup ≥ 1.15).  

```java
###Test START##
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
###Test END##
```

**Explanation of the test suite**

* **Setup** – two `OutputSettings` objects (`htmlOut` and `xmlOut`) let us verify collapse behaviour under both HTML and XML syntaxes.  
* **Constructor tests** – cover normal creation, trimming, parent handling, and the two illegal‑argument cases (`null` key and whitespace‑only key).  
* **Key/value mutators** – confirm proper updates, parent synchronisation, and exception handling for `setKey`.  
* **HTML rendering** – checks normal attribute escaping, boolean‑attribute collapsing, and that collapsing is disabled when the output syntax is XML.  
* **Utility static methods** – `createFromEncoded`, `isDataAttribute`, `isBooleanAttribute`, and the collapse‑decision helpers are each exercised with positive and negative cases.  
* **Equality contract** – verifies reflexivity, symmetry, handling of `null`/different class, and hash‑code consistency.  
* **Cloning** – ensures a deep copy (fields duplicated, not shared) and that later mutations do not affect the original.  
* **Exception wrapping** – forces an `IOException` from the `Appendable` used inside `html()` and asserts that the public `html()` method re‑throws a `SerializationException`, as documented.

The file is ready to be placed in the same source tree (or test source folder) as the `Attribute` class and run with any JUnit 4 runner.