###Test START##
```java
package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Parser;
import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link XmlTreeBuilder}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public parse methods (Reader and String)</li>
 *   <li>Fragment parsing</li>
 *   <li>Handling of self‑closing tags, comments, XML declarations and doctypes</li>
 *   <li>Edge cases such as empty input, null arguments and mismatched tags</li>
 * </ul>
 *
 * The test class resides in the same package as {@code XmlTreeBuilder}
 * so that package‑private methods can be accessed if needed.
 */
public class XmlTreeBuilderTest {

    /** Helper to create a fresh XmlTreeBuilder for each test. */
    private XmlTreeBuilder newBuilder() {
        return new XmlTreeBuilder();
    }

    /* --------------------------------------------------------------------- *
     *  Basic parsing – element with text
     * --------------------------------------------------------------------- */
    @Test
    public void parseSimpleElement() {
        XmlTreeBuilder builder = newBuilder();
        Document doc = builder.parse("<root>Hello World</root>", "");

        Element root = doc.selectFirst("root");
        assertNotNull("Root element should be present", root);
        assertEquals("Root tag name", "root", root.tagName());
        assertEquals("Root text content", "Hello World", root.text());
    }

    /* --------------------------------------------------------------------- *
     *  Empty input – should produce an empty Document (no child elements)
     * --------------------------------------------------------------------- */
    @Test
    public void parseEmptyString() {
        XmlTreeBuilder builder = newBuilder();
        Document doc = builder.parse("", "");

        // The document should have no element children.
        assertTrue("Document should have no child elements", doc.children().isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  Self‑closing tag – <br/> should be an element with no children and
     *  must not remain on the internal stack.
     * --------------------------------------------------------------------- */
    @Test
    public void parseSelfClosingTag() {
        XmlTreeBuilder builder = newBuilder();
        Document doc = builder.parse("<br/>", "");

        Element br = doc.selectFirst("br");
        assertNotNull("Self‑closing <br> element should exist", br);
        assertTrue("Self‑closing <br> should have no children", br.childNodeSize() == 0);
    }

    /* --------------------------------------------------------------------- *
     *  Comment handling – regular comment and XML declaration comment.
     * --------------------------------------------------------------------- */
    @Test
    public void parseCommentsAndXmlDeclaration() {
        XmlTreeBuilder builder = newBuilder();
        String xml = "<!-- regular comment -->\n<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root/>";

        Document doc = builder.parse(xml, "");

        // Regular comment should be a Comment node.
        List<Node> rootChildren = doc.childNodes();
        assertEquals("Document should have three top‑level nodes", 3, rootChildren.size());

        Node first = rootChildren.get(0);
        assertTrue("First node should be a Comment", first instanceof Comment);
        assertEquals("Comment data", " regular comment ", ((Comment) first).getData());

        // XML declaration should be parsed as an XmlDeclaration node.
        Node second = rootChildren.get(1);
        assertTrue("Second node should be an XmlDeclaration", second instanceof XmlDeclaration);
        XmlDeclaration decl = (XmlDeclaration) second;
        assertTrue("Declaration should be a processing instruction (not a DOCTYPE)", decl.isProcessingInstruction());
        assertEquals("Version attribute", "1.0", decl.attr("version"));
        assertEquals("Encoding attribute", "UTF-8", decl.attr("encoding"));
    }

    /* --------------------------------------------------------------------- *
     *  Doctype handling – ensure a DocumentType node is created with the
     *  correct name and identifiers.
     * --------------------------------------------------------------------- */
    @Test
    public void parseDoctype() {
        XmlTreeBuilder builder = newBuilder();
        String xml = "<!DOCTYPE note PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n<note/>";

        Document doc = builder.parse(xml, "");

        List<Node> nodes = doc.childNodes();
        assertFalse("Document should contain at least one child node", nodes.isEmpty());

        Node first = nodes.get(0);
        assertTrue("First node should be a DocumentType", first instanceof DocumentType);
        DocumentType dt = (DocumentType) first;

        assertEquals("Doctype name", "note", dt.name());
        assertEquals("Public identifier", "-//W3C//DTD XHTML 1.0 Transitional//EN", dt.publicId());
        assertEquals("System identifier", "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd", dt.systemId());
    }

    /* --------------------------------------------------------------------- *
     *  Mismatched end tag – parser should not throw an exception and should
     *  close the first matching element on the stack.
     * --------------------------------------------------------------------- */
    @Test
    public void parseMismatchedEndTag() {
        XmlTreeBuilder builder = newBuilder();
        String xml = "<a><b></a></b>";

        Document doc = builder.parse(xml, "");

        // After parsing, both <a> and <b> should be present as elements,
        // but the mismatched closing order should be handled gracefully.
        Element a = doc.selectFirst("a");
        Element b = doc.selectFirst("b");

        assertNotNull("Element <a> should exist", a);
        assertNotNull("Element <b> should exist", b);
        // <b> should be a child of <a> (because </a> closed <a> first)
        assertEquals("Parent of <b> should be <a>", a, b.parent());
    }

    /* --------------------------------------------------------------------- *
     *  parseFragment – parses a fragment and returns the list of top‑level nodes.
     * --------------------------------------------------------------------- */
    @Test
    public void parseFragmentReturnsChildNodes() {
        XmlTreeBuilder builder = newBuilder();
        ParseErrorList errors = ParseErrorList.noTracking();
        ParseSettings settings = ParseSettings.preserveCase;

        List<Node> nodes = builder.parseFragment("<p>Fragment</p><!-- comment -->", "", errors, settings);
        assertEquals("Fragment should produce exactly two nodes", 2, nodes.size());

        Node first = nodes.get(0);
        assertTrue("First node should be an Element <p>", first instanceof Element);
        assertEquals("Element tag name", "p", ((Element) first).tagName());
        assertEquals("Element text", "Fragment", ((Element) first).text());

        Node second = nodes.get(1);
        assertTrue("Second node should be a Comment", second instanceof Comment);
        assertEquals("Comment content", " comment ", ((Comment) second).getData());
    }

    /* --------------------------------------------------------------------- *
     *  Edge case – null Reader argument. Expect a NullPointerException.
     * --------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void parseNullReaderThrowsException() {
        XmlTreeBuilder builder = newBuilder();
        builder.parse((Reader) null, "");
    }

    /* --------------------------------------------------------------------- *
     *  Edge case – null String argument. Expect a NullPointerException.
     * --------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void parseNullStringThrowsException() {
        XmlTreeBuilder builder = newBuilder();
        builder.parse((String) null, "");
    }

    /* --------------------------------------------------------------------- *
     *  Internal token processing – verify that an unexpected token type
     *  triggers a Validate.fail (IllegalArgumentException).
     *  This uses reflection to invoke the protected {@code process} method.
     * --------------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void processUnexpectedTokenThrows() throws Exception {
        XmlTreeBuilder builder = newBuilder();

        // Create a dummy Token with an undefined type.
        Token token = new Token() {
            @Override
            public Token reset() { return this; }
        };
        // Use reflection to set a non‑existent type.
        java.lang.reflect.Field typeField = Token.class.getDeclaredField("type");
        typeField.setAccessible(true);
        // Assign a bogus enum value by casting an integer (will cause NPE in switch)
        typeField.set(token, null);

        java.lang.reflect.Method process = XmlTreeBuilder.class.getDeclaredMethod("process", Token.class);
        process.setAccessible(true);
        process.invoke(builder, token);
    }
}
```
###Test END##