package org.jsoup.nodes;

import org.jsoup.helper.Validate;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Document.OutputSettings.Syntax;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link DocumentType}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors – normal, deprecated and edge cases.</li>
 *   <li>Attribute handling (name, publicId, systemId, pubSysKey).</li>
 *   <li>Behaviour of {@code setPubSysKey} with null and non‑null values.</li>
 *   <li>Correct {@code nodeName()} value.</li>
 *   <li>HTML generation via {@code outerHtmlHead} for both HTML5 and legacy doctypes.</li>
 *   <li>That {@code outerHtmlTail} does not modify the output.</li>
 *   <li>Internal {@code has()} logic exercised indirectly through the public API.</li>
 * </ul>
 */
public class DocumentTypeTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /** Helper that creates an {@link Appendable} backed by a {@link StringBuilder}. */
    private static Appendable builder() {
        return new StringBuilder();
    }

    /** Helper that returns a fresh {@link OutputSettings} configured for the given syntax. */
    private static OutputSettings settings(Syntax syntax) {
        OutputSettings out = new OutputSettings();
        out.syntax(syntax);
        return out;
    }

    /* -------------------------------------------------
     *  Constructor tests
     * ------------------------------------------------- */

    @Test
    public void constructor_ValidArguments_ShouldSetAttributes() {
        DocumentType dt = new DocumentType("html", "PUBLIC_ID", "SYSTEM_ID");
        assertEquals("html", dt.attr("name"));
        assertEquals("PUBLIC_ID", dt.attr("publicId"));
        assertEquals("SYSTEM_ID", dt.attr("systemId"));
        // Because a publicId is present, the PUB_SYS_KEY should be set to "PUBLIC"
        assertEquals(DocumentType.PUBLIC_KEY, dt.attr("pubSysKey"));
    }

    @Test
    public void constructor_NullName_ShouldThrow() {
        thrown.expect(IllegalArgumentException.class);
        new DocumentType(null, "pub", "sys");
    }

    @Test
    public void constructor_NullPublicId_ShouldThrow() {
        thrown.expect(IllegalArgumentException.class);
        new DocumentType("html", null, "sys");
    }

    @Test
    public void constructor_NullSystemId_ShouldThrow() {
        thrown.expect(IllegalArgumentException.class);
        new DocumentType("html", "pub", null);
    }

    @Test
    public void constructor_BlankPublicId_ShouldNotSetPubSysKey() {
        DocumentType dt = new DocumentType("html", "   ", "sys");
        // blank publicId -> has(PUBLIC_ID) == false, therefore no PUB_SYS_KEY
        assertFalse(dt.has("publicId"));
        assertFalse(dt.has("pubSysKey"));
        // systemId is still present
        assertEquals("sys", dt.attr("systemId"));
    }

    @Test
    public void deprecatedConstructor_ThreeArg_ShouldBehaveLikePrimary() {
        DocumentType dt = new DocumentType("html", "pub", "sys", "ignoredBaseUri");
        assertEquals("html", dt.attr("name"));
        assertEquals("pub", dt.attr("publicId"));
        assertEquals("sys", dt.attr("systemId"));
        assertEquals(DocumentType.PUBLIC_KEY, dt.attr("pubSysKey"));
    }

    @Test
    public void deprecatedConstructor_FourArg_WithCustomPubSysKey() {
        DocumentType dt = new DocumentType("html", "SYSTEM", "pub", "sys", "ignored");
        assertEquals("html", dt.attr("name"));
        assertEquals("pub", dt.attr("publicId"));
        assertEquals("sys", dt.attr("systemId"));
        assertEquals("SYSTEM", dt.attr("pubSysKey"));
    }

    @Test
    public void deprecatedConstructor_FourArg_NullPubSysKey_ShouldNotCreateKey() {
        DocumentType dt = new DocumentType("html", null, "pub", "sys", "ignored");
        assertFalse(dt.has("pubSysKey"));
    }

    /* -------------------------------------------------
     *  setPubSysKey tests
     * ------------------------------------------------- */

    @Test
    public void setPubSysKey_NonNull_ShouldAddAttribute() {
        DocumentType dt = new DocumentType("html", "", "");
        dt.setPubSysKey("CUSTOM");
        assertEquals("CUSTOM", dt.attr("pubSysKey"));
    }

    @Test
    public void setPubSysKey_Null_ShouldLeaveAttributeUnchanged() {
        DocumentType dt = new DocumentType("html", "", "");
        dt.setPubSysKey("FOO");
        dt.setPubSysKey(null); // should be a noop
        assertEquals("FOO", dt.attr("pubSysKey"));
    }

    /* -------------------------------------------------
     *  nodeName test
     * ------------------------------------------------- */

    @Test
    public void nodeName_ShouldReturnDoctypeToken() {
        DocumentType dt = new DocumentType("html", "", "");
        assertEquals("#doctype", dt.nodeName());
    }

    /* -------------------------------------------------
     *  outerHtmlHead tests – HTML5 vs legacy doctypes
     * ------------------------------------------------- */

    @Test
    public void outerHtmlHead_Html5Doctype_ShouldBeLowerCase() throws IOException {
        DocumentType dt = new DocumentType("html", "", "");
        Appendable accum = builder();
        dt.outerHtmlHead(accum, 0, settings(Syntax.html));
        assertEquals("<!doctype>", accum.toString());
    }

    @Test
    public void outerHtmlHead_LegacyDoctype_WithPublicAndSystem_ShouldRenderFull() throws IOException {
        DocumentType dt = new DocumentType("html", "PUBLIC_ID", "SYSTEM_ID");
        Appendable accum = builder();
        dt.outerHtmlHead(accum, 0, settings(Syntax.html));
        String expected = "<!DOCTYPE html PUBLIC \"PUBLIC_ID\" \"SYSTEM_ID\">";
        assertEquals(expected, accum.toString());
    }

    @Test
    public void outerHtmlHead_LegacyDoctype_WithOnlySystemId_ShouldRenderSystemOnly() throws IOException {
        DocumentType dt = new DocumentType("html", "", "SYSTEM_ID");
        Appendable accum = builder();
        dt.outerHtmlHead(accum, 0, settings(Syntax.html));
        // No PUBLIC_KEY because publicId is blank, but systemId is present
        String expected = "<!DOCTYPE html \"SYSTEM_ID\">";
        assertEquals(expected, accum.toString());
    }

    @Test
    public void outerHtmlHead_WithCustomPubSysKey_ShouldUseProvidedKey() throws IOException {
        DocumentType dt = new DocumentType("html", "SYSTEM", "pub", "sys", "ignored");
        Appendable accum = builder();
        dt.outerHtmlHead(accum, 0, settings(Syntax.xml)); // XML syntax forces uppercase DOCTYPE
        String expected = "<!DOCTYPE html SYSTEM \"pub\" \"sys\">";
        assertEquals(expected, accum.toString());
    }

    @Test
    public void outerHtmlHead_XmlSyntax_ShouldAlwaysUseUppercaseDoctype() throws IOException {
        DocumentType dt = new DocumentType("svg", "", "");
        Appendable accum = builder();
        dt.outerHtmlHead(accum, 0, settings(Syntax.xml));
        assertTrue(accum.toString().startsWith("<!DOCTYPE"));
    }

    /* -------------------------------------------------
     *  outerHtmlTail test – should be a no‑op
     * ------------------------------------------------- */

    @Test
    public void outerHtmlTail_ShouldNotModifyAccumulator() {
        DocumentType dt = new DocumentType("html", "", "");
        Appendable accum = builder();
        try {
            dt.outerHtmlTail(accum, 0, settings(Syntax.html));
        } catch (IOException e) {
            fail("outerHtmlTail threw IOException: " + e.getMessage());
        }
        assertEquals("", accum.toString());
    }

    /* -------------------------------------------------
     *  Private has() exercised via public behaviour
     * ------------------------------------------------- */

    @Test
    public void has_EmptyAttribute_ShouldBeFalse() {
        DocumentType dt = new DocumentType("html", "", "");
        // publicId and systemId are empty strings – has() should treat them as blank
        assertFalse(dt.has("publicId"));
        assertFalse(dt.has("systemId"));
    }

    @Test
    public void has_NonBlankAttribute_ShouldBeTrue() {
        DocumentType dt = new DocumentType("html", "pub", "sys");
        assertTrue(dt.has("publicId"));
        assertTrue(dt.has("systemId"));
        assertTrue(dt.has("name"));
    }
}
