###Test START##
```java
/**
 * JUnit 4 test suite for {@link XmlTreeBuilder}.
 *
 * The tests cover:
 * <ul>
 *   <li>default settings (case preservation)</li>
 *   <li>parsing of simple XML, self‑closing tags, comments, XML declarations and doctypes</li>
 *   <li>behaviour of the stack when closing tags (popStackToClose)</li>
 *   <li>fragment parsing</li>
 *   <li>edge cases such as empty input and unknown self‑closing tags</li>
 * </ul>
 *
 * The test class is placed in the same package {@code org.jsoup.parser}
 * so that package‑private methods of {@code XmlTreeBuilder} can be accessed
 * if needed (e.g. {@code parseFragment}).
 */
package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.*;
import org.jsoup.parser.Token.StartTag;
import org.jsoup.parser.Token.EndTag;
import org.jsoup.parser.Token.Comment;
import org.jsoup.parser.Token.Character;
import org.jsoup.parser.Token.Doctype;
import org.junit.Before;
import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.*;

public class XmlTreeBuilderTest {

    private XmlTreeBuilder builder;

    @Before
    public void setUp() {
        builder = new XmlTreeBuilder();
    }

    /** Ensure the default settings preserve case. */
    @Test
    public void testDefaultSettingsPreserveCase() {
        ParseSettings settings = builder.defaultSettings();
        assertTrue("Default settings should preserve case", settings == ParseSettings.preserveCase);
    }

    /** Parse a minimal XML document and verify element hierarchy and text. */
    @Test
    public void testParseSimpleXml() {
        String xml = "<root><child>Text</child></root>";
        Document doc = builder.parse(xml, "http://example.com/");
        Element root = doc.selectFirst("root");
        assertNotNull("Root element should be present", root);
        Element child = root.selectFirst("child");
        assertNotNull("Child element should be present", child);
        assertEquals("Text node should be preserved", "Text", child.text());
    }

    /** Self‑closing tags must not be added to the stack. */
    @Test
    public void testParseSelfClosingTag() {
        String xml = "<root><br/></root>";
        Document doc = builder.parse(xml, "");
        Element br = doc.selectFirst("br");
        assertNotNull("Self‑closing <br/> element must exist", br);
        // The Tag instance should be marked as self‑closing because <br> is a known HTML tag.
        assertTrue("Known tag <br> should be self‑closing after parsing", br.tag().isSelfClosing());
    }

    /** Unknown self‑closing tags should retain the self‑closing flag. */
    @Test
    public void testParseUnknownSelfClosingTag() {
        String xml = "<root><custom/></root>";
        Document doc = builder.parse(xml, "");
        Element custom = doc.selectFirst("custom");
        assertNotNull("Unknown self‑closing <custom/> element must exist", custom);
        // For unknown tags Jsoup creates a Tag that is not known; the builder sets self‑closing manually.
        assertTrue("Unknown tag should be marked self‑closing", custom.tag().isSelfClosing());
    }

    /** XML declaration (<?xml ...?>) is parsed as an {@link XmlDeclaration}. */
    @Test
    public void testParseXmlDeclaration() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root></root>";
        Document doc = builder.parse(xml, "");
        // The first child of the document should be an XmlDeclaration node.
        Node first = doc.childNode(0);
        assertTrue("First node should be an XmlDeclaration", first instanceof XmlDeclaration);
        XmlDeclaration decl = (XmlDeclaration) first;
        assertEquals("Version attribute should be parsed", "1.0", decl.attr("version"));
        assertEquals("Encoding attribute should be parsed", "UTF-8", decl.attr("encoding"));
        // The declaration is a processing instruction (not a comment), so isData should be false.
        assertFalse("XmlDeclaration.isData() should be false for <?xml ...?>", decl.isData());
    }

    /** HTML‑style comments are parsed as {@link Comment} nodes. */
    @Test
    public void testParseComment() {
        String xml = "<root><!-- This is a comment --></root>";
        Document doc = builder.parse(xml, "");
        Comment comment = (Comment) doc.selectFirst("root").childNode(0);
        assertEquals("Comment data should be preserved", " This is a comment ", comment.getData());
    }

    /** Doctype declarations are parsed as {@link DocumentType} nodes. */
    @Test
    public void testParseDoctype() {
        String xml = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><root></root>";
        Document doc = builder.parse(xml, "");
        // The first child of the document should be a DocumentType node.
        Node first = doc.childNode(0);
        assertTrue("First node should be a DocumentType", first instanceof DocumentType);
        DocumentType dt = (DocumentType) first;
        assertEquals("Doctype name", "html", dt.name());
        assertEquals("Public identifier", "-//W3C//DTD XHTML 1.0 Transitional//EN", dt.publicId());
        assertEquals("System identifier", "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd", dt.systemId());
    }

    /** Verify that mismatched end tags trigger the stack‑pop logic without throwing. */
    @Test
    public void testPopStackToCloseWithMismatchedTags() {
        // The XML is deliberately malformed: </b> appears before </a>
        String xml = "<a><b></a></b>";
        Document doc = builder.parse(xml, "");
        // After parsing, both elements should have been closed; the document should contain a single <a> element
        // whose child is <b>. The mismatched closing order is ignored by popStackToClose (it just removes up to the match).
        Element a = doc.selectFirst("a");
        assertNotNull("Element <a> should be present", a);
        Element b = a.selectFirst("b");
        assertNotNull("Element <b> should be present as child of <a>", b);
        // No stray elements should remain.
        assertEquals("No extra children under <a>", 1, a.childNodeSize());
    }

    /** Empty input should produce an empty document (no element children). */
    @Test
    public void testParseEmptyInput() {
        Document doc = builder.parse("", "");
        assertTrue("Document should have no element children", doc.childNodeSize() == 0);
    }

    /** Parsing a fragment should return the list of top‑level nodes only. */
    @Test
    public void testParseFragment() {
        String fragment = "<p>Para 1</p><p>Para 2</p><!-- comment -->";
        List<Node> nodes = builder.parseFragment(fragment, "", ParseErrorList.noTracking(), ParseSettings.preserveCase);
        assertEquals("Fragment should contain three top‑level nodes", 3, nodes.size());

        // Verify the two paragraph elements.
        assertTrue(nodes.get(0) instanceof Element);
        assertEquals("First element tag name", "p", ((Element) nodes.get(0)).tagName());
        assertEquals("First paragraph text", "Para 1", ((Element) nodes.get(0)).text());

        assertTrue(nodes.get(1) instanceof Element);
        assertEquals("Second element tag name", "p", ((Element) nodes.get(1)).tagName());
        assertEquals("Second paragraph text", "Para 2", ((Element) nodes.get(1)).text());

        // Verify the comment node.
        assertTrue(nodes.get(2) instanceof Comment);
        assertEquals("Comment data", " comment ", ((Comment) nodes.get(2)).getData());
    }

    /** Directly exercising the protected {@code initialiseParse} method via reflection
     *  to ensure the document is placed on the stack and output syntax set to XML. */
    @Test
    public void testInitialiseParseSetsXmlSyntax() throws Exception {
        // Use reflection to invoke the protected method.
        java.lang.reflect.Method initMethod = XmlTreeBuilder.class.getDeclaredMethod(
                "initialiseParse", Reader.class, String.class, ParseErrorList.class, ParseSettings.class);
        initMethod.setAccessible(true);
        initMethod.invoke(builder, new StringReader("<root/>"), "http://base/", ParseErrorList.noTracking(), ParseSettings.preserveCase);

        // After initialisation, the document's output syntax must be XML.
        assertEquals("Document output syntax should be XML after initialiseParse",
                Document.OutputSettings.Syntax.xml, builder.doc().outputSettings().syntax());
        // The stack should contain the document as its first element.
        java.lang.reflect.Field stackField = TreeBuilder.class.getDeclaredField("stack");
        stackField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.List<Element> stack = (java.util.List<Element>) stackField.get(builder);
        assertFalse("Stack should not be empty after initialiseParse", stack.isEmpty());
        assertEquals("First element on stack should be the document", builder.doc(), stack.get(0));
    }
}
```
###Test END##