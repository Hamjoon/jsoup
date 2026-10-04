**Step 1 – List of public members**

| Visibility | Return type | Method / Constructor | Signature |
|-------------|-------------|----------------------|-----------|
|public|`DataNode(String data)`|Constructor|`DataNode(String data)`|
|public|`DataNode(String data, String baseUri)`|Deprecated Constructor|`DataNode(String data, String baseUri)`|
|public|`String`|`nodeName`|`String nodeName()`|
|public|`String`|`getWholeData`|`String getWholeData()`|
|public|`DataNode`|`setWholeData`|`DataNode setWholeData(String data)`|
|public|`String`|`toString`|`String toString()`|
|public|`static DataNode`|`createFromEncoded`|`static DataNode createFromEncoded(String encodedData, String baseUri)`|

*Note:* `outerHtmlHead`, `outerHtmlTail` are package‑private and are exercised indirectly via `toString()` / `outerHtml()`.

---

**Step 2 – Basic JUnit 4 test ideas (one per method)**  

| Method | Basic test description |
|--------|------------------------|
|`DataNode(String)` | Verify that the instance stores the supplied data and `getWholeData()` returns it. |
|`DataNode(String,String)` | Verify that the deprecated ctor delegates to the primary ctor (same result). |
|`nodeName()` | Returns the constant “#data”. |
|`getWholeData()` | Returns the current data (already covered by ctor test). |
|`setWholeData(String)` | Changes the internal data, returns the same instance, and `getWholeData()` reflects the new value. |
|`toString()` | Returns the raw data (because `outerHtml()` for a DataNode just appends the data). |
|`createFromEncoded(String,String)` | Un‑escapes HTML entities and creates a node containing the decoded text. |

---

**Step 3 – Edge‑case & exception scenarios**

| Scenario | Reasoning |
|----------|-----------|
|**Empty string**| Should be handled like any other string – no NPE, length 0.|
|**`null` data passed to constructor**| `LeafNode.coreValue` eventually stores the value; calling `toString()` (which triggers `outerHtmlHead`) will cause `Appendable.append(null)` → `NullPointerException`.|
|**`null` data passed to `setWholeData`**| Same NPE when later rendered.|
|**Very long string**| Ensure no truncation / overflow – use a 10 KB string.|
|**Encoded data containing multiple entities**| Verify full un‑escaping (e.g. `&lt;&amp;&gt;`).|
|**Deprecated constructor with `null` baseUri**| Base‑uri is unused – should still work.|
|**`createFromEncoded` with `null` encodedData**| `Entities.unescape(null)` returns `null`; the new node will contain `null` and later rendering will NPE – test that the constructor does not throw, but rendering does.|

---

**Step 4 – JUnit 4 test cases for the identified edge cases**

*Each test follows the “one test case per method” rule, but we also add dedicated edge‑case tests where appropriate.*

```java
// Basic test for constructor & getWholeData
@Test
public void constructorStoresData() {
    DataNode node = new DataNode("hello");
    assertEquals("hello", node.getWholeData());
}

// Deprecated ctor
@Test
public void deprecatedConstructorDelegates() {
    DataNode node = new DataNode("world", "http://example.com");
    assertEquals("world", node.getWholeData());
}

// nodeName()
@Test
public void nodeNameIsConstant() {
    DataNode node = new DataNode("x");
    assertEquals("#data", node.nodeName());
}

// setWholeData()
@Test
public void setWholeDataUpdatesValueAndReturnsSelf() {
    DataNode node = new DataNode("a");
    DataNode returned = node.setWholeData("b");
    assertSame(node, returned);
    assertEquals("b", node.getWholeData());
}

// toString()
@Test
public void toStringReturnsRawData() {
    DataNode node = new DataNode("<script>alert('x');</script>");
    assertEquals("<script>alert('x');</script>", node.toString());
}

// createFromEncoded()
@Test
public void createFromEncodedUnescapesEntities() {
    DataNode node = DataNode.createFromEncoded("&lt;div&gt;Hello &amp; Bye&lt;/div&gt;", "http://example.com");
    assertEquals("<div>Hello & Bye</div>", node.getWholeData());
}

// Edge case – empty string
@Test
public void emptyStringHandled() {
    DataNode node = new DataNode("");
    assertEquals("", node.getWholeData());
    assertEquals("", node.toString());
}

// Edge case – null data in constructor (rendering should NPE)
@Test(expected = NullPointerException.class)
public void nullDataInConstructorThrowsOnRender() {
    DataNode node = new DataNode(null);
    // toString() triggers outerHtmlHead which calls Appendable.append(null)
    node.toString();
}

// Edge case – null data in setWholeData (rendering should NPE)
@Test(expected = NullPointerException.class)
public void nullDataInSetterThrowsOnRender() {
    DataNode node = new DataNode("valid");
    node.setWholeData(null);
    node.toString();
}

// Edge case – very long string
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

// Edge case – createFromEncoded with multiple entities
@Test
public void createFromEncodedMultipleEntities() {
    DataNode node = DataNode.createFromEncoded("&lt;&amp;&gt;", "http://example.com");
    assertEquals("<&>", node.getWholeData());
}

// Edge case – createFromEncoded with null encodedData (rendering NPE later)
@Test(expected = NullPointerException.class)
public void createFromEncodedNullDataThrowsOnRender() {
    DataNode node = DataNode.createFromEncoded(null, "http://example.com");
    // node holds null internally; rendering will NPE
    node.toString();
}
```

---

**Step 5 – Merged, complete JUnit 4 test file**

```java
###Test START##
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
###Test END##
```