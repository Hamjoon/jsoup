package org.jsoup;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit 4 tests for {@link UnsupportedMimeTypeException}.
 */
public class UnsupportedMimeTypeExceptionTest {

    /**
     * Verify that the constructor correctly stores the message, mime type and URL,
     * and that the getters return the exact same values.
     */
    @Test
    public void testConstructorAndGetters() {
        String message = "Unsupported mime type encountered";
        String mime = "application/unknown";
        String url = "http://example.com/resource";

        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(message, mime, url);

        // Message is stored in the superclass (IOException)
        assertEquals(message, ex.getMessage());

        // Custom fields
        assertEquals(mime, ex.getMimeType());
        assertEquals(url, ex.getUrl());
    }

    /**
     * The {@code toString()} implementation should contain the superclass string
     * (which includes the exception class name and message) as well as the mime
     * type and URL values.
     */
    @Test
    public void testToStringContainsAllInformation() {
        String message = "Bad mime";
        String mime = "image/xyz";
        String url = "https://host/path";

        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(message, mime, url);
        String str = ex.toString();

        // Superclass toString format: "org.jsoup.UnsupportedMimeTypeException: Bad mime"
        assertTrue("toString should start with the superclass representation",
                str.startsWith("org.jsoup.UnsupportedMimeTypeException: " + message));

        // The custom part must be appended exactly as defined in the class
        assertTrue("toString should contain mime type", str.contains(". Mimetype=" + mime));
        assertTrue("toString should contain URL", str.contains(", URL=" + url));
    }

    /**
     * Ensure that the exception really extends {@link IOException} and can be
     * caught as such.
     */
    @Test
    public void testInheritanceAndCatching() {
        String message = "inheritance test";
        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(message, "text/plain", "http://test");

        // Direct instanceof check
        assertTrue("Should be an instance of IOException", ex instanceof IOException);

        // Verify catching as IOException works
        try {
            throw ex;
        } catch (IOException caught) {
            assertSame("Caught exception should be the same instance that was thrown", ex, caught);
            assertEquals(message, caught.getMessage());
        }
    }

    /**
     * Passing {@code null} for mime type and URL should be handled gracefully;
     * getters must return {@code null} and {@code toString()} must include the
     * literal string "null" for those fields.
     */
    @Test
    public void testNullValues() {
        String message = "null fields test";
        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(message, null, null);

        assertNull("Mime type should be null", ex.getMimeType());
        assertNull("URL should be null", ex.getUrl());

        String str = ex.toString();
        // The custom part should display "null" for both values
        assertTrue(str.contains(". Mimetype=null"));
        assertTrue(str.contains(", URL=null"));
    }

    /**
     * When the message supplied to the constructor is {@code null}, the
     * superclass {@code Throwable.toString()} omits the colon and message.
     * The overridden {@code toString()} should still append the mime and URL
     * information correctly.
     */
    @Test
    public void testNullMessage() {
        String mime = "application/json";
        String url = "https://api.example.com/data";

        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(null, mime, url);
        String str = ex.toString();

        // Superclass toString when message is null is just the class name
        assertTrue("Superclass part should be just the class name without colon",
                str.startsWith("org.jsoup.UnsupportedMimeTypeException"));

        // Ensure it does NOT contain a colon after the class name
        assertFalse("Should not contain a colon when message is null", str.contains(": "));

        // Custom part must still be present
        assertTrue(str.contains(". Mimetype=" + mime));
        assertTrue(str.contains(", URL=" + url));
    }
}
