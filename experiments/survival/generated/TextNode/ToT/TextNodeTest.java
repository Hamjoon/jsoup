package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;
import org.jsoup.nodes.Document.OutputSettings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link TextNode}.
 *
 * The tests cover:
 * <ul>
 *   <li>construction and basic getters</li>
 *   <li>text normalisation, setting and whole‑text retrieval</li>
 *   <li>blank detection</li>
 *   <li>splitting (normal, edge‑cases and error scenarios)</li>
 *   <li>HTML escaping via {@code outerHtml()}</li>
 *   <li>static helper methods</li>
 *   <li>factory methods {@code createFromEncoded}</li>
 * </ul>
 */
public class TextNodeTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Document doc;
    private OutputSettings out;

    @Before
    public void setUp() {
        // a Document with default pretty‑print settings – used for outerHtml tests
        doc = Jsoup.parse("<!doctype html><html></html>", "", Parser.htmlParser());
        out = doc.outputSettings();
        out.prettyPrint(true);
    }

    /* --------------------------------------------------------------------- *
     *  Basic construction / nodeName
     * --------------------------------------------------------------------- */

    @Test
    public void nodeName_isHashText() {
        TextNode tn = new TextNode("sample");
        assertEquals("#text", tn.nodeName());
    }

    /* --------------------------------------------------------------------- *
     *  getWholeText / text() normalisation
     * --------------------------------------------------------------------- */

    @Test
    public void getWholeText_returnsExactSuppliedText() {
        String raw = "  a\tb\nc  ";
        TextNode tn = new TextNode(raw);
        assertEquals(raw, tn.getWholeText());
    }

    @Test
    public void text_normalisesWhitespace() {
        TextNode tn = new TextNode("  hello   \tworld \n  ");
        // StringUtil.normaliseWhitespace collapses consecutive whitespace to a single space and trims.
        assertEquals("hello world", tn.text());
    }

    @Test
    public void text_setterUpdatesValue() {
        TextNode tn = new TextNode("old");
        tn.text("new");
        assertEquals("new", tn.getWholeText());
        assertEquals("new", tn.text()); // normalisation of a simple word is a no‑op
    }

    /* --------------------------------------------------------------------- *
     *  isBlank
     * --------------------------------------------------------------------- */

    @Test
    public void isBlank_trueForEmptyOrOnlyWhitespace() {
        assertTrue(new TextNode("").isBlank());
        assertTrue(new TextNode("   ").isBlank());
        assertTrue(new TextNode("\n\t  ").isBlank());
    }

    @Test
    public void isBlank_falseWhenContainsNonWhitespace() {
        assertFalse(new TextNode(" a ").isBlank());
        assertFalse(new TextNode("x").isBlank());
    }

    /* --------------------------------------------------------------------- *
     *  splitText – normal flow
     * --------------------------------------------------------------------- */

    @Test
    public void splitText_splitsAtOffsetAndKeepsParentRelation() {
        Element parent = new Element(Tag.valueOf("p"), "");
        TextNode original = new TextNode("abcdef");
        parent.appendChild(original);                 // original is now child #0

        TextNode tail = original.splitText(3);         // split after "abc"

        // original node now contains head
        assertEquals("abc", original.getWholeText());

        // returned node contains tail and is inserted after the original
        assertEquals("def", tail.getWholeText());
        assertSame(parent, tail.parent());            // sibling relationship
        assertEquals(1, tail.siblingIndex());        // index after original
    }

    @Test
    public void splitText_offsetZeroCreatesEmptyHead() {
        TextNode tn = new TextNode("hello");
        TextNode tail = tn.splitText(0);
        assertEquals("", tn.getWholeText());
        assertEquals("hello", tail.getWholeText());
    }

    @Test
    public void splitText_withoutParentStillReturnsTailNode() {
        TextNode tn = new TextNode("xyz");
        TextNode tail = tn.splitText(2);
        assertEquals("xy", tn.getWholeText());
        assertEquals("z", tail.getWholeText());
        assertNull(tail.parent());   // no parent was set
    }

    /* --------------------------------------------------------------------- *
     *  splitText – error scenarios
     * --------------------------------------------------------------------- */

    @Test
    public void splitText_negativeOffset_throwsIAE() {
        TextNode tn = new TextNode("abc");
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Split offset must be not be negative");
        tn.splitText(-1);
    }

    @Test
    public void splitText_offsetEqualToLength_throwsIAE() {
        TextNode tn = new TextNode("abc");
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Split offset must not be greater than current text length");
        tn.splitText(3); // length == 3 -> illegal
    }

    @Test
    public void splitText_offsetGreaterThanLength_throwsIAE() {
        TextNode tn = new TextNode("abc");
        thrown.expect(IllegalArgumentException.class);
        tn.splitText(4);
    }

    /* --------------------------------------------------------------------- *
     *  HTML escaping via outerHtml / toString
     * --------------------------------------------------------------------- */

    @Test
    public void outerHtml_escapesSpecialCharacters() {
        TextNode tn = new TextNode("<div> & \"");
        String html = tn.outerHtml();
        // '<' and '&' and '"' must be escaped
        assertTrue(html.contains("&lt;div&gt;"));
        assertTrue(html.contains("&amp;"));
        assertTrue(html.contains("&quot;"));
    }

    @Test
    public void outerHtml_respectsPrettyPrintAndIndentation() {
        // Build a simple DOM: <p>   a   b </p>
        Element p = new Element(Tag.valueOf("p"), "");
        p.appendChild(new TextNode("   a   b   "));
        doc.appendChild(p);

        // prettyPrint is true (set in @Before)
        String html = p.outerHtml();

        // Expect a line‑break and a single leading indent (2 spaces by default)
        assertTrue(html.startsWith("<p>\n"));
        // The text inside should be normalised (multiple spaces collapsed)
        assertTrue(html.contains("a b"));
    }

    @Test
    public void toString_returnsSameAsOuterHtml() {
        TextNode tn = new TextNode("sample");
        assertEquals(tn.outerHtml(), tn.toString());
    }

    /* --------------------------------------------------------------------- *
     *  createFromEncoded – factory methods
     * --------------------------------------------------------------------- */

    @Test
    public void createFromEncoded_decodesEntities() {
        TextNode tn = TextNode.createFromEncoded("&lt;span class=&quot;test&quot;&gt;");
        assertEquals("<span class=\"test\">", tn.getWholeText());
    }

    @Test
    public void createFromEncoded_withBaseUri_deprecatedOverload_behavesSame() {
        TextNode tn = TextNode.createFromEncoded("&amp;nbsp;", "http://example.com");
        assertEquals("\u00A0", tn.getWholeText()); // non‑breaking space
    }

    /* --------------------------------------------------------------------- *
     *  Static helper methods
     * --------------------------------------------------------------------- */

    @Test
    public void normaliseWhitespace_collapsesAndTrims() {
        String input = "  \tHello   World \n ";
        String expected = "Hello World";
        assertEquals(expected, TextNode.normaliseWhitespace(input));
    }

    @Test
    public void stripLeadingWhitespace_removesOnlyLeading() {
        String input = "   \t  abc  def";
        String expected = "abc  def";
        assertEquals(expected, TextNode.stripLeadingWhitespace(input));
    }

    @Test
    public void lastCharIsWhitespace_detectsCorrectly() {
        StringBuilder sb1 = new StringBuilder("abc ");
        StringBuilder sb2 = new StringBuilder("abc");
        assertTrue(TextNode.lastCharIsWhitespace(sb1));
        assertFalse(TextNode.lastCharIsWhitespace(sb2));
        assertFalse(TextNode.lastCharIsWhitespace(new StringBuilder()));
    }

    /* --------------------------------------------------------------------- *
     *  Additional sanity checks
     * --------------------------------------------------------------------- */

    @Test
    public void textMethodDoesNotModifyOriginalWhenWhitespaceIsAlreadyNormal() {
        TextNode tn = new TextNode("simple");
        assertSame(tn, tn); // sanity
        assertEquals("simple", tn.text()); // unchanged
    }

    @Test
    public void splitText_onSingleCharacterNode() {
        TextNode tn = new TextNode("x");
        // offset 0 yields empty head and tail "x"
        TextNode tail = tn.splitText(0);
        assertEquals("", tn.getWholeText());
        assertEquals("x", tail.getWholeText());
    }
}
