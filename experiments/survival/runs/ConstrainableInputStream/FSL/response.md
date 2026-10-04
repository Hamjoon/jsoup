###Test START##
package org.jsoup.internal;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit 4 tests for {@link ConstrainableInputStream}.
 *
 * These tests aim to cover:
 * <ul>
 *   <li>wrap() – identity handling and new instance creation</li>
 *   <li>read(byte[],int,int) – normal reads, capping, thread interruption, timeout, and SocketTimeoutException handling</li>
 *   <li>readToByteBuffer(int) – unlimited reads, local caps, and proper truncation</li>
 *   <li>reset() – remaining bytes recomputed after a mark/reset cycle</li>
 *   <li>timeout(long,long) – expiration logic</li>
 * </ul>
 */
public class ConstrainableInputStreamTest {

    private static final byte[] SAMPLE_DATA = "abcdefghijklmnopqrstuvwxyz".getBytes();

    /** Helper to create a ConstrainableInputStream with a given maxSize. */
    private ConstrainableInputStream newCIS(int maxSize) {
        InputStream in = new ByteArrayInputStream(SAMPLE_DATA);
        return ConstrainableInputStream.wrap(in, 32, maxSize);
    }

    /** Clears the interrupt flag after each test to avoid cross‑test interference. */
    @After
    public void clearInterrupt() {
        Thread.interrupted(); // clear flag
    }

    // ----------------------------------------------------------------------
    // wrap()
    // ----------------------------------------------------------------------
    @Test
    public void testWrapReturnsSameInstanceIfAlreadyConstrainable() {
        ConstrainableInputStream cis = newCIS(0);
        ConstrainableInputStream wrapped = ConstrainableInputStream.wrap(cis, 64, 10);
        assertSame("wrap() should return the same instance when already a ConstrainableInputStream",
                cis, wrapped);
    }

    @Test
    public void testWrapCreatesNewInstanceWhenNotConstrainable() {
        InputStream plain = new ByteArrayInputStream(SAMPLE_DATA);
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(plain, 64, 5);
        assertNotNull(cis);
        assertTrue(cis instanceof ConstrainableInputStream);
    }

    // ----------------------------------------------------------------------
    // read(byte[],int,int) – normal read, capping, and end‑of‑stream
    // ----------------------------------------------------------------------
    @Test
    public void testReadRespectsMaxSizeCap() throws IOException {
        ConstrainableInputStream cis = newCIS(5); // allow only 5 bytes
        byte[] buf = new byte[10];
        int read = cis.read(buf, 0, buf.length);
        assertEquals("Should read exactly the capped amount", 5, read);
        // subsequent read should signal EOF (-1)
        assertEquals(-1, cis.read(buf, 0, buf.length));
    }

    @Test
    public void testReadDoesNotExceedRequestedLengthWhenRemainingSmaller() throws IOException {
        ConstrainableInputStream cis = newCIS(8);
        byte[] buf = new byte[10];
        // ask for 10 but only 8 remaining – should get 8
        int read = cis.read(buf, 0, 10);
        assertEquals(8, read);
    }

    @Test
    public void testReadUnlimitedWhenMaxSizeZero() throws IOException {
        ConstrainableInputStream cis = newCIS(0); // 0 means infinite
        byte[] buf = new byte[30];
        int total = 0;
        int read;
        while ((read = cis.read(buf, 0, buf.length)) != -1) {
            total += read;
        }
        assertEquals("Should read all data when maxSize == 0", SAMPLE_DATA.length, total);
    }

    // ----------------------------------------------------------------------
    // read(byte[],int,int) – thread interruption handling
    // ----------------------------------------------------------------------
    @Test
    public void testReadReturnsMinusOneWhenThreadInterrupted() throws IOException {
        ConstrainableInputStream cis = newCIS(0);
        Thread.currentThread().interrupt(); // set interrupt flag
        byte[] buf = new byte[4];
        int read = cis.read(buf, 0, buf.length);
        assertEquals("Interrupted read should return -1", -1, read);
        // subsequent reads stay at -1 because the internal flag is latched
        read = cis.read(buf, 0, buf.length);
        assertEquals(-1, read);
    }

    // ----------------------------------------------------------------------
    // read(byte[],int,int) – timeout handling (SocketTimeoutException)
    // ----------------------------------------------------------------------
    @Test(expected = SocketTimeoutException.class)
    public void testReadThrowsSocketTimeoutWhenExpired() throws IOException {
        // Underlying stream can be empty – we care about the timeout logic only
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(empty, 32, 0);
        // Set a timeout of 1 ms and a startTime far enough in the past to force expiration
        cis.timeout(System.nanoTime() - 10_000_000L, 1);
        byte[] buf = new byte[4];
        cis.read(buf, 0, buf.length); // should throw
    }

    // ----------------------------------------------------------------------
    // read(byte[],int,int) – underlying SocketTimeoutException handling
    // ----------------------------------------------------------------------
    @Test
    public void testReadReturnsZeroWhenUnderlyingThrowsSocketTimeout() throws IOException {
        InputStream throwing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new SocketTimeoutException("simulated");
            }
        };
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(throwing, 32, 0);
        byte[] buf = new byte[4];
        int read = cis.read(buf, 0, buf.length);
        assertEquals("When underlying throws SocketTimeoutException, read should return 0", 0, read);
    }

    // ----------------------------------------------------------------------
    // readToByteBuffer(int) – unlimited read (max == 0)
    // ----------------------------------------------------------------------
    @Test
    public void testReadToByteBufferUnlimited() throws IOException {
        ConstrainableInputStream cis = newCIS(0);
        ByteBuffer bb = cis.readToByteBuffer(0); // 0 means unlimited
        byte[] result = new byte[bb.remaining()];
        bb.get(result);
        assertArrayEquals("Unlimited readToByteBuffer should return all bytes", SAMPLE_DATA, result);
    }

    // ----------------------------------------------------------------------
    // readToByteBuffer(int) – local cap smaller than stream's maxSize
    // ----------------------------------------------------------------------
    @Test
    public void testReadToByteBufferWithLocalCap() throws IOException {
        ConstrainableInputStream cis = newCIS(0);
        int localCap = 5;
        ByteBuffer bb = cis.readToByteBuffer(localCap);
        byte[] result = new byte[bb.remaining()];
        bb.get(result);
        assertEquals("Result length should match local cap", localCap, result.length);
        byte[] expected = new byte[localCap];
        System.arraycopy(SAMPLE_DATA, 0, expected, 0, localCap);
        assertArrayEquals(expected, result);
    }

    // ----------------------------------------------------------------------
    // reset() – remaining bytes recomputed after mark/reset
    // ----------------------------------------------------------------------
    @Test
    public void testResetRestoresRemainingAccordingToMark() throws IOException {
        // maxSize 10, underlying data 26 bytes – we will only read up to maxSize
        ConstrainableInputStream cis = newCIS(10);
        cis.mark(0); // mark at the beginning (position 0)
        byte[] buf = new byte[4];
        int firstRead = cis.read(buf, 0, buf.length); // read 4 bytes
        assertEquals(4, firstRead);
        // remaining should now be 6 (10 - 4)
        // Reset should bring remaining back to 10 - markpos (markpos == 0)
        cis.reset();
        // After reset, we should be able to read the full 10 bytes again
        int total = 0;
        while ((firstRead = cis.read(buf, 0, buf.length)) != -1) {
            total += firstRead;
        }
        assertEquals("After reset, should be able to read the original maxSize bytes again", 10, total);
    }

    // ----------------------------------------------------------------------
    // timeout(long,long) – verifies that the method returns the same instance (fluent API)
    // ----------------------------------------------------------------------
    @Test
    public void testTimeoutReturnsSelf() {
        ConstrainableInputStream cis = newCIS(0);
        ConstrainableInputStream returned = cis.timeout(System.nanoTime(), 100);
        assertSame("timeout() should return the same instance for chaining", cis, returned);
    }

    // ----------------------------------------------------------------------
    // Integration test: combination of cap, timeout, and interruption
    // ----------------------------------------------------------------------
    @Test
    public void testCombinedBehaviour() throws IOException {
        // Stream with maxSize 5, but we will also force a timeout after the first read
        InputStream source = new ByteArrayInputStream(SAMPLE_DATA);
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(source, 32, 5);
        // First successful read of 3 bytes
        byte[] buf = new byte[3];
        int read = cis.read(buf, 0, buf.length);
        assertEquals(3, read);
        // Set timeout to expire immediately
        cis.timeout(System.nanoTime() - 1_000_000L, 0); // zero millis -> immediate expiration
        // Next read should throw SocketTimeoutException
        try {
            cis.read(buf, 0, buf.length);
            fail("Expected SocketTimeoutException after timeout expiry");
        } catch (SocketTimeoutException ignored) {
            // expected
        }
        // Interrupt the thread and verify that subsequent reads return -1
        Thread.currentThread().interrupt();
        read = cis.read(buf, 0, buf.length);
        assertEquals(-1, read);
    }

    // ----------------------------------------------------------------------
    // Helper class to simulate a stream that returns a fixed number of bytes,
    // then blocks (used to test that read() respects the 'len' argument)
    // ----------------------------------------------------------------------
    private static class FixedByteInputStream extends InputStream {
        private final byte[] data;
        private int pos = 0;

        FixedByteInputStream(byte[] data) {
            this.data = data;
        }

        @Override
        public int read() {
            if (pos >= data.length) {
                return -1;
            }
            return data[pos++] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (pos >= data.length) {
                return -1;
            }
            int toCopy = Math.min(len, data.length - pos);
            System.arraycopy(data, pos, b, off, toCopy);
            pos += toCopy;
            return toCopy;
        }
    }

    @Test
    public void testReadHonoursRequestedLengthWhenRemainingIsLarger() throws IOException {
        byte[] payload = new byte[50];
        for (int i = 0; i < payload.length; i++) payload[i] = (byte) i;
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(new FixedByteInputStream(payload), 16, 0);
        byte[] buf = new byte[10];
        int read = cis.read(buf, 0, buf.length);
        assertEquals(10, read);
        // Verify the content matches the first 10 bytes of payload
        for (int i = 0; i < 10; i++) {
            assertEquals(payload[i], buf[i]);
        }
    }
}
###Test END##