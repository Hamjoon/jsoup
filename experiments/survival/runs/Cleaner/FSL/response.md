###Test START##
```java
package org.jsoup.safety;

import static org.junit.Assert.*;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.ParseErrorList;
import org.jsoup.parser.Parser;
import org.jsoup.select.NodeTraversor;
import org.junit.Test;

/**
 * Comprehensive unit tests for {@link Cleaner}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>cleaning of allowed and disallowed tags/attributes</li>
 *   <li>validation of whole documents (including head content)</li>
 *   <li>validation of HTML fragments via {@code isValidBodyHtml}</li>
 *   <li>handling of {@link DataNode}s</li>
 *   <li>exception handling for {@code null} arguments</li>
 * </ul>
 * </p>
 */
public class CleanerTest {

    /* --------------------------------------------------------------------- */
    /*  Helper methods                                                     */
    /* --------------------------------------------------------------------- */

    /** Parses the given HTML into a {@link Document} using the default base URI. */
    private Document parse(String html) {
        return Jsoup.parse(html);
    }

    /** Returns the HTML of the body element of the given document (without surrounding &lt;body&gt; tags). */
    private String bodyHtml(Document doc) {
        return doc.body().html();
    }

    /* --------------------------------------------------------------------- */
    /*  Constructor / argument validation                                 */
    /* --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNullWhitelist() {
        new Cleaner(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCleanRejectsNullDocument() {
        Cleaner cleaner = new Cleaner(Whitelist.none());
        cleaner.clean(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsValidRejectsNullDocument() {
        Cleaner cleaner = new Cleaner(Whitelist.none());
        cleaner.isValid(null);
    }

    /* --------------------------------------------------------------------- */
    /*  clean(...) – allowed tags & attributes                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCleanPreservesAllowedElementsAndAttributes() {
        Whitelist wl = Whitelist.basic();               // allows <a href>, <p>, <b>, etc.
        Cleaner cleaner = new Cleaner(wl);

        String html = "<p>Hello <a href=\"http://example.com\" title=\"title\">world</a>!</p>";
        Document dirty = parse(html);
        Document clean = cleaner.clean(dirty);

        // The cleaned body should be identical to the input (basic whitelist permits everything used)
        assertEquals("<p>Hello <a href=\"http://example.com\" title=\"title\">world</a>!</p>", bodyHtml(clean));
    }

    @Test
    public void testCleanRemovesDisallowedTagButKeepsText() {
        Whitelist wl = Whitelist.none();                 // no tags allowed, but text nodes are always kept
        Cleaner cleaner = new Cleaner(wl);

        String html = "<div><p>Some <b>bold</b> text</p></div>";
        Document dirty = parse(html);
        Document clean = cleaner.clean(dirty);

        // No tags should survive; only the raw text nodes remain.
        assertEquals("Some bold text", clean.body().text());
        assertTrue(clean.body().children().isEmpty());
    }

    @Test
    public void testCleanRemovesDisallowedAttributes() {
        Whitelist wl = Whitelist.basic();               // allows href on <a> but not style
        Cleaner cleaner = new Cleaner(wl);

        String html = "<a href=\"http://example.com\" style=\"color:red;\" onclick=\"alert(1)\">link</a>";
        Document dirty = parse(html);
        Document clean = cleaner.clean(dirty);

        Element a = clean.selectFirst("a");
        assertNotNull(a);
        // href should be kept
        assertEquals("http://example.com", a.attr("href"));
        // style and onclick should be removed
        assertFalse(a.hasAttr("style"));
        assertFalse(a.hasAttr("onclick"));
    }

    /* --------------------------------------------------------------------- */
    /*  clean(...) – DataNode handling                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCleanCopiesDataNodeWhenParentTagIsAllowed() {
        // custom whitelist that allows <script> tags but no attributes
        Whitelist wl = new Whitelist();
        wl.addTags("script");
        Cleaner cleaner = new Cleaner(wl);

        String html = "<script>var x = 1;</script>";
        Document dirty = parse(html);
        Document clean = cleaner.clean(dirty);

        Element script = clean.selectFirst("script");
        assertNotNull(script);
        // The script element should contain a single DataNode with the original script text.
        List<DataNode> dataNodes = script.dataNodes();
        assertEquals(1, dataNodes.size());
        assertEquals("var x = 1;", dataNodes.get(0).getWholeData());
    }

    @Test
    public void testCleanDiscardsDataNodeWhenParentTagIsNotAllowed() {
        Whitelist wl = Whitelist.none(); // no tags allowed, thus <script> is disallowed
        Cleaner cleaner = new Cleaner(wl);

        String html = "<script>var x = 1;</script>";
        Document dirty = parse(html);
        Document clean = cleaner.clean(dirty);

        // No script element should be present, and no data node should survive.
        assertTrue(clean.body().children().isEmpty());
        assertEquals("", clean.body().text());
    }

    /* --------------------------------------------------------------------- */
    /*  isValid(Document) – various success / failure scenarios            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsValidReturnsTrueWhenDocumentIsCompletelyAllowed() {
        Whitelist wl = Whitelist.basic();
        Cleaner cleaner = new Cleaner(wl);

        String html = "<p>Paragraph with <a href=\"http://example.com\">link</a></p>";
        Document dirty = parse(html);
        assertTrue(cleaner.isValid(dirty));
    }

    @Test
    public void testIsValidReturnsFalseWhenDisallowedTagExists() {
        Whitelist wl = Whitelist.basic();
        Cleaner cleaner = new Cleaner(wl);

        String html = "<p>Ok</p><script>alert('x');</script>";
        Document dirty = parse(html);
        assertFalse(cleaner.isValid(dirty));
    }

    @Test
    public void testIsValidReturnsFalseWhenDisallowedAttributeExists() {
        Whitelist wl = Whitelist.basic();
        Cleaner cleaner = new Cleaner(wl);

        String html = "<a href=\"http://example.com\" style=\"color:red;\">link</a>";
        Document dirty = parse(html);
        assertFalse(cleaner.isValid(dirty));
    }

    @Test
    public void testIsValidReturnsFalseWhenHeadHasContent() {
        Whitelist wl = Whitelist.basic();
        Cleaner cleaner = new Cleaner(wl);

        Document dirty = Document.createShell("");
        dirty.head().appendElement("title").text("Bad Head");
        dirty.body().appendElement("p").text("Body only");

        assertFalse(cleaner.isValid(dirty));
    }

    /* --------------------------------------------------------------------- */
    /*  isValidBodyHtml(String) – fragment validation                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsValidBodyHtmlAcceptsCleanFragment() {
        Whitelist wl = Whitelist.basic();
        Cleaner cleaner = new Cleaner(wl);

        String fragment = "<p>Valid <b>fragment</b></p>";
        assertTrue(cleaner.isValidBodyHtml(fragment));
    }

    @Test
    public void testIsValidBodyHtmlRejectsFragmentWithDisallowedTag() {
        Whitelist wl = Whitelist.basic();
        Cleaner cleaner = new Cleaner(wl);

        String fragment = "<script>alert('x');</script>";
        assertFalse(cleaner.isValidBodyHtml(fragment));
    }

    @Test
    public void testIsValidBodyHtmlRejectsFragmentWithParseError() {
        Whitelist wl = Whitelist.basic();
        Cleaner cleaner = new Cleaner(wl);

        // Unclosed tag – this generates a parse error that the validator should catch.
        String fragment = "<p><b>Unclosed";
        assertFalse(cleaner.isValidBodyHtml(fragment));
    }

    /* --------------------------------------------------------------------- */
    /*  Internal sanity checks – copySafeNodes discarding count            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCopySafeNodesDiscardCountMatchesIsValidResult() throws Exception {
        // The Cleaner class does not expose copySafeNodes, but we can infer its count via isValid().
        Whitelist wl = Whitelist.none(); // everything should be discarded except text nodes
        Cleaner cleaner = new Cleaner(wl);

        String html = "<div>keep <span>discarded</span> text</div>";
        Document dirty = parse(html);
        // One element (<div>) is the root passed to copySafeNodes, so it is not counted as discarded.
        // The inner <span> should be counted as discarded (1). Text nodes are always kept.
        assertFalse(cleaner.isValid(dirty)); // because at least one element was discarded

        // To verify the exact count we can use reflection (optional, but demonstrates internal behaviour)
        java.lang.reflect.Method m = Cleaner.class.getDeclaredMethod("copySafeNodes", Element.class, Element.class);
        m.setAccessible(true);
        Document cleanShell = Document.createShell(dirty.baseUri());
        int discarded = (int) m.invoke(cleaner, dirty.body(), cleanShell.body());
        assertEquals(1, discarded);
    }
}
```
###Test END##