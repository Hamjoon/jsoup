**Public methods of `ConstrainableInputStream`**

| # | Modifier & Return | Method signature | Description |
|---|-------------------|------------------|-------------|
| 1 | `public static` `ConstrainableInputStream` | `wrap(InputStream in, int bufferSize, int maxSize)` | Returns a `ConstrainableInputStream` – either the supplied instance (if it already is one) or a newly wrapped instance. |
| 2 | `public` `int` | `read(byte[] b, int off, int len) throws IOException` | Reads up to `len` bytes respecting the *capped* size, interruption and timeout rules. |
| 3 | `public` `ByteBuffer` | `readToByteBuffer(int max) throws IOException` | Reads the whole stream (or up to `max` bytes) into a `ByteBuffer`. |
| 4 | `public` `void` | `reset() throws IOException` | Resets the stream to the last marked position and recomputes the remaining byte‑budget. |
| 5 | `public` `ConstrainableInputStream` | `timeout(long startTimeNanos, long timeoutMillis)` | Sets a read‑timeout (nanoseconds) and returns `this` for chaining. |

---

### Potential edge‑cases & exception scenarios to test
| Method | Edge case / Exception | What to verify |
|--------|-----------------------|----------------|
| `wrap` | `in` already a `ConstrainableInputStream` | Returns the same object (no new wrapper). |
| `wrap` | `maxSize` = 0 (unlimited) | Instance is created without *capped* flag. |
| `read` | Normal read within remaining budget | Returns the number of bytes read and decrements `remaining`. |
| `read` | `len` > `remaining` (capped) | Returns only `remaining` bytes, never more. |
| `read` | `remaining` already 0 (capped) | Returns `‑1` immediately. |
| `read` | Thread is interrupted before call | Sets internal `interrupted` flag and returns `‑1`. Subsequent reads also return `‑1`. |
| `read` | Timeout has expired | Throws `SocketTimeoutException`. |
| `read` | Underlying stream throws `SocketTimeoutException` | Method catches it and returns `0`. (Hard to provoke directly, but can be simulated with a custom `InputStream`). |
| `readToByteBuffer` | `max` = 0 (unlimited) | Returns a buffer containing the whole stream. |
| `readToByteBuffer` | `max` > 0 but smaller than actual stream size | Returns exactly `max` bytes. |
| `readToByteBuffer` | `max` larger than stream size | Returns the whole stream (no extra bytes). |
| `reset` | After `mark` and some reads, `reset` restores `remaining` correctly | Verify that subsequent reads respect the restored budget. |
| `timeout` | Correctly stores the supplied values (indirectly verified via a timeout test) | After calling `timeout`, a read must fail with `SocketTimeoutException` when the deadline is passed. |

---

## Complete JUnit‑4 test file  

```java
###Test START##
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

    private static final byte[] SAMPLE_DATA = "The quick brown fox jumps over the lazy dog".getBytes();

    private Thread testThread;

    @Before
    public void setUp() {
        // remember the original thread (so we can clear the interrupted flag after each test)
        testThread = Thread.currentThread();
    }

    @After
    public void tearDown() {
        // ensure the interrupted flag is cleared for the next test
        if (testThread.isInterrupted()) {
            testThread.interrupted(); // clears flag
        }
    }

    /* --------------------------------------------------------------------- */
    /* 1. wrap()                                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testWrapCreatesNewInstance() {
        InputStream raw = new ByteArrayInputStream(SAMPLE_DATA);
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(raw, 64, 100);
        assertNotNull(cis);
        assertTrue(cis instanceof ConstrainableInputStream);
    }

    @Test
    public void testWrapReturnsSameInstanceWhenAlreadyWrapped() {
        InputStream raw = new ByteArrayInputStream(SAMPLE_DATA);
        ConstrainableInputStream first = ConstrainableInputStream.wrap(raw, 64, 100);
        ConstrainableInputStream second = ConstrainableInputStream.wrap(first, 64, 100);
        assertSame(first, second);
    }

    /* --------------------------------------------------------------------- */
    /* 2. read(byte[],int,int) – normal, capped, len > remaining, etc.      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testReadWithinLimit() throws IOException {
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(
                new ByteArrayInputStream(SAMPLE_DATA), 32, SAMPLE_DATA.length);
        byte[] buf = new byte[10];
        int read = cis.read(buf, 0, buf.length);
        assertEquals(10, read);
        // remaining should have been decreased
        // (we cannot access the private field, but a second read of the same length should still succeed)
        read = cis.read(buf, 0, buf.length);
        assertEquals(10, read);
    }

    @Test
    public void testReadCappedLimit() throws IOException {
        // maxSize smaller than the data length
        int maxSize = 15;
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(
                new ByteArrayInputStream(SAMPLE_DATA), 32, maxSize);
        byte[] buf = new byte[20]; // request more than maxSize
        int read = cis.read(buf, 0, buf.length);
        assertEquals(maxSize, read);               // only maxSize bytes returned
        // subsequent read should indicate EOF because remaining == 0
        read = cis.read(buf, 0, buf.length);
        assertEquals(-1, read);
    }

    @Test
    public void testReadWhenRemainingZeroReturnsMinusOne() throws IOException {
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(
                new ByteArrayInputStream(new byte[0]), 32, 0); // uncapped (maxSize==0)
        // Force capped to true with a non‑zero maxSize and consume it
        cis = ConstrainableInputStream.wrap(
                new ByteArrayInputStream(new byte[5]), 32, 5);
        byte[] buf = new byte[5];
        assertEquals(5, cis.read(buf, 0, 5)); // consume all
        assertEquals(-1, cis.read(buf, 0, 5)); // now remaining == 0 -> -1
    }

    @Test
    public void testReadThreadInterruptedReturnsMinusOne() throws IOException {
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(
                new ByteArrayInputStream(SAMPLE_DATA), 32, SAMPLE_DATA.length);
        // interrupt the current thread
        Thread.currentThread().interrupt();
        byte[] buf = new byte[10];
        int read = cis.read(buf, 0, buf.length);
        assertEquals(-1, read);
        // subsequent reads must also return -1 because the internal flag stays true
        read = cis.read(buf, 0, buf.length);
        assertEquals(-1, read);
    }

    @Test(expected = SocketTimeoutException.class)
    public void testReadTimeoutThrowsException() throws IOException {
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(
                new ByteArrayInputStream(SAMPLE_DATA), 32, SAMPLE_DATA.length);
        // set a timeout that has already expired
        long past = System.nanoTime() - 2_000_000_000L; // 2 seconds ago
        cis.timeout(past, 1); // 1 ms timeout
        byte[] buf = new byte[10];
        cis.read(buf, 0, buf.length); // should throw SocketTimeoutException
    }

    @Test
    public void testReadLenGreaterThanRemainingGetsTrimmed() throws IOException {
        int maxSize = 8;
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(
                new ByteArrayInputStream(SAMPLE_DATA), 32, maxSize);
        byte[] buf = new byte[20]; // request more than remaining
        int read = cis.read(buf, 0, buf.length);
        assertEquals(maxSize, read);
    }

    /* --------------------------------------------------------------------- */
    /* 3. readToByteBuffer(int) – limited & unlimited                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testReadToByteBufferUnlimited() throws IOException {
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(
                new ByteArrayInputStream(SAMPLE_DATA), 32, 0); // uncapped
        ByteBuffer bb = cis.readToByteBuffer(0); // 0 => unlimited
        assertEquals(SAMPLE_DATA.length, bb.remaining());
        byte[] out = new byte[bb.remaining()];
        bb.get(out);
        assertArrayEquals(SAMPLE_DATA, out);
    }

    @Test
    public void testReadToByteBufferLimited() throws IOException {
        int limit = 12;
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(
                new ByteArrayInputStream(SAMPLE_DATA), 32, 0); // uncapped overall
        ByteBuffer bb = cis.readToByteBuffer(limit);
        assertEquals(limit, bb.remaining());
        byte[] out = new byte[bb.remaining()];
        bb.get(out);
        byte[] expected = new byte[limit];
        System.arraycopy(SAMPLE_DATA, 0, expected, 0, limit);
        assertArrayEquals(expected, out);
    }

    @Test
    public void testReadToByteBufferLimitedSmallerThanStreamAndOverallMax() throws IOException {
        // overall maxSize is 20, but we ask for only 10 bytes
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(
                new ByteArrayInputStream(SAMPLE_DATA), 32, 20);
        ByteBuffer bb = cis.readToByteBuffer(10);
        assertEquals(10, bb.remaining());
        byte[] out = new byte[bb.remaining()];
        bb.get(out);
        byte[] expected = new byte[10];
        System.arraycopy(SAMPLE_DATA, 0, expected, 0, 10);
        assertArrayEquals(expected, out);
    }

    /* --------------------------------------------------------------------- */
    /* 4. reset() – verify that remaining is recomputed after a mark/reset   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testResetRestoresRemainingBudget() throws IOException {
        // overall maxSize = 15
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(
                new ByteArrayInputStream(SAMPLE_DATA), 32, 15);
        cis.mark(100); // mark at position 0
        byte[] buf = new byte[5];
        assertEquals(5, cis.read(buf, 0, 5)); // consume 5 bytes, remaining = 10
        cis.reset(); // should restore remaining to maxSize - markpos (i.e., 15)
        // After reset we should be able to read the full 15‑byte budget again
        int totalRead = 0;
        while (cis.read(buf, 0, buf.length) != -1) {
            totalRead += buf.length;
        }
        assertEquals(15, totalRead);
    }

    /* --------------------------------------------------------------------- */
    /* 5. timeout() – indirectly verified by the timeout test above           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTimeoutMethodIsChainable() {
        ConstrainableInputStream cis = ConstrainableInputStream.wrap(
                new ByteArrayInputStream(SAMPLE_DATA), 32, 0);
        ConstrainableInputStream returned = cis.timeout(System.nanoTime(), 100);
        assertSame(cis, returned);
    }
}
###Test END##
```