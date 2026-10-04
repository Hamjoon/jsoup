###Test START##
```java
package org.jsoup.internal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConstrainableInputStream}.
 *
 * The tests cover:
 * <ul>
 *   <li>Factory method {@code wrap}</li>
 *   <li>Reading with size caps, interrupts and timeouts</li>
 *   <li>Behaviour when the underlying stream throws {@link SocketTimeoutException}</li>
 *   <li>Reading to a {@link ByteBuffer} with both local and global caps</li>
 *   <li>Reset handling (mark/reset semantics)</li>
 *   <li>Timeout handling via {@code timeout(...)} and the private {@code expired()}</li>
 * </ul>
 */
public class ConstrainableInputStreamTest {

    private static final int DEFAULT_BUFFER = 32 * 1024; // same as ConstrainableInputStream.DefaultSize
    private static final byte[] SAMPLE_DATA = new byte[1024]; // 1 KiB of deterministic data

    static {
        for (int i = 0; i < SAMPLE_DATA.length; i++) {
            SAMPLE_DATA[i] = (byte) (i % 256);
        }
    }

    @Before
    public void setUp() {
        // Make sure the test thread is not in an interrupted state before each test.
        Thread.interrupted(); // clears the flag
    }

    @After
    public void tearDown() {
        // Ensure we leave the thread clean for the next test.
        Thread.interrupted();
    }

    /** Helper to create a fresh ConstrainableInputStream with the given maxSize. */
    private ConstrainableInputStream newStream(int maxSize) {
        InputStream base = new ByteArrayInputStream(SAMPLE_DATA);
        return ConstrainableInputStream.wrap(base, DEFAULT_BUFFER, maxSize);
    }

    /** Helper InputStream that throws a SocketTimeoutException on the first read call. */
    private static class TimeoutThrowingInputStream extends InputStream {
        private final InputStream delegate;
        private boolean firstRead = true;

        TimeoutThrowingInputStream(InputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            if (firstRead) {
                firstRead = false;
                throw new SocketTimeoutException("simulated timeout");
            }
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (firstRead) {
                firstRead = false;
                throw new SocketTimeoutException("simulated timeout");
            }
            return delegate.read(b, off, len);
        }
    }

    /** --------------------------------------------------------------------- */
    /** 1. wrap() tests */
    /** --------------------------------------------------------------------- */

    @Test
    public void testWrapReturnsSameInstanceWhenAlreadyConstrainable() {
        ConstrainableInputStream cis = newStream(100);
        ConstrainableInputStream wrapped = ConstrainableInputStream.wrap(cis, 64, 200);
        assertSame("wrap should return the original instance when already a ConstrainableInputStream",
                cis, wrapped);
    }

    @Test
    public void testWrapCreatesNewInstanceForPlainInputStream() {
        InputStream plain = new ByteArrayInputStream(SAMPLE_DATA);
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(plain, 64, 200);
        assertNotNull(cis);
        assertTrue(cis instanceof ConstrainableInputStream);
    }

    /** --------------------------------------------------------------------- */
    /** 2. read(byte[],int,int) – basic capped behavior */
    /** --------------------------------------------------------------------- */

    @Test
    public void testReadRespectsGlobalCap() throws IOException {
        int globalCap = 100; // bytes
        ConstrainableInputStream cis = newStream(globalCap);
        byte[] buf = new byte[200]; // request more than cap

        int read = cis.read(buf, 0, buf.length);
        assertEquals("Should read only up to the global cap", globalCap, read);

        // subsequent read should signal EOF (-1) because cap exhausted
        assertEquals(-1, cis.read(buf, 0, 10));
    }

    @Test
    public void testReadDecrementsRemainingAfterEachRead() throws IOException {
        ConstrainableInputStream cis = newStream(10);
        byte[] buf = new byte[4];
        assertEquals(4, cis.read(buf, 0, 4));
        assertEquals(4, cis.read(buf, 0, 4));
        // 8 bytes consumed, 2 remaining
        assertEquals(2, cis.read(buf, 0, 4));
        // cap exhausted
        assertEquals(-1, cis.read(buf, 0, 4));
    }

    /** --------------------------------------------------------------------- */
    /** 3. read – interrupt handling */
    /** --------------------------------------------------------------------- */

    @Test
    public void testReadReturnsMinusOneWhenThreadInterrupted() throws IOException {
        ConstrainableInputStream cis = newStream(0); // uncapped
        Thread.currentThread().interrupt(); // set interrupt flag

        byte[] buf = new byte[10];
        int result = cis.read(buf, 0, buf.length);
        assertEquals("Interrupted read must return -1", -1, result);
        // Subsequent reads stay -1 (interrupted flag latched)
        assertEquals(-1, cis.read(buf, 0, buf.length));
    }

    /** --------------------------------------------------------------------- */
    /** 4. read – timeout handling */
    /** --------------------------------------------------------------------- */

    @Test(expected = SocketTimeoutException.class)
    public void testReadThrowsSocketTimeoutExceptionWhenExpired() throws IOException {
        ConstrainableInputStream cis = newStream(0);
        // Set a timeout of 1 ms and artificially start time far in the past so that it is already expired.
        cis.timeout(System.nanoTime() - 10_000_000L, 1); // 10 ms ago, timeout = 1 ms

        byte[] buf = new byte[10];
        cis.read(buf, 0, buf.length); // should throw
    }

    @Test
    public void testReadHandlesUnderlyingSocketTimeoutExceptionReturningZero() throws IOException {
        // Underlying stream throws SocketTimeoutException on first read; ConstrainableInputStream should catch and return 0.
        InputStream throwing = new TimeoutThrowingInputStream(new ByteArrayInputStream(SAMPLE_DATA));
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(throwing, DEFAULT_BUFFER, 0);
        byte[] buf = new byte[10];
        int result = cis.read(buf, 0, buf.length);
        assertEquals("When underlying stream throws SocketTimeoutException, read should return 0", 0, result);
        // Next read should succeed normally.
        int normalRead = cis.read(buf, 0, buf.length);
        assertTrue("Subsequent read after timeout should succeed", normalRead > 0);
    }

    /** --------------------------------------------------------------------- */
    /** 5. readToByteBuffer – local cap smaller than global cap */
    /** --------------------------------------------------------------------- */

    @Test
    public void testReadToByteBufferRespectsLocalCap() throws IOException {
        // Global cap is large (unlimited), but we ask for only the first 50 bytes.
        ConstrainableInputStream cis = newStream(0);
        ByteBuffer buffer = cis.readToByteBuffer(50);
        assertEquals(50, buffer.remaining());

        // Verify content matches the source data.
        byte[] expected = new byte[50];
        System.arraycopy(SAMPLE_DATA, 0, expected, 0, 50);
        byte[] actual = new byte[50];
        buffer.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testReadToByteBufferStopsAtGlobalCapEvenIfLocalCapIsHigher() throws IOException {
        // Global cap = 100 bytes, request local cap = 200 (higher). Should stop at 100.
        ConstrainableInputStream cis = newStream(100);
        ByteBuffer buffer = cis.readToByteBuffer(200);
        assertEquals(100, buffer.remaining());

        byte[] expected = new byte[100];
        System.arraycopy(SAMPLE_DATA, 0, expected, 0, 100);
        byte[] actual = new byte[100];
        buffer.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testReadToByteBufferUnlimitedWhenMaxZero() throws IOException {
        ConstrainableInputStream cis = newStream(0);
        ByteBuffer buffer = cis.readToByteBuffer(0); // 0 means unlimited
        assertEquals(SAMPLE_DATA.length, buffer.remaining());

        byte[] actual = new byte[SAMPLE_DATA.length];
        buffer.get(actual);
        assertArrayEquals(SAMPLE_DATA, actual);
    }

    /** --------------------------------------------------------------------- */
    /** 6. reset() – mark/reset interaction */
    /** --------------------------------------------------------------------- */

    @Test
    public void testResetRestoresRemainingBasedOnMark() throws IOException {
        // Use a small buffer to force internal buffering.
        ConstrainableInputStream cis = newStream(20);
        byte[] buf = new byte[5];

        // Read 5 bytes, mark position, read another 5, then reset.
        assertEquals(5, cis.read(buf, 0, 5));
        cis.mark(10);
        assertEquals(5, cis.read(buf, 0, 5)); // 10 bytes consumed total

        // At this point remaining should be 10 (20 - 10)
        // Reset should bring remaining back to maxSize - markpos (20 - 5 = 15)
        cis.reset();

        // After reset we should be able to read up to the remaining (15) bytes.
        int totalRead = 0;
        while (cis.read(buf, 0, buf.length) != -1) {
            totalRead += buf.length;
        }
        // The initial 5 bytes before the mark are still available, plus the 5 after the mark,
        // plus the remaining 10 bytes = 20 total.
        assertEquals(20, totalRead);
    }

    /** --------------------------------------------------------------------- */
    /** 7. timeout() and expired() behavior */
    /** --------------------------------------------------------------------- */

    @Test
    public void testTimeoutMethodSetsExpirationCorrectly() throws IOException {
        ConstrainableInputStream cis = newStream(0);
        long start = System.nanoTime();
        cis.timeout(start, 10); // 10 ms timeout

        // Immediately after setting timeout, read should succeed (not expired yet)
        byte[] buf = new byte[5];
        assertTrue("Read should succeed before timeout expires", cis.read(buf, 0, buf.length) > 0);

        // Simulate passage of time by adjusting startTime backwards far enough.
        cis.timeout(start - 20_000_000L, 10); // start 20 ms ago, timeout still 10 ms => already expired

        try {
            cis.read(buf, 0, buf.length);
            fail("Expected SocketTimeoutException after forced expiration");
        } catch (SocketTimeoutException e) {
            // expected
        }
    }

    /** --------------------------------------------------------------------- */
    /** 8. Edge cases – zero/negative parameters */
    /** --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNegativeMaxSize() {
        // Direct call to private constructor via reflection is unnecessary; wrap validates the same condition.
        ConstrainableInputStream.wrap(new ByteArrayInputStream(new byte[0]), DEFAULT_BUFFER, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToByteBufferRejectsNegativeMax() throws IOException {
        ConstrainableInputStream cis = newStream(0);
        cis.readToByteBuffer(-5);
    }

    /** --------------------------------------------------------------------- */
    /** 9. Ensure interrupt flag is latched (subsequent reads stay -1) */
    /** --------------------------------------------------------------------- */

    @Test
    public void testInterruptedFlagIsLatchedAcrossMultipleReads() throws IOException {
        ConstrainableInputStream cis = newStream(0);
        Thread.currentThread().interrupt();

        byte[] buf = new byte[10];
        assertEquals(-1, cis.read(buf, 0, buf.length));
        // Clear the thread interrupt flag to verify that the stream still returns -1
        Thread.interrupted();
        assertEquals(-1, cis.read(buf, 0, buf.length));
    }

    /** --------------------------------------------------------------------- */
    /** 10. Verify that read(byte[]) (inherited) also respects caps via overridden read */
    /** --------------------------------------------------------------------- */

    @Test
    public void testReadByteArrayUsesCappedReadLogic() throws IOException {
        ConstrainableInputStream cis = newStream(7);
        byte[] buf = new byte[10];
        int read = cis.read(buf); // calls BufferedInputStream.read(byte[])
        assertEquals(7, read);
        assertEquals(-1, cis.read(buf));
    }

}
```
###Test END##