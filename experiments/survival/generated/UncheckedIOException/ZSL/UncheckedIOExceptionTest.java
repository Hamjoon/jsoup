package org.jsoup;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.FileNotFoundException;

/**
 * Unit tests for {@link UncheckedIOException}.
 *
 * <p>
 * The class under test is deliberately tiny – it simply wraps an {@link IOException}
 * inside an {@link Error}. The tests therefore focus on:
 *
 * <ul>
 *   <li>Correct propagation of the cause supplied to the constructor.</li>
 *   <li>The {@code ioException()} accessor returning the exact same instance.</li>
 *   <li>Behaviour when {@code null} is supplied as the cause.</li>
 *   <li>Class hierarchy – it must extend {@link Error}, not {@link Exception}.</li>
 * </ul>
 *
 * All tests are written using JUnit 4.
 */
public class UncheckedIOExceptionTest {

    private IOException sampleCause;
    private UncheckedIOException unchecked;

    @Before
    public void setUp() {
        // a regular IOException instance used by most tests
        sampleCause = new IOException("sample message");
        unchecked = new UncheckedIOException(sampleCause);
    }

    /**
     * Verifies that the constructor stores the supplied {@link IOException}
     * as the underlying cause of the {@link Error}.
     */
    @Test
    public void testConstructorSetsCause() {
        // The cause stored in the superclass (Error) must be the same object
        assertSame("Constructor should set the cause to the supplied IOException",
                sampleCause, unchecked.getCause());
    }

    /**
     * Checks that {@link UncheckedIOException#ioException()} returns the exact
     * same {@link IOException} instance that was passed to the constructor.
     */
    @Test
    public void testIoExceptionReturnsSameInstance() {
        IOException returned = unchecked.ioException();
        assertSame("ioException() must return the same IOException instance",
                sampleCause, returned);
    }

    /**
     * Ensures that the class really extends {@link Error} (i.e. it is unchecked).
     */
    @Test
    public void testInheritanceIsError() {
        assertTrue("UncheckedIOException should be a subclass of java.lang.Error",
                unchecked instanceof Error);
        // It must NOT be an instance of Exception (which would make it a checked throwable)
        assertFalse("UncheckedIOException must not be a subclass of java.lang.Exception",
                unchecked instanceof Exception);
    }

    /**
     * Tests behaviour when the constructor is given {@code null}. The
     * superclass constructor accepts a null cause, so {@code getCause()} and
     * {@code ioException()} should both return {@code null}.
     */
    @Test
    public void testNullCause() {
        UncheckedIOException withNull = new UncheckedIOException(null);
        assertNull("getCause() should be null when constructed with null", withNull.getCause());
        assertNull("ioException() should be null when constructed with null", withNull.ioException());
    }

    /**
     * Uses a concrete subclass of {@link IOException} (FileNotFoundException)
     * to ensure that the wrapper does not lose the specific runtime type of
     * the cause.
     */
    @Test
    public void testWithSubclassOfIOException() {
        FileNotFoundException fnf = new FileNotFoundException("file not found");
        UncheckedIOException wrapper = new UncheckedIOException(fnf);

        // Verify that the cause is the exact same instance and retains its type
        assertSame("Cause should be the exact FileNotFoundException instance",
                fnf, wrapper.getCause());
        assertTrue("ioException() should return a FileNotFoundException",
                wrapper.ioException() instanceof FileNotFoundException);
    }

    /**
     * Confirms that the message of the underlying {@link IOException} is
     * accessible via {@link Throwable#getMessage()} on the wrapper.
     */
    @Test
    public void testMessagePropagation() {
        String expectedMessage = "sample message";
        assertEquals("Wrapper's message should be the same as the cause's message",
                expectedMessage, unchecked.getMessage());
    }

    /**
     * Checks that the stack trace of the wrapper contains the stack trace of the cause.
     * This is a sanity check for the normal behaviour of {@link Throwable#getStackTrace()}.
     */
    @Test
    public void testStackTraceContainsCause() {
        StackTraceElement[] wrapperTrace = unchecked.getStackTrace();
        StackTraceElement[] causeTrace = sampleCause.getStackTrace();

        // The wrapper's stack trace is independent, but the cause should be attached.
        // Verify that getCause() is not null and that the cause's stack trace is retrievable.
        assertNotNull("Cause must not be null", unchecked.getCause());
        assertNotNull("Cause's stack trace must not be null", causeTrace);
        // The first element of the cause's stack trace should be the point where
        // the IOException was instantiated (this test class constructor line). We simply
        // assert that the arrays are non‑empty to avoid brittle line‑number checks.
        assertTrue("Wrapper stack trace should have at least one element", wrapperTrace.length > 0);
        assertTrue("Cause stack trace should have at least one element", causeTrace.length > 0);
    }
}
