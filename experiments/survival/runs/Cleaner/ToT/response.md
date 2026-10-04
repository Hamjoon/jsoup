###Test START##
```java
package org.jsoup.safety;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.ParseErrorList;
import org.jsoup.parser.Parser;
import org.jsoup.select.NodeTraversor;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Cleaner}.
 *
 * The tests cover:
 * <ul>
 *   <li>Constructor argument validation.</li>
 *   <li>Document‑level cleaning (including handling of {@code null} arguments).</li>
 *   <li>Correct copying of safe elements, attributes and enforced attributes.</li>
 *   <li>Discarding of unsafe elements, attributes and other node types (comments, processing instructions, etc.).</li>
 *   <li>Validation logic for both whole {@link Document} objects and raw body HTML strings.</li>
 *   <li>Special handling of {@link DataNode}s when the parent element is safe.</li>
 *   <li>Impact of head content on {@link Cleaner#isValid(Document)}.</li>
 * </ul>
 *
 * All tests are written for JUnit‑4 and use Jsoup’s public API only.
 */
public class CleanerTest {

    /* --------------------------------------------------------------------- *
     *  Helper methods
     * --------------------------------------------------------------------- */

    /** Returns a whitelist that allows only <code>p</code>, <code>a</code> (with href) and <code>script</code>. */
    private static Whitelist simpleWhitelist() {
        Whitelist wl = new Whitelist();
        wl.addTags("p", "a", "script");
        wl.addAttributes("a", "href");
        // Enforce rel=nofollow on all <a> elements – this also tests enforced attributes.
        wl.addEnforcedAttribute("a", "rel", "nofollow");
        return wl;
    }

    /** Builds a dirty document containing a mixture of safe and unsafe nodes. */
    private static Document buildDirtyDocument() {
        String html = ""
                + "<html><head><title>Should be ignored</title></head>"
                + "<body>"
                + "  <p id='p1'>Paragraph <b>bold</b></p>"                // <b> is unsafe
                + "  <a href='http://example.com' onclick='alert(1)'>Link</a>" // onclick is unsafe
                + "  <script type='text/javascript'>var x=1;</script>"   // script is safe (allowed)
                + "  <img src='image.png'/>"                            // img is unsafe
                + "  <!-- a comment -->"
                + "</body></html>";
        return Jsoup.parse(html);
    }

    /* --------------------------------------------------------------------- *
     *  Constructor validation
     * --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNullWhitelist() {
        new Cleaner(null);
    }

    /* --------------------------------------------------------------------- *
     *  clean(...) validation
     * --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void cleanRejectsNullDocument() {
        Cleaner cleaner = new Cleaner(simpleWhitelist());
        cleaner.clean(null);
    }

    @Test
    public void cleanCopiesOnlySafeElementsAndAttributes() {
        Cleaner cleaner = new Cleaner(simpleWhitelist());
        Document dirty = buildDirtyDocument();

        Document cleaned = cleaner.clean(dirty);
        Element body = cleaned.body();

        // Expected structure: <p>Paragraph </p> <a href=... rel=nofollow>Link</a> <script>var x=1;</script>
        assertEquals(3, body.children().size());

        // 1) <p> – should be present, without the unsafe <b> child.
        Element p = body.selectFirst("p");
        assertNotNull(p);
        assertEquals("Paragraph ", p.text()); // <b> stripped, text only

        // 2) <a> – href kept, onclick removed, enforced rel added.
        Element a = body.selectFirst("a");
        assertNotNull(a);
        assertEquals("http://example.com", a.attr("href"));
        assertEquals("nofollow", a.attr("rel"));
        assertFalse("Unsafe attribute 'onclick' should be removed", a.hasAttr("onclick"));

        // 3) <script> – allowed as a safe tag, its DataNode should be copied.
        Element script = body.selectFirst("script");
        assertNotNull(script);
        List<DataNode> data = script.dataNodes();
        assertEquals(1, data.size());
        assertEquals("var x=1;", data.get(0).getWholeData());

        // Ensure that unsafe tags (<b>, <img>) and comment nodes are not present.
        assertNull(body.selectFirst("b"));
        assertNull(body.selectFirst("img"));
        assertTrue(body.childNodes().stream().noneMatch(node -> node instanceof Comment));
    }

    /* --------------------------------------------------------------------- *
     *  isValid(Document) – happy path
     * --------------------------------------------------------------------- */

    @Test
    public void isValidReturnsTrueWhenDocumentIsCompletelySafe() {
        // Build a document that only contains safe tags/attributes.
        String html = "<html><head></head><body>"
                + "<p>Safe paragraph</p>"
                + "<a href='http://example.com'>Safe link</a>"
                + "<script>console.log('ok');</script>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);
        Cleaner cleaner = new Cleaner(simpleWhitelist());

        assertTrue("Document should be considered valid", cleaner.isValid(doc));
    }

    /* --------------------------------------------------------------------- *
     *  isValid(Document) – detection of unsafe tags/attributes and head content
     * --------------------------------------------------------------------- */

    @Test
    public void isValidDetectsUnsafeTagAndAttribute() {
        Document dirty = buildDirtyDocument();
        Cleaner cleaner = new Cleaner(simpleWhitelist());

        assertFalse("Document contains unsafe <b> and onclick attribute", cleaner.isValid(dirty));
    }

    @Test
    public void isValidFailsWhenHeadContainsNodes() {
        String html = "<html><head><meta charset='utf-8'></head><body><p>OK</p></body></html>";
        Document doc = Jsoup.parse(html);
        Cleaner cleaner = new Cleaner(simpleWhitelist());

        assertFalse("Presence of nodes in <head> should make document invalid", cleaner.isValid(doc));
    }

    /* --------------------------------------------------------------------- *
     *  isValidBodyHtml(String) – parsing and validation of raw body fragments
     * --------------------------------------------------------------------- */

    @Test
    public void isValidBodyHtmlAcceptsCleanFragment() {
        String fragment = "<p>Hello <a href='http://jsoup.org'>Jsoup</a></p>";
        Cleaner cleaner = new Cleaner(simpleWhitelist());

        assertTrue("Clean fragment should be valid", cleaner.isValidBodyHtml(fragment));
    }

    @Test
    public void isValidBodyHtmlRejectsFragmentWithUnsafeTag() {
        String fragment = "<p>Hello <script>alert(1)</script></p>"; // script is safe, but we will add an unsafe tag
        Cleaner cleaner = new Cleaner(simpleWhitelist());

        // Add an unsafe tag <iframe> to the fragment – not allowed by the whitelist.
        String unsafeFragment = fragment.replace("<script>", "<iframe>").replace("</script>", "</iframe>");
        assertFalse("Fragment containing <iframe> should be invalid", cleaner.isValidBodyHtml(unsafeFragment));
    }

    @Test
    public void isValidBodyHtmlDetectsParseErrors() {
        // Malformed HTML: a <p> that is never closed and a stray closing tag.
        String malformed = "<p>Unclosed paragraph</div>";
        Cleaner cleaner = new Cleaner(simpleWhitelist());

        assertFalse("Malformed fragment should cause parse errors and be invalid", cleaner.isValidBodyHtml(malformed));
    }

    /* --------------------------------------------------------------------- *
     *  DataNode handling – safe parent vs unsafe parent
     * --------------------------------------------------------------------- */

    @Test
    public void dataNodeCopiedWhenParentIsSafe() {
        // Whitelist that allows <script> (safe) – already done in simpleWhitelist().
        Cleaner cleaner = new Cleaner(simpleWhitelist());

        String html = "<body><script>var a = 1;</script></body>";
        Document dirty = Jsoup.parse(html);
        Document cleaned = cleaner.clean(dirty);

        Element script = cleaned.body().selectFirst("script");
        assertNotNull("Script element should be present", script);
        assertEquals(1, script.dataNodes().size());
        assertEquals("var a = 1;", script.dataNodes().get(0).getWholeData());
    }

    @Test
    public void dataNodeNotCopiedWhenParentIsUnsafe() {
        // Whitelist that does NOT allow <style>.
        Whitelist wl = new Whitelist();
        wl.addTags("p"); // only <p> is safe
        Cleaner cleaner = new Cleaner(wl);

        String html = "<body><style>body {color:red;}</style></body>";
        Document dirty = Jsoup.parse(html);
        Document cleaned = cleaner.clean(dirty);

        // <style> should be discarded entirely, therefore no DataNode.
        assertNull("Style element should not be present", cleaned.body().selectFirst("style"));
        // The body should be empty.
        assertTrue("Cleaned body should have no child nodes", cleaned.body().childNodeSize() == 0);
    }

    /* --------------------------------------------------------------------- *
     *  Edge cases – frameset document (no body) and empty documents
     * --------------------------------------------------------------------- */

    @Test
    public void cleanHandlesDocumentWithoutBodyGracefully() {
        // Simulate a frameset document – Jsoup parses it with no <body>.
        String html = "<html><frameset><frame src='a.html'></frame></frameset></html>";
        Document framesetDoc = Jsoup.parse(html);
        assertNull("Frameset doc should have no body", framesetDoc.body());

        Cleaner cleaner = new Cleaner(simpleWhitelist());
        Document cleaned = cleaner.clean(framesetDoc);

        // The cleaned document must still have a body (created by createShell) but be empty.
        assertNotNull("Cleaned doc must have a body", cleaned.body());
        assertEquals(0, cleaned.body().childNodeSize());
    }

    @Test
    public void isValidReturnsTrueForEmptyDocument() {
        Document empty = Document.createShell("");
        Cleaner cleaner = new Cleaner(simpleWhitelist());
        assertTrue("Empty document (no body nodes, empty head) should be valid", cleaner.isValid(empty));
    }
}
```
###Test END##