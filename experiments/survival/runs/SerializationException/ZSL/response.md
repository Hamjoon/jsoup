###Test START##
package org.jsoup;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Modifier;

import org.junit.Test;

/**
 * Unit tests for {@link SerializationException}.
 */
public class SerializationExceptionTest {

    /**
     * Verify that the default constructor creates an exception with {@code null}
     * message and {@code null} cause.
     */
    @Test
    public void testDefaultConstructor() {
        SerializationException ex = new SerializationException();
        assertNull("Message should be null", ex.getMessage());
        assertNull("Cause should be null", ex.getCause());
    }

    /**
     * Verify that the constructor accepting only a message stores that
     * message and leaves the cause {@code null}.
     */
    @Test
    public void testMessageConstructor() {
        // non‑null message
        String msg = "Serialization failed";
        SerializationException ex = new SerializationException(msg);
        assertEquals("Message should match the one supplied", msg, ex.getMessage());
        assertNull("Cause should be null", ex.getCause());

        // null message
        SerializationException exNull = new SerializationException((String) null);
        assertNull("Message should be null when null is supplied", exNull.getMessage());
        assertNull("Cause should still be null", exNull.getCause());
    }

    /**
     * Verify that the constructor accepting only a {@link Throwable} sets the
     * cause correctly and the message follows {@code Throwable(Throwable)} contract:
     * <pre>
     *   (cause == null ? null : cause.toString())
     * </pre>
     */
    @Test
    public void testCauseConstructor() {
        // cause is null
        SerializationException exNullCause = new SerializationException((Throwable) null);
        assertNull("Message should be null when cause is null", exNullCause.getMessage());
        assertNull("Cause should be null when supplied null", exNullCause.getCause());

        // cause is non‑null
        IOException ioCause = new IOException("IO error");
        SerializationException ex = new SerializationException(ioCause);
        assertSame("Cause should be the same instance supplied", ioCause, ex.getCause());

        // Message should be cause.toString()
        String expectedMessage = ioCause.toString(); // includes class name + message
        assertEquals("Message should be cause.toString()", expectedMessage, ex.getMessage());
    }

    /**
     * Verify that the constructor accepting both a message and a cause stores
     * them exactly as supplied.
     */
    @Test
    public void testMessageAndCauseConstructor() {
        String msg = "Unable to write element";
        IOException ioCause = new IOException("Stream closed");

        SerializationException ex = new SerializationException(msg, ioCause);
        assertEquals("Message should match the one supplied", msg, ex.getMessage());
        assertSame("Cause should match the one supplied", ioCause, ex.getCause());

        // Both arguments null
        SerializationException exBothNull = new SerializationException((String) null, null);
        assertNull("Message should be null when null is supplied", exBothNull.getMessage());
        assertNull("Cause should be null when null is supplied", exBothNull.getCause());
    }

    /**
     * Ensure that {@link SerializationException} is a {@link RuntimeException}
     * (i.e., unchecked) and that it inherits from {@link Exception}.
     */
    @Test
    public void testInheritance() {
        SerializationException ex = new SerializationException();
        assertTrue("Should be instance of RuntimeException", ex instanceof RuntimeException);
        assertTrue("Should be instance of Exception", ex instanceof Exception);
        assertTrue("Should be instance of Throwable", ex instanceof Throwable);
    }

    /**
     * Verify that the class is declared {@code final}. This is a compile‑time
     * guarantee, but we assert it via reflection to protect against accidental
     * future changes.
     */
    @Test
    public void testClassIsFinal() {
        int modifiers = SerializationException.class.getModifiers();
        assertTrue("SerializationException should be final", Modifier.isFinal(modifiers));
    }

    /**
     * Verify that the exception correctly propagates the stack trace from the
     * cause when both message and cause are supplied.
     */
    @Test
    public void testStackTracePropagation() {
        IOException ioCause = new IOException("Underlying I/O problem");
        SerializationException ex = new SerializationException("Wrapper message", ioCause);

        // The cause's stack trace should be present in the exception's cause
        assertNotNull("Cause stack trace should not be null", ex.getCause().getStackTrace());
        // Ensure that the top element of the cause's stack trace points to this test method
        StackTraceElement[] causeTrace = ex.getCause().getStackTrace();
        assertTrue("Cause stack trace should contain at least one element", causeTrace.length > 0);
        // The first element should be the line where the IOException was instantiated
        assertEquals("First stack trace element class name should be the test class",
                this.getClass().getName(), causeTrace[0].getClassName());
    }
}
###Test END##