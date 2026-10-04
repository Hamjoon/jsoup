###Test START##
package org.jsoup.nodes;

import org.jsoup.parser.Tag;
import org.jsoup.parser.Parser;
import org.jsoup.nodes.Document.OutputSettings;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.*;

public class PseudoTextElementTest {

    private Tag tag;
    private String baseUri;
    private Attributes attributes;

    @Before
    public void setUp() {
        tag = Tag.valueOf("p");               // a simple, valid tag
        baseUri = "http://example.com";
        attributes = new Attributes();
        attributes.put("class", "myClass");
    }

    /*** ----------------------------------------------------------------------
     *  Constructor tests
     * ---------------------------------------------------------------------- ***/

    @Test
    public void constructor_ValidArguments_ShouldCreateElement() {
        PseudoTextElement el = new PseudoTextElement(tag, baseUri, attributes);
        assertEquals("p", el.tagName());
        assertEquals(baseUri, el.baseUri());
        assertEquals("myClass", el.attr("class"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_NullTag_ShouldThrowIllegalArgumentException() {
        new PseudoTextElement(null, baseUri, attributes);
    }

    @Test
    public void constructor_NullAttributes_ShouldCreateElementWithEmptyAttributes() {
        PseudoTextElement el = new PseudoTextElement(tag, baseUri, null);
        // No NPE – attributes are initialised to an empty set by the super‑class
        assertTrue(el.attributes().size() == 0);
    }

    @Test
    public void constructor_NullBaseUri_ShouldAcceptNullBaseUri() {
        PseudoTextElement el = new PseudoTextElement(tag, null, attributes);
        // The super‑class treats a null baseUri as an empty string
        assertEquals("", el.baseUri());
        assertEquals("myClass", el.attr("class"));
    }

    /*** ----------------------------------------------------------------------
     *  outerHtmlHead / outerHtmlTail – normal behaviour (no output)
     * ---------------------------------------------------------------------- ***/

    @Test
    public void outerHtmlHead_ShouldNotModifyAppendable() throws IOException {
        PseudoTextElement el = new PseudoTextElement(tag, baseUri, attributes);
        StringWriter accum = new StringWriter();
        OutputSettings settings = new Document("").outputSettings();

        el.outerHtmlHead(accum, 0, settings);
        assertEquals("", accum.toString());
    }

    @Test
    public void outerHtmlTail_ShouldNotModifyAppendable() throws IOException {
        PseudoTextElement el = new PseudoTextElement(tag, baseUri, attributes);
        StringWriter accum = new StringWriter();
        OutputSettings settings = new Document("").outputSettings();

        el.outerHtmlTail(accum, 0, settings);
        assertEquals("", accum.toString());
    }

    /*** ----------------------------------------------------------------------
     *  outerHtmlHead / outerHtmlTail – exception propagation
     * ---------------------------------------------------------------------- ***/

    private static class ExplodingAppendable implements Appendable {
        @Override
        public Appendable append(CharSequence csq) throws IOException {
            throw new IOException("boom");
        }

        @Override
        public Appendable append(CharSequence csq, int start, int end) throws IOException {
            throw new IOException("boom");
        }

        @Override
        public Appendable append(char c) throws IOException {
            throw new IOException("boom");
        }
    }

    @Test(expected = IOException.class)
    public void outerHtmlHead_WhenAppendableThrowsIOException_ShouldPropagate() throws IOException {
        PseudoTextElement el = new PseudoTextElement(tag, baseUri, attributes);
        Appendable exploding = new ExplodingAppendable();
        OutputSettings settings = new Document("").outputSettings();

        el.outerHtmlHead(exploding, 0, settings);
    }

    @Test(expected = IOException.class)
    public void outerHtmlTail_WhenAppendableThrowsIOException_ShouldPropagate() throws IOException {
        PseudoTextElement el = new PseudoTextElement(tag, baseUri, attributes);
        Appendable exploding = new ExplodingAppendable();
        OutputSettings settings = new Document("").outputSettings();

        el.outerHtmlTail(exploding, 0, settings);
    }

    /*** ----------------------------------------------------------------------
     *  Integration sanity check – the element behaves like a normal Element
     * ---------------------------------------------------------------------- ***/

    @Test
    public void pseudoTextElement_ShouldBeSelectableByTagName() {
        // Build a tiny document containing the pseudo‑text element
        Document doc = new Document("");
        PseudoTextElement pseudo = new PseudoTextElement(tag, baseUri, attributes);
        doc.appendChild(pseudo);

        // The selector should be able to find the element by its tag name
        assertEquals(1, doc.select("p").size());
        assertEquals(pseudo, doc.selectFirst("p"));
    }
}
###Test END##