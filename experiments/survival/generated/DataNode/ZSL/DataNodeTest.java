package org.jsoup.nodes;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link DataNode}.
 *
 * These tests cover:
 * <ul>
 *   <li>Both constructors (including the deprecated one)</li>
 *   <li>nodeName()</li>
 *   <li>getWholeData() / setWholeData()</li>
 *   <li>outerHtmlHead() and outerHtmlTail()</li>
 *   <li>toString()</li>
 *   <li>createFromEncoded()</li>
 *   <li>Behaviour with {@code null} data</li>
 * </ul>
 *
 * The tests assume that the jsoup library (including {@link Entities}
 * and {@link Document.OutputSettings}) is on the test classpath.
 */
public class DataNodeTest {

    private DataNode simpleNode;
    private DataNode encodedNode;

    @Before
    public void setUp() {
        simpleNode = new DataNode("initial data");
        encodedNode = DataNode.createFromEncoded("a &amp; b &lt; c", "http://example.com");
    }

    /** Test the primary constructor and getter. */
    @Test
    public void constructorAndGetWholeData() {
        DataNode node = new DataNode("hello world");
        assertEquals("hello world", node.getWholeData());
    }

    /** Test the deprecated constructor – it should delegate to the primary one. */
    @Test
    public void deprecatedConstructorBehavesSame() {
        DataNode node = new DataNode("some data", "http://unused-base-uri");
        assertEquals("some data", node.getWholeData());
    }

    /** nodeName() must always return the literal "#data". */
    @Test
    public void nodeNameIsConstant() {
        assertEquals("#data", simpleNode.nodeName());
    }

    /** setWholeData should mutate the node and return the same instance for chaining. */
    @Test
    public void setWholeDataChainsAndUpdates() {
        DataNode node = new DataNode("old");
        DataNode returned = node.setWholeData("new value");
        assertSame("setWholeData should return the same instance", node, returned);
        assertEquals("new value", node.getWholeData());
    }

    /** outerHtmlHead should append the raw (unescaped) data to the Appendable. */
    @Test
    public void outerHtmlHeadAppendsRawData() throws IOException {
        DataNode node = new DataNode("raw \"data\" with <tags>");
        StringBuilder accum = new StringBuilder();

        // Document.OutputSettings is a simple holder – default settings are fine for this test.
        Document.OutputSettings out = new Document.OutputSettings();

        node.outerHtmlHead(accum, 0, out);
        assertEquals("raw \"data\" with <tags>", accum.toString());
    }

    /** outerHtmlTail is a no‑op; calling it must not modify the Appendable. */
    @Test
    public void outerHtmlTailDoesNothing() throws IOException {
        DataNode node = new DataNode("anything");
        StringBuilder accum = new StringBuilder("prefix");
        Document.OutputSettings out = new Document.OutputSettings();

        node.outerHtmlTail(accum, 0, out);
        assertEquals("prefix", accum.toString());
    }

    /** toString() should delegate to outerHtml(), which for a DataNode is just its data. */
    @Test
    public void toStringReturnsOuterHtml() {
        DataNode node = new DataNode("some content");
        // For a DataNode outerHtml() is defined in the superclass Node as:
        //   outerHtmlHead + outerHtmlTail
        // Since outerHtmlTail does nothing, the result equals the raw data.
        assertEquals(node.getWholeData(), node.toString());
    }

    /** createFromEncoded must unescape HTML entities. */
    @Test
    public void createFromEncodedUnescapesEntities() {
        DataNode node = DataNode.createFromEncoded("x &amp; y &lt; z", "http://base");
        assertEquals("x & y < z", node.getWholeData());
    }

    /** Verify that the node created in @Before via createFromEncoded behaves as expected. */
    @Test
    public void encodedNodeHasCorrectData() {
        assertEquals("a & b < c", encodedNode.getWholeData());
    }

    /** Null data should be handled gracefully – getters return null, setters work. */
    @Test
    public void nullDataHandling() {
        DataNode nullNode = new DataNode((String) null);
        assertNull("getWholeData should return null when constructed with null", nullNode.getWholeData());

        nullNode.setWholeData("now not null");
        assertEquals("now not null", nullNode.getWholeData());
    }

    /** setWholeData should accept empty strings and preserve them. */
    @Test
    public void setWholeDataEmptyString() {
        DataNode node = new DataNode("non‑empty");
        node.setWholeData("");
        assertEquals("", node.getWholeData());
        assertEquals("", node.toString());
    }
}
