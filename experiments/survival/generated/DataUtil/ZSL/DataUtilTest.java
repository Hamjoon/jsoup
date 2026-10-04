package org.jsoup.helper;

import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class DataUtilTest {
    private File tempFile;

    @Before
    public void setUp() throws IOException {
        // create a temporary file for readFileToByteBuffer tests
        tempFile = File.createTempFile("jsoup-datautil-test", ".txt");
        tempFile.deleteOnExit();
    }

    @After
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    /** Test that crossStreams copies all bytes unchanged. */
    @Test
    public void testCrossStreams() throws IOException {
        byte[] source = new byte[1024];
        for (int i = 0; i < source.length; i++) source[i] = (byte) i;
        InputStream in = new ByteArrayInputStream(source);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataUtil.crossStreams(in, out);
        assertArrayEquals(source, out.toByteArray());
    }

    /** Test readToByteBuffer with a limited maxSize. */
    @Test
    public void testReadToByteBufferLimited() throws IOException {
        byte[] data = new byte[200];
        for (int i = 0; i < data.length; i++) data[i] = (byte) i;
        InputStream in = new ByteArrayInputStream(data);
        int maxSize = 50;
        ByteBuffer buffer = DataUtil.readToByteBuffer(in, maxSize);
        assertEquals(maxSize, buffer.remaining());
        for (int i = 0; i < maxSize; i++) {
            assertEquals((byte) i, buffer.get(i));
        }
    }

    /** Test readToByteBuffer with unlimited size (maxSize = 0). */
    @Test
    public void testReadToByteBufferUnlimited() throws IOException {
        String text = "The quick brown fox jumps over the lazy dog";
        byte[] data = text.getBytes(Charset.forName("UTF-8"));
        InputStream in = new ByteArrayInputStream(data);
        ByteBuffer buffer = DataUtil.readToByteBuffer(in, 0);
        assertEquals(data.length, buffer.remaining());
        byte[] read = new byte[data.length];
        buffer.get(read);
        assertArrayEquals(data, read);
    }

    /** Test readFileToByteBuffer reads the exact file contents. */
    @Test
    public void testReadFileToByteBuffer() throws IOException {
        String content = "Hello, file!";
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write(content);
        }
        ByteBuffer buffer = DataUtil.readFileToByteBuffer(tempFile);
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        assertEquals(content, new String(bytes, Charset.forName("UTF-8")));
    }

    /** emptyByteBuffer should return a buffer with capacity 0. */
    @Test
    public void testEmptyByteBuffer() {
        ByteBuffer empty = DataUtil.emptyByteBuffer();
        assertEquals(0, empty.capacity());
        assertEquals(0, empty.remaining());
    }

    /** getCharsetFromContentType parses various valid and invalid inputs. */
    @Test
    public void testGetCharsetFromContentType() {
        // null input
        assertNull(DataUtil.getCharsetFromContentType(null));

        // simple charset
        assertEquals("EUC-JP", DataUtil.getCharsetFromContentType("text/html; charset=EUC-JP"));

        // spaces and quotes
        assertEquals("utf-8", DataUtil.getCharsetFromContentType("text/html; charset=\"utf-8\""));

        // upper‑case charset name (should be returned as‑is if supported)
        assertEquals("ISO-8859-1", DataUtil.getCharsetFromContentType("text/plain; charset=ISO-8859-1"));

        // unsupported charset => null
        assertNull(DataUtil.getCharsetFromContentType("text/html; charset=UNSUPPORTED-CHARSET"));

        // no charset present
        assertNull(DataUtil.getCharsetFromContentType("application/json"));
    }

    /** mimeBoundary should return a string of length 32 consisting only of allowed characters. */
    @Test
    public void testMimeBoundary() {
        String boundary = DataUtil.mimeBoundary();
        assertEquals(DataUtil.boundaryLength, boundary.length());

        Set<Character> allowed = new HashSet<>();
        for (char c : "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()) {
            allowed.add(c);
        }
        for (char c : boundary.toCharArray()) {
            assertTrue("Invalid character in mime boundary: " + c, allowed.contains(c));
        }
    }

    /** parseInputStream should return an empty Document when the InputStream is null. */
    @Test
    public void testParseInputStreamNullInput() throws IOException {
        String baseUri = "http://example.com/";
        Document doc = DataUtil.parseInputStream(null, null, baseUri, Parser.htmlParser());
        assertNotNull(doc);
        assertEquals(baseUri, doc.baseUri());
        // No elements should be present
        assertTrue(doc.select("*").isEmpty());
    }

    /** parseInputStream should detect charset from a UTF‑8 BOM. */
    @Test
    public void testParseInputStreamBomUtf8() throws IOException {
        // UTF‑8 BOM + UTF‑8 encoded HTML containing a non‑ASCII character
        byte[] bom = new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF};
        String html = "<html><head></head><body>é</body></html>";
        byte[] htmlBytes = html.getBytes(Charset.forName("UTF-8"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(bom);
        out.write(htmlBytes);
        InputStream in = new ByteArrayInputStream(out.toByteArray());

        Document doc = DataUtil.parseInputStream(in, null, "http://test/", Parser.htmlParser());
        // The charset should be UTF‑8 (derived from BOM) and the character must be decoded correctly.
        assertEquals("UTF-8", doc.outputSettings().charset().name());
        assertEquals("é", doc.body().text());
    }

    /** parseInputStream should fallback to meta charset when no BOM is present. */
    @Test
    public void testParseInputStreamMetaCharset() throws IOException {
        // HTML encoded in ISO‑8859‑1 with a meta charset declaration
        String html = "<html><head><meta charset=\"ISO-8859-1\"></head><body>é</body></html>";
        byte[] bytes = html.getBytes(Charset.forName("ISO-8859-1"));
        InputStream in = new ByteArrayInputStream(bytes);

        Document doc = DataUtil.parseInputStream(in, null, "http://test/", Parser.htmlParser());
        // The parser should have re‑decoded using ISO‑8859‑1
        assertEquals("ISO-8859-1", doc.outputSettings().charset().name());
        assertEquals("é", doc.body().text());
    }

    /** parseInputStream should throw when an empty charset name is supplied (Validate.notEmpty). */
    @Test(expected = IllegalArgumentException.class)
    public void testParseInputStreamEmptyCharsetThrows() throws IOException {
        String html = "<html></html>";
        InputStream in = new ByteArrayInputStream(html.getBytes(Charset.forName("UTF-8")));
        // Supplying an empty string forces Validate.notEmpty to fail
        DataUtil.parseInputStream(in, "", "http://example.com", Parser.htmlParser());
    }

    /** Ensure that detectCharsetFromBom correctly identifies UTF‑16 and UTF‑32 signatures via reflection. */
    @Test
    public void testDetectCharsetFromBomViaReflection() throws Exception {
        // Helper to invoke the private static method
        java.lang.reflect.Method m = DataUtil.class.getDeclaredMethod("detectCharsetFromBom", ByteBuffer.class);
        m.setAccessible(true);

        // UTF‑16 BE BOM
        byte[] utf16be = new byte[]{(byte)0xFE, (byte)0xFF, 0x00, 0x41};
        ByteBuffer buf16 = ByteBuffer.wrap(utf16be);
        Object result16 = m.invoke(null, buf16);
        assertNotNull(result16);
        java.lang.reflect.Field charsetField = result16.getClass().getDeclaredField("charset");
        charsetField.setAccessible(true);
        assertEquals("UTF-16", charsetField.get(result16));

        // UTF‑32 LE BOM
        byte[] utf32le = new byte[]{(byte)0xFF, (byte)0xFE, 0x00, 0x00, 0x41, 0x00, 0x00, 0x00};
        ByteBuffer buf32 = ByteBuffer.wrap(utf32le);
        Object result32 = m.invoke(null, buf32);
        assertNotNull(result32);
        java.lang.reflect.Field charsetField32 = result32.getClass().getDeclaredField("charset");
        charsetField32.setAccessible(true);
        assertEquals("UTF-32", charsetField32.get(result32));
    }
}
