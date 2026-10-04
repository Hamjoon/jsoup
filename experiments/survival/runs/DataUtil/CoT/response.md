###Test START##
```java
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

public class DataUtilTest {

    private File tempFile;

    /** --------------------------------------------------------------------
     *  Helper methods
     *  -------------------------------------------------------------------- */
    private static final Set<Character> MIME_BOUNDARY_ALLOWED = new HashSet<>();
    static {
        for (char c : "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray())
            MIME_BOUNDARY_ALLOWED.add(c);
    }

    /** --------------------------------------------------------------------
     *  Setup / Teardown for file‑based tests
     *  -------------------------------------------------------------------- */
    @Before
    public void setUp() throws IOException {
        tempFile = File.createTempFile("DataUtilTest", ".txt");
        tempFile.deleteOnExit();
    }

    @After
    public void tearDown() {
        if (tempFile != null && tempFile.exists())
            tempFile.delete();
    }

    /** --------------------------------------------------------------------
     *  Public method: load(File, String, String)
     *  -------------------------------------------------------------------- */
    @Test
    public void testLoadFileWithMetaCharset() throws IOException {
        // HTML with ISO‑8859‑1 meta charset
        String html = "<meta charset=\"ISO-8859-1\"><title>MetaCharset</title>";
        try (Writer w = new OutputStreamWriter(new FileOutputStream(tempFile), "ISO-8859-1")) {
            w.write(html);
        }

        Document doc = DataUtil.load(tempFile, null, "http://example.com/");
        assertEquals("MetaCharset", doc.title());
        // The parser should have set the document charset to the detected one
        assertEquals("ISO-8859-1", doc.outputSettings().charset().name());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadFileWithEmptyCharsetThrows() throws IOException {
        // Empty charset string should trigger Validate.notEmpty() inside load(...)
        DataUtil.load(tempFile, "", "http://example.com/");
    }

    /** --------------------------------------------------------------------
     *  Public method: load(InputStream, String, String)
     *  -------------------------------------------------------------------- */
    @Test
    public void testLoadInputStreamWithBom() throws IOException {
        // UTF‑8 BOM + simple HTML
        byte[] bom = new byte[] {(byte)0xEF, (byte)0xBB, (byte)0xBF};
        String html = "<title>BOMTest</title>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(bom);
        out.write(html.getBytes("UTF-8"));
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

        Document doc = DataUtil.load(in, null, "http://example.com/");
        assertEquals("BOMTest", doc.title());
        // BOM detection forces UTF‑8
        assertEquals("UTF-8", doc.outputSettings().charset().name());
    }

    /** --------------------------------------------------------------------
     *  Public method: load(InputStream, String, String, Parser)
     *  -------------------------------------------------------------------- */
    @Test
    public void testLoadWithExplicitParser() throws IOException {
        String html = "<title>XMLParser</title>";
        ByteArrayInputStream in = new ByteArrayInputStream(html.getBytes("UTF-8"));
        Document doc = DataUtil.load(in, "UTF-8", "http://example.com/", Parser.xmlParser());
        // XML parser will treat the document as XML; title is still accessible via tag name
        assertEquals("XMLParser", doc.select("title").first().text());
        assertEquals("UTF-8", doc.outputSettings().charset().name());
    }

    /** --------------------------------------------------------------------
     *  Public method: readToByteBuffer(InputStream, int)
     *  -------------------------------------------------------------------- */
    @Test
    public void testReadToByteBufferWithLimit() throws IOException {
        byte[] data = "HelloWorld".getBytes("UTF-8");
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ByteBuffer buffer = DataUtil.readToByteBuffer(in, 5);
        assertEquals(5, buffer.remaining());
        byte[] expected = "Hello".getBytes("UTF-8");
        byte[] actual = new byte[5];
        buffer.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testReadToByteBufferUnlimited() throws IOException {
        byte[] data = "Unlimited".getBytes("UTF-8");
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ByteBuffer buffer = DataUtil.readToByteBuffer(in, 0); // 0 = unlimited
        assertEquals(data.length, buffer.remaining());
        byte[] actual = new byte[data.length];
        buffer.get(actual);
        assertArrayEquals(data, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToByteBufferNegativeMaxSize() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[]{1, 2, 3});
        DataUtil.readToByteBuffer(in, -1);
    }

    /** --------------------------------------------------------------------
     *  Public method: readFileToByteBuffer(File)
     *  -------------------------------------------------------------------- */
    @Test
    public void testReadFileToByteBuffer() throws IOException {
        byte[] content = "FileContent".getBytes("UTF-8");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(content);
        }

        ByteBuffer buf = DataUtil.readFileToByteBuffer(tempFile);
        assertEquals(content.length, buf.remaining());
        byte[] actual = new byte[content.length];
        buf.get(actual);
        assertArrayEquals(content, actual);
    }

    /** --------------------------------------------------------------------
     *  Public method: emptyByteBuffer()
     *  -------------------------------------------------------------------- */
    @Test
    public void testEmptyByteBuffer() {
        ByteBuffer buf = DataUtil.emptyByteBuffer();
        assertEquals(0, buf.capacity());
        assertEquals(0, buf.remaining());
    }

    /** --------------------------------------------------------------------
     *  Public method: getCharsetFromContentType(String)
     *  -------------------------------------------------------------------- */
    @Test
    public void testGetCharsetFromContentTypeStandard() {
        String ct = "text/html; charset=EUC-JP";
        assertEquals("EUC-JP", DataUtil.getCharsetFromContentType(ct));
    }

    @Test
    public void testGetCharsetFromContentTypeQuotesAndSpaces() {
        String ct = "text/html; charset=\"utf-8\" ";
        assertEquals("utf-8", DataUtil.getCharsetFromContentType(ct));
    }

    @Test
    public void testGetCharsetFromContentTypeMissing() {
        assertNull(DataUtil.getCharsetFromContentType("text/html"));
    }

    @Test
    public void testGetCharsetFromContentTypeUnsupported() {
        // "X-UNSUPPORTED" is not a known charset – should return null
        assertNull(DataUtil.getCharsetFromContentType("text/html; charset=X-UNSUPPORTED"));
    }

    @Test
    public void testGetCharsetFromContentTypeNull() {
        assertNull(DataUtil.getCharsetFromContentType(null));
    }

    /** --------------------------------------------------------------------
     *  Public method: mimeBoundary()
     *  -------------------------------------------------------------------- */
    @Test
    public void testMimeBoundaryLengthAndChars() {
        String boundary = DataUtil.mimeBoundary();
        assertEquals(32, boundary.length());
        for (char c : boundary.toCharArray()) {
            assertTrue("Invalid boundary character: " + c, MIME_BOUNDARY_ALLOWED.contains(c));
        }
    }

    /** --------------------------------------------------------------------
     *  Package‑private helpers (tested from same package)
     *  -------------------------------------------------------------------- */
    @Test
    public void testCrossStreamsCopiesData() throws IOException {
        byte[] src = "CrossStreamData".getBytes("UTF-8");
        ByteArrayInputStream in = new ByteArrayInputStream(src);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // crossStreams is package‑private, so we can call it directly
        DataUtil.crossStreams(in, out);
        assertArrayEquals(src, out.toByteArray());
    }

    @Test
    public void testDetectCharsetFromBomUtf8() throws IOException {
        // Build a ByteBuffer that starts with UTF‑8 BOM
        byte[] bomUtf8 = new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF, 'a', 'b'};
        ByteBuffer bb = ByteBuffer.wrap(bomUtf8);
        // Use reflection to invoke the private method
        java.lang.reflect.Method m = DataUtil.class.getDeclaredMethod("detectCharsetFromBom", ByteBuffer.class);
        m.setAccessible(true);
        Object result = m.invoke(null, bb);
        // The result should be a BomCharset with charset "UTF-8"
        assertNotNull(result);
        java.lang.reflect.Field charsetField = result.getClass().getDeclaredField("charset");
        charsetField.setAccessible(true);
        assertEquals("UTF-8", charsetField.get(result));
    }
}
```
###Test END##