package org.jsoup.helper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.Test;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import static org.junit.Assert.*;

public class W3CDomTest {

    /**
     * Verify that a simple HTML document is converted correctly:
     * - root element name
     * - child element name
     * - text node content
     * - document URI is transferred when the jsoup document has a location.
     */
    @Test
    public void testSimpleConversion() {
        String html = "<html><body>Hello</body></html>";
        Document jsoupDoc = Jsoup.parse(html, "http://example.com", Parser.xmlParser());

        W3CDom converter = new W3CDom();
        org.w3c.dom.Document w3cDoc = converter.fromJsoup(jsoupDoc);

        // Verify document URI
        assertEquals("http://example.com", w3cDoc.getDocumentURI());

        // Verify root element
        Element root = w3cDoc.getDocumentElement();
        assertNotNull(root);
        assertEquals("html", root.getTagName());

        // Verify child element
        Node bodyNode = root.getFirstChild();
        while (bodyNode != null && bodyNode.getNodeType() != Node.ELEMENT_NODE) {
            bodyNode = bodyNode.getNextSibling();
        }
        assertNotNull(bodyNode);
        assertTrue(bodyNode instanceof Element);
        Element body = (Element) bodyNode;
        assertEquals("body", body.getTagName());

        // Verify text node inside <body>
        Node textNode = body.getFirstChild();
        assertNotNull(textNode);
        assertTrue(textNode instanceof Text);
        assertEquals("Hello", ((Text) textNode).getWholeText());
    }

    /**
     * Verify namespace handling:
     * - default namespace on the root element
     * - prefixed namespace on a child element
     * - namespace URI is attached to the created W3C Element
     */
    @Test
    public void testNamespaceConversion() {
        String xml = "<root xmlns=\"http://default\" xmlns:ns=\"http://ns\">" +
                     "<ns:child attr=\"val\">Text</ns:child>" +
                     "</root>";
        Document jsoupDoc = Jsoup.parse(xml, "", Parser.xmlParser());

        W3CDom converter = new W3CDom();
        org.w3c.dom.Document w3cDoc = converter.fromJsoup(jsoupDoc);

        // Root element should be in the default namespace
        Element root = w3cDoc.getDocumentElement();
        assertEquals("root", root.getTagName());
        assertEquals("http://default", root.getNamespaceURI());

        // Child element should be in the 'ns' namespace
        Node childNode = root.getFirstChild();
        while (childNode != null && childNode.getNodeType() != Node.ELEMENT_NODE) {
            childNode = childNode.getNextSibling();
        }
        assertNotNull(childNode);
        assertTrue(childNode instanceof Element);
        Element child = (Element) childNode;
        assertEquals("ns:child", child.getTagName()); // tagName keeps the prefix
        assertEquals("http://ns", child.getNamespaceURI());

        // Attribute should be copied unchanged
        assertEquals("val", child.getAttribute("attr"));
    }

    /**
     * Verify that attribute names are sanitised according to the rules in
     * {@code W3CBuilder.copyAttributes}:
     * - illegal characters are stripped
     * - the resulting name must match the XML name pattern to be kept
     */
    @Test
    public void testAttributeSanitisation() {
        String xml = "<elem data-!bad=\"value\" good=\"ok\" \"!@#\"=\"ignored\"/>";
        Document jsoupDoc = Jsoup.parse(xml, "", Parser.xmlParser());

        W3CDom converter = new W3CDom();
        org.w3c.dom.Document w3cDoc = converter.fromJsoup(jsoupDoc);

        Element elem = w3cDoc.getDocumentElement();
        assertEquals("elem", elem.getTagName());

        // \"data-!bad\" becomes \"databad\" after illegal characters are removed
        assertEquals("value", elem.getAttribute("databad"));

        // valid attribute stays unchanged
        assertEquals("ok", elem.getAttribute("good"));

        // attribute consisting solely of illegal characters becomes empty string → not set
        assertFalse(elem.hasAttribute(""));
    }

    /**
     * Verify that comment nodes and data nodes are converted to the appropriate W3C DOM types.
     */
    @Test
    public void testCommentAndDataNodeConversion() {
        // Build a jsoup document manually to include a comment and a data node
        Document jsoupDoc = Jsoup.parse("<root></root>", "", Parser.xmlParser());
        org.jsoup.nodes.Element root = jsoupDoc.selectFirst("root");
        // comment
        root.appendChild(new org.jsoup.nodes.Comment("this is a comment", ""));
        // data node (e.g., script contents)
        root.appendChild(new DataNode("some data", ""));
        // text node
        root.appendText("plain text");

        W3CDom converter = new W3CDom();
        org.w3c.dom.Document w3cDoc = converter.fromJsoup(jsoupDoc);
        Element w3cRoot = w3cDoc.getDocumentElement();

        // Traverse children and verify types
        Node child = w3cRoot.getFirstChild();
        boolean foundComment = false, foundData = false, foundText = false;

        while (child != null) {
            if (child.getNodeType() == Node.COMMENT_NODE) {
                foundComment = true;
                assertEquals("this is a comment", ((Comment) child).getData());
            } else if (child.getNodeType() == Node.TEXT_NODE) {
                String txt = ((Text) child).getWholeText();
                // The data node is turned into a text node, as is the plain text
                if (txt.equals("some data")) {
                    foundData = true;
                } else if (txt.equals("plain text")) {
                    foundText = true;
                }
            }
            child = child.getNextSibling();
        }

        assertTrue("Comment node should be present", foundComment);
        assertTrue("Data node should become a text node", foundData);
        assertTrue("Plain text node should be present", foundText);
    }

    /**
     * Verify that {@code asString()} produces a well‑formed XML string that contains
     * the expected elements and namespace declarations.
     */
    @Test
    public void testAsStringProducesValidXml() {
        String xml = "<ns:root xmlns:ns=\"http://example.org/ns\"><ns:child>value</ns:child></ns:root>";
        Document jsoupDoc = Jsoup.parse(xml, "", Parser.xmlParser());

        W3CDom converter = new W3CDom();
        org.w3c.dom.Document w3cDoc = converter.fromJsoup(jsoupDoc);
        String result = converter.asString(w3cDoc);

        // The result should contain the namespace declaration and the element hierarchy
        assertTrue(result.contains("xmlns:ns=\"http://example.org/ns\""));
        assertTrue(result.contains("<ns:root"));
        assertTrue(result.contains("<ns:child>value</ns:child>"));
    }

    /**
     * Verify that passing a {@code null} argument to {@code fromJsoup} results in an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testFromJsoupNullArgument() {
        new W3CDom().fromJsoup(null);
    }
}
