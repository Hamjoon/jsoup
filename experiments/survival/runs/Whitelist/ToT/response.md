###Test START##
```java
package org.jsoup.safety;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Whitelist}.
 * <p>
 * The suite is written as if three independent experts contributed one test each,
 * then merged their work into a single file.  It covers:
 * <ul>
 *   <li>Construction and basic state.</li>
 *   <li>Adding / removing tags.</li>
 *   <li>Adding / removing attributes (including the “:all” pseudo‑tag).</li>
 *   <li>Enforced attributes handling.</li>
 *   <li>Protocol whitelisting, anchor handling and relative‑link preservation.</li>
 *   <li>Edge‑case validation (nulls, empty strings, illegal removals).</li>
 *   <li>Internal helper methods such as {@code getEnforcedAttributes}.</li>
 * </ul>
 * </p>
 */
public class WhitelistTest {

    private Whitelist whitelist;

    @Before
    public void setUp() {
        whitelist = new Whitelist(); // start with a clean slate for each test
    }

    /*** Expert 1 – Tag management ***/
    @Test
    public void testAddAndRemoveTags() {
        // initially no tags are allowed
        assertFalse(whitelist.isSafeTag("p"));

        // add a few tags
        whitelist.addTags("p", "div", "span");
        assertTrue(whitelist.isSafeTag("p"));
        assertTrue(whitelist.isSafeTag("DIV")); // case‑insensitive via TagName.valueOf()
        assertTrue(whitelist.isSafeTag("span"));
        assertFalse(whitelist.isSafeTag("a"));

        // remove one tag and verify its associated maps are cleared
        whitelist.addAttributes("p", "class"); // add an attribute map that should be removed with the tag
        whitelist.addEnforcedAttribute("p", "data-test", "ok");
        whitelist.addProtocols("p", "href", "http");

        whitelist.removeTags("p");
        assertFalse(whitelist.isSafeTag("p"));
        // attribute map for 'p' must be gone
        assertNull(getPrivateField("attributes"));
        // enforced attribute map for 'p' must be gone
        assertNull(getPrivateField("enforcedAttributes"));
        // protocol map for 'p' must be gone
        assertNull(getPrivateField("protocols"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddTagsRejectsNullArray() {
        whitelist.addTags((String[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddTagsRejectsEmptyTag() {
        whitelist.addTags("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveTagsRejectsNullArray() {
        whitelist.removeTags((String[]) null);
    }

    /*** Expert 2 – Attribute handling ***/
    @Test
    public void testAddAndRemoveAttributesIncludingAllTag() {
        // add attribute to specific tag
        whitelist.addTags("a");
        whitelist.addAttributes("a", "href", "title");
        Element a = new Element(Tag.valueOf("a"), "");

        // allowed attributes
        assertTrue(whitelist.isSafeAttribute("a", a, new Attribute("href", "http://example.com")));
        assertTrue(whitelist.isSafeAttribute("a", a, new Attribute("title", "my title")));

        // disallowed attribute
        assertFalse(whitelist.isSafeAttribute("a", a, new Attribute("style", "color:red")));

        // add a global attribute via pseudo‑tag ":all"
        whitelist.addAttributes(":all", "class");
        Element div = new Element(Tag.valueOf("div"), "");
        assertTrue(whitelist.isSafeAttribute(":all", div, new Attribute("class", "myClass")));

        // now remove the global attribute
        whitelist.removeAttributes(":all", "class");
        assertFalse(whitelist.isSafeAttribute(":all", div, new Attribute("class", "myClass")));

        // remove a tag‑specific attribute and verify map cleanup
        whitelist.removeAttributes("a", "title");
        assertFalse(whitelist.isSafeAttribute("a", a, new Attribute("title", "my title")));
        // after removal the attribute set for 'a' should be empty => entry removed
        assertNull(getPrivateMapEntry("attributes", Whitelist.TagName.valueOf("a")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddAttributesRejectsEmptyAttributeArray() {
        whitelist.addAttributes("a");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveAttributesRejectsEmptyTag() {
        whitelist.removeAttributes("", "href");
    }

    /*** Expert 3 – Enforced attributes, protocols and relative‑link handling ***/
    @Test
    public void testEnforcedAttributesBehaviour() {
        whitelist.addEnforcedAttribute("a", "rel", "nofollow");
        whitelist.addTags("a"); // tag must be present for enforcement to be checked

        Element a = new Element(Tag.valueOf("a"), "");
        // attribute matches enforced value -> safe
        assertTrue(whitelist.isSafeAttribute("a", a, new Attribute("rel", "nofollow")));

        // attribute present but with wrong value -> unsafe
        assertFalse(whitelist.isSafeAttribute("a", a, new Attribute("rel", "author")));

        // attribute missing entirely -> unsafe (enforced attribute must be present)
        assertFalse(whitelist.isSafeAttribute("a", a, new Attribute("href", "http://example.com")));

        // remove the enforced attribute and verify it no longer blocks
        whitelist.removeEnforcedAttribute("a", "rel");
        assertTrue(whitelist.isSafeAttribute("a", a, new Attribute("href", "http://example.com")));
    }

    @Test
    public void testAddAndRemoveProtocolsIncludingAnchorAndRelativeLinks() {
        whitelist.addTags("a");
        whitelist.addAttributes("a", "href");
        whitelist.addProtocols("a", "href", "http", "https", "#");

        Element a = new Element(Tag.valueOf("a"), "http://base.org/");

        // allowed protocol
        assertTrue(whitelist.isSafeAttribute("a", a, new Attribute("href", "http://example.com")));

        // disallowed protocol
        assertFalse(whitelist.isSafeAttribute("a", a, new Attribute("href", "ftp://example.com")));

        // allowed anchor
        assertTrue(whitelist.isSafeAttribute("a", a, new Attribute("href", "#section1")));

        // invalid anchor (contains whitespace) -> rejected
        assertFalse(whitelist.isSafeAttribute("a", a, new Attribute("href", "#bad anchor")));

        // relative URL handling – default is to convert to absolute and reject if protocol not allowed
        Attribute relAttr = new Attribute("href", "/path/page.html");
        assertTrue(whitelist.isSafeAttribute("a", a, relAttr));
        // after conversion the attribute value should become absolute (preserveRelativeLinks == false)
        assertEquals("http://base.org/path/page.html", relAttr.getValue());

        // now preserve relative links – the original value must stay unchanged
        whitelist.preserveRelativeLinks(true);
        Attribute relAttr2 = new Attribute("href", "/another/page");
        assertTrue(whitelist.isSafeAttribute("a", a, relAttr2));
        assertEquals("/another/page", relAttr2.getValue()); // unchanged
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveProtocolsFailsWhenTagNotConfigured() {
        // No protocols have been added for <img>, removal must fail
        whitelist.removeProtocols("img", "src", "http");
    }

    @Test
    public void testRemoveProtocolsClearsMapsWhenEmpty() {
        whitelist.addTags("a");
        whitelist.addAttributes("a", "href");
        whitelist.addProtocols("a", "href", "http", "https");

        // remove both protocols – the map entries should be cleared completely
        whitelist.removeProtocols("a", "href", "http", "https");
        // internal state must no longer contain the tag entry
        assertNull(getPrivateMapEntry("protocols", Whitelist.TagName.valueOf("a")));
    }

    @Test
    public void testGetEnforcedAttributes() {
        whitelist.addEnforcedAttribute("img", "alt", "default");
        whitelist.addEnforcedAttribute("img", "width", "100");

        Attributes attrs = whitelist.getEnforcedAttributes("img");
        assertEquals(2, attrs.size());
        assertEquals("default", attrs.get("alt"));
        assertEquals("100", attrs.get("width"));
    }

    /*** Helper reflection utilities – used only by the test suite ***/
    @SuppressWarnings("unchecked")
    private Object getPrivateField(String fieldName) {
        try {
            java.lang.reflect.Field f = Whitelist.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(whitelist);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object getPrivateMapEntry(String mapField, Object key) {
        try {
            java.lang.reflect.Field f = Whitelist.class.getDeclaredField(mapField);
            f.setAccessible(true);
            java.util.Map<Object, Object> map = (java.util.Map<Object, Object>) f.get(whitelist);
            return map.get(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```
###Test END##