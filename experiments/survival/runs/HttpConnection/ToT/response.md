###Test START##
```java
package org.jsoup.helper;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.HttpStatusException;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.junit.*;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.charset.IllegalCharsetNameException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link HttpConnection}.
 *
 * <p>The suite covers:
 * <ul>
 *   <li>Static helpers (encodeUrl, encodeMimeName)</li>
 *   <li>Request building – headers, cookies, data, proxy, time‑outs, etc.</li>
 *   <li>KeyVal creation and behaviour</li>
 *   <li>Multipart detection logic</li>
 *   <li>Execution paths using an embedded {@link com.sun.net.httpserver.HttpServer}:
 *       <ul>
 *         <li>simple GET (HTML parsing)</li>
 *         <li>302 redirect handling</li>
 *         <li>error status with {@code ignoreHttpErrors}</li>
 *         <li>unsupported content‑type with {@code ignoreContentType}</li>
 *       </ul>
 *   </li>
 *   <li>Invalid argument handling (malformed URL, bad charset, etc.)</li>
 * </ul>
 *
 * The server is started once for the entire test class and stopped afterwards.
 */
public class HttpConnectionTest {

    /** Embedded HTTP server used by several tests */
    private static com.sun.net.httpserver.HttpServer server;
    /** Base URL (e.g. http://127.0.0.1:12345) */
    private static String baseUrl;

    /** Simple HTML page used by {@code testExecuteGetSimple()} */
    private static final String SIMPLE_HTML = "<html><head><title>Jsoup Test</title></head><body>Hello</body></html>";

    @BeforeClass
    public static void setUpServer() throws Exception {
        server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(0), 0);
        // /simple – returns 200 with HTML body
        server.createContext("/simple", exchange -> {
            byte[] bytes = SIMPLE_HTML.getBytes("UTF-8");
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        // /redirect – redirects to /simple
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/simple");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        // /notfound – returns 404
        server.createContext("/notfound", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });

        // /binary – returns PDF mime type (unsupported unless ignored)
        server.createContext("/binary", exchange -> {
            byte[] dummy = new byte[]{0x25, 0x50, 0x44, 0x46}; // "%PDF"
            exchange.getResponseHeaders().add("Content-Type", "application/pdf");
            exchange.sendResponseHeaders(200, dummy.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(dummy);
            }
        });

        server.setExecutor(null); // default executor
        server.start();
        int port = server.getAddress().getPort();
        baseUrl = "http://127.0.0.1:" + port;
    }

    @AfterClass
    public static void tearDownServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    /* -------------------------------------------------
     *  Static helper methods
     * ------------------------------------------------- */

    @Test
    public void testEncodeUrl_Valid() {
        String raw = "http://example.com/space here.html";
        String encoded = HttpConnection.encodeUrl(raw);
        assertEquals("http://example.com/space%20here.html", encoded);
    }

    @Test
    public void testEncodeUrl_Invalid() {
        // malformed URL – encodeUrl should return the original string, not throw
        String raw = "http:////bad url";
        String encoded = HttpConnection.encodeUrl(raw);
        assertEquals(raw, encoded);
    }

    @Test
    public void testEncodeMimeName() {
        assertEquals("file%22name%22.txt", HttpConnection.encodeMimeName("file\"name\".txt"));
        assertNull(HttpConnection.encodeMimeName(null));
    }

    /* -------------------------------------------------
     *  KeyVal tests
     * ------------------------------------------------- */

    @Test
    public void testKeyValCreateAndToString() {
        Connection.KeyVal kv = HttpConnection.KeyVal.create("k", "v");
        assertEquals("k", kv.key());
        assertEquals("v", kv.value());
        assertEquals("k=v", kv.toString());
    }

    @Test
    public void testKeyValInputStream() throws IOException {
        byte[] data = "hello".getBytes("UTF-8");
        InputStream is = new ByteArrayInputStream(data);
        Connection.KeyVal kv = HttpConnection.KeyVal.create("file", "hello.txt", is);
        assertTrue(kv.hasInputStream());
        assertEquals(is, kv.inputStream());
        // content type can be set
        kv.contentType("text/plain");
        assertEquals("text/plain", kv.contentType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testKeyValCreateWithEmptyKey() {
        HttpConnection.KeyVal.create("", "value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testKeyValCreateWithNullValue() {
        HttpConnection.KeyVal.create("key", null);
    }

    /* -------------------------------------------------
     *  Request building – headers, cookies, data
     * ------------------------------------------------- */

    @Test
    public void testHeaderAddRemoveCaseInsensitive() {
        Connection con = HttpConnection.connect("http://example.com");
        con.header("Accept-Encoding", "gzip");
        con.header("accept-encoding", "deflate"); // should replace previous
        assertEquals("deflate", con.request().header("Accept-Encoding"));
        con.header("User-Agent", "MyAgent");
        assertTrue(con.request().hasHeader("user-agent"));
        con.request().removeHeader("USER-AGENT");
        assertFalse(con.request().hasHeader("User-Agent"));
    }

    @Test
    public void testCookiesAddRemove() {
        Connection con = HttpConnection.connect("http://example.com");
        con.cookie("c1", "v1");
        con.cookie("C2", "v2");
        assertEquals("v1", con.request().cookie("c1"));
        assertTrue(con.request().hasCookie("C2"));
        con.request().removeCookie("c1");
        assertNull(con.request().cookie("c1"));
        Map<String, String> map = con.request().cookies();
        assertEquals(1, map.size());
        assertEquals("v2", map.get("C2"));
    }

    @Test
    public void testDataMethodsAndUrlEncoding() throws Exception {
        // data(key, value) -> should be moved into query string for GET
        Connection con = HttpConnection.connect(baseUrl + "/simple");
        con.method(Method.GET);
        con.data("a", "1");
        con.data("b", "two words");
        con.execute(); // triggers serialiseRequestUrl
        URL finalUrl = con.request().url();
        String query = finalUrl.getQuery();
        assertNotNull(query);
        // order of parameters is preserved
        assertTrue(query.contains("a=1"));
        assertTrue(query.contains("b=two+words"));
    }

    @Test
    public void testNeedsMultipartDetection() throws Exception {
        // private static needsMultipart – invoke via reflection
        Connection.Request req = new HttpConnection.Request();
        // initially false
        assertFalse((Boolean) invokeStatic("needsMultipart", req));

        // add a KeyVal with InputStream
        byte[] dummy = "x".getBytes("UTF-8");
        req.data(HttpConnection.KeyVal.create("file", "f.txt", new ByteArrayInputStream(dummy)));
        assertTrue((Boolean) invokeStatic("needsMultipart", req));
    }

    /** Helper to invoke the private static {@code needsMultipart} method. */
    private static Object invokeStatic(String methodName, Connection.Request req) throws Exception {
        java.lang.reflect.Method m = HttpConnection.class.getDeclaredMethod(methodName, Connection.Request.class);
        m.setAccessible(true);
        return m.invoke(null, req);
    }

    /* -------------------------------------------------
     *  Execution tests using embedded HTTP server
     * ------------------------------------------------- */

    @Test
    public void testExecuteGetSimple() throws IOException {
        Connection con = HttpConnection.connect(baseUrl + "/simple");
        Document doc = con.get(); // GET + parse
        assertEquals("Jsoup Test", doc.title());
        assertEquals("Hello", doc.body().text());
    }

    @Test
    public void testRedirectFollowedByDefault() throws IOException {
        Connection con = HttpConnection.connect(baseUrl + "/redirect");
        Document doc = con.get(); // should follow redirect to /simple
        assertEquals("Jsoup Test", doc.title());
    }

    @Test
    public void testIgnoreHttpErrors() throws IOException {
        Connection con = HttpConnection.connect(baseUrl + "/notfound")
                .ignoreHttpErrors(true);
        Connection.Response resp = con.execute();
        assertEquals(404, resp.statusCode());
    }

    @Test(expected = HttpStatusException.class)
    public void testHttpErrorWithoutIgnore() throws IOException {
        Connection con = HttpConnection.connect(baseUrl + "/notfound");
        con.execute(); // should throw
    }

    @Test
    public void testIgnoreContentType() throws IOException {
        Connection con = HttpConnection.connect(baseUrl + "/binary")
                .ignoreContentType(true);
        Connection.Response resp = con.execute();
        // content type is application/pdf – we only care that no exception is thrown
        assertEquals(200, resp.statusCode());
        assertEquals("application/pdf", resp.contentType());
        // body bytes should match the dummy PDF header
        byte[] body = resp.bodyAsBytes();
        assertArrayEquals(new byte[]{0x25, 0x50, 0x44, 0x46}, body);
    }

    @Test(expected = UnsupportedMimeTypeException.class)
    public void testUnsupportedMimeTypeThrows() throws IOException {
        Connection con = HttpConnection.connect(baseUrl + "/binary");
        con.execute(); // default: should reject application/pdf
    }

    /* -------------------------------------------------
     *  Invalid argument handling
     * ------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testMalformedUrlThrows() {
        HttpConnection.connect("ht!tp://bad").url("ht!tp://bad");
    }

    @Test(expected = IllegalCharsetNameException.class)
    public void testInvalidPostDataCharset() {
        HttpConnection.connect(baseUrl + "/simple")
                .postDataCharset("not-a-charset");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequestBodyOnGETThrowsOnExecute() throws IOException {
        // GET + request body is illegal – execute should validate and throw
        Connection con = HttpConnection.connect(baseUrl + "/simple")
                .method(Method.GET)
                .requestBody("illegal");
        con.execute();
    }

    /* -------------------------------------------------
     *  SSL validation toggle (uses a self‑signed cert – we only verify that the
     *  code path runs without throwing when validation is disabled)
     * ------------------------------------------------- */

    @Test
    public void testValidateTLSCertificatesDisabled() throws Exception {
        // Create a dummy HTTPS URL (we will not actually connect – just ensure the code path
        // that configures the insecure TrustManager runs without error.)
        // Use a non‑existent host to avoid network traffic.
        String httpsUrl = "https://self-signed.invalid/";
        Connection con = HttpConnection.connect(httpsUrl).validateTLSCertificates(false);
        // The following call will attempt to open a connection and fail with UnknownHostException.
        // We catch it and assert that the SSL configuration code was executed (no SSLException).
        try {
            con.execute();
            fail("Expected UnknownHostException");
        } catch (UnknownHostException ignored) {
            // success – the insecure SSL setup did not throw.
        }
    }
}
```
###Test END##