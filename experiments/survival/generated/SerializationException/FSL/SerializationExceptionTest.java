package org.jsoup;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for {@link SerializationException}.
 */
public class SerializationExceptionTest {

    @Test
    public void defaultConstructor_shouldHaveNoMessageAndNoCause() {
        SerializationException ex = new SerializationException();
        assertNull("Message should be null", ex.getMessage());
        assertNull("Cause should be null", ex.getCause());
    }

    @Test
    public void messageConstructor_withNonNullMessage_shouldSetMessageOnly() {
        String msg = "serialization failed";
        SerializationException ex = new SerializationException(msg);
        assertEquals("Message should match the supplied value", msg, ex.getMessage());
        assertNull("Cause should be null when not supplied", ex.getCause());
    }

    @Test
    public void messageConstructor_withNullMessage_shouldAllowNullMessage() {
        SerializationException ex = new SerializationException((String) null);
        assertNull("Message should be null when supplied null", ex.getMessage());
        assertNull("Cause should be null when not supplied", ex.getCause());
    }

    @Test
    public void causeConstructor_withNonNullCause_shouldSetCauseAndMessage() {
        Throwable cause = new IllegalArgumentException("illegal argument");
        SerializationException ex = new SerializationException(cause);
        assertEquals("Cause should be the supplied throwable", cause, ex.getCause());
        // RuntimeException(Throwable) uses cause.toString() as the message
        assertEquals("Message should be cause.toString()", cause.toString(), ex.getMessage());
    }

    @Test
    public void causeConstructor_withNullCause_shouldHaveNullMessageAndCause() {
        SerializationException ex = new SerializationException((Throwable) null);
        assertNull("Message should be null when cause is null", ex.getMessage());
        assertNull("Cause should be null when supplied null", ex.getCause());
    }

    @Test
    public void messageAndCauseConstructor_withBothNonNull_shouldSetBoth() {
        String msg = "serialization error";
        Throwable cause = new java.io.IOException("stream closed");
        SerializationException ex = new SerializationException(msg, cause);
        assertEquals("Message should match the supplied value", msg, ex.getMessage());
        assertEquals("Cause should match the supplied throwable", cause, ex.getCause());
    }

    @Test
    public void messageAndCauseConstructor_withNullMessage_shouldAllowNullMessage() {
        Throwable cause = new RuntimeException("runtime problem");
        SerializationException ex = new SerializationException(null, cause);
        assertNull("Message should be null when supplied null", ex.getMessage());
        assertEquals("Cause should be set even if message is null", cause, ex.getCause());
    }

    @Test
    public void messageAndCauseConstructor_withNullCause_shouldSetMessageOnly() {
        String msg = "only message";
        SerializationException ex = new SerializationException(msg, null);
        assertEquals("Message should be set even when cause is null", msg, ex.getMessage());
        assertNull("Cause should be null when supplied null", ex.getCause());
    }

    @Test
    public void serializationException_isUnchecked() {
        // The exception extends RuntimeException, so it is unchecked.
        // Verify that the compiler treats it as unchecked by throwing it without a try/catch.
        // This test will pass if the code compiles; we also assert the type at runtime.
        try {
            throw new SerializationException("unchecked test");
        } catch (RuntimeException e) {
            assertTrue("Caught exception should be an instance of SerializationException", e instanceof SerializationException);
            assertEquals("Message should be preserved", "unchecked test", e.getMessage());
        }
    }
}
