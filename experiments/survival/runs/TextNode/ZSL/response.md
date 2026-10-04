###Test START##
package org.jsoup.nodes;

import org.jsoup.parser.Tag;
import org.junit.Test;
import static org.junit.Assert.*;

public class TextNodeTest {

    @Test
    public void testConstructorAndNodeName() {
        TextNode tn = new TextNode("hello");
        assertEquals("#text", tn.nodeName());

        // deprecated constructor – should behave the same
        TextNode tn2 = new TextNode("world", "http://example.com");
        assertEquals("#text", tn2.nodeName());
        assertEquals("world", tn2.getWholeText());
    }

    @Test
    public void testTextAndWholeText() {
        TextNode tn = new TextNode(" a  b \n c ");
        // getWholeText returns the raw value
        assertEquals(" a  b \n c ", tn.getWholeText());

        // text() returns normalised whitespace
        assertEquals("a b c", tn.text());

        // chaining setter
        tn.text("   new   text   ");
        assertEquals("new text", tn.text());
        assertEquals("   new   text   ", tn.getWholeText());
    }

    @Test
    public void testIsBlank() {
        assertTrue(new TextNode("").isBlank());
        assertTrue(new TextNode("   \n\t ").isBlank());
        assertFalse(new TextNode(" not blank ").isBlank());
    }

    @Test
    public void testSplitTextBasic() {
        TextNode tn = new TextNode("abcdef");
        TextNode tail = tn.splitText(3);
        assertEquals("abc", tn.getWholeText());
        assertEquals("def", tail.getWholeText());
        assertNotSame(tn, tail);
    }

    @Test
    public void testSplitTextWithParent() {
        Element parent = new Element(Tag.valueOf("p"), "");
        TextNode first = new TextNode("first");
        parent.appendChild(first);

        // split inside the node
        TextNode second = first.splitText(2); // "fi" | "rst"
        assertEquals("fi", first.getWholeText());
        assertEquals("rst", second.getWholeText());

        // parent must now contain two ordered text nodes
        assertEquals(2, parent.childNodeSize());
        assertSame(first, parent.childNode(0));
        assertSame(second, parent.childNode(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSplitTextNegativeOffset() {
        TextNode tn = new TextNode("abc");
        tn.splitText(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSplitTextOffsetTooLarge() {
        TextNode tn = new TextNode("abc");
        tn.splitText(3); // length == 3, offset must be < length
    }

    @Test
    public void testCreateFromEncoded() {
        TextNode tn = TextNode.createFromEncoded("Hello &amp; World");
        assertEquals("Hello & World", tn.getWholeText());

        TextNode tn2 = TextNode.createFromEncoded("Hello &amp; World", "http://example.com");
        assertEquals("Hello & World", tn2.getWholeText());
    }

    @Test
    public void testStaticUtilityMethods() {
        // normaliseWhitespace collapses spaces and trims
        assertEquals("a b c", TextNode.normaliseWhitespace("  a   b   c  "));

        // stripLeadingWhitespace removes only leading whitespace
        assertEquals("text   ", TextNode.stripLeadingWhitespace("   text   "));

        // lastCharIsWhitespace
        StringBuilder sb1 = new StringBuilder("abc ");
        assertTrue(TextNode.lastCharIsWhitespace(sb1));
        StringBuilder sb2 = new StringBuilder("abc");
        assertFalse(TextNode.lastCharIsWhitespace(sb2));
    }

    @Test
    public void testOuterHtmlAndToString() {
        // TextNode should escape HTML entities when outerHtml is called
        TextNode tn = new TextNode("<div> & \"");
        String html = tn.outerHtml(); // expected escaped representation
        assertEquals("&lt;div&gt; &amp; &quot;", html);
        // toString delegates to outerHtml()
        assertEquals(html, tn.toString());
    }
}
###Test END##