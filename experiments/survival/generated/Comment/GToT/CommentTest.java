package org.jsoup.nodes;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Comprehensive JUnit‑4 test suite for {@link Comment}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods and constructors</li>
 *   <li>Typical use cases (normal strings, empty strings)</li>
 *   <li>Edge cases (null data, special characters)</li>
 *   <li>Behaviour of the package‑private {@code outerHtmlHead} and {@code outerHtmlTail}
 *       methods with different {@link Document.OutputSettings}</li>
 *   <li>Verification that no unexpected {@link IOException} is thrown</li>
 * </ul>
 */
public class CommentTest {

    /* --------------------------------------------------------------------- */
    /*  1.  Public constructors & basic getters                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void constructorWithNormalString_setsDataCorrectly() {
        String data = "This is a comment";
        Comment comment = new Comment(data);
        assertEquals("Getter should return the same data passed to the ctor", data, comment.getData());
    }

    @Test
    public void constructorWithEmptyString_setsDataCorrectly() {
        Comment comment = new Comment("");
        assertEquals("Empty string must be preserved", "", comment.getData());
    }

    @Test
    public void constructorWithNullString_allowsNullData() {
        Comment comment = new Comment(null);
        assertNull("getData() should return null when ctor receives null", comment.getData());
    }

    @Test
    public void deprecatedConstructorDelegatesToPrimaryCtor() {
        String data = "Deprecated ctor test";
        Comment comment = new Comment(data, "http://example.com");
        // Behaviour is identical to the primary ctor
        assertEquals(data, comment.getData());
    }

    /* --------------------------------------------------------------------- */
    /*  2.  nodeName()                                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void nodeName_returnsCommentTag() {
        Comment comment = new Comment("any");
        assertEquals("#comment", comment.nodeName());
    }

    /* --------------------------------------------------------------------- */
    /*  3.  toString() – which delegates to outerHtml()                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void toString_producesStandardHtmlComment() {
        String data = "sample";
        Comment comment = new Comment(data);
        assertEquals("<!--sample-->", comment.toString());
    }

    @Test
    public void toString_withNullData_producesNullLiteralInsideComment() {
        Comment comment = new Comment(null);
        // StringBuilder.append(null) yields the string "null"
        assertEquals("<!--null-->", comment.toString());
    }

    @Test
    public void toString_withSpecialCharacters_preservesContent() {
        String data = "a --> b <!-- c";
        Comment comment = new Comment(data);
        assertEquals("<!--a --> b <!-- c-->", comment.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  4.  Package‑private outerHtmlHead() – indentation & pretty‑print      */
    /* --------------------------------------------------------------------- */

    /**
     * Helper that uses reflection to invoke the package‑private {@code outerHtmlHead}
     * method.
     */
    private void invokeOuterHtmlHead(Comment comment, Appendable accum,
                                      int depth, Document.OutputSettings out) throws Exception {
        Method m = Comment.class.getDeclaredMethod(
                "outerHtmlHead", Appendable.class, int.class, Document.OutputSettings.class);
        m.setAccessible(true);
        m.invoke(comment, accum, depth, out);
    }

    @Test
    public void outerHtmlHead_withoutPrettyPrint_noIndentation() throws Exception {
        Comment comment = new Comment("no‑pretty");
        StringBuilder sb = new StringBuilder();

        Document.OutputSettings out = new Document.OutputSettings()
                .prettyPrint(false);          // disabling pretty‑print

        invokeOuterHtmlHead(comment, sb, 2, out);   // depth is irrelevant when prettyPrint = false

        assertEquals("<!--no‑pretty-->", sb.toString());
    }

    @Test
    public void outerHtmlHead_withPrettyPrint_addsIndentation() throws Exception {
        Comment comment = new Comment("pretty");
        StringBuilder sb = new StringBuilder();

        Document.OutputSettings out = new Document.OutputSettings()
                .prettyPrint(true)            // enable pretty‑print
                .indentAmount(4);             // default indent is 4 spaces

        // depth = 1 → one level of indentation (4 spaces)
        invokeOuterHtmlHead(comment, sb, 1, out);

        String expectedIndent = "    "; // four spaces
        assertEquals(expectedIndent + "<!--pretty-->", sb.toString());
    }

    @Test
    public void outerHtmlHead_indentRespectsCustomIndentAmount() throws Exception {
        Comment comment = new Comment("custom");
        StringBuilder sb = new StringBuilder();

        Document.OutputSettings out = new Document.OutputSettings()
                .prettyPrint(true)
                .indentAmount(2); // two spaces per level

        // depth = 3 → three levels → 6 spaces
        invokeOuterHtmlHead(comment, sb, 3, out);

        String expectedIndent = "      "; // six spaces
        assertEquals(expectedIndent + "<!--custom-->", sb.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  5.  outerHtmlTail() – should not modify the Appendable               */
    /* --------------------------------------------------------------------- */

    @Test
    public void outerHtmlTail_doesNotChangeAppendable() throws Exception {
        Comment comment = new Comment("tail‑test");
        StringBuilder sb = new StringBuilder("initial");

        Method m = Comment.class.getDeclaredMethod(
                "outerHtmlTail", Appendable.class, int.class, Document.OutputSettings.class);
        m.setAccessible(true);
        // invoke with any depth and any OutputSettings – they are ignored
        m.invoke(comment, sb, 0, new Document.OutputSettings());

        assertEquals("initial", sb.toString()); // unchanged
    }

    /* --------------------------------------------------------------------- */
    /*  6.  Verify that no IOException leaks out of the package‑private methods */
    /* --------------------------------------------------------------------- */

    @Test
    public void outerHtmlHead_doesNotThrowIOException() throws Exception {
        Comment comment = new Comment("io‑test");
        StringBuilder sb = new StringBuilder();

        Document.OutputSettings out = new Document.OutputSettings().prettyPrint(false);
        // The method declares IOException but StringBuilder never throws it.
        // The test simply ensures that the call completes normally.
        invokeOuterHtmlHead(comment, sb, 0, out);
        assertEquals("<!--io‑test-->", sb.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  7.  Additional sanity checks – equals / hashCode are inherited from */
    /*      Node – we only ensure they do not throw.                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void equalsAndHashCode_doNotThrow() {
        Comment c1 = new Comment("same");
        Comment c2 = new Comment("same");
        // The implementation is from the super‑class; we only verify it behaves
        // consistently for two identical objects.
        assertTrue(c1.equals(c2));
        assertEquals(c1.hashCode(), c2.hashCode());
    }
}
