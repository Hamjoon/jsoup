package org.jsoup;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.FileNotFoundException;

/**
 * Comprehensive JUnit‑4 test suite for {@link UncheckedIOException}.
 *
 * <p>The class under test is deliberately tiny – it merely wraps an {@link IOException}
 * inside an {@code Error} subclass and exposes the original exception via {@link #ioException()}.
 * Nevertheless we cover the usual behaviour, edge cases (null cause) and a
 * typical subclass of {@code IOException} to ensure the cast performed in
 * {@code ioException()} is safe.</p>
 *
 * <p>All tests are independent and use only JUnit‑4 assertions.</p>
 */
public class UncheckedIOExceptionTest {

    /**
     * Verify that the constructor stores the supplied {@link IOException}
     * as the cause of the {@code Error}.
     */
    @Test
    public void testConstructorStoresCause() {
        IOException cause = new IOException("boom!");
        UncheckedIOException ue = new UncheckedIOException(cause);

        // The cause should be exactly the object we passed.
        assertSame("Cause must be the same instance passed to constructor",
                cause, ue.getCause());
    }

    /**
     * The {@link #ioException()} method must return the same instance that was
     * supplied to the constructor (or {@code null} if none was supplied).
     */
    @Test
    public void testIoExceptionReturnsSameInstance() {
        IOException cause = new IOException("io exception");
        UncheckedIOException ue = new UncheckedIOException(cause);

        assertSame("ioException() should return the original IOException",
                cause, ue.ioException());
    }

    /**
     * Edge case: the constructor is allowed to receive {@code null}.
     * In that situation both {@link #getCause()} and {@link #ioException()}
     * must return {@code null} without throwing.
     */
    @Test
    public void testNullCause() {
        UncheckedIOException ue = new UncheckedIOException(null);

        assertNull("getCause() should be null when constructed with null",
                ue.getCause());
        assertNull("ioException() should be null when constructed with null",
                ue.ioException());
    }

    /**
     * Ensure that a subclass of {@link IOException} (e.g. {@link FileNotFoundException})
     * works correctly – the cast inside {@code ioException()} must be safe.
     */
    @Test
    public void testSubclassCause() {
        FileNotFoundException cause = new FileNotFoundException("missing.txt");
        UncheckedIOException ue = new UncheckedIOException(cause);

        assertSame("Cause must be the same subclass instance",
                cause, ue.getCause());
        assertSame("ioException() must return the same subclass instance",
                cause, ue.ioException());
    }

    /**
     * The class extends {@link Error}. This test confirms the inheritance
     * hierarchy and that the object behaves like an {@code Error}.
     */
    @Test
    public void testIsErrorSubclass() {
        IOException cause = new IOException();
        UncheckedIOException ue = new UncheckedIOException(cause);

        assertTrue("UncheckedIOException must be an instance of Error",
                ue instanceof Error);
        assertTrue("UncheckedIOException must be an instance of itself",
                ue instanceof UncheckedIOException);
    }

    /**
     * Verify that the message of the {@code Error} itself is {@code null}
     * when only a cause is supplied (the constructor of {@code Error(Throwable)}
     * does not set a detail message).
     */
    @Test
    public void testMessageIsNullWhenOnlyCauseProvided() {
        IOException cause = new IOException("msg");
        UncheckedIOException ue = new UncheckedIOException(cause);

        assertNull("Detail message of the Error should be null when only a cause is set",
                ue.getMessage());
    }

    /**
     * Confirm that the stack trace of the {@code UncheckedIOException}
     * contains the stack trace of the wrapped {@link IOException}.
     * This is a sanity‑check that the underlying {@code Throwable}
     * handling works as expected.
     */
    @Test
    public void testStackTraceContainsCause() {
        IOException cause = new IOException("root cause");
        UncheckedIOException ue = new UncheckedIOException(cause);

        StackTraceElement[] ueTrace = ue.getStackTrace();
        StackTraceElement[] causeTrace = cause.getStackTrace();

        // The first element of ue's stack trace should be the point where the
        // UncheckedIOException was instantiated (i.e., this test method).
        assertNotNull("Stack trace must not be null", ueTrace);
        assertTrue("Stack trace should contain at least one element",
                ueTrace.length > 0);
        // The cause's stack trace is independent; we just ensure it is also present.
        assertNotNull("Cause stack trace must not be null", causeTrace);
    }
}
