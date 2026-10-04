###Test START##
```java
package org.jsoup.nodes;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Field;

import org.junit.Test;

/**
 * Unit tests for {@link DataNode}.
 *
 * These tests cover:
 * <ul>
 *   <li>Constructors (including the deprecated one)</li>
 *   <li>{@link DataNode#nodeName()}</li>
 *   <li>Getter / setter for the whole data</li>
 *   <li>HTML output methods {@code outerHtmlHead} and {@code outerHtmlTail}</li>
 *   <li>{@link DataNode#toString()}</li>
 *   <li>Static factory {@link DataNode#createFromEncoded(String, String)}</li>
 * </ul>
 */
public class DataNodeTest {

    /** Helper to obtain a fresh {@link Document.OutputSettings} instance. */
    private Document.OutputSettings outputSettings() {
        // The Document constructor needs a base URI; an empty string is fine for tests.
        return new Document("").outputSettings();
    }

    @Test
    public void constructorSetsData() {
        DataNode node = new DataNode("sample data");
        assertEquals("sample data", node.getWholeData());
    }

    @Test
    public void constructorAcceptsEmptyString() {
        DataNode node = new DataNode("");
        assertEquals("", node.getWholeData());
    }

    @Test
    public void constructorAcceptsNull() {
        DataNode node = new DataNode(null);
        assertNull(node.getWholeData());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void deprecatedConstructorDelegatesToPrimary() {
        // The deprecated ctor should behave exactly like the primary one.
        DataNode node = new DataNode("deprecated data", "http://example.com");
        assertEquals("deprecated data", node.getWholeData());
    }

    @Test
    public void nodeNameIsConstant() {
        DataNode node = new DataNode("any");
        assertEquals("#data", node.nodeName());
    }

    @Test
    public void setWholeDataUpdatesValueAndReturnsSelf() {
        DataNode node = new DataNode("old");
        DataNode returned = node.setWholeData("new value");
        assertSame("setWholeData should return the same instance for chaining", node, returned);
        assertEquals("new value", node.getWholeData());
    }

    @Test
    public void setWholeDataAcceptsNull() {
        DataNode node = new DataNode("initial");
        node.setWholeData(null);
        assertNull(node.getWholeData());
    }

    @Test
    public void outerHtmlHeadWritesUnescapedData() throws IOException {
        String raw = "var s = \"<test> &amp; \"";
        DataNode node = new DataNode(raw);
        StringBuilder accum = new StringBuilder();

        node.outerHtmlHead(accum, 0, outputSettings());

        // The data should be written exactly as‑is, without any escaping.
        assertEquals(raw, accum.toString());
    }

    @Test
    public void outerHtmlTailDoesNotModifyAppendable() throws IOException {
        DataNode node = new DataNode("irrelevant");
        StringBuilder accum = new StringBuilder("prefix");
        node.outerHtmlTail(accum, 0, outputSettings());
        // outerHtmlTail is a no‑op, so the buffer must stay unchanged.
        assertEquals("prefix", accum.toString());
    }

    @Test
    public void toStringDelegatesToOuterHtml() {
        // In Jsoup, a DataNode's outerHtml is simply its data.
        String data = "script content \" with <tags> & entities";
        DataNode node = new DataNode(data);
        assertEquals(data, node.toString());
    }

    @Test
    public void createFromEncodedUnescapesEntities() {
        // Encoded string contains HTML entities that should be unescaped.
        String encoded = "&lt;div class=&quot;test&quot;&gt;Hello &amp; Bye&lt;/div&gt;";
        DataNode node = DataNode.createFromEncoded(encoded, "http://base");
        String expected = "<div class=\"test\">Hello & Bye</div>";
        assertEquals(expected, node.getWholeData());
    }

    @Test
    public void createFromEncodedHandlesEmptyString() {
        DataNode node = DataNode.createFromEncoded("", "http://base");
        assertEquals("", node.getWholeData());
    }

    @Test
    public void createFromEncodedHandlesNullEncodedString() {
        // The static method does not guard against null; we verify the behaviour.
        // Jsoup's Entities.unescape(null) returns null, so the DataNode should contain null.
        DataNode node = DataNode.createFromEncoded(null, "http://base");
        assertNull(node.getWholeData());
    }

    @Test
    public void dataPreservesNewlinesAndWhitespace() {
        String multiLine = "line1\n\t line2\r\nline3  ";
        DataNode node = new DataNode(multiLine);
        assertEquals(multiLine, node.getWholeData());

        // Ensure outerHtmlHead also preserves the whitespace.
        StringBuilder accum = new StringBuilder();
        try {
            node.outerHtmlHead(accum, 0, outputSettings());
        } catch (IOException e) {
            fail("IOException should never be thrown by outerHtmlHead in these tests");
        }
        assertEquals(multiLine, accum.toString());
    }

    /**
     * Verify that the internal value field of {@link LeafNode} (the superclass) is
     * correctly updated by {@code setWholeData}. This uses reflection only for
     * additional confidence; the public API already covers the contract.
     */
    @Test
    public void internalCoreValueMatchesPublicGetter() throws Exception {
        DataNode node = new DataNode("initial");
        node.setWholeData("changed");

        // Access the protected field `value` defined in Node/LeafNode.
        Field valueField = LeafNode.class.getDeclaredField("value");
        valueField.setAccessible(true);
        Object internal = valueField.get(node);

        assertEquals("changed", internal);
    }
}
```
###Test END##