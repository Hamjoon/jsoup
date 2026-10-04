###Test START##
```java
package org.jsoup.nodes;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Entities.EscapeMode;
import org.junit.Test;

import java.io.IOException;
import java.util.StringJoiner;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link TextNode}.
 *
 * <p>This file was produced by a collaborative “three‑expert” exercise.
 * The steps were:</p>
 * <ul>
 *   <li>Extract public (and package‑private static) API.</li>
 *   <li>Write a basic test for each method.</li>
 *   <li>Identify edge‑cases &amp; exception scenarios.</li>
 *   <li>Write targeted tests for those scenarios.</li>
 *   <li>Merge everything into a single test class.</li>
 * </ul>
 */
public class TextNodeTest {

    /* --------------------------------------------------------------------- */
    /*  1.  Public constructors & basic getters / setters                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void constructor_setsCoreValue() {
        TextNode tn = new TextNode("hello");
        assertEquals("hello", tn.getWholeText());
    }

    @Test
    public void deprecatedConstructor_delegatesToPrimary() {
        TextNode tn = new TextNode("world", "http://example.com");
        assertEquals("world", tn.getWholeText());
    }

    @Test
    public void nodeName_isHashText() {
        TextNode tn = new TextNode("x");
        assertEquals("#text", tn.nodeName());
    }

    @Test
    public void text_returnsNormalisedWhitespace() {
        TextNode tn = new TextNode("a   b\tc\n\n d");
        // normaliseWhitespace collapses consecutive whitespace to a single space
        assertEquals("a b c d", tn.text());
    }

    @Test
    public void text_setterUpdatesCoreValue_andReturnsSelf() {
        TextNode tn = new TextNode("old");
        TextNode returned = tn.text("new value");
        assertSame(tn, returned);
        assertEquals("new value", tn.getWholeText());
    }

    @Test
    public void getWholeText_returnsUnmodifiedCoreValue() {
        TextNode tn = new TextNode("  keep   spaces \n");
        assertEquals("  keep   spaces \n", tn.getWholeText());
    }

    @Test
    public void isBlank_trueForEmptyString() {
        assertTrue(new TextNode("").isBlank());
    }

    @Test
    public void isBlank_trueForOnlyWhitespace() {
        assertTrue(new TextNode(" \t\n  ").isBlank());
    }

    @Test
    public void isBlank_falseWhenContainsNonWhitespace() {
        assertFalse(new TextNode("  a  ").isBlank());
    }

    /* --------------------------------------------------------------------- */
    /*  2.  splitText – normal operation & edge cases                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void splitText_createsSiblingWithTailAndKeepsHead() {
        TextNode original = new TextNode("HelloWorld");
        TextNode tail = original.splitText(5); // split after "Hello"

        assertEquals("Hello", original.getWholeText());
        assertEquals("World", tail.getWholeText());
        // No parent – the tail node should not be attached to anything
        assertNull(tail.parent());
    }

    @Test
    public void splitText_offsetZeroCreatesEmptyHead() {
        TextNode tn = new TextNode("ABC");
        TextNode tail = tn.splitText(0);
        assertEquals("", tn.getWholeText());
        assertEquals("ABC", tail.getWholeText());
    }

    @Test
    public void splitText_offsetAtLengthMinusOneLeavesOneCharInTail() {
        TextNode tn = new TextNode("XYZ");
        TextNode tail = tn.splitText(2); // split before last char
        assertEquals("XY", tn.getWholeText());
        assertEquals("Z", tail.getWholeText());
    }

    @Test(expected = IllegalArgumentException.class)
    public void splitText_negativeOffset_throws() {
        new TextNode("test").splitText(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void splitText_offsetEqualToLength_throws() {
        new TextNode("test").splitText(4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void splitText_offsetGreaterThanLength_throws() {
        new TextNode("test").splitText(10);
    }

    /* --------------------------------------------------------------------- */
    /*  3.  Static factory methods (createFromEncoded)                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void createFromEncoded_unescapesHtmlEntities() {
        TextNode tn = TextNode.createFromEncoded("a &lt; b &amp; c");
        assertEquals("a < b & c", tn.getWholeText());
    }

    @Test
    public void createFromEncoded_deprecatedVersion_behavesSame() {
        TextNode tn = TextNode.createFromEncoded("a &gt; b", "http://example.com");
        assertEquals("a > b", tn.getWholeText());
    }

    @Test(expected = NullPointerException.class)
    public void createFromEncoded_nullInput_throwsNPE() {
        TextNode.createFromEncoded(null);
    }

    /* --------------------------------------------------------------------- */
    /*  4.  Helper static methods (normaliseWhitespace, stripLeadingWhitespace,
     *      lastCharIsWhitespace)                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void normaliseWhitespace_collapsesWhitespace() {
        String input = "a   b\tc\n\n d";
        String expected = "a b c d";
        assertEquals(expected, TextNode.normaliseWhitespace(input));
    }

    @Test
    public void stripLeadingWhitespace_removesOnlyLeading() {
        assertEquals("foo bar", TextNode.stripLeadingWhitespace("   foo bar"));
        assertEquals("foo", TextNode.stripLeadingWhitespace("\t\nfoo"));
        // When there is no leading whitespace the string stays unchanged
        assertEquals("nochange", TextNode.stripLeadingWhitespace("nochange"));
    }

    @Test
    public void lastCharIsWhitespace_behaviour() {
        StringBuilder sbEmpty = new StringBuilder();
        assertFalse(TextNode.lastCharIsWhitespace(sbEmpty));

        StringBuilder sbSpace = new StringBuilder("test ");
        assertTrue(TextNode.lastCharIsWhitespace(sbSpace));

        StringBuilder sbNonSpace = new StringBuilder("testX");
        assertFalse(TextNode.lastCharIsWhitespace(sbNonSpace));
    }

    /* --------------------------------------------------------------------- */
    /*  5.  toString() – delegated to outerHtml() (basic sanity check)       */
    /* --------------------------------------------------------------------- */

    @Test
    public void toString_returnsOuterHtmlRepresentation() {
        TextNode tn = new TextNode("<b>bold</b>");
        // outerHtml escapes the text, so we expect the escaped version
        assertEquals("&lt;b&gt;bold&lt;/b&gt;", tn.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  6.  outerHtmlHead – ensure it does not throw when prettyPrint is on */
    /* --------------------------------------------------------------------- */

    @Test
    public void outerHtmlHead_doesNotThrowWhenPrettyPrint() throws IOException {
        // Build a tiny document tree so that parent() is non‑null and the indent
        // logic is exercised.
        Element parent = new Element(Tag.valueOf("p"), "");
        TextNode tn = new TextNode("some text");
        parent.appendChild(tn); // now tn.parent() == parent

        Document.OutputSettings settings = new Document.OutputSettings();
        settings.prettyPrint(true);
        StringBuilder accum = new StringBuilder();
        tn.outerHtmlHead(accum, 0, settings);
        // No exception means the test passes; we also assert that something was
        // written (the escaped text).
        assertTrue(accum.length() > 0);
    }
}
```
###Test END##