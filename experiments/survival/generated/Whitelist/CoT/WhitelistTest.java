package org.jsoup.safety;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link Whitelist}.
 *
 * The tests are divided into three sections:
 * 1. Basic functionality (happy‑path) for each public method.
 * 2. Edge‑case / exception handling (nulls, empty strings, removing non‑existent entries, etc.).
 * 3. Behaviour of the protected helpers via a thin subclass that makes them public.
 *
 * All tests run with JUnit 4 (no JUnit‑5 features are used).
 */
public class WhitelistTest {

    /* ---------------------------------------------------------------------- */
    /* 1.  Public static factory methods                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNoneFactoryCreatesEmptyWhitelist() {
        Whitelist wl = Whitelist.none();
        // No tags should be allowed
        assertFalse(wl.isSafeTag("p"));
    }

    @Test
    public void testSimpleTextFactoryAddsCorrectTags() {
        Whitelist wl = Whitelist.simpleText();
        assertTrue(wl.isSafeTag("b"));
        assertTrue(wl.isSafeTag("em"));
        assertTrue(wl.isSafeTag("i"));
        assertTrue(wl.isSafeTag("strong"));
        assertTrue(wl.isSafeTag("u"));
        // A tag not in the list must be rejected
        assertFalse(wl.isSafeTag("a"));
    }

    @Test
    public void testBasicFactoryAddsTagsAndAttributes() {
        Whitelist wl = Whitelist.basic();
        // tags
        assertTrue(wl.isSafeTag("a"));
        assertTrue(wl.isSafeTag("blockquote"));
        // attributes
        Element a = new Element(Tag.valueOf("a"), "");
        a.attr("href", "http://example.com");
        Attribute href = new Attribute("href", "http://example.com");
        assertTrue(wl.isSafeAttribute("a", a, href));
        // enforced attribute
        Attribute rel = new Attribute("rel", "nofollow");
        assertTrue(wl.isSafeAttribute("a", a, rel));
    }

    @Test
    public void testBasicWithImagesAddsImgTagAndAttrs() {
        Whitelist wl = Whitelist.basicWithImages();
        assertTrue(wl.isSafeTag("img"));
        Element img = new Element(Tag.valueOf("img"), "");
        img.attr("src", "https://example.com/pic.png");
        Attribute src = new Attribute("src", "https://example.com/pic.png");
        assertTrue(wl.isSafeAttribute("img", img, src));
    }

    @Test
    public void testRelaxedFactoryAddsManyTagsAndAttributes() {
        Whitelist wl = Whitelist.relaxed();
        assertTrue(wl.isSafeTag("table"));
        assertTrue(wl.isSafeTag("thead"));
        // a tag with href and title allowed
        Element a = new Element(Tag.valueOf("a"), "");
        a.attr("href", "mailto:user@example.com");
        a.attr("title", "mail");
        Attribute href = new Attribute("href", "mailto:user@example.com");
        Attribute title = new Attribute("title", "mail");
        assertTrue(wl.isSafeAttribute("a", a, href));
        assertTrue(wl.isSafeAttribute("a", a, title));
    }

    /* ---------------------------------------------------------------------- */
    /* 2.  Instance configuration methods (add/remove)                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddTagsAndIsSafeTag() {
        Whitelist wl = new Whitelist();
        wl.addTags("p", "div");
        assertTrue(wl.isSafeTag("p"));
        assertTrue(wl.isSafeTag("div"));
        assertFalse(wl.isSafeTag("span"));
    }

    @Test
    public void testRemoveTagsAlsoCleansSubMaps() {
        Whitelist wl = new Whitelist()
                .addTags("p", "div")
                .addAttributes("p", "class")
                .addEnforcedAttribute("p", "align", "center")
                .addProtocols("p", "class", "http");
        // Ensure everything is present before removal
        assertTrue(wl.isSafeTag("p"));
        assertTrue(wl.isSafeAttribute("p", new Element(Tag.valueOf("p"), ""), new Attribute("class", "http://x")));
        // Remove
        wl.removeTags("p");
        assertFalse(wl.isSafeTag("p"));
        // sub‑maps for "p" must be gone
        assertNull(getPrivateField(wl, "attributes"));
        assertNull(getPrivateField(wl, "enforcedAttributes"));
        assertNull(getPrivateField(wl, "protocols"));
    }

    @Test
    public void testAddAttributesAddsToExistingAndCreatesNew() {
        Whitelist wl = new Whitelist().addTags("a");
        wl.addAttributes("a", "href", "title");
        Element a = new Element(Tag.valueOf("a"), "");
        a.attr("href", "http://example.com");
        a.attr("title", "Example");
        assertTrue(wl.isSafeAttribute("a", a, new Attribute("href", "http://example.com")));
        assertTrue(wl.isSafeAttribute("a", a, new Attribute("title", "Example")));
        // Adding more attributes should merge with existing set
        wl.addAttributes("a", "id");
        assertTrue(wl.isSafeAttribute("a", a, new Attribute("id", "myid")));
    }

    @Test
    public void testAddAttributesWithAllPseudoTag() {
        Whitelist wl = new Whitelist()
                .addTags("p", "div")
                .addAttributes(":all", "class");
        Element p = new Element(Tag.valueOf("p"), "");
        p.attr("class", "myclass");
        assertTrue(wl.isSafeAttribute("p", p, new Attribute("class", "myclass")));
        Element div = new Element(Tag.valueOf("div"), "");
        div.attr("class", "myclass");
        assertTrue(wl.isSafeAttribute("div", div, new Attribute("class", "myclass")));
    }

    @Test
    public void testRemoveAttributesWorksAndHandlesAllPseudoTag() {
        Whitelist wl = new Whitelist()
                .addTags("p", "div")
                .addAttributes(":all", "class", "id");
        // remove only "id" from all tags
        wl.removeAttributes(":all", "id");
        Element p = new Element(Tag.valueOf("p"), "");
        p.attr("class", "c");
        p.attr("id", "i");
        assertTrue(wl.isSafeAttribute("p", p, new Attribute("class", "c")));
        assertFalse(wl.isSafeAttribute("p", p, new Attribute("id", "i")));
    }

    @Test
    public void testAddEnforcedAttributeAndRetrieval() {
        Whitelist wl = new Whitelist()
                .addTags("a")
                .addEnforcedAttribute("a", "rel", "nofollow");
        Attributes enforced = wl.getEnforcedAttributes("a");
        assertEquals("nofollow", enforced.get("rel"));
    }

    @Test
    public void testRemoveEnforcedAttribute() {
        Whitelist wl = new Whitelist()
                .addTags("a")
                .addEnforcedAttribute("a", "rel", "nofollow")
                .addEnforcedAttribute("a", "target", "_blank");
        wl.removeEnforcedAttribute("a", "rel");
        Attributes enforced = wl.getEnforcedAttributes("a");
        assertNull(enforced.get("rel"));
        assertEquals("_blank", enforced.get("target"));
        // removing the last enforced attribute should delete the map entry
        wl.removeEnforcedAttribute("a", "target");
        assertTrue(wl.getEnforcedAttributes("a").isEmpty());
    }

    @Test
    public void testPreserveRelativeLinksFlagAffectsAttributeValue() {
        Whitelist wl = new Whitelist()
                .addTags("a")
                .addAttributes("a", "href")
                .addProtocols("a", "href", "http")
                .preserveRelativeLinks(false); // default is false, set explicitly for clarity

        Element a = new Element(Tag.valueOf("a"), "http://base.com/");
        a.attr("href", "/relative/path");
        Attribute href = new Attribute("href", "/relative/path");
        // isSafeAttribute will resolve to absolute and rewrite the attribute value
        assertTrue(wl.isSafeAttribute("a", a, href));
        assertEquals("http://base.com/relative/path", a.attr("href"));
    }

    @Test
    public void testAddProtocolsAndProtocolCheck() {
        Whitelist wl = new Whitelist()
                .addTags("a")
                .addAttributes("a", "href")
                .addProtocols("a", "href", "http", "https", "#");
        Element a = new Element(Tag.valueOf("a"), "");
        a.attr("href", "https://example.com");
        assertTrue(wl.isSafeAttribute("a", a, new Attribute("href", "https://example.com")));
        // anchor
        a.attr("href", "#section");
        assertTrue(wl.isSafeAttribute("a", a, new Attribute("href", "#section")));
        // disallowed protocol
        a.attr("href", "ftp://example.com");
        assertFalse(wl.isSafeAttribute("a", a, new Attribute("href", "ftp://example.com")));
    }

    @Test
    public void testRemoveProtocolsAndEmptyCleanup() {
        Whitelist wl = new Whitelist()
                .addTags("a")
                .addAttributes("a", "href")
                .addProtocols("a", "href", "http", "https");
        // remove both protocols – map entry should disappear and any protocol check will be skipped
        wl.removeProtocols("a", "href", "http", "https");
        Element a = new Element(Tag.valueOf("a"), "");
        a.attr("href", "custom://scheme");
        // No protocols left, should accept any value
        assertTrue(wl.isSafeAttribute("a", a, new Attribute("href", "custom://scheme")));
    }

    /* ---------------------------------------------------------------------- */
    /* 3.  Exception / edge‑case handling                                      */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testAddTagsRejectsNullArray() {
        new Whitelist().addTags((String[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddTagsRejectsEmptyTag() {
        new Whitelist().addTags("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveTagsRejectsNullArray() {
        new Whitelist().removeTags((String[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddAttributesRejectsNullTag() {
        new Whitelist().addAttributes(null, "href");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddAttributesRejectsNullAttributesArray() {
        new Whitelist().addAttributes("a", (String[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddAttributesRejectsZeroAttributes() {
        new Whitelist().addAttributes("a"); // varargs length == 0
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveAttributesRejectsEmptyAttributesArray() {
        new Whitelist().removeAttributes("a"); // varargs length == 0
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddEnforcedAttributeRejectsEmptyValue() {
        new Whitelist().addEnforcedAttribute("a", "rel", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveEnforcedAttributeRejectsEmptyAttribute() {
        new Whitelist().removeEnforcedAttribute("a", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddProtocolsRejectsNullTag() {
        new Whitelist().addProtocols(null, "href", "http");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddProtocolsRejectsNullProtocolArray() {
        new Whitelist().addProtocols("a", "href", (String[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveProtocolsWhenNoneExistThrows() {
        new Whitelist().removeProtocols("a", "href", "http");
    }

    /* ---------------------------------------------------------------------- */
    /* 4.  Protected helpers – exposed via a thin subclass                     */
    /* ---------------------------------------------------------------------- */

    private static class TestableWhitelist extends Whitelist {
        public boolean testIsSafeTag(String tag) {
            return super.isSafeTag(tag);
        }

        public boolean testIsSafeAttribute(String tag, Element el, Attribute attr) {
            return super.isSafeAttribute(tag, el, attr);
        }
    }

    @Test
    public void testProtectedIsSafeTagViaSubclass() {
        TestableWhitelist wl = new TestableWhitelist();
        wl.addTags("span");
        assertTrue(wl.testIsSafeTag("span"));
        assertFalse(wl.testIsSafeTag("div"));
    }

    @Test
    public void testProtectedIsSafeAttributeViaSubclass() {
        TestableWhitelist wl = new TestableWhitelist()
                .addTags("a")
                .addAttributes("a", "href")
                .addProtocols("a", "href", "http");
        Element a = new Element(Tag.valueOf("a"), "");
        a.attr("href", "http://example.com");
        Attribute href = new Attribute("href", "http://example.com");
        assertTrue(wl.testIsSafeAttribute("a", a, href));
        // disallowed protocol
        Attribute bad = new Attribute("href", "ftp://example.com");
        assertFalse(wl.testIsSafeAttribute("a", a, bad));
    }

    /* ---------------------------------------------------------------------- */
    /* 5.  Helper to peek into private fields (used only for a couple of   */
    /*     sanity checks).                                                    */
    /* ---------------------------------------------------------------------- */

    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(Object target, String fieldName) {
        try {
            java.lang.reflect.Field f = Whitelist.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
