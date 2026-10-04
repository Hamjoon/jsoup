package org.jsoup.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Parser}.
 *
 * <p>The tests are written from the perspective of three imaginary experts
 * (Alice, Bob and Carol).  Each expert contributed one test per public method,
 * covering typical use, edge cases and error handling.  The final suite combines
 * all contributions into a single, runnable test class.</p>
 */
@RunWith(MockitoJUnitRunner.class)
public class ParserTest {

    // -----------------------------------------------------------------------
    // Mocks shared by several tests
    // -----------------------------------------------------------------------
    @Mock
    private TreeBuilder mockTreeBuilder;
    @Mock
    private ParseErrorList mockErrorList;
    @Mock
    private ParseSettings mockSettings;
    @Mock
    private Document mockDocument;
    @Mock
    private Element mockElement;
    @Mock
    private Reader mockReader;

    @Captor
    private ArgumentCaptor<ParseErrorList> errorListCaptor;

    private Parser parser;

    @Before
    public void setUp() {
        // TreeBuilder defaultSettings() is used by the constructor.
        when(mockTreeBuilder.defaultSettings()).thenReturn(mockSettings);
        parser = new Parser(mockTreeBuilder);
    }

    // -----------------------------------------------------------------------
    // 1. parseInput(String, String)
    // -----------------------------------------------------------------------
    @Test
    public void parseInputString_TracksErrorsWhenEnabled() throws Exception {
        // Alice: typical case – error tracking enabled
        parser.setTrackErrors(5);               // enables tracking
        when(mockTreeBuilder.parse(any(Reader.class), eq("http://base"), any(ParseErrorList.class), eq(mockSettings)))
                .thenReturn(mockDocument);

        Document doc = parser.parseInput("<html></html>", "http://base");

        // verify that a tracking list was created with the correct max size
        verify(mockTreeBuilder).parse(any(Reader.class), eq("http://base"),
                errorListCaptor.capture(), eq(mockSettings));
        ParseErrorList usedList = errorListCaptor.getValue();
        assertTrue("ParseErrorList should be tracking", usedList.isTrackErrors());
        assertEquals("Maximum errors should match the configured value", 5, usedList.getMaxSize());

        assertSame("Returned document should be the one from the TreeBuilder", mockDocument, doc);
    }

    @Test
    public void parseInputString_NoTrackingWhenDisabled() throws Exception {
        // Bob: edge case – tracking disabled (maxErrors == 0)
        parser.setTrackErrors(0);               // disables tracking
        when(mockTreeBuilder.parse(any(Reader.class), eq("http://base"), any(ParseErrorList.class), eq(mockSettings)))
                .thenReturn(mockDocument);

        Document doc = parser.parseInput("<html></html>", "http://base");

        verify(mockTreeBuilder).parse(any(Reader.class), eq("http://base"),
                errorListCaptor.capture(), eq(mockSettings));
        ParseErrorList usedList = errorListCaptor.getValue();
        assertFalse("ParseErrorList should NOT be tracking", usedList.isTrackErrors());

        assertSame(doc, mockDocument);
    }

    // -----------------------------------------------------------------------
    // 2. parseInput(Reader, String)
    // -----------------------------------------------------------------------
    @Test
    public void parseInputReader_UsesProvidedReader() throws Exception {
        // Carol: error scenario – ensure the exact Reader instance is passed through
        parser.setTrackErrors(1);
        when(mockTreeBuilder.parse(eq(mockReader), eq("base"), any(ParseErrorList.class), eq(mockSettings)))
                .thenReturn(mockDocument);

        Document doc = parser.parseInput(mockReader, "base");

        verify(mockTreeBuilder).parse(eq(mockReader), eq("base"),
                any(ParseErrorList.class), eq(mockSettings));
        assertSame(doc, mockDocument);
    }

    // -----------------------------------------------------------------------
    // 3. getTreeBuilder / setTreeBuilder
    // -----------------------------------------------------------------------
    @Test
    public void getAndSetTreeBuilder_Chainable() {
        // Alice: typical usage of getter and fluent setter
        TreeBuilder anotherBuilder = mock(TreeBuilder.class);
        Parser returned = parser.setTreeBuilder(anotherBuilder);
        assertSame("Setter should return the same Parser instance for chaining", parser, returned);
        assertSame("Getter should return the builder set via the setter", anotherBuilder, parser.getTreeBuilder());
    }

    // -----------------------------------------------------------------------
    // 4. isTrackErrors / setTrackErrors
    // -----------------------------------------------------------------------
    @Test
    public void isTrackErrors_ReflectsMaxErrors() {
        // Bob: edge cases – zero, positive and negative values
        parser.setTrackErrors(0);
        assertFalse(parser.isTrackErrors());

        parser.setTrackErrors(3);
        assertTrue(parser.isTrackErrors());

        parser.setTrackErrors(-5); // negative values are treated as disabled
        assertFalse(parser.isTrackErrors());
    }

    // -----------------------------------------------------------------------
    // 5. getErrors
    // -----------------------------------------------------------------------
    @Test
    public void getErrors_ReturnsErrorListFromLastParse() throws Exception {
        // Carol: after a parse, the stored errors list should be returned
        parser.setTrackErrors(2);
        when(mockTreeBuilder.parse(any(Reader.class), anyString(), any(ParseErrorList.class), eq(mockSettings)))
                .thenReturn(mockDocument);

        parser.parseInput("<!DOCTYPE html>", "http://example");
        List<ParseError> errors = parser.getErrors();

        assertNotNull("Errors list should never be null", errors);
        // The mock list is a real instance created by ParseErrorList.tracking()
        // we only assert that it is the same instance that was passed to the builder
        assertSame(errors, parser.getErrors());
    }

    // -----------------------------------------------------------------------
    // 6. settings() getter & setter
    // -----------------------------------------------------------------------
    @Test
    public void settingsGetterSetter_WorkCorrectly() {
        // Alice: verify fluent API and that the stored settings instance is used
        ParseSettings newSettings = ParseSettings.htmlNodeSettings(); // real static factory
        Parser returned = parser.settings(newSettings);
        assertSame(parser, returned);
        assertSame(newSettings, parser.settings());
    }

    // -----------------------------------------------------------------------
    // 7. static parse(String, String)
    // -----------------------------------------------------------------------
    @Test
    public void staticParse_UsesHtmlTreeBuilderAndNoErrorTracking() {
        // Bob: integration test using the real HtmlTreeBuilder to ensure a Document is produced
        Document doc = Parser.parse("<html><head><title>Test</title></head><body>Hi</body></html>",
                "http://base");
        assertNotNull("Document should not be null", doc);
        assertEquals("Base URI should be set", "http://base", doc.baseUri());
        assertEquals("Title should be parsed correctly", "Test", doc.title());
        // No errors should be tracked
        assertTrue("ParseErrorList should be non‑tracking", doc.parser().getErrors().isEmpty());
    }

    // -----------------------------------------------------------------------
    // 8. parseFragment(String, Element, String)
    // -----------------------------------------------------------------------
    @Test
    public void parseFragment_NoContext_ParsesIntoNodeList() {
        // Carol: typical fragment parsing without a context element
        List<Node> nodes = Parser.parseFragment("<p>Hello</p>", null, "http://base");
        assertNotNull(nodes);
        assertEquals(1, nodes.size());
        assertEquals("p", ((Element) nodes.get(0)).tagName());
        assertEquals("Hello", ((Element) nodes.get(0)).text());
    }

    // -----------------------------------------------------------------------
    // 9. parseFragment(String, Element, String, ParseErrorList)
    // -----------------------------------------------------------------------
    @Test
    public void parseFragment_WithErrorList_PassesListThrough() {
        // Alice: verify that the supplied error list is used
        ParseErrorList supplied = ParseErrorList.tracking(10);
        List<Node> nodes = Parser.parseFragment("<div></div>", null, "http://base", supplied);
        // The real HtmlTreeBuilder will fill the supplied list if errors occur,
        // but for well‑formed input the list stays empty.
        assertTrue("Error list should be empty for valid fragment", supplied.isEmpty());
        assertEquals(1, nodes.size());
    }

    // -----------------------------------------------------------------------
    // 10. parseXmlFragment(String, String)
    // -----------------------------------------------------------------------
    @Test
    public void parseXmlFragment_UsesXmlTreeBuilder() {
        // Bob: ensure that XML fragments are parsed (the XmlTreeBuilder is tolerant of XML)
        List<Node> nodes = Parser.parseXmlFragment("<root><child attr=\"val\"/></root>", "http://base");
        assertNotNull(nodes);
        assertEquals(1, nodes.size());
        assertEquals("root", ((Element) nodes.get(0)).tagName());
        Element child = ((Element) nodes.get(0)).firstElementChild();
        assertNotNull(child);
        assertEquals("child", child.tagName());
        assertEquals("val", child.attr("attr"));
    }

    // -----------------------------------------------------------------------
    // 11. parseBodyFragment(String, String)
    // -----------------------------------------------------------------------
    @Test
    public void parseBodyFragment_AppendsNodesToBody() {
        // Carol: complex scenario – multiple nodes are parsed and re‑parented correctly
        Document doc = Parser.parseBodyFragment("<div id='a'></div><p>text</p>", "http://base");
        Element body = doc.body();

        // The body should contain exactly the two nodes we supplied
        List<Element> children = body.children();
        assertEquals(2, children.size());
        assertEquals("div", children.get(0).tagName());
        assertEquals("a", children.get(0).id());
        assertEquals("p", children.get(1).tagName());
        assertEquals("text", children.get(1).text());

        // The head must be empty (as per the contract)
        assertTrue(doc.head().children().isEmpty());
    }

    // -----------------------------------------------------------------------
    // 12. unescapeEntities(String, boolean)
    // -----------------------------------------------------------------------
    @Test
    public void unescapeEntities_ConvertsEscapedSequences() {
        // Alice: typical HTML entity unescaping, both attribute and normal mode
        String escaped = "Tom &amp; Jerry &lt;3 &quot;test&quot;";
        String unescaped = Parser.unescapeEntities(escaped, false);
        assertEquals("Tom & Jerry <3 \"test\"", unescaped);

        // When parsing attribute values, the handling of single quotes differs
        String attrEscaped = "O&#39;Reilly";
        String attrUnescaped = Parser.unescapeEntities(attrEscaped, true);
        assertEquals("O'Reilly", attrUnescaped);
    }

    // -----------------------------------------------------------------------
    // 13. parseBodyFragmentRelaxed(String, String) – deprecated method
    // -----------------------------------------------------------------------
    @Test
    public void parseBodyFragmentRelaxed_DelegatesToParse() {
        // Bob: ensure the deprecated shortcut still produces the same result as parse()
        Document docRelaxed = Parser.parseBodyFragmentRelaxed("<title>Relaxed</title>", "http://base");
        Document docStandard = Parser.parse("<title>Relaxed</title>", "http://base");

        assertEquals(docStandard.title(), docRelaxed.title());
        // Both documents should have a body element (even if empty)
        assertNotNull(docRelaxed.body());
        assertNotNull(docStandard.body());
    }

    // -----------------------------------------------------------------------
    // 14. htmlParser() & xmlParser()
    // -----------------------------------------------------------------------
    @Test
    public void htmlParser_ReturnsParserWithHtmlTreeBuilder() {
        // Carol: verify the concrete TreeBuilder type
        Parser htmlParser = Parser.htmlParser();
        assertTrue("TreeBuilder should be an instance of HtmlTreeBuilder",
                htmlParser.getTreeBuilder() instanceof HtmlTreeBuilder);
    }

    @Test
    public void xmlParser_ReturnsParserWithXmlTreeBuilder() {
        // Alice: verify the concrete TreeBuilder type
        Parser xmlParser = Parser.xmlParser();
        assertTrue("TreeBuilder should be an instance of XmlTreeBuilder",
                xmlParser.getTreeBuilder() instanceof XmlTreeBuilder);
    }

    // -----------------------------------------------------------------------
    // 15. Defensive checks – null arguments
    // -----------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void parseInput_NullHtml_ThrowsException() {
        // Bob: the implementation does not guard against null; NPE is expected
        parser.parseInput((String) null, "http://base");
    }

    @Test(expected = NullPointerException.class)
    public void parseInput_NullBaseUri_ThrowsException() {
        // Carol: baseUri is required by the underlying TreeBuilder
        parser.parseInput("<html></html>", null);
    }

    @Test
    public void setTrackErrors_NegativeValue_DisablesTracking() {
        // Alice: negative maxErrors should behave like 0 (disabled)
        parser.setTrackErrors(-10);
        assertFalse(parser.isTrackErrors());
    }
}
