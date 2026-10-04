package org.jsoup.helper;

import org.jsoup.parser.Parser;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link DataUtil}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public API methods (load, readToByteBuffer, readFileToByteBuffer)</li>
 *   <li>Package‑private utility methods that are part of the contract (getCharsetFromContentType, mimeBoundary)</li>
 *   <li>Edge‑cases, error handling and exception scenarios</li>
 *   <li>Special internal behaviours via reflection (detectCharsetFromBom, validateCharset)</li>
 * </ul>
 *
 * The suite is deliberately placed in the same package (org.jsoup.helper) so that
 * package‑private members can be accessed directly without breaking encapsulation
 * for the production code.
 */
public class DataUtilTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /* --------------------------------------------------------------------- *
     *  PUBLIC API TESTS
     * --------------------------------------------------------------------- */

    @Test
    public void loadFile_WithExplicitCharset_ReturnsDocumentWithBaseUri() throws IOException {
        // create a tiny HTML file in UTF‑8
        File htmlFile = folder.newFile("sample.html");
        String html = "<!doctype html><html><head><title>Test</title></head><body>Hello</body></html>";
        try (Writer w = new OutputStreamWriter(new FileOutputStream(htmlFile), "UTF-8")) {
            w.write(html);
        }

        Document doc = DataUtil.load(htmlFile, "UTF-8", "http://example.com/");
        assertNotNull(doc);
        assertEquals("http://example.com/", doc.baseUri());
        assertEquals("Test", doc.title());
    }

    @Test
    public void loadInputStream_NullStream_ReturnsEmptyDocumentWithBaseUri() throws IOException {
        Document doc = DataUtil.load((InputStream) null, null, "http://base/");
        assertNotNull(doc);
        assertEquals("http://base/", doc.baseUri());
        // the document should have no child nodes
        assertEquals(0, doc.childNodeSize());
    }

    @Test
    public void loadInputStream_WithParserArgument_UsesGivenParser() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><child/></root>";
        InputStream in = new ByteArrayInputStream(xml.getBytes(Charset.forName("UTF-8")));
        Document doc = DataUtil.load(in, "UTF-8", "http://xml/", Parser.xmlParser());
        assertNotNull(doc);
        // XML parser creates a document whose root element name is "root"
        Element root = doc.selectFirst("root");
        assertNotNull(root);
        assertEquals("child", root.selectFirst("child").tagName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void loadInputStream_WithEmptyCharset_ThrowsValidateException() throws IOException {
        // The Validate.notEmpty check should reject an empty charset string.
        String html = "<html></html>";
        InputStream in = new ByteArrayInputStream(html.getBytes("UTF-8"));
        DataUtil.load(in, "", "http://example.com/");
    }

    @Test
    public void readToByteBuffer_UnlimitedSize_ReadsAllBytes() throws IOException {
        byte[] data = "abcdefghijklmnopqrstuvwxyz".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        ByteBuffer buffer = DataUtil.readToByteBuffer(in, 0); // 0 == unlimited
        assertNotNull(buffer);
        assertEquals(data.length, buffer.remaining());
        byte[] readBack = new byte[buffer.remaining()];
        buffer.get(readBack);
        assertArrayEquals(data, readBack);
    }

    @Test
    public void readToByteBuffer_WithMaxSize_RespectsLimit() throws IOException {
        byte[] data = "0123456789".getBytes("UTF-8");
        InputStream in = new ByteArrayInputStream(data);
        int max = 5;
        ByteBuffer buffer = DataUtil.readToByteBuffer(in, max);
        assertNotNull(buffer);
        assertEquals(max, buffer.remaining());
        byte[] expected = new byte[max];
        System.arraycopy(data, 0, expected, 0, max);
        byte[] actual = new byte[max];
        buffer.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void readToByteBuffer_NegativeMaxSize_ThrowsException() throws IOException {
        InputStream in = new ByteArrayInputStream(new byte[0]);
        DataUtil.readToByteBuffer(in, -1);
    }

    @Test
    public void readFileToByteBuffer_ReadsExactFileContent() throws IOException {
        File file = folder.newFile("binary.dat");
        byte[] payload = new byte[256];
        new Random().nextBytes(payload);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(payload);
        }

        ByteBuffer buffer = DataUtil.readFileToByteBuffer(file);
        assertNotNull(buffer);
        assertEquals(payload.length, buffer.remaining());
        byte[] readBack = new byte[buffer.remaining()];
        buffer.get(readBack);
        assertArrayEquals(payload, readBack);
    }

    /* --------------------------------------------------------------------- *
     *  PACKAGE‑PRIVATE UTILITY METHOD TESTS
     * --------------------------------------------------------------------- */

    @Test
    public void getCharsetFromContentType_ValidHeader_ReturnsCharset() {
        assertEquals("UTF-8", DataUtil.getCharsetFromContentType("text/html; charset=UTF-8"));
        assertEquals("ISO-8859-1", DataUtil.getCharsetFromContentType("text/plain; charset=ISO-8859-1"));
        // also works with quotes and whitespace
        assertEquals("EUC-JP", DataUtil.getCharsetFromContentType("text/html; charset=\"EUC-JP\""));
        assertEquals("GB2312", DataUtil.getCharsetFromContentType("   text/html ; charset=  GB2312  "));
    }

    @Test
    public void getCharsetFromContentType_InvalidOrMissing_ReturnsNull() {
        assertNull(DataUtil.getCharsetFromContentType(null));
        assertNull(DataUtil.getCharsetFromContentType("text/html"));
        assertNull(DataUtil.getCharsetFromContentType("charset=UNKNOWN"));
        // unsupported charset should also result in null
        assertNull(DataUtil.getCharsetFromContentType("text/html; charset=UNSUPPORTED-CHARSET"));
    }

    @Test
    public void mimeBoundary_GeneratesStringOfCorrectLengthAndAllowedChars() {
        String boundary = DataUtil.mimeBoundary();
        assertNotNull(boundary);
        assertEquals(DataUtil.boundaryLength, boundary.length());

        // ensure each character belongs to the allowed set
        for (char c : boundary.toCharArray()) {
            boolean allowed = false;
            for (char allowedChar : DataUtil.mimeBoundaryChars) {
                if (c == allowedChar) {
                    allowed = true;
                    break;
                }
            }
            assertTrue("Found illegal character in mime boundary: " + c, allowed);
        }
    }

    /* --------------------------------------------------------------------- *
     *  REFLECTION‑BASED INTERNAL BEHAVIOUR TESTS
     * --------------------------------------------------------------------- */

    @Test
    public void detectCharsetFromBom_Utf8Bom_ReturnsUtf8BomCharset() throws Exception {
        Method m = DataUtil.class.getDeclaredMethod("detectCharsetFromBom", ByteBuffer.class);
        m.setAccessible(true);

        byte[] bom = new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF, 0x41, 0x42}; // UTF‑8 BOM + "AB"
        ByteBuffer bb = ByteBuffer.wrap(bom);
        Object result = m.invoke(null, bb);
        assertNotNull(result);

        // The returned object is DataUtil.BomCharset – use reflection to read its fields
        Class<?> bomCls = result.getClass();
        String charset = (String) bomCls.getDeclaredField("charset").get(result);
        int offset = (int) bomCls.getDeclaredField("offset").get(result);
        assertEquals("UTF-8", charset);
        assertEquals(3, offset);
    }

    @Test
    public void validateCharset_UnsupportedCharset_ReturnsNullViaReflection() throws Exception {
        // The private validateCharset method is exercised indirectly via getCharsetFromContentType,
        // but we also verify it directly for completeness.
        Method m = DataUtil.class.getDeclaredMethod("validateCharset", String.class);
        m.setAccessible(true);

        // supported charset
        assertEquals("UTF-8", m.invoke(null, "UTF-8"));
        // supported but lower‑case (method should return as‑is)
        assertEquals("utf-8", m.invoke(null, "utf-8"));
        // unsupported charset
        assertNull(m.invoke(null, "UNSUPPORTED-CHARSET"));
        // illegal charset name
        assertNull(m.invoke(null, "123-!@#"));
    }

    @Test
    public void parseInputStream_WithBOMAndExplicitCharset_SkipsBomCorrectly() throws IOException {
        // Content with UTF‑8 BOM followed by a simple HTML document.
        byte[] bom = new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF};
        String html = "<html><head><meta charset=\"UTF-8\"></head><body>bom</body></html>";
        byte[] payload = (new StringBuilder()).append(new String(bom, "ISO-8859-1"))
                .append(html).toString().getBytes("UTF-8");

        InputStream in = new ByteArrayInputStream(payload);
        Document doc = DataUtil.load(in, null, "http://bomtest/");
        assertNotNull(doc);
        // The document should contain the body text "bom" – if the BOM were not skipped,
        // it would appear as an invisible character and cause parsing issues.
        assertEquals("bom", doc.body().text());
    }

    /* --------------------------------------------------------------------- *
     *  HELPER METHODS
     * --------------------------------------------------------------------- */

    /**
     * Helper to create a temporary file containing the given string using the supplied charset.
     */
    private File writeTempFile(String content, String charset) throws IOException {
        File f = folder.newFile("temp_" + System.nanoTime() + ".txt");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), charset)) {
            w.write(content);
        }
        return f;
    }
}
