package org.jsoup.parser;

import static org.junit.Assert.*;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.ParseError;
import org.jsoup.parser.ParseErrorList;
import org.jsoup.parser.Parser;
import org.jsoup.parser.TreeBuilder;
import org.jsoup.parser.HtmlTreeBuilder;
import org.jsoup.parser.XmlTreeBuilder;
import org.jsoup.parser.ParseSettings;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link Parser}.
 *
 * <p>The suite validates:
 * <ul>
 *   <li>instance getters / setters</li>
 *   <li>error‑tracking behaviour</li>
 *   <li>static convenience parsing methods</li>
 *   <li>entity un‑escaping</li>
 *   <li>the two factory methods (htmlParser / xmlParser)</li>
 *   <li>that {@code parseBodyFragmentRelaxed} is just an alias for {@code parse}</li>
 * </ul>
 *
 * <p>Where possible we rely on the real Jsoup implementation (e.g. {@link HtmlTreeBuilder},
 * {@link XmlTreeBuilder}) because they are part of the public API and provide deterministic
 * results for the small fragments used in the tests.  For the instance‑methods that require
 * a {@link TreeBuilder} we also use a minimal stub implementation to verify that the
 * parser forwards the correct arguments.
 */
public class ParserTest {

    private Parser parser;
    private StubTreeBuilder stubBuilder;

    /** A very small {@link TreeBuilder} implementation used only for the instance‑method tests. */
    private static class StubTreeBuilder extends TreeBuilder {
        Document lastDocument;
        Reader   lastReader;
        String   lastBaseUri;
        ParseErrorList lastErrorList;
        ParseSettings   lastSettings;

        @Override
        public Document parse(Reader inputHtml, String baseUri,
                              ParseErrorList errorList, ParseSettings settings) {
            // Record the arguments so the test can assert on them
            this.lastReader = inputHtml;
            this.lastBaseUri = baseUri;
            this.lastErrorList = errorList;
            this.lastSettings = settings;

            // Return a tiny document – the content does not matter for the unit test
            Document doc = new Document(baseUri);
            doc.appendElement("stub");
            this.lastDocument = doc;
            return doc;
        }

        @Override
        public ParseSettings defaultSettings() {
            // Return a brand‑new (empty) settings object – sufficient for the tests
            return new ParseSettings();
        }
    }

    @Before
    public void setUp() {
        stubBuilder = new StubTreeBuilder();
        parser = new Parser(stubBuilder);
    }

    /** --------------------------------------------------------------
     *  Instance‑method tests
     *  -------------------------------------------------------------- */

    @Test
    public void getAndSetTreeBuilder() {
        // initial builder is the stub we passed in the constructor
        assertSame(stubBuilder, parser.getTreeBuilder());

        // replace with a real HtmlTreeBuilder and verify chaining
        HtmlTreeBuilder htmlBuilder = new HtmlTreeBuilder();
        Parser returned = parser.setTreeBuilder(htmlBuilder);
        assertSame(parser, returned);                 // method returns this
        assertSame(htmlBuilder, parser.getTreeBuilder());
    }

    @Test
    public void trackErrorsFlagAndErrorList() {
        // default: tracking disabled
        assertFalse(parser.isTrackErrors());
        assertNull(parser.getErrors());

        // enable tracking for up to 2 errors
        parser.setTrackErrors(2);
        assertTrue(parser.isTrackErrors());

        // parse a tiny fragment that will not generate errors – we only care that a
        // non‑null error list is attached to the parser
        Document doc = parser.parseInput("<p>test</p>", "http://example/");
        assertNotNull("Error list should be non‑null after parsing with tracking enabled", parser.getErrors());
        assertTrue("Error list should be an instance of ParseErrorList",
                parser.getErrors() instanceof ParseErrorList);

        // the stub builder should have received the same error list instance
        assertSame(parser.getErrors(), stubBuilder.lastErrorList);
        // the document returned by the stub builder must be the one we got back
        assertSame(stubBuilder.lastDocument, doc);
    }

    @Test
    public void setTrackErrorsDisablesTracking() {
        parser.setTrackErrors(0);
        assertFalse(parser.isTrackErrors());
        parser.parseInput("<div></div>", "http://example/");
        // when tracking is disabled the parser stores a "no‑tracking" list (which is still non‑null)
        assertNotNull(parser.getErrors());
        // but the list should be empty because no errors were recorded
        assertEquals(0, parser.getErrors().size());
    }

    @Test
    public void settingsGetterAndSetter() {
        ParseSettings custom = new ParseSettings();
        custom.normaliseTag("custom");   // make a tiny change so we can recognise it
        parser.settings(custom);
        assertSame(custom, parser.settings());
    }

    @Test
    public void parseInputWithString() {
        String html = "<span id='s'>hello</span>";
        Document doc = parser.parseInput(html, "http://base/");
        // verify that the stub builder received the correct arguments
        assertEquals("http://base/", stubBuilder.lastBaseUri);
        assertNotNull(stubBuilder.lastReader);
        // the stub builder always returns a document with a single <stub> element
        assertEquals("stub", doc.body().child(0).tagName());
    }

    @Test
    public void parseInputWithReader() {
        Reader reader = new StringReader("<b>bold</b>");
        Document doc = parser.parseInput(reader, "http://base/");
        assertSame(reader, stubBuilder.lastReader);
        assertEquals("http://base/", stubBuilder.lastBaseUri);
        assertEquals("stub", doc.body().child(0).tagName());
    }

    /** --------------------------------------------------------------
     *  Static parsing convenience methods – using real Jsoup parsers
     *  -------------------------------------------------------------- */

    @Test
    public void staticParseCreatesDocument() {
        Document doc = Parser.parse("<title>My Title</title><p>Hello</p>", "http://example/");
        assertEquals("http://example/", doc.baseUri());
        // Title should be in the head element
        assertEquals("My Title", doc.title());
        // Body should contain the <p>
        assertEquals("p", doc.body().child(0).tagName());
        assertEquals("Hello", doc.body().text());
    }

    @Test
    public void parseFragmentWithoutContext() {
        List<Node> nodes = Parser.parseFragment("<a href='x'>link</a>", null, "http://base/");
        assertEquals(1, nodes.size());
        Node node = nodes.get(0);
        assertTrue(node instanceof Element);
        Element el = (Element) node;
        assertEquals("a", el.tagName());
        assertEquals("link", el.text());
        assertEquals("x", el.attr("href"));
    }

    @Test
    public void parseFragmentWithContextPreservesContext() {
        Document doc = Document.createShell("http://base/");
        Element ctx = doc.body();               // use body as the parsing context
        List<Node> nodes = Parser.parseFragment("<div>inside</div>", ctx, "http://base/");
        // The returned list must contain a div element
        assertEquals(1, nodes.size());
        assertEquals("div", ((Element) nodes.get(0)).tagName());
        // The original context element must NOT be modified by the parse call
        assertEquals(0, ctx.childNodeSize());
    }

    @Test
    public void parseFragmentWithCustomErrorList() {
        ParseErrorList errors = ParseErrorList.tracking(5);
        List<Node> nodes = Parser.parseFragment("<p>test</p>", null, "http://base/", errors);
        // No errors expected for a well‑formed fragment
        assertEquals(0, errors.size());
        assertEquals(1, nodes.size());
        assertEquals("p", ((Element) nodes.get(0)).tagName());
    }

    @Test
    public void parseXmlFragmentProducesXmlNodes() {
        List<Node> nodes = Parser.parseXmlFragment("<root><child/></root>", "http://base/");
        // XML parsing returns the root element as the first node
        assertEquals(1, nodes.size());
        Element root = (Element) nodes.get(0);
        assertEquals("root", root.tagName());
        assertEquals(1, root.childrenSize());
        assertEquals("child", root.child(0).tagName());
    }

    @Test
    public void parseBodyFragmentWrapsIntoDocumentBody() {
        Document doc = Parser.parseBodyFragment("<section><h1>Header</h1></section>", "http://base/");
        Element body = doc.body();
        // The body should now contain a single <section> element
        assertEquals(1, body.childrenSize());
        Element section = body.child(0);
        assertEquals("section", section.tagName());
        assertEquals("Header", section.selectFirst("h1").text());
    }

    @Test
    public void unescapeEntitiesSimple() {
        String escaped = "A &amp; B &lt; C &gt; D &quot;E&quot;";
        String unescaped = Parser.unescapeEntities(escaped, false);
        assertEquals("A & B < C > D \"E\"", unescaped);
    }

    @Test
    public void unescapeEntitiesInAttributeMode() {
        // In attribute mode the parser also decodes &apos; and leaves whitespace as‑is
        String escaped = "val=&quot;test&apos;s&quot;";
        String unescaped = Parser.unescapeEntities(escaped, true);
        assertEquals("val=\"test's\"", unescaped);
    }

    @Test
    public void parseBodyFragmentRelaxedIsAliasForParse() {
        String html = "<p>Relaxed</p>";
        Document viaRelaxed = Parser.parseBodyFragmentRelaxed(html, "http://base/");
        Document viaParse = Parser.parse(html, "http://base/");
        // The two documents should be structurally identical
        assertEquals(viaParse.title(), viaRelaxed.title());
        assertEquals(viaParse.body().html(), viaRelaxed.body().html());
    }

    @Test
    public void htmlParserFactoryCreatesHtmlTreeBuilder() {
        Parser htmlParser = Parser.htmlParser();
        assertTrue(htmlParser.getTreeBuilder() instanceof HtmlTreeBuilder);
        // Verify that the parser is functional by parsing a tiny fragment
        Document doc = htmlParser.parseInput("<div>ok</div>", "http://base/");
        assertEquals("div", doc.body().child(0).tagName());
    }

    @Test
    public void xmlParserFactoryCreatesXmlTreeBuilder() {
        Parser xmlParser = Parser.xmlParser();
        assertTrue(xmlParser.getTreeBuilder() instanceof XmlTreeBuilder);
        // Verify that the parser parses XML correctly
        Document doc = xmlParser.parseInput("<root><item/></root>", "http://base/");
        assertEquals("root", doc.child(0).tagName());
        assertEquals(1, doc.select("item").size());
    }
}
