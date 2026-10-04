###Test START##
```java
package org.jsoup;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit 4 test cases for {@link UnsupportedMimeTypeException}.
 */
public class UnsupportedMimeTypeExceptionTest {

    /**
     * Verify that the constructor correctly stores the message, mime type and URL,
     * and that the getter methods return the expected values.
     */
    @Test
    public void testConstructorAndGetters_normalValues() {
        String message = "Unsupported MIME type";
        String mime = "application/unknown";
        String url = "http://example.com/resource";

        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(message, mime, url);

        // getters
        assertEquals(mime, ex.getMimeType());
        assertEquals(url, ex.getUrl());

        // inherited IOException#getMessage()
        assertEquals(message, ex.getMessage());
    }

    /**
     * Verify that the class behaves correctly when {@code null} is supplied for
     * the mime type, URL or message. The class does not perform any null checks,
     * so the stored values should be {@code null} and the getters should return
     * {@code null} accordingly.
     */
    @Test
    public void testConstructorAndGetters_nullValues() {
        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(null, null, null);

        assertNull(ex.getMessage());
        assertNull(ex.getMimeType());
        assertNull(ex.getUrl());
    }

    /**
     * Verify that {@link UnsupportedMimeTypeException#toString()} concatenates the
     * superclass {@code toString()} with the mime type and URL information.
     */
    @Test
    public void testToString_containsAllInformation() {
        String message = "Bad MIME";
        String mime = "image/xyz";
        String url = "https://host/path";

        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(message, mime, url);

        String superString = superToString(ex); // helper to get IOException's toString()
        String expected = superString + ". Mimetype=" + mime + ", URL=" + url;

        assertEquals(expected, ex.toString());
    }

    /**
     * Verify that {@code toString()} works when mime type and URL are {@code null}.
     * The string should contain the literal "null" for those parts.
     */
    @Test
    public void testToString_withNullFields() {
        String message = "Null fields test";
        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(message, null, null);

        String superString = superToString(ex);
        String expected = superString + ". Mimetype=null, URL=null";

        assertEquals(expected, ex.toString());
    }

    /**
     * Helper method that reproduces the behavior of {@link IOException#toString()}
     * for the given exception instance. This is needed because {@code super.toString()}
     * is not directly accessible from the test class.
     */
    private String superToString(UnsupportedMimeTypeException ex) {
        // IOException's toString() is defined as:
        //   getClass().getName() + ": " + getMessage()
        // If the message is null, only the class name is returned.
        String className = ex.getClass().getName();
        String message = ex.getMessage();
        return (message != null) ? (className + ": " + message) : className;
    }

    /**
     * Ensure that the exception is an instance of {@link IOException}
     * and that the message passed to the constructor is retrievable via
     * {@link Throwable#getMessage()}.
     */
    @Test
    public void testInheritanceAndMessage() {
        String message = "Inheritance test";
        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(message, "type", "url");

        assertTrue(ex instanceof java.io.IOException);
        assertEquals(message, ex.getMessage());
    }
}
```
###Test END##