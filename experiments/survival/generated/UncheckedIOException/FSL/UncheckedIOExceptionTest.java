package org.jsoup;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;

/**
 * Unit tests for {@link UncheckedIOException}.
 *
 * <p>These tests verify that the wrapper correctly stores and returns the
 * underlying {@link IOException}, that it behaves correctly when the cause
 * is {@code null}, and that it is an {@link Error} subclass.</p>
 */
public class UncheckedIOExceptionTest {

    @Test
    public void testConstructorStoresCause() {
        IOException io = new IOException("IO error");
        UncheckedIOException uio = new UncheckedIOException(io);

        // The cause stored in the Throwable should be the same instance
        assertSame("The cause should be the same IOException instance", io, uio.getCause());

        // The convenience method must return the same instance
        assertSame("ioException() should return the same IOException instance", io, uio.ioException());

        // The message of the UncheckedIOException (as an Error) is null
        // because we only passed a cause to the super constructor.
        assertNull("Message of UncheckedIOException should be null when only cause is supplied", uio.getMessage());

        // The wrapper must be an instance of Error
        assertTrue("UncheckedIOException must be an instance of Error", uio instanceof Error);
    }

    @Test
    public void testConstructorWithSubclassOfIOException() {
        FileNotFoundException fnf = new FileNotFoundException("file not found");
        UncheckedIOException uio = new UncheckedIOException(fnf);

        // The cause must be the exact same subclass instance
        assertSame("The cause should be the same FileNotFoundException instance", fnf, uio.getCause());

        // ioException() must return the same object and preserve its type
        IOException returned = uio.ioException();
        assertSame("ioException() should return the original FileNotFoundException", fnf, returned);
        assertTrue("Returned exception should be a FileNotFoundException", returned instanceof FileNotFoundException);
    }

    @Test
    public void testConstructorWithNullCause() {
        UncheckedIOException uio = new UncheckedIOException(null);

        // getCause() should be null
        assertNull("Cause should be null when constructor receives null", uio.getCause());

        // ioException() should also return null (casting null is safe)
        assertNull("ioException() should return null when cause is null", uio.ioException());

        // The wrapper still behaves like an Error
        assertTrue("UncheckedIOException with null cause must still be an Error", uio instanceof Error);
    }

    @Test
    public void testThrowingUncheckedIOExceptionDoesNotRequireCatchOrDeclare() {
        // This test simply verifies that the compiler accepts throwing the wrapper
        // without a throws declaration. The actual throw is performed inside a
        // lambda to keep the test method itself clean.
        Runnable r = () -> {
            throw new UncheckedIOException(new IOException("forced"));
        };

        try {
            r.run();
            fail("Expected UncheckedIOException to be thrown");
        } catch (UncheckedIOException ex) {
            // Expected path – verify that the cause is preserved
            assertNotNull("Cause must not be null", ex.getCause());
            assertEquals("IO error message must be preserved", "forced", ex.getCause().getMessage());
        }
    }
}
