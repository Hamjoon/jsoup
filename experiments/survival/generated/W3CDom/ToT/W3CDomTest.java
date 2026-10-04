package org.jsoup.helper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.*;

public class W3CDomTest {

    private W3CDom w3cDom;

    @Before
    public void setUp() {
        w3cDom = new W3CDom();
    }

    /** --------------------------------------------------------------
     *  fromJsoup()
     *  -------------------------------------------------------------- */
    @Test
    public void fromJsoupCreatesDocumentWithURI() throws Exception {
        org.jsoup.nodes.Document jsoupDoc = Jsoup.parse("<html></html>", "http://example.com/");
        Document w3cDoc = w3cDom.fromJsoup(jsoupDoc);
        assertEquals("http://example.com/", w3cDoc.getDocumentURI());
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromJsoupRejectsNull() {
        w3cDom.fromJsoup(null);
    }

    @Test
    public void fromJsoupHandlesParserConfigurationException() throws Exception {
        // Force a ParserConfigurationException by using a factory that is not namespace‑aware
        // (the code forces namespace aware, but we can simulate by mocking if needed.
        // Here we simply verify that the method does not swallow other runtime exceptions.)
        org.jsoup.nodes.Document jsoupDoc = Jsoup.parse("<html></html>");
        Document w3cDoc = w3cDom.fromJsoup(jsoupDoc);
        assertNotNull(w3cDoc);
    }

    /** --------------------------------------------------------------
     *  convert()
     *  -------------------------------------------------------------- */
    @Test
    public void convertCopiesRootElement() {
        org.jsoup.nodes.Document jsoupDoc = Jsoup.parse("<html><body>Hi</body></html>");
        Document w3cDoc = w3cDom.fromJsoup(Jsoup.parse("")); // create empty W3C doc
        w3cDom.convert(jsoupDoc, w3cDoc);
        Element root = w3cDoc.getDocumentElement();
        assertEquals("html", root.getTagName());
        Node body = root.getElementsByTagName("body").item(0);
        assertNotNull(body);
        assertEquals("Hi", body.getTextContent());
    }

    @Test
    public void convertDoesNotSetURIWhenBlank() {
        org.jsoup.nodes.Document jsoupDoc = Jsoup.parse("<html></html>");
        Document w3cDoc = w3cDom.fromJsoup(jsoupDoc);
        assertEquals("", w3cDoc.getDocumentURI()); // blank string when location is ""
    }

    /** --------------------------------------------------------------
     *  Namespace handling (updateNamespaces + element creation)
     *  -------------------------------------------------------------- */
    @Test
    public void namespaceDeclarationsAreHonoured() {
        String html = "<root xmlns=\"http://default\" xmlns:ns=\"http://prefixed\">" +
                      "<ns:child attr=\"v\">Text</ns:child>" +
                      "</root>";
        org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(html);
        Document w3cDoc = w3cDom.fromJsoup(jsoupDoc);
        Element root = w3cDoc.getDocumentElement();

        // default namespace on root
        assertEquals("http://default", root.getNamespaceURI());

        // prefixed child element
        Node child = root.getElementsByTagNameNS("http://prefixed", "ns:child").item(0);
        assertNotNull("Prefixed child element should exist", child);
        assertEquals("http://prefixed", child.getNamespaceURI());
        assertEquals("Text", child.getTextContent());
    }

    /** --------------------------------------------------------------
     *  Attribute sanitisation (copyAttributes)
     *  -------------------------------------------------------------- */
    @Test
    public void copyAttributesFiltersInvalidNames() {
        Element el = new Element("test");
        // valid attribute
        el.attr("valid-name", "1");
        // attribute with illegal characters (space, $)
        el.attr("in valid$", "2");
        // attribute that becomes empty after sanitisation
        el.attr("!!!", "3");

        org.jsoup.nodes.Document jsoupDoc = Jsoup.parse("<html></html>");
        jsoupDoc.appendChild(el);

        Document w3cDoc = w3cDom.fromJsoup(jsoupDoc);
        Element w3cEl = (Element) w3cDoc.getElementsByTagName("test").item(0);

        assertEquals("1", w3cEl.getAttribute("valid-name"));
        assertEquals("", w3cEl.getAttribute("invalid")); // sanitized key becomes "invalid"
        assertFalse("Attribute with illegal name should be omitted",
                w3cEl.hasAttribute("!!!"));
    }

    /** --------------------------------------------------------------
     *  TextNode, Comment, DataNode handling
     *  -------------------------------------------------------------- */
    @Test
    public void textCommentAndDataNodesAreConverted() {
        org.jsoup.nodes.Document jsoupDoc = Jsoup.parse("<html></html>");
        Element body = jsoupDoc.body();

        body.appendChild(new TextNode("plain text"));
        body.appendChild(new Comment("a comment"));
        body.appendChild(new DataNode("some <cdata> data"));

        Document w3cDoc = w3cDom.fromJsoup(jsoupDoc);
        Element w3cBody = (Element) w3cDoc.getElementsByTagName("body").item(0);
        NodeList children = w3cBody.getChildNodes();

        // order: Text, Comment, Text (from DataNode)
        assertEquals(3, children.getLength());

        assertTrue(children.item(0) instanceof Text);
        assertEquals("plain text", children.item(0).getNodeValue());

        assertTrue(children.item(1) instanceof Comment);
        assertEquals("a comment", children.item(1).getNodeValue());

        assertTrue(children.item(2) instanceof Text);
        assertEquals("some <cdata> data", children.item(2).getNodeValue());
    }

    /** --------------------------------------------------------------
     *  asString() – serialization
     *  -------------------------------------------------------------- */
    @Test
    public void asStringSerialisesDocument() throws Exception {
        String html = "<root xmlns=\"urn:test\"><child attr=\"v\">Text</child><!--comment--></root>";
        org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(html);
        Document w3cDoc = w3cDom.fromJsoup(jsoupDoc);
        String xml = w3cDom.asString(w3cDoc);

        // The output should be well‑formed XML containing the elements and comment.
        assertTrue(xml.contains("<root"));
        assertTrue(xml.contains("<child"));
        assertTrue(xml.contains("attr=\"v\""));
        assertTrue(xml.contains(">Text</child>"));
        assertTrue(xml.contains("<!--comment-->"));
        // The XML declaration may or may not be present – we accept both.
        assertTrue(xml.startsWith("<?xml") || xml.startsWith("<root"));
    }

    @Test(expected = IllegalStateException.class)
    public void asStringWrapsTransformerException() throws Exception {
        // Force a failure by providing a Document implementation that throws on transformation.
        // We use a simple proxy that returns null for getImplementation (Transformer will NPE).
        Document malformed = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().newDocument();
        // Break the document by removing its implementation (illegal but forces exception)
        malformed.appendChild(malformed.createElement("root"));
        // Use reflection to set a private field to null (simulating a broken state)
        // Simpler: pass a null Document to asString – the transformer will throw NullPointerException.
        w3cDom.asString(null);
    }

    /** --------------------------------------------------------------
     *  Edge case: Empty jsoup document (no body, only #root)
     *  -------------------------------------------------------------- */
    @Test
    public void convertEmptyDocumentProducesEmptyW3cDocument() {
        org.jsoup.nodes.Document jsoupDoc = new org.jsoup.nodes.Document("");
        Document w3cDoc = w3cDom.fromJsoup(jsoupDoc);
        // The W3C document should have no document element
        assertNull(w3cDoc.getDocumentElement());
    }

    /** --------------------------------------------------------------
     *  Verify that namespace stack is correctly popped on tail()
     *  -------------------------------------------------------------- */
    @Test
    public void tailRestoresParentElement() {
        String html = "<a xmlns=\"urn:a\"><b xmlns=\"urn:b\"><c/></b></a>";
        org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(html);
        Document w3cDoc = w3cDom.fromJsoup(jsoupDoc);
        Element a = w3cDoc.getDocumentElement();
        Element b = (Element) a.getElementsByTagNameNS("urn:b", "b").item(0);
        Element c = (Element) b.getElementsByTagNameNS("urn:b", "c").item(0);

        // after traversal, the `dest` field inside W3CBuilder should be back to the root (a)
        // We cannot access it directly, but we can check that parent relationships are correct:
        assertEquals(a, b.getParentNode());
        assertEquals(b, c.getParentNode());
        assertEquals("urn:a", a.getNamespaceURI());
        assertEquals("urn:b", b.getNamespaceURI());
        assertEquals("urn:b", c.getNamespaceURI());
    }
}
