package org.jsoup.helper;

import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link DataUtil}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical usage of each public method.</li>
 *   <li>Edge‑case handling (null inputs, empty streams, limits).</li>
 *   <li>Error scenarios (invalid arguments, unsupported charset).</li>
 *   <li>Internal behaviour exercised through package‑private access.</li>
 * </ul>
 *
 * All tests run in the same package {@code org.jsoup.helper} so that package‑private
 * members (e.g. {@code parseInputStream}, {@code detectCharsetFromBom}) are directly
 * accessible without reflection.
 */
public class DataUtilTest {

    private File tempFile;
    private final String baseUri = "http://example.com/";

    /** Helper to create a temporary file with the given content and charset. */
    private File createTempFile(String content, String charset) throws IOException {
        File f = File.createTempFile("jsoup-datautil-test", ".html");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(f), charset)) {
            writer.write(content);
        }
        f.deleteOnExit();
        return f;
    }

    /** Helper to read a {@link ByteBuffer} into a byte array (for assertions). */
    private byte[] toByteArray(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    @After
    public void cleanUp() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    /* --------------------------------------------------------------------- */
    /*  crossStreams                                                          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCrossStreamsCopiesDataExactly() throws IOException {
        byte[] data = "The quick brown fox jumps over the lazy dog".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataUtil.crossStreams(in, out);
        assertArrayEquals("crossStreams should copy all bytes unchanged", data, out.toByteArray());
    }

    /* --------------------------------------------------------------------- */
    /*  readToByteBuffer (unlimited)                                          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testReadToByteBufferUnlimited() throws IOException {
        byte[] payload = "Hello, world!".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(payload);

        ByteBuffer buf = DataUtil.readToByteBuffer(in, 0); // 0 = unlimited
        assertEquals("Buffer should contain exactly the payload length", payload.length, buf.remaining());

        byte[] result = toByteArray(buf);
        assertArrayEquals(payload, result);
    }

    /* --------------------------------------------------------------------- */
    /*  readToByteBuffer (maxSize)                                            */
    /* --------------------------------------------------------------------- */
    @Test
    public void testReadToByteBufferRespectsMaxSize() throws IOException {
        byte[] payload = "1234567890".getBytes("UTF-8"); // 10 bytes
        InputStream in = new ByteArrayInputStream(payload);

        ByteBuffer buf = DataUtil.readToByteBuffer(in, 5); // limit to 5 bytes
        assertEquals(5, buf.remaining());

        byte[] result = toByteArray(buf);
        assertArrayEquals("12345".getBytes("UTF-8"), result);
    }

    /* --------------------------------------------------------------------- */
    /*  readToByteBuffer (negative maxSize)                                   */
    /* --------------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testReadToByteBufferNegativeMaxSizeThrows() throws IOException {
        InputStream in = new ByteArrayInputStream(new byte[0]);
        DataUtil.readToByteBuffer(in, -1);
    }

    /* --------------------------------------------------------------------- */
    /*  readFileToByteBuffer                                                   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testReadFileToByteBufferReadsAllBytes() throws IOException {
        String content = "File content for testing.";
        tempFile = createTempFile(content, "UTF-8");

        ByteBuffer buf = DataUtil.readFileToByteBuffer(tempFile);
        assertEquals(content.length(), buf.remaining());
        assertEquals(content, new String(toByteArray(buf), "UTF-8"));
    }

    @Test(expected = FileNotFoundException.class)
    public void testReadFileToByteBufferFileNotFound() throws IOException {
        File nonExistent = new File("non-existent-" + System.nanoTime() + ".tmp");
        DataUtil.readFileToByteBuffer(nonExistent);
    }

    /* --------------------------------------------------------------------- */
    /*  emptyByteBuffer                                                        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testEmptyByteBufferHasZeroCapacity() {
        ByteBuffer buf = DataUtil.emptyByteBuffer();
        assertEquals(0, buf.capacity());
        assertEquals(0, buf.limit());
    }

    /* --------------------------------------------------------------------- */
    /*  getCharsetFromContentType                                              */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetCharsetFromContentTypeVariousForms() {
        // plain
        assertEquals("UTF-8", DataUtil.getCharsetFromContentType("text/html; charset=UTF-8"));
        // spaces and quotes
        assertEquals("ISO-8859-1", DataUtil.getCharsetFromContentType("text/html; charset = \"ISO-8859-1\""));
        // mixed case
        assertEquals("euc-jp", DataUtil.getCharsetFromContentType("text/html; CHARSET=EUC-JP"));
        // no charset
        assertNull(DataUtil.getCharsetFromContentType("text/html"));
        // null input
        assertNull(DataUtil.getCharsetFromContentType(null));
        // unsupported charset should be filtered out (returns null)
        assertNull(DataUtil.getCharsetFromContentType("text/html; charset=UNSUPPORTED-CHARSET"));
    }

    /* --------------------------------------------------------------------- */
    /*  mimeBoundary                                                          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testMimeBoundaryLengthAndAllowedChars() {
        String boundary = DataUtil.mimeBoundary();
        assertEquals(DataUtil.boundaryLength, boundary.length());

        Set<Character> allowed = new HashSet<>();
        for (char c : "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()) {
            allowed.add(c);
        }

        for (char c : boundary.toCharArray()) {
            assertTrue("Boundary contains only allowed characters", allowed.contains(c));
        }

        // Very low probability of collision – generate a second boundary and ensure they differ
        String another = DataUtil.mimeBoundary();
        // It's *possible* they are equal, but extremely unlikely; if they are, retry once.
        if (boundary.equals(another)) {
            another = DataUtil.mimeBoundary();
        }
        assertNotEquals("Two consecutive mimeBoundary() calls should produce different strings", boundary, another);
    }

    /* --------------------------------------------------------------------- */
    /*  detectCharsetFromBom (via parseInputStream)                           */
    /* --------------------------------------------------------------------- */
    @Test
    public void testParseInputStreamDetectsUtf8Bom() throws IOException {
        // UTF‑8 BOM + simple HTML
        byte[] bom = {(byte)0xEF, (byte)0xBB, (byte)0xBF};
        byte[] html = "<html><head><title>UTF‑8 BOM</title></head><body></body></html>".getBytes("UTF-8");
        ByteArrayInputStream in = new ByteArrayInputStream(concat(bom, html));

        Document doc = DataUtil.parseInputStream(in, null, baseUri, Parser.htmlParser());
        assertEquals("UTF-8", doc.outputSettings().charset().name());
    }

    @Test
    public void testParseInputStreamDetectsUtf16BeBom() throws IOException {
        // UTF‑16BE BOM + HTML encoded in UTF‑16BE
        byte[] bom = {(byte)0xFE, (byte)0xFF};
        String htmlStr = "<html><head><title>UTF‑16BE BOM</title></head><body></body></html>";
        byte[] html = htmlStr.getBytes("UTF-16BE");
        ByteArrayInputStream in = new ByteArrayInputStream(concat(bom, html));

        Document doc = DataUtil.parseInputStream(in, null, baseUri, Parser.htmlParser());
        assertEquals("UTF-16", doc.outputSettings().charset().name());
    }

    /* --------------------------------------------------------------------- */
    /*  parseInputStream – meta charset detection                             */
    /* --------------------------------------------------------------------- */
    @Test
    public void testParseInputStreamDetectsMetaCharsetWhenNoneProvided() throws IOException {
        String html = "<!doctype html><html><head>"
                + "<meta charset=\"ISO-8859-1\">"
                + "<title>Meta charset test</title></head><body></body></html>";
        InputStream in = new ByteArrayInputStream(html.getBytes("ISO-8859-1"));

        Document doc = DataUtil.parseInputStream(in, null, baseUri, Parser.htmlParser());
        assertEquals("ISO-8859-1", doc.outputSettings().charset().name());
    }

    @Test
    public void testParseInputStreamUsesSpecifiedCharsetOverMeta() throws IOException {
        String html = "<!doctype html><html><head>"
                + "<meta charset=\"ISO-8859-1\">"
                + "<title>Specified charset wins</title></head><body></body></html>";
        InputStream in = new ByteArrayInputStream(html.getBytes("UTF-8"));

        Document doc = DataUtil.parseInputStream(in, "UTF-8", baseUri, Parser.htmlParser());
        assertEquals("UTF-8", doc.outputSettings().charset().name());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseInputStreamRejectsEmptySpecifiedCharset() throws IOException {
        // charsetName is empty → Validate.notEmpty throws IllegalArgumentException
        InputStream in = new ByteArrayInputStream("<html></html>".getBytes("UTF-8"));
        DataUtil.parseInputStream(in, "", baseUri, Parser.htmlParser());
    }

    @Test
    public void testParseInputStreamWithNullInputReturnsEmptyDocument() throws IOException {
        Document doc = DataUtil.parseInputStream(null, null, baseUri, Parser.htmlParser());
        assertNotNull(doc);
        assertEquals(baseUri, doc.baseUri());
        assertEquals(0, doc.childNodeSize());
    }

    /* --------------------------------------------------------------------- */
    /*  load(File) – end‑to‑end test                                          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testLoadFileDetectsMetaCharset() throws IOException {
        String html = "<html><head><meta charset=\"ISO-8859-2\"></head><body>ž</body></html>";
        tempFile = createTempFile(html, "ISO-8859-2");

        Document doc = DataUtil.load(tempFile, null, baseUri);
        assertEquals("ISO-8859-2", doc.outputSettings().charset().name());

        // The special character should be preserved when read with the detected charset
        assertTrue(doc.body().text().contains("ž"));
    }

    /* --------------------------------------------------------------------- */
    /*  Helper method to concatenate two byte arrays                         */
    /* --------------------------------------------------------------------- */
    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
