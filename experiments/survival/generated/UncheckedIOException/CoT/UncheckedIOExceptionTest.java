package org.jsoup;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.FileNotFoundException;

/**
 * JUnit‑4 test suite for {@link UncheckedIOException}.
 * Covers normal behaviour, edge cases and exception handling.
 */
public class UncheckedIOExceptionTest {

    /* -------------------------------------------------
     *  Basic functionality tests
     * ------------------------------------------------- */

    @Test
    public void testConstructorStoresCause() {
        IOException io = new IOException("boom");
        UncheckedIOException ue = new UncheckedIOException(io);

        // The cause stored in the Error must be exactly the IOException we passed
        assertSame("The cause should be the same instance passed to the constructor",
                io, ue.getCause());
    }

    @Test
    public void testIoExceptionReturnsCause() {
        IOException io = new IOException("io error");
        UncheckedIOException ue = new UncheckedIOException(io);

        // ioException() must return the same object
        assertSame("ioException() should return the original IOException",
                io, ue.ioException());
    }

    @Test
    public void testInheritanceIsError() {
        IOException io = new IOException();
        UncheckedIOException ue = new UncheckedIOException(io);

        assertTrue("UncheckedIOException must be an instance of java.lang.Error",
                ue instanceof Error);
        // also ensure it is NOT a checked Exception
        assertFalse("UncheckedIOException must NOT be a subclass of java.lang.Exception",
                ue instanceof Exception);
    }

    /* -------------------------------------------------
     *  Edge‑case tests
     * ------------------------------------------------- */

    @Test
    public void testConstructorWithNullCause() {
        // Passing null is legal – the cause should be null and ioException() must return null
        UncheckedIOException ue = new UncheckedIOException(null);
        assertNull("getCause() should be null when constructed with null", ue.getCause());
        assertNull("ioException() should return null when cause is null", ue.ioException());
    }

    @Test
    public void testIoExceptionWithSpecificSubclass() {
        FileNotFoundException fnf = new FileNotFoundException("missing file");
        UncheckedIOException ue = new UncheckedIOException(fnf);

        // The method must return the exact subclass instance
        assertSame("ioException() must return the same FileNotFoundException instance",
                fnf, ue.ioException());
        assertTrue("Returned exception should be an instance of FileNotFoundException",
                ue.ioException() instanceof FileNotFoundException);
    }

    @Test
    public void testIOExceptionMessagePreserved() {
        String msg = "custom message";
        IOException io = new IOException(msg);
        UncheckedIOException ue = new UncheckedIOException(io);

        // Verify the message is still accessible via the cause
        assertEquals("The message of the wrapped IOException must be preserved",
                msg, ue.getCause().getMessage());
        assertEquals("ioException() should expose the same message",
                msg, ue.ioException().getMessage());
    }
}
