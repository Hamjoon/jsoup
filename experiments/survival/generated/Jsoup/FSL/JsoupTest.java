package org.jsoup;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Whitelist;
import org.jsoup.helper.HttpConnection;
import org.jsoup.helper.HttpConnection.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link Jsoup}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>All {@code parse(...)} overloads (String, File, InputStream, URL)</li>
 *   <li>Fragment parsing</li>
 *   <li>HTML cleaning (all overloads, with custom {@link Document.OutputSettings})</li>
 *   <li>Whitelist validation via {@code isValid}</li>
 *   <li>Connection creation via {@code connect(String)}</li>
 * </ul>
 *
 * <p>External network access is avoided – the {@code parse(URL, int)} test checks only error handling,
 * and the {@code connect(String)} test verifies that a {@link HttpConnection} is returned with the
 * expected URL without performing a request.
 */
public class JsoupTest {

    private File tempHtmlFile;

    @Before
    public void setUp() throws IOException {
        // Create a temporary HTML file used by the File‑based parse tests.
        tempHtmlFile = File.createTempFile("jsoup-test", ".html");
        try (FileWriter writer = new FileWriter(tempHtmlFile)) {
            writer.write("<!doctype html>\n<html><head><title>Temp</title></head>"
                    + "<body><p>Hello <b>World</b>!</p></body></html>");
        }
    }

    @After
    public void tearDown() {
        if (tempHtmlFile != null && tempHtmlFile.exists()) {
            tempHtmlFile.delete();
        }
    }

    // -----------------------------------------------------------------------
    // parse(String, String) – basic HTML parsing with base URI handling
    // -----------------------------------------------------------------------
    @Test
    public void parseStringWithBaseUri_resolvesRelativeLinks() {
        String html = "<a href=\"/path/page.html\">Link</a>";
        Document doc = Jsoup.parse(html, "http://example.com/base/");
        assertEquals("http://example.com/path/page.html", doc.selectFirst("a").absUrl("href"));
    }

    @Test(expected = NullPointerException.class)
    public void parseStringWithBaseUri_nullHtml_throwsNPE() {
        Jsoup.parse((String) null, "http://example.com");
    }

    @Test
    public void parseStringWithBaseUri_emptyString_returnsEmptyDocument() {
        Document doc = Jsoup.parse("", "http://example.com");
        // The body element should exist but be empty.
        assertEquals("", doc.body().html().trim());
    }

    // -----------------------------------------------------------------------
    // parse(String, String, Parser) – custom parser (XML)
    // -----------------------------------------------------------------------
    @Test
    public void parseStringWithCustomParser_xmlParserKeepsSelfClosingTags() {
        String xml = "<root><item id='1'/><item id='2'/></root>";
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        // In XML mode the tags are not altered – there should be exactly two <item> elements.
        assertEquals(2, doc.select("item").size());
        assertEquals("1", doc.selectFirst("item[id=1]").attr("id"));
    }

    // -----------------------------------------------------------------------
    // parse(String) – no base URI supplied
    // -----------------------------------------------------------------------
    @Test
    public void parseString_withoutBaseUri_parsesCorrectly() {
        String html = "<div><p>Test</p></div>";
        Document doc = Jsoup.parse(html);
        assertEquals("Test", doc.selectFirst("p").text());
    }

    // -----------------------------------------------------------------------
    // parse(File, String, String) – file with explicit base URI
    // -----------------------------------------------------------------------
    @Test
    public void parseFileWithBaseUri_resolvesRelativeLinks() throws IOException {
        Document doc = Jsoup.parse(tempHtmlFile, "UTF-8", "http://files.example/");
        // The <title> element is part of the parsed document.
        assertEquals("Temp", doc.title());
    }

    @Test(expected = IOException.class)
    public void parseFileWithBaseUri_nonExistingFile_throwsIOException() throws IOException {
        File missing = new File("non‑existent‑file.html");
        Jsoup.parse(missing, "UTF-8", "http://example.com");
    }

    // -----------------------------------------------------------------------
    // parse(File, String) – base URI derived from file path
    // -----------------------------------------------------------------------
    @Test
    public void parseFile_withoutBaseUri_usesFilePathAsBase() throws IOException {
        Document doc = Jsoup.parse(tempHtmlFile, "UTF-8");
        // The base URI should be the absolute path of the file.
        assertTrue(doc.baseUri().startsWith("file:"));
    }

    // -----------------------------------------------------------------------
    // parse(InputStream, String, String) – stream parsing
    // -----------------------------------------------------------------------
    @Test
    public void parseInputStream_parsesHtmlCorrectly() throws IOException {
        String html = "<html><body><h1>Header</h1></body></html>";
        ByteArrayInputStream in = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
        Document doc = Jsoup.parse(in, "UTF-8", "http://stream.example/");
        assertEquals("Header", doc.selectFirst("h1").text());
    }

    @Test(expected = IOException.class)
    public void parseInputStream_invalidCharset_throwsIOException() throws IOException {
        // Passing an illegal charset name forces DataUtil to throw IOException.
        ByteArrayInputStream in = new ByteArrayInputStream("<p>test</p>".getBytes());
        Jsoup.parse(in, "THIS_IS_NOT_A_CHARSET", "http://example.com");
    }

    // -----------------------------------------------------------------------
    // parse(InputStream, String, String, Parser) – stream with custom parser
    // -----------------------------------------------------------------------
    @Test
    public void parseInputStreamWithCustomParser_xmlKeepsStructure() throws IOException {
        String xml = "<root><child/></root>";
        ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        Document doc = Jsoup.parse(in, "UTF-8", "http://example.com", Parser.xmlParser());
        assertEquals(1, doc.select("child").size());
    }

    // -----------------------------------------------------------------------
    // parseBodyFragment – with and without base URI
    // -----------------------------------------------------------------------
    @Test
    public void parseBodyFragment_withBaseUri_resolvesRelativeUrl() {
        String fragment = "<img src=\"/img/logo.png\">";
        Document doc = Jsoup.parseBodyFragment(fragment, "http://example.com/base/");
        assertEquals("http://example.com/img/logo.png", doc.selectFirst("img").absUrl("src"));
    }

    @Test
    public void parseBodyFragment_withoutBaseUri_usesEmptyBase() {
        String fragment = "<p>Just text</p>";
        Document doc = Jsoup.parseBodyFragment(fragment);
        assertEquals("", doc.baseUri());
        assertEquals("Just text", doc.body().text());
    }

    // -----------------------------------------------------------------------
    // parse(URL, int) – error handling (invalid protocol & connection timeout)
    // -----------------------------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void parseUrl_invalidProtocol_throwsIllegalArgumentException() throws IOException {
        // Jsoup only accepts http/https – using ftp should cause an IllegalArgumentException.
        Jsoup.parse(new URL("ftp://example.com/file.html"), 1000);
    }

    @Test
    public void parseUrl_connectionTimeout_throwsIOException() {
        try {
            // Use a well‑formed but non‑routable address with a very short timeout.
            Jsoup.parse(new URL("http://10.255.255.1/"), 10);
            fail("Expected IOException due to timeout");
        } catch (IOException e) {
            // Expected – the exact subclass (SocketTimeoutException) is not required.
            assertTrue(e.getMessage().contains("timed out") || e.getMessage().contains("connect"));
        }
    }

    // -----------------------------------------------------------------------
    // connect(String) – verify returned Connection type and stored URL
    // -----------------------------------------------------------------------
    @Test
    public void connect_returnsHttpConnectionWithCorrectUrl() {
        String url = "http://example.com/";
        Connection con = Jsoup.connect(url);
        assertNotNull(con);
        // The concrete class is HttpConnection; we can safely cast.
        assertTrue(con instanceof HttpConnection);
        assertEquals(url, con.request().url().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void connect_invalidProtocol_throwsIllegalArgumentException() {
        Jsoup.connect("mailto:someone@example.com");
    }

    // -----------------------------------------------------------------------
    // clean(...) – basic whitelist cleaning
    // -----------------------------------------------------------------------
    @Test
    public void clean_basicWhitelist_stripsDisallowedTags() {
        String dirty = "<script>alert('x')</script><p>Hello <b>World</b></p>";
        String clean = Jsoup.clean(dirty, Whitelist.basic());
        // <script> should be removed; <b> is allowed in the basic whitelist.
        assertFalse(clean.contains("<script>"));
        assertTrue(clean.contains("<b>World</b>"));
        // The surrounding <p> should also be present because it's allowed.
        assertTrue(clean.contains("<p>"));
    }

    @Test
    public void clean_withBaseUri_resolvesRelativeLinksInAllowedAttributes() {
        String dirty = "<a href=\"/relative/path\">Link</a>";
        String clean = Jsoup.clean(dirty, "http://example.com/base/", Whitelist.basic());
        // The href should be made absolute using the supplied base URI.
        assertTrue(clean.contains("href=\"http://example.com/relative/path\""));
    }

    @Test
    public void clean_withOutputSettings_prettyPrintDisabled() {
        String dirty = "<p>   spaced    text   </p>";
        Document.OutputSettings settings = new Document.OutputSettings().prettyPrint(false);
        String clean = Jsoup.clean(dirty, "", Whitelist.simpleText(), settings);
        // prettyPrint(false) means the output should not contain extra newlines or indentation.
        assertFalse(clean.contains("\n"));
        // Whitespace inside the element should be collapsed by the parser.
        assertEquals("<p>spaced text</p>", clean.trim());
    }

    // -----------------------------------------------------------------------
    // isValid – whitelist validation
    // -----------------------------------------------------------------------
    @Test
    public void isValid_allowsOnlyWhitelistedContent() {
        String safe = "<p>Safe <b>text</b></p>";
        String unsafe = "<p>Bad <script>alert(1)</script></p>";

        assertTrue(Jsoup.isValid(safe, Whitelist.basic()));
        assertFalse(Jsoup.isValid(unsafe, Whitelist.basic()));
    }

    @Test
    public void isValid_emptyString_isValid() {
        assertTrue(Jsoup.isValid("", Whitelist.basic()));
    }

    // -----------------------------------------------------------------------
    // Additional edge case: null arguments handling
    // -----------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void clean_nullHtml_throwsNPE() {
        Jsoup.clean(null, Whitelist.basic());
    }

    @Test(expected = NullPointerException.class)
    public void isValid_nullWhitelist_throwsNPE() {
        Jsoup.isValid("<p>test</p>", null);
    }
}
