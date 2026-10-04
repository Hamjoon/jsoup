package org.jsoup;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;

/**
 * JUnit 4 test suite for {@link UnsupportedMimeTypeException}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction with normal values</li>
 *   <li>Construction with {@code null} or empty arguments</li>
 *   <li>Correct inheritance from {@link IOException}</li>
 *   <li>Getter methods (mime type & URL)</li>
 *   <li>toString() format – normal, null and empty cases</li>
 * </ul>
 */
public class UnsupportedMimeTypeExceptionTest {

    // -----------------------------------------------------------------
    // Helper constants – used by several test methods
    // -----------------------------------------------------------------
    private static final String MESSAGE = "Unsupported MIME type";
    private static final String MIME_TYPE = "application/unknown";
    private static final String URL = "http://example.com/resource";

    // -----------------------------------------------------------------
    // 1. Constructor & getters – normal (non‑null, non‑empty) values
    // -----------------------------------------------------------------
    @Test
    public void testConstructorAndGetters_normalValues() {
        UnsupportedMimeTypeException ex =
                new UnsupportedMimeTypeException(MESSAGE, MIME_TYPE, URL);

        // Verify inheritance
        assertTrue("Exception should be an IOException", ex instanceof IOException);

        // Verify message stored in super
        assertEquals("Message should be preserved", MESSAGE, ex.getMessage());

        // Verify fields via getters
        assertEquals("MIME type getter failed", MIME_TYPE, ex.getMimeType());
        assertEquals("URL getter failed", URL, ex.getUrl());
    }

    // -----------------------------------------------------------------
    // 2. Constructor & getters – null arguments
    // -----------------------------------------------------------------
    @Test
    public void testConstructorAndGetters_nullValues() {
        UnsupportedMimeTypeException ex =
                new UnsupportedMimeTypeException(null, null, null);

        // Null message is allowed – super.getMessage() returns null
        assertNull("Message should be null", ex.getMessage());

        // Getters should return null as supplied
        assertNull("MIME type should be null", ex.getMimeType());
        assertNull("URL should be null", ex.getUrl());
    }

    // -----------------------------------------------------------------
    // 3. Constructor & getters – empty strings
    // -----------------------------------------------------------------
    @Test
    public void testConstructorAndGetters_emptyStrings() {
        UnsupportedMimeTypeException ex =
                new UnsupportedMimeTypeException("", "", "");

        assertEquals("Empty message should be preserved", "", ex.getMessage());
        assertEquals("Empty MIME type should be preserved", "", ex.getMimeType());
        assertEquals("Empty URL should be preserved", "", ex.getUrl());
    }

    // -----------------------------------------------------------------
    // 4. toString() – typical case (non‑null values)
    // -----------------------------------------------------------------
    @Test
    public void testToString_normalValues() {
        UnsupportedMimeTypeException ex =
                new UnsupportedMimeTypeException(MESSAGE, MIME_TYPE, URL);

        String str = ex.toString();

        // The super.toString() part contains the class name and message
        assertTrue("toString() should contain class name", str.contains(UnsupportedMimeTypeException.class.getName()));
        assertTrue("toString() should contain original message", str.contains(MESSAGE));

        // The overridden part must be present exactly as specified
        String expectedSuffix = ". Mimetype=" + MIME_TYPE + ", URL=" + URL;
        assertTrue("toString() should end with the MIME and URL suffix",
                str.endsWith(expectedSuffix));
    }

    // -----------------------------------------------------------------
    // 5. toString() – null MIME type and URL
    // -----------------------------------------------------------------
    @Test
    public void testToString_nullValues() {
        UnsupportedMimeTypeException ex =
                new UnsupportedMimeTypeException(MESSAGE, null, null);

        String str = ex.toString();

        // Null values are stringified as “null”
        String expectedSuffix = ". Mimetype=null, URL=null";
        assertTrue("toString() should handle null fields", str.endsWith(expectedSuffix));
    }

    // -----------------------------------------------------------------
    // 6. toString() – empty strings
    // -----------------------------------------------------------------
    @Test
    public void testToString_emptyStrings() {
        UnsupportedMimeTypeException ex =
                new UnsupportedMimeTypeException(MESSAGE, "", "");

        String str = ex.toString();

        String expectedSuffix = ". Mimetype=, URL=";
        assertTrue("toString() should correctly handle empty strings", str.endsWith(expectedSuffix));
    }

    // -----------------------------------------------------------------
    // 7. Serializability sanity check – the class extends IOException (which is Serializable)
    // -----------------------------------------------------------------
    @Test
    public void testSerializationRoundTrip() throws Exception {
        UnsupportedMimeTypeException original =
                new UnsupportedMimeTypeException(MESSAGE, MIME_TYPE, URL);

        // Serialize
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(bos);
        out.writeObject(original);
        out.flush();
        byte[] bytes = bos.toByteArray();

        // Deserialize
        java.io.ObjectInputStream in = new java.io.ObjectInputStream(
                new java.io.ByteArrayInputStream(bytes));
        Object deserialized = in.readObject();

        assertTrue("Deserialized object should be of same type",
                deserialized instanceof UnsupportedMimeTypeException);
        UnsupportedMimeTypeException ex = (UnsupportedMimeTypeException) deserialized;

        // Verify fields survived the round‑trip
        assertEquals(MESSAGE, ex.getMessage());
        assertEquals(MIME_TYPE, ex.getMimeType());
        assertEquals(URL, ex.getUrl());
    }
}
