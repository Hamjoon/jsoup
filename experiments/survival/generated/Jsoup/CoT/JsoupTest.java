package org.jsoup;

import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Whitelist;
import org.junit.Test;

import java.io.*;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class JsoupTest {

    /* -------------------------------------------------
       BASIC FUNCTIONALITY TESTS
       ------------------------------------------------- */

    @Test
    public void parse_StringAndBaseUri() {
        String html = "<html><head><title>Test</title></head><body>Hello</body></html>";
        Document doc = Jsoup.parse(html, "http://example.com/");
        assertEquals("Test", doc.title());
        assertEquals("http://example.com/", doc.baseUri());
    }

    @Test
    public void parse_StringOnly() {
        String html = "<p>Just a fragment</p>";
        Document doc = Jsoup.parse(html);
        assertTrue(doc.body().html().contains("Just a fragment"));
        assertEquals("", doc.baseUri());
    }

    @Test
    public void parse_WithCustomParser() {
        String xml = "<root><item>value</item></root>";
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());
        assertEquals("root", doc.selectFirst("root").tagName());
        assertEquals("value", doc.selectFirst("item").text());
    }

    @Test
    public void parseFile() throws IOException {
        File tmp = File.createTempFile("jsoup-test", ".html");
        tmp.deleteOnExit();
        Files.write(tmp.toPath(), "<html><body>File content</body></html>".getBytes("UTF-8"));
        Document doc = Jsoup.parse(tmp, "UTF-8");
        assertTrue(doc.body().html().contains("File content"));
    }

    @Test
    public void parseInputStream() throws IOException {
        String html = "<html><body>Stream content</body></html>";
        InputStream in = new ByteArrayInputStream(html.getBytes("UTF-8"));
        Document doc = Jsoup.parse(in, "UTF-8", "http://example.com/");
        assertTrue(doc.body().html().contains("Stream content"));
    }

    @Test
    public void parseBodyFragment_WithBaseUri() {
        String fragment = "<a href=\"/link\">Link</a>";
        Document doc = Jsoup.parseBodyFragment(fragment, "http://example.com/");
        assertEquals("http://example.com/", doc.baseUri());
        assertEquals("Link", doc.selectFirst("a").text());
    }

    @Test
    public void clean_WithWhitelist() {
        String dirty = "<script>alert('x');</script><p>OK</p>";
        String clean = Jsoup.clean(dirty, Whitelist.basic());
        assertFalse(clean.contains("<script>"));
        assertTrue(clean.contains("<p>OK</p>"));
    }

    @Test
    public void clean_WithOutputSettings() {
        String dirty = "<p>   spaced   </p>";
        Document.OutputSettings os = new Document.OutputSettings().prettyPrint(false);
        String clean = Jsoup.clean(dirty, "", Whitelist.basic(), os);
        assertEquals("<p>   spaced   </p>", clean.trim());
    }

    @Test
    public void isValid_ValidHtml() {
        String html = "<p>Hello <b>world</b></p>";
        assertTrue(Jsoup.isValid(html, Whitelist.basic()));
    }

    @Test
    public void isValid_InvalidHtml() {
        String html = "<script>alert(1)</script>";
        assertFalse(Jsoup.isValid(html, Whitelist.basic()));
    }

    @Test
    public void connect_ReturnsConnection() {
        Connection con = Jsoup.connect("http://example.com");
        assertNotNull(con);
        assertEquals("http://example.com", con.request().url().toString());
    }

    /* -------------------------------------------------
       EDGE‑CASE & EXCEPTION TESTS
       ------------------------------------------------- */

    // parse(String, String) -------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void parse_NullHtml_Throws() {
        Jsoup.parse((String) null, "http://example.com");
    }

    @Test
    public void parse_NullBaseUri_TreatedAsEmpty() {
        Document doc = Jsoup.parse("<p>test</p>", null);
        assertEquals("", doc.baseUri());
    }

    // parse(String, String, Parser) -----------------------------------------
    @Test(expected = NullPointerException.class)
    public void parse_NullParser_Throws() {
        Jsoup.parse("<p>test</p>", "http://example.com", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parse_NullHtmlWithParser_Throws() {
        Jsoup.parse((String) null, "http://example.com", Parser.htmlParser());
    }

    // parse(File, …) ---------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void parseFile_NullFile_Throws() throws IOException {
        Jsoup.parse((File) null, "UTF-8", "http://example.com");
    }

    @Test(expected = IOException.class)
    public void parseFile_NonExistingFile_Throws() throws IOException {
        File bogus = new File("nonexistent-" + System.nanoTime() + ".html");
        Jsoup.parse(bogus, "UTF-8", "http://example.com");
    }

    @Test(expected = IOException.class)
    public void parseFile_InvalidCharset_Throws() throws IOException {
        File tmp = File.createTempFile("jsoup-test", ".html");
        tmp.deleteOnExit();
        Files.write(tmp.toPath(), "<p>test</p>".getBytes("UTF-8"));
        Jsoup.parse(tmp, "THIS-CHARSET-DOES-NOT-EXIST", "http://example.com");
    }

    // parse(InputStream, …) ---------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void parseInputStream_NullStream_Throws() throws IOException {
        Jsoup.parse((InputStream) null, "UTF-8", "http://example.com");
    }

    @Test(expected = IOException.class)
    public void parseInputStream_InvalidCharset_Throws() throws IOException {
        InputStream in = new ByteArrayInputStream("<p>test</p>".getBytes("UTF-8"));
        Jsoup.parse(in, "BAD-CHARSET", "http://example.com");
    }

    // parse(URL, timeout) -----------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void parseUrl_NullUrl_Throws() throws IOException {
        Jsoup.parse((URL) null, 1000);
    }

    @Test(expected = MalformedURLException.class)
    public void parseUrl_UnsupportedProtocol_Throws() throws IOException {
        Jsoup.parse(new URL("ftp://example.com/file.txt"), 1000);
    }

    @Test(expected = SocketTimeoutException.class)
    public void parseUrl_Timeout_Throws() throws IOException {
        // Non‑routable address forces a timeout.
        Jsoup.parse(new URL("http://10.255.255.1/"), 100);
    }

    // clean(..) ---------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void clean_NullHtml_Throws() {
        Jsoup.clean(null, Whitelist.basic());
    }

    @Test(expected = NullPointerException.class)
    public void clean_NullWhitelist_Throws() {
        Jsoup.clean("<p>test</p>", (Whitelist) null);
    }

    // isValid(..) -------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void isValid_NullHtml_Throws() {
        Jsoup.isValid(null, Whitelist.basic());
    }

    @Test(expected = NullPointerException.class)
    public void isValid_NullWhitelist_Throws() {
        Jsoup.isValid("<p>test</p>", null);
    }

    // connect(String) ---------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void connect_NullUrl_Throws() {
        Jsoup.connect(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void connect_UnsupportedProtocol_Throws() {
        Jsoup.connect("ftp://example.com");
    }
}
