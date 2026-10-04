package org.jsoup;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;

/**
 * JUnit 4 test suite for {@link SerializationException}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors and their effect on {@code getMessage()} and {@code getCause()}.</li>
 *   <li>Edge‑case handling such as {@code null} messages, {@code null} causes,
 *       empty strings, and different types of {@link Throwable}.</li>
 *   <li>Behaviour inherited from {@link RuntimeException} (e.g., {@code toString()}).</li>
 * </ul>
 */
public class SerializationExceptionTest {

    /* -------------------------------------------------
     *  List of public constructors (treated as public methods)
     * -------------------------------------------------
     *
     * 1. SerializationException()
     * 2. SerializationException(String message)
     * 3. SerializationException(Throwable cause)
     * 4. SerializationException(String message, Throwable cause)
     *
     * All constructors delegate to the corresponding {@link RuntimeException}
     * constructors.
     */

    // -----------------------------------------------------------------
    // Basic tests for each constructor
    // -----------------------------------------------------------------

    @Test
    public void testDefaultConstructor() {
        SerializationException ex = new SerializationException();
        assertNull("Default constructor should have null message", ex.getMessage());
        assertNull("Default constructor should have null cause", ex.getCause());
        assertTrue("Should be a RuntimeException", ex instanceof RuntimeException);
    }

    @Test
    public void testMessageConstructor_WithNonNullMessage() {
        String msg = "serialization failed";
        SerializationException ex = new SerializationException(msg);
        assertEquals("Message should be stored", msg, ex.getMessage());
        assertNull("Cause should be null when only message is supplied", ex.getCause());
    }

    @Test
    public void testMessageConstructor_WithNullMessage() {
        SerializationException ex = new SerializationException((String) null);
        assertNull("Message should be null when null is supplied", ex.getMessage());
        assertNull("Cause should be null when only message is supplied", ex.getCause());
    }

    @Test
    public void testCauseConstructor_WithNonNullCause() {
        IOException ioCause = new IOException("IO error");
        SerializationException ex = new SerializationException(ioCause);

        // The message of this constructor is defined as (cause == null ? null : cause.toString())
        assertEquals("Message should be cause.toString()", ioCause.toString(), ex.getMessage());
        assertSame("Cause should be the supplied throwable", ioCause, ex.getCause());
    }

    @Test
    public void testCauseConstructor_WithNullCause() {
        SerializationException ex = new SerializationException((Throwable) null);
        assertNull("Message should be null when cause is null", ex.getMessage());
        assertNull("Cause should be null when null is supplied", ex.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor_BothNonNull() {
        String msg = "combined failure";
        IllegalArgumentException cause = new IllegalArgumentException("bad argument");
        SerializationException ex = new SerializationException(msg, cause);

        assertEquals("Message should be the supplied message", msg, ex.getMessage());
        assertSame("Cause should be the supplied throwable", cause, ex.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor_NullMessage() {
        IOException cause = new IOException("IO problem");
        SerializationException ex = new SerializationException(null, cause);

        assertNull("Message should be null when null is supplied", ex.getMessage());
        assertSame("Cause should be the supplied throwable", cause, ex.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor_NullCause() {
        String msg = "only message";
        SerializationException ex = new SerializationException(msg, null);

        assertEquals("Message should be the supplied message", msg, ex.getMessage());
        assertNull("Cause should be null when null is supplied", ex.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor_BothNull() {
        SerializationException ex = new SerializationException(null, null);
        assertNull("Message should be null", ex.getMessage());
        assertNull("Cause should be null", ex.getCause());
    }

    // -----------------------------------------------------------------
    // Edge‑case tests
    // -----------------------------------------------------------------

    @Test
    public void testMessageConstructor_EmptyString() {
        SerializationException ex = new SerializationException("");
        assertEquals("Empty string message should be preserved", "", ex.getMessage());
        assertNull("Cause should still be null", ex.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor_EmptyMessageAndRuntimeCause() {
        RuntimeException runtimeCause = new RuntimeException("runtime");
        SerializationException ex = new SerializationException("", runtimeCause);

        assertEquals("Message should be the empty string", "", ex.getMessage());
        assertSame("Cause should be the supplied RuntimeException", runtimeCause, ex.getCause());
    }

    @Test
    public void testCauseConstructor_WithCheckedException() {
        // Using a checked exception (IOException) as cause – already covered,
        // but this verifies that the type does not affect behaviour.
        IOException ioCause = new IOException("checked");
        SerializationException ex = new SerializationException(ioCause);
        assertSame("Cause should be the IOException instance", ioCause, ex.getCause());
        assertEquals("Message should be cause.toString()", ioCause.toString(), ex.getMessage());
    }

    @Test
    public void testToStringReflectsMessageAndCause() {
        String msg = "serialization error";
        IllegalStateException cause = new IllegalStateException("state broken");
        SerializationException ex = new SerializationException(msg, cause);

        String toString = ex.toString();
        // The default RuntimeException.toString() prints: ClassName: message
        assertTrue("toString should contain class name", toString.startsWith(SerializationException.class.getName()));
        assertTrue("toString should contain the message", toString.contains(msg));
        // Cause is not printed by default, but we check that no unexpected NPE occurs
        assertNotNull("toString must not be null", toString);
    }

    // -----------------------------------------------------------------
    // Additional sanity check – inheritance hierarchy
    // -----------------------------------------------------------------

    @Test
    public void testInheritance() {
        SerializationException ex = new SerializationException();
        assertTrue("Should be an instance of RuntimeException", ex instanceof RuntimeException);
        assertTrue("Should be an instance of Exception", ex instanceof Exception);
        assertTrue("Should be an instance of Throwable", ex instanceof Throwable);
    }
}
