###Test START##
package org.jsoup.nodes;

import static org.junit.Assert.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.parser.ParseSettings;
import org.jsoup.parser.Tag;
import org.jsoup.parser.XmlDeclaration;
import org.jsoup.select.Elements;
import org.junit.Test;

/**
 * Comprehensive JUnit4 test suite for {@link Document}.
 */
public class DocumentTest {

    /* ---------- Constructor & basic accessors ---------- */

    @Test
    public void testDocumentConstructorAndLocation() {
        String base = "http://example.com/";
        Document doc = new Document(base);
        assertEquals("Location should be the base URI passed to constructor", base, doc.location());
        assertEquals("Base URI of root element should match", base, doc.baseUri());
    }

    @Test
    public void testCreateShellStructure() {
        String base = "http://example.com/";
        Document shell = Document.createShell(base);
        assertNotNull("Shell must have an <html> element", shell.selectFirst("html"));
        assertNotNull("Shell must have a <head> element", shell.head());
        assertNotNull("Shell must have a <body> element", shell.body());

        // Verify hierarchy: html > head, body
        Element html = shell.selectFirst("html");
        assertEquals("Head should be child of html", html, shell.head().parent());
        assertEquals("Body should be child of html", html, shell.body().parent());

        // Verify base URI propagation
        assertEquals(base, shell.head().baseUri());
        assertEquals(base, shell.body().baseUri());
    }

    @Test
    public void testHeadAndBodyAccessors() {
        Document doc = Document.createShell("http://example.com/");
        assertEquals("head() should return the first <head>", "head", doc.head().tagName());
        assertEquals("body() should return the first <body>", "body", doc.body().tagName());
    }

    /* ---------- Title getter / setter ---------- */

    @Test
    public void testTitleGetterAndSetterWhenAbsent() {
        Document doc = Document.createShell("http://example.com/");
        // Initially no title
        assertEquals("Empty title when none is set", "", doc.title());

        // Set title – should create <title> inside <head>
        doc.title("  My   Title  ");
        Element titleEl = doc.selectFirst("title");
        assertNotNull("Title element should be created", titleEl);
        assertEquals("Title text should be set", "  My   Title  ", titleEl.text());

        // Getter should normalise whitespace and trim
        assertEquals("Getter should normalise and trim whitespace", "My Title", doc.title());
    }

    @Test
    public void testTitleSetterWhenPresent() {
        Document doc = Document.createShell("http://example.com/");
        doc.head().appendElement("title").text("Old");
        assertEquals("Old", doc.title());

        doc.title("New Title");
        assertEquals("New Title", doc.title());
        assertEquals("Only one title element should exist", 1, doc.getElementsByTag("title").size());
    }

    /* ---------- createElement ---------- */

    @Test
    public void testCreateElementInheritsBaseUri() {
        Document doc = new Document("http://example.com/");
        Element p = doc.createElement("p");
        assertEquals("p", p.tagName());
        assertEquals("Base URI should be inherited", "http://example.com/", p.baseUri());
    }

    /* ---------- normalise ---------- */

    @Test
    public void testNormaliseAddsMissingHtmlHeadBody() {
        Document doc = new Document("http://example.com/");
        // No html/head/body yet
        doc.appendText(" stray text ");
        doc.normalise();

        assertNotNull("After normalise, <html> should exist", doc.selectFirst("html"));
        assertNotNull("After normalise, <head> should exist", doc.head());
        assertNotNull("After normalise, <body> should exist", doc.body());

        // Text should have been moved into body (preserving original whitespace)
        assertEquals("Body should contain the stray text", " stray text ", doc.body().text());
    }

    @Test
    public void testNormaliseMergesDuplicateHeadAndBody() {
        Document doc = Document.createShell("http://example.com/");
        // Add duplicate head with a meta element
        Element extraHead = doc.appendElement("head");
        extraHead.appendElement("meta").attr("name", "extra");
        // Add duplicate body with a paragraph
        Element extraBody = doc.appendElement("body");
        extraBody.appendElement("p").text("extra paragraph");

        doc.normalise();

        // Only one head and one body should remain
        assertEquals("Only one <head> after normalise", 1, doc.getElementsByTag("head").size());
        assertEquals("Only one <body> after normalise", 1, doc.getElementsByTag("body").size());

        // Contents from duplicates should be merged into the master head/body
        Elements metas = doc.head().select("meta[name=extra]");
        assertEquals("Meta from duplicate head should be merged", 1, metas.size());

        Elements paras = doc.body().select("p");
        assertEquals("Paragraph from duplicate body should be merged", 1, paras.size());
        assertEquals("Paragraph text should be preserved", "extra paragraph", paras.first().text());
    }

    @Test
    public void testNormaliseMovesTextNodesInOrder() {
        Document doc = Document.createShell("http://example.com/");
        // Add text nodes at various levels
        doc.appendText("A");
        doc.head().appendText("B");
        doc.body().appendText("C");

        doc.normalise();

        // The body should now contain the moved texts in the original order: A B C
        List<TextNode> texts = doc.body().textNodes();
        assertEquals("Three text nodes expected in body", 3, texts.size());
        assertEquals("First moved text should be 'A'", "A", texts.get(0).text());
        assertEquals("Second moved text should be 'B'", "B", texts.get(1).text());
        assertEquals("Third moved text should be 'C'", "C", texts.get(2).text());
    }

    /* ---------- charset handling & meta charset element ---------- */

    @Test
    public void testCharsetUpdatesMetaWhenFlagEnabledHtmlSyntax() {
        Document doc = Document.createShell("http://example.com/");
        doc.updateMetaCharsetElement(true);
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.html);
        doc.charset(StandardCharsets.ISO_8859_1);

        // Meta charset element should be present with the correct charset name
        Element meta = doc.selectFirst("meta[charset]");
        assertNotNull("Meta charset element should be added", meta);
        assertEquals("Charset attribute should match set charset", StandardCharsets.ISO_8859_1.displayName(), meta.attr("charset"));

        // Obsolete meta[name=charset] should be removed (none present initially)
        assertTrue("No obsolete meta[name=charset] should remain", doc.select("meta[name=charset]").isEmpty());
    }

    @Test
    public void testCharsetUpdatesXmlDeclarationWhenXmlSyntax() {
        Document doc = new Document("http://example.com/");
        doc.updateMetaCharsetElement(true);
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        doc.charset(StandardCharsets.UTF_16);

        // First child should be an XmlDeclaration with correct attributes
        Node first = doc.childNode(0);
        assertTrue("First node should be an XmlDeclaration", first instanceof XmlDeclaration);
        XmlDeclaration decl = (XmlDeclaration) first;
        assertEquals("xml", decl.name());
        assertEquals("1.0", decl.attr("version"));
        assertEquals(StandardCharsets.UTF_16.displayName(), decl.attr("encoding"));
    }

    @Test
    public void testCharsetDoesNotUpdateMetaWhenFlagDisabled() {
        Document doc = Document.createShell("http://example.com/");
        // Ensure flag is false (default)
        assertFalse("updateMetaCharsetElement should be false by default", doc.updateMetaCharsetElement());

        doc.charset(StandardCharsets.US_ASCII);
        // No meta charset should be added
        assertNull("Meta charset element should not be added when flag is false", doc.selectFirst("meta[charset]"));
    }

    @Test
    public void testUpdateMetaCharsetFlagGetterSetter() {
        Document doc = new Document("http://example.com/");
        assertFalse(doc.updateMetaCharsetElement());

        doc.updateMetaCharsetElement(true);
        assertTrue(doc.updateMetaCharsetElement());

        doc.updateMetaCharsetElement(false);
        assertFalse(doc.updateMetaCharsetElement());
    }

    /* ---------- OutputSettings getter/setter ---------- */

    @Test
    public void testOutputSettingsGetterAndSetter() {
        Document doc = new Document("http://example.com/");
        Document.OutputSettings custom = new Document.OutputSettings()
                .prettyPrint(false)
                .outline(true)
                .indentAmount(4)
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.extended)
                .charset(StandardCharsets.UTF_16);

        doc.outputSettings(custom);
        Document.OutputSettings out = doc.outputSettings();

        assertEquals(false, out.prettyPrint());
        assertEquals(true, out.outline());
        assertEquals(4, out.indentAmount());
        assertEquals(Document.OutputSettings.Syntax.xml, out.syntax());
        assertEquals(Entities.EscapeMode.extended, out.escapeMode());
        assertEquals(StandardCharsets.UTF_16, out.charset());
    }

    @Test
    public void testOutputSettingsCloneIndependence() {
        Document doc = new Document("http://example.com/");
        doc.outputSettings().prettyPrint(false).indentAmount(2);
        Document clone = doc.clone();

        // Settings objects should be distinct
        assertNotSame("Cloned document should have its own OutputSettings instance", doc.outputSettings(), clone.outputSettings());

        // Verify that cloned settings retain the same values
        assertEquals(false, clone.outputSettings().prettyPrint());
        assertEquals(2, clone.outputSettings().indentAmount());

        // Changing original should not affect clone
        doc.outputSettings().prettyPrint(true);
        assertEquals("Original change should not affect clone", false, clone.outputSettings().prettyPrint());
    }

    /* ---------- QuirksMode getter/setter ---------- */

    @Test
    public void testQuirksModeGetterSetter() {
        Document doc = new Document("http://example.com/");
        assertEquals(Document.QuirksMode.noQuirks, doc.quirksMode());

        doc.quirksMode(Document.QuirksMode.quirks);
        assertEquals(Document.QuirksMode.quirks, doc.quirksMode());

        doc.quirksMode(Document.QuirksMode.limitedQuirks);
        assertEquals(Document.QuirksMode.limitedQuirks, doc.quirksMode());
    }

    /* ---------- Document clone ---------- */

    @Test
    public void testDocumentCloneCreatesDeepCopy() {
        Document original = Document.createShell("http://example.com/");
        original.title("Original Title");
        original.body().appendElement("p").text("Paragraph");
        original.updateMetaCharsetElement(true);
        original.charset(StandardCharsets.ISO_8859_2);

        Document copy = original.clone();

        // Verify structural equality
        assertEquals(original.title(), copy.title());
        assertEquals(original.body().html(), copy.body().html());

        // Verify that modifications to the clone do not affect the original
        copy.title("Clone Title");
        copy.body().appendElement("div").text("New div");
        assertNotEquals("Title change in clone should not affect original", original.title(), copy.title());
        assertFalse("Original body should not contain the new div", original.body().selectFirst("div") != null);
    }

    /* ---------- OutputSettings internal behaviour ---------- */

    @Test
    public void testOutputSettingsIndentAmountValidation() {
        Document.OutputSettings out = new Document.OutputSettings();
        out.indentAmount(0); // valid
        assertEquals(0, out.indentAmount());

        try {
            out.indentAmount(-1);
            fail("Setting negative indentAmount should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testOutputSettingsCharsetPreparation() {
        Document.OutputSettings out = new Document.OutputSettings().charset(StandardCharsets.UTF_8);
        CharsetEncoder encoder = out.encoder(); // triggers prepare if needed
        assertNotNull("Encoder should be prepared", encoder);
        assertEquals(StandardCharsets.UTF_8, encoder.charset());
    }

    @Test
    public void testOutputSettingsCloneCopiesAllRelevantFields() {
        Document.OutputSettings original = new Document.OutputSettings()
                .charset(StandardCharsets.UTF_16)
                .escapeMode(Entities.EscapeMode.extended)
                .prettyPrint(false)
                .outline(true)
                .indentAmount(5)
                .syntax(Document.OutputSettings.Syntax.xml);

        Document.OutputSettings cloned = original.clone();

        assertEquals(original.charset(), cloned.charset());
        assertEquals(original.escapeMode(), cloned.escapeMode());
        assertEquals(original.prettyPrint(), cloned.prettyPrint());
        assertEquals(original.outline(), cloned.outline());
        assertEquals(original.indentAmount(), cloned.indentAmount());
        assertEquals(original.syntax(), cloned.syntax());

        // Ensure they are different instances
        assertNotSame(original, cloned);
    }

    /* ---------- Miscellaneous edge cases ---------- */

    @Test
    public void testTitleSetterRejectsNull() {
        Document doc = new Document("http://example.com/");
        try {
            doc.title(null);
            fail("Setting null title should throw NullPointerException");
        } catch (NullPointerException | IllegalArgumentException e) {
            // Expected (Validate.notNull throws IllegalArgumentException in Jsoup)
        }
    }

    @Test
    public void testCreateShellRejectsNullBaseUri() {
        try {
            Document.createShell(null);
            fail("createShell should reject null baseUri");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testDocumentTextOverridesBodyOnly() {
        Document doc = Document.createShell("http://example.com/");
        doc.body().appendElement("p").text("old");
        doc.text("new body text");
        assertEquals("new body text", doc.body().text());
        // Ensure <head> and <html> are untouched
        assertNotNull(doc.head());
        assertEquals("html", doc.selectFirst("html").tagName());
    }

    @Test
    public void testOuterHtmlUsesDocumentHtml() {
        Document doc = Document.createShell("http://example.com/");
        doc.title("Sample");
        String outer = doc.outerHtml();
        assertTrue("outerHtml should contain <title>", outer.contains("<title>Sample</title>"));
        assertFalse("outerHtml should not start with a wrapper tag", outer.startsWith("<html"));
    }
}
###Test END##