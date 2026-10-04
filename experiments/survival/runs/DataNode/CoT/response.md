###Test START##
```java
package org.jsoup.nodes;

import org.jsoup.nodes.Entities;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link DataNode}.
 *
 * The tests are generated to cover:
 * <ul>
 *   <li>All public methods and constructors</li>
 *   <li>Normal functional behaviour</li>
 *   <li>Edge‑case handling such as {@code null} and empty strings</li>
 *   <li>Exception scenarios (e.g. {@code NullPointerException}) where the contract is ambiguous</li>
 * </ul>
 */
public class DataNodeTest {

    /* --------------------------------------------------------------------- */
    /*  1.  List of public methods (including constructors) extracted from     */
    /*      the source code.                                                */
    /* --------------------------------------------------------------------- */
    // public DataNode(String data)
    // public DataNode(String data, String baseUri)               // deprecated
    // public String nodeName()
    // public String getWholeData()
    // public DataNode setWholeData(String data)
    // public static DataNode createFromEncoded(String encodedData, String baseUri)
    // public String toString()                                   // overrides Object

    /* --------------------------------------------------------------------- */
    /*  2.  Basic functionality tests                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void constructor_WithValidString_ShouldStoreData() {
        String src = "sample data";
        DataNode node = new DataNode(src);
        assertEquals("Data stored by constructor must be retrievable via getWholeData()", src, node.getWholeData());
    }

    @Test
    public void constructor_WithBaseUri_DeprecatedConstructorBehavesSameAsPrimary() {
        String src = "deprecated ctor data";
        String baseUri = "http://example.com";
        DataNode node = new DataNode(src, baseUri);
        assertEquals("Deprecated constructor must delegate to primary constructor", src, node.getWholeData());
    }

    @Test
    public void nodeName_ShouldReturnConstant() {
        DataNode node = new DataNode("any");
        assertEquals("#data", node.nodeName());
    }

    @Test
    public void setWholeData_ShouldReplaceExistingDataAndReturnSelf() {
        DataNode node = new DataNode("old");
        DataNode returned = node.setWholeData("new value");
        assertSame("setWholeData must return the same instance for chaining", node, returned);
        assertEquals("Data must be replaced by setWholeData()", "new value", node.getWholeData());
    }

    @Test
    public void toString_ShouldReturnSameAsOuterHtml_WhichIsDataContent() {
        // In jsoup, outerHtml() for a DataNode simply returns its data.
        String payload = "script <b>bold</b> & \"quotes\"";
        DataNode node = new DataNode(payload);
        assertEquals("toString must equal the raw data content", payload, node.toString());
    }

    @Test
    public void createFromEncoded_ShouldUnescapeHtmlEntities() {
        String encoded = "a &lt; b &amp; c &gt; d";
        String baseUri = "http://example.org";
        DataNode node = DataNode.createFromEncoded(encoded, baseUri);
        assertEquals("Unescaped data must match expected plain text", "a < b & c > d", node.getWholeData());
    }

    /* --------------------------------------------------------------------- */
    /*  3.  Edge‑case and exception handling tests                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void constructor_WithEmptyString_ShouldStoreEmptyString() {
        DataNode node = new DataNode("");
        assertEquals("Empty string must be stored unchanged", "", node.getWholeData());
    }

    @Test
    public void constructor_WithNull_ShouldStoreNull() {
        // jsoup's LeafNode stores the value directly; it allows null.
        DataNode node = new DataNode((String) null);
        assertNull("Constructor with null should result in getWholeData() returning null", node.getWholeData());
    }

    @Test(expected = NullPointerException.class)
    public void setWholeData_WithNull_ShouldThrowNPE() {
        // The underlying coreValue(String) in jsoup does not guard against null,
        // so a NullPointerException is the most likely outcome.
        DataNode node = new DataNode("initial");
        node.setWholeData(null);
    }

    @Test
    public void createFromEncoded_WithEmptyString_ShouldReturnEmptyDataNode() {
        DataNode node = DataNode.createFromEncoded("", "http://example.com");
        assertEquals("Empty encoded string must result in empty data node", "", node.getWholeData());
    }

    @Test
    public void createFromEncoded_WithNullEncodedData_ShouldReturnNodeWithNullData() {
        // Entities.unescape(null) returns null in jsoup, so the DataNode will contain null.
        DataNode node = DataNode.createFromEncoded(null, "http://example.com");
        assertNull("Encoded null should lead to DataNode holding null data", node.getWholeData());
    }

    @Test
    public void createFromEncoded_WithComplexEntities_ShouldUnescapeAll() {
        String encoded = "&lt;div class=&quot;test&quot;&gt;Hello&nbsp;World!&lt;/div&gt;";
        DataNode node = DataNode.createFromEncoded(encoded, "http://example.com");
        assertEquals("<div class=\"test\">Hello World!</div>", node.getWholeData());
    }

    @Test
    public void setWholeData_ChainingMultipleCalls_ShouldMaintainLatestValue() {
        DataNode node = new DataNode("first");
        node.setWholeData("second").setWholeData("third");
        assertEquals("latest data must be the one returned by getWholeData()", "third", node.getWholeData());
    }
}
```
###Test END##