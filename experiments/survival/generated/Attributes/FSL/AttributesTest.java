package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class AttributesTest {

    @Test
    public void testPutAndGet() {
        Attributes attrs = new Attributes();
        attrs.put("Foo", "Bar");
        assertEquals("Bar", attrs.get("Foo"));
        assertEquals("", attrs.get("Missing"));               // missing returns empty string
        assertEquals("Bar", attrs.getIgnoreCase("foo"));      // case‑insensitive get
        assertTrue(attrs.hasKey("Foo"));
        assertTrue(attrs.hasKeyIgnoreCase("foo"));
        assertEquals(1, attrs.size());
    }

    @Test
    public void testPutOverwrite() {
        Attributes attrs = new Attributes();
        attrs.put("key", "value1");
        attrs.put("key", "value2"); // overwrite
        assertEquals("value2", attrs.get("key"));
        assertEquals(1, attrs.size());
    }

    @Test
    public void testPutBooleanTrueAndFalse() {
        Attributes attrs = new Attributes();
        attrs.put("checked", true);               // boolean attribute stored as null
        assertTrue(attrs.hasKeyIgnoreCase("checked"));
        assertEquals("", attrs.get("checked"));   // null is presented as empty string
        attrs.put("checked", false);              // removes attribute
        assertFalse(attrs.hasKey("checked"));
        assertEquals(0, attrs.size());
    }

    @Test
    public void testRemoveAndRemoveIgnoreCase() {
        Attributes attrs = new Attributes();
        attrs.put("One", "1");
        attrs.put("Two", "2");
        attrs.remove("One");
        assertFalse(attrs.hasKey("One"));
        assertTrue(attrs.hasKey("Two"));
        attrs.removeIgnoreCase("tWO");
        assertFalse(attrs.hasKey("Two"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIndexOfKeyNullThrows() {
        Attributes attrs = new Attributes();
        attrs.get(null); // Validate.notNull should throw
    }

    @Test
    public void testAddAllMergesAndOverwrites() {
        Attributes a1 = new Attributes();
        a1.put("a", "1");
        a1.put("b", "2");

        Attributes a2 = new Attributes();
        a2.put("b", "22"); // same key, different value
        a2.put("c", "3");

        a1.addAll(a2);
        assertEquals(3, a1.size());
        assertEquals("1", a1.get("a"));
        assertEquals("22", a1.get("b")); // overwritten
        assertEquals("3", a1.get("c"));
    }

    @Test
    public void testIteratorAndRemoveViaIterator() {
        Attributes attrs = new Attributes();
        attrs.put("a", "1");
        attrs.put("b", "2");
        attrs.put("c", "3");

        Iterator<Attribute> it = attrs.iterator();
        while (it.hasNext()) {
            Attribute attr = it.next();
            if ("b".equals(attr.getKey())) {
                it.remove(); // remove key "b"
            }
        }
        assertEquals(2, attrs.size());
        assertFalse(attrs.hasKey("b"));
    }

    @Test
    public void testAsListIsUnmodifiable() {
        Attributes attrs = new Attributes();
        attrs.put("x", "10");
        attrs.put("y", null); // boolean attribute

        List<Attribute> list = attrs.asList();
        assertEquals(2, list.size());
        assertEquals("x", list.get(0).getKey());
        assertEquals("10", list.get(0).getValue());
        assertEquals("y", list.get(1).getKey());
        assertNull(list.get(1).getValue());

        try {
            list.add(new Attribute("z", "30"));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testDatasetView() {
        Attributes attrs = new Attributes();
        attrs.put("data-name", "Alice");
        attrs.put("data-age", "30");
        attrs.put("title", "Mr."); // non‑data attribute

        Map<String, String> data = attrs.dataset();
        assertEquals(2, data.size());
        assertEquals("Alice", data.get("name"));
        assertEquals("30", data.get("age"));
        assertNull(data.get("title"));

        // put via dataset adds a data‑ attribute
        data.put("city", "London");
        assertEquals("London", attrs.get("data-city"));
        assertEquals(3, attrs.dataset().size());

        // remove via iterator
        Iterator<Map.Entry<String, String>> it = data.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if ("age".equals(e.getKey())) {
                it.remove();
            }
        }
        assertFalse(attrs.hasKey("data-age"));
        assertEquals(2, attrs.dataset().size());
    }

    @Test
    public void testHtmlGeneration() {
        Attributes attrs = new Attributes();
        attrs.put("id", "main");
        attrs.put("checked", true); // boolean attribute (null value)
        attrs.put("value", "a\"b&c"); // contains characters needing escaping

        String html = attrs.html();
        // Expected order: id, checked, value (order of insertion)
        assertTrue(html.contains(" id=\"main\""));
        assertTrue(html.contains(" checked")); // no ="checked"
        assertTrue(html.contains(" value=\"a&quot;b&amp;c\""));
    }

    @Test
    public void testToStringDelegatesToHtml() {
        Attributes attrs = new Attributes();
        attrs.put("class", "test");
        assertEquals(attrs.html(), attrs.toString());
    }

    @Test
    public void testEqualsAndHashCode() {
        Attributes a = new Attributes();
        a.put("k1", "v1");
        a.put("k2", "v2");

        Attributes b = new Attributes();
        b.put("k1", "v1");
        b.put("k2", "v2");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        b.put("k2", "different");
        assertNotEquals(a, b);
    }

    @Test
    public void testCloneCreatesDeepCopy() {
        Attributes original = new Attributes();
        original.put("a", "1");
        original.put("b", "2");

        Attributes clone = original.clone();
        assertEquals(original, clone);
        assertNotSame(original, clone);

        // modify original – clone must stay unchanged
        original.put("a", "10");
        original.remove("b");
        assertEquals("1", clone.get("a"));
        assertEquals("2", clone.get("b"));
        assertEquals(2, clone.size());
    }

    @Test
    public void testNormalizeLowercasesKeys() {
        Attributes attrs = new Attributes();
        attrs.put("HeLLo", "world");
        attrs.put("DATA-Info", "value"); // mixed case data prefix
        attrs.normalize();

        assertTrue(attrs.hasKey("hello"));
        assertFalse(attrs.hasKey("HeLLo"));
        assertEquals("world", attrs.get("hello"));
        assertEquals("value", attrs.get("data-info"));
    }

    @Test
    public void testPutAttributeObject() {
        Attributes attrs = new Attributes();
        Attribute attr = new Attribute("custom", "val");
        attrs.put(attr);
        assertEquals("val", attrs.get("custom"));
        assertSame(attrs, attr.parent); // parent should be set
    }

    @Test
    public void testDatasetEntrySetSize() {
        Attributes attrs = new Attributes();
        attrs.put("data-a", "1");
        attrs.put("data-b", "2");
        attrs.put("c", "3");

        Map<String, String> data = attrs.dataset();
        Set<Map.Entry<String, String>> entrySet = data.entrySet();
        assertEquals(2, entrySet.size());
    }

    @Test(expected = SerializationException.class)
    public void testHtmlThrowsSerializationExceptionOnIOException() throws Exception {
        // Force an IOException by using a custom Appendable that throws
        Attributes attrs = new Attributes();
        attrs.put("key", "value");

        Appendable failingAppendable = new Appendable() {
            @Override public Appendable append(CharSequence csq) { throw new IOException(); }
            @Override public Appendable append(CharSequence csq, int start, int end) { throw new IOException(); }
            @Override public Appendable append(char c) { throw new IOException(); }
        };
        // invoke the package‑private html method via reflection
        java.lang.reflect.Method m = Attributes.class.getDeclaredMethod("html", Appendable.class, Document.OutputSettings.class);
        m.setAccessible(true);
        try {
            m.invoke(attrs, failingAppendable, (new Document("")).outputSettings());
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause(); // rethrow the underlying SerializationException
        }
    }
}
