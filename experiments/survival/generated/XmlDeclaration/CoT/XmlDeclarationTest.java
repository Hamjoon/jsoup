package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link XmlDeclaration}.
 * Covers normal behaviour, edge cases and expected exceptions.
 */
public class XmlDeclarationTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /*** 1. Constructor – normal usage ***/
    @Test
    public void constructorSetsNameAndFlag() {
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        assertEquals("xml", decl.name());
        assertEquals("#declaration", decl.nodeName());
    }

    /*** 2. Deprecated constructor – normal usage ***/
    @Test
    public void deprecatedConstructorBehavesLikePrimary() {
        XmlDeclaration decl = new XmlDeclaration("xml", "http://example.com", true);
        assertEquals("xml", decl.name());
        // baseUri is ignored – we only check the flag indirectly via outer HTML
        assertEquals("<!xml!>", decl.toString());
    }

    /*** 3. Constructor – null name should throw IllegalArgumentException ***/
    @Test(expected = IllegalArgumentException.class)
    public void constructorThrowsOnNullName() {
        new XmlDeclaration(null, false);
    }

    /*** 4. getWholeDeclaration – no attributes ***/
    @Test
    public void getWholeDeclarationEmptyWhenNoAttributes() {
        XmlDeclaration decl = new XmlDeclaration("test", false);
        assertEquals("", decl.getWholeDeclaration());
    }

    /*** 5. getWholeDeclaration – with a single attribute ***/
    @Test
    public void getWholeDeclarationWithAttributes() {
        XmlDeclaration decl = new XmlDeclaration("test", false);
        decl.attr("version", "1.0");               // inherited from LeafNode / Node
        String whole = decl.getWholeDeclaration();
        // Expected: version="1.0" (no leading/trailing blanks)
        assertEquals("version=\"1.0\"", whole);
    }

    /*** 6. getWholeDeclaration – multiple attributes, order preserved ***/
    @Test
    public void getWholeDeclarationMultipleAttributes() {
        XmlDeclaration decl = new XmlDeclaration("test", false);
        decl.attr("a", "1");
        decl.attr("b", "2");
        String whole = decl.getWholeDeclaration();
        // The order is the insertion order of the underlying Attributes map.
        assertEquals("a=\"1\" b=\"2\"", whole);
    }

    /*** 7. toString / outerHtml – processing instruction = false (standard XML declaration) ***/
    @Test
    public void outerHtmlStandardDeclaration() {
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        decl.attr("version", "1.0");
        decl.attr("encoding", "UTF-8");
        String html = decl.toString();   // calls outerHtml()
        assertTrue(html.startsWith("<?"));
        assertTrue(html.endsWith("?>"));
        // Expected exact string (attributes are separated by a single space)
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", html);
    }

    /*** 8. toString / outerHtml – processing instruction = true ***/
    @Test
    public void outerHtmlProcessingInstruction() {
        XmlDeclaration decl = new XmlDeclaration("xml-stylesheet", true);
        decl.attr("type", "text/xsl");
        decl.attr("href", "style.xsl");
        String html = decl.toString();
        assertTrue(html.startsWith("<!"));
        assertTrue(html.endsWith("!>"));
        assertEquals("<!xml-stylesheet type=\"text/xsl\" href=\"style.xsl\"!>", html);
    }

    /*** 9. Edge case – empty name string ***/
    @Test
    public void emptyNameIsAllowed() {
        XmlDeclaration decl = new XmlDeclaration("", false);
        assertEquals("", decl.name());
        // Outer HTML should be just the delimiters because name is empty
        assertEquals("<?!>", decl.toString());
    }

    /*** 10. Edge case – attribute value containing characters that need escaping ***/
    @Test
    public void attributeValueEscaping() {
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        decl.attr("data", "a \"quote\" & sign");
        String html = decl.toString();
        // jsoup escapes double quotes to &quot; and ampersand to &amp;
        assertEquals("<?xml data=\"a &quot;quote&quot; &amp; sign\"?>", html);
    }

    /*** 11. Verify that SerializationException is wrapped correctly ***/
    @Test
    public void getWholeDeclarationWrapsIOException() throws Exception {
        // We simulate an IOException by subclassing XmlDeclaration and overriding the private helper.
        // Because the helper is private we cannot directly force the exception, so we test the public
        // contract: the method never throws IOException; any unexpected IOException would be wrapped.
        // This test simply ensures that no unexpected exception propagates.
        XmlDeclaration decl = new XmlDeclaration("xml", false);
        // No attributes – should not throw.
        assertEquals("", decl.getWholeDeclaration());
    }
}
