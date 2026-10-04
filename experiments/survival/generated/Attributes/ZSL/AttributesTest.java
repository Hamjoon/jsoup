package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.helper.Validate;
import org.junit.Test;

import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit4 test suite for {@link Attributes}.
 */
public class AttributesTest {

    /** Helper to create a fresh Attributes instance with a few entries. */
    private Attributes sampleAttributes() {
        Attributes attrs = new Attributes();
        attrs.put("href", "http://example.com");
        attrs.put("id", "elem1");
        attrs.put("checked", true);               // boolean attribute (null value)
        attrs.put("data-name", "John");
        return attrs;
    }

    @Test
    public void testPutAndGet() {
        Attributes attrs = new Attributes();
        attrs.put("class", "myClass");
        assertEquals("myClass", attrs.get("class"));
        // overwrite existing key
        attrs.put("class", "newClass");
        assertEquals("newClass", attrs.get("class"));
        // get non‑existing key returns empty string
        assertEquals("", attrs.get("nonexistent"));
    }

    @Test
    public void testGetIgnoreCase() {
        Attributes attrs = new Attributes();
        attrs.put("Content-Type", "text/html");
        // exact case
        assertEquals("text/html", attrs.getIgnoreCase("Content-Type"));
        // different case
        assertEquals("text/html", attrs.getIgnoreCase("content-type"));
        // absent key
        assertEquals("", attrs.getIgnoreCase("missing"));
    }

    @Test
    public void testPutBooleanAttribute() {
        Attributes attrs = new Attributes();
        attrs.put("disabled", true);
        // Boolean attribute is stored with null value
        assertTrue(attrs.hasKey("disabled"));
        assertEquals("", attrs.get("disabled"));
        // false removes it
        attrs.put("disabled", false);
        assertFalse(attrs.hasKey("disabled"));
    }

    @Test
    public void testRemoveAndRemoveIgnoreCase() {
        Attributes attrs = sampleAttributes();
        assertTrue(attrs.hasKey("href"));
        attrs.remove("href");
        assertFalse(attrs.hasKey("href"));

        // case‑sensitive remove does not affect different case key
        attrs.put("Title", "Doc");
        attrs.removeIgnoreCase("title"); // should remove it
        assertFalse(attrs.hasKey("Title"));
    }

    @Test
    public void testHasKeyAndHasKeyIgnoreCase() {
        Attributes attrs = new Attributes();
        attrs.put("src", "image.png");
        assertTrue(attrs.hasKey("src"));
        assertFalse(attrs.hasKey("SRC"));
        assertTrue(attrs.hasKeyIgnoreCase("SRC"));
        assertFalse(attrs.hasKeyIgnoreCase("alt"));
    }

    @Test
    public void testSizeAndAddAll() {
        Attributes a1 = new Attributes();
        a1.put("a", "1");
        a1.put("b", "2");

        Attributes a2 = new Attributes();
        a2.put("c", "3");
        a2.put("d", "4");

        assertEquals(2, a1.size());
        a1.addAll(a2);
        assertEquals(4, a1.size());

        // ensure values are present
        assertEquals("3", a1.get("c"));
        assertEquals("4", a1.get("d"));
    }

    @Test
    public void testIteratorAndRemoveViaIterator() {
        Attributes attrs = new Attributes();
        attrs.put("x", "1");
        attrs.put("y", "2");
        attrs.put("z", "3");

        Iterator<Attribute> it = attrs.iterator();
        while (it.hasNext()) {
            Attribute attr = it.next();
            if ("y".equals(attr.getKey())) {
                it.remove(); // remove the attribute "y"
            }
        }
        assertEquals(2, attrs.size());
        assertFalse(attrs.hasKey("y"));
    }

    @Test
    public void testAsListAndBooleanAttributeHandling() {
        Attributes attrs = new Attributes();
        attrs.put("checked", true); // boolean attribute -> null value
        attrs.put("title", "Hello");

        // asList should contain a BooleanAttribute for "checked"
        assertEquals(2, attrs.asList().size());

        Attribute first = attrs.asList().get(0);
        assertTrue(first instanceof BooleanAttribute);
        assertEquals("checked", first.getKey());

        Attribute second = attrs.asList().get(1);
        assertFalse(second instanceof BooleanAttribute);
        assertEquals("title", second.getKey());
        assertEquals("Hello", second.getValue());
    }

    @Test
    public void testDatasetView() {
        Attributes attrs = new Attributes();
        attrs.put("data-age", "30");
        attrs.put("data-city", "Paris");
        attrs.put("class", "person");

        Map<String, String> dataset = attrs.dataset();

        // dataset should expose only data- attributes without the prefix
        assertEquals(2, dataset.size());
        assertEquals("30", dataset.get("age"));
        assertEquals("Paris", dataset.get("city"));
        assertNull(dataset.get("class"));

        // put via dataset adds a data- attribute
        dataset.put("country", "France");
        assertEquals("France", attrs.get("data-country"));
        assertEquals(3, dataset.size());

        // iterator remove on dataset removes underlying attribute
        Iterator<Map.Entry<String, String>> it = dataset.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if ("age".equals(e.getKey())) {
                it.remove();
            }
        }
        assertFalse(attrs.hasKey("data-age"));
        assertEquals(2, dataset.size());
    }

    @Test
    public void testNormalize() {
        Attributes attrs = new Attributes();
        attrs.put("HREF", "a");
        attrs.put("Content-Type", "b");
        attrs.normalize();
        assertTrue(attrs.hasKey("href"));
        assertTrue(attrs.hasKey("content-type"));
        // original mixed‑case keys should be gone
        assertFalse(attrs.hasKey("HREF"));
        assertFalse(attrs.hasKey("Content-Type"));
    }

    @Test
    public void testEqualsAndHashCode() {
        Attributes a1 = new Attributes();
        a1.put("k1", "v1");
        a1.put("k2", "v2");

        Attributes a2 = new Attributes();
        a2.put("k1", "v1");
        a2.put("k2", "v2");

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());

        a2.put("k3", "v3");
        assertNotEquals(a1, a2);
    }

    @Test
    public void testCloneIndependence() {
        Attributes original = new Attributes();
        original.put("a", "1");
        Attributes clone = original.clone();

        // they are equal initially
        assertEquals(original, clone);
        // modify clone
        clone.put("b", "2");
        assertNotEquals(original, clone);
        // original unchanged
        assertFalse(original.hasKey("b"));
        assertTrue(clone.hasKey("b"));
    }

    @Test
    public void testHtmlGeneration() {
        Attributes attrs = new Attributes();
        attrs.put("href", "http://example.com");
        attrs.put("title", "A \"quote\" & more");
        attrs.put("checked", true); // boolean attribute

        String html = attrs.html();
        // Order is the insertion order; we inserted href, title, checked
        // Expected:  href="http://example.com" title="A &quot;quote&quot; &amp; more" checked
        assertTrue(html.contains(" href=\"http://example.com\""));
        assertTrue(html.contains(" title=\"A &quot;quote&quot; &amp; more\""));
        assertTrue(html.contains(" checked")); // boolean attribute has no value part
    }

    @Test
    public void testToStringDelegatesToHtml() {
        Attributes attrs = new Attributes();
        attrs.put("id", "test");
        String html = attrs.html();
        assertEquals(html, attrs.toString());
    }

    @Test(expected = SerializationException.class)
    public void testHtmlExceptionPropagation() {
        // Force an IOException by providing a broken Appendable
        Attributes attrs = new Attributes();
        attrs.put("key", "value");
        Appendable broken = new Appendable() {
            @Override public Appendable append(CharSequence csq) throws IOException { throw new IOException(); }
            @Override public Appendable append(CharSequence csq, int start, int end) throws IOException { throw new IOException(); }
            @Override public Appendable append(char c) throws IOException { throw new IOException(); }
        };
        try {
            // Directly call the package‑private html method to trigger the exception
            attrs.html(broken, (new Document("")).outputSettings());
        } catch (IOException e) {
            // This should never happen because the Appendable throws IOException
            fail("Unexpected IOException");
        }
    }
}
