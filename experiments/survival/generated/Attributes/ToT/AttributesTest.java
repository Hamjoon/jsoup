package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.helper.Validate;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Attributes}.
 *
 * The tests are written from the perspective of three “experts” that each
 * contributed a test case for every public method.  The final suite merges
 * their contributions, checks normal, edge‑case and error behaviour and
 * ensures full coverage of the class.
 */
public class AttributesTest {

    private Attributes attrs;

    @Before
    public void setUp() {
        attrs = new Attributes();
    }

    /* --------------------------------------------------------------------- *
     *  Basic get / put / remove behaviour (Expert A)                         *
     * --------------------------------------------------------------------- */

    @Test
    public void getReturnsEmptyStringWhenKeyMissing() {
        assertEquals("", attrs.get("non‑existent"));
    }

    @Test
    public void putAddsNewAttributeAndGetRetrievesIt() {
        attrs.put("href", "http://example.com");
        assertEquals("http://example.com", attrs.get("href"));
        assertTrue(attrs.hasKey("href"));
        assertEquals(1, attrs.size());
    }

    @Test
    public void putReplacesExistingAttribute() {
        attrs.put("id", "a");
        attrs.put("id", "b");
        assertEquals("b", attrs.get("id"));
        assertEquals(1, attrs.size());
    }

    @Test
    public void removeDeletesAttributeCaseSensitive() {
        attrs.put("class", "foo");
        attrs.remove("class");
        assertFalse(attrs.hasKey("class"));
        assertEquals(0, attrs.size());
    }

    @Test
    public void removeIgnoreCaseDeletesAttributeCaseInsensitive() {
        attrs.put("Title", "Hello");
        attrs.removeIgnoreCase("title");
        assertFalse(attrs.hasKey("Title"));
    }

    /* --------------------------------------------------------------------- *
     *  Case‑insensitive lookup and insertion (Expert B)                     *
     * --------------------------------------------------------------------- */

    @Test
    public void getIgnoreCaseFindsKeyRegardlessOfCase() {
        attrs.put("Content-Type", "text/html");
        assertEquals("text/html", attrs.getIgnoreCase("content-type"));
    }

    @Test
    public void putIgnoreCaseUpdatesKeyCaseWhenNecessary() {
        attrs.put("src", "a.jpg");
        attrs.putIgnoreCase("SRC", "b.jpg"); // should replace value and keep key "SRC"
        assertEquals("b.jpg", attrs.get("SRC"));
        assertFalse(attrs.hasKey("src"));
        assertTrue(attrs.hasKey("SRC"));
    }

    @Test
    public void hasKeyIgnoreCaseWorks() {
        attrs.put("Data-Id", "123");
        assertTrue(attrs.hasKeyIgnoreCase("data-id"));
    }

    /* --------------------------------------------------------------------- *
     *  Boolean attributes handling (Expert C)                               *
     * --------------------------------------------------------------------- */

    @Test
    public void putBooleanTrueCreatesBooleanAttribute() {
        attrs.put("checked", true);
        // stored as null value, but appears as a key
        assertTrue(attrs.hasKey("checked"));
        assertEquals("", attrs.get("checked")); // null is rendered as empty string
    }

    @Test
    public void putBooleanFalseRemovesAttributeIfPresent() {
        attrs.put("disabled", true);
        assertTrue(attrs.hasKey("disabled"));
        attrs.put("disabled", false);
        assertFalse(attrs.hasKey("disabled"));
    }

    @Test
    public void htmlRendersBooleanAttributeCorrectly() {
        attrs.put("checked", true); // boolean attribute
        attrs.put("value", "on");   // normal attribute
        String html = attrs.html();
        // Boolean attribute should appear without a value, normal attribute with ="value"
        assertTrue(html.contains(" checked"));
        assertTrue(html.contains(" value=\"on\""));
        // No stray ="" for the boolean attribute
        assertFalse(html.contains("checked=\""));
    }

    /* --------------------------------------------------------------------- *
     *  Attribute collection utilities                                       *
     * --------------------------------------------------------------------- */

    @Test
    public void addAllCopiesAllAttributes() {
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
    public void asListReturnsUnmodifiableList() {
        attrs.put("one", "1");
        List<Attribute> list = attrs.asList();
        assertEquals(1, list.size());
        assertEquals("1", list.get(0).getValue());

        try {
            list.add(new Attribute("two", "2"));
            fail("List should be unmodifiable");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void iteratorIteratesInInsertionOrderAndSupportsRemove() {
        attrs.put("first", "1");
        attrs.put("second", "2");
        Iterator<Attribute> it = attrs.iterator();
        assertTrue(it.hasNext());
        Attribute a1 = it.next();
        assertEquals("first", a1.getKey());
        it.remove(); // remove "first"
        assertFalse(attrs.hasKey("first"));
        assertEquals(1, attrs.size());

        Attribute a2 = it.next();
        assertEquals("second", a2.getKey());
    }

    /* --------------------------------------------------------------------- *
     *  Dataset view (data‑* attributes)                                     *
     * --------------------------------------------------------------------- */

    @Test
    public void datasetProvidesMapViewOfDataAttributes() {
        attrs.put("data-name", "jsoup");
        attrs.put("data-value", "42");
        attrs.put("class", "test");
        Map<String, String> ds = attrs.dataset();

        assertEquals(2, ds.size());
        assertEquals("jsoup", ds.get("name"));
        assertEquals("42", ds.get("value"));
        assertNull(ds.get("class"));

        // put via dataset map should prepend the prefix automatically
        ds.put("extra", "yes");
        assertEquals("yes", attrs.get("data-extra"));
    }

    @Test
    public void datasetIteratorRemovesUnderlyingAttribute() {
        attrs.put("data-x", "1");
        attrs.put("data-y", "2");
        Iterator<Map.Entry<String, String>> it = attrs.dataset().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if ("x".equals(e.getKey())) {
                it.remove();
            }
        }
        assertFalse(attrs.hasKey("data-x"));
        assertTrue(attrs.hasKey("data-y"));
    }

    /* --------------------------------------------------------------------- *
     *  Equality, hashCode and cloning                                        *
     * --------------------------------------------------------------------- */

    @Test
    public void equalsAndHashCodeRespectContent() {
        Attributes a1 = new Attributes().put("k1", "v1").put("k2", "v2");
        Attributes a2 = new Attributes().put("k1", "v1").put("k2", "v2");
        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());

        a2.put("k3", "v3");
        assertNotEquals(a1, a2);
    }

    @Test
    public void cloneCreatesDeepCopy() {
        attrs.put("one", "1");
        attrs.put("data-num", "10");
        Attributes copy = attrs.clone();

        // modify original
        attrs.put("one", "99");
        attrs.remove("data-num");

        // copy must stay unchanged
        assertEquals("1", copy.get("one"));
        assertEquals("10", copy.get("data-num"));
        assertEquals(2, copy.size());
    }

    /* --------------------------------------------------------------------- *
     *  Normalisation (lower‑casing of keys)                                 *
     * --------------------------------------------------------------------- */

    @Test
    public void normalizeLowercasesAllKeys() {
        attrs.put("HeAdEr", "value");
        attrs.put("DATA-Info", "x");
        attrs.normalize();
        assertTrue(attrs.hasKey("header"));
        assertTrue(attrs.hasKey("data-info"));
        // original mixed‑case keys are gone
        assertFalse(attrs.hasKey("HeAdEr"));
    }

    /* --------------------------------------------------------------------- *
     *  Defensive checks (null arguments)                                    *
     * --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void indexOfKeyRejectsNull() {
        attrs.indexOfKey(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void indexOfKeyIgnoreCaseRejectsNull() {
        // use reflection to call the private method – easier is to trigger via public API
        attrs.putIgnoreCase(null, "val");
    }

    @Test(expected = IllegalArgumentException.class)
    public void putAttributeRejectsNullKey() {
        attrs.put(null, "value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void putAttributeRejectsNullAttribute() {
        attrs.put((Attribute) null);
    }

    /* --------------------------------------------------------------------- *
     *  Capacity growth (internal check)                                      *
     * --------------------------------------------------------------------- */

    @Test
    public void addingMoreThanInitialCapacityGrowsArrays() {
        // Initial capacity is 4; add 6 entries to force growth
        for (int i = 0; i < 6; i++) {
            attrs.put("k" + i, "v" + i);
        }
        assertEquals(6, attrs.size());

        // Verify that all values are still accessible
        for (int i = 0; i < 6; i++) {
            assertEquals("v" + i, attrs.get("k" + i));
        }
    }

    /* --------------------------------------------------------------------- *
     *  toString delegates to html()                                         *
     * --------------------------------------------------------------------- */

    @Test
    public void toStringReturnsSameAsHtml() {
        attrs.put("a", "b");
        attrs.put("c", "d");
        assertEquals(attrs.html(), attrs.toString());
    }

    /* --------------------------------------------------------------------- *
     *  HTML generation edge cases (Attribute with value equal to key)       *
     * --------------------------------------------------------------------- */

    @Test
    public void htmlEscapesAttributeValues() {
        attrs.put("title", "Tom & Jerry");
        String html = attrs.html();
        assertTrue(html.contains("title=\"Tom &amp; Jerry\""));
    }

    @Test
    public void htmlDoesNotDuplicateBooleanAttributeWhenValueEqualsKey() {
        // Boolean attribute where value == key (e.g., selected="selected")
        attrs.put("selected", "selected"); // not a boolean attribute, but should be rendered as normal
        String html = attrs.html();
        assertTrue(html.contains("selected=\"selected\""));
    }
}
