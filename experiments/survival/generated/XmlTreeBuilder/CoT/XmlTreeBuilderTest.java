package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.parser.Token;
import org.jsoup.parser.Token.Character;
import org.jsoup.parser.Token.Comment;
import org.jsoup.parser.Token.Doctype;
import org.jsoup.parser.Token.EndTag;
import org.jsoup.parser.Token.EOF;
import org.jsoup.parser.Token.StartTag;
import org.jsoup.nodes.*;
import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.*;

public class XmlTreeBuilderTest {

    /* --------------------------------------------------------------------
       1.  List of public (or package‑visible) methods that we can invoke
       --------------------------------------------------------------------
       Document parse(Reader input, String baseUri)
       Document parse(String input, String baseUri)
       List<Node> parseFragment(String inputFragment,
                                String baseUri,
                                ParseErrorList errors,
                                ParseSettings settings)
       protected void initialiseParse(Reader input,
                                      String baseUri,
                                      ParseErrorList errors,
                                      ParseSettings settings)
       protected boolean process(Token token)
       Element insert(Token.StartTag startTag)
       void insert(Token.Comment commentToken)
       void insert(Token.Character characterToken)
       void insert(Token.Doctype d)
    */

    // --------------------------------------------------------------
    // 2.  Basic test cases exercising the public API
    // --------------------------------------------------------------

    @Test
    public void testMethodSignaturesExist() {
        XmlTreeBuilder builder = new XmlTreeBuilder();

        // parse(Reader,String)
        Document d1 = builder.parse(new StringReader("<root/>"), "http://example.com");
        assertNotNull(d1);

        // parse(String,String)
        Document d2 = builder.parse("<root/>", "http://example.com");
        assertNotNull(d2);

        // parseFragment(...)
        List<Node> frag = builder.parseFragment("<a>text</a>",
                "http://example.com",
                ParseErrorList.noTracking(),
                ParseSettings.preserveCase);
        assertNotNull(frag);
    }

    @Test
    public void testParseEmptyInput() {
        XmlTreeBuilder builder = new XmlTreeBuilder();
        Document doc = builder.parse("", "http://example.com");
        assertEquals(0, doc.childNodeSize());
    }

    @Test
    public void testParseSimpleElement() {
        XmlTreeBuilder builder = new XmlTreeBuilder();
        Document doc = builder.parse("<note><to>Tove</to></note>", "http://example.com");

        Element note = doc.selectFirst("note");
        assertNotNull(note);

        Element to = note.selectFirst("to");
        assertNotNull(to);
        assertEquals("Tove", to.text());
    }

    @Test
    public void testSelfClosingUnknownTag() {
        XmlTreeBuilder builder = new XmlTreeBuilder();
        Document doc = builder.parse("<custom/>", "http://example.com");

        Element custom = doc.selectFirst("custom");
        assertNotNull(custom);
        // unknown/self‑closing tag – should have no children
        assertEquals(0, custom.childNodeSize());
    }

    @Test
    public void testCommentBecomesXmlDeclaration() {
        XmlTreeBuilder builder = new XmlTreeBuilder();

        // An XML declaration is emitted by the parser as a *bogus* comment.
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root></root>";
        Document doc = builder.parse(xml, "http://example.com");

        // First node of the document must be an XmlDeclaration
        Node first = doc.childNode(0);
        assertTrue(first instanceof XmlDeclaration);

        XmlDeclaration decl = (XmlDeclaration) first;
        assertEquals("xml", decl.name());
        assertEquals("1.0", decl.attr("version"));
        assertEquals("UTF-8", decl.attr("encoding"));
    }

    @Test
    public void testDoctypeInsertion() {
        XmlTreeBuilder builder = new XmlTreeBuilder();

        String input = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" " +
                "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><root/>";
        Document doc = builder.parse(input, "http://example.com");

        List<Node> children = doc.childNodes();
        assertFalse(children.isEmpty());
        assertTrue(children.get(0) instanceof DocumentType);

        DocumentType dt = (DocumentType) children.get(0);
        assertEquals("html", dt.name());
        assertEquals("-//W3C//DTD XHTML 1.0 Transitional//EN", dt.publicId());
        assertEquals("http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd", dt.systemId());
    }

    // --------------------------------------------------------------
    // 3.  Edge‑case and exception handling tests
    // --------------------------------------------------------------

    @Test
    public void testProcessAllTokenTypes() {
        XmlTreeBuilder builder = new XmlTreeBuilder();

        // Initialise the parser so that internal structures (stack, document, …) exist.
        builder.initialiseParse(new StringReader(""), "http://example.com",
                ParseErrorList.noTracking(),
                ParseSettings.preserveCase);

        // 1. StartTag
        StartTag start = new StartTag();
        start.name("item");
        start.appendAttribute("id", "1");
        assertTrue(builder.process(start));

        // 2. Character
        Character ch = new Character();
        ch.data("hello");
        assertTrue(builder.process(ch));

        // 3. EndTag
        EndTag end = new EndTag();
        end.name("item");
        assertTrue(builder.process(end));

        // 4. Comment (non‑bogus)
        Comment comment = new Comment();
        comment.data("a comment");
        comment.bogus = false;
        assertTrue(builder.process(comment));

        // 5. Doctype
        Doctype doctype = new Doctype();
        doctype.name("root");
        assertTrue(builder.process(doctype));

        // 6. EOF
        EOF eof = new EOF();
        assertTrue(builder.process(eof));
    }

    @Test(expected = NullPointerException.class)
    public void testParseNullReaderThrowsNPE() {
        XmlTreeBuilder builder = new XmlTreeBuilder();
        // The implementation does not guard against a null Reader,
        // so a NPE is expected when initialiseParse dereferences it.
        builder.parse((Reader) null, "http://example.com");
    }

    @Test
    public void testMismatchedEndTagIsIgnoredGracefully() {
        XmlTreeBuilder builder = new XmlTreeBuilder();

        // Input has an end tag for <a> but the inner <b> is never closed.
        Document doc = builder.parse("<a><b></a>", "http://example.com");

        Element a = doc.selectFirst("a");
        Element b = doc.selectFirst("b");

        assertNotNull("Element <a> should be present", a);
        assertNotNull("Element <b> should still be present (unclosed)", b);
        // No exception should be thrown – the parser simply drops the stray end‑tag.
    }

    // --------------------------------------------------------------
    // 4.  Additional sanity test: parseFragment returns the exact nodes
    // --------------------------------------------------------------

    @Test
    public void testParseFragmentReturnsCorrectNodes() {
        XmlTreeBuilder builder = new XmlTreeBuilder();

        List<Node> nodes = builder.parseFragment("<p>One</p><p>Two</p>",
                "http://example.com",
                ParseErrorList.noTracking(),
                ParseSettings.preserveCase);

        assertEquals(2, nodes.size());
        assertTrue(nodes.get(0) instanceof Element);
        assertTrue(nodes.get(1) instanceof Element);
        assertEquals("p", ((Element) nodes.get(0)).tagName());
        assertEquals("One", ((Element) nodes.get(0)).text());
        assertEquals("Two", ((Element) nodes.get(1)).text());
    }
}
