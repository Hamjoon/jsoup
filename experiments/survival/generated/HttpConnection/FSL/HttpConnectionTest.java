package org.jsoup.helper;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.HttpStatusException;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.parser.TokenQueue;
import org.junit.Test;

import javax.net.ssl.HostnameVerifier;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method as ReflectMethod;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;

import static org.junit.Assert.*;

public class HttpConnectionTest {

    /*** -------------------  encodeUrl / encodeMimeName tests ------------------- ***/

    @Test
    public void testEncodeUrl_validUrlWithSpaces() {
        String raw = "http://example.com/a b/c d.html";
        String encoded = HttpConnection.encodeUrl(raw);
        assertEquals("http://example.com/a%20b/c%20d.html", encoded);
    }

    @Test
    public void testEncodeUrl_malformedUrlReturnsOriginal() {
        String raw = "http://[invalid]";
        String encoded = HttpConnection.encodeUrl(raw);
        // encodeUrl catches the exception and returns the original string
        assertEquals(raw, encoded);
    }

    @Test
    public void testEncodeMimeName_nullAndQuotes() {
        assertNull(HttpConnection.encodeMimeName(null));
        assertEquals("a%22b%22c", HttpConnection.encodeMimeName("a\"b\"c"));
    }

    /*** -------------------  Request default values & header handling ------------------- ***/

    @Test
    public void testRequestDefaults() {
        Connection req = new HttpConnection().request();
        assertEquals(Method.GET, req.method());
        // default headers contain Accept-Encoding and User-Agent
        assertTrue(req.hasHeader("Accept-Encoding"));
        assertEquals(HttpConnection.DEFAULT_UA, req.header(HttpConnection.USER_AGENT));
    }

    @Test
    public void testHeaderCaseInsensitivityAndRemoval() {
        Connection req = new HttpConnection().request();
        req.header("X-Test", "value1");
        req.addHeader("x-test", "value2"); // addHeader is exposed via Request (inherits Base)

        // headers() returns first value only, but headers(name) returns all values
        List<String> vals = req.headers("X-Test");
        assertEquals(2, vals.size());
        assertEquals("value1", vals.get(0));
        assertEquals("value2", vals.get(1));

        // header(name) joins with ", "
        assertEquals("value1, value2", req.header("X-Test"));

        // hasHeaderWithValue (case‑insensitive comparison)
        assertTrue(req.hasHeaderWithValue("x-test", "VALUE1"));
        assertTrue(req.hasHeaderWithValue("X-TEST", "value2"));

        // removeHeader should delete the entry irrespective of case
        req.removeHeader("x-TeSt");
        assertFalse(req.hasHeader("X-Test"));
        assertTrue(req.headers().isEmpty());
    }

    @Test
    public void testCookieManagement() {
        Connection req = new HttpConnection().request();
        req.cookie("c1", "v1");
        req.cookie("c2", "v2");

        assertEquals("v1", req.cookie("c1"));
        assertTrue(req.hasCookie("c2"));
        assertFalse(req.hasCookie("c3"));

        req.removeCookie("c1");
        assertNull(req.cookie("c1"));
        assertEquals(1, req.cookies().size());
    }

    @Test
    public void testPostDataCharsetValidation() {
        Connection.Request req = new HttpConnection().request();
        // default charset equals DataUtil.defaultCharset (usually UTF‑8)
        String defaultCs = req.postDataCharset();
        assertNotNull(defaultCs);

        req.postDataCharset("UTF-8");
        assertEquals("UTF-8", req.postDataCharset());

        try {
            req.postDataCharset("UNSUPPORTED-CHARSET");
            fail("Expected IllegalCharsetNameException");
        } catch (IllegalCharsetNameException e) {
            // expected
        }
    }

    /*** -------------------  KeyVal behavior ------------------- ***/

    @Test
    public void testKeyValCreationAndToString() {
        HttpConnection.KeyVal kv = HttpConnection.KeyVal.create("name", "value");
        assertEquals("name", kv.key());
        assertEquals("value", kv.value());
        assertEquals("name=value", kv.toString());

        // with InputStream
        InputStream is = new ByteArrayInputStream(new byte[]{1, 2, 3});
        HttpConnection.KeyVal fileKv = HttpConnection.KeyVal.create("file", "my.txt", is);
        assertTrue(fileKv.hasInputStream());
        assertEquals(is, fileKv.inputStream());

        // content type handling
        fileKv.contentType("image/png");
        assertEquals("image/png", fileKv.contentType());
    }

    /*** -------------------  needsMultipart detection ------------------- ***/

    @Test
    public void testNeedsMultipartTrueWhenInputStreamPresent() {
        Connection.Request req = new HttpConnection().request();
        req.data(HttpConnection.KeyVal.create("field", "value"));
        assertFalse(HttpConnection.needsMultipart(req));

        // add a file part
        InputStream is = new ByteArrayInputStream(new byte[]{0});
        req.data(HttpConnection.KeyVal.create("file", "test.txt", is));
        assertTrue(HttpConnection.needsMultipart(req));
    }

    /*** -------------------  getRequestCookieString (private static) ------------------- ***/

    @Test
    public void testGetRequestCookieString() throws Exception {
        Connection.Request req = new HttpConnection().request();
        req.cookie("a", "1");
        req.cookie("b", "2");

        // Use reflection to call the private static method
        java.lang.reflect.Method m = HttpConnection.Response.class.getDeclaredMethod("getRequestCookieString", Connection.Request.class);
        m.setAccessible(true);
        String cookieHeader = (String) m.invoke(null, req);
        // Order must match insertion order (LinkedHashMap)
        assertEquals("a=1; b=2", cookieHeader);
    }

    /*** -------------------  processResponseHeaders (Set-Cookie parsing) ------------------- ***/

    @Test
    public void testProcessResponseHeadersParsesSetCookie() throws Exception {
        HttpConnection.Response resp = new HttpConnection.Response();
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Set-Cookie", Collections.singletonList("sessionId=abc123; Path=/; HttpOnly"));
        headers.put("Content-Type", Collections.singletonList("text/html"));

        resp.processResponseHeaders(headers);
        assertEquals("abc123", resp.cookie("sessionId"));
        assertEquals("text/html", resp.contentType());
    }

    /*** -------------------  createHeaderMap respects order ------------------- ***/

    @Test
    public void testCreateHeaderMapPreservesOrder() throws Exception {
        MockHttpURLConnection conn = new MockHttpURLConnection(new URL("http://example.com"));
        conn.addHeader("First", "1");
        conn.addHeader("Second", "2");
        conn.addHeader("First", "3"); // duplicate key to test list aggregation

        java.lang.reflect.Method m = HttpConnection.Response.class.getDeclaredMethod("createHeaderMap", HttpURLConnection.class);
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, List<String>> map = (LinkedHashMap<String, List<String>>) m.invoke(null, conn);

        Iterator<Map.Entry<String, List<String>>> it = map.entrySet().iterator();

        Map.Entry<String, List<String>> e1 = it.next();
        assertEquals("First", e1.getKey());
        assertEquals(Arrays.asList("1", "3"), e1.getValue());

        Map.Entry<String, List<String>> e2 = it.next();
        assertEquals("Second", e2.getKey());
        assertEquals(Collections.singletonList("2"), e2.getValue());

        assertFalse(it.hasNext());
    }

    /*** -------------------  encodeUrl(URL) handling of spaces ------------------- ***/

    @Test
    public void testEncodeUrlWithUrlObjectContainingSpace() throws Exception {
        URL raw = new URL("http://example.com/space here/");
        URL encoded = HttpConnection.encodeUrl(raw);
        assertEquals("http://example.com/space%20here/", encoded.toExternalForm());
    }

    /*** -------------------  Response charset handling & body retrieval ------------------- ***/

    @Test
    public void testResponseBodyAndCharset() throws IOException {
        // Simulate a response with a known body
        HttpConnection.Response resp = new HttpConnection.Response();
        resp.bodyStream = new ByteArrayInputStream("Hello".getBytes(Charset.forName("UTF-8")));
        resp.charset = "UTF-8";
        resp.executed = true;

        // first call to body() reads and buffers the stream
        String body = resp.body();
        assertEquals("Hello", body);

        // second call should reuse buffered bytes, not re‑read the stream
        String body2 = resp.body();
        assertEquals("Hello", body2);
    }

    /*** -------------------  Helper Mock HttpURLConnection ------------------- ***/

    /**
     * Minimal mock of HttpURLConnection used only for createHeaderMap test.
     */
    private static class MockHttpURLConnection extends HttpURLConnection {
        private final List<String> keys = new ArrayList<>();
        private final List<String> values = new ArrayList<>();

        protected MockHttpURLConnection(URL u) {
            super(u);
        }

        void addHeader(String name, String value) {
            keys.add(name);
            values.add(value);
        }

        @Override
        public String getHeaderFieldKey(int n) {
            if (n < 0 || n >= keys.size()) return null;
            return keys.get(n);
        }

        @Override
        public String getHeaderField(int n) {
            if (n < 0 || n >= values.size()) return null;
            return values.get(n);
        }

        // The remaining abstract methods are not used in the test and can stay empty.
        @Override public void disconnect() {}
        @Override public boolean usingProxy() { return false; }
        @Override public void connect() throws IOException {}
        @Override public InputStream getInputStream() throws IOException { return null; }
        @Override public OutputStream getOutputStream() throws IOException { return null; }
        @Override public int getResponseCode() throws IOException { return 200; }
        @Override public String getResponseMessage() throws IOException { return "OK"; }
        @Override public String getContentType() { return null; }
    }
}
