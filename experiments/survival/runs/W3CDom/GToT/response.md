###Test START##
```java
/**
 * JUnit 4 test suite for {@link org.jsoup.helper.W3CDom}.
 *
 * The tests cover:
 * 1. Public API surface (fromJsoup, convert, asString).
 * 2. Normal usage – converting a simple HTML document and checking the resulting DOM.
 * 3. Edge‑cases such as null arguments, empty locations, namespace handling,
 *    attribute sanitisation and comment/DataNode handling.
 * 4. Exception handling – IllegalArgumentException for null input and
 *    IllegalStateException when the underlying transformer fails (simulated via reflection).
 *
 * The tests are written with plain JUnit 4 and jsoup (latest version) on the
 * class‑path.
 */
package org.jsoup.helper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.TransformerException;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.Assert.*;

public class W3CDomTest {

    private W3CDom w3cDom;

    @Before
    public void setUp() {
        w3cDom = new W3CDom();
    }

    @After
    public void tearDown() {
        w3cDom = null;
    }

    /* --------------------------------------------------------------------- */
    /* 1.  fromJsoup() – basic conversion & null handling                    */
    /* --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void fromJsoup_NullInput_Throws() {
        w3cDom.fromJsoup(null);
    }

    @Test
    public void fromJsoup_SimpleHtml_ProducesEquivalentW3cDocument() {
        String html = "<!DOCTYPE html>"
                + "<html>"
                + "  <head><title>Test Title</title></head>"
                + "  <body>"
                + "    <p id='p1'>Hello <b>world</b>!</p>"
                + "    <!-- a comment -->"
                + "    <script>var x = 1;</script>"
                + "  </body>"
                + "</html>";

        Document jsoupDoc = Jsoup.parse(html, "http://example.com/");
        org.w3c.dom.Document w3cDoc = w3cDom.fromJsoup(jsoupDoc);

        // document URI should be copied from the jsoup Document location
        assertEquals("http://example.com/", w3cDoc.getDocumentURI());

        // root element should be <html>
        assertEquals("html", w3cDoc.getDocumentElement().getTagName());

        // <head>/<title> text check
        Node titleNode = w3cDoc.getElementsByTagName("title").item(0);
        assertNotNull(titleNode);
        assertEquals("Test Title", titleNode.getTextContent());

        // <p id='p1'> content check
        Node pNode = w3cDoc.getElementsByTagName("p").item(0);
        assertNotNull(pNode);
        assertEquals("p1", ((org.w3c.dom.Element) pNode).getAttribute("id"));
        assertEquals("Hello world!", pNode.getTextContent().trim());

        // comment node check
        NodeList bodyChildren = w3cDoc.getElementsByTagName("body").item(0).getChildNodes();
        boolean commentFound = false;
        for (int i = 0; i < bodyChildren.getLength(); i++) {
            Node n = bodyChildren.item(i);
            if (n.getNodeType() == Node.COMMENT_NODE) {
                commentFound = true;
                assertEquals(" a comment ", n.getNodeValue());
            }
        }
        assertTrue("Comment node should be present in the W3C DOM", commentFound);
    }

    /* --------------------------------------------------------------------- */
    /* 2.  convert() – using a pre‑created W3C Document & location edge case */
    /* --------------------------------------------------------------------- */

    @Test
    public void convert_WithEmptyLocation_DoesNotSetDocumentURI() throws Exception {
        String html = "<html><body><p>Hi</p></body></html>";
        Document jsoupDoc = Jsoup.parse(html);               // no base URI supplied => location is blank
        org.w3c.dom.Document w3cDoc = createEmptyW3cDocument();

        w3cDom.convert(jsoupDoc, w3cDoc);
        // When location is blank the document URI must stay null (or empty string)
        assertTrue(w3cDoc.getDocumentURI() == null || w3cDoc.getDocumentURI().isEmpty());
        // Verify conversion still happened
        assertEquals("html", w3cDoc.getDocumentElement().getTagName());
        assertEquals("p", w3cDoc.getElementsByTagName("p").item(0).getNodeName());
    }

    @Test
    public void convert_WithLocation_SetsDocumentURI() throws Exception {
        String html = "<html><body></body></html>";
        Document jsoupDoc = Jsoup.parse(html, "https://my.site/page.html");
        org.w3c.dom.Document w3cDoc = createEmptyW3cDocument();

        w3cDom.convert(jsoupDoc, w3cDoc);
        assertEquals("https://my.site/page.html", w3cDoc.getDocumentURI());
    }

    /* --------------------------------------------------------------------- */
    /* 3.  Namespace handling – ensure xmlns attributes are honoured          */
    /* --------------------------------------------------------------------- */

    @Test
    public void convert_WithXmlnsAttribute_CreatesNamespacedElement() throws Exception {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\">"
                + "<rect width=\"100\" height=\"100\" style=\"fill:rgb(0,0,255);\"/>"
                + "</svg>";
        Document jsoupDoc = Jsoup.parse(svg, "", org.jsoup.parser.Parser.xmlParser());
        org.w3c.dom.Document w3cDoc = w3cDom.fromJsoup(jsoupDoc);

        org.w3c.dom.Element svgEl = w3cDoc.getDocumentElement();
        assertEquals("svg", svgEl.getTagName());
        assertEquals("http://www.w3.org/2000/svg", svgEl.getNamespaceURI());

        org.w3c.dom.Element rect = (org.w3c.dom.Element) svgEl.getElementsByTagName("rect").item(0);
        assertNotNull(rect);
        assertEquals("http://www.w3.org/2000/svg", rect.getNamespaceURI());
    }

    /* --------------------------------------------------------------------- */
    /* 4.  Attribute sanitisation – illegal XML attribute names are filtered */
    /* --------------------------------------------------------------------- */

    @Test
    public void convert_AttributesWithIllegalNames_AreSanitisedOrIgnored() throws Exception {
        String html = "<div bad@attr='value' good='yes' 1invalid='no'></div>";
        Document jsoupDoc = Jsoup.parse(html);
        org.w3c.dom.Document w3cDoc = w3cDom.fromJsoup(jsoupDoc);
        org.w3c.dom.Element div = w3cDoc.getElementsByTagName("div").item(0);

        // "bad@attr" becomes "badattr" after sanitisation and should be present
        assertEquals("value", div.getAttribute("badattr"));

        // "good" is a legal name and should be present unchanged
        assertEquals("yes", div.getAttribute("good"));

        // "1invalid" starts with a digit – after sanitisation it becomes "1invalid"
        // which does NOT match the XML name pattern, therefore it must be omitted.
        assertFalse(div.hasAttribute("1invalid"));
    }

    /* --------------------------------------------------------------------- */
    /* 5.  asString() – normal serialization                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void asString_SerialisesDocumentToString() throws Exception {
        String html = "<html><head><title>XML</title></head><body><p>Text</p></body></html>";
        Document jsoupDoc = Jsoup.parse(html);
        org.w3c.dom.Document w3cDoc = w3cDom.fromJsoup(jsoupDoc);

        String result = w3cDom.asString(w3cDoc);
        assertNotNull(result);
        // The result should contain the root tags and the text content
        assertTrue(result.contains("<html"));
        assertTrue(result.contains("<title>XML</title>"));
        assertTrue(result.contains("<p>Text</p>"));
    }

    /* --------------------------------------------------------------------- */
    /* 6.  asString() – transformer failure simulation                        */
    /* --------------------------------------------------------------------- */

    @Test(expected = IllegalStateException.class)
    public void asString_TransformerThrows_ConvertedToIllegalStateException() throws Exception {
        // Create a normal W3C Document first
        org.w3c.dom.Document doc = createEmptyW3cDocument();
        doc.appendChild(doc.createElement("root"));

        // Use reflection to replace the internal TransformerFactory with a stub that always throws
        TransformerFactoryStub stubFactory = new TransformerFactoryStub();
        setPrivateStaticField(TransformerFactory.class, "newInstance", stubFactory);

        // The call should now throw IllegalStateException
        w3cDom.asString(doc);
    }

    /* --------------------------------------------------------------------- */
    /* Helper methods                                                       */
    /* --------------------------------------------------------------------- */

    /** Creates a brand‑new empty W3C Document using the default builder. */
    private org.w3c.dom.Document createEmptyW3cDocument() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().newDocument();
    }

    /**
     * Utility to replace a private static field via reflection.
     *
     * @param clazz   The class containing the static field.
     * @param name    The field name.
     * @param value   The new value to inject.
     */
    private static void setPrivateStaticField(Class<?> clazz, String name, Object value) throws Exception {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    /**
     * Stub TransformerFactory that returns a Transformer which throws a
     * {@link TransformerException} on any call to {@code transform}.
     */
    public static class TransformerFactoryStub extends javax.xml.transform.TransformerFactory {
        @Override
        public javax.xml.transform.Transformer newTransformer() throws TransformerConfigurationException {
            return new javax.xml.transform.Transformer() {
                @Override
                public void transform(DOMSource source, StreamResult result) throws TransformerException {
                    throw new TransformerException("Simulated failure");
                }

                // The remaining abstract methods are no‑ops for the stub
                @Override public void setParameter(String name, Object value) {}
                @Override public Object getParameter(String name) { return null; }
                @Override public void clearParameters() {}
                @Override public void setOutputProperties(java.util.Properties oformat) {}
                @Override public java.util.Properties getOutputProperties() { return null; }
                @Override public void setErrorListener(javax.xml.transform.ErrorListener listener) {}
                @Override public javax.xml.transform.ErrorListener getErrorListener() { return null; }
            };
        }

        @Override public javax.xml.transform.Templates newTemplates(javax.xml.transform.Source source) { return null; }
        @Override public javax.xml.transform.Source getAssociatedStylesheet(javax.xml.transform.Source source, java.lang.String media, java.lang.String title, java.lang.String charset) { return null; }
        @Override public void setURIResolver(javax.xml.transform.URIResolver resolver) {}
        @Override public javax.xml.transform.URIResolver getURIResolver() { return null; }
        @Override public void setFeature(java.lang.String name, boolean value) {}
        @Override public boolean getFeature(java.lang.String name) { return false; }
    }
}
```
###Test END##