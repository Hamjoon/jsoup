package org.jsoup;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link UnsupportedMimeTypeException}.
 *
 * Covers:
 *   - normal usage of all public members,
 *   - edge‑case handling of null / empty / whitespace / Unicode / very long values,
 *   - inheritance contract (IOException#getMessage()).
 */
public class UnsupportedMimeTypeExceptionTest {

    // -----------------------------------------------------------------
    // 1️⃣  Basic / typical use‑case tests
    // -----------------------------------------------------------------

    @Test
    public void testConstructorInitialisesFields() {
        String message = "Unsupported MIME type";
        String mime = "application/unknown";
        String url = "http://example.com/resource";

        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(message, mime, url);

        assertEquals(message, ex.getMessage());
        assertEquals(mime, ex.getMimeType());
        assertEquals(url, ex.getUrl());
    }

    @Test
    public void testGetMimeTypeReturnsProvidedValue() {
        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException("msg", "image/xyz", "http://a.b");
        assertEquals("image/xyz", ex.getMimeType());
    }

    @Test
    public void testGetUrlReturnsProvidedValue() {
        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException("msg", "text/plain", "https://host/path");
        assertEquals("https://host/path", ex.getUrl());
    }

    @Test
    public void testToStringContainsAllParts() {
        String message = "Bad mime";
        String mime = "video/xyz";
        String url = "http://site.com/video";

        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(message, mime, url);
        String str = ex.toString();

        // The toString implementation concatenates super.toString() with ". Mimetype=" + mime + ", URL=" + url
        assertTrue(str.contains(message));
        assertTrue(str.contains(". Mimetype=" + mime + ", URL=" + url));
    }

    // -----------------------------------------------------------------
    // 2️⃣  Edge‑case tests
    // -----------------------------------------------------------------

    @Test
    public void testConstructorAcceptsNullValues() {
        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(null, null, null);

        assertNull(ex.getMessage());          // super ctor receives null → getMessage() returns null
        assertNull(ex.getMimeType());
        assertNull(ex.getUrl());

        String str = ex.toString();
        // When concatenating null, Java produces the string "null"
        assertTrue(str.contains("Mimetype=null"));
        assertTrue(str.contains("URL=null"));
    }

    @Test
    public void testConstructorAcceptsEmptyStrings() {
        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException("", "", "");

        assertEquals("", ex.getMessage());
        assertEquals("", ex.getMimeType());
        assertEquals("", ex.getUrl());

        String str = ex.toString();
        // Should contain the literal empty parts after the '=' signs
        assertTrue(str.contains("Mimetype="));
        assertTrue(str.contains(", URL="));
        // Ensure there is no stray "null"
        assertFalse(str.contains("null"));
    }

    @Test
    public void testConstructorWithWhitespaceAndUnicode() {
        String message = "   leading and trailing   ";
        String mime = "\tapplication/😀\n";
        String url = "http://例子.测试/路径";

        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(message, mime, url);

        assertEquals(message, ex.getMessage());
        assertEquals(mime, ex.getMimeType());
        assertEquals(url, ex.getUrl());

        String str = ex.toString();
        assertTrue(str.contains(message));
        assertTrue(str.contains("Mimetype=" + mime + ", URL=" + url));
    }

    @Test
    public void testConstructorWithVeryLongStrings() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10_000; i++) {
            sb.append('a' + (i % 26));
        }
        String longMime = sb.toString();
        String longUrl = "http://" + sb.toString() + ".com";

        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException("big", longMime, longUrl);

        assertEquals(longMime, ex.getMimeType());
        assertEquals(longUrl, ex.getUrl());

        String str = ex.toString();
        assertTrue(str.contains(longMime));
        assertTrue(str.contains(longUrl));
    }

    @Test
    public void testGetMessageFromIOException() {
        String message = "Underlying IO problem";
        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(message, "type", "url");
        assertEquals(message, ex.getMessage());
    }
}
