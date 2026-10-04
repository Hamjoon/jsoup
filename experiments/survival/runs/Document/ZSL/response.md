###Test START##
```java
package org.jsoup.nodes;

import org.jsoup.parser.ParseSettings;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Document}.
 * <p>
 * The tests cover all public behaviours of the class, including:
 * <ul>
 *   <li>construction and {@code createShell}</li>
 *   <li>head / body accessors</li>
 *   <li>title getter‑setter</li>
 *   <li>element creation</li>
 *   <li>normalisation (missing html/head/body, duplicate structures, text node handling)</li>
 *   <li>charset handling and meta‑charset updates (HTML syntax)</li>
 *   <li>output‑settings getter / setter and cloning</li>
 *   <li>quirks‑mode handling</li>
 *   <li>document cloning</li>
 *   <li>outerHtml, text override and nodeName</li>
 * </ul>
 * </p>
 */
public class DocumentTest {

    private static final String BASE_URI = "http://example.com/";
    private Document doc;

    @Before
    public void setUp() {
        doc = new Document(BASE_URI);
    }

    /*** Construction & basic accessors ***/

    @Test
    public void testLocation() {
        assertEquals(BASE_URI, doc.location());
    }

    @Test
    public void testCreateShell() {
        Document shell = Document.createShell(BASE_URI);
        assertNotNull(shell);
        assertEquals(BASE_URI, shell.location());

        // root must contain exactly one html element
        List<Element> htmls = shell.getElementsByTag("html");
        assertEquals(1, htmls.size());

        // html must contain head and body
        Element html = htmls.get(0);
        assertNotNull(html.selectFirst("head"));
        assertNotNull(html.selectFirst("body"));
        assertSame(shell.head(), html.selectFirst("head"));
        assertSame(shell.body(), html.selectFirst("body"));
    }

    @Test
    public void testHeadAndBodyAccessorsWhenMissing() {
        // freshly created document has no html/head/body yet
        assertNull(doc.head());
        assertNull(doc.body());

        // after normalising they should be present
        doc.normalise();
        assertNotNull(doc.head());
        assertNotNull(doc.body());
        assertEquals("head", doc.head().nodeName());
        assertEquals("body", doc.body().nodeName());
    }

    /*** Title handling ***/

    @Test
    public void testTitleGetterWhenAbsent() {
        assertEquals("", doc.title());
    }

    @Test
    public void testTitleSetterCreatesElement() {
        doc.title("My Title");
        assertEquals("My Title", doc.title());

        Element titleEl = doc.selectFirst("title");
        assertNotNull(titleEl);
        assertEquals("head", titleEl.parent().nodeName());
        assertEquals("My Title", titleEl.text());
    }

    @Test
    public void testTitleSetterUpdatesExisting() {
        doc.normalise(); // ensures head exists
        doc.head().appendElement("title").text("Old");
        doc.title("New");
        assertEquals("New", doc.title());
        assertEquals(1, doc.select("title").size());
        assertEquals("New", doc.selectFirst("title").text());
    }

    @Test(expected = NullPointerException.class)
    public void testTitleSetterRejectsNull() {
        doc.title(null);
    }

    /*** Element creation ***/

    @Test
    public void testCreateElementUsesBaseUri() {
        Element a = doc.createElement("a");
        assertEquals("a", a.tagName());
        assertEquals(BASE_URI, a.baseUri());
    }

    /*** Normalisation ***/

    @Test
    public void testNormaliseAddsMissingHtmlHeadBody() {
        // document is empty – call normalise and verify structure
        doc.normalise();

        Element html = doc.selectFirst("html");
        assertNotNull(html);
        assertNotNull(doc.head());
        assertNotNull(doc.body());

        // head and body must be direct children of html
        assertEquals(html, doc.head().parent());
        assertEquals(html, doc.body().parent());
    }

    @Test
    public void testNormaliseMergesDuplicateHeadElements() {
        doc.normalise(); // creates first html/head/body

        // add a second head with some content
        Element secondHead = doc.appendElement("head");
        secondHead.appendElement("meta").attr("name", "second");
        secondHead.appendElement("title").text("Second Title");

        // also add a second body with content
        Element secondBody = doc.appendElement("body");
        secondBody.appendElement("p").text("Second body paragraph");

        doc.normalise();

        // there must be exactly one head and one body
        assertEquals(1, doc.getElementsByTag("head").size());
        assertEquals(1, doc.getElementsByTag("body").size());

        // content from duplicates must be merged into the first head/body
        assertNotNull(doc.head().selectFirst("meta[name=second]"));
        assertEquals("Second Title", doc.head().selectFirst("title").text());
        assertNotNull(doc.body().selectFirst("p"));
        assertEquals("Second body paragraph", doc.body().selectFirst("p").text());
    }

    @Test
    public void testNormaliseMovesRootTextNodesToBody() {
        // add non‑blank text directly under the document root
        doc.appendChild(new TextNode("Root text"));
        // also add blank text – should be ignored
        doc.appendChild(new TextNode("   "));

        doc.normalise();

        // body should now contain the moved text node preceded by a single space node
        List<Node> bodyChildren = doc.body().childNodes();
        assertEquals(2, bodyChildren.size());

        // first child is a space TextNode
        assertTrue(bodyChildren.get(0) instanceof TextNode);
        assertEquals(" ", ((TextNode) bodyChildren.get(0)).text());

        // second child is the original non‑blank text
        assertTrue(bodyChildren.get(1) instanceof TextNode);
        assertEquals("Root text", ((TextNode) bodyChildren.get(1)).text());
    }

    /*** Charset handling and meta‑charset element ***/

    @Test
    public void testCharsetGetterAndSetterUpdatesMetaWhenEnabled() {
        doc.updateMetaCharsetElement(true);
        Charset iso = Charset.forName("ISO-8859-1");
        doc.charset(iso);

        // OutputSettings must reflect the new charset
        assertEquals(iso, doc.charset());

        // meta charset element should have been added/updated in the head
        Element meta = doc.head().selectFirst("meta[charset]");
        assertNotNull(meta);
        assertEquals(iso.displayName(), meta.attr("charset"));
    }

    @Test
    public void testCharsetDoesNotCreateMetaWhenUpdateDisabled() {
        doc.updateMetaCharsetElement(false);
        Charset utf16 = Charset.forName("UTF-16");
        doc.charset(utf16);

        // No meta charset element should be present
        assertNull(doc.head().selectFirst("meta[charset]"));
        // OutputSettings still holds the charset
        assertEquals(utf16, doc.charset());
    }

    @Test
    public void testObsoleteMetaCharsetRemovedWhenUpdating() {
        // Create an obsolete meta element
        doc.normalise();
        doc.head().appendElement("meta").attr("name", "charset").attr("content", "old");
        // Enable updating and set new charset
        doc.updateMetaCharsetElement(true);
        doc.charset(Charset.forName("UTF-8"));

        // obsolete meta must be gone
        assertNull(doc.head().selectFirst("meta[name=charset]"));
        // new meta must exist
        Element meta = doc.head().selectFirst("meta[charset]");
        assertNotNull(meta);
        assertEquals("UTF-8", meta.attr("charset"));
    }

    /*** OutputSettings getter / setter ***/

    @Test
    public void testOutputSettingsGetterAndSetter() {
        Document.OutputSettings custom = new Document.OutputSettings()
                .prettyPrint(false)
                .indentAmount(4)
                .syntax(Document.OutputSettings.Syntax.xml);
        doc.outputSettings(custom);
        assertSame(custom, doc.outputSettings());

        // verify settings are applied
        assertFalse(doc.outputSettings().prettyPrint());
        assertEquals(4, doc.outputSettings().indentAmount());
        assertEquals(Document.OutputSettings.Syntax.xml, doc.outputSettings().syntax());
    }

    @Test
    public void testDocumentCloneCopiesOutputSettings() {
        doc.outputSettings().prettyPrint(false);
        doc.title("Clone Test");
        Document cloned = doc.clone();

        // cloned document should have its own OutputSettings instance
        assertNotSame(doc.outputSettings(), cloned.outputSettings());
        assertFalse(cloned.outputSettings().prettyPrint());

        // structural clone – title element should be present and equal
        assertEquals("Clone Test", cloned.title());
        assertNotSame(doc.title(), cloned.title()); // strings are immutable, but we check element identity
        assertNotSame(doc.head(), cloned.head());
    }

    /*** Quirks mode ***/

    @Test
    public void testQuirksModeSetterAndGetter() {
        assertEquals(Document.QuirksMode.noQuirks, doc.quirksMode());
        doc.quirksMode(Document.QuirksMode.quirks);
        assertEquals(Document.QuirksMode.quirks, doc.quirksMode());
    }

    /*** OuterHtml, text override and nodeName ***/

    @Test
    public void testOuterHtmlDoesNotIncludeDocumentWrapper() {
        doc.normalise();
        doc.body().appendElement("p").text("Hello");
        String html = doc.outerHtml();
        // outerHtml should start with <html> not with #document
        assertTrue(html.trim().startsWith("<html"));
        assertTrue(html.contains("<p>Hello</p>"));
    }

    @Test
    public void testTextOverridesBodyContent() {
        doc.normalise();
        doc.body().appendElement("div").text("old");
        doc.text("new body text");
        assertEquals("new body text", doc.body().text());
        // ensure the old div has been removed
        assertNull(doc.body().selectFirst("div"));
    }

    @Test
    public void testNodeName() {
        assertEquals("#document", doc.nodeName());
    }

    /*** UpdateMetaCharset flag getter / setter ***/

    @Test
    public void testUpdateMetaCharsetFlag() {
        assertFalse(doc.updateMetaCharsetElement());
        doc.updateMetaCharsetElement(true);
        assertTrue(doc.updateMetaCharsetElement());
        doc.updateMetaCharsetElement(false);
        assertFalse(doc.updateMetaCharsetElement());
    }

    /*** Additional edge cases ***/

    @Test(expected = IllegalArgumentException.class)
    public void testIndentAmountValidation() {
        new Document.OutputSettings().indentAmount(-1);
    }

    @Test
    public void testCharsetMethodChainsOutputSettings() {
        Document.OutputSettings settings = doc.outputSettings();
        doc.charset(Charset.forName("US-ASCII"));
        // charset method must have delegated to OutputSettings.charset()
        assertEquals(Charset.forName("US-ASCII"), settings.charset());
    }
}
```
###Test END##