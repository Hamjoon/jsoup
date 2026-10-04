###Test START##
package org.jsoup;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;

/**
 * Unit tests for {@link UncheckedIOException}.
 *
 * <p>This test suite verifies the behavior of the constructor, the {@code ioException()}
 * accessor, handling of {@code null} causes, and inheritance from {@link Error}.</p>
 */
public class UncheckedIOExceptionTest {

    /**
     * Verifies that the constructor correctly stores the supplied {@link IOException}
     * as the cause of the {@link UncheckedIOException}.
     */
    @Test
    public void testConstructorSetsCause() {
        IOException ioCause = new IOException("IO failure");
        UncheckedIOException ue = new UncheckedIOException(ioCause);

        // The cause should be exactly the IOException we passed
        assertSame("Cause should be the same IOException instance", ioCause, ue.getCause());

        // The message of the Error (when created with only a cause) is null
        assertNull("Error message should be null when only a cause is supplied", ue.getMessage());
    }

    /**
     * Checks that {@link UncheckedIOException#ioException()} returns the same
     * {@link IOException} that was supplied to the constructor.
     */
    @Test
    public void testIoExceptionMethod() {
        IOException ioCause = new IOException("Another failure");
        UncheckedIOException ue = new UncheckedIOException(ioCause);

        IOException retrieved = ue.ioException();

        assertSame("ioException() should return the original IOException cause", ioCause, retrieved);
    }

    /**
     * Ensures that a {@code null} cause is handled gracefully.
     * The {@link Throwable#getCause()} method should return {@code null},
     * and calling {@code ioException()} should also return {@code null}.
     */
    @Test
    public void testNullCause() {
        UncheckedIOException ue = new UncheckedIOException(null);

        assertNull("getCause() should be null when constructed with null", ue.getCause());
        assertNull("ioException() should be null when constructed with null", ue.ioException());
    }

    /**
     * Verifies that a subclass of {@link IOException} is preserved correctly.
     */
    @Test
    public void testSubclassIOException() {
        class MyIOException extends IOException {
            private static final long serialVersionUID = 1L;
            MyIOException(String message) {
                super(message);
            }
        }

        MyIOException myCause = new MyIOException("custom subclass");
        UncheckedIOException ue = new UncheckedIOException(myCause);

        // The cause should be the exact subclass instance
        assertSame("Cause should be the MyIOException instance", myCause, ue.getCause());

        // The accessor should return the same subclass instance, not just a generic IOException
        assertSame("ioException() should return the MyIOException instance", myCause, ue.ioException());
        assertTrue("Returned exception should be an instance of MyIOException", ue.ioException() instanceof MyIOException);
    }

    /**
     * Confirms that {@link UncheckedIOException} is a subclass of {@link Error}.
     */
    @Test
    public void testInheritance() {
        IOException ioCause = new IOException("inheritance test");
        UncheckedIOException ue = new UncheckedIOException(ioCause);

        assertTrue("UncheckedIOException should be an instance of Error", ue instanceof Error);
        assertTrue("UncheckedIOException should be an instance of Throwable", ue instanceof Throwable);
    }

    /**
     * Demonstrates that the unchecked exception can be thrown without being declared.
     * The test simply throws and catches the exception to prove compile‑time behavior.
     */
    @Test
    public void testThrowingUncheckedIOException() {
        IOException ioCause = new IOException("throw test");
        try {
            throw new UncheckedIOException(ioCause);
        } catch (UncheckedIOException e) {
            // Verify that the caught exception is the same as the one thrown
            assertSame("Caught exception should be the same instance that was thrown", ioCause, e.getCause());
        }
    }
}
###Test END##