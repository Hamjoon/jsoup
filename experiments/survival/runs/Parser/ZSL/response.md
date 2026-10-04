###Test START##
```java
package org.jsoup.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.ParseError;
import org.jsoup.parser.ParseErrorList;
import org.jsoup.parser.ParseSettings;
import org.jsoup.parser.Tokeniser;
import org.jsoup.parser.TreeBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Parser}.
 *
 * The tests cover:
 * <ul>
 *   <li>Instance methods (get/set TreeBuilder, error tracking, settings)</li>
 *   <li>Static convenience methods (parse, parseFragment, parseXmlFragment, parseBodyFragment, …)</li>
 *   <li>Factory methods (htmlParser, xmlParser)</li>
 *   <li>Behaviour of {@code unescapeEntities}</li>
 * </ul>
 *
 * Real Jsoup objects are used where the library already provides a deterministic implementation
 * (e.g. {@code Document}, {@code Element}, {@code Node}).  For the parts that depend on the
 * {@link TreeBuilder} hierarchy we employ Mockito mocks so that we can capture the arguments
 * passed by {@link Parser} and assert that they are correct.
 */
@RunWith(MockitoJUnitRunner.class)
public class ParserTest {

    // ----------------------------------------------------------------------
    // Mocks for the TreeBuilder‑related tests
    // ----------------------------------------------------------------------
    @Mock
    private TreeBuilder mockTreeBuilder;

    @Mock
    private Document mockDocument;

    @Captor
    private ArgumentCaptor<ParseErrorList> errorListCaptor;

    private static final String BASE_URI = "http://example.com/";
    private static final String SIMPLE_HTML = "<p>Hello <b>world</b></p>";
    private static final String SIMPLE_XML = "<root><item>1</item></root>";

    private Parser parser;

    @Before
    public void setUp() {
        // The defaultSettings() call is made in the constructor, so we stub it first.
        when(mockTreeBuilder.defaultSettings()).thenReturn(new ParseSettings());

        // Build the parser with the mocked TreeBuilder.
        parser = new Parser(mockTreeBuilder);
    }

    // ----------------------------------------------------------------------
    // Instance method tests
    // ----------------------------------------------------------------------
    @Test
    public void testGetAndSetTreeBuilder() {
        // verify constructor set the builder
        assertSame(mockTreeBuilder, parser.getTreeBuilder());

        // replace with a new mock and verify chaining
        TreeBuilder anotherBuilder = mock(TreeBuilder.class);
        when(anotherBuilder.defaultSettings()).thenReturn(new ParseSettings());

        Parser returned = parser.setTreeBuilder(anotherBuilder);
        assertSame(parser, returned);
        assertSame(anotherBuilder, parser.getTreeBuilder());
    }

    @Test
    public void testTrackErrorsFlag() {
        assertFalse("By default tracking is disabled", parser.isTrackErrors());

        parser.setTrackErrors(5);
        assertTrue("Tracking should be enabled when maxErrors > 0", parser.isTrackErrors());

        parser.setTrackErrors(0);
        assertFalse("Tracking should be disabled when maxErrors == 0", parser.isTrackErrors());
    }

    @Test
    public void testSetTrackErrorsChaining() {
        Parser returned = parser.setTrackErrors(3);
        assertSame(parser, returned);
        assertTrue(parser.isTrackErrors());
    }

    @Test
    public void testParseInputString_WithErrorTracking() {
        // Enable tracking for this parse
        parser.setTrackErrors(2);

        // Stub the parse method to return our mock document
        when(mockTreeBuilder.parse(any(Reader.class), eq(BASE_URI), any(ParseErrorList.class), any(ParseSettings.class)))
                .thenReturn(mockDocument);

        Document result = parser.parseInput(SIMPLE_HTML, BASE_URI);
        assertSame("Parser should return the Document supplied by TreeBuilder", mockDocument, result);

        // Capture the ParseErrorList that was handed to the TreeBuilder
        verify(mockTreeBuilder).parse(any(Reader.class), eq(BASE_URI), errorListCaptor.capture(), any(ParseSettings.class));
        ParseErrorList capturedErrorList = errorListCaptor.getValue();

        // Because we set maxErrors to 2, the captured list must be a tracking list.
        assertTrue("Error list should be tracking when maxErrors > 0", capturedErrorList.isTracking());

        // The parser should expose the same list via getErrors()
        assertSame("getErrors() must return the same instance that was passed to the builder",
                capturedErrorList, parser.getErrors());
    }

    @Test
    public void testParseInputReader_WithoutErrorTracking() {
        // Ensure tracking is disabled
        parser.setTrackErrors(0);
        assertFalse(parser.isTrackErrors());

        // Stub parse to return the mock document
        when(mockTreeBuilder.parse(any(Reader.class), eq(BASE_URI), any(ParseErrorList.class), any(ParseSettings.class)))
                .thenReturn(mockDocument);

        Reader reader = new StringReader(SIMPLE_HTML);
        Document result = parser.parseInput(reader, BASE_URI);
        assertSame(mockDocument, result);

        // Capture the error list passed to the builder
        verify(mockTreeBuilder).parse(any(Reader.class), eq(BASE_URI), errorListCaptor.capture(), any(ParseSettings.class));
        ParseErrorList captured = errorListCaptor.getValue();

        // No tracking -> list should be non‑tracking
        assertFalse("Error list should NOT be tracking when maxErrors == 0", captured.isTracking());

        // getErrors must expose that list
        assertSame(captured, parser.getErrors());
    }

    @Test
    public void testSettingsGetterAndSetter() {
        ParseSettings custom = new ParseSettings(); // default ctor is fine for test
        parser.settings(custom);
        assertSame(custom, parser.settings());
    }

    // ----------------------------------------------------------------------
    // Static convenience methods – real Jsoup parsing is used
    // ----------------------------------------------------------------------
    @Test
    public void testStaticParseCreatesDocument() {
        Document doc = Parser.parse("<div id='test'>Content</div>", BASE_URI);
        assertNotNull(doc);
        Element div = doc.getElementById("test");
        assertNotNull("Parsed document should contain the div with id='test'", div);
        assertEquals("Content", div.text());
    }

    @Test
    public void testParseFragmentWithoutContext() {
        List<Node> nodes = Parser.parseFragment("<span>hello</span>", null, BASE_URI);
        assertNotNull(nodes);
        assertEquals(1, nodes.size());
        assertTrue(nodes.get(0) instanceof Element);
        assertEquals("span", ((Element) nodes.get(0)).tagName());
        assertEquals("hello", ((Element) nodes.get(0)).text());
    }

    @Test
    public void testParseFragmentWithContext() {
        Document doc = Document.createShell(BASE_URI);
        Element context = doc.body(); // using body as the context element
        List<Node> nodes = Parser.parseFragment("<p>para</p>", context, BASE_URI);
        assertEquals(1, nodes.size());
        assertTrue(nodes.get(0) instanceof Element);
        assertEquals("p", ((Element) nodes.get(0)).tagName());
    }

    @Test
    public void testParseFragmentWithErrorList() {
        ParseErrorList errorList = ParseErrorList.tracking(5);
        List<Node> nodes = Parser.parseFragment("<b>bold</b>", null, BASE_URI, errorList);
        assertEquals(1, nodes.size());
        assertEquals("b", ((Element) nodes.get(0)).tagName());
        // The provided error list should be the same instance used internally
        assertSame(errorList, nodes.get(0).ownerDocument().parser().getErrors());
    }

    @Test
    public void testParseXmlFragment() {
        List<Node> nodes = Parser.parseXmlFragment(SIMPLE_XML, BASE_URI);
        assertEquals(1, nodes.size());
        Node root = nodes.get(0);
        assertTrue(root instanceof Element);
        assertEquals("root", ((Element) root).tagName());
        // Verify that the child element is present
        List<Element> children = ((Element) root).children();
        assertEquals(1, children.size());
        assertEquals("item", children.get(0).tagName());
        assertEquals("1", children.get(0).text());
    }

    @Test
    public void testParseBodyFragment() {
        String bodyHtml = "<div class='box'>Box content</div>";
        Document doc = Parser.parseBodyFragment(bodyHtml, BASE_URI);
        assertNotNull(doc);
        Element body = doc.body();
        assertNotNull(body);
        List<Element> children = body.children();
        assertEquals(1, children.size());
        Element div = children.get(0);
        assertEquals("div", div.tagName());
        assertEquals("box", div.className());
        assertEquals("Box content", div.text());
    }

    @Test
    public void testUnescapeEntities() {
        String escaped = "Tom &amp; Jerry &lt;3";
        String unescaped = Parser.unescapeEntities(escaped, false);
        assertEquals("Tom & Jerry <3", unescaped);
    }

    @Test
    public void testParseBodyFragmentRelaxedDelegatesToParse() {
        String html = "<h1>Title</h1><p>Paragraph</p>";
        Document viaRelaxed = Parser.parseBodyFragmentRelaxed(html, BASE_URI);
        Document viaParse = Parser.parse(html, BASE_URI);
        // The two documents should be structurally identical
        assertEquals(viaParse.title(), viaRelaxed.title());
        assertEquals(viaParse.body().html(), viaRelaxed.body().html());
    }

    // ----------------------------------------------------------------------
    // Factory methods
    // ----------------------------------------------------------------------
    @Test
    public void testHtmlParserFactory() {
        Parser htmlParser = Parser.htmlParser();
        assertNotNull(htmlParser);
        assertTrue("htmlParser() must return a Parser backed by HtmlTreeBuilder",
                htmlParser.getTreeBuilder() instanceof HtmlTreeBuilder);
    }

    @Test
    public void testXmlParserFactory() {
        Parser xmlParser = Parser.xmlParser();
        assertNotNull(xmlParser);
        assertTrue("xmlParser() must return a Parser backed by XmlTreeBuilder",
                xmlParser.getTreeBuilder() instanceof XmlTreeBuilder);
    }
}
```
###Test END##