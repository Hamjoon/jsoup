###Test START##
package org.jsoup.nodes;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jsoup.helper.StringUtil;
import org.jsoup.helper.Validate;
import org.jsoup.parser.Parser;
import org.junit.Test;

/**
 * Comprehensive JUnit4 tests for {@link TextNode}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction and basic getters/setters</li>
 *   <li>nodeName(), text(), getWholeText(), isBlank()</li>
 *   <li>splitText() – normal operation and error conditions</li>
 *   <li>createFromEncoded() static factories</li>
 *   <li>static helpers: normaliseWhitespace, stripLeadingWhitespace, lastCharIsWhitespace</li>
 *   <li>outerHtmlHead() behaviour with different {@link Document.OutputSettings}</li>
 * </ul>
 *
 * The test class is placed in the same package as {@link TextNode} so that package‑private
 * methods (e.g. {@code outerHtmlHead}) can be invoked directly.
 */
public class TextNodeTest {

    // ----------------------------------------------------------------------
    // Basic construction and simple getters / setters
    // ----------------------------------------------------------------------
    @Test
    public void constructorAndBasicAccessors() {
        TextNode tn = new TextNode("Hello world");
        assertEquals("Hello world", tn.getWholeText());
        assertEquals("Hello world", tn.text()); // text() normalises whitespace (no change here)

        // chaining setter
        tn.text("New <b>text</b>");
        assertEquals("New <b>text</b>", tn.getWholeText());
    }

    @Test
    public void nodeNameIsHashText() {
        TextNode tn = new TextNode("sample");
        assertEquals("#text", tn.nodeName());
    }

    // ----------------------------------------------------------------------
    // Whitespace handling
    // ----------------------------------------------------------------------
    @Test
    public void isBlankDetectsVariousWhitespace() {
        assertTrue(new TextNode("").isBlank());
        assertTrue(new TextNode("   ").isBlank());
        assertTrue(new TextNode("\n\t  \r").isBlank());
        assertFalse(new TextNode(" a ").isBlank());
    }

    @Test
    public void normaliseWhitespaceStaticMethod() {
        String input = "a \t  b\nc  ";
        // Expected behaviour of StringUtil.normaliseWhitespace: collapse consecutive whitespace to a single space
        String expected = "a b c ";
        assertEquals(expected, TextNode.normaliseWhitespace(input));
    }

    @Test
    public void stripLeadingWhitespaceStaticMethod() {
        assertEquals("foo", TextNode.stripLeadingWhitespace("   foo"));
        assertEquals("foo", TextNode.stripLeadingWhitespace("\n\tfoo"));
        assertEquals("foo", TextNode.stripLeadingWhitespace("foo")); // no leading whitespace unchanged
    }

    @Test
    public void lastCharIsWhitespaceStaticMethod() {
        StringBuilder sb = new StringBuilder();
        assertFalse(TextNode.lastCharIsWhitespace(sb)); // empty

        sb.append("abc");
        assertFalse(TextNode.lastCharIsWhitespace(sb));

        sb.append(' ');
        assertTrue(TextNode.lastCharIsWhitespace(sb));
    }

    // ----------------------------------------------------------------------
    // splitText – normal operation
    // ----------------------------------------------------------------------
    @Test
    public void splitTextCreatesSiblingAndPreservesParent() {
        Document doc = new Document("");
        Element parent = doc.body(); // a block element
        TextNode original = new TextNode("HelloWorld");
        parent.appendChild(original);

        // split after 5 characters -> "Hello" | "World"
        TextNode tail = original.splitText(5);

        assertEquals("Hello", original.getWholeText());
        assertEquals("World", tail.getWholeText());

        // Verify sibling relationship inside parent
        assertEquals(original, parent.childNode(0));
        assertEquals(tail, parent.childNode(1));
        assertEquals(2, parent.childNodeSize());
    }

    @Test
    public void splitTextAtZeroOffsetProducesEmptyHead() {
        Document doc = new Document("");
        Element parent = doc.body();
        TextNode tn = new TextNode("Data");
        parent.appendChild(tn);

        TextNode tail = tn.splitText(0);

        assertEquals("", tn.getWholeText());
        assertEquals("Data", tail.getWholeText());
        assertEquals(tn, parent.childNode(0));
        assertEquals(tail, parent.childNode(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void splitTextNegativeOffsetThrows() {
        TextNode tn = new TextNode("abc");
        tn.splitText(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void splitTextOffsetEqualToLengthThrows() {
        TextNode tn = new TextNode("abc");
        tn.splitText(3); // length == 3, not allowed
    }

    // ----------------------------------------------------------------------
    // createFromEncoded static factories
    // ----------------------------------------------------------------------
    @Test
    public void createFromEncodedUnescapesHtmlEntities() {
        TextNode tn = TextNode.createFromEncoded("Tom &amp; Jerry &lt;3");
        assertEquals("Tom & Jerry <3", tn.getWholeText());

        // deprecated overload – still works the same
        TextNode tn2 = TextNode.createFromEncoded("A &gt; B", "http://example.com");
        assertEquals("A > B", tn2.getWholeText());
    }

    // ----------------------------------------------------------------------
    // outerHtmlHead – indentation and escaping
    // ----------------------------------------------------------------------
    @Test
    public void outerHtmlHeadPrettyPrintIndentationAndEscaping() throws IOException {
        Document.OutputSettings settings = new Document.OutputSettings()
                .prettyPrint(true)      // enable pretty printing (indentation)
                .outline(false);

        Document doc = new Document("");
        Element parent = doc.body(); // block element, will trigger indentation
        TextNode tn = new TextNode("<span> & \"test\"");
        parent.appendChild(tn);

        StringBuilder accum = new StringBuilder();
        // use reflection to call the package‑private method
        Method m = TextNode.class.getDeclaredMethod("outerHtmlHead", Appendable.class, int.class, Document.OutputSettings.class);
        m.setAccessible(true);
        m.invoke(tn, accum, 0, settings);

        // Expected: one indentation (two spaces by default) + escaped text
        String expectedIndent = "  "; // default indent is two spaces
        String expectedEscaped = "&lt;span&gt; &amp; &quot;test&quot;";
        assertTrue(accum.toString().startsWith(expectedIndent));
        assertTrue(accum.toString().contains(expectedEscaped));
    }

    @Test
    public void outerHtmlHeadNoPrettyPrintNoIndentation() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Document.OutputSettings settings = new Document.OutputSettings()
                .prettyPrint(false);

        TextNode tn = new TextNode("a<b>&c");
        StringBuilder accum = new StringBuilder();

        Method m = TextNode.class.getDeclaredMethod("outerHtmlHead", Appendable.class, int.class, Document.OutputSettings.class);
        m.setAccessible(true);
        m.invoke(tn, accum, 0, settings);

        // No leading spaces expected
        assertFalse(accum.toString().startsWith(" "));
        // Characters must be escaped (e.g., '<' -> &lt;)
        assertEquals("&lt;a&lt;b&gt;&amp;c", accum.toString());
    }

    // ----------------------------------------------------------------------
    // toString delegates to outerHtml()
    // ----------------------------------------------------------------------
    @Test
    public void toStringReturnsOuterHtml() {
        TextNode tn = new TextNode("<b>bold</b>");
        // outerHtml() escapes the text, so we expect the escaped version
        assertEquals("&lt;b&gt;bold&lt;/b&gt;", tn.toString());
    }
}
###Test END##