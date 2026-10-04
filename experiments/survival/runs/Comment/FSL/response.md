###Test START##
package org.jsoup.nodes;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

/**
 * Unit tests for {@link Comment}.
 *
 * These tests cover:
 * <ul>
 *   <li>Construction (both constructors)</li>
 *   <li>nodeName()</li>
 *   <li>getData()</li>
 *   <li>outerHtmlHead() with pretty‑print on and off</li>
 *   <li>outerHtmlTail() (no‑op)</li>
 *   <li>toString() delegating to {@code outerHtml()}</li>
 * </ul>
 */
public class CommentTest {

    /** Helper that creates a default {@link Document.OutputSettings}. */
    private Document.OutputSettings defaultSettings() {
        Document.OutputSettings out = new Document.OutputSettings();
        out.prettyPrint(true);          // default pretty‑print
        out.indentAmount(2);            // make indentation deterministic
        return out;
    }

    @Test
    public void constructor_SingleArg_SetsData() {
        Comment c = new Comment("sample");
        assertEquals("sample", c.getData());
    }

    @Test
    public void constructor_TwoArgs_Deprecated_DelegatesToSingleArg() {
        Comment c = new Comment("sample", "http://example.com");
        assertEquals("sample", c.getData());
    }

    @Test
    public void nodeName_ReturnsCommentTag() {
        Comment c = new Comment("any");
        assertEquals("#comment", c.nodeName());
    }

    @Test
    public void getData_ReturnsCoreValue() {
        String payload = "This is a comment";
        Comment c = new Comment(payload);
        assertEquals(payload, c.getData());
    }

    @Test
    public void outerHtmlHead_PrettyPrintFalse_NoIndentation() throws IOException {
        Comment c = new Comment("no‑pretty");
        StringBuilder accum = new StringBuilder();

        Document.OutputSettings out = new Document.OutputSettings();
        out.prettyPrint(false);          // disable pretty‑print

        c.outerHtmlHead(accum, 3, out);   // depth is irrelevant when prettyPrint is false

        assertEquals("<!--no‑pretty-->", accum.toString());
    }

    @Test
    public void outerHtmlHead_PrettyPrintTrue_WithIndentation() throws IOException {
        Comment c = new Comment("indented");
        StringBuilder accum = new StringBuilder();

        Document.OutputSettings out = defaultSettings(); // prettyPrint = true, indentAmount = 2
        int depth = 2; // expect two * indentAmount spaces after a newline

        c.outerHtmlHead(accum, depth, out);

        // Expected: newline + (depth * indentAmount) spaces + comment markup
        String expected = "\n" + "    " + "<!--indented-->";
        // depth=2, indentAmount=2 → 4 spaces
        assertEquals(expected, accum.toString());
    }

    @Test
    public void outerHtmlTail_DoesNothing() throws IOException {
        Comment c = new Comment("tail‑test");
        StringBuilder accum = new StringBuilder("initial");

        Document.OutputSettings out = defaultSettings();

        c.outerHtmlTail(accum, 0, out);

        assertEquals("initial", accum.toString());
    }

    @Test
    public void toString_DelegatesToOuterHtml() {
        Comment c = new Comment("string‑repr");
        // outerHtml() is a public method inherited from Node
        String outer = c.outerHtml();
        assertEquals(outer, c.toString());
    }

    @Test
    public void outerHtml_WholeCommentStructure() {
        // Verify that the public outerHtml() method produces a correctly formatted comment
        Comment c = new Comment("full‑comment");
        String result = c.outerHtml();

        // By default Jsoup's OutputSettings prettyPrint = true, indentAmount = 1
        // With depth = 0 there is no leading newline or spaces.
        assertEquals("<!--full‑comment-->", result);
    }
}
###Test END##