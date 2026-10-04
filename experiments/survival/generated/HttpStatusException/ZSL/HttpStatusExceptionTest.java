package org.jsoup;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link HttpStatusException}.
 *
 * These tests cover:
 * <ul>
 *   <li>Construction and getter methods</li>
 *   <li>Inheritance hierarchy</li>
 *   <li>String representation via {@link HttpStatusException#toString()}</li>
 *   <li>Edge‑case handling (null message, null URL, extreme status codes)</li>
 * </ul>
 */
public class HttpStatusExceptionTest {

    /**
     * Verify that the constructor correctly stores the supplied values and that the
     * getters return exactly those values.
     */
    @Test
    public void testConstructorAndGetters() {
        String message = "Not Found";
        int status = 404;
        String url = "http://example.com/missing";

        HttpStatusException ex = new HttpStatusException(message, status, url);

        // getters
        assertEquals("Status code should match the one passed to the constructor", status, ex.getStatusCode());
        assertEquals("URL should match the one passed to the constructor", url, ex.getUrl());

        // IOException's message handling
        assertEquals("Exception message should be the one supplied", message, ex.getMessage());

        // verify the inheritance chain
        assertTrue("HttpStatusException should be an instance of IOException", ex instanceof java.io.IOException);
    }

    /**
     * Ensure that {@code toString()} concatenates the super string with the status
     * and URL in the exact format defined by the class.
     */
    @Test
    public void testToStringWithMessageAndUrl() {
        String message = "Internal Server Error";
        int status = 500;
        String url = "https://api.example.com/endpoint";

        HttpStatusException ex = new HttpStatusException(message, status, url);

        // Throwable.toString() yields "className: message"
        String expectedSuper = ex.getClass().getName() + ": " + message;
        String expected = expectedSuper + ". Status=" + status + ", URL=" + url;

        assertEquals("toString() should include super string, status and URL", expected, ex.toString());
    }

    /**
     * Verify the behaviour when the message supplied to the constructor is {@code null}.
     * {@link Throwable#toString()} omits the colon and message in this case.
     */
    @Test
    public void testToStringWithNullMessage() {
        String message = null;
        int status = 401;
        String url = "https://secure.example.com/login";

        HttpStatusException ex = new HttpStatusException(message, status, url);

        // When message is null, Throwable.toString() returns just the class name
        String expectedSuper = ex.getClass().getName();
        String expected = expectedSuper + ". Status=" + status + ", URL=" + url;

        assertEquals("toString() should handle a null message correctly", expected, ex.toString());
    }

    /**
     * Verify that a {@code null} URL does not cause a {@code NullPointerException}
     * and is rendered as the literal string {@code "null"} in the output.
     */
    @Test
    public void testToStringWithNullUrl() {
        String message = "Bad Request";
        int status = 400;
        String url = null;

        HttpStatusException ex = new HttpStatusException(message, status, url);

        String expectedSuper = ex.getClass().getName() + ": " + message;
        String expected = expectedSuper + ". Status=" + status + ", URL=" + url; // url is null → "null"

        assertEquals("toString() should render a null URL as the string \"null\"", expected, ex.toString());
    }

    /**
     * Test a range of status codes, including edge values, to ensure they are stored
     * and reported correctly.
     */
    @Test
    public void testStatusCodeBoundaries() {
        int[] boundaryCodes = {0, -1, Integer.MAX_VALUE};

        for (int code : boundaryCodes) {
            String msg = "Status " + code;
            String url = "http://example.com/status/" + code;

            HttpStatusException ex = new HttpStatusException(msg, code, url);

            assertEquals("Getter should return the exact status code supplied", code, ex.getStatusCode());
            assertEquals("URL getter should return the exact URL supplied", url, ex.getUrl());

            // Validate toString for each boundary case
            String expectedSuper = ex.getClass().getName() + ": " + msg;
            String expected = expectedSuper + ". Status=" + code + ", URL=" + url;
            assertEquals("toString() should correctly embed boundary status code " + code, expected, ex.toString());
        }
    }

    /**
     * Ensure that the exception is serializable (inherited from {@link java.io.IOException})
     * by writing it to a byte array and reading it back.
     */
    @Test
    public void testSerializationRoundTrip() throws Exception {
        String message = "Service Unavailable";
        int status = 503;
        String url = "https://service.example.com";

        HttpStatusException original = new HttpStatusException(message, status, url);

        // Serialize
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(baos)) {
            out.writeObject(original);
        }

        // Deserialize
        HttpStatusException deserialized;
        try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(
                new java.io.ByteArrayInputStream(baos.toByteArray()))) {
            deserialized = (HttpStatusException) in.readObject();
        }

        // Verify state after round‑trip
        assertEquals("Message should survive serialization", message, deserialized.getMessage());
        assertEquals("Status code should survive serialization", status, deserialized.getStatusCode());
        assertEquals("URL should survive serialization", url, deserialized.getUrl());
        assertEquals("toString() result should be identical after serialization", original.toString(), deserialized.toString());
    }
}
