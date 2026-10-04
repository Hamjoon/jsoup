###Test START##
```java
package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.helper.Validate;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Attributes}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic functionality of every public method</li>
 *   <li>edge‑case handling (null keys, case‑insensitivity, boolean attributes, empty values)</li>
 *   <li>exception scenarios (invalid arguments, I/O failures during HTML generation)</li>
 *   <li>object contracts (equals / hashCode, clone, iterator behaviour)</li>
 *   <li>the {@code dataset()} view</li>
 * </ul>
 *
 * The test class resides in the same package as {@link Attributes} so that the
 * package‑private {@code html(Appendable, Document.OutputSettings)} method can be exercised.
 */
public class AttributesTest {

    private Attributes attrs;

    @Before
    public void setUp() {
        attrs = new Attributes();
    }

    /*** --------------------------------------------------------------------
     *  1.  get / getIgnoreCase
     *  -------------------------------------------------------------------*/
    @Test
    public void getReturnsEmptyStringWhenKeyMissing() {
        assertEquals("", attrs.get("missing"));
    }

    @Test
    public void getReturnsValueWhenKeyPresent() {
        attrs.put("href", "http://example.com");
        assertEquals("http://example.com", attrs.get("href"));
    }

    @Test
    public void getIgnoreCaseIsCaseInsensitive() {
        attrs.put("Content-Type", "text/html");
        assertEquals("text/html", attrs.getIgnoreCase("content-type"));
        assertEquals("text/html", attrs.getIgnoreCase("CONTENT-TYPE"));
    }

    /*** --------------------------------------------------------------------
     *  2.  put(String, String)
     *  -------------------------------------------------------------------*/
    @Test
    public void putAddsNewAttribute() {
        attrs.put("class", "myClass");
        assertEquals("myClass", attrs.get("class"));
        assertEquals(1, attrs.size());
    }

    @Test
    public void putReplacesExistingAttribute() {
        attrs.put("id", "first");
        attrs.put("id", "second");
        assertEquals("second", attrs.get("id"));
        assertEquals(1, attrs.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void putRejectsNullKey() {
        attrs.put(null, "value");
    }

    /*** --------------------------------------------------------------------
     *  3.  put(String, boolean) – boolean attributes
     *  -------------------------------------------------------------------*/
    @Test
    public void putBooleanTrueCreatesBooleanAttribute() {
        attrs.put("checked", true);
        // boolean attributes are stored with a null value – getIgnoreCase returns empty string
        assertTrue(attrs.hasKeyIgnoreCase("checked"));
        assertEquals("", attrs.getIgnoreCase("checked"));
    }

    @Test
    public void putBooleanFalseRemovesAttributeIfPresent() {
        attrs.put("readonly", true);
        assertTrue(attrs.hasKey("readonly"));
        attrs.put("readonly", false);
        assertFalse(attrs.hasKey("readonly"));
    }

    /*** --------------------------------------------------------------------
     *  4.  put(Attribute)
     *  -------------------------------------------------------------------*/
    @Test
    public void putAttributeObjectLinksParent() {
        Attribute attr = new Attribute("data-foo", "bar");
        attrs.put(attr);
        assertSame(attrs, attr.parent);
        assertEquals("bar", attrs.get("data-foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void putAttributeRejectsNullAttribute() {
        attrs.put((Attribute) null);
    }

    /*** --------------------------------------------------------------------
     *  5.  remove / removeIgnoreCase
     *  -------------------------------------------------------------------*/
    @Test
    public void removeDeletesCaseSensitiveKey() {
        attrs.put("src", "a.png");
        attrs.remove("src");
        assertFalse(attrs.hasKey("src"));
    }

    @Test
    public void removeDoesNothingWhenKeyAbsent() {
        attrs.put("alt", "text");
        attrs.remove("title"); // not present – should not throw
        assertEquals(1, attrs.size());
    }

    @Test
    public void removeIgnoreCaseDeletesKeyIrrespectiveOfCase() {
        attrs.put("HREF", "link");
        attrs.removeIgnoreCase("href");
        assertFalse(attrs.hasKey("HREF"));
    }

    /*** --------------------------------------------------------------------
     *  6.  hasKey / hasKeyIgnoreCase
     *  -------------------------------------------------------------------*/
    @Test
    public void hasKeyAndHasKeyIgnoreCaseWorkAsExpected() {
        attrs.put("Lang", "en");
        assertTrue(attrs.hasKey("Lang"));
        assertFalse(attrs.hasKey("lang"));
        assertTrue(attrs.hasKeyIgnoreCase("lang"));
    }

    /*** --------------------------------------------------------------------
     *  7.  size
     *  -------------------------------------------------------------------*/
    @Test
    public void sizeReflectsNumberOfAttributes() {
        assertEquals(0, attrs.size());
        attrs.put("a", "1");
        attrs.put("b", "2");
        assertEquals(2, attrs.size());
        attrs.remove("a");
        assertEquals(1, attrs.size());
    }

    /*** --------------------------------------------------------------------
     *  8.  addAll
     *  -------------------------------------------------------------------*/
    @Test
    public void addAllCopiesIncomingAttributes() {
        Attributes other = new Attributes();
        other.put("x", "10");
        other.put("y", "20");

        attrs.put("z", "30");
        attrs.addAll(other);

        assertEquals(3, attrs.size());
        assertEquals("10", attrs.get("x"));
        assertEquals("20", attrs.get("y"));
        assertEquals("30", attrs.get("z"));
    }

    @Test
    public void addAllWithEmptyIncomingDoesNothing() {
        Attributes empty = new Attributes();
        attrs.put("a", "1");
        attrs.addAll(empty);
        assertEquals(1, attrs.size());
    }

    /*** --------------------------------------------------------------------
     *  9.  iterator (including remove via iterator)
     *  -------------------------------------------------------------------*/
    @Test
    public void iteratorTraversesAllAttributes() {
        attrs.put("one", "1");
        attrs.put("two", "2");
        Iterator<Attribute> it = attrs.iterator();

        Set<String> seen = new HashSet<>();
        while (it.hasNext()) {
            Attribute a = it.next();
            seen.add(a.getKey());
        }
        assertEquals(new HashSet<>(Arrays.asList("one", "two")), seen);
    }

    @Test
    public void iteratorRemoveDeletesCurrentElement() {
        attrs.put("k1", "v1");
        attrs.put("k2", "v2");
        Iterator<Attribute> it = attrs.iterator();

        while (it.hasNext()) {
            Attribute a = it.next();
            if ("k1".equals(a.getKey())) {
                it.remove(); // should delete k1
            }
        }
        assertFalse(attrs.hasKey("k1"));
        assertTrue(attrs.hasKey("k2"));
        assertEquals(1, attrs.size());
    }

    /*** --------------------------------------------------------------------
     * 10. asList – unmodifiable view
     * -------------------------------------------------------------------*/
    @Test
    public void asListReturnsUnmodifiableList() {
        attrs.put("a", "A");
        List<Attribute> list = attrs.asList();
        assertEquals(1, list.size());
        assertEquals("a", list.get(0).getKey());

        try {
            list.add(new Attribute("b", "B"));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    /*** --------------------------------------------------------------------
     * 11. dataset – custom data‑attribute view
     * -------------------------------------------------------------------*/
    @Test
    public void datasetFiltersOnlyDataAttributes() {
        attrs.put("data-name", "jsoup");
        attrs.put("class", "test");
        Map<String, String> data = attrs.dataset();

        assertEquals(1, data.size());
        assertEquals("jsoup", data.get("name"));
        assertNull(data.get("class"));

        // put via the view should add a data‑ attribute
        data.put("age", "5");
        assertEquals("5", attrs.get("data-age"));
    }

    @Test
    public void datasetIteratorRemoveDeletesUnderlyingAttribute() {
        attrs.put("data-x", "1");
        attrs.put("data-y", "2");
        Map<String, String> data = attrs.dataset();
        Iterator<Map.Entry<String, String>> it = data.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if ("x".equals(e.getKey())) {
                it.remove();
            }
        }
        assertFalse(attrs.hasKey("data-x"));
        assertTrue(attrs.hasKey("data-y"));
    }

    /*** --------------------------------------------------------------------
     * 12. html – normal rendering and exception handling
     * -------------------------------------------------------------------*/
    @Test
    public void htmlEscapesSpecialCharactersAndHandlesBooleanAttributes() {
        attrs.put("title", "Tom & Jerry");
        attrs.put("checked", true); // boolean attribute
        String html = attrs.html();

        // Expected: space before each attribute, boolean attr without value, escaped ampersand
        assertTrue(html.contains(" title=\"Tom &amp; Jerry\""));
        assertTrue(html.contains(" checked"));
        // Boolean attribute must not have ="checked"
        assertFalse(html.contains("checked=\"checked\""));
    }

    /** Helper Appendable that always throws IOException */
    private static class FailingAppendable implements Appendable {
        @Override public Appendable append(CharSequence csq) throws IOException { throw new IOException("fail"); }
        @Override public Appendable append(CharSequence csq, int start, int end) throws IOException { throw new IOException("fail"); }
        @Override public Appendable append(char c) throws IOException { throw new IOException("fail"); }
    }

    @Test(expected = SerializationException.class)
    public void htmlWrapsIOExceptionInSerializationException() throws IOException {
        // Directly call the package‑private html(Appendable, OutputSettings) to force the IOException
        attrs.put("a", "b");
        attrs.html(new FailingAppendable(), new Document("").outputSettings());
    }

    /*** --------------------------------------------------------------------
     * 13. toString delegates to html()
     * -------------------------------------------------------------------*/
    @Test
    public void toStringEqualsHtml() {
        attrs.put("id", "test");
        assertEquals(attrs.html(), attrs.toString());
    }

    /*** --------------------------------------------------------------------
     * 14. equals / hashCode contract
     * -------------------------------------------------------------------*/
    @Test
    public void equalsAndHashCodeWorkForIdenticalContent() {
        Attributes a1 = new Attributes();
        Attributes a2 = new Attributes();

        a1.put("k", "v");
        a2.put("k", "v");

        assertTrue(a1.equals(a2));
        assertTrue(a2.equals(a1));
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    @Test
    public void equalsIsFalseForDifferentContent() {
        Attributes a1 = new Attributes();
        Attributes a2 = new Attributes();

        a1.put("k1", "v1");
        a2.put("k2", "v2");

        assertFalse(a1.equals(a2));
    }

    /*** --------------------------------------------------------------------
     * 15. clone – deep copy
     * -------------------------------------------------------------------*/
    @Test
    public void cloneCreatesIndependentCopy() {
        attrs.put("a", "1");
        attrs.put("b", "2");

        Attributes copy = attrs.clone();

        // The copy should be equal but not the same instance
        assertEquals(attrs, copy);
        assertNotSame(attrs, copy);

        // Modifying the original must not affect the clone
        attrs.put("c", "3");
        assertFalse(copy.hasKey("c"));
        assertEquals(2, copy.size());
    }

    /*** --------------------------------------------------------------------
     * 16. normalize – lower‑cases all keys
     * -------------------------------------------------------------------*/
    @Test
    public void normalizeLowerCasesAllKeys() {
        attrs.put("HeAdEr", "value");
        attrs.normalize();
        assertTrue(attrs.hasKey("header"));
        assertFalse(attrs.hasKey("HeAdEr"));
    }
}
```
###Test END##