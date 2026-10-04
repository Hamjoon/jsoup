###Test START##
```java
package org.jsoup.helper;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.HttpStatusException;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.*;

import java.io.*;
import java.net.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link HttpConnection}.
 *
 * <p>The suite covers:
 * <ul>
 *   <li>Public API surface (method signatures)</li>
 *   <li>Basic happy‑path usage of each public method</li>
 *   <li>Edge‑case and exception handling (nulls, illegal arguments, charset errors, etc.)</li>
 *   <li>Real HTTP interaction using an embedded {@link com.sun.net.httpserver.HttpServer}</li>
 * </ul>
 *
 * <p>All tests are written for JUnit‑4 (no assumptions about JUnit‑5).  The test class can be run
 * directly from an IDE or via Maven/Gradle with the jsoup library on the classpath.
 */
public class HttpConnectionTest {

    /** --------------------------------------------------------------------
     *  1.  List of public methods (including signatures) of HttpConnection
     * -------------------------------------------------------------------- */
    // static factory methods
    //   public static Connection connect(String url)
    //   public static Connection connect(URL url)
    //   private static String encodeUrl(String url)
    //   static URL encodeUrl(URL u)
    //   private static String encodeMimeName(String val)

    // instance builder methods (all return Connection for chaining)
    //   public Connection url(URL url)
    //   public Connection url(String url)
    //   public Connection proxy(Proxy proxy)
    //   public Connection proxy(String host, int port)
    //   public Connection userAgent(String userAgent)
    //   public Connection timeout(int millis)
    //   public Connection maxBodySize(int bytes)
    //   public Connection followRedirects(boolean followRedirects)
    //   public Connection referrer(String referrer)
    //   public Connection method(Method method)
    //   public Connection ignoreHttpErrors(boolean ignoreHttpErrors)
    //   public Connection ignoreContentType(boolean ignoreContentType)
    //   public Connection validateTLSCertificates(boolean value)
    //   public Connection data(String key, String value)
    //   public Connection data(String key, String filename, InputStream inputStream)
    //   public Connection data(String key, String filename, InputStream inputStream, String contentType)
    //   public Connection data(Map<String,String> data)
    //   public Connection data(String... keyvals)
    //   public Connection data(Collection<Connection.KeyVal> data)
    //   public Connection.KeyVal data(String key)
    //   public Connection requestBody(String body)
    //   public Connection header(String name, String value)
    //   public Connection headers(Map<String,String> headers)
    //   public Connection cookie(String name, String value)
    //   public Connection cookies(Map<String,String> cookies)
    //   public Connection parser(Parser parser)
    //   public Document get() throws IOException
    //   public Document post() throws IOException
    //   public Connection.Response execute() throws IOException
    //   public Connection.Request request()
    //   public Connection request(Connection.Request request)
    //   public Connection.Response response()
    //   public Connection response(Connection.Response response)
    //   public Connection postDataCharset(String charset)

    // inner static classes
    //   public static class Request extends HttpConnection.Base<Connection.Request>
    //   public static class Response extends HttpConnection.Base<Connection.Response>
    //   public static class KeyVal implements Connection.KeyVal

    /** --------------------------------------------------------------------
     *  2.  Test fixtures (embedded HTTP server)
     * -------------------------------------------------------------------- */
    private static com.sun.net.httpserver.HttpServer server;
    private static final int PORT = 8888;
    private static final String BASE_URL = "http://localhost:" + PORT;

    @BeforeClass
    public static void startServer() throws IOException {
        server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(PORT), 0);
        // simple echo endpoint for GET
        server.createContext("/echo", exchange -> {
            String response = "echo:" + exchange.getRequestURI().getQuery();
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes("UTF-8"));
            }
        });
        // POST endpoint that returns posted body
        server.createContext("/post", exchange -> {
            InputStream is = exchange.getRequestBody();
            String body = new BufferedReader(new InputStreamReader(is, "UTF-8"))
                    .lines()
                    .reduce("", (a, b) -> a + b);
            String response = "posted:" + body;
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes("UTF-8"));
            }
        });
        // redirect endpoint (301)
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/echo?msg=redirected");
            exchange.sendResponseHeaders(301, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterClass
    public static void stopServer() {
        if (server != null) server.stop(0);
    }

    /** --------------------------------------------------------------------
     *  3.  Basic happy‑path tests for public methods
     * -------------------------------------------------------------------- */

    @Test
    public void testConnectStringCreatesConnectionWithUrl() {
        Connection con = HttpConnection.connect(BASE_URL + "/echo");
        assertNotNull(con);
        assertEquals(BASE_URL + "/echo", con.request().url().toString());
    }

    @Test
    public void testConnectUrlCreatesConnectionWithUrl() throws MalformedURLException {
        URL url = new URL(BASE_URL + "/echo");
        Connection con = HttpConnection.connect(url);
        assertNotNull(con);
        assertEquals(url, con.request().url());
    }

    @Test
    public void testEncodeUrlValid() {
        // space must be %20
        String raw = "http://example.com/a b";
        String encoded = invokePrivateEncodeUrl(raw);
        assertEquals("http://example.com/a%20b", encoded);
    }

    @Test
    public void testEncodeUrlInvalidReturnsOriginal() {
        // malformed URL – encodeUrl should just return the original string
        String raw = "http://[bad address]";
        String encoded = invokePrivateEncodeUrl(raw);
        assertEquals(raw, encoded);
    }

    @Test
    public void testEncodeMimeNameEscapesQuotes() {
        String raw = "my\"file\".txt";
        String encoded = invokePrivateEncodeMimeName(raw);
        assertEquals("my%22file%22.txt", encoded);
    }

    @Test
    public void testKeyValCreateAndToString() {
        Connection.KeyVal kv = HttpConnection.KeyVal.create("name", "value");
        assertEquals("name", kv.key());
        assertEquals("value", kv.value());
        assertEquals("name=value", kv.toString());
    }

    @Test
    public void testKeyValInputStreamRequiresNonNullValue() {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        Connection.KeyVal kv = HttpConnection.KeyVal.create("file", "filename", is);
        assertTrue(kv.hasInputStream());
        assertEquals(is, kv.inputStream());
    }

    @Test
    public void testRequestHeaderAddAndRetrieve() {
        Connection con = HttpConnection.connect(BASE_URL + "/echo");
        con.header("X-Test", "123");
        assertEquals("123", con.request().header("X-Test"));
        assertTrue(con.request().hasHeader("X-Test"));
    }

    @Test
    public void testRequestHeaderCaseInsensitive() {
        Connection con = HttpConnection.connect(BASE_URL + "/echo");
        con.header("X-Case", "abc");
        assertEquals("abc", con.request().header("x-case"));
        assertTrue(con.request().hasHeader("X-CASE"));
    }

    @Test
    public void testRequestRemoveHeader() {
        Connection con = HttpConnection.connect(BASE_URL + "/echo");
        con.header("To-Delete", "yes");
        assertTrue(con.request().hasHeader("To-Delete"));
        con.request().removeHeader("To-Delete");
        assertFalse(con.request().hasHeader("To-Delete"));
    }

    @Test
    public void testRequestCookieAddAndRetrieve() {
        Connection con = HttpConnection.connect(BASE_URL + "/echo");
        con.cookie("c1", "v1");
        assertEquals("v1", con.request().cookie("c1"));
        assertTrue(con.request().hasCookie("c1"));
    }

    @Test
    public void testRequestTimeoutValid() {
        Connection con = HttpConnection.connect(BASE_URL + "/echo");
        con.timeout(5000);
        assertEquals(5000, con.request().timeout());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequestTimeoutNegativeThrows() {
        Connection con = HttpConnection.connect(BASE_URL + "/echo");
        con.timeout(-1);
    }

    @Test
    public void testRequestPostDataCharsetValid() {
        Connection con = HttpConnection.connect(BASE_URL + "/echo");
        con.postDataCharset("UTF-8");
        assertEquals("UTF-8", ((HttpConnection.Request) con.request()).postDataCharset());
    }

    @Test(expected = IllegalCharsetNameException.class)
    public void testRequestPostDataCharsetInvalidThrows() {
        Connection con = HttpConnection.connect(BASE_URL + "/echo");
        con.postDataCharset("no‑such‑charset");
    }

    @Test
    public void testRequestDataVarargsEvenNumber() {
        Connection con = HttpConnection.connect(BASE_URL + "/echo");
        con.data("k1", "v1", "k2", "v2");
        List<Connection.KeyVal> list = new ArrayList<>(con.request().data());
        assertEquals(2, list.size());
        assertEquals("k1", list.get(0).key());
        assertEquals("v1", list.get(0).value());
        assertEquals("k2", list.get(1).key());
        assertEquals("v2", list.get(1).value());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequestDataVarargsOddNumberThrows() {
        Connection con = HttpConnection.connect(BASE_URL + "/echo");
        con.data("onlyKey"); // odd number -> IllegalArgumentException from Validate.isTrue
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequestDataVarargsNullThrows() {
        Connection con = HttpConnection.connect(BASE_URL + "/echo");
        con.data((String[]) null);
    }

    /** --------------------------------------------------------------------
     *  4.  Real HTTP interaction tests (execute, redirect, error handling)
     * -------------------------------------------------------------------- */

    @Test
    public void testExecuteGetReturnsResponseBody() throws IOException {
        Connection con = HttpConnection.connect(BASE_URL + "/echo")
                .data("msg", "hello")
                .method(Method.GET);
        Connection.Response resp = con.execute();
        assertEquals(200, resp.statusCode());
        assertEquals("echo:msg=hello", resp.body().trim());
    }

    @Test
    public void testExecutePostReturnsResponseBody() throws IOException {
        Connection con = HttpConnection.connect(BASE_URL + "/post")
                .data("field", "value")
                .method(Method.POST);
        Connection.Response resp = con.execute();
        assertEquals(200, resp.statusCode());
        assertEquals("posted:field=value", resp.body().trim());
    }

    @Test
    public void testRedirectFollowedWhenEnabled() throws IOException {
        Connection con = HttpConnection.connect(BASE_URL + "/redirect")
                .followRedirects(true)
                .method(Method.GET);
        Document doc = con.get(); // get() triggers execute + parse
        // server redirects to /echo?msg=redirected; we expect that content
        assertTrue(doc.text().contains("redirected"));
    }

    @Test
    public void testRedirectNotFollowedWhenDisabled() throws IOException {
        Connection con = HttpConnection.connect(BASE_URL + "/redirect")
                .followRedirects(false)
                .method(Method.GET);
        Connection.Response resp = con.execute();
        assertEquals(301, resp.statusCode());
        assertEquals(BASE_URL + "/redirect", resp.url().toString());
        // No body expected for 301 in our tiny server
        assertTrue(resp.body().isEmpty());
    }

    @Test(expected = HttpStatusException.class)
    public void testHttpErrorThrowsWhenIgnoreHttpErrorsFalse() throws IOException {
        // create a handler that returns 418 (I'm a teapot)
        server.createContext("/error", exchange -> {
            exchange.sendResponseHeaders(418, -1);
            exchange.close();
        });
        Connection con = HttpConnection.connect(BASE_URL + "/error")
                .ignoreHttpErrors(false) // default, but make explicit
                .method(Method.GET);
        con.execute(); // should throw HttpStatusException
    }

    @Test
    public void testHttpErrorNotThrownWhenIgnoreHttpErrorsTrue() throws IOException {
        // reuse the /error context from previous test
        Connection con = HttpConnection.connect(BASE_URL + "/error")
                .ignoreHttpErrors(true)
                .method(Method.GET);
        Connection.Response resp = con.execute();
        assertEquals(418, resp.statusCode());
    }

    @Test(expected = UnsupportedMimeTypeException.class)
    public void testUnsupportedMimeTypeThrows() throws IOException {
        // handler that returns binary content type
        server.createContext("/binary", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
            exchange.sendResponseHeaders(200, 4);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(new byte[]{0,1,2,3});
            }
        });
        Connection con = HttpConnection.connect(BASE_URL + "/binary")
                .ignoreContentType(false) // default
                .method(Method.GET);
        con.execute(); // should throw UnsupportedMimeTypeException
    }

    /** --------------------------------------------------------------------
     *  5.  Helper methods to invoke private static utilities via reflection
     * -------------------------------------------------------------------- */

    private static String invokePrivateEncodeUrl(String url) {
        try {
            java.lang.reflect.Method m = HttpConnection.class.getDeclaredMethod("encodeUrl", String.class);
            m.setAccessible(true);
            return (String) m.invoke(null, url);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String invokePrivateEncodeMimeName(String val) {
        try {
            java.lang.reflect.Method m = HttpConnection.class.getDeclaredMethod("encodeMimeName", String.class);
            m.setAccessible(true);
            return (String) m.invoke(null, val);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```
###Test END##