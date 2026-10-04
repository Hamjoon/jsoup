**###Test START##**  
```java
package org.jsoup.nodes;

import org.jsoup.nodes.Document.OutputSettings;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.Rule;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Comment}.
 *
 * <p>All public and package‑private behaviours are exercised:
 * <ul>
 *   <li>Constructors (including the deprecated two‑arg variant)</li>
 *   <li>{@link Comment#nodeName()}</li>
 *   <li>{@link Comment#getData()}</li>
 *   <li>{@link Comment#outerHtmlHead(java.lang.Appendable, int, Document.OutputSettings)}</li>
 *   <li>{@link Comment#outerHtmlTail(java.lang.Appendable, int, Document.OutputSettings)} (no‑op)</li>
 *   <li>{@link Comment#toString()} – which delegates to {@code outerHtml()}</li>
 * </ul>
 *
 * <p>Typical, edge‑case and error scenarios are covered:
 * <ul>
 *   <li>Empty and {@code null} comment data</li>
 *   <li>Special characters that must be preserved</li>
 *   <li>Pretty‑print enabled/disabled (affects indentation)</li>
 *   <li>Simulated {@link IOException} from the {@link Appendable}</li>
 * </ul>
 */
public class CommentTest {

    private static final String SAMPLE_DATA = "This is a comment";
    private static final String EMPTY_DATA = "";
    private static final String SPECIAL_DATA = "a<b>c & d\"e'";

    private OutputSettings prettySettings;
    private OutputSettings compactSettings;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // Settings with prettyPrint = true (default indent = 1 tab = 1 * 4 spaces)
        prettySettings = new OutputSettings();
        prettySettings.prettyPrint(true);
        prettySettings.indentAmount(4);

        // Settings with prettyPrint = false – no indentation should be added
        compactSettings = new OutputSettings();
        compactSettings.prettyPrint(false);
    }

    /*** Constructor and getData() ***/
    @Test
    public void testConstructorAndGetData_Typical() {
        Comment c = new Comment(SAMPLE_DATA);
        assertEquals("Data returned by getData() must match constructor argument",
                SAMPLE_DATA, c.getData());
    }

    @Test
    public void testConstructorAndGetData_EmptyString() {
        Comment c = new Comment(EMPTY_DATA);
        assertEquals("Empty comment should be preserved", EMPTY_DATA, c.getData());
    }

    @Test
    public void testConstructorAndGetData_Null() {
        // The original Jsoup implementation does not guard against null,
        // it stores the null reference directly – we verify that behaviour.
        Comment c = new Comment(null);
        assertNull("getData() should return null when constructed with null", c.getData());
    }

    @Test
    public void testDeprecatedTwoArgConstructor() {
        // Even though deprecated, it must behave identically to the single‑arg ctor.
        Comment c = new Comment(SPECIAL_DATA, "http://example.com");
        assertEquals(SPECIAL_DATA, c.getData());
    }

    /*** nodeName() ***/
    @Test
    public void testNodeName() {
        Comment c = new Comment(SAMPLE_DATA);
        assertEquals("#comment", c.nodeName());
    }

    /*** outerHtmlHead() – prettyPrint = true ***/
    @Test
    public void testOuterHtmlHead_PrettyPrint() throws IOException {
        Comment c = new Comment(SAMPLE_DATA);
        StringWriter accum = new StringWriter();

        // depth = 2 → two indentation levels (2 * indentAmount spaces)
        c.outerHtmlHead(accum, 2, prettySettings);
        String expectedIndent = "        "; // 2 levels × 4 spaces
        String expected = expectedIndent + "<!--" + SAMPLE_DATA + "-->";
        assertEquals("outerHtmlHead should indent when prettyPrint is enabled", expected, accum.toString());
    }

    /*** outerHtmlHead() – prettyPrint = false ***/
    @Test
    public void testOuterHtmlHead_Compact() throws IOException {
        Comment c = new Comment(SPECIAL_DATA);
        StringWriter accum = new StringWriter();

        // depth is ignored when prettyPrint = false
        c.outerHtmlHead(accum, 5, compactSettings);
        String expected = "<!--" + SPECIAL_DATA + "-->";
        assertEquals("No indentation should be added when prettyPrint is disabled", expected, accum.toString());
    }

    /*** outerHtmlTail() – should be a no‑op ***/
    @Test
    public void testOuterHtmlTail_NoOp() throws IOException {
        Comment c = new Comment(SAMPLE_DATA);
        StringWriter accum = new StringWriter();
        c.outerHtmlTail(accum, 0, prettySettings);
        assertEquals("outerHtmlTail must not modify the Appendable", "", accum.toString());
    }

    /*** toString() – delegates to outerHtml() ***/
    @Test
    public void testToString_DelegatesToOuterHtml() {
        // outerHtml() is defined in the Node superclass; we rely on its contract:
        // it should call outerHtmlHead and outerHtmlTail.
        Comment c = new Comment(SAMPLE_DATA);
        // With prettyPrint = true and depth = 0 (default for outerHtml())
        String expected = "<!--" + SAMPLE_DATA + "-->";
        assertEquals("toString() should return the comment HTML", expected, c.toString());
    }

    /*** Error scenario – Appendable throws IOException ***/
    @Test
    public void testOuterHtmlHead_IOExceptionPropagation() throws IOException {
        Comment c = new Comment(SAMPLE_DATA);
        Appendable throwingAppendable = new Appendable() {
            @Override public Appendable append(CharSequence csq) throws IOException {
                throw new IOException("Simulated failure");
            }
            @Override public Appendable append(CharSequence csq, int start, int end) throws IOException {
                throw new IOException("Simulated failure");
            }
            @Override public Appendable append(char c) throws IOException {
                throw new IOException("Simulated failure");
            }
        };

        thrown.expect(IOException.class);
        thrown.expectMessage("Simulated failure");
        c.outerHtmlHead(throwingAppendable, 0, prettySettings);
    }

    /*** Edge case – comment data contains HTML‑like characters ***/
    @Test
    public void testCommentData_WithHtmlLikeCharacters() throws IOException {
        Comment c = new Comment(SPECIAL_DATA);
        StringWriter accum = new StringWriter();
        c.outerHtmlHead(accum, 0, compactSettings);
        // The data must be emitted verbatim; Jsoup does not escape comment contents.
        String expected = "<!--" + SPECIAL_DATA + "-->";
        assertEquals("Comment data containing <, >, &, quotes must be emitted unchanged", expected, accum.toString());
    }
}
```  
**###Test END##**