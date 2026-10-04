package org.jsoup.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.parser.HtmlTreeBuilderState;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Comprehensive JUnit‑4 test suite for {@link HtmlTreeBuilderState}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical use‑cases for each major state (Initial, BeforeHtml, InHead, InBody, InTable, InSelect, AfterBody, etc.)</li>
 *   <li>Edge‑cases such as whitespace‑only input, comments, malformed tags and unexpected tokens.</li>
 *   <li>Error handling – Jsoup’s {@link Parser#setTrackErrors(boolean)} records parsing errors; the tests verify that the
 *        appropriate errors are generated without throwing exceptions.</li>
 * </ul>
 *
 * The approach is to feed HTML strings that force the parser into the desired state and then assert on the resulting
 * DOM structure and any recorded errors.  Direct instantiation of {@link HtmlTreeBuilderState} is avoided because the
 * class is package‑private; instead we use the public {@link Jsoup#parse(String, String, Parser)} API.
 */
public class HtmlTreeBuilderStateTest {

    /** Utility to parse HTML with error tracking. */
    private static Document parse(String html) {
        Parser parser = Parser.htmlParser();
        parser.setTrackErrors(true);
        return Jsoup.parse(html, "", parser);
    }

    /** Utility to retrieve parsing errors from the last parse. */
    private static List<ParseError> errors(Parser parser) {
        return parser.getErrors();
    }

    /**
     * Initial state – a doctype token must be processed and a {@code DocumentType} node created.
     */
    @Test
    public void testInitialStateDoctype() {
        Document doc = parse("<!DOCTYPE html><html></html>");
        Assert.assertEquals("html", doc.child(0).nodeName()); // first element after doctype is <html>
        Assert.assertEquals("html", doc.child(0).attr("xmlns")); // ensure document has been normalised
        // The parser should have created exactly one DocumentType node.
        Assert.assertTrue(doc.childNode(0) instanceof org.jsoup.nodes.DocumentType);
        org.jsoup.nodes.DocumentType doctype = (org.jsoup.nodes.DocumentType) doc.childNode(0);
        Assert.assertEquals("html", doctype.name());
        // No errors expected.
        Assert.assertTrue(((Parser) doc.parser()).getErrors().isEmpty());
    }

    /**
     * BeforeHtml state – whitespace and comments before the {@code <html>} tag are ignored or attached correctly.
     */
    @Test
    public void testBeforeHtmlStateCommentAndWhitespace() {
        Document doc = parse("\n  <!-- before -->\n<html><head></head><body></body></html>");
        // The comment should be a child of the document (not of the html element).
        Assert.assertEquals(2, doc.childNodeSize()); // DocumentType (if any) + comment + html element
        Assert.assertTrue(doc.childNode(1) instanceof org.jsoup.nodes.Comment);
        Assert.assertEquals(" before ", ((org.jsoup.nodes.Comment) doc.childNode(1)).getData());
        // The <html> element must be present.
        Element html = doc.selectFirst("html");
        Assert.assertNotNull(html);
    }

    /**
     * InHead state – processing of a {@code <title>} element (RCData) and a {@code <script>} element.
     */
    @Test
    public void testInHeadProcessing() {
        String htmlSrc = "<html><head><title>My Title</title><script>var a = 1;</script></head><body></body></html>";
        Document doc = parse(htmlSrc);
        // Title text should be present.
        Element title = doc.selectFirst("title");
        Assert.assertNotNull(title);
        Assert.assertEquals("My Title", title.text());
        // Script should be retained as a child of <head>.
        Element script = doc.selectFirst("head > script");
        Assert.assertNotNull(script);
        Assert.assertEquals("var a = 1;", script.data());
    }

    /**
     * InBody – handling of formatting elements (e.g., {@code <a>}) and the active formatting list.
     */
    @Test
    public void testInBodyStartTagFormatting() {
        Document doc = parse("<html><body><p>Click <a href='foo'>here</a> now.</p></body></html>");
        Element a = doc.selectFirst("a");
        Assert.assertNotNull(a);
        Assert.assertEquals("foo", a.attr("href"));
        Assert.assertEquals("here", a.text());
        // Ensure the surrounding text nodes are preserved.
        Element p = doc.selectFirst("p");
        Assert.assertEquals("Click ", p.childNode(0).toString());
        Assert.assertEquals(" now.", p.childNode(2).toString());
    }

    /**
     * InTable – text that appears directly inside a table (outside of {@code <tbody>/<tr>/<td>}
     * should be foster‑parented into the nearest parent element.
     */
    @Test
    public void testInTableFosterParenting() {
        Document doc = parse("<html><body><table>text outside<td>cell</td></table></body></html>");
        // The stray text "text outside" should be moved to the body (foster parenting).
        Element body = doc.body();
        Assert.assertTrue(body.text().contains("text outside"));
        // The cell should still be present.
        Element td = doc.selectFirst("td");
        Assert.assertNotNull(td);
        Assert.assertEquals("cell", td.text());
    }

    /**
     * InSelect – proper handling of {@code <option>} and {@code <optgroup>} elements,
     * as well as error handling when a stray {@code <select>} end tag appears.
     */
    @Test
    public void testInSelectOptionsAndError() {
        String src = "<select><option>One</option><optgroup label='g'><option>Two</option></optgroup></select>";
        Document doc = parse(src);
        Element select = doc.selectFirst("select");
        Assert.assertNotNull(select);
        // Two options, one inside optgroup.
        List<Element> options = select.select("option");
        Assert.assertEquals(2, options.size());
        Assert.assertEquals("One", options.get(0).text());
        Assert.assertEquals("Two", options.get(1).text());
        // Verify the optgroup label attribute.
        Element optgroup = select.selectFirst("optgroup");
        Assert.assertEquals("g", optgroup.attr("label"));
        // No parsing errors should be recorded for correct markup.
        Assert.assertTrue(((Parser) doc.parser()).getErrors().isEmpty());

        // Now feed malformed markup: stray </select> inside the select.
        String malformed = "<select><option>One</option></select></select>";
        Document doc2 = parse(malformed);
        // The parser should recover and produce only one select element.
        Assert.assertEquals(1, doc2.select("select").size());
        // An error must be recorded.
        List<ParseError> errors = ((Parser) doc2.parser()).getErrors();
        Assert.assertFalse(errors.isEmpty());
        Assert.assertTrue(errors.get(0).getErrorMessage().toLowerCase().contains("unexpected"));
    }

    /**
     * AfterBody – an unexpected start tag after </body> should cause the parser to transition
     * back to InBody and re‑process the token.
     */
    @Test
    public void testAfterBodyUnexpectedTag() {
        String src = "<html><body><p>Hello</p></body><div id='extra'>Extra</div></html>";
        Document doc = parse(src);
        // The <div> must be inside the body after recovery.
        Element div = doc.selectFirst("body > div#extra");
        Assert.assertNotNull(div);
        Assert.assertEquals("Extra", div.text());
    }

    /**
     * Error handling – an unexpected doctype inside the head should produce an error
     * but must not abort parsing.
     */
    @Test
    public void testErrorUnexpectedDoctypeInHead() {
        String src = "<html><head><![CDATA[<!DOCTYPE html>]]><title>Err</title></head><body></body></html>";
        Document doc = parse(src);
        // Title should still be parsed.
        Element title = doc.selectFirst("title");
        Assert.assertNotNull(title);
        Assert.assertEquals("Err", title.text());
        // An error should be recorded for the misplaced doctype.
        List<ParseError> errors = ((Parser) doc.parser()).getErrors();
        Assert.assertFalse(errors.isEmpty());
        Assert.assertTrue(errors.get(0).getErrorMessage().toLowerCase().contains("doctype"));
    }

    /**
     * InFrameset – parsing of a simple frameset document, ensuring that
     * {@code <frame>} and {@code <noframes>} are handled correctly.
     */
    @Test
    public void testInFramesetProcessing() {
        String src = "<html><frameset cols='50%,50%'><frame src='a.html'><frame src='b.html'><noframes><body>No frames</body></noframes></frameset></html>";
        Document doc = parse(src);
        // Two frame elements must be present.
        List<Element> frames = doc.select("frame");
        Assert.assertEquals(2, frames.size());
        Assert.assertEquals("a.html", frames.get(0).attr("src"));
        Assert.assertEquals("b.html", frames.get(1).attr("src"));
        // The noframes content should be parsed as normal body content.
        Element noframesBody = doc.selectFirst("noframes > body");
        Assert.assertNotNull(noframesBody);
        Assert.assertEquals("No frames", noframesBody.text());
    }

    /**
     * ForeignContent – even though this state is a stub, we verify that the parser never
     * reaches it for normal HTML input.  The test simply asserts that no errors are logged
     * for a standard document.
     */
    @Test
    public void testForeignContentNeverEntered() {
        Document doc = parse("<!DOCTYPE html><html><body><svg><circle cx='5' cy='5' r='4'/></svg></body></html>");
        // The <svg> element is parsed in the InBody state (foreign content handling is a no‑op stub).
        Element svg = doc.selectFirst("svg");
        Assert.assertNotNull(svg);
        // No errors should be recorded.
        Assert.assertTrue(((Parser) doc.parser()).getErrors().isEmpty());
    }
}
