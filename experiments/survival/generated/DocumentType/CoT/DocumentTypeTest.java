package org.jsoup.nodes;

import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Document.OutputSettings.Syntax;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link DocumentType}.
 *
 * The test class follows the requested steps:
 * <ol>
 *   <li>List all public methods (including constructors).</li>
 *   <li>Provide a basic test for each method.</li>
 *   <li>Identify edge‑cases and exception scenarios.</li>
 *   <li>Add dedicated tests for those edge‑cases.</li>
 *   <li>Combine everything into a single file.</li>
 * </ol>
 */
public class DocumentTypeTest {

    /* -------------------------------------------------------
     *  1. Public API extracted from DocumentType
     * -------------------------------------------------------
     *
     *  Constructors
     *  -----------------
     *  DocumentType(String name, String publicId, String systemId)
     *  DocumentType(String name, String publicId, String systemId, String baseUri)          // deprecated
     *  DocumentType(String name, String pubSysKey, String publicId, String systemId,
     *               String baseUri)                                                    // deprecated
     *
     *  Methods
     *  -----------------
     *  void setPubSysKey(String value)
     *  String nodeName()
     *
     *  (Inherited package‑private methods `outerHtmlHead` and `outerHtmlTail`
     *   are exercised via reflection because they are part of the observable
     *   behaviour of the class.)
     */

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* -------------------------------------------------------
     *  2. Basic tests for each public method / constructor
     * ------------------------------------------------------- */

    @Test
    public void constructorThreeArgs_setsAttributesCorrectly() {
        DocumentType dt = new DocumentType("html", "-//W3C//DTD XHTML 1.0 Transitional//EN",
                "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd");
        // name attribute
        assertEquals("html", dt.attr("name"));
        // publicId attribute
        assertEquals("-//W3C//DTD XHTML 1.0 Transitional//EN", dt.attr("publicId"));
        // systemId attribute
        assertEquals("http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd", dt.attr("systemId"));
        // pubSysKey should be set to "PUBLIC" because publicId is non‑blank
        assertEquals(DocumentType.PUBLIC_KEY, dt.attr("pubSysKey"));
    }

    @Test
    public void deprecatedConstructorFourArgs_behavesLikeThreeArgConstructor() {
        DocumentType dt = new DocumentType("html", "-//W3C//DTD XHTML 1.0 Transitional//EN",
                "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd", "http://example.com");
        assertEquals("html", dt.attr("name"));
        assertEquals("-//W3C//DTD XHTML 1.0 Transitional//EN", dt.attr("publicId"));
        assertEquals("http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd", dt.attr("systemId"));
        assertEquals(DocumentType.PUBLIC_KEY, dt.attr("pubSysKey"));
    }

    @Test
    public void deprecatedConstructorFiveArgs_allowsCustomPubSysKey() {
        DocumentType dt = new DocumentType("html", "SYSTEM", "", "http://example.com/sys.dtd", "base");
        // name
        assertEquals("html", dt.attr("name"));
        // custom pubSysKey should be present
        assertEquals("SYSTEM", dt.attr("pubSysKey"));
        // publicId is empty → attribute exists but is blank
        assertEquals("", dt.attr("publicId"));
        // systemId
        assertEquals("http://example.com/sys.dtd", dt.attr("systemId"));
    }

    @Test
    public void setPubSysKey_nullDoesNotCreateAttribute() {
        DocumentType dt = new DocumentType("html", "", "");
        dt.setPubSysKey(null);
        // attribute should be absent (blank)
        assertTrue(dt.attr("pubSysKey").isEmpty());
    }

    @Test
    public void setPubSysKey_nonNullSetsAttribute() {
        DocumentType dt = new DocumentType("html", "", "");
        dt.setPubSysKey("PUBLIC");
        assertEquals("PUBLIC", dt.attr("pubSysKey"));
    }

    @Test
    public void nodeName_returnsDoctypeLiteral() {
        DocumentType dt = new DocumentType("html", "", "");
        assertEquals("#doctype", dt.nodeName());
    }

    /* -------------------------------------------------------
     *  3. Edge‑case & exception identification
     * -------------------------------------------------------
     *
     * - Constructors must reject null arguments (Validate.notNull throws
     *   IllegalArgumentException).
     * - When publicId is blank, the PUB_SYS_KEY attribute must NOT be set.
     * - outerHtmlHead behaviour varies with:
     *   * OutputSettings.Syntax (HTML vs XML)
     *   * presence/absence of name, pubSysKey, publicId, systemId
     * - outerHtmlHead propagates IOException from the Appendable.
     */

    @Test
    public void constructorRejectsNullName() {
        thrown.expect(IllegalArgumentException.class);
        new DocumentType(null, "", "");
    }

    @Test
    public void constructorRejectsNullPublicId() {
        thrown.expect(IllegalArgumentException.class);
        new DocumentType("html", null, "");
    }

    @Test
    public void constructorRejectsNullSystemId() {
        thrown.expect(IllegalArgumentException.class);
        new DocumentType("html", "", null);
    }

    @Test
    public void blankPublicIdDoesNotCreatePubSysKey() {
        DocumentType dt = new DocumentType("html", "   ", "sys");
        // publicId is blank → has(PUBLIC_ID) returns false → no PUB_SYS_KEY
        assertTrue(dt.attr("pubSysKey").isEmpty());
    }

    @Test
    public void outerHtmlHead_htmlSyntax_noIds_generatesHtml5DoctypeLowercase() throws IOException {
        DocumentType dt = new DocumentType("html", "", "");
        OutputSettings out = new OutputSettings().syntax(Syntax.html);
        StringBuilder accum = new StringBuilder();

        // Use reflection to access the package‑private method
        java.lang.reflect.Method m = DocumentType.class.getDeclaredMethod(
                "outerHtmlHead", Appendable.class, int.class, OutputSettings.class);
        m.setAccessible(true);
        m.invoke(dt, accum, 0, out);

        assertEquals("<!doctype>", accum.toString());
    }

    @Test
    public void outerHtmlHead_xmlSyntax_includesUppercaseDoctypeAndAllAttributes() throws Exception {
        DocumentType dt = new DocumentType("html", "-//W3C//DTD HTML 4.01//EN",
                "http://www.w3.org/TR/html4/strict.dtd");
        OutputSettings out = new OutputSettings().syntax(Syntax.xml);
        StringBuilder accum = new StringBuilder();

        java.lang.reflect.Method m = DocumentType.class.getDeclaredMethod(
                "outerHtmlHead", Appendable.class, int.class, OutputSettings.class);
        m.setAccessible(true);
        m.invoke(dt, accum, 0, out);

        String expected = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">";
        assertEquals(expected, accum.toString());
    }

    @Test
    public void outerHtmlHead_withOnlyNameAndSystemId() throws Exception {
        DocumentType dt = new DocumentType("html", "", "system.dtd");
        OutputSettings out = new OutputSettings().syntax(Syntax.xml);
        StringBuilder accum = new StringBuilder();

        java.lang.reflect.Method m = DocumentType.class.getDeclaredMethod(
                "outerHtmlHead", Appendable.class, int.class, OutputSettings.class);
        m.setAccessible(true);
        m.invoke(dt, accum, 0, out);

        String expected = "<!DOCTYPE html \"system.dtd\">";
        assertEquals(expected, accum.toString());
    }

    @Test
    public void outerHtmlHead_throwsIOExceptionWhenAppendableFails() throws Exception {
        DocumentType dt = new DocumentType("html", "", "");
        OutputSettings out = new OutputSettings().syntax(Syntax.html);

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

        java.lang.reflect.Method m = DocumentType.class.getDeclaredMethod(
                "outerHtmlHead", Appendable.class, int.class, OutputSettings.class);
        m.setAccessible(true);

        thrown.expect(IOException.class);
        thrown.expectMessage("forced failure");
        m.invoke(dt, failingAppendable, 0, out);
    }

    /* -------------------------------------------------------
     *  4. Additional sanity checks for inherited behaviour
     * ------------------------------------------------------- */

    @Test
    public void outerHtmlTail_isNoOp() throws Exception {
        DocumentType dt = new DocumentType("html", "", "");
        OutputSettings out = new OutputSettings();
        StringBuilder accum = new StringBuilder("prefix");

        java.lang.reflect.Method tail = DocumentType.class.getDeclaredMethod(
                "outerHtmlTail", Appendable.class, int.class, OutputSettings.class);
        tail.setAccessible(true);
        tail.invoke(dt, accum, 0, out);

        // The tail should not modify the accumulator
        assertEquals("prefix", accum.toString());
    }
}
