package org.jsoup.nodes;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;

/**
 * Comprehensive JUnit 4 test suite for {@link DataNode}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction (both constructors, including the deprecated one)</li>
 *   <li>nodeName()</li>
 *   <li>getWholeData() and setWholeData(String) – normal, edge and chaining cases</li>
 *   <li>outerHtmlHead(Appendable,int,Document.OutputSettings) – ensures data is written unescaped</li>
 *   <li>outerHtmlTail (no‑op) – just verifies it does not modify the Appendable</li>
 *   <li>toString() – should be equivalent to outerHtml()</li>
 *   <li>createFromEncoded(String,String) – normal and error scenarios</li>
 * </ul>
 *
 * The test class is deliberately self‑contained: it does not rely on any external
 * HTML parsing; only the public API of {@code DataNode} is exercised.
 */
public class DataNodeTest {

    private static final String SAMPLE_DATA = "Hello \"world\" & <test>";
    private static final String ENCODED_DATA = "Hello &quot;world&quot; &amp; &lt;test&gt;";

    private DataNode dataNode;

    @Before
    public void setUp() {
        dataNode = new DataNode(SAMPLE_DATA);
    }

    /*** Constructor and basic getter ***/
    @Test
    public void testConstructorAndGetWholeData() {
        assertEquals("Constructor must store the supplied data",
                SAMPLE_DATA, dataNode.getWholeData());

        // Edge case: null data – the class does not forbid it, so we expect a node that returns null
        DataNode nullNode = new DataNode((String) null);
        assertNull("DataNode should allow null data", nullNode.getWholeData());
    }

    /*** Deprecated two‑argument constructor – should behave like the single‑arg constructor ***/
    @Test
    public void testDeprecatedConstructor() {
        DataNode deprecated = new DataNode(SAMPLE_DATA, "http://example.com");
        assertEquals("Deprecated constructor must store the data unchanged",
                SAMPLE_DATA, deprecated.getWholeData());
    }

    /*** nodeName() must always return the constant "#data" ***/
    @Test
    public void testNodeName() {
        assertEquals("#data", dataNode.nodeName());
    }

    /*** setWholeData must replace the stored value and return the same instance for chaining ***/
    @Test
    public void testSetWholeDataAndChaining() {
        String newData = "New content";
        DataNode returned = dataNode.setWholeData(newData);
        assertSame("setWholeData should return the same instance for chaining", dataNode, returned);
        assertEquals("Data should be updated after setWholeData", newData, dataNode.getWholeData());

        // Edge case: set to null
        dataNode.setWholeData(null);
        assertNull("Data should become null after setting null", dataNode.getWholeData());
    }

    /*** outerHtmlHead must write the raw data (unescaped) to the Appendable ***/
    @Test
    public void testOuterHtmlHeadWritesUnescapedData() throws IOException {
        StringBuilder accum = new StringBuilder();
        dataNode.outerHtmlHead(accum, 0, new Document.OutputSettings());

        assertEquals("outerHtmlHead must write the raw (unescaped) data",
                SAMPLE_DATA, accum.toString());

        // Verify that characters that would normally be escaped are indeed *not* escaped
        DataNode special = new DataNode("a \"quote\" & amp");
        accum.setLength(0);
        special.outerHtmlHead(accum, 0, new Document.OutputSettings());
        assertEquals("a \"quote\" & amp", accum.toString());
    }

    /*** outerHtmlTail is a no‑op – it must not modify the Appendable ***/
    @Test
    public void testOuterHtmlTailNoOp() throws IOException {
        StringBuilder accum = new StringBuilder("prefix");
        dataNode.outerHtmlTail(accum, 0, new Document.OutputSettings());
        assertEquals("outerHtmlTail should not change the Appendable", "prefix", accum.toString());
    }

    /*** toString() should delegate to outerHtml(), which for a DataNode is just the data ***/
    @Test
    public void testToStringEqualsOuterHtml() {
        // outerHtml() is defined in Node; for DataNode it returns the same as getWholeData()
        // We can safely compare to getWholeData()
        assertEquals("toString must return the node's outer HTML (i.e., its data)",
                dataNode.getWholeData(), dataNode.toString());
    }

    /*** createFromEncoded – normal case ***/
    @Test
    public void testCreateFromEncoded() {
        DataNode created = DataNode.createFromEncoded(ENCODED_DATA, "http://ignored.com");
        assertEquals("Encoded entities must be unescaped",
                SAMPLE_DATA, created.getWholeData());

        // Ensure the returned node is a distinct instance
        assertNotSame("createFromEncoded must return a new instance", dataNode, created);
    }

    /*** createFromEncoded – null encoded data should throw NullPointerException ***/
    @Test(expected = NullPointerException.class)
    public void testCreateFromEncodedWithNull() {
        DataNode.createFromEncoded(null, "http://ignored.com");
    }

    /*** Additional sanity test: chaining multiple setWholeData calls ***/
    @Test
    public void testMultipleSetWholeDataCalls() {
        dataNode.setWholeData("first").setWholeData("second").setWholeData("third");
        assertEquals("third", dataNode.getWholeData());
    }
}
