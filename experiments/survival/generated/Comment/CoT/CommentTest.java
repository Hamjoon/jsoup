package org.jsoup.nodes;

import org.junit.Test;
import static org.junit.Assert.*;

public class CommentTest {

    /* --------------------------------------------------------------------
     *  Basic functionality tests
     * -------------------------------------------------------------------- */
    @Test
    public void constructorSetsData() {
        Comment c = new Comment("sample");
        assertEquals("sample", c.getData());
    }

    @Test
    public void deprecatedConstructorBehavesSame() {
        Comment c = new Comment("sample", "http://example.com");
        assertEquals("sample", c.getData());
    }

    @Test
    public void nodeNameIsConstant() {
        Comment c = new Comment("any");
        assertEquals("#comment", c.nodeName());
    }

    @Test
    public void toStringProducesHtmlComment() {
        Comment c = new Comment("hello world");
        assertEquals("<!--hello world-->", c.toString());
    }

    /* --------------------------------------------------------------------
     *  Edge‑case / exceptional behaviour tests
     * -------------------------------------------------------------------- */
    @Test
    public void emptyStringProducesEmptyComment() {
        Comment c = new Comment("");
        assertEquals("", c.getData());
        assertEquals("<!---->", c.toString());
    }

    @Test(expected = NullPointerException.class)
    public void toStringThrowsWhenDataIsNull() {
        Comment c = new Comment(null);
        // Rendering the comment should trigger NPE because Appendable.append(null)
        // is illegal.
        c.toString();
    }

    @Test
    public void dataContainingCommentCloseSequenceIsNotEscaped() {
        String payload = "a-->b";
        Comment c = new Comment(payload);
        assertEquals(payload, c.getData());
        assertEquals("<!--a-->b-->", c.toString());
    }

    @Test
    public void unicodeCharactersArePreserved() {
        String payload = "こんにちは 🌏";
        Comment c = new Comment(payload);
        assertEquals(payload, c.getData());
        assertEquals("<!--" + payload + "-->", c.toString());
    }

    @Test
    public void deprecatedConstructorAcceptsNullBaseUri() {
        Comment c = new Comment("test", null);
        assertEquals("test", c.getData());
    }
}
