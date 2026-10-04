package org.jsoup;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.FileNotFoundException;

/**
 * JUnit 4 test suite for {@link UncheckedIOException}.
 *
 * The tests cover:
 *   • basic constructor behaviour,
 *   • the {@code ioException()} accessor,
 *   • edge cases such as a {@code null} cause and subclass causes,
 *   • inheritance from {@code java.lang.Error},
 *   • independence of multiple instances.
 */
public class UncheckedIOExceptionTest {

    /** Helper to create a simple IOException with a custom message. */
    private IOException newIOException(String message) {
        return new IOException(message);
    }

    /** 1. Basic constructor – the supplied IOException must become the cause. */
    @Test
    public void testConstructorStoresCause() {
        IOException io = newIOException("basic test");
        UncheckedIOException ue = new UncheckedIOException(io);

        // The cause stored in the Error must be the same instance.
        assertSame("Cause should be the same object passed to constructor", io, ue.getCause());

        // The class must also be an instance of Error.
        assertTrue("UncheckedIOException must be an Error", ue instanceof Error);
    }

    /** 2. ioException() must return exactly the cause supplied at construction time. */
    @Test
    public void testIoExceptionReturnsCause() {
        IOException io = newIOException("ioException test");
        UncheckedIOException ue = new UncheckedIOException(io);

        assertSame("ioException() should return the original IOException", io, ue.ioException());
    }

    /** 3. Passing null as the cause should be allowed; ioException() must then return null. */
    @Test
    public void testConstructorWithNullCause() {
        UncheckedIOException ue = new UncheckedIOException(null);

        // getCause() should be null and ioException() must safely cast and return null.
        assertNull("Cause should be null when constructor receives null", ue.getCause());
        assertNull("ioException() should return null when cause is null", ue.ioException());
    }

    /** 4. Subclass of IOException (e.g., FileNotFoundException) must be preserved. */
    @Test
    public void testConstructorWithSubclassCause() {
        FileNotFoundException fnf = new FileNotFoundException("file not found");
        UncheckedIOException ue = new UncheckedIOException(fnf);

        // The cause must be exactly the subclass instance.
        assertSame("Cause should be the FileNotFoundException instance", fnf, ue.getCause());

        // ioException() must return the same subclass instance (no down‑casting loss).
        assertSame("ioException() should return the same FileNotFoundException", fnf, ue.ioException());
    }

    /** 5. Verify that the object is an instance of java.lang.Error. */
    @Test
    public void testErrorInheritance() {
        IOException io = newIOException("inheritance test");
        UncheckedIOException ue = new UncheckedIOException(io);

        assertTrue("UncheckedIOException must extend Error", ue instanceof Error);
        assertTrue("UncheckedIOException must be assignable to Throwable", ue instanceof Throwable);
    }

    /** 6. Ensure multiple instances keep independent causes (no static leakage). */
    @Test
    public void testIndependentInstances() {
        IOException io1 = newIOException("first");
        IOException io2 = newIOException("second");

        UncheckedIOException ue1 = new UncheckedIOException(io1);
        UncheckedIOException ue2 = new UncheckedIOException(io2);

        assertNotSame("Instances must be distinct objects", ue1, ue2);
        assertNotSame("Causes must be distinct objects", ue1.getCause(), ue2.getCause());

        assertSame("ue1 should return its own cause", io1, ue1.ioException());
        assertSame("ue2 should return its own cause", io2, ue2.ioException());
    }
}
