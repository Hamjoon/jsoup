###Test START##
```java
package org.jsoup;

import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Whitelist;
import org.jsoup.safety.Cleaner;
import org.jsoup.helper.HttpConnection;
import org.junit.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class JsoupTest {

    /* ---------------------------------------------------------------------- */
    /*  Helper methods                                                       */
    /* ---------------------------------------------------------------------- */

    private static File createTempHtmlFile(String content) throws IOException {
        Path tmp = Files.createTempFile("jsoup-test-", ".html");
        Files.write(tmp, content.getBytes("UTF-8"));
        tmp.toFile().deleteOnExit();
        return tmp.toFile();
    }

    private static InputStream toInputStream(String content) {
        return new ByteArrayInputStream(content.getBytes());
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for parse(String, String)                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testParseStringBaseUri() {
        String html = "<html><head><title>Test</title></head><body><a href='page.html'>Link</a></body></html>";
        Document doc = Jsoup.parse(html, "http://example.com/");
        assertEquals("http://example.com/", doc.baseUri());
        assertEquals("Test", doc.title());
        assertEquals("http://example.com/page.html", doc.selectFirst("a").absUrl("href"));
    }

    @Test(expected = NullPointerException.class)
    public void testParseStringBaseUri_NullHtml() {
        Jsoup.parse((String) null, "http://example.com/");
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for parse(String, String, Parser)                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testParseStringBaseUriWithParser() {
        String xml = "<note><to>Tove</to><from>Jani</from></note>";
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        assertEquals("note", doc.selectFirst("note").tagName());
        assertEquals("Tove", doc.selectFirst("to").text());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseStringBaseUriWithParser_NullParser() {
        Jsoup.parse("<p>hi</p>", "", null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for parse(String)                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testParseString_NoBaseUri() {
        String html = "<p>Hello</p>";
        Document doc = Jsoup.parse(html);
        assertEquals("", doc.baseUri());
        assertEquals("Hello", doc.body().text());
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for connect(String)                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConnectReturnsConnection() {
        HttpConnection conn = (HttpConnection) Jsoup.connect("http://example.com");
        assertNotNull(conn);
        assertEquals("http://example.com", conn.request().url().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConnectInvalidProtocol() {
        Jsoup.connect("ftp://example.com");
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for parse(File, String, String)                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testParseFileWithCharsetAndBaseUri() throws IOException {
        File file = createTempHtmlFile("<html><body>File Test</body></html>");
        Document doc = Jsoup.parse(file, "UTF-8", "http://base/");
        assertEquals("File Test", doc.body().text());
        assertEquals("http://base/", doc.baseUri());
    }

    @Test(expected = IOException.class)
    public void testParseFile_NonExisting() throws IOException {
        File nonExisting = new File("nonexistent.html");
        Jsoup.parse(nonExisting, "UTF-8", "http://base/");
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for parse(File, String)                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testParseFileWithCharsetUsesFilePathAsBaseUri() throws IOException {
        File file = createTempHtmlFile("<html><body>Path Test</body></html>");
        Document doc = Jsoup.parse(file, null);
        assertEquals("Path Test", doc.body().text());
        assertTrue(doc.baseUri().endsWith(file.getAbsolutePath()));
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for parse(InputStream, String, String)                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testParseInputStream() throws IOException {
        InputStream is = toInputStream("<html><body>Stream Test</body></html>");
        Document doc = Jsoup.parse(is, null, "http://stream/");
        assertEquals("Stream Test", doc.body().text());
        assertEquals("http://stream/", doc.baseUri());
    }

    @Test(expected = NullPointerException.class)
    public void testParseInputStream_NullInputStream() throws IOException {
        Jsoup.parse((InputStream) null, null, "http://x/");
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for parse(InputStream, String, String, Parser)                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testParseInputStreamWithParser() throws IOException {
        InputStream is = toInputStream("<root><child>XML</child></root>");
        Document doc = Jsoup.parse(is, null, "", Parser.xmlParser());
        assertEquals("XML", doc.selectFirst("child").text());
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for parseBodyFragment(String, String)                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testParseBodyFragmentWithBaseUri() {
        String fragment = "<a href='page.html'>Link</a>";
        Document doc = Jsoup.parseBodyFragment(fragment, "http://example.com/");
        assertEquals("http://example.com/page.html", doc.selectFirst("a").absUrl("href"));
        assertEquals("Link", doc.body().text());
    }

    @Test
    public void testParseBodyFragment_NoBaseUri() {
        String fragment = "<p>Just text</p>";
        Document doc = Jsoup.parseBodyFragment(fragment);
        assertEquals("", doc.baseUri());
        assertEquals("Just text", doc.body().text());
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for parse(URL, int)                                             */
    /* ---------------------------------------------------------------------- */

    @Test(expected = MalformedURLException.class)
    public void testParseUrl_Malformed() throws IOException {
        URL badUrl = new URL("ftp://example.com");
        Jsoup.parse(badUrl, 1000);
    }

    @Test(expected = IOException.class)
    public void testParseUrl_Timeout() throws IOException {
        // Assuming that connecting to a non‑routable address will timeout quickly.
        URL url = new URL("http://10.255.255.1/"); // unlikely to exist
        Jsoup.parse(url, 1); // 1 ms timeout forces a timeout exception
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for clean(String, String, Whitelist)                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCleanRemovesDisallowedTags() {
        String dirty = "<script>alert('x');</script><p>Hello <b>World</b></p>";
        Whitelist wl = Whitelist.basic(); // <script> not allowed
        String clean = Jsoup.clean(dirty, "", wl);
        assertFalse(clean.contains("<script>"));
        assertTrue(clean.contains("<p>"));
        assertTrue(clean.contains("<b>"));
    }

    @Test(expected = NullPointerException.class)
    public void testClean_NullWhitelist() {
        Jsoup.clean("<p>test</p>", "", null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for clean(String, Whitelist)                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCleanSimple() {
        String dirty = "<img src='x' onerror='alert(1)'><p>OK</p>";
        Whitelist wl = Whitelist.basic(); // img allowed but without onerror
        String clean = Jsoup.clean(dirty, wl);
        assertTrue(clean.contains("<img"));
        assertFalse(clean.contains("onerror"));
        assertTrue(clean.contains("<p>OK</p>"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for clean(String, String, Whitelist, OutputSettings)           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCleanWithOutputSettings() {
        String dirty = "<p>   spaced   </p>";
        Whitelist wl = Whitelist.relaxed();
        Document.OutputSettings os = new Document.OutputSettings().prettyPrint(false);
        String clean = Jsoup.clean(dirty, "", wl, os);
        // prettyPrint false -> no extra whitespace added
        assertEquals("<p>   spaced   </p>", clean);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for isValid(String, Whitelist)                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsValidTrue() {
        String html = "<p>Hello <b>World</b></p>";
        assertTrue(Jsoup.isValid(html, Whitelist.basic()));
    }

    @Test
    public void testIsValidFalse() {
        String html = "<script>evil()</script><p>Hi</p>";
        assertFalse(Jsoup.isValid(html, Whitelist.basic()));
    }

    @Test(expected = NullPointerException.class)
    public void testIsValid_NullWhitelist() {
        Jsoup.isValid("<p>test</p>", null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Edge case: empty strings and nulls where allowed                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testParseEmptyString() {
        Document doc = Jsoup.parse("");
        assertEquals("", doc.body().html());
    }

    @Test
    public void testParseBodyFragmentEmpty() {
        Document doc = Jsoup.parseBodyFragment("");
        assertEquals("", doc.body().html());
    }

    @Test
    public void testCleanEmptyInput() {
        String result = Jsoup.clean("", Whitelist.basic());
        assertEquals("", result);
    }

    /* ---------------------------------------------------------------------- */
    /*  Edge case: unsupported charset                                         */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IOException.class)
    public void testParseFileUnsupportedCharset() throws IOException {
        File file = createTempHtmlFile("<p>test</p>");
        Jsoup.parse(file, "UNSUPPORTED-CHARSET", "http://base/");
    }

    /* ---------------------------------------------------------------------- */
    /*  Edge case: null baseUri handling (should default to empty string)    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testParseNullBaseUri() {
        Document doc = Jsoup.parse("<a href='page.html'>link</a>", null);
        // baseUri null should be treated as empty string, so absUrl will be empty
        assertEquals("", doc.baseUri());
        assertEquals("", doc.selectFirst("a").absUrl("href"));
    }

    @Test
    public void testParseBodyFragmentNullBaseUri() {
        Document doc = Jsoup.parseBodyFragment("<a href='page.html'>link</a>", null);
        assertEquals("", doc.baseUri());
        assertEquals("", doc.selectFirst("a").absUrl("href"));
    }
}
```
###Test END##