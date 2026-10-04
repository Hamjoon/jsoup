###Test START##
```java
package org.jsoup.helper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.parser.Parser;
import org.junit.Test;
import org.junit.Assert;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * JUnit‑4 test suite for {@link W3CDom}.
 * <p>
 * The suite covers:
 * <ul>
 *   <li>Public API surface (signatures listed in comments).</li>
 *   <li>Basic happy‑path functionality.</li>
 *   <li>Edge cases such as null arguments, namespace handling,
 *       attribute sanitisation and comment conversion.</li>
 *   <li>Exception handling – verifying that the wrapper methods
 *       re‑throw the expected unchecked exceptions.</li>
 * </ul>
 */
public class W3CDomTest {

    /* --------------------------------------------------------------
     *  Public methods of W3CDom (extracted)
     * --------------------------------------------------------------
     *
     * public Document fromJsoup(org.jsoup.nodes.Document in)
     * public void    convert(org.jsoup.nodes.Document in, Document out)
     * public String  asString(Document doc)
     *
     * -------------------------------------------------------------- */

    /**
     * Basic conversion test – a simple HTML document is turned into a
     * W3C DOM document and the root element name is verified.
     */
    @Test
    public void testFromJsoupBasic() {
        // given – a tiny HTML document
        org.jsoup.nodes.Document jsoupDoc = Jsoup.parse("<html><body><p>Hello</p></body></html>");

        // when
        W3CDom converter = new W3CDom();
        Document w3cDoc = converter.fromJsoup(jsoupDoc);

        // then
        Assert.assertNotNull("Resulting W3C document must not be null", w3cDoc);
        Element root = w3cDoc.getDocumentElement();
        Assert.assertEquals("Root element must be <html>", "html", root.getTagName());

        // verify that the <p> element exists and contains the expected text
        Node pNode = root.getElementsByTagName("p").item(0);
        Assert.assertNotNull("Paragraph element should be present", pNode);
        Assert.assertTrue("Paragraph node must be an Element", pNode instanceof Element);
        Assert.assertEquals("Paragraph text must be transferred",
                "Hello", ((Element) pNode).getTextContent());
    }

    /**
     * {@code fromJsoup(null)} must fail fast – the internal Validate
     * utility throws an {@link IllegalArgumentException}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromJsoupNullInput() {
        new W3CDom().fromJsoup(null);
    }

    /**
     * Verify that {@code convert(..)} correctly populates an *existing*
     * empty W3C document.
     */
    @Test
    public void testConvertBasic() throws ParserConfigurationException {
        // given – a jsoup document
        org.jsoup.nodes.Document jsoupDoc = Jsoup.parse("<div id='root'><span>text</span></div>");

        // create an empty W3C document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document w3cDoc = factory.newDocumentBuilder().newDocument();

        // when
        new W3CDom().convert(jsoupDoc, w3cDoc);

        // then
        Element root = w3cDoc.getDocumentElement();
        Assert.assertNotNull("Root element should be created", root);
        Assert.assertEquals("Root tag name must match jsoup source", "div", root.getTagName());
        Assert.assertEquals("Attribute from jsoup must be copied", "root", root.getAttribute("id"));

        // child <span> must exist
        Node span = root.getElementsByTagName("span").item(0);
        Assert.assertNotNull("Span element must be present", span);
        Assert.assertEquals("Span text must be transferred", "text", span.getTextContent());
    }

    /**
     * Namespace handling – an element that declares a default namespace
     * must be created with that namespace URI on the W3C side.
     */
    @Test
    public void testConvertNamespace() throws Exception {
        // XML with a default namespace
        String xml = "<root xmlns=\"http://example.com/ns\"><child>cdata</child></root>";
        org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(xml, "", Parser.xmlParser());

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document w3cDoc = factory.newDocumentBuilder().newDocument();

        new W3CDom().convert(jsoupDoc, w3cDoc);

        Element root = w3cDoc.getDocumentElement();
        Assert.assertEquals("Namespace URI must be preserved",
                "http://example.com/ns", root.getNamespaceURI());

        Element child = (Element) root.getElementsByTagNameNS("http://example.com/ns", "child").item(0);
        Assert.assertNotNull("Child element must be found in the same namespace", child);
        Assert.assertEquals("Child text must be transferred", "cdata", child.getTextContent());
    }

    /**
     * Attribute sanitisation – illegal characters are stripped from the
     * attribute name before it is written to the W3C document.
     */
    @Test
    public void testConvertAttributeSanitisation() throws Exception {
        // element with an illegal attribute name "data!bad"
        String html = "<div data!bad=\"value\" normal=\"ok\"></div>";
        org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(html);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document w3cDoc = factory.newDocumentBuilder().newDocument();

        new W3CDom().convert(jsoupDoc, w3cDoc);

        Element div = w3cDoc.getDocumentElement();
        Assert.assertNotNull("Div element must exist", div);

        // The illegal character '!' should be removed – resulting name "databad"
        Assert.assertEquals("Sanitised attribute must be present", "value", div.getAttribute("databad"));

        // The legal attribute must be unchanged
        Assert.assertEquals("Legal attribute must be unchanged", "ok", div.getAttribute("normal"));
    }

    /**
     * Comment nodes are converted into {@link org.w3c.dom.Comment} objects.
     */
    @Test
    public void testConvertCommentNode() throws Exception {
        String html = "<!DOCTYPE html><!-- This is a comment --><p>para</p>";
        org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(html);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document w3cDoc = factory.newDocumentBuilder().newDocument();

        new W3CDom().convert(jsoupDoc, w3cDoc);

        // The comment should be the first child of the root element
        Element root = w3cDoc.getDocumentElement();
        Node firstChild = root.getFirstChild();

        // Skip possible whitespace text nodes that Jsoup may have produced
        while (firstChild != null && firstChild.getNodeType() == Node.TEXT_NODE
                && ((Text) firstChild).getWholeText().trim().isEmpty()) {
            firstChild = firstChild.getNextSibling();
        }

        Assert.assertTrue("First non‑whitespace child should be a Comment",
                firstChild instanceof Comment);
        Assert.assertEquals("Comment data must be preserved",
                " This is a comment ", ((Comment) firstChild).getData());
    }

    /**
     * A simple W3C document can be serialised back to a string via {@code asString}.
     */
    @Test
    public void testAsStringBasic() throws Exception {
        // Build a tiny W3C document manually
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document w3cDoc = factory.newDocumentBuilder().newDocument();

        Element el = w3cDoc.createElement("greeting");
        el.setAttribute("lang", "en");
        el.appendChild(w3cDoc.createTextNode("Hello"));
        w3cDoc.appendChild(el);

        String xml = new W3CDom().asString(w3cDoc);

        Assert.assertTrue("Resulting XML should contain the element name",
                xml.contains("<greeting"));
        Assert.assertTrue("Resulting XML should contain the attribute",
                xml.contains("lang=\"en\""));
        Assert.assertTrue("Resulting XML should contain the text node",
                xml.contains(">Hello<"));
    }

    /**
     * Passing {@code null} to {@code asString} should result in an
     * {@link IllegalStateException} (wrapped {@link NullPointerException}
     * from the Transformer).
     */
    @Test(expected = IllegalStateException.class)
    public void testAsStringNullDocument() {
        new W3CDom().asString(null);
    }

    /**
     * Verify that when the source jsoup document has no location,
     * {@code convert} does not set a document URI on the W3C document.
     */
    @Test
    public void testConvertNoLocation() throws Exception {
        org.jsoup.nodes.Document jsoupDoc = Jsoup.parse("<body><p>test</p></body>");
        // ensure location is blank
        Assert.assertTrue("Location should be blank for a parsed string",
                StringUtil.isBlank(jsoupDoc.location()));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document w3cDoc = factory.newDocumentBuilder().newDocument();

        new W3CDom().convert(jsoupDoc, w3cDoc);

        Assert.assertNull("Document URI must remain null when source location is blank",
                w3cDoc.getDocumentURI());
    }
}
```
###Test END##