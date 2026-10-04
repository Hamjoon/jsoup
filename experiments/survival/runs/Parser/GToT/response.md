###Test START##
```java
package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.parser.TreeBuilder;
import org.jsoup.parser.HtmlTreeBuilder;
import org.jsoup.parser.XmlTreeBuilder;
import org.jsoup.parser.ParseSettings;
import org.jsoup.parser.ParseError;
import org.jsoup.parser.ParseErrorList;
import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link org.jsoup.parser.Parser}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>All public instance methods (including getters/setters and error‑tracking logic)</li>
 *   <li>All public static convenience methods</li>
 *   <li>Typical use‑cases, edge‑cases (null / empty inputs, large error limits, etc.)</li>
 *   <li>Exception handling where the contract of the method implies a {@link NullPointerException}</li>
 * </ul>
 *
 * <p>These tests assume that the real jsoup implementation is on the test class‑path.
 */
public class ParserTest {

    /* --------------------------------------------------------------------- *
     *  Instance method tests
     * --------------------------------------------------------------------- */

    @Test
    public void testParseInput_String_SimpleHtml() {
        Parser parser = Parser.htmlParser();
        Document doc = parser.parseInput("<html><head><title>Test</title></head><body>Hello</body></html>", "http://example.com");
        assertNotNull(doc);
        assertEquals("Test", doc.title());
        assertEquals("Hello", doc.body().text());
    }

    @Test
    public void testParseInput_Reader_SimpleHtml() {
        Parser parser = Parser.htmlParser();
        Reader reader = new StringReader("<p>Reader test</p>");
        Document doc = parser.parseInput(reader, "http://example.com");
        assertNotNull(doc);
        assertEquals("Reader test", doc.body().text());
    }

    @Test(expected = NullPointerException.class)
    public void testParseInput_String_NullHtml() {
        Parser parser = Parser.htmlParser();
        parser.parseInput((String) null, "http://example.com");
    }

    @Test(expected = NullPointerException.class)
    public void testParseInput_Reader_NullReader() {
        Parser parser = Parser.htmlParser();
        parser.parseInput((Reader) null, "http://example.com");
    }

    @Test
    public void testGetSetTreeBuilder() {
        Parser parser = Parser.htmlParser();
        TreeBuilder original = parser.getTreeBuilder();
        assertTrue(original instanceof HtmlTreeBuilder);

        TreeBuilder newBuilder = new XmlTreeBuilder();
        parser.setTreeBuilder(newBuilder);
        assertSame(newBuilder, parser.getTreeBuilder());
    }

    @Test
    public void testSetTreeBuilder_Chaining() {
        Parser parser = Parser.htmlParser();
        TreeBuilder newBuilder = new XmlTreeBuilder();
        Parser returned = parser.setTreeBuilder(newBuilder);
        assertSame(parser, returned);
        assertSame(newBuilder, parser.getTreeBuilder());
    }

    @Test
    public void testTrackErrors_EnableAndRetrieve() {
        Parser parser = Parser.htmlParser();
        parser.setTrackErrors(3);               // enable tracking, max 3 errors
        assertTrue(parser.isTrackErrors());

        // Intentionally feed malformed HTML to generate errors
        Document doc = parser.parseInput("<!doctype html><html><head><title>Bad</title></head><body><p>Unclosed", "http://example.com");
        assertNotNull(doc);
        List<ParseError> errors = parser.getErrors();
        assertNotNull(errors);
        // The exact number of errors is implementation dependent; we only assert the limit is respected
        assertTrue(errors.size() <= 3);
    }

    @Test
    public void testTrackErrors_Disable() {
        Parser parser = Parser.htmlParser();
        parser.setTrackErrors(0);               // disable tracking
        assertFalse(parser.isTrackErrors());

        Document doc = parser.parseInput("<html></html>", "http://example.com");
        assertNotNull(doc);
        List<ParseError> errors = parser.getErrors();
        // When tracking disabled, jsoup returns an empty, non‑null list
        assertNotNull(errors);
        assertEquals(0, errors.size());
    }

    @Test
    public void testGetErrors_NoParsePerformed() {
        Parser parser = Parser.htmlParser();
        // No parse yet – errors should be null (as the parser never created a list)
        assertNull(parser.getErrors());
    }

    @Test
    public void testSettings_GetSet() {
        Parser parser = Parser.htmlParser();
        ParseSettings custom = new ParseSettings(true, false);
        parser.settings(custom);
        assertSame(custom, parser.settings());
    }

    @Test
    public void testSettings_Chaining() {
        Parser parser = Parser.htmlParser();
        ParseSettings custom = new ParseSettings(false, true);
        Parser returned = parser.settings(custom);
        assertSame(parser, returned);
        assertSame(custom, parser.settings());
    }

    /* --------------------------------------------------------------------- *
     *  Static convenience method tests
     * --------------------------------------------------------------------- */

    @Test
    public void testStaticParse() {
        Document doc = Parser.parse("<title>Static</title>", "http://example.com");
        assertNotNull(doc);
        // The static parse always creates a full document with <html> etc.
        assertEquals("Static", doc.title());
        assertEquals("http://example.com", doc.baseUri());
    }

    @Test(expected = NullPointerException.class)
    public void testStaticParse_NullHtml() {
        Parser.parse(null, "http://example.com");
    }

    @Test
    public void testParseFragment_WithContext() {
        Document contextDoc = Jsoup.parse("<div id='ctx'><p>ctx</p></div>", "http://example.com");
        Element context = contextDoc.getElementById("ctx");
        List<Node> nodes = Parser.parseFragment("<span>frag</span>", context, "http://example.com");
        assertNotNull(nodes);
        assertEquals(1, nodes.size());
        assertTrue(nodes.get(0) instanceof Element);
        assertEquals("span", ((Element) nodes.get(0)).tagName());
        // Ensure context element is unchanged
        assertEquals("ctx", context.text());
    }

    @Test
    public void testParseFragment_NullContext() {
        List<Node> nodes = Parser.parseFragment("<b>noContext</b>", null, "http://example.com");
        assertNotNull(nodes);
        assertEquals(1, nodes.size());
        assertTrue(nodes.get(0) instanceof Element);
        assertEquals("b", ((Element) nodes.get(0)).tagName());
    }

    @Test
    public void testParseFragment_WithErrorList() {
        ParseErrorList errorList = ParseErrorList.tracking(5);
        List<Node> nodes = Parser.parseFragment("<p>ok</p><", null, "http://example.com", errorList);
        // The fragment is malformed; we expect at least one error
        assertTrue(errorList.size() > 0);
        assertEquals(1, nodes.size()); // only the well‑formed <p> node is returned
    }

    @Test
    public void testParseXmlFragment() {
        List<Node> nodes = Parser.parseXmlFragment("<root><child/></root>", "http://example.com");
        assertNotNull(nodes);
        assertEquals(1, nodes.size());
        assertTrue(nodes.get(0) instanceof Element);
        assertEquals("root", ((Element) nodes.get(0)).tagName());
    }

    @Test
    public void testParseBodyFragment() {
        Document doc = Parser.parseBodyFragment("<div class='a'>Hello</div><p>World</p>", "http://example.com");
        assertNotNull(doc);
        Element body = doc.body();
        // The body should contain exactly the two nodes we passed in
        assertEquals(2, body.children().size());
        assertEquals("div", body.child(0).tagName());
        assertEquals("a", body.child(0).className());
        assertEquals("p", body.child(1).tagName());
    }

    @Test
    public void testParseBodyFragment_EmptyHtml() {
        Document doc = Parser.parseBodyFragment("", "http://example.com");
        assertNotNull(doc);
        assertTrue(doc.body().children().isEmpty());
    }

    @Test
    public void testUnescapeEntities_Basic() {
        String escaped = "Tom &amp; Jerry &lt;3";
        String unescaped = Parser.unescapeEntities(escaped, false);
        assertEquals("Tom & Jerry <3", unescaped);
    }

    @Test
    public void testUnescapeEntities_InAttributeMode() {
        // In attribute mode the parser is stricter about semicolons
        String escaped = "a=1&amp;b=2";
        String unescaped = Parser.unescapeEntities(escaped, true);
        assertEquals("a=1&b=2", unescaped);
    }

    @Test
    public void testParseBodyFragmentRelaxed_DeprecatedBehaviour() {
        Document doc1 = Parser.parseBodyFragmentRelaxed("<p>relaxed</p>", "http://example.com");
        Document doc2 = Parser.parse("<p>relaxed</p>", "http://example.com");
        assertEquals(doc2.body().html(), doc1.body().html());
    }

    @Test
    public void testHtmlParserFactory() {
        Parser parser = Parser.htmlParser();
        assertNotNull(parser);
        assertTrue(parser.getTreeBuilder() instanceof HtmlTreeBuilder);
    }

    @Test
    public void testXmlParserFactory() {
        Parser parser = Parser.xmlParser();
        assertNotNull(parser);
        assertTrue(parser.getTreeBuilder() instanceof XmlTreeBuilder);
    }

    /* --------------------------------------------------------------------- *
     *  Additional edge‑case tests for robustness
     * --------------------------------------------------------------------- */

    @Test
    public void testSetTrackErrors_Chaining() {
        Parser parser = Parser.htmlParser();
        Parser returned = parser.setTrackErrors(2);
        assertSame(parser, returned);
        assertTrue(parser.isTrackErrors());
    }

    @Test
    public void testParseInput_EmptyString() {
        Parser parser = Parser.htmlParser();
        Document doc = parser.parseInput("", "http://example.com");
        assertNotNull(doc);
        // An empty string should result in a document with an empty body
        assertTrue(doc.body().children().isEmpty());
    }

    @Test
    public void testParseFragment_MultipleRootNodes() {
        List<Node> nodes = Parser.parseFragment("<li>One</li><li>Two</li>", null, "http://example.com");
        assertEquals(2, nodes.size());
        assertEquals("li", ((Element) nodes.get(0)).tagName());
        assertEquals("li", ((Element) nodes.get(1)).tagName());
    }
}
```
###Test END##