/**
 * JUnit‑4 test suite for {@link org.jsoup.nodes.XmlDeclaration}.
 *
 * The tests are written from the perspective of three independent “experts”.
 * Each expert contributes one test case for every public (or package‑private) method,
 * covering normal usage, edge cases and error handling.  The suite can be run with
 * any JUnit‑4 runner (e.g. Maven Surefire, IDE integration, etc.).
 *
 * The jsoup library (version 1.17.2 or later) must be on the test class‑path.
 */
package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.junit.Assert.*;

public class XmlDeclarationTest {

    /* --------------------------------------------------------------------- */
    /* 1. Constructor validation – null name should raise an exception.      */
    /* --------------------------------------------------------------------- */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void constructorShouldThrowWhenNameIsNull() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Object must not be null");
        new XmlDeclaration(null, true);
    }

    /* --------------------------------------------------------------------- */
    /* 2. nodeName() – must always return the constant "#declaration".       */
    /* --------------------------------------------------------------------- */
    @Test
    public void nodeNameReturnsConstant() {
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        assertEquals("#declaration", decl.nodeName());
    }

    /* --------------------------------------------------------------------- */
    /* 3. name() – should expose the core value (the declaration name).      */
    /* --------------------------------------------------------------------- */
    @Test
    public void nameReturnsCoreValue() {
        XmlDeclaration decl = new XmlDeclaration("xml", true);
        assertEquals("xml", decl.name());
    }

    /* --------------------------------------------------------------------- */
    /* 4. getWholeDeclaration() – no attributes → empty string.              */
    /* --------------------------------------------------------------------- */
    @Test
    public void getWholeDeclarationWithoutAttributesIsEmpty() {
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        assertEquals("", decl.getWholeDeclaration());
    }

    /* --------------------------------------------------------------------- */
    /* 5. getWholeDeclaration() – with attributes → space‑separated list.    */
    /* --------------------------------------------------------------------- */
    @Test
    public void getWholeDeclarationWithAttributes() {
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        decl.attr("version", "1.0");
        decl.attr("encoding", "utf-8");

        // The public method trims the leading space, so we expect a clean list.
        String expected = "version=\"1.0\" encoding=\"utf-8\"";
        assertEquals(expected, decl.getWholeDeclaration());
    }

    /* --------------------------------------------------------------------- */
    /* 6. outerHtmlHead() – processing‑instruction (isProcessingInstruction = true) */
    /*    without attributes.                                                */
    /* --------------------------------------------------------------------- */
    @Test
    public void outerHtmlHeadProcessingInstructionWithoutAttributes() throws IOException {
        XmlDeclaration decl = new XmlDeclaration("xml", true);
        StringBuilder sb = new StringBuilder();
        decl.outerHtmlHead(sb, 0, new Document.OutputSettings());

        // Expected format: <!xml!>
        assertEquals("<!xml!>", sb.toString());
    }

    /* --------------------------------------------------------------------- */
    /* 7. outerHtmlHead() – declaration (isProcessingInstruction = false)    */
    /*    with attributes.                                                   */
    /* --------------------------------------------------------------------- */
    @Test
    public void outerHtmlHeadDeclarationWithAttributes() throws IOException {
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        decl.attr("version", "1.0");
        decl.attr("encoding", "utf-8");

        StringBuilder sb = new StringBuilder();
        decl.outerHtmlHead(sb, 0, new Document.OutputSettings());

        // Expected format: <?xml version="1.0" encoding="utf-8"?>
        String expected = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
        assertEquals(expected, sb.toString());
    }

    /* --------------------------------------------------------------------- */
    /* 8. outerHtmlTail() – should be a no‑op and never throw.               */
    /* --------------------------------------------------------------------- */
    @Test
    public void outerHtmlTailDoesNothing() {
        XmlDeclaration decl = new XmlDeclaration("xml", true);
        // The method is void; we simply ensure it does not throw.
        decl.outerHtmlTail(new StringBuilder(), 0, new Document.OutputSettings());
    }

    /* --------------------------------------------------------------------- */
    /* 9. toString() – must delegate to outerHtml() (which uses outerHtmlHead). */
    /* --------------------------------------------------------------------- */
    @Test
    public void toStringProducesSameOutputAsOuterHtml() {
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        decl.attr("standalone", "yes");

        String expected = decl.outerHtml(); // outerHtml() is defined in Node
        String actual = decl.toString();

        assertEquals(expected, actual);
    }

    /* --------------------------------------------------------------------- */
    /* 10. getWholeDeclaration() error handling – simulate an IOException */
    /*     inside the private helper (via reflection) and verify that the   */
    /*     wrapped exception is a SerializationException.                  */
    /* --------------------------------------------------------------------- */
    @Test
    public void getWholeDeclarationWrapsIOExceptionInSerializationException() throws Exception {
        // Use a subclass to inject a failing Appendable.
        XmlDeclaration decl = new XmlDeclaration("xml", false) {
            @Override
            public String getWholeDeclaration() {
                Appendable failingAppendable = new Appendable() {
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
                    // Directly invoke the private helper via reflection.
                    java.lang.reflect.Method m = XmlDeclaration.class
                            .getDeclaredMethod("getWholeDeclaration", Appendable.class, Document.OutputSettings.class);
                    m.setAccessible(true);
                    m.invoke(this, failingAppendable, new Document.OutputSettings());
                    fail("Expected SerializationException");
                    return null; // unreachable
                } catch (java.lang.reflect.InvocationTargetException ite) {
                    // The invoked method throws SerializationException wrapping IOException.
                    Throwable cause = ite.getCause();
                    assertTrue(cause instanceof SerializationException);
                    assertTrue(cause.getCause() instanceof IOException);
                    assertEquals("forced failure", cause.getCause().getMessage());
                    // Re‑throw to satisfy the test framework (JUnit will see it as a success).
                    return null;
                } catch (Exception e) {
                    fail("Unexpected exception: " + e);
                    return null;
                }
            }
        };
        // Trigger the overridden method – the assertions are inside it.
        decl.getWholeDeclaration();
    }
}
