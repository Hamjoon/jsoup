package org.jsoup.safety;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 tests for {@link Cleaner}.
 *
 * The tests cover:
 * <ul>
 *   <li>cleaning of allowed tags, attributes and enforced attributes</li>
 *   <li>removal of unsafe tags while preserving safe children</li>
 *   <li>preservation of text nodes and data nodes</li>
 *   <li>validation logic (isValid, isValidBodyHtml)</li>
 *   <li>interaction with a custom {@link Whitelist}</li>
 * </ul>
 */
public class CleanerTest {

    private Whitelist whitelist;
    private Cleaner cleaner;

    @Before
    public void setUp() {
        // Build a whitelist that allows only a few specific tags/attributes.
        whitelist = new Whitelist();

        // Allow <p> with a "title" attribute and enforce a "class" attribute.
        whitelist.addTags("p");
        whitelist.addAttributes("p", "title");
        whitelist.addEnforcedAttribute("p", "class", "safe");

        // Allow <span> with no attributes.
        whitelist.addTags("span");

        // Allow <style> (to test DataNode handling) – no attributes.
        whitelist.addTags("style");

        // Allow <a> with href and enforce rel=nofollow.
        whitelist.addTags("a");
        whitelist.addAttributes("a", "href");
        whitelist.addEnforcedAttribute("a", "rel", "nofollow");

        cleaner = new Cleaner(whitelist);
    }

    /**
     * Test that allowed tags and attributes are retained, disallowed ones are removed,
     * and enforced attributes are added.
     */
    @Test
    public void clean_AllowsSafeTagAndAttributes_EnforcesAttributes() {
        String html = "<html><head><title>Should be ignored</title></head>"
                + "<body>"
                + "<p title='ok'>Hello <b>world</b></p>"
                + "</body></html>";

        Document dirty = Jsoup.parse(html);
        Document cleaned = cleaner.clean(dirty);

        // The <b> tag is not whitelisted, so it must be stripped.
        // The enforced class attribute must be added.
        String expected = "<p title=\"ok\" class=\"safe\">Hello world</p>";
        assertEquals(expected, cleaned.body().html());
        // Validation should be false because a tag (<b>) was removed.
        assertFalse(cleaner.isValid(dirty));
    }

    /**
     * Test that an unsafe parent element is removed but its safe child survives.
     */
    @Test
    public void clean_UnsafeParentRemoved_ChildPreserved() {
        String html = "<body><div><span>inner</span></div></body>";
        Document dirty = Jsoup.parse(html);
        Document cleaned = cleaner.clean(dirty);

        // <div> is not allowed, <span> is allowed – it should be lifted to the body.
        String expected = "<span>inner</span>";
        assertEquals(expected, cleaned.body().html());
    }

    /**
     * Test that a DataNode inside an allowed element is preserved.
     */
    @Test
    public void clean_DataNodePreservedWhenParentIsSafe() {
        String html = "<style>body{color:red;}</style>";
        Document dirty = Jsoup.parse(html);
        Document cleaned = cleaner.clean(dirty);

        // <style> is allowed; its raw data must be kept.
        String expected = "<style>body{color:red;}</style>";
        assertEquals(expected, cleaned.body().html());
    }

    /**
     * Test that {@link Cleaner#isValid(Document)} returns true when the document
     * contains only whitelisted content and the head is empty.
     */
    @Test
    public void isValid_ValidDocumentReturnsTrue() {
        String html = "<body><p title='x'>Valid</p></body>";
        Document dirty = Jsoup.parse(html);
        assertTrue(cleaner.isValid(dirty));
    }

    /**
     * Test that {@link Cleaner#isValid(Document)} returns false when the head
     * contains any nodes.
     */
    @Test
    public void isValid_DocumentWithHeadContentReturnsFalse() {
        String html = "<head><meta charset='utf-8'></head><body><p>Hi</p></body>";
        Document dirty = Jsoup.parse(html);
        assertFalse(cleaner.isValid(dirty));
    }

    /**
     * Test {@link Cleaner#isValidBodyHtml(String)} with safe HTML fragment.
     */
    @Test
    public void isValidBodyHtml_SafeFragmentReturnsTrue() {
        String fragment = "<p title='ok'>Fragment</p>";
        assertTrue(cleaner.isValidBodyHtml(fragment));
    }

    /**
     * Test {@link Cleaner#isValidBodyHtml(String)} with an unsafe tag.
     */
    @Test
    public void isValidBodyHtml_UnsafeFragmentReturnsFalse() {
        String fragment = "<script>alert(1)</script>";
        assertFalse(cleaner.isValidBodyHtml(fragment));
    }

    /**
     * Verify that enforced attributes are added even when the source element
     * does not contain them.
     */
    @Test
    public void clean_EnforcedAttributesAreAdded() {
        String html = "<a href='http://example.com'>link</a>";
        Document dirty = Jsoup.parse(html);
        Document cleaned = cleaner.clean(dirty);

        // The whitelist enforces rel="nofollow" on every <a>.
        String expected = "<a href=\"http://example.com\" rel=\"nofollow\">link</a>";
        assertEquals(expected, cleaned.body().html());
    }

    /**
     * Verify that unsafe attributes are discarded while safe ones are retained.
     */
    @Test
    public void clean_UnsafeAttributesAreDiscarded() {
        // <p> allows only "title". The "style" attribute should be stripped.
        String html = "<p title='ok' style='color:red;'>Text</p>";
        Document dirty = Jsoup.parse(html);
        Document cleaned = cleaner.clean(dirty);

        // The "style" attribute must be gone, "title" stays, and class is enforced.
        String expected = "<p title=\"ok\" class=\"safe\">Text</p>";
        assertEquals(expected, cleaned.body().html());
    }

    /**
     * Confirm that text nodes are always preserved, even when they are children
     * of an unsafe element that gets removed.
     */
    @Test
    public void clean_TextNodePreservedWhenParentRemoved() {
        String html = "<div>Orphan <p>Safe</p></div>";
        Document dirty = Jsoup.parse(html);
        Document cleaned = cleaner.clean(dirty);

        // "Orphan " text node should be kept (as it is a direct child of the body after <div> is stripped),
        // and the safe <p> element should also be present.
        String expected = "Orphan <p class=\"safe\">Safe</p>";
        assertEquals(expected, cleaned.body().html());
    }
}
