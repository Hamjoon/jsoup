###Test START##
package org.jsoup.safety;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class WhitelistTest {

    /** Helper subclass exposing protected methods for testing */
    private static class TestableWhitelist extends Whitelist {
        public boolean isSafeTagPublic(String tag) {
            return super.isSafeTag(tag);
        }
        public boolean isSafeAttributePublic(String tag, Element el, Attribute attr) {
            return super.isSafeAttribute(tag, el, attr);
        }
        public Attributes getEnforcedAttributesPublic(String tag) {
            return super.getEnforcedAttributes(tag);
        }
    }

    @Test
    public void testFactoryMethods() {
        Whitelist none = Whitelist.none();
        assertNotNull(none);
        // none should have no tags
        TestableWhitelist twNone = new TestableWhitelist();
        assertFalse(twNone.isSafeTagPublic("p"));

        Whitelist simple = Whitelist.simpleText();
        assertTrue(simple instanceof Whitelist);
        TestableWhitelist twSimple = new TestableWhitelist();
        twSimple.addTags("b", "em", "i", "strong", "u");
        assertTrue(twSimple.isSafeTagPublic("b"));
        assertTrue(twSimple.isSafeTagPublic("em"));
        assertFalse(twSimple.isSafeTagPublic("a"));

        Whitelist basic = Whitelist.basic();
        assertTrue(basic instanceof Whitelist);
        TestableWhitelist twBasic = new TestableWhitelist();
        twBasic.addTags("a","b","blockquote","br","cite","code","dd","dl","dt","em",
                "i","li","ol","p","pre","q","small","span","strike","strong","sub",
                "sup","u","ul");
        assertTrue(twBasic.isSafeTagPublic("a"));
        assertTrue(twBasic.isSafeTagPublic("blockquote"));
        assertFalse(twBasic.isSafeTagPublic("img"));

        Whitelist basicImg = Whitelist.basicWithImages();
        assertTrue(basicImg instanceof Whitelist);
        TestableWhitelist twBasicImg = new TestableWhitelist();
        twBasicImg.addTags("img");
        assertTrue(twBasicImg.isSafeTagPublic("img"));

        Whitelist relaxed = Whitelist.relaxed();
        assertTrue(relaxed instanceof Whitelist);
        TestableWhitelist twRelaxed = new TestableWhitelist();
        twRelaxed.addTags("table","thead","tbody","tr","td","th");
        assertTrue(twRelaxed.isSafeTagPublic("table"));
        assertTrue(twRelaxed.isSafeTagPublic("thead"));
    }

    @Test
    public void testAddAndRemoveTags() {
        TestableWhitelist wl = new TestableWhitelist();
        wl.addTags("p", "div", "span");
        assertTrue(wl.isSafeTagPublic("p"));
        assertTrue(wl.isSafeTagPublic("div"));
        assertTrue(wl.isSafeTagPublic("span"));

        wl.removeTags("div");
        assertTrue(wl.isSafeTagPublic("p"));
        assertFalse(wl.isSafeTagPublic("div"));
        assertTrue(wl.isSafeTagPublic("span"));
    }

    @Test
    public void testAddAndRemoveAttributes() {
        TestableWhitelist wl = new TestableWhitelist();
        wl.addTags("a");
        wl.addAttributes("a", "href", "title");
        Element el = new Element(Tag.valueOf("a"), "http://example.com");
        Attribute href = new Attribute("href", "http://example.com");
        Attribute title = new Attribute("title", "mytitle");
        Attribute style = new Attribute("style", "color:red");

        assertTrue(wl.isSafeAttributePublic("a", el, href));
        assertTrue(wl.isSafeAttributePublic("a", el, title));
        assertFalse(wl.isSafeAttributePublic("a", el, style));

        // remove one attribute
        wl.removeAttributes("a", "title");
        assertTrue(wl.isSafeAttributePublic("a", el, href));
        assertFalse(wl.isSafeAttributePublic("a", el, title));

        // remove all attributes for a tag – map entry should be cleared
        wl.removeAttributes("a", "href");
        assertFalse(wl.isSafeAttributePublic("a", el, href));
    }

    @Test
    public void testRemoveAttributesAllTag() {
        TestableWhitelist wl = new TestableWhitelist();
        wl.addTags("p", "div");
        wl.addAttributes("p", "class", "id");
        wl.addAttributes("div", "class", "style");
        // remove 'class' from all tags using ":all"
        wl.removeAttributes(":all", "class");

        Element p = new Element(Tag.valueOf("p"), "");
        Element div = new Element(Tag.valueOf("div"), "");

        Attribute classAttr = new Attribute("class", "myclass");
        Attribute idAttr = new Attribute("id", "myid");
        Attribute styleAttr = new Attribute("style", "color:red");

        assertFalse(wl.isSafeAttributePublic("p", p, classAttr));
        assertTrue(wl.isSafeAttributePublic("p", p, idAttr));

        assertFalse(wl.isSafeAttributePublic("div", div, classAttr));
        assertTrue(wl.isSafeAttributePublic("div", div, styleAttr));
    }

    @Test
    public void testEnforcedAttributes() {
        TestableWhitelist wl = new TestableWhitelist();
        wl.addTags("a");
        wl.addEnforcedAttribute("a", "rel", "nofollow");
        Element a = new Element(Tag.valueOf("a"), "");
        Attribute relGood = new Attribute("rel", "nofollow");
        Attribute relBad = new Attribute("rel", "author");
        Attribute href = new Attribute("href", "http://example.com");

        // enforced attribute must match exactly
        assertTrue(wl.isSafeAttributePublic("a", a, relGood));
        assertFalse(wl.isSafeAttributePublic("a", a, relBad));

        // other allowed attributes (none added) should be rejected
        assertFalse(wl.isSafeAttributePublic("a", a, href));

        // after removal, attribute should no longer be enforced
        wl.removeEnforcedAttribute("a", "rel");
        assertFalse(wl.isSafeAttributePublic("a", a, relGood));
    }

    @Test
    public void testProtocols() {
        TestableWhitelist wl = new TestableWhitelist();
        wl.addTags("a");
        wl.addAttributes("a", "href");
        wl.addProtocols("a", "href", "http", "https");

        Element a = new Element(Tag.valueOf("a"), "http://base.com");
        Attribute http = new Attribute("href", "http://example.com");
        Attribute https = new Attribute("href", "https://example.com");
        Attribute ftp = new Attribute("href", "ftp://example.com");

        assertTrue(wl.isSafeAttributePublic("a", a, http));
        assertTrue(wl.isSafeAttributePublic("a", a, https));
        assertFalse(wl.isSafeAttributePublic("a", a, ftp));

        // remove http protocol, leaving only https
        wl.removeProtocols("a", "href", "http");
        assertFalse(wl.isSafeAttributePublic("a", a, http));
        assertTrue(wl.isSafeAttributePublic("a", a, https));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveNonexistentProtocolThrows() {
        TestableWhitelist wl = new TestableWhitelist();
        wl.addTags("a");
        wl.addAttributes("a", "href");
        wl.addProtocols("a", "href", "http");
        // Attempt to remove a protocol that was never added – should trigger validation failure
        wl.removeProtocols("a", "href", "ftp");
    }

    @Test
    public void testPreserveRelativeLinks() {
        TestableWhitelist wl = new TestableWhitelist();
        wl.addTags("a");
        wl.addAttributes("a", "href");
        wl.addProtocols("a", "href", "http", "https");
        wl.preserveRelativeLinks(false);

        Element a = new Element(Tag.valueOf("a"), "http://example.com");
        Attribute rel = new Attribute("href", "/path/page.html"); // relative URL

        // With preserveRelativeLinks false, attribute value should be converted to absolute
        assertTrue(wl.isSafeAttributePublic("a", a, rel));
        assertEquals("http://example.com/path/page.html", rel.getValue());

        // With preserveRelativeLinks true, original relative value is retained
        wl.preserveRelativeLinks(true);
        Attribute rel2 = new Attribute("href", "/other/page.html");
        assertTrue(wl.isSafeAttributePublic("a", a, rel2));
        assertEquals("/other/page.html", rel2.getValue());
    }

    @Test
    public void testAnchorProtocol() {
        TestableWhitelist wl = new TestableWhitelist();
        wl.addTags("a");
        wl.addAttributes("a", "href");
        wl.addProtocols("a", "href", "#");

        Element a = new Element(Tag.valueOf("a"), "http://example.com");
        Attribute goodAnchor = new Attribute("href", "#section1");
        Attribute badAnchor = new Attribute("href", "#bad anchor");

        assertTrue(wl.isSafeAttributePublic("a", a, goodAnchor));
        assertFalse(wl.isSafeAttributePublic("a", a, badAnchor));
    }

    @Test
    public void testGetEnforcedAttributes() {
        TestableWhitelist wl = new TestableWhitelist();
        wl.addTags("img");
        wl.addEnforcedAttribute("img", "alt", "default");
        wl.addEnforcedAttribute("img", "loading", "lazy");

        Attributes attrs = wl.getEnforcedAttributesPublic("img");
        assertEquals("default", attrs.get("alt"));
        assertEquals("lazy", attrs.get("loading"));
        assertEquals(2, attrs.size());
    }

    @Test
    public void testIsSafeTagAfterAddRemove() {
        TestableWhitelist wl = new TestableWhitelist();
        wl.addTags("section", "article");
        assertTrue(wl.isSafeTagPublic("section"));
        assertTrue(wl.isSafeTagPublic("article"));
        wl.removeTags("section");
        assertFalse(wl.isSafeTagPublic("section"));
        assertTrue(wl.isSafeTagPublic("article"));
    }
}
###Test END##