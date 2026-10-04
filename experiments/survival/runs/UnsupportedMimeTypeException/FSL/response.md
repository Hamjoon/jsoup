###Test START##
package org.jsoup;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

/**
 * Comprehensive JUnit4 test suite for {@link UnsupportedMimeTypeException}.
 */
public class UnsupportedMimeTypeExceptionTest {

    @Test
    public void testConstructorAndGetters() {
        String message = "Unsupported MIME type";
        String mime = "image/png";
        String url = "http://example.com/image.png";

        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(message, mime, url);

        // Verify that the superclass message is stored correctly
        assertEquals(message, ex.getMessage());

        // Verify getters return the values supplied to the constructor
        assertEquals(mime, ex.getMimeType());
        assertEquals(url, ex.getUrl());
    }

    @Test
    public void testGettersWithNullValues() {
        String message = "null values test";

        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(message, null, null);

        assertEquals(message, ex.getMessage());
        assertNull("MIME type should be null", ex.getMimeType());
        assertNull("URL should be null", ex.getUrl());
    }

    @Test
    public void testToStringContainsAllDetails() {
        String message = "Bad mime";
        String mime = "application/unknown";
        String url = "https://host/resource";

        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(message, mime, url);
        String toString = ex.toString();

        // The toString should start with the standard IOException toString (class name + message)
        assertTrue("toString should contain the exception class name and message",
                toString.startsWith(IOException.class.getName() + ": " + message));

        // It must also contain the mime type and URL in the expected format
        assertTrue("toString should contain mime type", toString.contains(". Mimetype=" + mime));
        assertTrue("toString should contain URL", toString.contains(", URL=" + url));
    }

    @Test
    public void testToStringWithNullMessageMimeAndUrl() {
        // All arguments null – ensure toString does not throw NPE and formats gracefully
        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(null, null, null);
        String toString = ex.toString();

        // When message is null, super.toString() ends with "null"
        assertTrue(toString.startsWith(IOException.class.getName() + ": null"));
        assertTrue(toString.contains(". Mimetype=null"));
        assertTrue(toString.contains(", URL=null"));
    }

    @Test
    public void testInheritance() {
        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException("msg", "type", "url");
        // The exception must be an instance of IOException
        assertTrue("Exception should be an instance of IOException", ex instanceof IOException);
    }

    @Test
    public void testMultipleInstancesIndependence() {
        UnsupportedMimeTypeException ex1 = new UnsupportedMimeTypeException("msg1", "type1", "url1");
        UnsupportedMimeTypeException ex2 = new UnsupportedMimeTypeException("msg2", "type2", "url2");

        // Ensure each instance keeps its own state
        assertEquals("msg1", ex1.getMessage());
        assertEquals("type1", ex1.getMimeType());
        assertEquals("url1", ex1.getUrl());

        assertEquals("msg2", ex2.getMessage());
        assertEquals("type2", ex2.getMimeType());
        assertEquals("url2", ex2.getUrl());

        // Ensure the toString values differ accordingly
        assertNotEquals(ex1.toString(), ex2.toString());
    }

    @Test
    public void testMessageWithSpecialCharacters() {
        String specialMessage = "Error: \"Unsupported\" mime/type \\ test\nNewLine";
        String mime = "text/plain";
        String url = "http://example.com";

        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(specialMessage, mime, url);

        // Verify that the message is stored exactly as provided
        assertEquals(specialMessage, ex.getMessage());

        // Verify that toString incorporates the special characters correctly
        String toString = ex.toString();
        assertTrue(toString.contains(specialMessage));
        assertTrue(toString.contains(". Mimetype=" + mime));
        assertTrue(toString.contains(", URL=" + url));
    }
}
###Test END##