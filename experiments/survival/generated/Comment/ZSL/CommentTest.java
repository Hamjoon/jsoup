package org.jsoup.nodes;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * JUnit4 test suite for {@link Comment}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Both constructors (including the deprecated one)</li>
 *   <li>nodeName()</li>
 *   <li>getData()</li>
 *   <li>outerHtmlHead() with pretty‑print on and off</li>
 *   <li>outerHtmlTail() (no‑op)</li>
 *   <li>toString() which delegates to {@code outerHtml()}</li>
 * </ul>
 * <p>
 * Because {@code outerHtmlHead} and {@code outerHtmlTail} are package‑private,
 * the test class resides in the same package {@code org.jsoup.nodes}.
 * A minimal stub for {@code Document.OutputSettings} is provided inside the test
 * file – only the {@code prettyPrint()} method is required for the current
 * implementation.
 */
public class CommentTest {

    /** Simple stub mimicking the parts of {@link Document.OutputSettings} used by {@link Comment}. */
    private static class OutputSettingsStub extends Document.OutputSettings {
        private final boolean prettyPrint;

        OutputSettingsStub(boolean prettyPrint) {
            this.prettyPrint = prettyPrint;
        }

        @Override
        public boolean prettyPrint() {
            return prettyPrint;
        }
    }

    private Comment comment;
    private StringBuilder sb;

    @Before
    public void setUp() {
        // a fresh comment and StringBuilder for each test
        comment = new Comment("initial data");
        sb = new StringBuilder();
    }

    /** Verify that the primary constructor stores the data correctly. */
    @Test
    public void testPrimaryConstructorAndGetData() {
        assertEquals("initial data", comment.getData());
        assertEquals("initial data", comment.coreValue()); // coreValue is protected in LeafNode
    }

    /** Verify that the deprecated two‑argument constructor behaves identically to the primary one. */
    @Test
    public void testDeprecatedConstructor() {
        Comment deprecated = new Comment("deprecated data", "http://example.com");
        assertEquals("deprecated data", deprecated.getData());
        assertEquals("#comment", deprecated.nodeName());
    }

    /** nodeName() must always return the constant "#comment". */
    @Test
    public void testNodeName() {
        assertEquals("#comment", comment.nodeName());
    }

    /** outerHtmlHead should produce the correct comment markup when pretty‑print is disabled. */
    @Test
    public void testOuterHtmlHeadWithoutPrettyPrint() throws IOException {
        OutputSettingsStub settings = new OutputSettingsStub(false);
        comment.outerHtmlHead(sb, 0, settings);
        assertEquals("<!--initial data-->", sb.toString());
    }

    /** outerHtmlHead should honour pretty‑print flag – when enabled it should prepend indentation. */
    @Test
    public void testOuterHtmlHeadWithPrettyPrint() throws IOException {
        // depth 2 with prettyPrint enabled – the indent implementation in the real library
        // adds two * (depth) spaces per level. Since we don't have the real indent logic,
        // we only verify that the method does *not* throw and that the comment text is present.
        OutputSettingsStub settings = new OutputSettingsStub(true);
        comment.outerHtmlHead(sb, 2, settings);
        String result = sb.toString();

        // The exact indentation depends on the hidden `indent` method; we assert the suffix.
        assertTrue("Result should end with the comment markup", result.endsWith("<!--initial data-->"));
    }

    /** outerHtmlTail is a no‑op; invoking it must not modify the Appendable. */
    @Test
    public void testOuterHtmlTailDoesNothing() throws IOException {
        OutputSettingsStub settings = new OutputSettingsStub(false);
        comment.outerHtmlTail(sb, 0, settings);
        assertEquals("", sb.toString());
    }

    /** toString() should delegate to outerHtml() which for a leaf comment yields the same as outerHtmlHead. */
    @Test
    public void testToStringProducesCommentMarkup() {
        // The real outerHtml implementation concatenates head and tail; for a leaf node tail is empty.
        assertEquals("<!--initial data-->", comment.toString());
    }

    /** Passing {@code null} as comment data should be handled gracefully (no NPE inside Comment). */
    @Test
    public void testNullDataHandling() {
        Comment nullComment = new Comment(null);
        assertNull(nullComment.getData());

        // When rendered, StringBuilder.append(null) yields the literal "null".
        OutputSettingsStub settings = new OutputSettingsStub(false);
        StringBuilder localSb = new StringBuilder();
        try {
            nullComment.outerHtmlHead(localSb, 0, settings);
        } catch (IOException e) {
            fail("outerHtmlHead should not throw IOException for null data");
        }
        assertEquals("<!--null-->", localSb.toString());

        // toString() should reflect the same rendering.
        assertEquals("<!--null-->", nullComment.toString());
    }
}
