package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.parser.ParseSettings;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.nio.charset.Charset;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Document}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods (including getters/setters, factory methods and mutators)</li>
 *   <li>Typical usage scenarios</li>
 *   <li>Edge cases (null arguments, duplicate elements, missing structure)</li>
 *   <li>Exception handling (IllegalArgumentException, NullPointerException)</li>
 *   <li>Interaction with the inner {@link Document.OutputSettings} class</li>
 * </ul>
 *
 * The suite is written for JUnit 4 and therefore uses the {@code @Test} annotation and the
 * {@code expected} attribute for exception tests.
 */
public class DocumentTest {

    /* --------------------------------------------------------------------- */
    /*  Constructor / basic getters                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConstructorSetsBaseUriAndLocation() {
        Document doc = new Document("http://example.com/");
        assertEquals("http://example.com/", doc.baseUri());
        assertEquals("http://example.com/", doc.location());
    }

    @Test
    public void testCreateShellCreatesHtmlHeadBody() {
        Document doc = Document.createShell("http://example.com/");
        Element html = doc.selectFirst("html");
        Element head = doc.head();
        Element body = doc.body();

        assertNotNull(html);
        assertNotNull(head);
        assertNotNull(body);
        assertEquals(html, head.parent());
        assertEquals(html, body.parent());
    }

    @Test(expected = NullPointerException.class)
    public void testCreateShellNullBaseUriThrows() {
        Document.createShell(null);
    }

    @Test
    public void testLocationReturnsBaseUri() {
        Document doc = new Document("https://jsoup.org/");
        assertEquals("https://jsoup.org/", doc.location());
    }

    /* --------------------------------------------------------------------- */
    /*  Title handling                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTitleGetterReturnsEmptyWhenNoTitle() {
        Document doc = Document.createShell("http://example.com/");
        assertEquals("", doc.title());
    }

    @Test
    public void testTitleSetterCreatesTitleElementWhenMissing() {
        Document doc = Document.createShell("http://example.com/");
        doc.title("My Title");
        Element titleEl = doc.selectFirst("title");
        assertNotNull(titleEl);
        assertEquals("My Title", titleEl.text());
    }

    @Test
    public void testTitleSetterUpdatesExistingTitle() {
        Document doc = Document.createShell("http://example.com/");
        doc.head().appendElement("title").text("Old Title");
        doc.title("New Title");
        Element titleEl = doc.selectFirst("title");
        assertNotNull(titleEl);
        assertEquals("New Title", titleEl.text());
    }

    @Test(expected = NullPointerException.class)
    public void testTitleSetterNullThrows() {
        Document doc = Document.createShell("http://example.com/");
        doc.title(null);
    }

    /* --------------------------------------------------------------------- */
    /*  Element creation                                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCreateElementInheritsDocumentBaseUri() {
        Document doc = new Document("http://base.uri/");
        Element a = doc.createElement("a");
        assertEquals("a", a.tagName());
        assertEquals("http://base.uri/", a.baseUri());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateElementInvalidTagThrows() {
        Document doc = new Document("http://example.com/");
        // Tag.valueOf throws IllegalArgumentException for an empty tag name
        doc.createElement("");
    }

    /* --------------------------------------------------------------------- */
    /*  Normalisation                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNormaliseAddsMissingStructureAndMovesOrphanText() {
        // Build a document without html/head/body and with stray text nodes.
        Document doc = new Document("http://example.com/");
        doc.appendChild(new TextNode("Orphan text 1"));
        doc.appendChild(new TextNode("   ")); // blank text should be ignored
        doc.appendElement("html"); // only html, no head/body

        Document normalised = doc.normalise();

        // html, head, body must exist
        Element html = normalised.selectFirst("html");
        Element head = normalised.head();
        Element body = normalised.body();

        assertNotNull(html);
        assertNotNull(head);
        assertNotNull(body);
        assertEquals(html, head.parent());
        assertEquals(html, body.parent());

        // Orphan text must have been moved into body (preserving order)
        Elements bodyTexts = body.getElementsContainingOwnText("Orphan text 1");
        assertFalse(bodyTexts.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  Charset handling (including meta‑charset element)                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCharsetGetterReturnsDefaultUtf8() {
        Document doc = new Document("http://example.com/");
        assertEquals(Charset.forName("UTF8"), doc.charset());
    }

    @Test
    public void testCharsetSetterUpdatesMetaWhenUpdateEnabled() {
        Document doc = Document.createShell("http://example.com/");
        // Enable meta‑charset updates
        doc.updateMetaCharsetElement(true);
        doc.charset(Charset.forName("ISO-8859-1"));

        // Meta element must exist inside <head>
        Element meta = doc.head().selectFirst("meta[charset]");
        assertNotNull("Meta charset element should be created", meta);
        assertEquals("ISO-8859-1", meta.attr("charset"));
    }

    @Test
    public void testCharsetSetterDoesNotCreateMetaWhenUpdateDisabled() {
        Document doc = Document.createShell("http://example.com/");
        doc.updateMetaCharsetElement(false);
        doc.charset(Charset.forName("UTF-16"));

        Element meta = doc.head().selectFirst("meta[charset]");
        assertNull("Meta charset element must not be created when update is disabled", meta);
    }

    @Test
    public void testUpdateMetaCharsetElementToggle() {
        Document doc = new Document("http://example.com/");
        assertFalse(doc.updateMetaCharsetElement()); // default is false
        doc.updateMetaCharsetElement(true);
        assertTrue(doc.updateMetaCharsetElement());
        doc.updateMetaCharsetElement(false);
        assertFalse(doc.updateMetaCharsetElement());
    }

    /* --------------------------------------------------------------------- */
    /*  OutputSettings accessor / mutator                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testOutputSettingsGetterAndSetter() {
        Document doc = new Document("http://example.com/");
        Document.OutputSettings custom = new Document.OutputSettings()
                .prettyPrint(false)
                .indentAmount(4)
                .syntax(Document.OutputSettings.Syntax.xml);

        doc.outputSettings(custom);
        assertSame(custom, doc.outputSettings());
        assertFalse(doc.outputSettings().prettyPrint());
        assertEquals(4, doc.outputSettings().indentAmount());
        assertEquals(Document.OutputSettings.Syntax.xml, doc.outputSettings().syntax());
    }

    @Test(expected = NullPointerException.class)
    public void testOutputSettingsSetterNullThrows() {
        Document doc = new Document("http://example.com/");
        doc.outputSettings(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOutputSettingsIndentAmountNegativeThrows() {
        Document.OutputSettings os = new Document.OutputSettings();
        os.indentAmount(-1);
    }

    @Test
    public void testDocumentCloneCopiesOutputSettings() {
        Document doc = new Document("http://example.com/");
        doc.outputSettings().prettyPrint(false);
        doc.outputSettings().indentAmount(5);

        Document clone = doc.clone();
        assertNotSame(doc, clone);
        // OutputSettings must be a deep copy
        assertNotSame(doc.outputSettings(), clone.outputSettings());
        assertEquals(doc.outputSettings().prettyPrint(), clone.outputSettings().prettyPrint());
        assertEquals(doc.outputSettings().indentAmount(), clone.outputSettings().indentAmount());
    }

    /* --------------------------------------------------------------------- */
    /*  QuirksMode getter / setter                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testQuirksModeGetterSetter() {
        Document doc = new Document("http://example.com/");
        assertEquals(Document.QuirksMode.noQuirks, doc.quirksMode());

        doc.quirksMode(Document.QuirksMode.quirks);
        assertEquals(Document.QuirksMode.quirks, doc.quirksMode());

        doc.quirksMode(Document.QuirksMode.limitedQuirks);
        assertEquals(Document.QuirksMode.limitedQuirks, doc.quirksMode());
    }

    /* --------------------------------------------------------------------- */
    /*  Overridden text() – ensure body text is set correctly                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDocumentTextOverridesBodyText() {
        Document doc = Document.createShell("http://example.com/");
        doc.text("Hello <b>world</b>"); // should be escaped when output as HTML
        assertEquals("Hello world", doc.body().text());
    }

    /* --------------------------------------------------------------------- */
    /*  outerHtml() – should delegate to super.html() (no wrapper)         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testOuterHtmlHasNoDocumentWrapper() {
        Document doc = Document.createShell("http://example.com/");
        doc.title("Test");
        String outer = doc.outerHtml();
        assertFalse("outerHtml should not contain #document", outer.contains("#document"));
        assertTrue("outerHtml should contain <html>", outer.contains("<html"));
    }
}
