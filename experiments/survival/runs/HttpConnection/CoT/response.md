###Test START##
```java
package org.jsoup.helper;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.HttpStatusException;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.parser.Parser;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.IllegalCharsetNameException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link HttpConnection}.  The tests focus on the
 * public API (fluent setters, getters and validation) and on the
 * edge‑case / exception behaviour that can be exercised without a real
 * HTTP server.
 *
 * The test class is deliberately exhaustive – each public method of
 * {@code HttpConnection} has at least one basic test and the methods
 * that perform validation have additional tests for the failure
 * paths.
 */
public class HttpConnectionTest {

    private HttpConnection conn;               // the instance under test
    private static final String TEST_URL = "http://example.com";

    @Before
    public void setUp() {
        // use the public static factory – it returns a Connection that is
        // actually an HttpConnection instance
        conn = (HttpConnection) HttpConnection.connect(TEST_URL);
    }

    /*** -----------------------------------------------------------------
     *  1.  Static factory methods
     * ----------------------------------------------------------------- ***/
    @Test
    public void testStaticConnectString() {
        Connection c = HttpConnection.connect("http://jsoup.org");
        assertNotNull(c);
        assertTrue(c instanceof HttpConnection);
        assertEquals("http://jsoup.org", c.request().url().toString());
    }

    @Test
    public void testStaticConnectURL() throws Exception {
        URL url = new URL("http://jsoup.org");
        Connection c = HttpConnection.connect(url);
        assertNotNull(c);
        assertEquals(url, c.request().url());
    }

    /*** -----------------------------------------------------------------
     *  2.  Fluent configuration methods – they must return the same
     *     instance and correctly update the underlying request object.
     * ----------------------------------------------------------------- ***/
    @Test
    public void testUrlString() {
        conn.url("http://jsoup.org");
        assertEquals("http://jsoup.org", conn.request().url().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUrlStringEmpty() {
        conn.url("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUrlStringMalformed() {
        conn.url("ht!tp://bad_url");
    }

    @Test
    public void testUrlObject() throws Exception {
        URL u = new URL("http://jsoup.org");
        conn.url(u);
        assertEquals(u, conn.request().url());
    }

    @Test
    public void testProxyObject() {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new java.net.InetSocketAddress("localhost", 8080));
        Connection returned = conn.proxy(proxy);
        assertSame(conn, returned);
        assertEquals(proxy, conn.request().proxy());
    }

    @Test
    public void testProxyHostPort() {
        conn.proxy("localhost", 8888);
        Proxy p = conn.request().proxy();
        assertNotNull(p);
        assertEquals("localhost", ((java.net.InetSocketAddress) p.address()).getHostName());
        assertEquals(8888, ((java.net.InetSocketAddress) p.address()).getPort());
    }

    @Test
    public void testUserAgent() {
        conn.userAgent("MyAgent/1.0");
        assertEquals("MyAgent/1.0", conn.request().header(HttpConnection.USER_AGENT));
    }

    @Test
    public void testTimeout() {
        conn.timeout(1234);
        assertEquals(1234, conn.request().timeout());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTimeoutNegative() {
        conn.timeout(-1);
    }

    @Test
    public void testMaxBodySize() {
        conn.maxBodySize(500_000);
        assertEquals(500_000, conn.request().maxBodySize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaxBodySizeNegative() {
        conn.maxBodySize(-10);
    }

    @Test
    public void testFollowRedirects() {
        conn.followRedirects(false);
        assertFalse(conn.request().followRedirects());
    }

    @Test
    public void testReferrer() {
        conn.referrer("http://referrer");
        assertEquals("http://referrer", conn.request().header("Referer"));
    }

    @Test
    public void testMethod() {
        conn.method(Method.POST);
        assertEquals(Method.POST, conn.request().method());
    }

    @Test
    public void testIgnoreHttpErrors() {
        conn.ignoreHttpErrors(true);
        assertTrue(conn.request().ignoreHttpErrors());
    }

    @Test
    public void testIgnoreContentType() {
        conn.ignoreContentType(true);
        assertTrue(conn.request().ignoreContentType());
    }

    @Test
    public void testValidateTLSCertificates() {
        conn.validateTLSCertificates(false);
        assertFalse(conn.request().validateTLSCertificates());
    }

    @Test
    public void testDataKeyValue() {
        conn.data("k1", "v1");
        Connection.KeyVal kv = conn.data("k1");
        assertNotNull(kv);
        assertEquals("k1", kv.key());
        assertEquals("v1", kv.value());
    }

    @Test
    public void testDataKeyFilenameStream() {
        ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{1, 2, 3});
        conn.data("file", "my.txt", is);
        Connection.KeyVal kv = conn.data("file");
        assertNotNull(kv);
        assertTrue(kv.hasInputStream());
        assertEquals("my.txt", kv.value());
    }

    @Test
    public void testDataKeyFilenameStreamContentType() {
        ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{1});
        conn.data("file", "my.txt", is, "text/plain");
        Connection.KeyVal kv = conn.data("file");
        assertEquals("text/plain", kv.contentType());
    }

    @Test
    public void testDataMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a", "1");
        map.put("b", "2");
        conn.data(map);
        assertEquals("1", conn.data("a").value());
        assertEquals("2", conn.data("b").value());
    }

    @Test
    public void testDataVarargsEven() {
        conn.data("x", "10", "y", "20");
        assertEquals("10", conn.data("x").value());
        assertEquals("20", conn.data("y").value());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDataVarargsOddNumber() {
        conn.data("onlyKey");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDataVarargsNullKey() {
        conn.data(null, "value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDataVarargsNullValue() {
        conn.data("key", null);
    }

    @Test
    public void testDataCollection() {
        List<Connection.KeyVal> list = new ArrayList<>();
        list.add(HttpConnection.KeyVal.create("c1", "v1"));
        conn.data(list);
        assertEquals("v1", conn.data("c1").value());
    }

    @Test
    public void testDataLookupMissingKeyReturnsNull() {
        assertNull(conn.data("nonexistent"));
    }

    @Test
    public void testRequestBody() {
        conn.requestBody("my body");
        assertEquals("my body", conn.request().requestBody());
    }

    @Test
    public void testHeader() {
        conn.header("X-Test", "123");
        assertEquals("123", conn.request().header("X-Test"));
    }

    @Test
    public void testHeadersMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("A", "a");
        map.put("B", "b");
        conn.headers(map);
        assertEquals("a", conn.request().header("A"));
        assertEquals("b", conn.request().header("B"));
    }

    @Test
    public void testCookie() {
        conn.cookie("c1", "v1");
        assertEquals("v1", conn.request().cookie("c1"));
    }

    @Test
    public void testCookiesMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("cA", "vA");
        map.put("cB", "vB");
        conn.cookies(map);
        assertEquals("vA", conn.request().cookie("cA"));
        assertEquals("vB", conn.request().cookie("cB"));
    }

    @Test
    public void testParser() {
        Parser p = Parser.xmlParser();
        conn.parser(p);
        assertSame(p, conn.request().parser());
    }

    @Test
    public void testPostDataCharset() {
        conn.postDataCharset("UTF-16");
        assertEquals("UTF-16", conn.request().postDataCharset());
    }

    @Test(expected = IllegalCharsetNameException.class)
    public void testPostDataCharsetUnsupported() {
        conn.postDataCharset("NO_SUCH_CHARSET");
    }

    /*** -----------------------------------------------------------------
     *  3.  Request / Response accessors & execution edge cases
     * ----------------------------------------------------------------- ***/
    @Test
    public void testRequestAccessor() {
        Connection.Request r = conn.request();
        assertNotNull(r);
        assertSame(r, conn.request());
    }

    @Test
    public void testResponseAccessorBeforeExecute() {
        // before any execute() the response is the empty placeholder created in ctor
        Connection.Response resp = conn.response();
        assertNotNull(resp);
    }

    @Test(expected = MalformedURLException.class)
    public void testExecuteUnsupportedProtocol() throws IOException {
        conn.url("ftp://example.com");
        conn.execute(); // should throw because only http/https are allowed
    }

    /*** -----------------------------------------------------------------
     *  4.  KeyVal creation edge cases
     * ----------------------------------------------------------------- ***/
    @Test
    public void testKeyValCreateSimple() {
        HttpConnection.KeyVal kv = HttpConnection.KeyVal.create("k", "v");
        assertEquals("k", kv.key());
        assertEquals("v", kv.value());
    }

    @Test
    public void testKeyValCreateWithStream() {
        ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{9});
        HttpConnection.KeyVal kv = HttpConnection.KeyVal.create("file", "name.txt", is);
        assertEquals("file", kv.key());
        assertEquals("name.txt", kv.value());
        assertSame(is, kv.inputStream());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testKeyValCreateNullKey() {
        HttpConnection.KeyVal.create(null, "v");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testKeyValCreateEmptyKey() {
        HttpConnection.KeyVal.create("", "v");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testKeyValCreateNullValue() {
        HttpConnection.KeyVal.create("k", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testKeyValSetContentTypeEmpty() {
        HttpConnection.KeyVal kv = HttpConnection.KeyVal.create("k", "v");
        kv.contentType("");
    }

    /*** -----------------------------------------------------------------
     *  5.  Miscellaneous helper methods (private/static) – exercised via
     *     reflection where possible.
     * ----------------------------------------------------------------- ***/
    @Test
    public void testEncodeMimeNameNull() throws Exception {
        java.lang.reflect.Method m = HttpConnection.class.getDeclaredMethod("encodeMimeName", String.class);
        m.setAccessible(true);
        Object result = m.invoke(null, (Object) null);
        assertNull(result);
    }

    @Test
    public void testEncodeMimeNameEscapesQuotes() throws Exception {
        java.lang.reflect.Method m = HttpConnection.class.getDeclaredMethod("encodeMimeName", String.class);
        m.setAccessible(true);
        String src = "a\"b\"c";
        String encoded = (String) m.invoke(null, src);
        assertEquals("a%22b%22c", encoded);
    }
}
```
###Test END##