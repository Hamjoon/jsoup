###Test START##
```java
package org.jsoup;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * JUnit 4 test suite for {@link SerializationException}.
 * <p>
 * The tests cover all four constructors, including typical usage,
 * edge‑cases (null message, null cause) and verification of the
 * inheritance hierarchy.
 * </p>
 */
public class SerializationExceptionTest {

    /**
     * Test the no‑arg constructor.
     * <ul>
     *   <li>Message should be {@code null}</li>
     *   <li>Cause should be {@code null}</li>
     *   <li>The exception must be a {@link RuntimeException}</li>
     * </ul>
     */
    @Test
    public void testDefaultConstructor() {
        SerializationException ex = new SerializationException();
        assertNull("Message should be null for default constructor", ex.getMessage());
        assertNull("Cause should be null for default constructor", ex.getCause());
        assertTrue("Should be instance of RuntimeException",
                ex instanceof RuntimeException);
    }

    /**
     * Test constructor with a non‑null message.
     */
    @Test
    public void testMessageConstructor() {
        String msg = "Serialization failed";
        SerializationException ex = new SerializationException(msg);
        assertEquals("Message should match the one passed to the constructor", msg, ex.getMessage());
        assertNull("Cause should be null when only message is provided", ex.getCause());
    }

    /**
     * Test constructor with a non‑null cause.
     * The message of the exception should be {@code cause.toString()}.
     */
    @Test
    public void testCauseConstructor() {
        Throwable cause = new IllegalStateException("IO error");
        SerializationException ex = new SerializationException(cause);
        assertEquals("Message should be cause.toString()", cause.toString(), ex.getMessage());
        assertSame("Cause should be the same instance passed to the constructor", cause, ex.getCause());
    }

    /**
     * Test constructor with both message and cause.
     */
    @Test
    public void testMessageAndCauseConstructor() {
        String msg = "Unable to write DOM";
        Throwable cause = new java.io.IOException("Stream closed");
        SerializationException ex = new SerializationException(msg, cause);
        assertEquals("Message should match the one passed to the constructor", msg, ex.getMessage());
        assertSame("Cause should be the same instance passed to the constructor", cause, ex.getCause());
    }

    /**
     * Edge case: pass {@code null} as the message.
     * Expected behaviour: {@code getMessage()} returns {@code null}.
     */
    @Test
    public void testNullMessage() {
        SerializationException ex = new SerializationException((String) null);
        assertNull("Message should be null when null is passed", ex.getMessage());
        assertNull("Cause should be null when only null message is provided", ex.getCause());
    }

    /**
     * Edge case: pass {@code null} as the cause.
     * Expected behaviour: {@code getCause()} returns {@code null}
     * and the message is {@code null}.
     */
    @Test
    public void testNullCause() {
        SerializationException ex = new SerializationException((Throwable) null);
        assertNull("Message should be null when null cause is passed", ex.getMessage());
        assertNull("Cause should be null when null is passed", ex.getCause());
    }

    /**
     * Edge case: non‑null message with {@code null} cause.
     */
    @Test
    public void testMessageAndNullCause() {
        String msg = "Partial failure";
        SerializationException ex = new SerializationException(msg, null);
        assertEquals("Message should be the provided one even if cause is null", msg, ex.getMessage());
        assertNull("Cause should be null when null is provided", ex.getCause());
    }

    /**
     * Edge case: {@code null} message with a non‑null cause.
     * The message is expected to be {@code cause.toString()} (as per
     * {@link Throwable#Throwable(Throwable)} semantics).
     */
    @Test
    public void testNullMessageWithCause() {
        Throwable cause = new RuntimeException("Root problem");
        SerializationException ex = new SerializationException(null, cause);
        assertEquals("Message should be cause.toString() when null message is provided", cause.toString(), ex.getMessage());
        assertSame("Cause should be the same instance passed", cause, ex.getCause());
    }

    /**
     * Verify the inheritance hierarchy explicitly.
     */
    @Test
    public void testInheritance() {
        SerializationException ex = new SerializationException();
        assertTrue("Should be an instance of Exception", ex instanceof Exception);
        assertTrue("Should be an instance of RuntimeException", ex instanceof RuntimeException);
        assertTrue("Should be an instance of Throwable", ex instanceof Throwable);
    }
}
```
###Test END##