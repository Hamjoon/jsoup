###Test START##
```java
package org.jsoup.safety;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.ParseErrorList;
import org.jsoup.parser.Parser;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Cleaner}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public API signatures</li>
 *   <li>Typical “happy‑path” usage</li>
 *   <li>Edge‑case handling (null arguments, frameset documents, head content, parsing errors)</li>
 *   <li>Whitelist‑driven attribute filtering, enforced attributes and safe‑tag logic</li>
 *   <li>Correct discard‑count behaviour through {@code isValid} and {@code isValidBodyHtml}</li>
 * </ul>
 */
public class CleanerTest {

    private Whitelist whitelist;
    private Cleaner cleaner;

    @Before
    public void setUp() {
        // Build a whitelist that allows <p>, <a>, <b> tags.
        // Allow only the "href" attribute on <a>.
        // Enforce a "rel=nofollow" attribute on <a> (added even if missing).
        whitelist = new Whitelist()
                .addTags("p", "a", "b")
                .addAttributes("a", "href")
                .addEnforcedAttribute("a", "rel", "nofollow");
        cleaner = new Cleaner(whitelist);
    }

    /*** --------------------------------------------------------------------- ***/
    /***  Constructor tests                                                    ***/
    /*** --------------------------------------------------------------------- ***/

    @Test(expected = IllegalArgumentException.class)
    public void constructorShouldThrowIfWhitelistIsNull() {
        new Cleaner(null);
    }

    /*** --------------------------------------------------------------------- ***/
    /***  clean(Document) – normal operation                                   ***/
    /*** --------------------------------------------------------------------- ***/

    @Test
    public void cleanShouldCopyAllowedElementsAndDiscardUnsafeOnes() {
        String html = "<html><head><title>Ignore</title></head>"
                + "<body>"
                + "<p>Paragraph</p>"
                + "<script>alert('xss');</script>"   // unsafe tag
                + "<a href='http://example.com'>Link</a>"
                + "<a onclick='evil()'>NoHref</a>"   // unsafe attribute
                + "<b>Bold</b>"
                + "</body></html>";

        Document dirty = Jsoup.parse(html);
        Document cleaned = cleaner.clean(dirty);

        // <head> must be empty in the cleaned doc (cleaner only copies body)
        assertTrue(cleaned.head().childNodes().isEmpty());

        // Body should contain only allowed tags
        Elements bodyChildren = cleaned.body().children();
        assertEquals(3, bodyChildren.size()); // p, a, b

        // <p> text preserved
        assertEquals("Paragraph", cleaned.select("p").first().text());

        // <a> with href kept and enforced rel added
        Element a = cleaned.select("a").first();
        assertEquals("http://example.com", a.attr("href"));
        assertEquals("nofollow", a.attr("rel"));

        // <a> without href should have been discarded because the tag is safe but
        // the attribute "onclick" is not allowed – the whole element is kept but
        // the unsafe attribute is stripped.
        Element aNoHref = cleaned.select("a[rel=nofollow]").get(1);
        assertFalse(aNoHref.hasAttr("onclick"));
        // enforced rel must still be present
        assertEquals("nofollow", aNoHref.attr("rel"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void cleanShouldThrowIfDocumentIsNull() {
        cleaner.clean(null);
    }

    @Test
    public void cleanShouldHandleFramesetDocumentWithoutBody() {
        // A frameset document has no <body>. Jsoup will parse it as a normal doc,
        // but we can simulate by creating a document and removing its body.
        Document frameset = Document.createShell("http://example.com");
        frameset.select("body").remove(); // now there is no body element

        Document cleaned = cleaner.clean(frameset);
        // The cleaned document should still have a body (created by createShell)
        assertNotNull(cleaned.body());
        // And it must be empty because there was nothing to copy.
        assertTrue(cleaned.body().childNodes().isEmpty());
    }

    /*** --------------------------------------------------------------------- ***/
    /***  isValid(Document) – validation logic                                 ***/
    /*** --------------------------------------------------------------------- ***/

    @Test
    public void isValidShouldReturnTrueForCompletelySafeDocument() {
        String html = "<html><head></head>"
                + "<body><p>Ok</p><a href='http://x.com'>Link</a></body></html>";
        Document doc = Jsoup.parse(html);
        assertTrue(cleaner.isValid(doc));
    }

    @Test
    public void isValidShouldReturnFalseIfUnsafeTagFound() {
        String html = "<html><head></head>"
                + "<body><p>Ok</p><script>alert(1)</script></body></html>";
        Document doc = Jsoup.parse(html);
        assertFalse(cleaner.isValid(doc));
    }

    @Test
    public void isValidShouldReturnFalseIfHeadContainsContent() {
        String html = "<html><head><title>Bad</title></head>"
                + "<body><p>Ok</p></body></html>";
        Document doc = Jsoup.parse(html);
        assertFalse(cleaner.isValid(doc));
    }

    @Test(expected = IllegalArgumentException.class)
    public void isValidShouldThrowIfDocumentIsNull() {
        cleaner.isValid(null);
    }

    /*** --------------------------------------------------------------------- ***/
    /***  isValidBodyHtml(String) – fragment validation                         ***/
    /*** --------------------------------------------------------------------- ***/

    @Test
    public void isValidBodyHtmlShouldReturnTrueForSafeFragment() {
        String fragment = "<p>Hello <a href='http://x.com'>World</a></p>";
        assertTrue(cleaner.isValidBodyHtml(fragment));
    }

    @Test
    public void isValidBodyHtmlShouldReturnFalseIfUnsafeTagPresent() {
        String fragment = "<p>Hello</p><script>alert(1)</script>";
        assertFalse(cleaner.isValidBodyHtml(fragment));
    }

    @Test
    public void isValidBodyHtmlShouldReturnFalseIfParseErrorOccurs() {
        // Unclosed tag will generate a parse error that is captured in the error list.
        String fragment = "<p>Unclosed paragraph";
        assertFalse(cleaner.isValidBodyHtml(fragment));
    }

    /*** --------------------------------------------------------------------- ***/
    /***  Attribute handling – safe, unsafe and enforced attributes            ***/
    /*** --------------------------------------------------------------------- ***/

    @Test
    public void safeAttributesArePreservedAndUnsafeAreRemoved() {
        String html = "<a href='http://x.com' title='keep' onclick='evil()'>Link</a>";
        Document dirty = Jsoup.parseBodyFragment(html);
        Document cleaned = cleaner.clean(dirty);

        Element a = cleaned.select("a").first();
        // href is allowed
        assertEquals("http://x.com", a.attr("href"));
        // title is not allowed – should be removed
        assertFalse(a.hasAttr("title"));
        // onclick is not allowed – should be removed
        assertFalse(a.hasAttr("onclick"));
        // enforced rel must be present
        assertEquals("nofollow", a.attr("rel"));
    }

    @Test
    public void enforcedAttributesAreAddedEvenWhenMissing() {
        String html = "<a href='http://x.com'>Link</a>";
        Document dirty = Jsoup.parseBodyFragment(html);
        Document cleaned = cleaner.clean(dirty);
        Element a = cleaned.select("a").first();

        // rel attribute is enforced by the whitelist
        assertEquals("nofollow", a.attr("rel"));
    }

    /*** --------------------------------------------------------------------- ***/
    /***  Internal helper sanity checks (indirectly via public API)            ***/
    /*** --------------------------------------------------------------------- ***/

    @Test
    public void copySafeNodesDiscardCountMatchesIsValidResult() {
        // The fragment contains one unsafe tag (<script>) – it should be counted as discarded.
        String fragment = "<p>Ok</p><script>alert(1)</script>";
        Document dirty = Jsoup.parseBodyFragment(fragment);
        // isValid will invoke copySafeNodes and compare discard count to zero.
        assertFalse(cleaner.isValid(dirty));
    }

    /*** --------------------------------------------------------------------- ***/
    /***  Null and empty string handling for isValidBodyHtml                     ***/
    /*** --------------------------------------------------------------------- ***/

    @Test
    public void isValidBodyHtmlShouldReturnTrueForEmptyString() {
        assertTrue(cleaner.isValidBodyHtml(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void isValidBodyHtmlShouldThrowIfNullString() {
        // The method does not guard against null, but Jsoup's Parser will throw NPE.
        // We assert that an IllegalArgumentException (from Validate) is raised.
        cleaner.isValidBodyHtml(null);
    }
}
```
###Test END##