package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Document}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction and static factory methods</li>
 *   <li>Basic accessors (location, head, body, title, nodeName)</li>
 *   <li>Mutators (title setter, text, createElement)</li>
 *   <li>Normalisation logic – duplicate heads/bodies, stray text nodes, element ordering</li>
 *   <li>Charset handling – meta charset element creation, update, removal for both HTML and XML syntax</li>
 *   <li>OutputSettings getter / setter and cloning behaviour</li>
 *   <li>Quirks mode handling</li>
 *   <li>Error scenarios (null arguments, illegal state)</li>
 * </ul>
 *
 * The test class can be executed directly with JUnit‑4 or any IDE that supports it.
 */
public class DocumentTest {

    private static final String BASE_URI = "http://example.com";

    private Document emptyDoc;
    private Document shellDoc;

    @Before
    public void setUp() {
        emptyDoc = new Document(BASE_URI);
        shellDoc = Document.createShell(BASE_URI);
    }

    // -----------------------------------------------------------------
    // Construction & static factory
    // -----------------------------------------------------------------
    @Test
    public void testDocumentConstructorSetsLocation() {
        assertEquals(BASE_URI, emptyDoc.location());
    }

    @Test
    public void testCreateShellCreatesHtmlHeadAndBody() {
        assertNotNull(shellDoc.head());
        assertNotNull(shellDoc.body());
        assertEquals("head", shellDoc.head().tagName());
        assertEquals("body", shellDoc.body().tagName());
        // the root element must be <html>
        Element html = shellDoc.selectFirst("html");
        assertNotNull(html);
        assertTrue(html.children().contains(shellDoc.head()));
        assertTrue(html.children().contains(shellDoc.body()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateShellWithNullBaseUriThrows() {
        Document.createShell(null);
    }

    // -----------------------------------------------------------------
    // Basic accessors
    // -----------------------------------------------------------------
    @Test
    public void testHeadAndBodyAccessors() {
        Document doc = new Document(BASE_URI);
        Element html = doc.appendElement("html");
        Element head = html.appendElement("head");
        Element body = html.appendElement("body");

        assertSame(head, doc.head());
        assertSame(body, doc.body());
    }

    @Test
    public void testTitleGetterWhenAbsentReturnsEmptyString() {
        assertEquals("", emptyDoc.title());
    }

    @Test
    public void testTitleGetterNormalisesWhitespace() {
        Document doc = new Document(BASE_URI);
        doc.appendElement("html").appendElement("head")
                .appendElement("title").text("  My   Title \n");
        // title() should trim and normalise spaces
        assertEquals("My Title", doc.title());
    }

    @Test
    public void testNodeNameIsDocument() {
        assertEquals("#document", emptyDoc.nodeName());
    }

    // -----------------------------------------------------------------
    // Title mutator
    // -----------------------------------------------------------------
    @Test
    public void testTitleSetterCreatesTitleIfMissing() {
        Document doc = Document.createShell(BASE_URI);
        assertNull(doc.selectFirst("title"));
        doc.title("New Title");
        Element title = doc.selectFirst("title");
        assertNotNull(title);
        assertEquals("New Title", title.text());
    }

    @Test
    public void testTitleSetterUpdatesExistingTitle() {
        Document doc = Document.createShell(BASE_URI);
        doc.head().appendElement("title").text("Old Title");
        doc.title("Updated Title");
        assertEquals("Updated Title", doc.selectFirst("title").text());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTitleSetterRejectsNull() {
        emptyDoc.title(null);
    }

    // -----------------------------------------------------------------
    // createElement
    // -----------------------------------------------------------------
    @Test
    public void testCreateElementUsesDocumentBaseUri() {
        Element a = emptyDoc.createElement("a");
        assertEquals("a", a.tagName());
        assertEquals(BASE_URI, a.baseUri());
    }

    // -----------------------------------------------------------------
    // text() override
    // -----------------------------------------------------------------
    @Test
    public void testTextOverridesBodyTextOnly() {
        Document doc = Document.createShell(BASE_URI);
        doc.text("Hello World");
        assertEquals("Hello World", doc.body().text());
        // The <head> must remain untouched
        assertEquals(0, doc.head().childNodeSize());
    }

    // -----------------------------------------------------------------
    // normalise()
    // -----------------------------------------------------------------
    @Test
    public void testNormaliseAddsMissingHtmlHeadBody() {
        Document doc = new Document(BASE_URI);
        // No html/head/body at this point
        doc.normalise();

        Element html = doc.selectFirst("html");
        assertNotNull(html);
        assertNotNull(doc.head());
        assertNotNull(doc.body());
        assertSame(html, doc.head().parent());
        assertSame(html, doc.body().parent());
    }

    @Test
    public void testNormaliseMergesDuplicateHeadsAndBodies() {
        Document doc = new Document(BASE_URI);
        // Build a structure with duplicate heads / bodies and stray text nodes
        Element html = doc.appendElement("html");
        Element head1 = html.appendElement("head");
        head1.appendElement("title").text("First");
        Element head2 = html.appendElement("head");
        head2.appendElement("meta").attr("charset", "UTF-8");
        Element body1 = html.appendElement("body");
        body1.appendElement("p").text("First body");
        Element body2 = html.appendElement("body");
        body2.appendElement("div").text("Second body");
        // stray text nodes at root and html
        doc.prependChild(new TextNode("Root text"));
        html.prependChild(new TextNode("Html text"));
        head1.prependChild(new TextNode("Head text"));
        // Normalise
        doc.normalise();

        // Only one head and one body should remain
        Elements heads = doc.getElementsByTag("head");
        Elements bodies = doc.getElementsByTag("body");
        assertEquals(1, heads.size());
        assertEquals(1, bodies.size());

        // All titles/meta should have been merged into the single head
        assertEquals("First", doc.selectFirst("head > title").text());
        assertEquals("UTF-8", doc.selectFirst("head > meta").attr("charset"));

        // All body content should be merged
        assertTrue(doc.select("body > p").first().text().contains("First body"));
        assertTrue(doc.select("body > div").first().text().contains("Second body"));

        // All stray text nodes should now be children of the body (preserving order)
        String bodyText = doc.body().text();
        assertTrue(bodyText.contains("Root text"));
        assertTrue(bodyText.contains("Html text"));
        assertTrue(bodyText.contains("Head text"));
    }

    // -----------------------------------------------------------------
    // OutputSettings getter / setter
    // -----------------------------------------------------------------
    @Test
    public void testOutputSettingsGetterAndSetter() {
        Document doc = new Document(BASE_URI);
        Document.OutputSettings settings = new Document.OutputSettings()
                .prettyPrint(false)
                .indentAmount(4)
                .escapeMode(Entities.EscapeMode.extended);
        doc.outputSettings(settings);
        Document.OutputSettings retrieved = doc.outputSettings();
        assertSame(settings, retrieved);
        assertFalse(retrieved.prettyPrint());
        assertEquals(4, retrieved.indentAmount());
        assertEquals(Entities.EscapeMode.extended, retrieved.escapeMode());
    }

    // -----------------------------------------------------------------
    // Charset handling (HTML syntax)
    // -----------------------------------------------------------------
    @Test
    public void testCharsetUpdatesMetaCharsetElementWhenEnabledHtml() {
        Document doc = Document.createShell(BASE_URI);
        doc.updateMetaCharsetElement(true);
        doc.charset(Charset.forName("ISO-8859-1")); // triggers ensureMetaCharsetElement()

        Element meta = doc.selectFirst("head > meta[charset]");
        assertNotNull(meta);
        assertEquals("ISO-8859-1", meta.attr("charset"));
        // obsolete meta[name=charset] must be removed (none present in this test)
        assertTrue(doc.select("meta[name=charset]").isEmpty());
    }

    @Test
    public void testCharsetDoesNotCreateMetaWhenUpdateDisabled() {
        Document doc = Document.createShell(BASE_URI);
        doc.updateMetaCharsetElement(false);
        doc.charset(Charset.forName("UTF-16"));
        // No meta[charset] should be added
        assertTrue(doc.select("meta[charset]").isEmpty());
    }

    // -----------------------------------------------------------------
    // Charset handling (XML syntax)
    // -----------------------------------------------------------------
    @Test
    public void testCharsetCreatesOrUpdatesXmlDeclaration() {
        Document doc = new Document(BASE_URI);
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        doc.updateMetaCharsetElement(true);
        doc.charset(Charset.forName("UTF-16"));

        // The first child should be an XmlDeclaration with encoding UTF-16
        Node first = doc.childNode(0);
        assertTrue(first instanceof XmlDeclaration);
        XmlDeclaration decl = (XmlDeclaration) first;
        assertEquals("xml", decl.name());
        assertEquals("UTF-16", decl.attr("encoding"));
        assertEquals("1.0", decl.attr("version"));
    }

    @Test
    public void testCharsetUpdatesExistingXmlDeclaration() {
        Document doc = new Document(BASE_URI);
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        doc.updateMetaCharsetElement(true);
        // Manually add an xml declaration
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        decl.attr("version", "1.0");
        decl.attr("encoding", "ISO-8859-1");
        doc.prependChild(decl);
        // Change charset – should update the same declaration
        doc.charset(Charset.forName("UTF-8"));
        XmlDeclaration updated = (XmlDeclaration) doc.childNode(0);
        assertEquals("UTF-8", updated.attr("encoding"));
        assertEquals("1.0", updated.attr("version"));
    }

    // -----------------------------------------------------------------
    // charset() getter
    // -----------------------------------------------------------------
    @Test
    public void testCharsetGetterReflectsOutputSettings() {
        Document doc = new Document(BASE_URI);
        Charset cs = Charset.forName("UTF-16");
        doc.charset(cs);
        assertEquals(cs, doc.charset());
    }

    // -----------------------------------------------------------------
    // updateMetaCharsetElement getter / setter
    // -----------------------------------------------------------------
    @Test
    public void testUpdateMetaCharsetElementFlag() {
        Document doc = new Document(BASE_URI);
        assertFalse(doc.updateMetaCharsetElement()); // default false
        doc.updateMetaCharsetElement(true);
        assertTrue(doc.updateMetaCharsetElement());
    }

    // -----------------------------------------------------------------
    // outerHtml() delegating to html()
    // -----------------------------------------------------------------
    @Test
    public void testOuterHtmlEqualsHtml() {
        Document doc = Document.createShell(BASE_URI);
        doc.head().appendElement("title").text("Sample");
        String outer = doc.outerHtml();
        String html = doc.html(); // html() is defined in Element
        assertEquals(html, outer);
    }

    // -----------------------------------------------------------------
    // clone()
    // -----------------------------------------------------------------
    @Test
    public void testCloneCreatesDeepCopyWithIndependentOutputSettings() {
        Document original = Document.createShell(BASE_URI);
        original.title("Original");
        original.outputSettings().prettyPrint(false);
        original.updateMetaCharsetElement(true);
        original.charset(Charset.forName("ISO-8859-1"));

        Document cloned = original.clone();

        // Verify deep copy semantics
        assertNotSame(original, cloned);
        assertEquals("Original", cloned.title());

        // OutputSettings must be a different instance but with same values
        assertNotSame(original.outputSettings(), cloned.outputSettings());
        assertEquals(original.outputSettings().prettyPrint(),
                cloned.outputSettings().prettyPrint());

        // Changing original's settings must not affect clone
        original.outputSettings().prettyPrint(true);
        assertFalse(cloned.outputSettings().prettyPrint());

        // Charset and meta element must be cloned correctly
        assertEquals(original.charset(), cloned.charset());
        Element metaOriginal = original.selectFirst("meta[charset]");
        Element metaClone = cloned.selectFirst("meta[charset]");
        assertNotNull(metaClone);
        assertEquals(metaOriginal.attr("charset"), metaClone.attr("charset"));
    }

    // -----------------------------------------------------------------
    // quirksMode getter / setter
    // -----------------------------------------------------------------
    @Test
    public void testQuirksModeGetterAndSetter() {
        Document doc = new Document(BASE_URI);
        assertEquals(Document.QuirksMode.noQuirks, doc.quirksMode());
        doc.quirksMode(Document.QuirksMode.quirks);
        assertEquals(Document.QuirksMode.quirks, doc.quirksMode());
    }

    // -----------------------------------------------------------------
    // Error handling for title setter (Validate.notNull)
    // -----------------------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void testTitleSetterValidateNotNullThrows() {
        emptyDoc.title(null);
    }

    // -----------------------------------------------------------------
    // Ensure meta charset element is not created when update flag is false
    // -----------------------------------------------------------------
    @Test
    public void testEnsureMetaCharsetElementNoOpWhenUpdateFlagFalse() {
        Document doc = Document.createShell(BASE_URI);
        doc.updateMetaCharsetElement(false);
        doc.charset(Charset.forName("UTF-8"));
        // No meta[charset] should be added
        assertTrue(doc.select("meta[charset]").isEmpty());
    }
}
