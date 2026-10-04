###Test START##
```java
/**
 * JUnit 4 test suite for {@link org.jsoup.nodes.PseudoTextElement}.
 *
 * The class under test is a very thin subclass of {@link org.jsoup.nodes.Element}
 * that overrides {@code outerHtmlHead} and {@code outerHtmlTail} with empty
 * implementations.  The tests therefore focus on:
 *
 * <ul>
 *   <li>Correct construction – the supplied {@link org.jsoup.parser.Tag},
 *       base URI and {@link org.jsoup.nodes.Attributes} are stored and exposed
 *       via the inherited {@code tagName()}, {@code baseUri()} and
 *       {@code attributes()} methods.</li>
 *   <li>The overridden {@code outerHtmlHead} and {@code outerHtmlTail}
 *       methods do not modify the provided {@link Appendable} and do not
 *       throw any {@link IOException}.</li>
 *   <li>Basic inheritance contracts – the instance is an {@code Element}
 *       and behaves like a regular {@code Element} for methods that are not
 *       overridden.</li>
 * </ul>
 *
 * The tests are deliberately exhaustive for the tiny public surface of the
 * class, covering normal usage as well as edge‑cases such as {@code null}
 * arguments where the superclass is expected to throw {@link NullPointerException}.
 *
 * @author  OpenAI ChatGPT
 * @since   2026‑10‑04
 */
package org.jsoup.nodes;

import org.jsoup.parser.Tag;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class PseudoTextElementTest {

    private static final String TAG_NAME = "span";
    private static final String BASE_URI = "http://example.com/";
    private Tag tag;
    private Attributes attrs;
    private PseudoTextElement element;

    @Before
    public void setUp() {
        // Create a Tag instance via the public factory method.
        tag = Tag.valueOf(TAG_NAME);
        // Initialise a fresh Attributes container and add a dummy attribute.
        attrs = new Attributes();
        attrs.put("data-test", "value");
        // Construct the element under test.
        element = new PseudoTextElement(tag, BASE_URI, attrs);
    }

    /**
     * Verify that the constructor correctly stores the tag, base URI and attributes.
     */
    @Test
    public void testConstructorInitialisesFields() {
        // Tag name should be exactly the one supplied.
        assertEquals("Tag name mismatch", TAG_NAME, element.tagName());

        // Base URI should be preserved.
        assertEquals("Base URI mismatch", BASE_URI, element.baseUri());

        // Attributes should be the same instance (the Element constructor copies the reference).
        assertSame("Attributes instance should be the same", attrs, element.attributes());

        // The attribute we added must be present.
        assertTrue("Attribute 'data-test' missing", element.hasAttr("data-test"));
        assertEquals("Attribute value mismatch", "value", element.attr("data-test"));
    }

    /**
     * The {@code outerHtmlHead} implementation is empty – calling it must not change
     * the supplied {@link Appendable} and must not throw an {@link IOException}.
     */
    @Test
    public void testOuterHtmlHeadDoesNothing() throws IOException {
        StringBuilder accum = new StringBuilder("initial");
        Document.OutputSettings out = new Document("").outputSettings();

        // The method should complete silently.
        element.outerHtmlHead(accum, 0, out);

        // The accumulator must be unchanged.
        assertEquals("outerHtmlHead should not modify the Appendable", "initial", accum.toString());
    }

    /**
     * The {@code outerHtmlTail} implementation is empty – calling it must not change
     * the supplied {@link Appendable} and must not throw an {@link IOException}.
     */
    @Test
    public void testOuterHtmlTailDoesNothing() throws IOException {
        StringBuilder accum = new StringBuilder("tail");
        Document.OutputSettings out = new Document("").outputSettings();

        element.outerHtmlTail(accum, 0, out);
        assertEquals("outerHtmlTail should not modify the Appendable", "tail", accum.toString());
    }

    /**
     * Ensure that calling the empty methods with a {@code null} Appendable
     * results in a {@link NullPointerException} from the underlying {@code Appendable}
     * contract (the method itself does not guard against null).
     */
    @Test(expected = NullPointerException.class)
    public void testOuterHtmlHeadWithNullAppendable() throws IOException {
        Document.OutputSettings out = new Document("").outputSettings();
        element.outerHtmlHead(null, 0, out);
    }

    @Test(expected = NullPointerException.class)
    public void testOuterHtmlTailWithNullAppendable() throws IOException {
        Document.OutputSettings out = new Document("").outputSettings();
        element.outerHtmlTail(null, 0, out);
    }

    /**
     * Verify that the element behaves like a normal {@link Element} for methods
     * that are not overridden.  For example, {@code outerHtml()} should render an
     * empty tag because the element has no children and the head/tail methods are
     * no‑ops.
     */
    @Test
    public void testOuterHtmlRenderingIsEmptyTag() {
        // The element has no child nodes; its HTML representation should be an empty tag.
        String expected = "<span></span>";
        assertEquals("Rendered outer HTML does not match expected empty tag", expected, element.outerHtml());
    }

    /**
     * Attempt to construct a {@code PseudoTextElement} with {@code null} arguments.
     * The superclass {@link Element} validates its inputs and throws
     * {@link NullPointerException} for a null {@link Tag} or {@code null} attributes.
     */
    @Test(expected = NullPointerException.class)
    public void testConstructorNullTagThrows() {
        new PseudoTextElement(null, BASE_URI, attrs);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullAttributesThrows() {
        new PseudoTextElement(tag, BASE_URI, null);
    }

    /**
     * The element should be an instance of {@link Element} and also of its
     * super‑classes up to {@link Node}.
     */
    @Test
    public void testInheritanceHierarchy() {
        assertTrue("Should be an instance of Element", element instanceof Element);
        assertTrue("Should be an instance of Node", element instanceof Node);
    }
}
```
###Test END##