###Test START##
```java
package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.helper.Validate;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link XmlDeclaration}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction and validation of arguments.</li>
 *   <li>Core getters: {@code nodeName()}, {@code name()}, {@code getWholeDeclaration()}.</li>
 *   <li>HTML rendering via {@code outerHtmlHead()} and the no‑op {@code outerHtmlTail()}.</li>
 *   <li>{@code toString()} delegation to {@code outerHtml()}.</li>
 *   <li>Exception handling when an {@link Appendable} throws {@link IOException}.</li>
 * </ul>
 */
public class XmlDeclarationTest {

    private XmlDeclaration processingInstruction;
    private XmlDeclaration declaration;

    @Before
    public void setUp() {
        processingInstruction = new XmlDeclaration("xml", true);
        declaration = new XmlDeclaration("xml", false);
    }

    /*** Constructor validation ***/
    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNullName() {
        new XmlDeclaration(null, true);
    }

    /*** Basic getters ***/
    @Test
    public void nodeNameIsConstant() {
        assertEquals("#declaration", processingInstruction.nodeName());
        assertEquals("#declaration", declaration.nodeName());
    }

    @Test
    public void nameReturnsCoreValue() {
        assertEquals("xml", processingInstruction.name());
        assertEquals("xml", declaration.name());
    }

    /*** getWholeDeclaration – attribute handling ***/
    @Test
    public void getWholeDeclarationFormatsAttributesCorrectly() {
        processingInstruction.attr("version", "1.0");
        processingInstruction.attr("encoding", "UTF-8");

        // The method trims leading/trailing whitespace, so the result should be a space‑separated
        // list of attributes without a leading space.
        String whole = processingInstruction.getWholeDeclaration();
        assertEquals("version=\"1.0\" encoding=\"UTF-8\"", whole);
    }

    /*** outerHtmlHead – processing instruction (<! … !>) ***/
    @Test
    public void outerHtmlHeadProducesProcessingInstructionSyntax() throws IOException {
        processingInstruction.attr("version", "1.0");
        processingInstruction.attr("encoding", "UTF-8");

        StringBuilder sb = new StringBuilder();
        processingInstruction.outerHtmlHead(sb, 0, new Document.OutputSettings());

        // Expected: <!xml version="1.0" encoding="UTF-8"!>
        assertEquals("<!xml version=\"1.0\" encoding=\"UTF-8\"!>", sb.toString());
    }

    /*** outerHtmlHead – normal declaration (<? … ?>) ***/
    @Test
    public void outerHtmlHeadProducesDeclarationSyntax() throws IOException {
        declaration.attr("version", "1.0");
        declaration.attr("encoding", "UTF-8");

        StringBuilder sb = new StringBuilder();
        declaration.outerHtmlHead(sb, 0, new Document.OutputSettings());

        // Expected: <?xml version="1.0" encoding="UTF-8"?>
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", sb.toString());
    }

    /*** outerHtmlTail – should be a no‑op ***/
    @Test
    public void outerHtmlTailDoesNothing() throws IOException {
        StringBuilder sb = new StringBuilder("already");
        processingInstruction.outerHtmlTail(sb, 0, new Document.OutputSettings());

        // No change expected.
        assertEquals("already", sb.toString());
    }

    /*** toString delegates to outerHtml ***/
    @Test
    public void toStringEqualsOuterHtml() {
        processingInstruction.attr("standalone", "yes");
        String outer = processingInstruction.outerHtml(); // provided by Node/LeafNode
        assertEquals(outer, processingInstruction.toString());
    }

    /*** Private getWholeDeclaration should propagate IOException ***/
    @Test
    public void privateGetWholeDeclarationPropagatesIOException() throws Exception {
        // Create an Appendable that always throws IOException.
        Appendable failingAppendable = new Appendable() {
            @Override public Appendable append(CharSequence csq) throws IOException {
                throw new IOException("forced failure");
            }
            @Override public Appendable append(CharSequence csq, int start, int end) throws IOException {
                throw new IOException("forced failure");
            }
            @Override public Appendable append(char c) throws IOException {
                throw new IOException("forced failure");
            }
        };

        // Access the private method via reflection.
        Method privateMethod = XmlDeclaration.class.getDeclaredMethod(
                "getWholeDeclaration", Appendable.class, Document.OutputSettings.class);
        privateMethod.setAccessible(true);

        try {
            privateMethod.invoke(processingInstruction, failingAppendable, new Document.OutputSettings());
            fail("Expected an IOException wrapped in InvocationTargetException");
        } catch (java.lang.reflect.InvocationTargetException e) {
            // The underlying cause should be an IOException.
            assertTrue(e.getCause() instanceof IOException);
            assertEquals("forced failure", e.getCause().getMessage());
        }
    }

    /*** Public getWholeDeclaration should never throw SerializationException for normal use ***/
    @Test
    public void getWholeDeclarationDoesNotThrowWhenUsingStringBuilder() {
        // Normal usage – internal StringBuilder never throws.
        processingInstruction.attr("foo", "bar");
        String result = processingInstruction.getWholeDeclaration();
        assertEquals("foo=\"bar\"", result);
    }

    /*** Ensure that the deprecated constructor forwards correctly ***/
    @Test
    public void deprecatedConstructorForwardsToPrimary() {
        XmlDeclaration deprecated = new XmlDeclaration("test", "http://example.com", false);
        assertEquals("test", deprecated.name());
        // The flag must be false (declaration, not processing instruction)
        StringBuilder sb = new StringBuilder();
        deprecated.outerHtmlHead(sb, 0, new Document.OutputSettings());
        assertEquals("<?test?>", sb.toString());
    }
}
```
###Test END##