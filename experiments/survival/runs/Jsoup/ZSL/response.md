###Test START##
```java
/*
 * JUnit‑4 test suite for {@link org.jsoup.Jsoup}.
 *
 * The tests cover every public static method of the Jsoup class.
 * Real parsing is exercised for the core HTML parsing methods while
 * the network‑related methods are mocked using Mockito’s static‑mock
 * capabilities (requires mockito‑inline on the class‑path).
 *
 * To run the tests you need the following dependencies on the test
 * class‑path:
 *   - junit:junit:4.13.2
 *   - org.mockito:mockito-core:5.x
 *   - org.mockito:mockito-inline:5.x   (enables static mocking)
 *
 * The test class is deliberately self‑contained – it creates temporary
 * files, streams and a simple XML parser to verify the overloads.
 */
package org.jsoup;

import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.jsoup.helper.HttpConnection;
import org.jsoup.Connection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Comprehensive unit tests for {@link Jsoup}.
 */
public class JsoupTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File htmlFile;
    private final String htmlContent = "<!DOCTYPE html>\n" +
            "<html><head><title>Test Title</title></head>\n" +
            "<body><p class=\"msg\">Hello <b>World</b>!</p></body></html>";

    @Before
    public void setUp() throws IOException {
        // create a temporary file that will be used by the file‑based parse methods
        htmlFile = folder.newFile("sample.html");
        Files.write(htmlFile.toPath(), htmlContent.getBytes(StandardCharsets.UTF_8));
    }

    @After
    public void tearDown() {
        // JUnit's TemporaryFolder rule automatically deletes the files.
    }

    /* --------------------------------------------------------------
     *  parse(String html, String baseUri)   and   parse(String html)
     * -------------------------------------------------------------- */
    @Test
    public void testParseStringWithBaseUri() {
        String base = "http://example.com/";
        Document doc = Jsoup.parse(htmlContent, base);
        assertEquals("Base URI should be propagated", base, doc.baseUri());
        assertEquals("Title must be parsed correctly", "Test Title", doc.title());
        assertEquals("Paragraph text must be present", "Hello World!", doc.select("p").text());
    }

    @Test
    public void testParseStringWithoutBaseUri() {
        Document doc = Jsoup.parse(htmlContent);
        // When no base URI is supplied Jsoup uses an empty string.
        assertEquals("", doc.baseUri());
        assertEquals("Test Title", doc.title());
    }

    /* --------------------------------------------------------------
     *  parse(String html, String baseUri, Parser parser)
     * -------------------------------------------------------------- */
    @Test
    public void testParseWithCustomParser() {
        // Use the XML parser on a tiny XML fragment.
        String xml = "<root><item>Value</item></root>";
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        // The XML parser treats the fragment as a document with a root element.
        assertEquals("root", doc.child(0).tagName());
        assertEquals("Value", doc.selectFirst("item").text());
    }

    /* --------------------------------------------------------------
     *  parse(File in, String charsetName, String baseUri)
     * -------------------------------------------------------------- */
    @Test
    public void testParseFileWithCharsetAndBaseUri() throws IOException {
        Document doc = Jsoup.parse(htmlFile, "UTF-8", "http://baseuri/");
        assertEquals("http://baseuri/", doc.baseUri());
        assertEquals("Test Title", doc.title());
    }

    /* --------------------------------------------------------------
     *  parse(File in, String charsetName)   (base URI = file path)
     * -------------------------------------------------------------- */
    @Test
    public void testParseFileUsesAbsolutePathAsBaseUri() throws IOException {
        Document doc = Jsoup.parse(htmlFile, "UTF-8");
        assertEquals(htmlFile.getAbsolutePath(), doc.baseUri());
        assertEquals("Test Title", doc.title());
    }

    /* --------------------------------------------------------------
     *  parse(InputStream in, String charsetName, String baseUri)
     * -------------------------------------------------------------- */
    @Test
    public void testParseInputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(htmlContent.getBytes(StandardCharsets.UTF_8))) {
            Document doc = Jsoup.parse(is, null, "http://stream/");
            assertEquals("http://stream/", doc.baseUri());
            assertEquals("Test Title", doc.title());
        }
    }

    /* --------------------------------------------------------------
     *  parse(InputStream in, String charsetName, String baseUri, Parser parser)
     * -------------------------------------------------------------- */
    @Test
    public void testParseInputStreamWithCustomParser() throws IOException {
        String xml = "<note><to>Bob</to></note>";
        try (InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            Document doc = Jsoup.parse(is, null, "http://xml/", Parser.xmlParser());
            assertEquals("note", doc.selectFirst("note").tagName());
            assertEquals("Bob", doc.selectFirst("to").text());
        }
    }

    /* --------------------------------------------------------------
     *  parseBodyFragment overloads
     * -------------------------------------------------------------- */
    @Test
    public void testParseBodyFragmentWithBaseUri() {
        String fragment = "<div><a href=\"/relative\">Link</a></div>";
        Document doc = Jsoup.parseBodyFragment(fragment, "http://example.com/");
        assertEquals("http://example.com/", doc.baseUri());
        assertEquals("Link", doc.selectFirst("a").text());
        // The href should be resolved against the base URI.
        assertEquals("http://example.com/relative", doc.selectFirst("a").absUrl("href"));
    }

    @Test
    public void testParseBodyFragmentWithoutBaseUri() {
        String fragment = "<p>Just text</p>";
        Document doc = Jsoup.parseBodyFragment(fragment);
        assertEquals("", doc.baseUri());
        assertEquals("Just text", doc.body().text());
    }

    /* --------------------------------------------------------------
     *  clean(...) overloads
     * -------------------------------------------------------------- */
    @Test
    public void testCleanWithWhitelist() {
        String dirty = "<script>alert('x');</script><p class=\"good\">OK</p>";
        Whitelist wl = Whitelist.basic(); // allows <p> but not <script>
        String clean = Jsoup.clean(dirty, wl);
        // script tag must be removed, p must stay
        assertFalse("Script tag should be removed", clean.contains("<script>"));
        assertTrue("Paragraph should stay", clean.contains("<p"));
        assertTrue("Class attribute is allowed by basic whitelist", clean.contains("class=\"good\""));
    }

    @Test
    public void testCleanWithBaseUriAndOutputSettings() {
        String dirty = "<a href=\"/path\">Link</a>";
        Whitelist wl = Whitelist.basic();
        Document.OutputSettings settings = new Document.OutputSettings()
                .prettyPrint(false)          // produce compact output
                .escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml);
        String clean = Jsoup.clean(dirty, "http://example.com/", wl, settings);
        // href must be resolved
        assertTrue("Absolute URL must be resolved", clean.contains("href=\"http://example.com/path\""));
        // prettyPrint false -> no newlines
        assertFalse("No newline should be present", clean.contains("\n"));
    }

    /* --------------------------------------------------------------
     *  isValid(...)
     * -------------------------------------------------------------- */
    @Test
    public void testIsValid() {
        Whitelist wl = Whitelist.basic();
        assertTrue("Allowed HTML should be valid", Jsoup.isValid("<p>Hello</p>", wl));
        assertFalse("Disallowed <script> should make HTML invalid", Jsoup.isValid("<script></script>", wl));
    }

    /* --------------------------------------------------------------
     *  connect(String) – simple sanity check (no network call)
     * -------------------------------------------------------------- */
    @Test
    public void testConnectReturnsConnection() {
        // HttpConnection.connect returns a concrete Connection implementation that
        // does not perform any I/O until .execute() or .get() is called.
        Connection conn = Jsoup.connect("http://example.org/");
        assertNotNull("Connection must not be null", conn);
        assertEquals("URL stored in connection must match", "http://example.org/", conn.request().url().toString());
    }

    /* --------------------------------------------------------------
     *  parse(URL, int) – mocked HttpConnection to avoid real HTTP traffic
     * -------------------------------------------------------------- */
    @Test
    public void testParseUrlWithTimeout() throws Exception {
        URL url = new URL("http://dummy.test/");
        int timeout = 1234;

        // Mock the static HttpConnection.connect(URL) method.
        try (MockedStatic<HttpConnection> mockedHttp = Mockito.mockStatic(HttpConnection.class)) {
            // Create a mock Connection that records the timeout call and returns a stub Document.
            Connection mockConn = Mockito.mock(Connection.class);
            Document stubDoc = Jsoup.parse("<html><head><title>Stub</title></head></html>");

            // Stubbing: when timeout(..) is called, return the same mock (fluent API).
            Mockito.when(mockConn.timeout(timeout)).thenReturn(mockConn);
            // Stubbing: when get() is called, return the stub Document.
            Mockito.when(mockConn.get()).thenReturn(stubDoc);

            // Wire the static method to return our mock connection.
            mockedHttp.when(() -> HttpConnection.connect(eq(url))).thenReturn(mockConn);

            // Call the method under test.
            Document result = Jsoup.parse(url, timeout);

            // Verify interactions.
            mockedHttp.verify(() -> HttpConnection.connect(eq(url)));
            Mockito.verify(mockConn).timeout(eq(timeout));
            Mockito.verify(mockConn).get();

            // Verify the returned Document is the stub.
            assertEquals("Stub", result.title());
        }
    }

    /* --------------------------------------------------------------
     *  parse(URL, int) – ensure IllegalArgumentException for unsupported protocol
     *    (this is a behaviour of HttpConnection; we just verify that the
     *    exception propagates). This test uses the real implementation
     *    because it fails fast before any network access.
     * -------------------------------------------------------------- */
    @Test(expected = java.net.MalformedURLException.class)
    public void testParseUrlWithUnsupportedProtocol() throws Exception {
        // Attempt to parse a non‑http URL; HttpConnection.connect should throw.
        URL badUrl = new URL("ftp://example.com/");
        Jsoup.parse(badUrl, 1000);
    }
}
```
###Test END##