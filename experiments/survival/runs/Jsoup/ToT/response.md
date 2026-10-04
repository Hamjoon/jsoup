###Test START##
```java
package org.jsoup;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Comprehensive JUnit‑4 test suite for {@link Jsoup}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Parsing from {@code String}, {@code File}, {@code InputStream} and {@code URL}</li>
 *   <li>Base‑URI handling and absolute URL resolution</li>
 *   <li>Custom {@link Parser} usage (HTML vs XML)</li>
 *   <li>Body‑fragment parsing</li>
 *   <li>HTML cleaning with different {@link Whitelist}s and {@link Document.OutputSettings}</li>
 *   <li>Validation of cleanable HTML</li>
 *   <li>Error handling – malformed URLs, unsupported protocols, null arguments, I/O problems</li>
 * </ul>
 *
 * <p>All tests are self‑contained; they never hit the network and use temporary files / streams only.
 */
public class JsoupTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    // -----------------------------------------------------------------------
    // 1. parse(String html, String baseUri)
    // -----------------------------------------------------------------------
    @Test
    public void parseStringWithBaseUri_resolvesRelativeLinks() {
        String html = "<a href=\"/path/page.html\">Link</a>";
        String base = "http://example.com/base/";
        Document doc = Jsoup.parse(html, base);
        assertEquals("http://example.com/path/page.html", doc.selectFirst("a").absUrl("href"));
    }

    @Test
    public void parseStringWithoutBaseUri_hasEmptyBase() {
        String html = "<p>Hello</p>";
        Document doc = Jsoup.parse(html);
        // The base URI should be the empty string as defined by the overload.
        assertEquals("", doc.baseUri());
        assertEquals("Hello", doc.body().text());
    }

    // -----------------------------------------------------------------------
    // 2. parse(String html, String baseUri, Parser parser)
    // -----------------------------------------------------------------------
    @Test
    public void parseStringWithCustomParser_xmlParser() {
        String xml = "<root><child attr=\"1\"/></root>";
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        // xmlParser keeps tags as they are and does not add <html>/<body>
        assertEquals("root", doc.child(0).nodeName());
        assertEquals("child", doc.selectFirst("child").nodeName());
        assertEquals("1", doc.selectFirst("child").attr("attr"));
    }

    // -----------------------------------------------------------------------
    // 3. parse(File in, String charsetName, String baseUri)
    // -----------------------------------------------------------------------
    @Test
    public void parseFile_withExplicitCharsetAndBaseUri() throws IOException {
        File f = folder.newFile("sample.html");
        String content = "<!DOCTYPE html><html><head><title>Test</title></head>"
                       + "<body><img src=\"image.png\"></body></html>";
        // write using UTF‑8
        java.nio.file.Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));

        Document doc = Jsoup.parse(f, "UTF-8", "http://myhost/");
        assertEquals("Test", doc.title());
        // Verify that the base URI supplied is used for absolute URL resolution
        assertEquals("http://myhost/image.png", doc.selectFirst("img").absUrl("src"));
    }

    @Test
    public void parseFile_nullCharset_fallsBackToUtf8() throws IOException {
        File f = folder.newFile("utf8.html");
        String html = "<p>ß ∂ €</p>"; // contains non‑ASCII characters
        java.nio.file.Files.write(f.toPath(), html.getBytes(StandardCharsets.UTF_8));

        Document doc = Jsoup.parse(f, null); // charsetName == null
        assertEquals("ß ∂ €", doc.body().text());
    }

    // -----------------------------------------------------------------------
    // 4. parse(InputStream in, String charsetName, String baseUri)
    // -----------------------------------------------------------------------
    @Test
    public void parseInputStream_withNullCharsetAndBase() throws IOException {
        String html = "<div><a href=\"/relative\">link</a></div>";
        ByteArrayInputStream in = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));

        Document doc = Jsoup.parse(in, null, "http://example.org/");
        assertEquals("http://example.org/relative", doc.selectFirst("a").absUrl("href"));
    }

    @Test
    public void parseInputStream_withCustomParser() throws IOException {
        String xml = "<note><to>Tove</to></note>";
        ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

        Document doc = Jsoup.parse(in, null, "", Parser.xmlParser());
        assertEquals("note", doc.child(0).nodeName());
        assertEquals("Tove", doc.selectFirst("to").text());
    }

    // -----------------------------------------------------------------------
    // 5. parseBodyFragment(...)
    // -----------------------------------------------------------------------
    @Test
    public void parseBodyFragment_withBaseUri() {
        String fragment = "<img src=\"/img.png\">";
        Document doc = Jsoup.parseBodyFragment(fragment, "http://host/");
        assertEquals("http://host/img.png", doc.selectFirst("img").absUrl("src"));
    }

    @Test
    public void parseBodyFragment_withoutBaseUri() {
        String fragment = "<span>hello</span>";
        Document doc = Jsoup.parseBodyFragment(fragment);
        assertEquals("hello", doc.body().text());
    }

    // -----------------------------------------------------------------------
    // 6. clean(...) overloads
    // -----------------------------------------------------------------------
    @Test
    public void clean_basicWhitelist_removesDisallowedTags() {
        String dirty = "<p>Hello <script>alert('x');</script> world</p>";
        String clean = Jsoup.clean(dirty, Whitelist.basic());
        // <script> must be stripped, but <p> stays
        assertEquals("<p>Hello  world</p>", clean);
    }

    @Test
    public void clean_withBaseUri_andWhitelist() {
        String dirty = "<a href=\"/relative\">click</a>";
        String clean = Jsoup.clean(dirty, "http://example.com", Whitelist.basic());
        // The href should be resolved to an absolute URL
        assertTrue(clean.contains("href=\"http://example.com/relative\""));
    }

    @Test
    public void clean_withOutputSettings_prettyPrintDisabled() {
        String dirty = "<div><p>One</p><p>Two</p></div>";
        Document.OutputSettings settings = new Document.OutputSettings().prettyPrint(false);
        String clean = Jsoup.clean(dirty, "", Whitelist.relaxed(), settings);
        // prettyPrint disabled → no newline/indentation
        assertEquals("<div><p>One</p><p>Two</p></div>", clean);
    }

    // -----------------------------------------------------------------------
    // 7. isValid(...)
    // -----------------------------------------------------------------------
    @Test
    public void isValid_returnsTrueWhenAllTagsAllowed() {
        String html = "<b>Bold</b> <i>Italic</i>";
        assertTrue(Jsoup.isValid(html, Whitelist.basic()));
    }

    @Test
    public void isValid_returnsFalseWhenDisallowedTagPresent() {
        String html = "<b>Ok</b><script>bad</script>";
        assertFalse(Jsoup.isValid(html, Whitelist.basic()));
    }

    // -----------------------------------------------------------------------
    // 8. connect(String url) – protocol validation
    // -----------------------------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void connect_unsupportedProtocol_throws() {
        // Jsoup only supports http/https; ftp should cause an IllegalArgumentException
        Jsoup.connect("ftp://example.com");
    }

    // -----------------------------------------------------------------------
    // 9. parse(URL, int) – error handling for unsupported protocol
    // -----------------------------------------------------------------------
    @Test
    public void parseUrl_unsupportedProtocol_throwsIllegalArgumentException() throws Exception {
        URL ftpUrl = new URL("ftp://example.com/file.html");
        try {
            Jsoup.parse(ftpUrl, 1000);
            fail("Expected IllegalArgumentException for non‑http/https URL");
        } catch (IllegalArgumentException e) {
            // expected – message contains protocol name
            assertTrue(e.getMessage().contains("ftp"));
        }
    }

    // -----------------------------------------------------------------------
    // 10. Defensive null handling (NPEs are expected as per Jsoup contract)
    // -----------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void parse_nullHtml_throwsNPE() {
        Jsoup.parse((String) null, "http://base/");
    }

    @Test(expected = NullPointerException.class)
    public void parseBodyFragment_nullHtml_throwsNPE() {
        Jsoup.parseBodyFragment((String) null, "http://base/");
    }

    @Test(expected = NullPointerException.class)
    public void clean_nullWhitelist_throwsNPE() {
        Jsoup.clean("<p>test</p>", Whitelist.basic());
        // The overload that accepts a Whitelist is the one that can be null‑checked.
        // Passing null should lead to NPE from the Cleaner constructor.
        Jsoup.clean("<p>test</p>", "", null);
    }
}
```
###Test END##