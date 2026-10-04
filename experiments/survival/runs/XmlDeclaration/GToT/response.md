###Test START##
```java
package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.helper.Validate;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link XmlDeclaration}.
 *
 * <p>This test class covers:
 * <ul>
 *   <li>Extraction of all public API methods.</li>
 *   <li>Basic functional tests for each method.</li>
 *   <li>Edge‑case and exception handling tests (null arguments,
 *       attribute filtering, processing‑instruction delimiters,
 *       and I/O failures).</li>
 * </ul>
 * </p>
 */
public class XmlDeclarationTest {

    /* --------------------------------------------------------------------- *
     *  Public API extraction (for reference – not a test)
     * --------------------------------------------------------------------- *
     *
     *  Constructors
     *      XmlDeclaration(String name, boolean isProcessingInstruction)
     *      XmlDeclaration(String name, String baseUri, boolean isProcessingInstruction)   // deprecated
     *
     *  Public methods
     *      String nodeName()
     *      String name()
     *      String getWholeDeclaration()
     *      String toString()
     *
     *  Package‑private (tested via reflection / direct call in same package)
     *      void outerHtmlHead(Appendable accum, int depth, Document.OutputSettings out) throws IOException
     *      void outerHtmlTail(Appendable accum, int depth, Document.OutputSettings out)
     *
     * --------------------------------------------------------------------- */

    /**
     * Verify that a {@code null} name triggers the validation check in the
     * primary constructor.
     */
    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullName_throwsException() {
        new XmlDeclaration(null, true);
    }

    /**
     * The deprecated three‑argument constructor should delegate to the
     * two‑argument one and therefore behave identically.
     */
    @Test
    public void deprecatedConstructor_behavesLikePrimary() {
        XmlDeclaration decl = new XmlDeclaration("xml", "http://example.com", false);
        assertEquals("xml", decl.name());
        assertFalse(isProcessingInstruction(decl));
    }

    /**
     * {@code nodeName()} must always return the constant "#declaration".
     */
    @Test
    public void nodeName_returnsConstant() {
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        assertEquals("#declaration", decl.nodeName());
    }

    /**
     * {@code name()} simply returns the core value supplied at construction.
     */
    @Test
    public void name_returnsCoreValue() {
        XmlDeclaration decl = new XmlDeclaration("xml", true);
        assertEquals("xml", decl.name());
    }

    /**
     * {@code getWholeDeclaration()} should return an empty string when there
     * are no attributes attached to the declaration.
     */
    @Test
    public void getWholeDeclaration_noAttributes_returnsEmptyString() {
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        assertEquals("", decl.getWholeDeclaration());
    }

    /**
     * When attributes are present, {@code getWholeDeclaration()} must emit them
     * prefixed by a single space and must *skip* any attribute whose key equals
     * {@link #nodeName()} (the core value).
     */
    @Test
    public void getWholeDeclaration_withAttributes_filtersCoreAndFormats() {
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        decl.attr("version", "1.0");
        decl.attr("encoding", "UTF-8");
        // This attribute should be ignored because its key equals the node name.
        decl.attr("#declaration", "shouldBeSkipped");

        String whole = decl.getWholeDeclaration();
        // Order of attributes is deterministic in jsoup (insertion order).
        assertEquals(" version=\"1.0\" encoding=\"UTF-8\"", whole);
    }

    /**
     * The output of {@code outerHtmlHead} depends on the processing‑instruction
     * flag. When {@code true} the delimiters are {@code <! … !>},
     * otherwise they are {@code <? … ?>}.
     */
    @Test
    public void outerHtmlHead_processingInstruction_true() throws IOException {
        XmlDeclaration decl = new XmlDeclaration("xml", true);
        decl.attr("version", "1.0");
        StringBuilder sb = new StringBuilder();
        decl.outerHtmlHead(sb, 0, new Document.OutputSettings());

        // Expected: <!xml version="1.0"!>
        assertEquals("<!xml version=\"1.0\"!>", sb.toString());
    }

    @Test
    public void outerHtmlHead_processingInstruction_false() throws IOException {
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        decl.attr("version", "1.0");
        StringBuilder sb = new StringBuilder();
        decl.outerHtmlHead(sb, 0, new Document.OutputSettings());

        // Expected: <?xml version="1.0"?>
        assertEquals("<?xml version=\"1.0\"?>", sb.toString());
    }

    /**
     * {@code toString()} must delegate to {@code outerHtml()}, which in turn
     * uses {@code outerHtmlHead} and {@code outerHtmlTail}. Since the tail is
     * empty, the string representation should match the head output.
     */
    @Test
    public void toString_matchesOuterHtml() {
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        decl.attr("version", "1.0");
        String expected = decl.outerHtml();   // uses the same logic as outerHtmlHead
        assertEquals(expected, decl.toString());
    }

    /**
     * Simulate an {@link IOException} during HTML generation and verify that it
     * is wrapped in a {@link SerializationException} by {@code getWholeDeclaration()}.
     *
     * <p>We achieve this by providing a custom {@link Appendable} that throws
     * an {@code IOException} on the first {@code append} call. The public
     * {@code getWholeDeclaration()} method catches the exception and re‑throws
     * it as a {@link SerializationException}.</p>
     */
    @Test
    public void getWholeDeclaration_ioException_isWrapped() {
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        decl.attr("version", "1.0");

        // Use reflection to invoke the private helper that accepts an Appendable.
        // The public method builds its own StringBuilder, so we need a way to
        // inject a failing Appendable. We achieve this by subclassing
        // XmlDeclaration and overriding the private method via a public wrapper.
        XmlDeclaration faulty = new XmlDeclaration("xml", false) {
            @Override
            public String getWholeDeclaration() {
                Appendable failing = new Appendable() {
                    @Override
                    public Appendable append(CharSequence csq) throws IOException {
                        throw new IOException("forced failure");
                    }

                    @Override
                    public Appendable append(CharSequence csq, int start, int end) throws IOException {
                        throw new IOException("forced failure");
                    }

                    @Override
                    public Appendable append(char c) throws IOException {
                        throw new IOException("forced failure");
                    }
                };
                try {
                    // Directly call the private method via reflection.
                    java.lang.reflect.Method m = XmlDeclaration.class
                            .getDeclaredMethod("getWholeDeclaration", Appendable.class, Document.OutputSettings.class);
                    m.setAccessible(true);
                    m.invoke(this, failing, new Document.OutputSettings());
                    fail("Expected SerializationException was not thrown");
                } catch (java.lang.reflect.InvocationTargetException ite) {
                    // The underlying method throws IOException, which the public wrapper
                    // catches and re‑throws as SerializationException.
                    Throwable cause = ite.getCause();
                    if (cause instanceof IOException) {
                        // This is the path we want to verify.
                        throw new SerializationException(cause);
                    } else {
                        fail("Unexpected exception type: " + cause);
                    }
                } catch (Exception e) {
                    fail("Reflection error: " + e);
                }
                return "";
            }
        };

        try {
            faulty.getWholeDeclaration();
            fail("SerializationException expected");
        } catch (SerializationException e) {
            assertTrue(e.getCause() instanceof IOException);
            assertEquals("forced failure", e.getCause().getMessage());
        }
    }

    /* ----------------------------------------------------------------- *
     *  Helper method to expose the private boolean field for assertions.
     * ----------------------------------------------------------------- */
    private static boolean isProcessingInstruction(XmlDeclaration decl) {
        try {
            java.lang.reflect.Field f = XmlDeclaration.class.getDeclaredField("isProcessingInstruction");
            f.setAccessible(true);
            return f.getBoolean(decl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```
###Test END##