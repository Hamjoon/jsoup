package org.jsoup.nodes;

import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Document.OutputSettings.Syntax;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit4 tests for {@link DocumentType}.
 */
public class DocumentTypeTest {

    private DocumentType dtFull;      // full doctype with name, publicId, systemId
    private DocumentType dtMinimal;   // doctype with empty publicId and systemId (HTML5 style)

    @Before
    public void setUp() {
        dtFull = new DocumentType("html", "PUBLIC_ID", "SYSTEM_ID");
        dtMinimal = new DocumentType("html", "", "");
    }

    /*** Constructor validation ***/
    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullName_throws() {
        new DocumentType(null, "pub", "sys");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullPublicId_throws() {
        new DocumentType("html", null, "sys");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullSystemId_throws() {
        new DocumentType("html", "pub", null);
    }

    /*** Attribute handling in the primary constructor ***/
    @Test
    public void primaryConstructor_setsAttributesCorrectly() {
        assertEquals("html", dtFull.attr("name"));
        assertEquals("PUBLIC_ID", dtFull.attr("publicId"));
        assertEquals("SYSTEM_ID", dtFull.attr("systemId"));
        // Because publicId is non‑blank, PUB_SYS_KEY should be set to PUBLIC
        assertEquals(DocumentType.PUBLIC_KEY, dtFull.attr("pubSysKey"));
    }

    @Test
    public void primaryConstructor_blankPublicIdDoesNotSetPubSysKey() {
        DocumentType dt = new DocumentType("html", "", "sys");
        // Blank publicId → no PUB_SYS_KEY attribute
        assertTrue(dt.attr("pubSysKey").isEmpty());
    }

    /*** Deprecated constructors ***/
    @Test
    public void deprecatedConstructor_threeArgs_setsAttributes() {
        DocumentType dt = new DocumentType("html", "pub", "sys", "base");
        assertEquals("html", dt.attr("name"));
        assertEquals("pub", dt.attr("publicId"));
        assertEquals("sys", dt.attr("systemId"));
        // PUB_SYS_KEY should be set because publicId is non‑blank
        assertEquals(DocumentType.PUBLIC_KEY, dt.attr("pubSysKey"));
    }

    @Test
    public void deprecatedConstructor_fiveArgs_respectsPubSysKey() {
        DocumentType dt = new DocumentType("html", DocumentType.SYSTEM_KEY, "pub", "sys", "base");
        assertEquals(DocumentType.SYSTEM_KEY, dt.attr("pubSysKey"));
        assertEquals("pub", dt.attr("publicId"));
        assertEquals("sys", dt.attr("systemId"));
    }

    @Test
    public void deprecatedConstructor_fiveArgs_nullPubSysKey_omitsAttribute() {
        DocumentType dt = new DocumentType("html", null, "pub", "sys", "base");
        assertTrue(dt.attr("pubSysKey").isEmpty());
    }

    /*** setPubSysKey ***/
    @Test
    public void setPubSysKey_nonNull_updatesAttribute() {
        DocumentType dt = new DocumentType("html", "", "");
        dt.setPubSysKey(DocumentType.SYSTEM_KEY);
        assertEquals(DocumentType.SYSTEM_KEY, dt.attr("pubSysKey"));
    }

    @Test
    public void setPubSysKey_nullDoesNothing() {
        DocumentType dt = new DocumentType("html", "", "");
        dt.setPubSysKey(null);
        assertTrue(dt.attr("pubSysKey").isEmpty());
    }

    /*** nodeName ***/
    @Test
    public void nodeName_isDoctypeConstant() {
        assertEquals("#doctype", dtFull.nodeName());
    }

    /*** outerHtmlHead – HTML5 (no publicId/systemId) ***/
    @Test
    public void outerHtmlHead_htmlSyntax_html5DoctypeLowercase() throws IOException {
        OutputSettings settings = new OutputSettings().syntax(Syntax.html);
        StringBuilder accum = new StringBuilder();
        dtMinimal.outerHtmlHead(accum, 0, settings);
        // Expected: <!doctype> (lower‑case, no name because name is present but has() on name is true)
        // Since name is "html" (non‑blank), it will be included: "<!doctype html>"
        assertEquals("<!doctype html>", accum.toString());
    }

    /*** outerHtmlHead – HTML5 without name (blank name) ***/
    @Test
    public void outerHtmlHead_htmlSyntax_html5DoctypeNoName() throws IOException {
        DocumentType dt = new DocumentType("", "", "");
        OutputSettings settings = new OutputSettings().syntax(Syntax.html);
        StringBuilder accum = new StringBuilder();
        dt.outerHtmlHead(accum, 0, settings);
        // No name, no public/system IDs → just the lowercase doctype token
        assertEquals("<!doctype>", accum.toString());
    }

    /*** outerHtmlHead – HTML syntax with public/system IDs ***/
    @Test
    public void outerHtmlHead_htmlSyntax_fullDoctypeUppercase() throws IOException {
        OutputSettings settings = new OutputSettings().syntax(Syntax.html);
        StringBuilder accum = new StringBuilder();
        dtFull.outerHtmlHead(accum, 0, settings);
        // Expected: <!DOCTYPE html PUBLIC "PUBLIC_ID" "SYSTEM_ID">
        assertEquals("<!DOCTYPE html PUBLIC \"PUBLIC_ID\" \"SYSTEM_ID\">", accum.toString());
    }

    /*** outerHtmlHead – XML syntax (always uppercase) ***/
    @Test
    public void outerHtmlHead_xmlSyntax_fullDoctypeUppercase() throws IOException {
        OutputSettings settings = new OutputSettings().syntax(Syntax.xml);
        StringBuilder accum = new StringBuilder();
        dtFull.outerHtmlHead(accum, 0, settings);
        assertEquals("<!DOCTYPE html PUBLIC \"PUBLIC_ID\" \"SYSTEM_ID\">", accum.toString());
    }

    /*** outerHtmlHead – mixed presence (publicId blank, systemId set) ***/
    @Test
    public void outerHtmlHead_mixedPresence_systemIdOnly() throws IOException {
        DocumentType dt = new DocumentType("html", "", "sysOnly");
        OutputSettings settings = new OutputSettings().syntax(Syntax.html);
        StringBuilder accum = new StringBuilder();
        dt.outerHtmlHead(accum, 0, settings);
        // Since publicId is blank but systemId present, uppercase DOCTYPE is used
        assertEquals("<!DOCTYPE html \"sysOnly\">", accum.toString());
    }

    /*** outerHtmlTail – should not modify accumulator ***/
    @Test
    public void outerHtmlTail_doesNotChangeAccumulator() {
        DocumentType dt = new DocumentType("html", "pub", "sys");
        OutputSettings settings = new OutputSettings().syntax(Syntax.html);
        StringBuilder accum = new StringBuilder("START");
        dt.outerHtmlTail(accum, 0, settings);
        assertEquals("START", accum.toString());
    }

    /*** has() logic exercised via outerHtmlHead ***/
    @Test
    public void hasMethod_considersBlankStringsAsMissing() throws IOException {
        // publicId is blank, systemId is blank → treated as missing
        DocumentType dt = new DocumentType("html", "   ", "\t");
        OutputSettings settings = new OutputSettings().syntax(Syntax.html);
        StringBuilder accum = new StringBuilder();
        dt.outerHtmlHead(accum, 0, settings);
        // Because both are blank, we get lowercase doctype with name
        assertEquals("<!doctype html>", accum.toString());
    }
}
