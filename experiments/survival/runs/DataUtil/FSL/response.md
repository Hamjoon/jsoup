###Test START##
package org.jsoup.helper;

import static org.junit.Assert.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;

import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.Test;

/**
 * Comprehensive JUnit4 tests for {@link DataUtil}.
 */
public class DataUtilTest {

    // ----------------------------------------------------------------------
    // Helper methods
    // ----------------------------------------------------------------------
    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataUtil.crossStreams(in, out);
        return out.toString("UTF-8");
    }

    private static String charsetFromBom(byte[] bom, String html) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(bom);
        baos.write(html.getBytes("UTF-8"));
        ByteArrayInputStream in = new ByteArrayInputStream(baos.toByteArray());
        Document doc = DataUtil.load(in, null, "http://example.com");
        return doc.outputSettings().charset().name();
    }

    // ----------------------------------------------------------------------
    // Tests for crossStreams
    // ----------------------------------------------------------------------
    @Test
    public void testCrossStreamsCopiesExactly() throws IOException {
        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataUtil.crossStreams(in, out);
        assertArrayEquals(data, out.toByteArray());
    }

    // ----------------------------------------------------------------------
    // Tests for readToByteBuffer
    // ----------------------------------------------------------------------
    @Test
    public void testReadToByteBufferUnlimited() throws IOException {
        byte[] src = "Hello world!".getBytes("UTF-8");
        ByteArrayInputStream in = new ByteArrayInputStream(src);
        ByteBuffer buf = DataUtil.readToByteBuffer(in);
        assertEquals(src.length, buf.remaining());
        byte[] dst = new byte[buf.remaining()];
        buf.get(dst);
        assertArrayEquals(src, dst);
    }

    @Test
    public void testReadToByteBufferWithLimit() throws IOException {
        byte[] src = "abcdefghijklmnopqrstuvwxyz".getBytes("UTF-8");
        ByteArrayInputStream in = new ByteArrayInputStream(src);
        int limit = 10;
        ByteBuffer buf = DataUtil.readToByteBuffer(in, limit);
        assertEquals(limit, buf.remaining());
        byte[] dst = new byte[limit];
        buf.get(dst);
        assertArrayEquals(Arrays.copyOf(src, limit), dst);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToByteBufferNegativeSize() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        DataUtil.readToByteBuffer(in, -1);
    }

    // ----------------------------------------------------------------------
    // Tests for readFileToByteBuffer
    // ----------------------------------------------------------------------
    @Test
    public void testReadFileToByteBuffer() throws IOException {
        File temp = File.createTempFile("datutil", ".tmp");
        temp.deleteOnExit();
        byte[] content = "File content for testing".getBytes("UTF-8");
        try (FileOutputStream fos = new FileOutputStream(temp)) {
            fos.write(content);
        }
        ByteBuffer buf = DataUtil.readFileToByteBuffer(temp);
        assertEquals(content.length, buf.remaining());
        byte[] read = new byte[buf.remaining()];
        buf.get(read);
        assertArrayEquals(content, read);
    }

    // ----------------------------------------------------------------------
    // Tests for emptyByteBuffer
    // ----------------------------------------------------------------------
    @Test
    public void testEmptyByteBuffer() {
        ByteBuffer buf = DataUtil.emptyByteBuffer();
        assertEquals(0, buf.capacity());
        assertEquals(0, buf.remaining());
    }

    // ----------------------------------------------------------------------
    // Tests for getCharsetFromContentType and validateCharset (indirect)
    // ----------------------------------------------------------------------
    @Test
    public void testGetCharsetFromContentTypeSimple() {
        assertEquals("UTF-8", DataUtil.getCharsetFromContentType("text/html; charset=UTF-8"));
    }

    @Test
    public void testGetCharsetFromContentTypeWithQuotes() {
        assertEquals("ISO-8859-1", DataUtil.getCharsetFromContentType("text/html; charset=\"ISO-8859-1\""));
    }

    @Test
    public void testGetCharsetFromContentTypeUpperLowerCase() {
        assertEquals("utf-8", DataUtil.getCharsetFromContentType("TEXT/HTML; CHARSET=utf-8"));
    }

    @Test
    public void testGetCharsetFromContentTypeMissing() {
        assertNull(DataUtil.getCharsetFromContentType("text/html"));
    }

    @Test
    public void testGetCharsetFromContentTypeUnsupported() {
        // "foo-bar" is not a legal charset
        assertNull(DataUtil.getCharsetFromContentType("text/html; charset=foo-bar"));
    }

    @Test
    public void testValidateCharsetDirectlyViaReflection() throws Exception {
        Method m = DataUtil.class.getDeclaredMethod("validateCharset", String.class);
        m.setAccessible(true);
        // supported charset
        assertEquals("UTF-8", m.invoke(null, "UTF-8"));
        // supported but lower case, should be returned as is (method does not change case)
        assertEquals("utf-8", m.invoke(null, "utf-8"));
        // unsupported charset -> null
        assertNull(m.invoke(null, "UNSUPPORTED-CHARSET"));
        // illegal charset name -> null
        assertNull(m.invoke(null, "!!!"));
        // null input -> null
        assertNull(m.invoke(null, (Object) null));
    }

    // ----------------------------------------------------------------------
    // Tests for mimeBoundary
    // ----------------------------------------------------------------------
    @Test
    public void testMimeBoundaryLengthAndChars() {
        String boundary = DataUtil.mimeBoundary();
        assertEquals(DataUtil.boundaryLength, boundary.length());

        String allowed = new String(DataUtil.mimeBoundaryChars);
        for (char c : boundary.toCharArray()) {
            assertTrue("Character " + c + " not allowed in mime boundary", allowed.indexOf(c) >= 0);
        }
    }

    // ----------------------------------------------------------------------
    // Tests for charset detection via BOM in load()
    // ----------------------------------------------------------------------
    @Test
    public void testLoadDetectsUtf8Bom() throws IOException {
        byte[] bom = new byte[] {(byte)0xEF, (byte)0xBB, (byte)0xBF};
        String html = "<html><head><title>Test</title></head><body>こんにちは</body></html>";
        String charset = charsetFromBom(bom, html);
        assertEquals("UTF-8", charset);
    }

    @Test
    public void testLoadDetectsUtf16Bom() throws IOException {
        // UTF-16LE BOM
        byte[] bom = new byte[] {(byte)0xFF, (byte)0xFE};
        // Simple HTML encoded in UTF-16LE (with BOM already removed for the test)
        String html = "<html><head><title>UTF16</title></head><body>test</body></html>";
        byte[] htmlBytes = html.getBytes("UTF-16LE");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(bom);
        baos.write(htmlBytes);
        ByteArrayInputStream in = new ByteArrayInputStream(baos.toByteArray());

        Document doc = DataUtil.load(in, null, "http://example.com");
        assertEquals("UTF-16", doc.outputSettings().charset().name());
        assertEquals("UTF16", doc.title());
    }

    // ----------------------------------------------------------------------
    // Tests for private detectCharsetFromBom via reflection
    // ----------------------------------------------------------------------
    @Test
    public void testDetectCharsetFromBomPrivateMethod() throws Exception {
        Method detectMethod = DataUtil.class.getDeclaredMethod("detectCharsetFromBom", ByteBuffer.class);
        detectMethod.setAccessible(true);

        // UTF-8 BOM
        byte[] utf8Bom = new byte[] {(byte)0xEF, (byte)0xBB, (byte)0xBF, 0x00};
        ByteBuffer bbUtf8 = ByteBuffer.wrap(utf8Bom);
        Object result = detectMethod.invoke(null, bbUtf8);
        assertNotNull(result);
        Field charsetField = result.getClass().getDeclaredField("charset");
        charsetField.setAccessible(true);
        assertEquals("UTF-8", charsetField.get(result));

        // UTF-16BE BOM
        byte[] utf16beBom = new byte[] {(byte)0xFE, (byte)0xFF, 0x00, 0x00};
        ByteBuffer bbUtf16be = ByteBuffer.wrap(utf16beBom);
        result = detectMethod.invoke(null, bbUtf16be);
        assertNotNull(result);
        assertEquals("UTF-16", charsetField.get(result));

        // No BOM
        byte[] noBom = new byte[] {0x41, 0x42, 0x43};
        ByteBuffer bbNoBom = ByteBuffer.wrap(noBom);
        assertNull(detectMethod.invoke(null, bbNoBom));
    }

    // ----------------------------------------------------------------------
    // Tests for load(InputStream, charset, baseUri) with explicit charset
    // ----------------------------------------------------------------------
    @Test
    public void testLoadWithExplicitCharset() throws IOException {
        String html = "<html><head><title>Explicit</title></head><body>123</body></html>";
        byte[] data = html.getBytes("ISO-8859-1");
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        Document doc = DataUtil.load(in, "ISO-8859-1", "http://example.com");
        assertEquals("Explicit", doc.title());
        assertEquals("ISO-8859-1", doc.outputSettings().charset().name());
    }

    // ----------------------------------------------------------------------
    // Tests for load(File, ...) using temporary file
    // ----------------------------------------------------------------------
    @Test
    public void testLoadFile() throws IOException {
        File temp = File.createTempFile("datutil", ".html");
        temp.deleteOnExit();
        String html = "<html><head><title>FileLoad</title></head><body>abc</body></html>";
        try (Writer w = new OutputStreamWriter(new FileOutputStream(temp), "UTF-8")) {
            w.write(html);
        }
        Document doc = DataUtil.load(temp, "UTF-8", "http://example.com");
        assertEquals("FileLoad", doc.title());
    }

    // ----------------------------------------------------------------------
    // Tests for parseInputStream meta charset detection (indirect via load)
    // ----------------------------------------------------------------------
    @Test
    public void testMetaCharsetDetection() throws IOException {
        String html = "<html><head>"
                + "<meta charset=\"ISO-8859-2\">"
                + "<title>MetaCharset</title>"
                + "</head><body>xyz</body></html>";
        ByteArrayInputStream in = new ByteArrayInputStream(html.getBytes("ISO-8859-2"));
        Document doc = DataUtil.load(in, null, "http://example.com");
        // The document's output charset should be the detected one
        assertEquals("ISO-8859-2", doc.outputSettings().charset().name());
        assertEquals("MetaCharset", doc.title());
    }
}
###Test END##