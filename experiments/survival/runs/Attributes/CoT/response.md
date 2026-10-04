###Test START##
```java
package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.helper.Validate;
import org.junit.Test;
import org.junit.Before;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Attributes}.
 *
 * The tests are organised as follows:
 * <ol>
 *   <li>Extraction of public API (see the list in the comment block).</li>
 *   <li>Basic functional tests for each public method.</li>
 *   <li>Edge‑case / exception tests (null arguments, case handling, boolean attributes, capacity growth, etc.).</li>
 *   <li>Behavioural tests for derived views (iterator, asList, dataset, html, clone, equals / hashCode, normalize).</li>
 * </ol>
 *
 * All tests run with JUnit‑4.
 */
public class AttributesTest {

    /** List of public methods of {@link Attributes} (extracted from the source). */
    /*
        public String get(String key)
        public String getIgnoreCase(String key)
        public Attributes put(String key, String value)
        public Attributes put(String key, boolean value)
        public Attributes put(Attribute attribute)
        public void remove(String key)
        public void removeIgnoreCase(String key)
        public boolean hasKey(String key)
        public boolean hasKeyIgnoreCase(String key)
        public int size()
        public void addAll(Attributes incoming)
        public Iterator<Attribute> iterator()
        public List<Attribute> asList()
        public Map<String,String> dataset()
        public String html()
        public String toString()
        public boolean equals(Object o)
        public int hashCode()
        public Attributes clone()
        public void normalize()
    */

    private Attributes attrs;

    @Before
    public void setUp() {
        attrs = new Attributes();
    }

    /* --------------------------------------------------------------------- */
    /* --------------------------- BASIC FUNCTIONALITY -------------------- */
    /* --------------------------------------------------------------------- */

    @Test
    public void getReturnsEmptyStringWhenKeyMissing() {
        assertEquals("", attrs.get("nonexistent"));
    }

    @Test
    public void putAndGetSimpleStringAttribute() {
        attrs.put("title", "Hello");
        assertEquals("Hello", attrs.get("title"));
        assertEquals(1, attrs.size());
    }

    @Test
    public void putReplacesExistingKey() {
        attrs.put("class", "a");
        attrs.put("class", "b");
        assertEquals("b", attrs.get("class"));
        assertEquals(1, attrs.size());
    }

    @Test
    public void getIgnoreCaseWorks() {
        attrs.put("Content-Type", "text/html");
        assertEquals("text/html", attrs.getIgnoreCase("content-type"));
        assertEquals("text/html", attrs.getIgnoreCase("CONTENT-TYPE"));
    }

    @Test
    public void removeDeletesExistingKey() {
        attrs.put("id", "myId");
        attrs.remove("id");
        assertFalse(attrs.hasKey("id"));
        assertEquals(0, attrs.size());
    }

    @Test
    public void removeIgnoreCaseDeletesExistingKey() {
        attrs.put("Lang", "en");
        attrs.removeIgnoreCase("lang");
        assertFalse(attrs.hasKeyIgnoreCase("LANG"));
        assertEquals(0, attrs.size());
    }

    @Test
    public void hasKeyAndHasKeyIgnoreCase() {
        attrs.put("data-foo", "bar");
        assertTrue(attrs.hasKey("data-foo"));
        assertTrue(attrs.hasKeyIgnoreCase("DATA-FOO"));
        assertFalse(attrs.hasKey("missing"));
        assertFalse(attrs.hasKeyIgnoreCase("missing"));
    }

    @Test
    public void putBooleanTrueCreatesBooleanAttribute() {
        attrs.put("checked", true);
        // Boolean attributes are stored with a null value – get returns empty string
        assertEquals("", attrs.get("checked"));
        assertTrue(attrs.hasKey("checked"));
    }

    @Test
    public void putBooleanFalseRemovesAttributeIfPresent() {
        attrs.put("readonly", true);
        assertTrue(attrs.hasKey("readonly"));
        attrs.put("readonly", false);
        assertFalse(attrs.hasKey("readonly"));
    }

    @Test
    public void putAttributeObject() {
        Attribute attr = new Attribute("href", "https://example.com");
        attrs.put(attr);
        assertEquals("https://example.com", attrs.get("href"));
        // the attribute should now point back to its parent
        assertSame(attrs, attr.parent);
    }

    @Test
    public void addAllMergesAttributes() {
        Attributes other = new Attributes();
        other.put("a", "1");
        other.put("b", "2");

        attrs.put("c", "3");
        attrs.addAll(other);

        assertEquals(3, attrs.size());
        assertEquals("1", attrs.get("a"));
        assertEquals("2", attrs.get("b"));
        assertEquals("3", attrs.get("c"));
    }

    @Test
    public void iteratorIteratesInInsertionOrderAndSupportsRemove() {
        attrs.put("x", "1");
        attrs.put("y", "2");
        attrs.put("z", "3");

        List<String> keys = new ArrayList<>();
        Iterator<Attribute> it = attrs.iterator();
        while (it.hasNext()) {
            Attribute a = it.next();
            keys.add(a.getKey());
            if ("y".equals(a.getKey())) {
                it.remove(); // remove the element while iterating
            }
        }
        assertEquals(Arrays.asList("x", "y", "z"), keys);
        assertFalse(attrs.hasKey("y"));
        assertEquals(2, attrs.size());
    }

    @Test
    public void asListIsUnmodifiableAndReflectsCurrentState() {
        attrs.put("one", "1");
        attrs.put("two", "2");
        List<Attribute> list = attrs.asList();

        assertEquals(2, list.size());
        assertEquals("one", list.get(0).getKey());

        // the list must be unmodifiable
        try {
            list.add(new Attribute("three", "3"));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        // changes in the underlying Attributes are reflected in a *new* list, not the existing one
        attrs.put("three", "3");
        List<Attribute> newList = attrs.asList();
        assertEquals(3, newList.size());
    }

    @Test
    public void datasetFiltersDataAttributesAndAllowsMutations() {
        attrs.put("data-name", "jsoup");
        attrs.put("data-count", "5");
        attrs.put("class", "test");

        Map<String, String> data = attrs.dataset();

        // only data‑* keys are exposed, and the prefix is stripped
        assertEquals(2, data.size());
        assertEquals("jsoup", data.get("name"));
        assertEquals("5", data.get("count"));
        assertNull(data.get("class"));

        // put via the dataset view adds the proper data‑* attribute
        data.put("new", "value");
        assertEquals("value", attrs.get("data-new"));
        assertEquals("value", data.get("new"));

        // remove via iterator of dataset view removes the underlying attribute
        Iterator<Map.Entry<String, String>> it = data.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if ("count".equals(e.getKey())) {
                it.remove();
            }
        }
        assertFalse(attrs.hasKey("data-count"));
    }

    @Test
    public void htmlProducesCorrectAttributeString() {
        attrs.put("id", "myId");
        attrs.put("checked", true); // boolean attribute – should render without value
        attrs.put("title", "Tom & Jerry"); // value needs escaping

        String html = attrs.html();
        // order is insertion order
        assertTrue(html.contains(" id=\"myId\""));
        assertTrue(html.contains(" checked")); // boolean attribute no ="checked"
        assertTrue(html.contains(" title=\"Tom &amp; Jerry\""));
    }

    @Test
    public void toStringDelegatesToHtml() {
        attrs.put("a", "b");
        assertEquals(attrs.html(), attrs.toString());
    }

    @Test
    public void equalsAndHashCodeWorkForIdenticalAndDifferentInstances() {
        Attributes a1 = new Attributes();
        a1.put("k1", "v1");
        a1.put("k2", "v2");

        Attributes a2 = new Attributes();
        a2.put("k1", "v1");
        a2.put("k2", "v2");

        Attributes a3 = new Attributes();
        a3.put("k1", "v1");
        a3.put("k2", "different");

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());

        assertNotEquals(a1, a3);
        assertNotEquals(a1.hashCode(), a3.hashCode());
    }

    @Test
    public void cloneCreatesDeepCopy() {
        attrs.put("x", "1");
        attrs.put("y", "2");

        Attributes copy = attrs.clone();
        assertEquals(attrs, copy);
        assertNotSame(attrs, copy);

        // modifying the original must not affect the clone
        attrs.put("z", "3");
        assertFalse(copy.hasKey("z"));
        assertEquals(2, copy.size());
    }

    @Test
    public void normalizeLowercasesAllKeys() {
        attrs.put("CaSe", "value");
        attrs.put("ANOTHER", "v");
        attrs.normalize();
        assertTrue(attrs.hasKey("case"));
        assertTrue(attrs.hasKey("another"));
        assertFalse(attrs.hasKey("CaSe"));
        assertEquals("value", attrs.get("case"));
    }

    /* --------------------------------------------------------------------- */
    /* --------------------------- EDGE CASES / EXCEPTIONS ---------------- */
    /* --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void getThrowsOnNullKey() {
        attrs.get(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getIgnoreCaseThrowsOnNullKey() {
        attrs.getIgnoreCase(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void putStringThrowsOnNullKey() {
        attrs.put(null, "value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void putAttributeThrowsOnNullAttribute() {
        attrs.put((Attribute) null);
    }

    @Test
    public void putStringAcceptsNullValueAsBooleanAttribute() {
        // According to the implementation, a null value is a boolean attribute.
        attrs.put("disabled", null);
        assertTrue(attrs.hasKey("disabled"));
        assertEquals("", attrs.get("disabled"));
    }

    @Test
    public void addAllHandlesEmptyIncomingAttributesGracefully() {
        Attributes empty = new Attributes();
        attrs.put("a", "1");
        attrs.addAll(empty);
        assertEquals(1, attrs.size());
        assertEquals("1", attrs.get("a"));
    }

    @Test
    public void iteratorRemoveOnEmptyAttributesDoesNothing() {
        // Should not throw any exception
        Iterator<Attribute> it = attrs.iterator();
        assertFalse(it.hasNext());
        // calling remove without a preceding next should be a no‑op (the implementation guards against it)
        it.remove(); // no exception expected
        assertEquals(0, attrs.size());
    }

    @Test
    public void htmlHandlesIOExceptionFromAppendable() {
        // Create a faulty Appendable that throws IOException on append
        Appendable faulty = new Appendable() {
            @Override public Appendable append(CharSequence csq) throws IOException {
                throw new IOException("forced");
            }
            @Override public Appendable append(CharSequence csq, int start, int end) throws IOException {
                throw new IOException("forced");
            }
            @Override public Appendable append(char c) throws IOException {
                throw new IOException("forced");
            }
        };
        try {
            attrs.put("a", "b");
            // invoke the package‑private html(Appendable, OutputSettings) via reflection
            java.lang.reflect.Method m = Attributes.class.getDeclaredMethod("html", Appendable.class, Document.OutputSettings.class);
            m.setAccessible(true);
            m.invoke(attrs, faulty, new Document("").outputSettings());
            fail("Expected SerializationException");
        } catch (Exception e) {
            // The reflective invocation wraps the original exception in InvocationTargetException
            Throwable cause = e.getCause();
            assertTrue(cause instanceof SerializationException);
            assertTrue(cause.getCause() instanceof IOException);
        }
    }

    @Test
    public void datasetKeyNormalization() {
        attrs.put("data-User", "Bob");
        Map<String, String> data = attrs.dataset();
        // The map key should be lower‑cased as per dataKey() implementation
        assertEquals("Bob", data.get("User")); // original case preserved after stripping prefix
        // Changing the underlying attribute updates the view
        attrs.put("data-User", "Alice");
        assertEquals("Alice", data.get("User"));
    }

    @Test
    public void capacityGrowthBeyondInitialCapacity() {
        // Initial capacity is 4; add 5 entries to trigger growth
        for (int i = 0; i < 5; i++) {
            attrs.put("k" + i, "v" + i);
        }
        assertEquals(5, attrs.size());
        for (int i = 0; i < 5; i++) {
            assertEquals("v" + i, attrs.get("k" + i));
        }
    }
}
```
###Test END##