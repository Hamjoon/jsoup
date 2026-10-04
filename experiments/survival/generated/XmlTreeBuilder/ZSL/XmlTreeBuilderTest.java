package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Token.*;
import org.junit.Test;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link XmlTreeBuilder}.
 *
 * <p>The tests are written in the same package ({@code org.jsoup.parser}) so that they can
 * access package‑private members of {@code XmlTreeBuilder}. Private methods are exercised
 * via reflection.</p>
 */
public class XmlTreeBuilderTest {

    /**
     * Verify that the default parse settings preserve the case of tag and attribute names.
     */
    @Test
    public void testDefaultSettingsPreserveCase() {
        XmlTreeBuilder builder = new XmlTreeBuilder();
        ParseSettings settings = builder.defaultSettings();
        assertTrue("Default settings should preserve case", settings.equals(ParseSettings.preserveCase));
    }

    /**
     * Parse a simple XML document containing a root element, a child element with an attribute,
     * a text node, and a comment. Verify that the resulting DOM matches the source.
     */
    @Test
    public void testParseSimpleXmlStructure() {
        String xml = "<Root><Child Attr=\"Value\">Hello World</Child><!-- A comment --></Root>";
        XmlTreeBuilder builder = new XmlTreeBuilder();
        Document doc = builder.parse(xml, "http://example.com");

        // The document should contain a single root element named "Root"
        Element root = doc.child(0);
        assertEquals("Root", root.nodeName());

        // Child element checks
        Element child = root.selectFirst("Child");
        assertNotNull("Child element should be present", child);
        assertEquals("Value", child.attr("Attr"));
        assertEquals("Hello World", child.text());

        // Comment node check – it should be the third child of the root (index 2)
        List<Node> rootChildren = root.childNodes();
        assertTrue("Root should contain at least three child nodes", rootChildren.size() >= 3);
        Node commentNode = rootChildren.get(2);
        assertTrue("Third node should be a Comment", commentNode instanceof Comment);
        assertEquals(" A comment ", ((Comment) commentNode).getData());
    }

    /**
     * Test {@link XmlTreeBuilder#parseFragment(String, String, ParseErrorList, ParseSettings)}.
     * The fragment contains a self‑closing tag and a normal element with text.
     */
    @Test
    public void testParseFragmentCreatesExpectedNodes() {
        String fragment = "<item id='1'/><container>Text</container>";
        XmlTreeBuilder builder = new XmlTreeBuilder();
        List<Node> nodes = builder.parseFragment(
                fragment,
                "http://example.com",
                ParseErrorList.noTracking(),
                ParseSettings.preserveCase);

        // Expect two top‑level nodes: the self‑closing <item> and the <container>
        assertEquals(2, nodes.size());

        // First node: self‑closing <item>
        Node first = nodes.get(0);
        assertTrue(first instanceof Element);
        Element item = (Element) first;
        assertEquals("item", item.nodeName());
        assertEquals("1", item.attr("id"));
        assertTrue("Self‑closing element should have no children", item.childNodeSize() == 0);

        // Second node: <container> with text
        Node second = nodes.get(1);
        assertTrue(second instanceof Element);
        Element container = (Element) second;
        assertEquals("container", container.nodeName());
        assertEquals("Text", container.text());
    }

    /**
     * Verify that an XML declaration (e.g. {@code <?xml …?>}) is parsed into an {@link XmlDeclaration}
     * node. This path exercises the “bogus comment” handling inside {@code insert(Token.Comment)}.
     */
    @Test
    public void testXmlDeclarationParsing() {
        String xml = "<?xml version='1.0' encoding='UTF-8'?><root/>";
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());

        // The first child of the document should be an XmlDeclaration
        Node first = doc.childNode(0);
        assertTrue("First node should be an XmlDeclaration", first instanceof XmlDeclaration);
        XmlDeclaration decl = (XmlDeclaration) first;
        assertEquals("xml", decl.name());                     // tag name is normalized to lower‑case
        assertEquals("1.0", decl.attr("version"));
        assertEquals("UTF-8", decl.attr("encoding"));
        assertTrue("XmlDeclaration should be marked as a declaration, not a comment", decl.isXmlDeclaration());
    }

    /**
     * Ensure that a {@code <!DOCTYPE …>} token is turned into a {@link DocumentType} node.
     */
    @Test
    public void testDoctypeInsertion() {
        String xml = "<!DOCTYPE note SYSTEM \"note.dtd\"><note>Content</note>";
        Document doc = Jsoup.parse(xml, "", Parser.xmlParser());

        // The first node should be the DocumentType
        Node first = doc.childNode(0);
        assertTrue(first instanceof DocumentType);
        DocumentType doctype = (DocumentType) first;
        assertEquals("note", doctype.name());
        assertEquals("", doctype.publicId());
        assertEquals("note.dtd", doctype.systemId());
    }

    /**
     * Verify that {@code popStackToClose} correctly removes elements from the internal stack.
     * The method is private, so we invoke it via reflection.
     */
    @Test
    public void testPopStackToCloseRemovesElements() throws Exception {
        XmlTreeBuilder builder = new XmlTreeBuilder();

        // Initialise the parser so that the internal stack is ready (document + empty stack)
        builder.initialiseParse(
                new StringReader(""),               // empty input – we are not parsing here
                "http://example.com",
                ParseErrorList.noTracking(),
                ParseSettings.preserveCase);

        // Insert two start‑tags: <parent> and <child>
        Token.StartTag parentTag = new Token.StartTag();
        parentTag.name("parent");
        parentTag.selfClosing(false);
        builder.insert(parentTag);                     // pushes <parent> onto the stack

        Token.StartTag childTag = new Token.StartTag();
        childTag.name("child");
        childTag.selfClosing(false);
        builder.insert(childTag);                      // pushes <child> onto the stack

        // Verify the stack now contains: [Document, <parent>, <child>]
        Field stackField = TreeBuilder.class.getDeclaredField("stack");
        stackField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Element> stack = (List<Element>) stackField.get(builder);
        assertEquals(3, stack.size());
        assertEquals("child", stack.get(stack.size() - 1).nodeName());

        // Use reflection to call the private popStackToClose method with an EndTag for "parent"
        Method popMethod = XmlTreeBuilder.class.getDeclaredMethod("popStackToClose", Token.EndTag.class);
        popMethod.setAccessible(true);
        Token.EndTag endParent = new Token.EndTag();
        endParent.name("parent");
        popMethod.invoke(builder, endParent);

        // After popping, the stack should contain only the Document (the <child> and <parent> were removed)
        @SuppressWarnings("unchecked")
        List<Element> afterStack = (List<Element>) stackField.get(builder);
        assertEquals(1, afterStack.size());
        assertEquals("#document", afterStack.get(0).nodeName());
    }

    /**
     * Directly test insertion of a character token (text node) using the package‑private
     * {@code insert(Token.Character)} method.
     */
    @Test
    public void testCharacterInsertion() {
        XmlTreeBuilder builder = new XmlTreeBuilder();
        builder.initialiseParse(
                new StringReader("<root/>"),
                "http://example.com",
                ParseErrorList.noTracking(),
                ParseSettings.preserveCase);
        // Manually insert a character token into the current element (<root>)
        Token.Character charToken = new Token.Character();
        charToken.data("sample text");
        builder.insert(charToken);

        // The document now contains <root>sample text</root>
        Element root = builder.doc.selectFirst("root");
        assertNotNull(root);
        assertEquals("sample text", root.text());
    }
}
