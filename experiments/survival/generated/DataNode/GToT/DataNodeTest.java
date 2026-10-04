package org.jsoup.nodes;

import org.junit.Test;
import static org.junit.Assert.*;

public class DataNodeTest {

    // -----------------------------------------------------------------
    //  Basic functionality tests
    // -----------------------------------------------------------------

    @Test
    public void constructorStoresData() {
        DataNode node = new DataNode("hello");
        assertEquals("hello", node.getWholeData());
    }

    @Test
    public void deprecatedConstructorDelegates() {
        DataNode node = new DataNode("world", "http://example.com");
        assertEquals("world", node.getWholeData());
    }

    @Test
    public void nodeNameIsConstant() {
        DataNode node = new DataNode("x");
        assertEquals("#data", node.nodeName());
    }

    @Test
    public void setWholeDataUpdatesValueAndReturnsSelf() {
        DataNode node = new DataNode("a");
        DataNode returned = node.setWholeData("b");
        assertSame(node, returned);
        assertEquals("b", node.getWholeData());
    }

    @Test
    public void toStringReturnsRawData() {
        DataNode node = new DataNode("<script>alert('x');</script>");
        assertEquals("<script>alert('x');</script>", node.toString());
    }

    @Test
    public void createFromEncodedUnescapesEntities() {
        DataNode node = DataNode.createFromEncoded("&lt;div&gt;Hello &amp; Bye&lt;/div&gt;", "http://example.com");
        assertEquals("<div>Hello & Bye</div>", node.getWholeData());
    }

    // -----------------------------------------------------------------
    //  Edge‑case / error handling tests
    // -----------------------------------------------------------------

    @Test
    public void emptyStringHandled() {
        DataNode node = new DataNode("");
        assertEquals("", node.getWholeData());
        assertEquals("", node.toString());
    }

    @Test(expected = NullPointerException.class)
    public void nullDataInConstructorThrowsOnRender() {
        DataNode node = new DataNode(null);
        // Rendering triggers Appendable.append(null) → NPE
        node.toString();
    }

    @Test(expected = NullPointerException.class)
    public void nullDataInSetterThrowsOnRender() {
        DataNode node = new DataNode("valid");
        node.setWholeData(null);
        node.toString();
    }

    @Test
    public void longStringIsPreserved() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append('a');
        }
        String longStr = sb.toString();
        DataNode node = new DataNode(longStr);
        assertEquals(longStr, node.getWholeData());
        assertEquals(longStr, node.toString());
    }

    @Test
    public void createFromEncodedMultipleEntities() {
        DataNode node = DataNode.createFromEncoded("&lt;&amp;&gt;", "http://example.com");
        assertEquals("<&>", node.getWholeData());
    }

    @Test(expected = NullPointerException.class)
    public void createFromEncodedNullDataThrowsOnRender() {
        DataNode node = DataNode.createFromEncoded(null, "http://example.com");
        // The internal value is null; rendering will NPE
        node.toString();
    }
}
