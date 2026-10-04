package org.jsoup;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link HttpStatusException}.
 *
 * These tests cover:
 * <ul>
 *   <li>Public constructors and methods signatures</li>
 *   <li>Normal behaviour of getters and {@code toString()}</li>
 *   <li>Edge‑case handling such as {@code null} arguments and extreme status codes</li>
 * </ul>
 */
public class HttpStatusExceptionTest {

    /* ----------------------------------------------------------------------
     *  Public methods (including constructor) extracted from HttpStatusException
     * ----------------------------------------------------------------------
     *
     * 1. public HttpStatusException(String message, int statusCode, String url)
     * 2. public int getStatusCode()
     * 3. public String getUrl()
     * 4. @Override public String toString()
     *
     * ---------------------------------------------------------------------- */

    /** Basic constructor test – verifies that all fields are stored correctly. */
    @Test
    public void testConstructorAndGetters_NormalValues() {
        String message = "Not Found";
        int status = 404;
        String url = "http://example.com/missing";

        HttpStatusException ex = new HttpStatusException(message, status, url);

        assertEquals("Message should be stored in the superclass", message, ex.getMessage());
        assertEquals("Status code getter", status, ex.getStatusCode());
        assertEquals("URL getter", url, ex.getUrl());
    }

    /** Edge case – {@code null} message. */
    @Test
    public void testConstructor_NullMessage() {
        String message = null;
        int status = 500;
        String url = "http://example.com/error";

        HttpStatusException ex = new HttpStatusException(message, status, url);

        assertNull("Message should be null", ex.getMessage());
        assertEquals(status, ex.getStatusCode());
        assertEquals(url, ex.getUrl());
    }

    /** Edge case – {@code null} URL. */
    @Test
    public void testConstructor_NullUrl() {
        String message = "Service Unavailable";
        int status = 503;
        String url = null;

        HttpStatusException ex = new HttpStatusException(message, status, url);

        assertEquals(message, ex.getMessage());
        assertEquals(status, ex.getStatusCode());
        assertNull("URL should be null", ex.getUrl());
    }

    /** Edge case – negative status code (should be accepted as is). */
    @Test
    public void testConstructor_NegativeStatusCode() {
        String message = "Custom Error";
        int status = -1;
        String url = "http://example.com/negative";

        HttpStatusException ex = new HttpStatusException(message, status, url);

        assertEquals(status, ex.getStatusCode());
        assertEquals(message, ex.getMessage());
        assertEquals(url, ex.getUrl());
    }

    /** Edge case – very large status code (Integer.MAX_VALUE). */
    @Test
    public void testConstructor_MaxIntStatusCode() {
        String message = "Huge Status";
        int status = Integer.MAX_VALUE;
        String url = "http://example.com/max";

        HttpStatusException ex = new HttpStatusException(message, status, url);

        assertEquals(status, ex.getStatusCode());
        assertEquals(message, ex.getMessage());
        assertEquals(url, ex.getUrl());
    }

    /** Verify that {@code toString()} concatenates the superclass string with status and URL. */
    @Test
    public void testToString_ContainsAllInformation() {
        String message = "Forbidden";
        int status = 403;
        String url = "http://example.com/forbidden";

        HttpStatusException ex = new HttpStatusException(message, status, url);
        String str = ex.toString();

        // Expected format: "org.jsoup.HttpStatusException: Forbidden. Status=403, URL=http://example.com/forbidden"
        assertTrue("toString should start with the superclass representation",
                str.startsWith(HttpStatusException.class.getName() + ": " + message));
        assertTrue("toString should contain status code", str.contains("Status=" + status));
        assertTrue("toString should contain URL", str.contains("URL=" + url));
    }

    /** {@code toString()} when the message is {@code null}. */
    @Test
    public void testToString_NullMessage() {
        String message = null;
        int status = 418;
        String url = "http://example.com/teapot";

        HttpStatusException ex = new HttpStatusException(message, status, url);
        String str = ex.toString();

        // When message is null, Throwable.toString() returns just the class name.
        assertTrue("toString should start with class name only when message is null",
                str.startsWith(HttpStatusException.class.getName()));
        assertTrue("toString should still contain status code", str.contains("Status=" + status));
        assertTrue("toString should still contain URL", str.contains("URL=" + url));
    }
}
