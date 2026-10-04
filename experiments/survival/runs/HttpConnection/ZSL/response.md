###Test START##
```java
package org.jsoup.helper;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.Before;
import org.junit.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit4 tests for {@link HttpConnection}.
 * <p>
 * The tests avoid real network access by supplying a custom {@link URLStreamHandler}
 * that returns a mock {@link HttpURLConnection}.  The mock connection implements the
 * minimal behaviour required by {@link HttpConnection.Response#execute(Connection.Request)}.
 * </p>
 */
public class HttpConnectionTest {

    private static final String TEST_URL = "http://example.com/";
    private static final String TEST_HTML = "<html><head><title>Test</title></head><body>Hello</body></html>";
    private static final String REDIRECT_LOCATION = "http://example.com/redirected";

    private MockHttpURLConnection mockConn;
    private URL testUrl;

    @Before
    public void setUp() throws Exception {
        // Create a mock connection that we can configure per test.
        mockConn = new MockHttpURLConnection();

        // Use a URL with a custom handler that returns the mock connection.
        testUrl = new URL(null, TEST_URL, new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) {
                return mockConn;
            }
        });
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    /** Configures the mock connection for a successful 200 OK HTML response. */
    private void configureOkResponse() {
        mockConn.setResponseCode(200);
        mockConn.setResponseMessage("OK");
        mockConn.setContentType("text/html; charset=UTF-8");
        mockConn.setInputStream(new ByteArrayInputStream(TEST_HTML.getBytes()));
        mockConn.setHeader("Content-Type", "text/html; charset=UTF-8");
    }

    /** Configures the mock connection for a redirect (302) response. */
    private void configureRedirectResponse() {
        mockConn.setResponseCode(302);
        mockConn.setResponseMessage("Found");
        mockConn.setHeader("Location", REDIRECT_LOCATION);
        mockConn.setHeader("Set-Cookie", "session=abc123; Path=/");
    }

    /** Configures the mock connection for a 404 error response. */
    private void configureNotFoundResponse() {
        mockConn.setResponseCode(404);
        mockConn.setResponseMessage("Not Found");
        mockConn.setContentType("text/html");
        mockConn.setInputStream(new ByteArrayInputStream("<h1>Not Found</h1>".getBytes()));
    }

    /** Configures the mock connection for an unsupported MIME type response. */
    private void configureUnsupportedMimeResponse() {
        mockConn.setResponseCode(200);
        mockConn.setResponseMessage("OK");
        mockConn.setContentType("application/octet-stream");
        mockConn.setInputStream(new ByteArrayInputStream(new byte[0]));
    }

    // -------------------------------------------------------------------------
    // Tests for static utility methods
    // -------------------------------------------------------------------------

    @Test
    public void testEncodeUrl_ReplacesSpaces() {
        String raw = "http://example.com/a b c";
        String encoded = HttpConnection.encodeUrl(raw);
        assertEquals("http://example.com/a%20b%20c", encoded);
    }

    @Test
    public void testEncodeMimeName_ReplacesQuotes() {
        assertEquals("file%22name%22.txt", HttpConnection.encodeMimeName("file\"name\".txt"));
        assertNull(HttpConnection.encodeMimeName(null));
    }

    // -------------------------------------------------------------------------
    // Tests for request building (headers, cookies, data, etc.)
    // -------------------------------------------------------------------------

    @Test
    public void testRequestBuilderSetsValues() {
        Connection con = HttpConnection.connect(testUrl)
                .userAgent("MyAgent")
                .referrer("http://referrer")
                .header("X-Custom", "value")
                .cookie("c1", "v1")
                .data("key", "val")
                .requestBody("rawbody")
                .postDataCharset("ISO-8859-1")
                .timeout(1234)
                .maxBodySize(5678)
                .followRedirects(false)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .validateTLSCertificates(false);

        Connection.Request req = con.request();

        assertEquals("MyAgent", req.header("User-Agent"));
        assertEquals("http://referrer", req.header("Referer"));
        assertEquals("value", req.header("X-Custom"));
        assertEquals("v1", req.cookie("c1"));
        assertEquals("val", con.data("key").value());
        assertEquals("rawbody", req.requestBody());
        assertEquals("ISO-8859-1", ((HttpConnection.Request) req).postDataCharset());
        assertEquals(1234, ((HttpConnection.Request) req).timeout());
        assertEquals(5678, ((HttpConnection.Request) req).maxBodySize());
        assertFalse(req.followRedirects());
        assertTrue(((HttpConnection.Request) req).ignoreHttpErrors());
        assertTrue(((HttpConnection.Request) req).ignoreContentType());
        assertFalse(((HttpConnection.Request) req).validateTLSCertificates());
    }

    @Test
    public void testHeaderCaseInsensitivityAndRemoval() {
        HttpConnection.Request req = new HttpConnection.Request();
        req.addHeader("Accept-Encoding", "gzip");
        req.addHeader("accept-encoding", "deflate");
        // The two adds should result in two values (order preserved)
        List<String> vals = req.headers("Accept-Encoding");
        assertEquals(2, vals.size());
        assertTrue(vals.contains("gzip"));
        assertTrue(vals.contains("deflate"));

        // hasHeader should be case‑insensitive
        assertTrue(req.hasHeader("ACCEPT-encoding"));

        // Remove using different case
        req.removeHeader("ACCEPT-ENCODING");
        assertFalse(req.hasHeader("Accept-Encoding"));
    }

    @Test
    public void testCookieOperations() {
        HttpConnection.Request req = new HttpConnection.Request();
        req.cookie("a", "1");
        req.cookie("b", "2");

        assertTrue(req.hasCookie("a"));
        assertEquals("1", req.cookie("a"));
        assertEquals("2", req.cookie("b"));

        req.removeCookie("a");
        assertFalse(req.hasCookie("a"));
        assertTrue(req.hasCookie("b"));
    }

    // -------------------------------------------------------------------------
    // Tests for KeyVal creation and behaviour
    // -------------------------------------------------------------------------

    @Test
    public void testKeyValCreationAndToString() {
        HttpConnection.KeyVal kv = HttpConnection.KeyVal.create("name", "value");
        assertEquals("name", kv.key());
        assertEquals("value", kv.value());
        assertEquals("name=value", kv.toString());

        HttpConnection.KeyVal fileKv = HttpConnection.KeyVal.create("file", "my.txt", new ByteArrayInputStream(new byte[0]));
        assertTrue(fileKv.hasInputStream());
        assertEquals("my.txt", fileKv.value());
    }

    // -------------------------------------------------------------------------
    // Tests for response handling (status, body, parsing, redirects, errors)
    // -------------------------------------------------------------------------

    @Test
    public void testExecuteOkResponseParsesDocument() throws IOException {
        configureOkResponse();

        Connection.Response res = HttpConnection.connect(testUrl).execute();

        assertEquals(200, res.statusCode());
        assertEquals("OK", res.statusMessage());
        assertEquals("text/html; charset=UTF-8", res.contentType());

        Document doc = res.parse();
        assertEquals("Test", doc.title());
        assertEquals("Hello", doc.body().text());
    }

    @Test
    public void testRedirectFollowing() throws IOException {
        // First response is a redirect, second is OK
        configureRedirectResponse();

        // Prepare a second mock connection for the redirected URL
        MockHttpURLConnection secondConn = new MockHttpURLConnection();
        secondConn.setResponseCode(200);
        secondConn.setResponseMessage("OK");
        secondConn.setContentType("text/html");
        secondConn.setInputStream(new ByteArrayInputStream(TEST_HTML.getBytes()));

        // Override the URL handler for the redirected URL to return the second mock
        URL redirectedUrl = new URL(null, REDIRECT_LOCATION, new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) {
                return secondConn;
            }
        });

        // The first connection will redirect to redirectedUrl
        mockConn.setHeader("Location", REDIRECT_LOCATION);

        // Execute with followRedirects = true (default)
        Connection.Response res = HttpConnection.connect(testUrl).execute();

        // The final response should be from the second connection
        assertEquals(200, res.statusCode());
        assertEquals("OK", res.statusMessage());

        // Verify that the request now contains the cookie from the redirect response
        assertEquals("abc123", ((HttpConnection.Request) res.req).cookie("session"));
    }

    @Test
    public void testRedirectNotFollowedWhenDisabled() throws IOException {
        configureRedirectResponse();

        // Disable following redirects
        Connection.Response res = HttpConnection.connect(testUrl)
                .followRedirects(false)
                .execute();

        // Should retain the original 302 status code
        assertEquals(302, res.statusCode());
        assertEquals(REDIRECT_LOCATION, res.header("Location"));
    }

    @Test(expected = HttpStatusException.class)
    public void testHttpErrorThrowsWhenNotIgnored() throws IOException {
        configureNotFoundResponse();

        HttpConnection.connect(testUrl).execute();
    }

    @Test
    public void testHttpErrorDoesNotThrowWhenIgnored() throws IOException {
        configureNotFoundResponse();

        Connection.Response res = HttpConnection.connect(testUrl)
                .ignoreHttpErrors(true)
                .execute();

        assertEquals(404, res.statusCode());
        assertTrue(res.body().contains("Not Found"));
    }

    @Test(expected = UnsupportedMimeTypeException.class)
    public void testUnsupportedMimeThrowsWhenNotIgnored() throws IOException {
        configureUnsupportedMimeResponse();

        HttpConnection.connect(testUrl).execute();
    }

    @Test
    public void testUnsupportedMimeIsIgnoredWhenFlagSet() throws IOException {
        configureUnsupportedMimeResponse();

        Connection.Response res = HttpConnection.connect(testUrl)
                .ignoreContentType(true)
                .execute();

        assertEquals(200, res.statusCode());
    }

    // -------------------------------------------------------------------------
    // Tests for TLS validation disabling (uses HTTPS)
    // -------------------------------------------------------------------------

    @Test
    public void testInsecureTlsConfiguration() throws Exception {
        // Use HTTPS URL with a custom handler that returns a mock HttpsURLConnection.
        MockHttpsURLConnection httpsConn = new MockHttpsURLConnection();
        httpsConn.setResponseCode(200);
        httpsConn.setResponseMessage("OK");
        httpsConn.setContentType("text/html");
        httpsConn.setInputStream(new ByteArrayInputStream(TEST_HTML.getBytes()));

        URL httpsUrl = new URL(null, "https://secure.example.com/", new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) {
                return httpsConn;
            }
        });

        // Execute with TLS validation disabled – should not throw any SSL related exception.
        Connection.Response res = HttpConnection.connect(httpsUrl)
                .validateTLSCertificates(false)
                .execute();

        assertEquals(200, res.statusCode());
        assertNotNull(((HttpsURLConnection) httpsConn).getSSLSocketFactory());
        assertTrue(((HttpsURLConnection) httpsConn).getHostnameVerifier().verify("anyhost", null));
    }

    // -------------------------------------------------------------------------
    // Mock connection implementations
    // -------------------------------------------------------------------------

    /** Minimal mock for {@link HttpURLConnection}. */
    private static class MockHttpURLConnection extends HttpURLConnection {
        private int responseCode;
        private String responseMessage;
        private String contentType;
        private InputStream inputStream;
        private final Map<String, List<String>> requestHeaders = new LinkedHashMap<>();
        private final Map<String, List<String>> responseHeaders = new LinkedHashMap<>();
        private boolean doOutput = false;

        protected MockHttpURLConnection() {
            super(null);
        }

        @Override
        public void disconnect() {}

        @Override
        public boolean usingProxy() { return false; }

        @Override
        public void connect() {}

        // ----- setters used by tests -----
        void setResponseCode(int code) { this.responseCode = code; }
        void setResponseMessage(String msg) { this.responseMessage = msg; }
        void setContentType(String ct) { this.contentType = ct; }
        void setInputStream(InputStream is) { this.inputStream = is; }
        void setHeader(String name, String value) {
            responseHeaders.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        }

        // ----- HttpURLConnection overrides -----
        @Override
        public int getResponseCode() { return responseCode; }

        @Override
        public String getResponseMessage() { return responseMessage; }

        @Override
        public String getContentType() { return contentType; }

        @Override
        public InputStream getInputStream() { return inputStream; }

        @Override
        public InputStream getErrorStream() { return null; }

        @Override
        public void setDoOutput(boolean dooutput) { this.doOutput = dooutput; }

        @Override
        public boolean getDoOutput() { return doOutput; }

        @Override
        public void setRequestMethod(String method) { /* ignore */ }

        @Override
        public String getRequestMethod() { return "GET"; }

        @Override
        public void addRequestProperty(String key, String value) {
            requestHeaders.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        @Override
        public String getHeaderFieldKey(int n) {
            // emulate getHeaderFieldKey based on ordered map
            List<String> keys = new ArrayList<>(responseHeaders.keySet());
            return n < keys.size() ? keys.get(n) : null;
        }

        @Override
        public String getHeaderField(int n) {
            List<String> keys = new ArrayList<>(responseHeaders.keySet());
            if (n < keys.size()) {
                List<String> vals = responseHeaders.get(keys.get(n));
                return vals.isEmpty() ? null : vals.get(0);
            }
            return null;
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return responseHeaders;
        }

        @Override
        public URL getURL() { return new URL("http://example.com/"); }
    }

    /** Minimal mock for {@link HttpsURLConnection} that extends MockHttpURLConnection. */
    private static class MockHttpsURLConnection extends MockHttpURLConnection {
        private SSLSocketFactory sslSocketFactory;
        private HostnameVerifier hostnameVerifier;

        @Override
        public void setSSLSocketFactory(SSLSocketFactory sf) {
            this.sslSocketFactory = sf;
        }

        @Override
        public SSLSocketFactory getSSLSocketFactory() {
            return sslSocketFactory;
        }

        @Override
        public void setHostnameVerifier(HostnameVerifier verifier) {
            this.hostnameVerifier = verifier;
        }

        @Override
        public HostnameVerifier getHostnameVerifier() {
            return hostnameVerifier;
        }
    }
}
```
###Test END##