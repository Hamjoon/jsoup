###Test START##
package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.helper.Validate;
import org.junit.Test;

import static org.junit.Assert.*;

public class XmlDeclarationTest {

    @Test
    public void testConstructorAndName() {
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        assertEquals("xml", decl.name());
        assertEquals("#declaration", decl.nodeName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullNameThrows() {
        new XmlDeclaration(null, false);
    }

    @Test
    public void testDeprecatedConstructor() {
        // The deprecated constructor simply forwards to the primary one
        XmlDeclaration decl = new XmlDeclaration("xml", "http://example.com", true);
        assertEquals("xml", decl.name());
        assertTrue(decl.isProcessingInstruction);
    }

    @Test
    public void testGetWholeDeclarationTrims() {
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        decl.attr("version", "1.0");
        decl.attr("encoding", "UTF-8");

        // The method should return the attributes part, without leading/trailing whitespace
        String whole = decl.getWholeDeclaration();
        // order of attributes is not guaranteed – check both possibilities
        assertTrue(
                whole.equals("version=\"1.0\" encoding=\"UTF-8\"") ||
                whole.equals("encoding=\"UTF-8\" version=\"1.0\"")
        );
    }

    @Test
    public void testOuterHtmlHeadProcessingInstructionFalse() throws Exception {
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        decl.attr("version", "1.0");

        StringBuilder sb = new StringBuilder();
        decl.outerHtmlHead(sb, 0, new Document.OutputSettings());

        // outerHtmlTail does nothing, but we call it to mimic the full rendering flow
        decl.outerHtmlTail(sb, 0, new Document.OutputSettings());

        // Expected output: <?xml version="1.0"?>
        assertEquals("<?xml version=\"1.0\"?>", sb.toString());
    }

    @Test
    public void testOuterHtmlHeadProcessingInstructionTrue() throws Exception {
        XmlDeclaration decl = new XmlDeclaration("xml", true);
        decl.attr("version", "1.0");

        StringBuilder sb = new StringBuilder();
        decl.outerHtmlHead(sb, 0, new Document.OutputSettings());
        decl.outerHtmlTail(sb, 0, new Document.OutputSettings());

        // According to the implementation, a processing‑instruction flag uses <! … !>
        assertEquals("<!xml version=\"1.0\"!>", sb.toString());
    }

    @Test
    public void testOuterHtmlTailDoesNotAlterAccumulator() throws Exception {
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        StringBuilder sb = new StringBuilder("PRE");
        decl.outerHtmlTail(sb, 0, new Document.OutputSettings());
        assertEquals("PRE", sb.toString());
    }

    @Test
    public void testToStringDelegatesToOuterHtml() {
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        decl.attr("version", "1.0");
        // toString() should produce the same as manually building the outer HTML
        String expected = "<?xml version=\"1.0\"?>";
        assertEquals(expected, decl.toString());
    }

    @Test
    public void testGetWholeDeclarationWhenNoAttributes() {
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        // No attributes added – the whole declaration should be an empty string after trimming
        assertEquals("", decl.getWholeDeclaration());
    }

    @Test
    public void testSerializationExceptionWrapped() {
        // Force an IOException by supplying a faulty Appendable
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        Appendable faulty = new Appendable() {
            @Override
            public Appendable append(CharSequence csq) throws IOException {
                throw new IOException("forced");
            }
            @Override
            public Appendable append(CharSequence csq, int start, int end) throws IOException {
                throw new IOException("forced");
            }
            @Override
            public Appendable append(char c) throws IOException {
                throw new IOException("forced");
            }
        };
        try {
            // Use reflection to invoke the private getWholeDeclaration(Appendable, Document.OutputSettings)
            java.lang.reflect.Method m = XmlDeclaration.class.getDeclaredMethod("getWholeDeclaration", Appendable.class, Document.OutputSettings.class);
            m.setAccessible(true);
            m.invoke(decl, faulty, new Document.OutputSettings());
            fail("Expected SerializationException");
        } catch (Exception e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof SerializationException);
            assertTrue(cause.getCause() instanceof IOException);
            assertEquals("forced", cause.getCause().getMessage());
        }
    }
}
###Test END##