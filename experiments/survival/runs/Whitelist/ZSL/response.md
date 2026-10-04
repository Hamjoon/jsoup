###Test START##
```java
package org.jsoup.safety;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit4 test suite for {@link Whitelist}.
 * <p>
 * The tests cover all public builder methods, the internal
 * configuration maps and the protected safety‑checking methods.
 * A small subclass {@code TestWhitelist} is used to expose the
 * protected {@code isSafeTag} and {@code isSafeAttribute} methods.
 * </p>
 */
public class WhitelistTest {

    /** Simple subclass that exposes the protected methods for testing. */
    private static class TestWhitelist extends Whitelist {
        public boolean safeTag(String tag) {
            return super.isSafeTag(tag);
        }

        public boolean safeAttribute(String tagName, Element el, Attribute attr) {
            return super.isSafeAttribute(tagName, el, attr);
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Static factory methods                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNoneCreatesEmptyWhitelist() {
        Whitelist wl = Whitelist.none();
        assertFalse("none() should not allow any tag", wl.isSafeTag("p"));
    }

    @Test
    public void testSimpleTextContainsOnlyFormattingTags() {
        Whitelist wl = Whitelist.simpleText();
        assertTrue(wl.isSafeTag("b"));
        assertTrue(wl.isSafeTag("em"));
        assertTrue(wl.isSafeTag("i"));
        assertTrue(wl.isSafeTag("strong"));
        assertTrue(wl.isSafeTag("u"));
        assertFalse(wl.isSafeTag("a"));
    }

    @Test
    public void testBasicHasExpectedTagsAndAttributes() {
        Whitelist wl = Whitelist.basic();
        // tags
        assertTrue(wl.isSafeTag("a"));
        assertTrue(wl.isSafeTag("blockquote"));
        assertTrue(wl.isSafeTag("q"));
        // attribute presence
        Element a = new Element(Tag.valueOf("a"), "http://example.com");
        a.attr("href", "http://jsoup.org");
        Attribute href = a.attributes().getIgnoreCase("href");
        assertTrue(wl.isSafeAttribute("a", a, href));
        // enforced attribute
        Attribute rel = new Attribute("rel", "nofollow");
        assertTrue(wl.isSafeAttribute("a", a, rel));
        // wrong enforced value should be rejected
        Attribute badRel = new Attribute("rel", "follow");
        assertFalse(wl.isSafeAttribute("a", a, badRel));
    }

    @Test
    public void testBasicWithImagesAddsImgTagAndAttributes() {
        Whitelist wl = Whitelist.basicWithImages();
        assertTrue(wl.isSafeTag("img"));
        Element img = new Element(Tag.valueOf("img"), "http://example.com");
        img.attr("src", "https://example.com/pic.png");
        Attribute src = img.attributes().getIgnoreCase("src");
        assertTrue(wl.isSafeAttribute("img", img, src));
    }

    @Test
    public void testRelaxedContainsManyTagsAndAttributes() {
        Whitelist wl = Whitelist.relaxed();
        assertTrue(wl.isSafeTag("table"));
        assertTrue(wl.isSafeTag("thead"));
        // check an attribute that is allowed only on a specific tag
        Element td = new Element(Tag.valueOf("td"), "http://example.com");
        td.attr("colspan", "2");
        Attribute colspan = td.attributes().getIgnoreCase("colspan");
        assertTrue(wl.isSafeAttribute("td", td, colspan));
    }

    /* --------------------------------------------------------------------- */
    /*  Builder methods – add / remove tags                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAndRemoveTags() {
        TestWhitelist wl = new TestWhitelist();
        wl.addTags("p", "div", "span");
        assertTrue(wl.safeTag("p"));
        assertTrue(wl.safeTag("div"));
        assertTrue(wl.safeTag("span"));
        // removing a tag should also clean up its sub‑maps
        wl.addAttributes("p", "class");
        wl.removeTags("p");
        assertFalse(wl.safeTag("p"));
        // after removal there should be no attribute entry for "p"
        Element p = new Element(Tag.valueOf("p"), "http://example.com");
        p.attr("class", "x");
        Attribute attr = p.attributes().getIgnoreCase("class");
        assertFalse(wl.safeAttribute("p", p, attr));
    }

    /* --------------------------------------------------------------------- */
    /*  Builder methods – add / remove attributes                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAttributesAddsTagIfMissing() {
        TestWhitelist wl = new TestWhitelist();
        wl.addAttributes("a", "href", "title");
        assertTrue(wl.safeTag("a"));
        Element a = new Element(Tag.valueOf("a"), "http://example.com");
        a.attr("href", "http://jsoup.org");
        Attribute href = a.attributes().getIgnoreCase("href");
        assertTrue(wl.safeAttribute("a", a, href));
    }

    @Test
    public void testRemoveAttributesSpecificTag() {
        TestWhitelist wl = new TestWhitelist();
        wl.addAttributes("a", "href", "title", "id");
        wl.removeAttributes("a", "title", "id");
        Element a = new Element(Tag.valueOf("a"), "http://example.com");
        a.attr("title", "t");
        Attribute title = a.attributes().getIgnoreCase("title");
        assertFalse(wl.safeAttribute("a", a, title));
        a.attr("href", "http://jsoup.org");
        Attribute href = a.attributes().getIgnoreCase("href");
        assertTrue(wl.safeAttribute("a", a, href));
    }

    @Test
    public void testRemoveAttributesAllTagWildcard() {
        TestWhitelist wl = new TestWhitelist();
        wl.addAttributes("a", "href", "class");
        wl.addAttributes("p", "class");
        wl.removeAttributes(":all", "class");
        // class should be rejected for both tags
        Element a = new Element(Tag.valueOf("a"), "http://example.com");
        a.attr("class", "c");
        Attribute clsA = a.attributes().getIgnoreCase("class");
        assertFalse(wl.safeAttribute("a", a, clsA));

        Element p = new Element(Tag.valueOf("p"), "http://example.com");
        p.attr("class", "c");
        Attribute clsP = p.attributes().getIgnoreCase("class");
        assertFalse(wl.safeAttribute("p", p, clsP));
    }

    /* --------------------------------------------------------------------- */
    /*  Builder methods – enforced attributes                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAndRemoveEnforcedAttributes() {
        TestWhitelist wl = new TestWhitelist();
        wl.addEnforcedAttribute("a", "rel", "nofollow");
        // enforced attribute should be accepted only with exact value
        Element a = new Element(Tag.valueOf("a"), "http://example.com");
        Attribute ok = new Attribute("rel", "nofollow");
        assertTrue(wl.safeAttribute("a", a, ok));

        Attribute bad = new Attribute("rel", "sponsored");
        assertFalse(wl.safeAttribute("a", a, bad));

        // removing the enforced attribute should make any value acceptable (if the attribute is otherwise allowed)
        wl.removeEnforcedAttribute("a", "rel");
        assertTrue(wl.safeAttribute("a", a, bad));
    }

    @Test
    public void testGetEnforcedAttributes() {
        TestWhitelist wl = new TestWhitelist();
        wl.addEnforcedAttribute("img", "alt", "logo");
        Attributes attrs = wl.getEnforcedAttributes("img");
        assertEquals("logo", attrs.get("alt"));
        // non‑existent tag returns empty attributes
        assertTrue(wl.getEnforcedAttributes("p").isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  Builder methods – protocol handling                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAndValidateProtocols() {
        TestWhitelist wl = new TestWhitelist();
        wl.addTags("a").addAttributes("a", "href").addProtocols("a", "href", "http", "https");
        Element a = new Element(Tag.valueOf("a"), "http://example.com");
        a.attr("href", "https://jsoup.org");
        Attribute href = a.attributes().getIgnoreCase("href");
        assertTrue(wl.safeAttribute("a", a, href));

        // protocol not allowed
        a.attr("href", "ftp://jsoup.org");
        href = a.attributes().getIgnoreCase("href");
        assertFalse(wl.safeAttribute("a", a, href));
    }

    @Test
    public void testRemoveProtocolsFallingBackToAnyProtocol() {
        TestWhitelist wl = new TestWhitelist();
        wl.addTags("a").addAttributes("a", "href")
                .addProtocols("a", "href", "http", "https")
                .removeProtocols("a", "href", "http", "https");
        Element a = new Element(Tag.valueOf("a"), "http://example.com");
        a.attr("href", "ftp://jsoup.org");
        Attribute href = a.attributes().getIgnoreCase("href");
        // after removal, any protocol should be accepted
        assertTrue(wl.safeAttribute("a", a, href));
    }

    @Test
    public void testProtocolAnchorAllowedViaHash() {
        TestWhitelist wl = new TestWhitelist();
        wl.addTags("a").addAttributes("a", "href")
                .addProtocols("a", "href", "#");
        Element a = new Element(Tag.valueOf("a"), "http://example.com");
        a.attr("href", "#section1");
        Attribute href = a.attributes().getIgnoreCase("href");
        assertTrue(wl.safeAttribute("a", a, href));

        // invalid anchor (contains whitespace) should be rejected
        a.attr("href", "#bad anchor");
        href = a.attributes().getIgnoreCase("href");
        assertFalse(wl.safeAttribute("a", a, href));
    }

    /* --------------------------------------------------------------------- */
    /*  preserveRelativeLinks flag                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPreserveRelativeLinksFlag() {
        TestWhitelist wl = new TestWhitelist();
        wl.addTags("a").addAttributes("a", "href")
                .addProtocols("a", "href", "http", "https")
                .preserveRelativeLinks(false); // default behaviour – convert to absolute

        // element with base URI, relative link
        Element a = new Element(Tag.valueOf("a"), "http://example.com/base/");
        a.attr("href", "page.html");
        Attribute href = a.attributes().getIgnoreCase("href");
        assertTrue(wl.safeAttribute("a", a, href));
        // after validation the attribute value should have been rewritten to absolute
        assertEquals("http://example.com/base/page.html", href.getValue());

        // now preserve relative links
        wl.preserveRelativeLinks(true);
        Element b = new Element(Tag.valueOf("b"), "http://example.com/base/");
        b.attr("href", "other.html");
        Attribute hrefB = b.attributes().getIgnoreCase("href");
        assertTrue(wl.safeAttribute("b", b, hrefB));
        // value must stay unchanged
        assertEquals("other.html", hrefB.getValue());
    }

    /* --------------------------------------------------------------------- */
    /*  Edge cases – validation of internal state after removals              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testRemoveTagCleansAllMaps() {
        TestWhitelist wl = new TestWhitelist();
        wl.addTags("a")
                .addAttributes("a", "href")
                .addEnforcedAttribute("a", "rel", "nofollow")
                .addProtocols("a", "href", "http");
        // ensure maps are populated
        assertTrue(wl.safeTag("a"));
        // remove tag
        wl.removeTags("a");
        assertFalse(wl.safeTag("a"));
        // after removal, attribute, enforced attribute and protocol maps must not contain 'a'
        Element a = new Element(Tag.valueOf("a"), "http://example.com");
        a.attr("href", "http://jsoup.org");
        Attribute href = a.attributes().getIgnoreCase("href");
        assertFalse(wl.safeAttribute("a", a, href));
        Attribute rel = new Attribute("rel", "nofollow");
        assertFalse(wl.safeAttribute("a", a, rel));
    }

    @Test
    public void testRemoveAttributesLeavesEmptyMapCleaned() {
        TestWhitelist wl = new TestWhitelist();
        wl.addAttributes("p", "class");
        // now remove the only attribute
        wl.removeAttributes("p", "class");
        // internal map for 'p' should be gone – safeAttribute should fallback to false
        Element p = new Element(Tag.valueOf("p"), "http://example.com");
        p.attr("class", "x");
        Attribute cls = p.attributes().getIgnoreCase("class");
        assertFalse(wl.safeAttribute("p", p, cls));
    }

    @Test
    public void testRemoveProtocolsClearsEmptyEntries() {
        TestWhitelist wl = new TestWhitelist();
        wl.addTags("a").addAttributes("a", "href")
                .addProtocols("a", "href", "http");
        // remove the only protocol -> map entry should disappear
        wl.removeProtocols("a", "href", "http");
        // after removal, any protocol should be allowed
        Element a = new Element(Tag.valueOf("a"), "http://example.com");
        a.attr("href", "ftp://jsoup.org");
        Attribute href = a.attributes().getIgnoreCase("href");
        assertTrue(wl.safeAttribute("a", a, href));
    }

    /* --------------------------------------------------------------------- */
    /*  Direct tests of protected helper methods via subclass                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsSafeTagDirectly() {
        TestWhitelist wl = new TestWhitelist();
        wl.addTags("h1", "h2");
        assertTrue(wl.safeTag("h1"));
        assertFalse(wl.safeTag("div"));
    }

    @Test
    public void testIsSafeAttributeAllWildcard() {
        TestWhitelist wl = new TestWhitelist();
        wl.addAttributes(":all", "class");
        Element div = new Element(Tag.valueOf("div"), "http://example.com");
        div.attr("class", "myclass");
        Attribute cls = div.attributes().getIgnoreCase("class");
        assertTrue(wl.safeAttribute("div", div, cls));
    }
}
```
###Test END##