package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.*;
import org.jsoup.parser.Token.*;
import org.junit.Test;
import org.junit.Before;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.*;

/**
 * JUnit 4 tests that exercise every method in {@link XmlTreeBuilder}.
 * The tests are placed in the same package as the class under test so that
 * package‑private members can be accessed directly.  Private methods are
 * invoked via reflection.
 */
public class XmlTreeBuilderTest {

    /**
     * Sub‑class that exposes the protected fields of {@link XmlTreeBuilder}
     * for verification in the tests.
     */
    static class TestableXmlTreeBuilder extends XmlTreeBuilder {
        Document getDocument() { return doc; }
        List<Element> getStack() { return stack; }
        String getBaseUri() { return baseUri; }
        ParseSettings getParseSettings() { return settings; }
    }

    private TestableXmlTreeBuilder builder;

    @Before
    public void setUp() {
        builder = new TestableXmlTreeBuilder();
    }

    /** Helper – set the {@code type} field of any {@link Token}. */
    private void setTokenType(Token token, Token.Type type) {
        try {
            Field f = Token.class.getDeclaredField("type");
            f.setAccessible(true);
            f.set(token, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Helper – create a {@link Token.StartTag}. */
    private Token.StartTag startTag(String name, boolean selfClosing) {
        Token.StartTag t = new Token.StartTag();
        setTokenType(t, Token.Type.StartTag);
        try {
            Field nameF = Token.StartTag.class.getDeclaredField("name");
            nameF.setAccessible(true);
            nameF.set(t, name);

            Field selfClosingF = Token.StartTag.class.getDeclaredField("selfClosing");
            selfClosingF.setAccessible(true);
            selfClosingF.setBoolean(t, selfClosing);

            // initialise empty attributes (the parser will fill them later)
            Field attrF = Token.StartTag.class.getDeclaredField("attributes");
            attrF.setAccessible(true);
            attrF.set(t, new Attributes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return t;
    }

    /** Helper – create a {@link Token.EndTag}. */
    private Token.EndTag endTag(String name) {
        Token.EndTag t = new Token.EndTag();
        setTokenType(t, Token.Type.EndTag);
        try {
            Field nameF = Token.EndTag.class.getDeclaredField("name");
            nameF.setAccessible(true);
            nameF.set(t, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return t;
    }

    /** Helper – create a {@link Token.Comment}. */
    private Token.Comment comment(String data, boolean bogus) {
        Token.Comment t = new Token.Comment();
        setTokenType(t, Token.Type.Comment);
        try {
            Field dataF = Token.Comment.class.getDeclaredField("data");
            dataF.setAccessible(true);
            dataF.set(t, data);

            Field bogusF = Token.Comment.class.getDeclaredField("bogus");
            bogusF.setAccessible(true);
            bogusF.setBoolean(t, bogus);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return t;
    }

    /** Helper – create a {@link Token.Character}. */
    private Token.Character character(String data) {
        Token.Character t = new Token.Character();
        setTokenType(t, Token.Type.Character);
        try {
            Field dataF = Token.Character.class.getDeclaredField("data");
            dataF.setAccessible(true);
            dataF.set(t, data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return t;
    }

    /** Helper – create a {@link Token.Doctype}. */
    private Token.Doctype doctype(String name, String pubId, String sysId, String pubSysKey) {
        Token.Doctype t = new Token.Doctype();
        setTokenType(t, Token.Type.Doctype);
        try {
            Field nameF = Token.Doctype.class.getDeclaredField("name");
            nameF.setAccessible(true);
            nameF.set(t, name);

            Field pubF = Token.Doctype.class.getDeclaredField("publicIdentifier");
            pubF.setAccessible(true);
            pubF.set(t, pubId);

            Field sysF = Token.Doctype.class.getDeclaredField("systemIdentifier");
            sysF.setAccessible(true);
            sysF.set(t, sysId);

            Field keyF = Token.Doctype.class.getDeclaredField("pubSysKey");
            keyF.setAccessible(true);
            keyF.set(t, pubSysKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return t;
    }

    /** -----------------------------------------------------------------
     *  1. defaultSettings()
     * ----------------------------------------------------------------- */
    @Test
    public void testDefaultSettingsPreservesCase() {
        assertSame(ParseSettings.preserveCase, builder.defaultSettings());
    }

    /** -----------------------------------------------------------------
     *  2. parse(String, String) – basic XML parsing & output settings
     * ----------------------------------------------------------------- */
    @Test
    public void testParseStringCreatesDocumentAndSetsXmlSyntax() {
        String xml = "<root><child id='c1'>Text</child></root>";
        Document doc = builder.parse(xml, "http://example.com/");

        // root element is present
        Element root = doc.selectFirst("root");
        assertNotNull(root);
        // child with attribute
        Element child = root.selectFirst("child");
        assertNotNull(child);
        assertEquals("c1", child.attr("id"));
        assertEquals("Text", child.text());

        // output settings must be XML
        assertEquals(Document.OutputSettings.Syntax.xml, doc.outputSettings().syntax());
    }

    /** -----------------------------------------------------------------
     *  3. initialiseParse – ensures stack initialisation & XML syntax flag
     * ----------------------------------------------------------------- */
    @Test
    public void testInitialiseParseStackAndSyntax() {
        builder.initialiseParse(new StringReader(""), "http://base/", ParseErrorList.noTracking(),
                ParseSettings.preserveCase);

        // The document itself should be the first element on the stack
        assertFalse(builder.getStack().isEmpty());
        assertEquals(builder.getDocument(), builder.getStack().get(0));

        // XML syntax flag
        assertEquals(Document.OutputSettings.Syntax.xml, builder.getDocument()
                .outputSettings().syntax());
    }

    /** -----------------------------------------------------------------
     *  4. process – StartTag, Character, EndTag flow
     * ----------------------------------------------------------------- */
    @Test
    public void testProcessStartTagCharacterAndEndTag() {
        builder.initialiseParse(new StringReader(""), "http://base/", ParseErrorList.noTracking(),
                ParseSettings.preserveCase);

        // <greeting>
        Token.StartTag start = startTag("greeting", false);
        assertTrue(builder.process(start));

        // text "hello"
        Token.Character chars = character("hello");
        assertTrue(builder.process(chars));

        // </greeting>
        Token.EndTag end = endTag("greeting");
        assertTrue(builder.process(end));

        // Verify the structure
        Element greeting = builder.getDocument().selectFirst("greeting");
        assertNotNull(greeting);
        assertEquals("hello", greeting.text());
    }

    /** -----------------------------------------------------------------
     *  5. insert(Token.Comment) – bogus XML declaration handling
     * ----------------------------------------------------------------- */
    @Test
    public void testInsertBogusCommentCreatesXmlDeclaration() {
        builder.initialiseParse(new StringReader(""), "http://base/", ParseErrorList.noTracking(),
                ParseSettings.preserveCase);

        // Simulate a bogus comment that actually represents an XML declaration
        String decl = "?xml version=\"1.0\" encoding=\"UTF-8\"?";
        Token.Comment bogusComment = comment(decl, true);
        assertTrue(builder.process(bogusComment));

        // The first child of the document should now be an XmlDeclaration
        Node first = builder.getDocument().childNode(0);
        assertTrue(first instanceof XmlDeclaration);
        XmlDeclaration xmlDecl = (XmlDeclaration) first;
        assertTrue(xmlDecl.isProcessingInstruction()); // because it started with '?'
        assertEquals("1.0", xmlDecl.attr("version"));
        assertEquals("UTF-8", xmlDecl.attr("encoding"));
    }

    /** -----------------------------------------------------------------
     *  6. insert(Token.Character) – plain text node insertion
     * ----------------------------------------------------------------- */
    @Test
    public void testInsertCharacterCreatesTextNode() {
        builder.initialiseParse(new StringReader(""), "http://base/", ParseErrorList.noTracking(),
                ParseSettings.preserveCase);

        Token.Character text = character("sample text");
        assertTrue(builder.process(text));

        Node node = builder.getDocument().childNode(0);
        assertTrue(node instanceof TextNode);
        assertEquals("sample text", ((TextNode) node).text());
    }

    /** -----------------------------------------------------------------
     *  7. insert(Token.Doctype) – document type node insertion
     * ----------------------------------------------------------------- */
    @Test
    public void testInsertDoctypeCreatesDocumentTypeNode() {
        builder.initialiseParse(new StringReader(""), "http://base/", ParseErrorList.noTracking(),
                ParseSettings.preserveCase);

        Token.Doctype dt = doctype("html", "-//W3C//DTD XHTML 1.0 Transitional//EN",
                "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd", "PUBLIC");
        assertTrue(builder.process(dt));

        Node node = builder.getDocument().childNode(0);
        assertTrue(node instanceof DocumentType);
        DocumentType docType = (DocumentType) node;
        assertEquals("html", docType.name());
        assertEquals("-//W3C//DTD XHTML 1.0 Transitional//EN", docType.publicId());
        assertEquals("http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd", docType.systemId());
        assertEquals("PUBLIC", docType.pubSysKey());
    }

    /** -----------------------------------------------------------------
     *  8. popStackToClose – normal closing and missing‑tag cases
     * ----------------------------------------------------------------- */
    @Test
    public void testPopStackToCloseRemovesElementsUpToMatchingTag() throws Exception {
        builder.initialiseParse(new StringReader(""), "http://base/", ParseErrorList.noTracking(),
                ParseSettings.preserveCase);

        // Push <a>, <b>, <c> onto the stack via start tags
        builder.process(startTag("a", false));
        builder.process(startTag("b", false));
        builder.process(startTag("c", false));

        // Ensure stack size is 4 (document + a + b + c)
        assertEquals(4, builder.getStack().size());

        // Close </b> – this should pop c then b
        Token.EndTag closeB = endTag("b");
        // popStackToClose is private; invoke via reflection
        java.lang.reflect.Method m = XmlTreeBuilder.class.getDeclaredMethod("popStackToClose", Token.EndTag.class);
        m.setAccessible(true);
        m.invoke(builder, closeB);

        // After popping, stack should contain document and a
        assertEquals(2, builder.getStack().size());
        assertEquals("a", builder.getStack().get(1).nodeName());
    }

    @Test
    public void testPopStackToCloseWhenTagNotPresentDoesNothing() throws Exception {
        builder.initialiseParse(new StringReader(""), "http://base/", ParseErrorList.noTracking(),
                ParseSettings.preserveCase);

        // Push only <root>
        builder.process(startTag("root", false));
        int initialSize = builder.getStack().size();

        // Attempt to close a non‑existent tag
        Token.EndTag missing = endTag("missing");
        java.lang.reflect.Method m = XmlTreeBuilder.class.getDeclaredMethod("popStackToClose", Token.EndTag.class);
        m.setAccessible(true);
        m.invoke(builder, missing);

        // Stack size must be unchanged
        assertEquals(initialSize, builder.getStack().size());
    }

    /** -----------------------------------------------------------------
     *  9. parseFragment – fragment parsing returns child nodes list
     * ----------------------------------------------------------------- */
    @Test
    public void testParseFragmentReturnsCorrectNodeList() {
        String fragment = "<item id='1'>One</item><item id='2'>Two</item>";
        List<Node> nodes = builder.parseFragment(fragment, "http://base/",
                ParseErrorList.noTracking(), ParseSettings.preserveCase);

        assertEquals(2, nodes.size());
        assertTrue(nodes.get(0) instanceof Element);
        assertTrue(nodes.get(1) instanceof Element);

        Element first = (Element) nodes.get(0);
        Element second = (Element) nodes.get(1);

        assertEquals("item", first.tagName());
        assertEquals("1", first.attr("id"));
        assertEquals("One", first.text());

        assertEquals("item", second.tagName());
        assertEquals("2", second.attr("id"));
        assertEquals("Two", second.text());
    }

    /** -----------------------------------------------------------------
     *  10. insert(Token.StartTag) – self‑closing unknown tag handling
     * ----------------------------------------------------------------- */
    @Test
    public void testInsertSelfClosingUnknownTagMarksTagSelfClosing() {
        builder.initialiseParse(new StringReader(""), "http://base/", ParseErrorList.noTracking(),
                ParseSettings.preserveCase);

        // Create a self‑closing tag with a name that is not a known HTML tag (e.g., "mytag")
        Token.StartTag selfClosing = startTag("mytag", true);
        // Ensure the Tag is reported as unknown (HtmlTreeBuilder would treat it as known,
        // but XmlTreeBuilder keeps the original behaviour)
        assertTrue(builder.process(selfClosing));

        // The element should have been added to the document directly (no stack push)
        Element el = builder.getDocument().selectFirst("mytag");
        assertNotNull(el);
        // The underlying Tag should have its selfClosing flag set
        assertTrue(el.tag().isSelfClosing());
    }

    /** -----------------------------------------------------------------
     *  11. insert(Token.Comment) – normal (non‑bogus) comment insertion
     * ----------------------------------------------------------------- */
    @Test
    public void testInsertNormalCommentCreatesCommentNode() {
        builder.initialiseParse(new StringReader(""), "http://base/", ParseErrorList.noTracking(),
                ParseSettings.preserveCase);

        Token.Comment normal = comment("a normal comment", false);
        assertTrue(builder.process(normal));

        Node node = builder.getDocument().childNode(0);
        assertTrue(node instanceof Comment);
        assertEquals("a normal comment", ((Comment) node).getData());
    }

    /** -----------------------------------------------------------------
     *  12. insert(Token.StartTag) – handling of known HTML tags (e.g., "br")
     * ----------------------------------------------------------------- */
    @Test
    public void testInsertKnownTagWithoutSelfClosingKeepsInStack() {
        builder.initialiseParse(new StringReader(""), "http://base/", ParseErrorList.noTracking(),
                ParseSettings.preserveCase);

        // "br" is a known HTML tag; we insert it non‑self‑closing
        Token.StartTag start = startTag("br", false);
        assertTrue(builder.process(start));

        // It should be on the stack (because it's not self‑closing)
        List<Element> stack = builder.getStack();
        assertTrue(stack.stream().anyMatch(e -> e.tagName().equals("br")));
    }
}
