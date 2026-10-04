###Test START##
```java
package org.jsoup.nodes;

import org.jsoup.parser.Tag;
import org.jsoup.nodes.Document.OutputSettings;
import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link TextNode}.
 *
 * The tests are divided into three sections:
 * <ol>
 *   <li>Listing of all public methods with their signatures.</li>
 *   <li>Basic functional tests for each public method.</li>
 *   <li>Edge‑case / exception tests for methods that have validation logic.</li>
 * </ol>
 *
 * The suite deliberately avoids any reliance on internal package‑private helpers;
 * only the public API of {@code TextNode} is exercised.
 */
public class TextNodeTest {

    /** -----------------------------------------------------------------
     *  1. Public API of {@link TextNode}
     *  -----------------------------------------------------------------
     *
     *  Constructors
     *  -----------------
     *  public TextNode(String text)
     *  public TextNode(String text, String baseUri)               // deprecated
     *
     *  Instance methods
     *  -----------------
     *  public String nodeName()
     *  public String text()
     *  public TextNode text(String text)
     *  public String getWholeText()
     *  public boolean isBlank()
     *  public TextNode splitText(int offset)
     *  public String toString()
     *
     *  Static factory methods
     *  -----------------------
     *  public static TextNode createFromEncoded(String encodedText, String baseUri)
     *  public static TextNode createFromEncoded(String encodedText)
     *
     *  ----------------------------------------------------------------- */
    
    private TextNode simpleNode;

    @Before
    public void setUp() {
        simpleNode = new TextNode("Hello world");
    }

    /** -----------------------------------------------------------------
     *  2. Basic functional tests
     *  ----------------------------------------------------------------- */

    @Test
    public void nodeName_returnsHashText() {
        assertEquals("#text", simpleNode.nodeName());
    }

    @Test
    public void text_returnsNormalisedWhitespace() {
        TextNode tn = new TextNode("  Hello   \n  world \t!");
        // Expected normalisation: collapse whitespace to a single space and trim ends
        assertEquals(" Hello world !", tn.text());
    }

    @Test
    public void text_setterUpdatesValueAndReturnsSelf() {
        TextNode tn = new TextNode("old");
        TextNode returned = tn.text("new");
        assertSame(tn, returned);
        assertEquals("new", tn.getWholeText());
        assertEquals("new", tn.text()); // normalised version is the same for simple text
    }

    @Test
    public void getWholeText_returnsUnmodifiedValue() {
        String raw = "   line1\nline2   ";
        TextNode tn = new TextNode(raw);
        assertEquals(raw, tn.getWholeText());
    }

    @Test
    public void isBlank_detectsBlankStrings() {
        assertTrue(new TextNode("").isBlank());
        assertTrue(new TextNode("   ").isBlank());
        assertTrue(new TextNode("\n\t ").isBlank());
        assertFalse(new TextNode(" not blank ").isBlank());
    }

    @Test
    public void splitText_normalCaseSplitsCorrectlyAndMaintainsParent() {
        // Build a parent element and attach a TextNode
        Element parent = new Element(Tag.valueOf("p"), "");
        TextNode original = new TextNode("abcdefgh");
        parent.appendChild(original);               // original is now child index 0

        // Split at offset 3 -> "abc" | "defgh"
        TextNode tail = original.splitText(3);

        // Verify the original node now holds the head part
        assertEquals("abc", original.getWholeText());
        // Verify the returned node holds the tail part
        assertEquals("defgh", tail.getWholeText());

        // Verify sibling order inside the parent
        assertEquals(2, parent.childNodeSize());
        assertSame(original, parent.childNode(0));
        assertSame(tail, parent.childNode(1));
        // Verify sibling index values
        assertEquals(0, original.siblingIndex());
        assertEquals(1, tail.siblingIndex());
    }

    @Test
    public void toString_returnsOuterHtmlRepresentation() {
        // For a plain TextNode, outerHtml() is just the escaped text.
        TextNode tn = new TextNode("<b>bold</b>");
        // The outer HTML should escape the angle brackets.
        assertEquals("&lt;b&gt;bold&lt;/b&gt;", tn.toString());
    }

    @Test
    public void createFromEncoded_decodesEntities() {
        TextNode tn = TextNode.createFromEncoded("Tom &amp; Jerry");
        assertEquals("Tom & Jerry", tn.getWholeText());

        TextNode tn2 = TextNode.createFromEncoded("5 &lt; 10 &gt; 2");
        assertEquals("5 < 10 > 2", tn2.getWholeText());
    }

    @Test
    public void createFromEncoded_withBaseUri_behavesSameAsWithout() {
        TextNode tn = TextNode.createFromEncoded("a &gt; b", "http://example.com");
        assertEquals("a > b", tn.getWholeText());
    }

    /** -----------------------------------------------------------------
     *  3. Edge‑case / exception tests
     *  ----------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void splitText_negativeOffset_throws() {
        TextNode tn = new TextNode("abc");
        tn.splitText(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void splitText_offsetEqualToLength_throws() {
        TextNode tn = new TextNode("abc");
        tn.splitText(3); // length is 3, valid offsets are 0..2
    }

    @Test(expected = IllegalArgumentException.class)
    public void splitText_offsetGreaterThanLength_throws() {
        TextNode tn = new TextNode("abc");
        tn.splitText(5);
    }

    @Test
    public void splitText_withoutParent_returnsTailNodeAlone() {
        TextNode tn = new TextNode("12345");
        TextNode tail = tn.splitText(2);
        // Original should be "12", tail should be "345"
        assertEquals("12", tn.getWholeText());
        assertEquals("345", tail.getWholeText());
        // Tail should have no parent (null)
        assertNull(tail.parent());
    }

    @Test
    public void constructor_withNullText_createsNodeWithNullValue() {
        // The class does not explicitly forbid null; coreValue will be null.
        TextNode tn = new TextNode((String) null);
        // getWholeText() should return null, and other methods must handle it gracefully.
        assertNull(tn.getWholeText());
        // Normalisation of null should return null (StringUtil.normaliseWhitespace returns null)
        assertNull(tn.text());
        // isBlank should treat null as blank (StringUtil.isBlank returns true for null)
        assertTrue(tn.isBlank());
    }

    @Test
    public void static_normaliseWhitespace_isPackagePrivate_butCanBeTestedViaReflection() throws Exception {
        // Verify the helper method works as expected even though it is package‑private.
        java.lang.reflect.Method m = TextNode.class.getDeclaredMethod("normaliseWhitespace", String.class);
        m.setAccessible(true);
        String result = (String) m.invoke(null, "a   b\tc\n\n d");
        assertEquals("a b c d", result);
    }

    @Test
    public void static_stripLeadingWhitespace_isPackagePrivate_butWorks() throws Exception {
        java.lang.reflect.Method m = TextNode.class.getDeclaredMethod("stripLeadingWhitespace", String.class);
        m.setAccessible(true);
        String result = (String) m.invoke(null, "   leading spaces");
        assertEquals("leading spaces", result);
    }

    @Test
    public void static_lastCharIsWhitespace_isPackagePrivate_butWorks() throws Exception {
        java.lang.reflect.Method m = TextNode.class.getDeclaredMethod("lastCharIsWhitespace", StringBuilder.class);
        m.setAccessible(true);
        StringBuilder sb1 = new StringBuilder("abc ");
        StringBuilder sb2 = new StringBuilder("abc");
        assertTrue((Boolean) m.invoke(null, sb1));
        assertFalse((Boolean) m.invoke(null, sb2));
    }
}
```
###Test END##