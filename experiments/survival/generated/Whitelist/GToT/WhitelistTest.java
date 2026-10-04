package org.jsoup.safety;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Whitelist}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public API methods (construction, configuration, queries)</li>
 *   <li>Typical use‑cases (default whitelists, chaining)</li>
 *   <li>Edge‑cases and validation (null/empty parameters, removing non‑existent entries)</li>
 *   <li>Exception scenarios (illegal arguments, illegal state)</li>
 *   <li>Behaviour of protected helper methods via a test‑only subclass</li>
 * </ul>
 */
public class WhitelistTest {

    /**
     * Helper subclass that exposes the protected methods so they can be unit‑tested.
     */
    private static class TestableWhitelist extends Whitelist {
        @Override
        public boolean isSafeTag(String tag) {
            return super.isSafeTag(tag);
        }

        @Override
        public boolean isSafeAttribute(String tagName, Element el, Attribute attr) {
            return super.isSafeAttribute(tagName, el, attr);
        }

        @Override
        public Attributes getEnforcedAttributes(String tagName) {
            return super.getEnforcedAttributes(tagName);
        }
    }

    /* --------------------------------------------------------------------- *
     *  Basic factory methods
     * --------------------------------------------------------------------- */

    @Test
    public void noneFactoryCreatesEmptyWhitelist() {
        Whitelist wl = Whitelist.none();
        assertNotNull(wl);
        assertFalse(((TestableWhitelist) wl).isSafeTag("p"));
    }

    @Test
    public void simpleTextFactoryAddsExpectedTags() {
        Whitelist wl = Whitelist.simpleText();
        TestableWhitelist twl = (TestableWhitelist) wl;
        assertTrue(twl.isSafeTag("b"));
        assertTrue(twl.isSafeTag("em"));
        assertTrue(twl.isSafeTag("i"));
        assertTrue(twl.isSafeTag("strong"));
        assertTrue(twl.isSafeTag("u"));
        // unrelated tag must be unsafe
        assertFalse(twl.isSafeTag("div"));
    }

    @Test
    public void basicFactoryHasEnforcedRelNoFollow() {
        Whitelist wl = Whitelist.basic();
        TestableWhitelist twl = (TestableWhitelist) wl;
        Attributes enforced = twl.getEnforcedAttributes("a");
        assertEquals("nofollow", enforced.get("rel"));
    }

    @Test
    public void basicWithImagesAddsImgTagAndAttributes() {
        Whitelist wl = Whitelist.basicWithImages();
        TestableWhitelist twl = (TestableWhitelist) wl;
        assertTrue(twl.isSafeTag("img"));
        // check that img has the expected attribute set
        Element img = new Element(Tag.valueOf("img"), "", new Attributes().put("src", "http://example.com"));
        Attribute srcAttr = img.attributes().getIgnoreCase("src");
        assertTrue(twl.isSafeAttribute("img", img, srcAttr));
    }

    @Test
    public void relaxedFactoryContainsWideRangeOfTags() {
        Whitelist wl = Whitelist.relaxed();
        TestableWhitelist twl = (TestableWhitelist) wl;
        String[] expected = {"table", "thead", "tbody", "tr", "td", "th", "caption", "col", "colgroup"};
        for (String tag : expected) {
            assertTrue("Tag " + tag + " should be allowed", twl.isSafeTag(tag));
        }
    }

    /* --------------------------------------------------------------------- *
     *  addTags / removeTags
     * --------------------------------------------------------------------- */

    @Test
    public void addTagsAddsMultipleTagsAndChains() {
        Whitelist wl = new Whitelist().addTags("p", "div", "span");
        TestableWhitelist twl = (TestableWhitelist) wl;
        assertTrue(twl.isSafeTag("p"));
        assertTrue(twl.isSafeTag("div"));
        assertTrue(twl.isSafeTag("span"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addTagsRejectsNullArray() {
        new Whitelist().addTags((String[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addTagsRejectsEmptyString() {
        new Whitelist().addTags("");
    }

    @Test
    public void removeTagsRemovesExistingAndLeavesOthersIntact() {
        Whitelist wl = new Whitelist().addTags("p", "div", "span");
        wl.removeTags("div");
        TestableWhitelist twl = (TestableWhitelist) wl;
        assertTrue(twl.isSafeTag("p"));
        assertFalse(twl.isSafeTag("div"));
        assertTrue(twl.isSafeTag("span"));
    }

    @Test
    public void removeTagsOnNonExistingTagDoesNotThrow() {
        Whitelist wl = new Whitelist().addTags("p");
        // should silently ignore unknown tag
        wl.removeTags("unknown");
        assertTrue(((TestableWhitelist) wl).isSafeTag("p"));
    }

    /* --------------------------------------------------------------------- *
     *  addAttributes / removeAttributes
     * --------------------------------------------------------------------- */

    @Test
    public void addAttributesAddsToSpecificTagAndCreatesTagIfMissing() {
        Whitelist wl = new Whitelist().addAttributes("a", "href", "title");
        // tag 'a' should now be allowed
        assertTrue(((TestableWhitelist) wl).isSafeTag("a"));
        // attribute check via isSafeAttribute
        Element a = new Element(Tag.valueOf("a"), "", new Attributes().put("href", "http://example.com"));
        Attribute href = a.attributes().getIgnoreCase("href");
        assertTrue(((TestableWhitelist) wl).isSafeAttribute("a", a, href));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addAttributesRejectsNullTag() {
        new Whitelist().addAttributes(null, "href");
    }

    @Test(expected = IllegalArgumentException.class)
    public void addAttributesRejectsEmptyAttributesArray() {
        new Whitelist().addAttributes("a");
    }

    @Test(expected = IllegalArgumentException.class)
    public void addAttributesRejectsEmptyAttributeName() {
        new Whitelist().addAttributes("a", "");
    }

    @Test
    public void removeAttributesWorksAndCleansUpEmptyMaps() {
        Whitelist wl = new Whitelist().addAttributes("a", "href", "title");
        wl.removeAttributes("a", "title");
        // after removal, only href should remain
        Element a = new Element(Tag.valueOf("a"), "", new Attributes().put("href", "http://example.com"));
        Attribute href = a.attributes().getIgnoreCase("href");
        Attribute title = new Attribute("title", "mytitle");
        TestableWhitelist twl = (TestableWhitelist) wl;
        assertTrue(twl.isSafeAttribute("a", a, href));
        assertFalse(twl.isSafeAttribute("a", a, title));
    }

    @Test
    public void removeAttributesAllTagWildcardRemovesFromAllTags() {
        Whitelist wl = new Whitelist()
                .addAttributes("a", "href")
                .addAttributes("p", "class");
        wl.removeAttributes(":all", "href"); // should remove href from a, leave p untouched
        TestableWhitelist twl = (TestableWhitelist) wl;
        Element a = new Element(Tag.valueOf("a"), "", new Attributes().put("href", "http://example.com"));
        Element p = new Element(Tag.valueOf("p"), "", new Attributes().put("class", "c"));
        assertFalse(twl.isSafeAttribute("a", a, a.attributes().getIgnoreCase("href")));
        assertTrue(twl.isSafeAttribute("p", p, p.attributes().getIgnoreCase("class")));
    }

    /* --------------------------------------------------------------------- *
     *  addEnforcedAttribute / removeEnforcedAttribute
     * --------------------------------------------------------------------- */

    @Test
    public void addEnforcedAttributeIsReturnedByGetEnforcedAttributes() {
        Whitelist wl = new Whitelist().addEnforcedAttribute("a", "rel", "nofollow");
        Attributes attrs = ((TestableWhitelist) wl).getEnforcedAttributes("a");
        assertEquals("nofollow", attrs.get("rel"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addEnforcedAttributeRejectsEmptyTag() {
        new Whitelist().addEnforcedAttribute("", "rel", "nofollow");
    }

    @Test(expected = IllegalArgumentException.class)
    public void addEnforcedAttributeRejectsEmptyAttribute() {
        new Whitelist().addEnforcedAttribute("a", "", "nofollow");
    }

    @Test(expected = IllegalArgumentException.class)
    public void addEnforcedAttributeRejectsEmptyValue() {
        new Whitelist().addEnforcedAttribute("a", "rel", "");
    }

    @Test
    public void removeEnforcedAttributeClearsMapWhenEmpty() {
        Whitelist wl = new Whitelist().addEnforcedAttribute("a", "rel", "nofollow");
        wl.removeEnforcedAttribute("a", "rel");
        Attributes attrs = ((TestableWhitelist) wl).getEnforcedAttributes("a");
        assertTrue(attrs.isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  preserveRelativeLinks
     * --------------------------------------------------------------------- */

    @Test
    public void preserveRelativeLinksAffectsProtocolTesting() {
        // whitelist that allows http and relative URLs on <a href>
        Whitelist wl = new Whitelist()
                .addTags("a")
                .addAttributes("a", "href")
                .addProtocols("a", "href", "http", "#");
        TestableWhitelist twl = (TestableWhitelist) wl;

        // Document with base URI to resolve relative URLs
        Document doc = Jsoup.parse("<a href=\"/relative/path\">link</a>", "http://example.com/");
        Element a = doc.selectFirst("a");
        Attribute href = a.attributes().getIgnoreCase("href");

        // default: relative links are converted to absolute, then validated → should be allowed
        assertTrue(twl.isSafeAttribute("a", a, href));
        assertEquals("http://example.com/relative/path", href.getValue());

        // disable preservation → attribute value is forced to absolute (already is) – still allowed
        wl.preserveRelativeLinks(false);
        href.setValue("/relative/path"); // reset to relative
        assertTrue(twl.isSafeAttribute("a", a, href));
        assertEquals("http://example.com/relative/path", href.getValue());

        // enable preservation → relative value stays relative, but still considered safe because "#" protocol not needed
        wl.preserveRelativeLinks(true);
        href.setValue("/relative/path");
        assertTrue(twl.isSafeAttribute("a", a, href));
        assertEquals("/relative/path", href.getValue()); // unchanged
    }

    /* --------------------------------------------------------------------- *
     *  addProtocols / removeProtocols
     * --------------------------------------------------------------------- */

    @Test
    public void addProtocolsAllowsOnlySpecifiedProtocols() {
        Whitelist wl = new Whitelist()
                .addTags("a")
                .addAttributes("a", "href")
                .addProtocols("a", "href", "http", "https");
        TestableWhitelist twl = (TestableWhitelist) wl;

        Document doc = Jsoup.parse("<a href=\"http://good.com\">good</a>", "http://base/");
        Element a = doc.selectFirst("a");
        Attribute href = a.attributes().getIgnoreCase("href");
        assertTrue(twl.isSafeAttribute("a", a, href));

        // change to a disallowed protocol
        href.setValue("ftp://bad.com");
        assertFalse(twl.isSafeAttribute("a", a, href));
    }

    @Test
    public void addProtocolsSupportsAnchorHash() {
        Whitelist wl = new Whitelist()
                .addTags("a")
                .addAttributes("a", "href")
                .addProtocols("a", "href", "#");
        TestableWhitelist twl = (TestableWhitelist) wl;

        Element a = new Element(Tag.valueOf("a"), "", new Attributes().put("href", "#section"));
        Attribute href = a.attributes().getIgnoreCase("href");
        assertTrue(twl.isSafeAttribute("a", a, href));

        // malformed anchor (contains space) should be rejected
        href.setValue("#bad anchor");
        assertFalse(twl.isSafeAttribute("a", a, href));
    }

    @Test
    public void removeProtocolsUpdatesInternalMapsAndAllowsAnyWhenEmpty() {
        Whitelist wl = new Whitelist()
                .addTags("a")
                .addAttributes("a", "href")
                .addProtocols("a", "href", "http", "https");
        // remove both protocols
        wl.removeProtocols("a", "href", "http", "https");
        TestableWhitelist twl = (TestableWhitelist) wl;

        Element a = new Element(Tag.valueOf("a"), "", new Attributes().put("href", "ftp://any.com"));
        Attribute href = a.attributes().getIgnoreCase("href");
        // since no protocols are configured, any protocol should be accepted
        assertTrue(twl.isSafeAttribute("a", a, href));
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeProtocolsThrowsWhenTagNotConfigured() {
        new Whitelist().removeProtocols("a", "href", "http");
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeProtocolsThrowsWhenAttributeNotConfigured() {
        Whitelist wl = new Whitelist()
                .addTags("a")
                .addAttributes("a", "href")
                .addProtocols("a", "href", "http");
        wl.removeProtocols("a", "src", "http");
    }

    /* --------------------------------------------------------------------- *
     *  Protected helpers exercised via subclass
     * --------------------------------------------------------------------- */

    @Test
    public void isSafeTagWorksForAddedAndMissingTags() {
        Whitelist wl = new Whitelist().addTags("p", "div");
        TestableWhitelist twl = (TestableWhitelist) wl;
        assertTrue(twl.isSafeTag("p"));
        assertFalse(twl.isSafeTag("span"));
    }

    @Test
    public void isSafeAttributeHandlesEnforcedAttributes() {
        Whitelist wl = new Whitelist()
                .addTags("a")
                .addEnforcedAttribute("a", "rel", "nofollow");
        TestableWhitelist twl = (TestableWhitelist) wl;

        Element a = new Element(Tag.valueOf("a"), "", new Attributes().put("rel", "nofollow"));
        Attribute rel = a.attributes().getIgnoreCase("rel");
        assertTrue(twl.isSafeAttribute("a", a, rel));

        // wrong value should be rejected
        rel.setValue("author");
        assertFalse(twl.isSafeAttribute("a", a, rel));
    }

    @Test
    public void isSafeAttributeFallsBackToAllWildcard() {
        Whitelist wl = new Whitelist()
                .addAttributes(":all", "class")
                .addTags("p", "div");
        TestableWhitelist twl = (TestableWhitelist) wl;

        Element p = new Element(Tag.valueOf("p"), "", new Attributes().put("class", "myclass"));
        Attribute cls = p.attributes().getIgnoreCase("class");
        assertTrue(twl.isSafeAttribute("p", p, cls));

        // attribute not in :all should be unsafe
        Attribute id = new Attribute("id", "x");
        assertFalse(twl.isSafeAttribute("p", p, id));
    }

    /* --------------------------------------------------------------------- *
     *  Miscellaneous internal helpers (via reflection if needed)
     * --------------------------------------------------------------------- */

    @Test
    public void isValidAnchorAcceptsSimpleAnchors() throws Exception {
        // Use reflection to call the private method for completeness
        java.lang.reflect.Method m = Whitelist.class.getDeclaredMethod("isValidAnchor", String.class);
        m.setAccessible(true);
        Whitelist wl = new Whitelist();
        assertTrue((Boolean) m.invoke(wl, "#section"));
        assertFalse((Boolean) m.invoke(wl, "#bad anchor"));
        assertFalse((Boolean) m.invoke(wl, "http://example.com"));
    }
}
