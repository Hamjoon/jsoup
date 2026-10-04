package org.jsoup.safety;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.ParseErrorList;
import org.jsoup.parser.Parser;
import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link Cleaner}.
 *
 * The tests are organised in the following sections:
 *
 * 1.  Public API extraction – the public members of {@code Cleaner} are:
 * <pre>
 *     public Cleaner(Whitelist whitelist)
 *     public Document clean(Document dirtyDocument)
 *     public boolean isValid(Document dirtyDocument)
 *     public boolean isValidBodyHtml(String bodyHtml)
 * </pre>
 *
 * 2.  Basic functional tests – each method is exercised with a typical
 *     “happy‑path” input to verify that it behaves as documented.
 *
 * 3.  Edge‑case & exception tests – null arguments, empty/frameset documents,
 *     unsafe tags/attributes, head content, malformed HTML and parse‑error
 *     handling are covered.
 *
 * All tests run with JUnit 4.
 */
public class CleanerTest {

    private Whitelist basicWhitelist;
    private Whitelist noneWhitelist;

    @Before
    public void setUp() {
        // a whitelist that allows a limited set of safe tags/attributes
        basicWhitelist = Whitelist.basic();          // allows <a href>, <p>, <b>, <i>, …
        // a whitelist that allows nothing – useful for testing discarding behaviour
        noneWhitelist = Whitelist.none();
    }

    /* --------------------------------------------------------------------- *
     *  Constructor tests
     * --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void constructor_NullWhitelist_Throws() {
        new Cleaner(null);
    }

    /* --------------------------------------------------------------------- *
     *  clean(Document) – basic functionality
     * --------------------------------------------------------------------- */

    @Test
    public void clean_HappyPath_CopiesOnlySafeElements() {
        Cleaner cleaner = new Cleaner(basicWhitelist);

        String html = "<head><title>Ignored</title></head>"
                    + "<body>"
                    + "  <p>Hello <a href='http://example.com' onclick='alert(1)'>link</a>"
                    + "  <script>alert('bad')</script>"
                    + "  </p>"
                    + "</body>";

        Document dirty = Jsoup.parse(html, "http://baseuri/");
        Document cleaned = cleaner.clean(dirty);

        // head must be empty in the cleaned document
        assertTrue("cleaned head should be empty", cleaned.head().childNodeSize() == 0);

        // body should contain only the safe <p> and <a> (script removed)
        Element p = cleaned.body().selectFirst("p");
        assertNotNull("cleaned <p> should be present", p);
        assertEquals("Hello ", p.ownText()); // own text before the link

        Element a = p.selectFirst("a");
        assertNotNull("cleaned <a> should be present", a);
        assertEquals("http://example.com", a.attr("href"));
        assertFalse("unsafe attribute 'onclick' must be removed", a.hasAttr("onclick"));
        assertFalse("script tag must be removed", cleaned.body().selectFirst("script") != null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void clean_NullDocument_Throws() {
        Cleaner cleaner = new Cleaner(basicWhitelist);
        cleaner.clean(null);
    }

    @Test
    public void clean_FramesetDocument_NoBodyHandledGracefully() {
        // a frameset document does not have a <body> element
        String framesetHtml = "<frameset><frame src='a.html'></frame></frameset>";
        Document dirty = Jsoup.parse(framesetHtml);
        // dirty.body() will be null
        assertNull("frameset document should have null body", dirty.body());

        Cleaner cleaner = new Cleaner(basicWhitelist);
        Document cleaned = cleaner.clean(dirty);

        // The cleaned document is a shell – it always has a body element,
        // but it should be empty because there was nothing to copy.
        assertNotNull("cleaned document must have a body", cleaned.body());
        assertTrue("cleaned body should be empty", cleaned.body().childNodeSize() == 0);
    }

    /* --------------------------------------------------------------------- *
     *  isValid(Document) – basic functionality
     * --------------------------------------------------------------------- */

    @Test
    public void isValid_AllSafe_ReturnsTrue() {
        Cleaner cleaner = new Cleaner(basicWhitelist);
        String html = "<body><p>Safe <b>content</b></p></body>";
        Document dirty = Jsoup.parse(html);
        assertTrue(cleaner.isValid(dirty));
    }

    @Test
    public void isValid_ContainsUnsafeTagOrAttrOrHead_ReturnsFalse() {
        Cleaner cleaner = new Cleaner(basicWhitelist);
        String html = "<head><title>Oops</title></head>"
                    + "<body>"
                    + "  <p>Bad <script>alert(1)</script></p>"
                    + "</body>";
        Document dirty = Jsoup.parse(html);
        assertFalse("document with head content and unsafe tag must be invalid", cleaner.isValid(dirty));
    }

    @Test(expected = IllegalArgumentException.class)
    public void isValid_NullDocument_Throws() {
        Cleaner cleaner = new Cleaner(basicWhitelist);
        cleaner.isValid(null);
    }

    @Test(expected = NullPointerException.class)
    public void isValid_FramesetDocument_ThrowsNPE() {
        // The implementation does not guard against a null body, so a NPE is expected.
        String framesetHtml = "<frameset><frame src='a.html'></frame></frameset>";
        Document dirty = Jsoup.parse(framesetHtml);
        Cleaner cleaner = new Cleaner(basicWhitelist);
        cleaner.isValid(dirty);
    }

    /* --------------------------------------------------------------------- *
     *  isValidBodyHtml(String) – basic functionality
     * --------------------------------------------------------------------- */

    @Test
    public void isValidBodyHtml_SafeHtml_ReturnsTrue() {
        Cleaner cleaner = new Cleaner(basicWhitelist);
        String safeHtml = "<p>Just <i>text</i></p>";
        assertTrue("Safe body HTML should be valid", cleaner.isValidBodyHtml(safeHtml));
    }

    @Test
    public void isValidBodyHtml_UnsafeTag_ReturnsFalse() {
        Cleaner cleaner = new Cleaner(basicWhitelist);
        String unsafeHtml = "<script>alert('xss')</script>";
        assertFalse("Body HTML containing unsafe tag must be invalid", cleaner.isValidBodyHtml(unsafeHtml));
    }

    @Test
    public void isValidBodyHtml_MalformedHtml_ReturnsFalseDueToParseError() {
        Cleaner cleaner = new Cleaner(basicWhitelist);
        // Missing closing </b> tag – Jsoup will generate a parse error.
        String malformed = "<p><b>Bold text</p>";
        assertFalse("Malformed body HTML should be invalid (parse error)", cleaner.isValidBodyHtml(malformed));
    }

    @Test
    public void isValidBodyHtml_EmptyString_ReturnsTrue() {
        Cleaner cleaner = new Cleaner(basicWhitelist);
        assertTrue("Empty body HTML should be considered valid", cleaner.isValidBodyHtml(""));
    }

    /* --------------------------------------------------------------------- *
     *  Edge‑case tests that indirectly exercise the private helper methods
     * --------------------------------------------------------------------- */

    @Test
    public void clean_WithNoneWhitelist_RemovesAllElementsExceptText() {
        Cleaner cleaner = new Cleaner(noneWhitelist);
        String html = "<body><p>Paragraph <a href='http://example.com'>link</a></p></body>";
        Document dirty = Jsoup.parse(html);
        Document cleaned = cleaner.clean(dirty);

        // Only the raw text nodes should survive; no element tags at all.
        assertEquals("Paragraph link", cleaned.body().text());
        assertTrue("No element tags should be present", cleaned.body().select("*").isEmpty());
    }

    @Test
    public void isValid_WithNoneWhitelist_AlwaysFalseWhenBodyHasElements() {
        Cleaner cleaner = new Cleaner(noneWhitelist);
        String html = "<body><p>Text</p></body>";
        Document dirty = Jsoup.parse(html);
        assertFalse("With a none‑whitelist any element makes the document invalid", cleaner.isValid(dirty));
    }

    @Test
    public void isValidBodyHtml_WithNoneWhitelist_OnlyPlainTextIsValid() {
        Cleaner cleaner = new Cleaner(noneWhitelist);
        assertTrue("Plain text is valid", cleaner.isValidBodyHtml("just text"));
        assertFalse("Any tag makes it invalid", cleaner.isValidBodyHtml("<b>bold</b>"));
    }
}
