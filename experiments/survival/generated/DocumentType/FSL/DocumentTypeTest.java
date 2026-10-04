package org.jsoup.nodes;

import static org.junit.Assert.*;

import org.junit.Test;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Document.OutputSettings.Syntax;

/**
 * Comprehensive JUnit4 tests for {@link DocumentType}.
 */
public class DocumentTypeTest {

    /* ---------- Constructor validation ---------- */

    @Test(expected = IllegalArgumentException.class)
    public void ctorShouldRejectNullName() {
        new DocumentType(null, "", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ctorShouldRejectNullPublicId() {
        new DocumentType("html", null, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ctorShouldRejectNullSystemId() {
        new DocumentType("html", "", null);
    }

    /* ---------- nodeName ---------- */

    @Test
    public void nodeNameIsDoctype() {
        DocumentType dt = new DocumentType("html", "", "");
        assertEquals("#doctype", dt.nodeName());
    }

    /* ---------- setPubSysKey ---------- */

    @Test
    public void setPubSysKeyAddsAttributeWhenNotNull() {
        DocumentType dt = new DocumentType("html", "", "");
        dt.setPubSysKey(DocumentType.SYSTEM_KEY);
        // The attribute is stored internally; we verify via outerHtmlHead.
        OutputSettings out = new OutputSettings().syntax(Syntax.xml);
        StringBuilder accum = new StringBuilder();
        try {
            dt.outerHtmlHead(accum, 0, out);
        } catch (Exception e) {
            fail("outerHtmlHead threw: " + e);
        }
        assertTrue(accum.toString().contains(" SYSTEM "));
    }

    @Test
    public void setPubSysKeyIgnoresNull() {
        DocumentType dt = new DocumentType("html", "", "");
        dt.setPubSysKey(null);
        OutputSettings out = new OutputSettings().syntax(Syntax.xml);
        StringBuilder accum = new StringBuilder();
        try {
            dt.outerHtmlHead(accum, 0, out);
        } catch (Exception e) {
            fail("outerHtmlHead threw: " + e);
        }
        // No PUB_SYS_KEY should be present
        assertFalse(accum.toString().contains("PUBLIC"));
        assertFalse(accum.toString().contains("SYSTEM"));
    }

    /* ---------- outerHtmlHead – HTML5 doctype (no public/system) ---------- */

    @Test
    public void html5DoctypeLowercaseWhenNoPublicOrSystem() {
        DocumentType dt = new DocumentType("html", "", "");
        OutputSettings out = new OutputSettings().syntax(Syntax.html);
        StringBuilder accum = new StringBuilder();
        try {
            dt.outerHtmlHead(accum, 0, out);
        } catch (Exception e) {
            fail("outerHtmlHead threw: " + e);
        }
        assertEquals("<!doctype html>", accum.toString());
    }

    @Test
    public void html5DoctypeWithoutNameProducesOnlyDoctype() {
        DocumentType dt = new DocumentType("", "", "");
        OutputSettings out = new OutputSettings().syntax(Syntax.html);
        StringBuilder accum = new StringBuilder();
        try {
            dt.outerHtmlHead(accum, 0, out);
        } catch (Exception e) {
            fail("outerHtmlHead threw: " + e);
        }
        assertEquals("<!doctype>", accum.toString());
    }

    /* ---------- outerHtmlHead – HTML syntax with PUBLIC identifier ---------- */

    @Test
    public void htmlDoctypeWithPublicIdAddsPublicKeyAndQuotes() {
        String publicId = "-//W3C//DTD HTML 4.01//EN";
        DocumentType dt = new DocumentType("html", publicId, "");
        OutputSettings out = new OutputSettings().syntax(Syntax.html);
        StringBuilder accum = new StringBuilder();
        try {
            dt.outerHtmlHead(accum, 0, out);
        } catch (Exception e) {
            fail("outerHtmlHead threw: " + e);
        }
        // Expected: <!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01//EN">
        assertEquals("<!DOCTYPE html PUBLIC \"" + publicId + "\">", accum.toString());
    }

    /* ---------- outerHtmlHead – XML syntax with SYSTEM identifier ---------- */

    @Test
    public void xmlDoctypeWithSystemIdAddsSystemKeyAndQuotes() {
        String systemId = "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd";
        DocumentType dt = new DocumentType("html", "", systemId);
        // Manually set the system key via the deprecated constructor that accepts pubSysKey
        dt.setPubSysKey(DocumentType.SYSTEM_KEY);
        OutputSettings out = new OutputSettings().syntax(Syntax.xml);
        StringBuilder accum = new StringBuilder();
        try {
            dt.outerHtmlHead(accum, 0, out);
        } catch (Exception e) {
            fail("outerHtmlHead threw: " + e);
        }
        // Expected: <!DOCTYPE html SYSTEM "http://...">
        assertEquals("<!DOCTYPE html SYSTEM \"" + systemId + "\">", accum.toString());
    }

    /* ---------- outerHtmlHead – Both PUBLIC and SYSTEM identifiers ---------- */

    @Test
    public void doctypeWithBothPublicAndSystemIds() {
        String publicId = "-//W3C//DTD XHTML 1.0 Transitional//EN";
        String systemId = "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd";
        DocumentType dt = new DocumentType("html", publicId, systemId);
        OutputSettings out = new OutputSettings().syntax(Syntax.html);
        StringBuilder accum = new StringBuilder();
        try {
            dt.outerHtmlHead(accum, 0, out);
        } catch (Exception e) {
            fail("outerHtmlHead threw: " + e);
        }
        // Expected: <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://...">
        assertEquals("<!DOCTYPE html PUBLIC \"" + publicId + "\" \"" + systemId + "\">", accum.toString());
    }

    /* ---------- deprecated constructors ---------- */

    @Test
    public void deprecatedCtorThreeArgsSetsPublicKeyWhenPublicIdNonBlank() {
        DocumentType dt = new DocumentType("html", "-//W3C//DTD HTML 4.01//EN", "", "unused");
        OutputSettings out = new OutputSettings().syntax(Syntax.html);
        StringBuilder accum = new StringBuilder();
        try {
            dt.outerHtmlHead(accum, 0, out);
        } catch (Exception e) {
            fail("outerHtmlHead threw: " + e);
        }
        assertTrue(accum.toString().contains("PUBLIC"));
    }

    @Test
    public void deprecatedCtorWithPubSysKeyKeepsProvidedKey() {
        DocumentType dt = new DocumentType("html", DocumentType.SYSTEM_KEY,
                "", "http://example.com", "unused");
        OutputSettings out = new OutputSettings().syntax(Syntax.xml);
        StringBuilder accum = new StringBuilder();
        try {
            dt.outerHtmlHead(accum, 0, out);
        } catch (Exception e) {
            fail("outerHtmlHead threw: " + e);
        }
        assertTrue(accum.toString().contains("SYSTEM"));
        assertTrue(accum.toString().contains("http://example.com"));
    }

    /* ---------- handling of blank (whitespace) publicId ---------- */

    @Test
    public void whitespacePublicIdIsTreatedAsBlankAndDoesNotSetPubSysKey() {
        DocumentType dt = new DocumentType("html", "   ", "", "");
        OutputSettings out = new OutputSettings().syntax(Syntax.html);
        StringBuilder accum = new StringBuilder();
        try {
            dt.outerHtmlHead(accum, 0, out);
        } catch (Exception e) {
            fail("outerHtmlHead threw: " + e);
        }
        // No PUBLIC keyword should appear because the publicId is blank (whitespace)
        assertFalse(accum.toString().contains("PUBLIC"));
    }

    /* ---------- outerHtmlTail does nothing (should not modify accumulator) ---------- */

    @Test
    public void outerHtmlTailLeavesAccumulatorUnchanged() throws Exception {
        DocumentType dt = new DocumentType("html", "", "");
        StringBuilder accum = new StringBuilder("prefix");
        dt.outerHtmlTail(accum, 0, new OutputSettings().syntax(Syntax.html));
        assertEquals("prefix", accum.toString());
    }
}
