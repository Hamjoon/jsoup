package org.jsoup.nodes;

import org.jsoup.parser.Tag;
import org.jsoup.parser.Parser;
import org.junit.Test;
import org.junit.Before;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link PseudoTextElement}.
 *
 * <p>The class under test is a thin subclass of {@link Element} that overrides
 * {@code outerHtmlHead} and {@code outerHtmlTail} with no‑op implementations.
 * The tests therefore focus on:</p>
 * <ul>
 *   <li>Correct construction of the object (tag, base URI, attributes).</li>
 *   <li>Ensuring the overridden methods do not modify the {@link Appendable}
 *       passed to them.</li>
 *   <li>Verifying that the overall {@code outerHtml()} result is an empty
 *       string because both head and tail are suppressed.</li>
 *   <li>Behaviour when {@code null} arguments are supplied to the constructor.</li>
 * </ul>
 */
public class PseudoTextElementTest {

    private static final String TAG_NAME = "p";
    private static final String BASE_URI = "http://example.com/";
    private Tag tag;
    private Attributes attributes;
    private PseudoTextElement element;

    @Before
    public void setUp() {
        tag = Tag.valueOf(TAG_NAME);
        attributes = new Attributes();
        attributes.put("class", "test");
        attributes.put("id", "elem1");
        element = new PseudoTextElement(tag, BASE_URI, attributes);
    }

    /** 
     * Verify that the constructor correctly stores the tag, base URI and attributes.
     */
    @Test
    public void testConstructorInitialisesFields() {
        assertEquals(TAG_NAME, element.tagName());
        assertEquals(BASE_URI, element.baseUri());
        assertEquals("test", element.attr("class"));
        assertEquals("elem1", element.id());
        // The element should have no child nodes (as a pseudo‑text node).
        assertTrue(element.childNodeSize() == 0);
    }

    /**
     * The {@code outerHtmlHead} implementation is a no‑op. Calling it must not
     * change the supplied {@link Appendable}.
     */
    @Test
    public void testOuterHtmlHeadDoesNothing() throws IOException {
        StringBuilder sb = new StringBuilder("initial");
        element.outerHtmlHead(sb, 0, new Document("").outputSettings());
        assertEquals("initial", sb.toString());
    }

    /**
     * The {@code outerHtmlTail} implementation is a no‑op. Calling it must not
     * change the supplied {@link Appendable}.
     */
    @Test
    public void testOuterHtmlTailDoesNothing() throws IOException {
        StringBuilder sb = new StringBuilder("tail");
        element.outerHtmlTail(sb, 0, new Document("").outputSettings());
        assertEquals("tail", sb.toString());
    }

    /**
     * Because both {@code outerHtmlHead} and {@code outerHtmlTail} are empty,
     * the full {@code outerHtml()} of a {@link PseudoTextElement} should be an
     * empty string.
     */
    @Test
    public void testOuterHtmlIsEmpty() {
        String html = element.outerHtml();
        assertEquals("", html);
    }

    /**
     * The superclass {@link Element} does not allow a {@code null} tag.
     * Passing {@code null} for the tag should result in a {@link IllegalArgumentException}
     * (the exact exception type is implementation dependent, but we expect an NPE or IAE).
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullTagThrows() {
        new PseudoTextElement(null, BASE_URI, attributes);
    }

    /**
     * Passing {@code null} for the attributes map is allowed by {@link Element}
     * (it creates an empty {@link Attributes} instance). The test confirms that
     * the element is still functional and its {@code outerHtml()} remains empty.
     */
    @Test
    public void testConstructorNullAttributes() {
        PseudoTextElement elem = new PseudoTextElement(tag, BASE_URI, null);
        assertNotNull(elem.attributes());
        assertTrue(elem.attributes().size() == 0);
        assertEquals("", elem.outerHtml());
    }

    /**
     * Verify that {@code outerHtmlHead} and {@code outerHtmlTail} gracefully
     * handle an {@link Appendable} that throws {@link IOException}. The
     * exception should propagate unchanged.
     */
    @Test
    public void testOuterHtmlHeadIOExceptionPropagation() {
        Appendable throwingAppendable = new Appendable() {
            @Override
            public Appendable append(CharSequence csq) throws IOException {
                throw new IOException("forced");
            }

            @Override
            public Appendable append(CharSequence csq, int start, int end) throws IOException {
                throw new IOException("forced");
            }

            @Override
            public Appendable append(char c) throws IOException {
                throw new IOException("forced");
            }
        };

        try {
            element.outerHtmlHead(throwingAppendable, 0, new Document("").outputSettings());
            fail("Expected IOException to be thrown");
        } catch (IOException e) {
            assertEquals("forced", e.getMessage());
        }
    }

    /**
     * Same propagation test for {@code outerHtmlTail}.
     */
    @Test
    public void testOuterHtmlTailIOExceptionPropagation() {
        Appendable throwingAppendable = new Appendable() {
            @Override
            public Appendable append(CharSequence csq) throws IOException {
                throw new IOException("forced");
            }

            @Override
            public Appendable append(CharSequence csq, int start, int end) throws IOException {
                throw new IOException("forced");
            }

            @Override
            public Appendable append(char c) throws IOException {
                throw new IOException("forced");
            }
        };

        try {
            element.outerHtmlTail(throwingAppendable, 0, new Document("").outputSettings());
            fail("Expected IOException to be thrown");
        } catch (IOException e) {
            assertEquals("forced", e.getMessage());
        }
    }

}
