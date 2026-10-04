/**
 * JUnit 4 test suite for {@link org.jsoup.internal.ConstrainableInputStream}.
 *
 * The tests cover:
 * <ul>
 *   <li>Factory method {@code wrap}</li>
 *   <li>Reading with and without size caps</li>
 *   <li>Thread‑interrupt handling</li>
 *   <li>Timeout handling (both normal and expired)</li>
 *   <li>Behaviour when the underlying stream throws {@code SocketTimeoutException}</li>
 *   <li>{@code readToByteBuffer} with local caps</li>
 *   <li>Reset logic and remaining byte count</li>
 *   <li>The {@code timeout} configuration method</li>
 * </ul>
 *
 * The tests use simple in‑memory streams and a few custom {@link java.io.InputStream}
 * implementations to simulate exceptional conditions.
 */
package org.jsoup.internal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class ConstrainableInputStreamTest {

    private static final int BUFFER_SIZE = 8;

    /** Helper InputStream that throws {@link SocketTimeoutException} on the first read. */
    private static class TimeoutThrowingInputStream extends InputStream {
        private boolean first = true;
        @Override
        public int read() throws IOException {
            if (first) {
                first = false;
                throw new SocketTimeoutException("simulated timeout");
            }
            return -1; // EOF after the exception
        }
    }

    private ConstrainableInputStream cis;

    @After
    public void tearDown() throws IOException {
        if (cis != null) {
            cis.close();
        }
        // clear interrupted status for the JVM thread after each test
        Thread.interrupted();
    }

    /** --------------------------------------------------------------
     *  wrap()
     *  -------------------------------------------------------------- */
    @Test
    public void wrapCreatesNewInstanceWhenNotConstrainable() {
        InputStream plain = new ByteArrayInputStream(new byte[10]);
        ConstrainableInputStream wrapped = ConstrainableInputStream.wrap(plain, BUFFER_SIZE, 5);
        assertNotNull(wrapped);
        assertNotSame(plain, wrapped);
    }

    @Test
    public void wrapReturnsSameInstanceIfAlreadyConstrainable() {
        InputStream plain = new ByteArrayInputStream(new byte[10]);
        ConstrainableInputStream first = ConstrainableInputStream.wrap(plain, BUFFER_SIZE, 5);
        ConstrainableInputStream second = ConstrainableInputStream.wrap(first, BUFFER_SIZE, 5);
        assertSame(first, second);
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrapRejectsNegativeMaxSize() {
        InputStream plain = new ByteArrayInputStream(new byte[10]);
        // Validate.isTrue throws IllegalArgumentException for a false condition.
        ConstrainableInputStream.wrap(plain, BUFFER_SIZE, -1);
    }

    /** --------------------------------------------------------------
     *  read(byte[],int,int) – size caps
     *  -------------------------------------------------------------- */
    @Test
    public void readRespectsMaxSizeCap() throws IOException {
        byte[] source = "ABCDEFGHIJ".getBytes(); // 10 bytes
        cis = ConstrainableInputStream.wrap(new ByteArrayInputStream(source), BUFFER_SIZE, 5);
        byte[] buf = new byte[10];
        int read = cis.read(buf, 0, buf.length);
        assertEquals(5, read);
        assertArrayEquals("ABCDE".getBytes(), java.util.Arrays.copyOf(buf, 5));

        // subsequent read should signal EOF because the cap is exhausted
        assertEquals(-1, cis.read(buf, 0, buf.length));
    }

    @Test
    public void readWithUnlimitedMaxSize() throws IOException {
        byte[] source = "Hello".getBytes();
        cis = ConstrainableInputStream.wrap(new ByteArrayInputStream(source), BUFFER_SIZE, 0); // 0 == unlimited
        byte[] buf = new byte[10];
        int read = cis.read(buf, 0, buf.length);
        assertEquals(5, read);
        assertArrayEquals(source, java.util.Arrays.copyOf(buf, 5));
        // EOF after full consumption
        assertEquals(-1, cis.read(buf, 0, buf.length));
    }

    /** --------------------------------------------------------------
     *  read – thread interruption
     *  -------------------------------------------------------------- */
    @Test
    public void readReturnsMinusOneWhenThreadInterrupted() throws IOException {
        byte[] source = "data".getBytes();
        cis = ConstrainableInputStream.wrap(new ByteArrayInputStream(source), BUFFER_SIZE, 10);
        // set the interrupt flag
        Thread.currentThread().interrupt();

        byte[] buf = new byte[4];
        int firstRead = cis.read(buf, 0, buf.length);
        assertEquals(-1, firstRead); // first call detects interruption

        // subsequent calls must keep returning -1 even though the interrupt flag is now cleared
        int secondRead = cis.read(buf, 0, buf.length);
        assertEquals(-1, secondRead);
    }

    /** --------------------------------------------------------------
     *  read – timeout expiration
     *  -------------------------------------------------------------- */
    @Test(expected = SocketTimeoutException.class)
    public void readThrowsSocketTimeoutExceptionWhenExpired() throws IOException {
        byte[] source = "any".getBytes();
        cis = ConstrainableInputStream.wrap(new ByteArrayInputStream(source), BUFFER_SIZE, 10);
        // Simulate that the request started 2 seconds ago and the timeout is 1 second.
        long startTime = System.nanoTime() - (2_000_000_000L); // 2 seconds in the past
        cis.timeout(startTime, 1); // 1‑millisecond timeout

        // The check happens before delegating to the underlying stream, so this should throw.
        cis.read(new byte[4], 0, 4);
    }

    /** --------------------------------------------------------------
     *  read – underlying SocketTimeoutException is swallowed and returns 0
     *  -------------------------------------------------------------- */
    @Test
    public void readCatchesUnderlyingSocketTimeoutExceptionAndReturnsZero() throws IOException {
        cis = ConstrainableInputStream.wrap(new TimeoutThrowingInputStream(), BUFFER_SIZE, 10);
        byte[] buf = new byte[4];
        int result = cis.read(buf, 0, buf.length);
        assertEquals(0, result);
        // After the exception, the stream reports EOF
        assertEquals(-1, cis.read(buf, 0, buf.length));
    }

    /** --------------------------------------------------------------
     *  readToByteBuffer – local caps
     *  -------------------------------------------------------------- */
    @Test
    public void readToByteBufferRespectsLocalMax() throws IOException {
        byte[] source = "0123456789".getBytes(); // 10 bytes
        cis = ConstrainableInputStream.wrap(new ByteArrayInputStream(source), BUFFER_SIZE, 20);
        ByteBuffer bb = cis.readToByteBuffer(5);
        assertEquals(5, bb.remaining());
        byte[] expected = "01234".getBytes();
        byte[] actual = new byte[5];
        bb.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void readToByteBufferStopsAtStreamEndWhenLocalMaxIsLarger() throws IOException {
        byte[] source = "XYZ".getBytes(); // 3 bytes
        cis = ConstrainableInputStream.wrap(new ByteArrayInputStream(source), BUFFER_SIZE, 0); // unlimited overall
        ByteBuffer bb = cis.readToByteBuffer(10); // request more than available
        assertEquals(3, bb.remaining());
        byte[] expected = "XYZ".getBytes();
        byte[] actual = new byte[3];
        bb.get(actual);
        assertArrayEquals(expected, actual);
    }

    /** --------------------------------------------------------------
     *  reset() – remaining count is restored
     *  -------------------------------------------------------------- */
    @Test
    public void resetRestoresRemainingBytesAfterMark() throws IOException {
        byte[] source = "ABCDEFGHIJ".getBytes(); // 10 bytes
        cis = ConstrainableInputStream.wrap(new ByteArrayInputStream(source), BUFFER_SIZE, 5);
        cis.mark(20); // mark at the beginning

        byte[] buf = new byte[2];
        int firstRead = cis.read(buf, 0, buf.length); // reads "AB"
        assertEquals(2, firstRead);
        assertEquals(3, cis.remaining); // internal field is package‑private, we can read it directly

        cis.reset(); // should restore remaining to maxSize (5)
        assertEquals(5, cis.remaining);

        // After reset we should be able to read the full 5‑byte payload again.
        byte[] full = new byte[5];
        int fullRead = cis.read(full, 0, full.length);
        assertEquals(5, fullRead);
        assertArrayEquals("ABCDE".getBytes(), full);
    }

    /** --------------------------------------------------------------
     *  timeout() – configuration helper
     *  -------------------------------------------------------------- */
    @Test
    public void timeoutMethodSetsStartTimeAndTimeout() throws IOException {
        byte[] source = "data".getBytes();
        cis = ConstrainableInputStream.wrap(new ByteArrayInputStream(source), BUFFER_SIZE, 10);
        long fakeStart = System.nanoTime() - 5_000_000L; // 5 ms ago
        cis.timeout(fakeStart, 1); // 1 ms timeout → already expired

        // Verify that the next read sees the timeout as expired.
        try {
            cis.read(new byte[4], 0, 4);
            fail("Expected SocketTimeoutException due to configured timeout");
        } catch (SocketTimeoutException e) {
            // expected
        }
    }
}
