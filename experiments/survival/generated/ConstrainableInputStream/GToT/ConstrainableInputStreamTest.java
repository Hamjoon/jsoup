package org.jsoup.internal;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit 4 test suite for {@link ConstrainableInputStream}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods and their basic contracts.</li>
 *   <li>Edge‑case handling such as zero/negative limits, thread interruption, and time‑outs.</li>
 *   <li>Exception handling – especially {@link SocketTimeoutException} bubbling up or being swallowed.</li>
 * </ul>
 */
public class ConstrainableInputStreamTest {

    private static final byte[] SAMPLE_DATA = "abcdefghijklmnopqrstuvwxyz".getBytes();

    /** Helper InputStream that throws {@link SocketTimeoutException} on each read. */
    private static class TimeoutInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            throw new SocketTimeoutException("forced timeout");
        }
    }

    /** Helper InputStream that returns a fixed byte value a given number of times and then -1. */
    private static class FixedByteInputStream extends InputStream {
        private final byte value;
        private int remaining;

        FixedByteInputStream(byte value, int length) {
            this.value = value;
            this.remaining = length;
        }

        @Override
        public int read() {
            if (remaining <= 0) return -1;
            remaining--;
            return value & 0xff;
        }
    }

    private InputStream baseStream;

    @Before
    public void setUp() {
        baseStream = new ByteArrayInputStream(SAMPLE_DATA);
    }

    /* -------------------------------------------------------------
     *  1. wrap(...)
     * ------------------------------------------------------------- */

    @Test
    public void testWrapCreatesNewInstance() {
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(baseStream, 64, 10);
        assertNotNull(cis);
        assertTrue(cis instanceof ConstrainableInputStream);
    }

    @Test
    public void testWrapReturnsSameInstanceWhenAlreadyConstrainable() {
        ConstrainableInputStream original = ConstrainableInputStream.wrap(baseStream, 64, 10);
        ConstrainableInputStream wrapped = ConstrainableInputStream.wrap(original, 128, 20);
        assertSame("wrap should return the original instance when it is already a ConstrainableInputStream",
                original, wrapped);
    }

    /* -------------------------------------------------------------
     *  2. read(byte[], int, int)
     * ------------------------------------------------------------- */

    @Test
    public void testReadRespectsMaxSizeCap() throws IOException {
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(baseStream, 32, 5); // cap at 5 bytes
        byte[] buf = new byte[10];
        int read = cis.read(buf, 0, buf.length);
        assertEquals("Should read exactly the capped amount", 5, read);
        // subsequent read must signal EOF (-1) because remaining == 0
        assertEquals(-1, cis.read(buf, 0, buf.length));
    }

    @Test
    public void testReadUnlimitedWhenMaxSizeZero() throws IOException {
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(baseStream, 32, 0); // 0 == infinite
        byte[] buf = new byte[100];
        int read = cis.read(buf, 0, buf.length);
        assertEquals("Should read the whole underlying stream", SAMPLE_DATA.length, read);
        // EOF after full consumption
        assertEquals(-1, cis.read(buf, 0, buf.length));
    }

    @Test
    public void testReadInterruptedThreadReturnsMinusOne() throws IOException {
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(baseStream, 32, 0);
        // set the interrupt flag for the current thread
        Thread.currentThread().interrupt();
        try {
            byte[] buf = new byte[10];
            int read = cis.read(buf, 0, buf.length);
            assertEquals("Interrupted thread should cause read() to return -1", -1, read);
        } finally {
            // clear the interrupt flag for other tests
            Thread.interrupted();
        }
    }

    @Test(expected = SocketTimeoutException.class)
    public void testReadThrowsSocketTimeoutWhenExpired() throws IOException {
        // Use a normal stream but force the timeout to have already expired.
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(baseStream, 32, 0);
        // Set start time far in the past and a tiny timeout.
        cis.timeout(System.nanoTime() - 10_000_000L, 1); // 1 ms timeout, already elapsed
        byte[] buf = new byte[10];
        cis.read(buf, 0, buf.length); // should throw
    }

    @Test
    public void testReadHandlesUnderlyingSocketTimeoutExceptionAndReturnsZero() throws IOException {
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(new TimeoutInputStream(), 32, 10);
        byte[] buf = new byte[10];
        int read = cis.read(buf, 0, buf.length);
        assertEquals("When underlying stream throws SocketTimeoutException, read() should return 0", 0, read);
    }

    /* -------------------------------------------------------------
     *  3. readToByteBuffer(int)
     * ------------------------------------------------------------- */

    @Test
    public void testReadToByteBufferRespectsLocalMax() throws IOException {
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(new ByteArrayInputStream(SAMPLE_DATA), 32, 0);
        ByteBuffer bb = cis.readToByteBuffer(3);
        assertEquals("ByteBuffer length should match the requested max", 3, bb.remaining());
        byte[] expected = new byte[]{'a', 'b', 'c'};
        byte[] actual = new byte[3];
        bb.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testReadToByteBufferUnlimitedWhenMaxZero() throws IOException {
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(new ByteArrayInputStream(SAMPLE_DATA), 32, 0);
        ByteBuffer bb = cis.readToByteBuffer(0); // 0 means unlimited
        assertEquals("ByteBuffer should contain the whole stream when max is 0", SAMPLE_DATA.length, bb.remaining());
        byte[] actual = new byte[SAMPLE_DATA.length];
        bb.get(actual);
        assertArrayEquals(SAMPLE_DATA, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToByteBufferRejectsNegativeMax() throws IOException {
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(baseStream, 32, 0);
        cis.readToByteBuffer(-1);
    }

    /* -------------------------------------------------------------
     *  4. reset()
     * ------------------------------------------------------------- */

    @Test
    public void testResetRestoresRemainingAfterMark() throws IOException {
        // maxSize = 5, mark at start, read 2 bytes, reset, then we should be able to read the full 5 bytes again.
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(new ByteArrayInputStream(SAMPLE_DATA), 32, 5);
        cis.mark(100);               // mark at position 0
        byte[] buf = new byte[2];
        int firstRead = cis.read(buf, 0, buf.length);
        assertEquals(2, firstRead);
        // remaining should now be 3
        cis.reset();                 // should set remaining back to maxSize (5)
        byte[] bufAll = new byte[5];
        int secondRead = cis.read(bufAll, 0, bufAll.length);
        assertEquals(5, secondRead);
        // after consuming the 5 bytes, further reads must return -1
        assertEquals(-1, cis.read(bufAll, 0, 1));
    }

    /* -------------------------------------------------------------
     *  5. timeout(long, long)
     * ------------------------------------------------------------- */

    @Test
    public void testTimeoutConfigurationCausesExpiration() throws IOException, InterruptedException {
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(new ByteArrayInputStream(SAMPLE_DATA), 32, 0);
        // Set a tiny timeout (5 ms) starting now.
        cis.timeout(System.nanoTime(), 5);
        // Sleep a little longer than the timeout.
        Thread.sleep(10);
        byte[] buf = new byte[10];
        try {
            cis.read(buf, 0, buf.length);
            fail("Expected SocketTimeoutException after timeout has elapsed");
        } catch (SocketTimeoutException e) {
            // Expected
        }
    }

    @Test
    public void testTimeoutZeroMeansNoExpiration() throws IOException {
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(new ByteArrayInputStream(SAMPLE_DATA), 32, 0);
        cis.timeout(System.nanoTime(), 0); // zero timeout => disabled
        byte[] buf = new byte[10];
        int read = cis.read(buf, 0, buf.length);
        assertTrue("Read should succeed when timeout is disabled", read > 0);
    }

    /* -------------------------------------------------------------
     *  6. Additional sanity checks
     * ------------------------------------------------------------- */

    @Test
    public void testWrapWithZeroMaxSizeActsAsUnlimited() throws IOException {
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(new ByteArrayInputStream(SAMPLE_DATA), 64, 0);
        byte[] buf = new byte[100];
        int read = cis.read(buf, 0, buf.length);
        assertEquals(SAMPLE_DATA.length, read);
    }

    @Test
    public void testReadReturnsMinusOneWhenCappedAndRemainingZeroBeforeCall() throws IOException {
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(new ByteArrayInputStream(SAMPLE_DATA), 32, 3);
        byte[] buf = new byte[2];
        // Exhaust the 3‑byte cap
        assertEquals(2, cis.read(buf, 0, buf.length));
        assertEquals(1, cis.read(buf, 0, buf.length));
        // Now remaining == 0, further read should instantly return -1
        assertEquals(-1, cis.read(buf, 0, buf.length));
    }
}
