package org.jsoup;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for {@link HttpStatusException}.
 */
public class HttpStatusExceptionTest {

    @Test
    public void testConstructorAndGetters_NormalValues() {
        String message = "Not Found";
        int status = 404;
        String url = "http://example.com/missing";

        HttpStatusException ex = new HttpStatusException(message, status, url);

        // Verify that the superclass (IOException) stores the message correctly
        assertEquals(message, ex.getMessage());

        // Verify getters
        assertEquals(status, ex.getStatusCode());
        assertEquals(url, ex.getUrl());
    }

    @Test
    public void testConstructorAndGetters_NullUrl() {
        String message = "Service Unavailable";
        int status = 503;
        String url = null;

        HttpStatusException ex = new HttpStatusException(message, status, url);

        assertEquals(message, ex.getMessage());
        assertEquals(status, ex.getStatusCode());
        assertNull(ex.getUrl());
    }

    @Test
    public void testConstructorAndGetters_EmptyUrl() {
        String message = "OK";
        int status = 200;
        String url = "";

        HttpStatusException ex = new HttpStatusException(message, status, url);

        assertEquals(message, ex.getMessage());
        assertEquals(status, ex.getStatusCode());
        assertEquals("", ex.getUrl());
    }

    @Test
    public void testConstructorAndGetters_NegativeStatusCode() {
        String message = "Custom Error";
        int status = -1;
        String url = "http://example.com";

        HttpStatusException ex = new HttpStatusException(message, status, url);

        assertEquals(message, ex.getMessage());
        assertEquals(status, ex.getStatusCode());
        assertEquals(url, ex.getUrl());
    }

    @Test
    public void testConstructorAndGetters_ZeroStatusCode() {
        String message = "Zero Status";
        int status = 0;
        String url = "http://example.com/zero";

        HttpStatusException ex = new HttpStatusException(message, status, url);

        assertEquals(message, ex.getMessage());
        assertEquals(status, ex.getStatusCode());
        assertEquals(url, ex.getUrl());
    }

    @Test
    public void testConstructor_NullMessage() {
        String message = null;
        int status = 418; // I'm a teapot
        String url = "http://example.com/teapot";

        HttpStatusException ex = new HttpStatusException(message, status, url);

        // getMessage should be null
        assertNull(ex.getMessage());

        // toString should not contain ": null"
        String toString = ex.toString();
        assertTrue("toString should start with class name", toString.startsWith(HttpStatusException.class.getName()));
        assertFalse("toString should not contain ': null'", toString.contains(": null"));
        assertTrue(toString.contains(". Status=" + status));
        assertTrue(toString.contains(", URL=" + url));
    }

    @Test
    public void testToString_Format_WithMessage() {
        String message = "Forbidden";
        int status = 403;
        String url = "https://secure.example.com";

        HttpStatusException ex = new HttpStatusException(message, status, url);
        String expectedPrefix = HttpStatusException.class.getName() + ": " + message;
        String expected = expectedPrefix + ". Status=" + status + ", URL=" + url;

        assertEquals(expected, ex.toString());
    }

    @Test
    public void testToString_Format_WithEmptyMessage() {
        String message = "";
        int status = 204;
        String url = "http://example.com/no-content";

        HttpStatusException ex = new HttpStatusException(message, status, url);
        // When message is empty, Throwable.toString still appends ": " (because empty string is not null)
        String expectedPrefix = HttpStatusException.class.getName() + ": " + message;
        String expected = expectedPrefix + ". Status=" + status + ", URL=" + url;

        assertEquals(expected, ex.toString());
    }

    @Test
    public void testToString_IncludesAllParts_EvenWhenUrlIsNull() {
        String message = "Bad Gateway";
        int status = 502;
        String url = null;

        HttpStatusException ex = new HttpStatusException(message, status, url);
        String expected = HttpStatusException.class.getName() + ": " + message
                + ". Status=" + status + ", URL=" + url;

        assertEquals(expected, ex.toString());
    }

    @Test
    public void testInheritance_IsInstanceOfIOException() {
        HttpStatusException ex = new HttpStatusException("Error", 500, "http://example.com");
        assertTrue("HttpStatusException should be an instance of IOException", ex instanceof java.io.IOException);
    }

    @Test
    public void testSerialVersionUID_NotRequiredButClassIsSerializable() {
        // The class does not declare a serialVersionUID, but as it extends IOException (which is Serializable),
        // it should still be serializable. This test simply ensures that serialization does not throw.
        HttpStatusException ex = new HttpStatusException("Serializable test", 200, "http://example.com");
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(bos);
            out.writeObject(ex);
            out.flush();
            byte[] bytes = bos.toByteArray();

            java.io.ObjectInputStream in = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes));
            Object deserialized = in.readObject();

            assertTrue(deserialized instanceof HttpStatusException);
            HttpStatusException deserializedEx = (HttpStatusException) deserialized;
            assertEquals(ex.getMessage(), deserializedEx.getMessage());
            assertEquals(ex.getStatusCode(), deserializedEx.getStatusCode());
            assertEquals(ex.getUrl(), deserializedEx.getUrl());
        } catch (Exception e) {
            fail("Serialization of HttpStatusException should not fail: " + e);
        }
    }
}
