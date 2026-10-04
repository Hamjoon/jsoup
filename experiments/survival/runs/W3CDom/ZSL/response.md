###Test START##
```java
package org.jsoup.helper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Comment;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link W3CDom}.
 *
 * The tests cover:
 * <ul>
 *   <li>Conversion of a simple Jsoup document (elements, text, comment, data node).</li>
 *   <li>Namespace handling – both default and prefixed namespaces.</li>
 *   <li>Attribute name sanitisation performed by {@code copyAttributes}.</li>
 *   <li>Document URI propagation via {@code fromJsoup}.</li>
 *   <li>Serialization via {@code asString}.</li>
 *   <li>Null‑argument validation.</li>
 * </ul>
 */
public class W3CDomTest {

    private W3CDom w3cDom;

    @Before
    public void setUp() {
        w3cDom = new W3CDom();
        // Ensure the factory is namespace‑aware – the production code does this,
        // but we set it explicitly here to avoid any test‑environment surprises.
        w3cDom.factory.setNamespaceAware(true);
    }

    /**
     * Helper that creates a tiny Jsoup document containing:
     * <ul>
     *   <li>an element with a comment child</li>
     *   <li>a text node</li>
     *   <li>a data node (which should become a text node in the W3C DOM)</li>
     * </ul>
     */
    private Document createSimpleJsoupDoc() {
        Document jsoupDoc = Jsoup.parse("", "http://example.com/base");
        Element root = jsoupDoc.appendElement("root");
        root.appendComment("a comment");
        root.appendText("some text");
        root.appendChild(new DataNode("raw data", ""));
        return jsoupDoc;
    }

    @Test
    public void testFromJsoup_NullArgument_ThrowsException() {
        try {
            w3cDom.fromJsoup(null);
            fail("Expected IllegalArgumentException (Validate.notNull) to be thrown");
        } catch (IllegalArgumentException | NullPointerException e) {
            // Expected – Validate.notNull throws IllegalArgumentException in recent jsoup versions.
        }
    }

    @Test
    public void testFromJsoup_PropagatesDocumentLocation() {
        Document jsoupDoc = Jsoup.parse("<html></html>", "https://my.test/location");
        org.w3c.dom.Document w3cDoc = w3cDom.fromJsoup(jsoupDoc);
        assertEquals("Document URI should be copied from Jsoup location", "https://my.test/location", w3cDoc.getDocumentURI());
    }

    @Test
    public void testSimpleConversion_IncludesAllNodeTypes() {
        Document jsoupDoc = createSimpleJsoupDoc();
        org.w3c.dom.Document w3cDoc = w3cDom.fromJsoup(jsoupDoc);

        // Root element
        org.w3c.dom.Element root = w3cDoc.getDocumentElement();
        assertNotNull("Root element should exist", root);
        assertEquals("root", root.getTagName());

        // Child nodes order: comment, text, text (data node converted to text)
        Node commentNode = root.getFirstChild();
        assertTrue("First child should be a comment", commentNode instanceof Comment);
        assertEquals("a comment", ((Comment) commentNode).getData());

        Node textNode1 = commentNode.getNextSibling();
        assertTrue("Second child should be a text node", textNode1 instanceof Text);
        assertEquals("some text", ((Text) textNode1).getWholeText());

        Node textNode2 = textNode1.getNextSibling();
        assertTrue("Third child (data node) should be a text node", textNode2 instanceof Text);
        assertEquals("raw data", ((Text) textNode2).getWholeText());

        // No extra nodes
        assertNull("No more children after the data node", textNode2.getNextSibling());
    }

    @Test
    public void testNamespaceHandling_PrefixedAndDefault() {
        // Build a Jsoup document with explicit namespace declarations.
        String html = ""
                + "<ns:root xmlns:ns=\"http://example.com/ns\" xmlns=\"http://default.org/default\">"
                + "   <ns:child attr=\"val\">Text</ns:child>"
                + "   <defaultChild/>"
                + "</ns:root>";
        Document jsoupDoc = Jsoup.parse(html, "", org.jsoup.parser.Parser.xmlParser());

        org.w3c.dom.Document w3cDoc = w3cDom.fromJsoup(jsoupDoc);
        org.w3c.dom.Element root = w3cDoc.getDocumentElement();

        // Verify root namespace (prefixed)
        assertEquals("http://example.com/ns", root.getNamespaceURI());
        assertEquals("ns:root", root.getTagName());

        // Verify prefixed child
        org.w3c.dom.Element child = (org.w3c.dom.Element) root.getElementsByTagNameNS("http://example.com/ns", "child").item(0);
        assertNotNull("Prefixed child element should be found", child);
        assertEquals("http://example.com/ns", child.getNamespaceURI());
        assertEquals("ns:child", child.getTagName());
        assertEquals("val", child.getAttribute("attr"));
        assertEquals("Text", child.getTextContent());

        // Verify default‑namespace child
        org.w3c.dom.Element defaultChild = (org.w3c.dom.Element) root.getElementsByTagNameNS("http://default.org/default", "defaultChild").item(0);
        assertNotNull("Default‑namespace child should be found", defaultChild);
        assertEquals("http://default.org/default", defaultChild.getNamespaceURI());
        assertEquals("defaultChild", defaultChild.getTagName());
    }

    @Test
    public void testAttributeSanitisation_RemovesInvalidChars() {
        // Attribute names with illegal characters: space, '@', etc.
        Element el = new Element("test");
        el.attributes().put("data-@bad", "value1"); // '@' should be stripped
        el.attributes().put("good_attr", "value2");
        el.attributes().put("invalid space", "value3"); // space removed

        Document jsoupDoc = new Document("");
        jsoupDoc.appendChild(el);

        org.w3c.dom.Document w3cDoc = w3cDom.fromJsoup(jsoupDoc);
        org.w3c.dom.Element w3cEl = w3cDoc.getDocumentElement();

        // The illegal '@' should be removed, leaving "data-bad"
        assertEquals("value1", w3cEl.getAttribute("data-bad"));
        // The space should be removed, leaving "invalidspace"
        assertEquals("value3", w3cEl.getAttribute("invalidspace"));
        // Valid attribute stays unchanged
        assertEquals("value2", w3cEl.getAttribute("good_attr"));
    }

    @Test
    public void testAsString_ProducesParsableXML() {
        Document jsoupDoc = createSimpleJsoupDoc();
        org.w3c.dom.Document w3cDoc = w3cDom.fromJsoup(jsoupDoc);
        String xml = w3cDom.asString(w3cDoc);

        // The output should contain the XML declaration (optional) and the root element.
        assertTrue("Serialized XML should contain the root element", xml.contains("<root"));
        assertTrue("Serialized XML should contain the comment", xml.contains("<!--a comment-->"));
        assertTrue("Serialized XML should contain the text node", xml.contains("some text"));
        assertTrue("Serialized XML should contain the data node text", xml.contains("raw data"));
    }

    @Test
    public void testConvert_WithPreconfiguredW3cDocument() throws Exception {
        // Create a blank W3C document and set a custom doctype or other properties before conversion.
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        org.w3c.dom.Document preconfigured = factory.newDocumentBuilder().newDocument();
        preconfigured.appendChild(preconfigured.createProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"style.xsl\""));

        // Simple Jsoup doc
        Document jsoupDoc = Jsoup.parse("<p>Hello</p>", "", org.jsoup.parser.Parser.xmlParser());

        // Use the convert method (does not set document URI because location is blank)
        w3cDom.convert(jsoupDoc, preconfigured);

        // Verify that the processing instruction is still present and that the element was added.
        Node pi = preconfigured.getFirstChild();
        assertNotNull("Processing instruction should remain", pi);
        assertEquals(Node.PROCESSING_INSTRUCTION_NODE, pi.getNodeType());

        org.w3c.dom.Element p = (org.w3c.dom.Element) preconfigured.getElementsByTagName("p").item(0);
        assertNotNull("Converted <p> element should be present", p);
        assertEquals("Hello", p.getTextContent());
    }
}
```
###Test END##