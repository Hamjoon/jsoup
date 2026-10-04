package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.junit.Before;
import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Parser}.
 *
 * The test class follows the requested steps:
 *
 * 1. **Public methods extracted** (see the comment block at the top of the file).
 * 2. **Basic test cases** for each public method.
 * 3. **Edge‑case / exception tests** that cover null arguments, empty inputs,
 *    abnormal maxError values, and behaviour of the static factory methods.
 * 4. All tests are merged into a single file that starts with
 *    <code>###Test START##</code> and ends with <code>###Test END##</code>.
 */
public class ParserTest {

    /* --------------------------------------------------------------------- *
     * 1. List of public methods (signatures) in {@link Parser}
     * --------------------------------------------------------------------- *
     *
     * Instance methods
     * -----------------
     * public Parser(TreeBuilder treeBuilder)
     * public Document parseInput(String html, String baseUri)
     * public Document parseInput(Reader inputHtml, String baseUri)
     * public TreeBuilder getTreeBuilder()
     * public Parser setTreeBuilder(TreeBuilder treeBuilder)
     * public boolean isTrackErrors()
     * public Parser setTrackErrors(int maxErrors)
     * public List<ParseError> getErrors()
     * public Parser settings(ParseSettings settings)
     * public ParseSettings settings()
     *
     * Static utility methods
     * ----------------------
     * public static Document parse(String html, String baseUri)
     * public static List<Node> parseFragment(String fragmentHtml, Element context, String baseUri)
     * public static List<Node> parseFragment(String fragmentHtml, Element context, String baseUri, ParseErrorList errorList)
     * public static List<Node> parseXmlFragment(String fragmentXml, String baseUri)
     * public static Document parseBodyFragment(String bodyHtml, String baseUri)
     * public static String unescapeEntities(String string, boolean inAttribute)
     * public static Document parseBodyFragmentRelaxed(String bodyHtml, String baseUri)   // deprecated
     * public static Parser htmlParser()
     * public static Parser xmlParser()
     */

    private Parser htmlParser;
    private Parser xmlParser;

    @Before
    public void setUp() {
        htmlParser = Parser.htmlParser();
        xmlParser = Parser.xmlParser();
    }

    /* --------------------------------------------------------------------- *
     * 2. Basic test cases for each public method
     * --------------------------------------------------------------------- */

    // ---- Constructor & getters/setters -------------------------------------------------
    @Test
    public void testConstructorAndTreeBuilderAccessors() {
        TreeBuilder tb = new HtmlTreeBuilder();
        Parser parser = new Parser(tb);
        assertSame("getTreeBuilder should return the same instance passed to the constructor",
                tb, parser.getTreeBuilder());

        // setTreeBuilder should replace the builder and return the same parser instance
        TreeBuilder newTb = new XmlTreeBuilder();
        Parser returned = parser.setTreeBuilder(newTb);
        assertSame("setTreeBuilder must return the same parser instance for chaining", parser, returned);
        assertSame("TreeBuilder should be updated after setTreeBuilder", newTb, parser.getTreeBuilder());
    }

    // ---- parseInput(String, String) ----------------------------------------------------
    @Test
    public void testParseInputFromString() {
        Document doc = htmlParser.parseInput("<p>Hello World</p>", "http://example.com/");
        assertNotNull("parseInput should never return null", doc);
        Element p = doc.selectFirst("p");
        assertNotNull("Parsed document must contain a <p> element", p);
        assertEquals("Element text should match input", "Hello World", p.text());
    }

    // ---- parseInput(Reader, String) ----------------------------------------------------
    @Test
    public void testParseInputFromReader() {
        Reader reader = new StringReader("<div>Reader Test</div>");
        Document doc = htmlParser.parseInput(reader, "http://example.com/");
        Element div = doc.selectFirst("div");
        assertNotNull(div);
        assertEquals("Reader Test", div.text());
    }

    // ---- isTrackErrors / setTrackErrors / getErrors ------------------------------------
    @Test
    public void testErrorTrackingToggle() {
        // default: tracking disabled
        assertFalse("By default tracking should be disabled", htmlParser.isTrackErrors());

        // enable tracking with a positive maxErrors value
        htmlParser.setTrackErrors(3);
        assertTrue("Tracking should be enabled when maxErrors > 0", htmlParser.isTrackErrors());

        // perform a parse that generates no errors – we just want to verify that the list is non‑null
        htmlParser.parseInput("<html></html>", "http://example.com/");
        List<ParseError> errors = htmlParser.getErrors();
        assertNotNull("Error list must be non‑null after a parse when tracking is enabled", errors);
        // No errors expected for the simple HTML used above
        assertTrue("Error list should be empty for well‑formed HTML", errors.isEmpty());

        // disable tracking again
        htmlParser.setTrackErrors(0);
        assertFalse("Tracking should be disabled when maxErrors == 0", htmlParser.isTrackErrors());
    }

    // ---- settings(ParseSettings) / settings() --------------------------------------------
    @Test
    public void testSettingsAccessor() {
        ParseSettings custom = ParseSettings.htmlDefault; // using an existing constant for simplicity
        Parser parser = Parser.htmlParser().settings(custom);
        assertSame("settings() should return the instance supplied to settings(ParseSettings)",
                custom, parser.settings());
    }

    // ---- Static parse(String, String) ----------------------------------------------------
    @Test
    public void testStaticParse() {
        Document doc = Parser.parse("<title>Static Title</title>", "http://example.com/");
        assertEquals("Static parse should correctly set the document title", "Static Title", doc.title());
    }

    // ---- Static parseFragment(String, Element, String) -----------------------------------
    @Test
    public void testStaticParseFragmentWithContext() {
        Document shell = Document.createShell("http://example.com/");
        Element context = shell.body(); // use body as the parsing context
        List<Node> nodes = Parser.parseFragment("<span>Fragment</span>", context, "http://example.com/");
        assertEquals("One node should be returned for a single element fragment", 1, nodes.size());
        assertTrue("Returned node should be an Element", nodes.get(0) instanceof Element);
        assertEquals("span", ((Element) nodes.get(0)).tagName());
        assertEquals("Fragment", ((Element) nodes.get(0)).text());
    }

    // ---- Static parseFragment(String, Element, String, ParseErrorList) -------------------
    @Test
    public void testStaticParseFragmentWithErrorList() {
        ParseErrorList errorList = ParseErrorList.tracking(5);
        List<Node> nodes = Parser.parseFragment("<invalid>", null, "http://example.com/", errorList);
        // The fragment is not valid HTML, but the parser still returns a node list (may be empty)
        assertNotNull("Node list must not be null even if errors occur", nodes);
        // Ensure that errors are being collected
        assertTrue("Error list should contain at least one error for malformed HTML",
                errorList.size() > 0);
    }

    // ---- Static parseXmlFragment(String, String) -----------------------------------------
    @Test
    public void testStaticParseXmlFragment() {
        List<Node> nodes = Parser.parseXmlFragment("<root><child/></root>", "http://example.com/");
        assertEquals(1, nodes.size());
        assertTrue(nodes.get(0) instanceof Element);
        assertEquals("root", ((Element) nodes.get(0)).tagName());
    }

    // ---- Static parseBodyFragment(String, String) ----------------------------------------
    @Test
    public void testParseBodyFragment() {
        Document doc = Parser.parseBodyFragment("<section><h1>Header</h1></section>", "http://example.com/");
        Element body = doc.body();
        Element section = body.selectFirst("section");
        assertNotNull(section);
        assertEquals("Header", section.selectFirst("h1").text());
    }

    // ---- Static unescapeEntities(String, boolean) -----------------------------------------
    @Test
    public void testUnescapeEntities() {
        String escaped = "&lt;div class=&quot;test&quot;&gt;Hello &amp; Bye&lt;/div&gt;";
        String unescaped = Parser.unescapeEntities(escaped, false);
        assertEquals("<div class=\"test\">Hello & Bye</div>", unescaped);
    }

    // ---- Deprecated parseBodyFragmentRelaxed(String, String) -------------------------------
    @Test
    public void testParseBodyFragmentRelaxedDelegatesToParse() {
        Document doc1 = Parser.parseBodyFragmentRelaxed("<p>Relaxed</p>", "http://example.com/");
        Document doc2 = Parser.parse("<p>Relaxed</p>", "http://example.com/");
        // Both methods should produce equivalent documents (the relaxed version simply calls parse)
        assertEquals(doc2.body().html(), doc1.body().html());
    }

    // ---- Static htmlParser() and xmlParser() ----------------------------------------------
    @Test
    public void testHtmlParserFactory() {
        Parser parser = Parser.htmlParser();
        assertTrue("htmlParser should create a parser backed by HtmlTreeBuilder",
                parser.getTreeBuilder() instanceof HtmlTreeBuilder);
    }

    @Test
    public void testXmlParserFactory() {
        Parser parser = Parser.xmlParser();
        assertTrue("xmlParser should create a parser backed by XmlTreeBuilder",
                parser.getTreeBuilder() instanceof XmlTreeBuilder);
    }

    /* --------------------------------------------------------------------- *
     * 3. Edge‑case / exception tests
     * --------------------------------------------------------------------- */

    // parseInput(String, String) with null html
    @Test(expected = NullPointerException.class)
    public void testParseInputString_NullHtmlThrows() {
        htmlParser.parseInput((String) null, "http://example.com/");
    }

    // parseInput(Reader, String) with null Reader
    @Test(expected = NullPointerException.class)
    public void testParseInputReader_NullReaderThrows() {
        htmlParser.parseInput((Reader) null, "http://example.com/");
    }

    // parseFragment with null fragmentHtml
    @Test(expected = NullPointerException.class)
    public void testParseFragment_NullHtmlThrows() {
        Parser.parseFragment(null, null, "http://example.com/");
    }

    // parseFragment with null context (allowed) – should still return a node list
    @Test
    public void testParseFragment_NullContext() {
        List<Node> nodes = Parser.parseFragment("<p>No Context</p>", null, "http://example.com/");
        assertEquals(1, nodes.size());
        assertEquals("p", ((Element) nodes.get(0)).tagName());
    }

    // unescapeEntities with null string – should throw NPE (Tokeniser expects non‑null)
    @Test(expected = NullPointerException.class)
    public void testUnescapeEntities_NullStringThrows() {
        Parser.unescapeEntities(null, false);
    }

    // setTrackErrors with a negative number – should disable tracking (isTrackErrors == false)
    @Test
    public void testSetTrackErrors_NegativeDisablesTracking() {
        htmlParser.setTrackErrors(-5);
        assertFalse("Negative maxErrors must result in tracking being disabled", htmlParser.isTrackErrors());
    }

    // getErrors when tracking is disabled – should return an empty, non‑null list (no‑tracking list)
    @Test
    public void testGetErrorsWhenTrackingDisabled() {
        htmlParser.setTrackErrors(0); // ensure disabled
        htmlParser.parseInput("<html></html>", "http://example.com/");
        List<ParseError> errors = htmlParser.getErrors();
        assertNotNull("Even when tracking is disabled, getErrors must return a non‑null list", errors);
        assertTrue("The list should be empty when tracking is disabled", errors.isEmpty());
    }

    // Verify that calling setTreeBuilder with the same instance returns the parser itself (fluent API)
    @Test
    public void testSetTreeBuilderFluentReturn() {
        TreeBuilder tb = new HtmlTreeBuilder();
        Parser parser = new Parser(tb);
        Parser returned = parser.setTreeBuilder(tb);
        assertSame("Fluent API: setTreeBuilder must return the same Parser instance", parser, returned);
    }

    // Verify that parseBodyFragment correctly handles an empty fragment (should produce an empty body)
    @Test
    public void testParseBodyFragment_EmptyInput() {
        Document doc = Parser.parseBodyFragment("", "http://example.com/");
        assertTrue("Body should be empty when parsing an empty fragment", doc.body().children().isEmpty());
    }
}
