**1. Public methods of `org.jsoup.nodes.Document`**

| # | Return type | Method signature |
|---|-------------|------------------|
| 1 | `Document` | `Document(String baseUri)` |
| 2 | `Document` | `static Document createShell(String baseUri)` |
| 3 | `String`   | `String location()` |
| 4 | `Element`  | `Element head()` |
| 5 | `Element`  | `Element body()` |
| 6 | `String`   | `String title()` |
| 7 | `void`     | `void title(String title)` |
| 8 | `Element`  | `Element createElement(String tagName)` |
| 9 | `Document` | `Document normalise()` |
|10 | `String`   | `String outerHtml()` |
|11 | `Element`  | `Element text(String text)` |
|12 | `String`   | `String nodeName()` |
|13 | `void`     | `void charset(Charset charset)` |
|14 | `Charset`  | `Charset charset()` |
|15 | `void`     | `void updateMetaCharsetElement(boolean update)` |
|16 | `boolean`  | `boolean updateMetaCharsetElement()` |
|17 | `Document` | `Document clone()` |
|18 | `OutputSettings` | `OutputSettings outputSettings()` |
|19 | `Document` | `Document outputSettings(OutputSettings outputSettings)` |
|20 | `QuirksMode` | `QuirksMode quirksMode()` |
|21 | `Document` | `Document quirksMode(QuirksMode quirksMode)` |

---

**2. Basic JUnit 4 test cases (one per method)**  

Below are simple “happy‑path” tests that verify the normal behaviour of each public method.

```java
@Test
public void testConstructorAndLocation() {
    Document doc = new Document("http://example.com/");
    assertEquals("http://example.com/", doc.location());
}

@Test
public void testCreateShell() {
    Document doc = Document.createShell("http://example.com/");
    assertNotNull(doc.head());
    assertNotNull(doc.body());
    assertEquals("head", doc.head().tagName());
    assertEquals("body", doc.body().tagName());
}

@Test
public void testHeadAndBodyAccessors() {
    Document doc = Document.createShell("http://example.com/");
    assertSame(doc.head(), doc.head());
    assertSame(doc.body(), doc.body());
}

@Test
public void testTitleGetterAndSetter() {
    Document doc = Document.createShell("http://example.com/");
    doc.title("  My   Title  ");               // set with extra spaces
    assertEquals("My Title", doc.title());    // normalised & trimmed
}

@Test
public void testCreateElement() {
    Document doc = new Document("http://example.com/");
    Element a = doc.createElement("a");
    assertEquals("a", a.tagName());
    assertEquals("http://example.com/", a.baseUri());
}

@Test
public void testNormaliseAddsMissingHtmlHeadBody() {
    Document doc = new Document("http://example.com/");
    doc.appendText(" stray ");
    doc.normalise();
    assertNotNull(doc.selectFirst("html"));
    assertNotNull(doc.head());
    assertNotNull(doc.body());
    assertTrue(doc.body().text().contains("stray"));
}

@Test
public void testOuterHtml() {
    Document doc = Document.createShell("http://example.com/");
    assertEquals(doc.html(), doc.outerHtml());
}

@Test
public void testTextOverridesBodyTextOnly() {
    Document doc = Document.createShell("http://example.com/");
    doc.text("Hello world");
    assertEquals("Hello world", doc.body().text());
    // the <html>, <head> elements must still be present
    assertNotNull(doc.selectFirst("html"));
    assertNotNull(doc.head());
}

@Test
public void testNodeName() {
    Document doc = new Document("http://example.com/");
    assertEquals("#document", doc.nodeName());
}

@Test
public void testCharsetGetterAndSetter() {
    Document doc = new Document("http://example.com/");
    Charset utf16 = Charset.forName("UTF-16");
    doc.charset(utf16);
    assertEquals(utf16, doc.charset());
}

@Test
public void testUpdateMetaCharsetElementFlag() {
    Document doc = Document.createShell("http://example.com/");
    doc.updateMetaCharsetElement(true);
    doc.charset(Charset.forName("ISO-8859-1"));
    Element meta = doc.selectFirst("meta[charset]");
    assertNotNull(meta);
    assertEquals("ISO-8859-1", meta.attr("charset"));
}

@Test
public void testCloneCopiesOutputSettings() {
    Document doc = Document.createShell("http://example.com/");
    doc.outputSettings().prettyPrint(false);
    Document copy = doc.clone();
    assertFalse(copy.outputSettings().prettyPrint());
    // modify original – clone must stay unchanged
    doc.outputSettings().prettyPrint(true);
    assertFalse(copy.outputSettings().prettyPrint());
}

@Test
public void testOutputSettingsGetterSetter() {
    Document doc = new Document("http://example.com/");
    Document.OutputSettings os = new Document.OutputSettings().prettyPrint(false);
    doc.outputSettings(os);
    assertSame(os, doc.outputSettings());
}

@Test
public void testQuirksModeGetterSetter() {
    Document doc = new Document("http://example.com/");
    doc.quirksMode(Document.QuirksMode.quirks);
    assertEquals(Document.QuirksMode.quirks, doc.quirksMode());
}
```

---

**3. Edge‑case & exception scenarios**

| Method | Edge case / exception | Reason |
|--------|----------------------|--------|
| `createShell(String)` | `baseUri == null` | `Validate.notNull` → `IllegalArgumentException` |
| `title(String)` | `title == null` | `Validate.notNull` → `NullPointerException` |
| `OutputSettings.indentAmount(int)` | negative value | `Validate.isTrue(indentAmount >= 0)` → `IllegalArgumentException` |
| `charset(Charset)` | `charset == null` **and** `updateMetaCharsetElement(true)` | later `charset().displayName()` would throw `NullPointerException` |
| `normalise()` | document contains **multiple** `<head>` or `<body>` elements – they must be merged into a single element |
| `charset(Charset)` with **XML** syntax | ensure an XML declaration is inserted/updated |
| `clone()` | verify deep copy – changes to original output settings must not affect the clone |
| `updateMetaCharsetElement(boolean)` | when set to `false`, calling `charset(..)` must **not** create a `<meta charset>` element |

---

**4. JUnit 4 tests for the identified edge cases**

```java
// 4.1  createShell(null) → IllegalArgumentException
@Test(expected = IllegalArgumentException.class)
public void testCreateShellNullBaseUri() {
    Document.createShell(null);
}

// 4.2  title(null) → NullPointerException
@Test(expected = NullPointerException.class)
public void testTitleNullArgument() {
    Document doc = Document.createShell("http://example.com/");
    doc.title(null);
}

// 4.3  indentAmount negative → IllegalArgumentException
@Test(expected = IllegalArgumentException.class)
public void testIndentAmountNegative() {
    Document.OutputSettings os = new Document.OutputSettings();
    os.indentAmount(-5);
}

// 4.4  charset(null) with meta‑update enabled → NullPointerException
@Test(expected = NullPointerException.class)
public void testCharsetNullWhenMetaUpdateEnabled() {
    Document doc = Document.createShell("http://example.com/");
    doc.updateMetaCharsetElement(true);
    doc.charset((Charset) null);   // NPE occurs inside ensureMetaCharsetElement()
}

// 4.5  normalise merges duplicate <head> and <body>
@Test
public void testNormaliseMergesDuplicateHeadAndBody() {
    Document doc = Document.createShell("http://example.com/");
    // add extra head and body elements manually
    doc.appendElement("head").appendElement("title").text("Extra");
    doc.appendElement("body").appendElement("p").text("Extra body");
    doc.normalise();

    Elements heads = doc.getElementsByTag("head");
    Elements bodies = doc.getElementsByTag("body");
    assertEquals(1, heads.size());
    assertEquals(1, bodies.size());

    // content from duplicates must have been merged
    assertTrue(heads.first().text().contains("Extra"));
    assertTrue(bodies.first().text().contains("Extra body"));
}

// 4.6  charset with XML syntax creates proper XML declaration
@Test
public void testCharsetWithXmlSyntaxAddsXmlDeclaration() {
    Document doc = Document.createShell("http://example.com/");
    doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
    doc.updateMetaCharsetElement(true);
    Charset utf16 = Charset.forName("UTF-16");
    doc.charset(utf16);

    Node first = doc.childNode(0);
    assertTrue(first instanceof XmlDeclaration);
    XmlDeclaration decl = (XmlDeclaration) first;
    assertEquals("xml", decl.name());
    assertEquals("UTF-16", decl.attr("encoding"));
    assertEquals("1.0", decl.attr("version"));
}

// 4.7  charset without meta‑update must NOT create <meta charset>
@Test
public void testCharsetWithoutMetaUpdateDoesNotAddMeta() {
    Document doc = Document.createShell("http://example.com/");
    doc.updateMetaCharsetElement(false);   // default is false, but set explicitly
    doc.charset(Charset.forName("ISO-8859-2"));
    assertNull(doc.selectFirst("meta[charset]"));
}

// 4.8  clone produces independent OutputSettings instance
@Test
public void testCloneIndependence() {
    Document original = Document.createShell("http://example.com/");
    original.outputSettings().prettyPrint(false);
    Document copy = original.clone();

    // modify original
    original.outputSettings().prettyPrint(true);
    // copy must retain its original setting
    assertFalse(copy.outputSettings().prettyPrint());
}
```

---

**5. Complete JUnit 4 test file (`DocumentTest.java`)**

```java
###Test START##
package org.jsoup.nodes;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Document.QuirksMode;
import org.jsoup.parser.XmlDeclaration;
import org.junit.Test;
import java.nio.charset.Charset;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit 4 tests for {@link Document}.
 * Covers normal behaviour, edge cases and exception handling.
 */
public class DocumentTest {

    /* -------------------------------------------------
       1. Basic “happy‑path” tests for every public method
       ------------------------------------------------- */

    @Test
    public void testConstructorAndLocation() {
        Document doc = new Document("http://example.com/");
        assertEquals("http://example.com/", doc.location());
    }

    @Test
    public void testCreateShell() {
        Document doc = Document.createShell("http://example.com/");
        assertNotNull(doc.head());
        assertNotNull(doc.body());
        assertEquals("head", doc.head().tagName());
        assertEquals("body", doc.body().tagName());
    }

    @Test
    public void testHeadAndBodyAccessors() {
        Document doc = Document.createShell("http://example.com/");
        assertSame(doc.head(), doc.head());
        assertSame(doc.body(), doc.body());
    }

    @Test
    public void testTitleGetterAndSetter() {
        Document doc = Document.createShell("http://example.com/");
        doc.title("  My   Title  ");               // set with extra spaces
        assertEquals("My Title", doc.title());    // normalised & trimmed
    }

    @Test
    public void testCreateElement() {
        Document doc = new Document("http://example.com/");
        Element a = doc.createElement("a");
        assertEquals("a", a.tagName());
        assertEquals("http://example.com/", a.baseUri());
    }

    @Test
    public void testNormaliseAddsMissingHtmlHeadBody() {
        Document doc = new Document("http://example.com/");
        doc.appendText(" stray ");
        doc.normalise();
        assertNotNull(doc.selectFirst("html"));
        assertNotNull(doc.head());
        assertNotNull(doc.body());
        assertTrue(doc.body().text().contains("stray"));
    }

    @Test
    public void testOuterHtml() {
        Document doc = Document.createShell("http://example.com/");
        assertEquals(doc.html(), doc.outerHtml());
    }

    @Test
    public void testTextOverridesBodyTextOnly() {
        Document doc = Document.createShell("http://example.com/");
        doc.text("Hello world");
        assertEquals("Hello world", doc.body().text());
        // ensure the document structure is still intact
        assertNotNull(doc.selectFirst("html"));
        assertNotNull(doc.head());
    }

    @Test
    public void testNodeName() {
        Document doc = new Document("http://example.com/");
        assertEquals("#document", doc.nodeName());
    }

    @Test
    public void testCharsetGetterAndSetter() {
        Document doc = new Document("http://example.com/");
        Charset utf16 = Charset.forName("UTF-16");
        doc.charset(utf16);
        assertEquals(utf16, doc.charset());
    }

    @Test
    public void testUpdateMetaCharsetElementFlag() {
        Document doc = Document.createShell("http://example.com/");
        doc.updateMetaCharsetElement(true);
        doc.charset(Charset.forName("ISO-8859-1"));
        Element meta = doc.selectFirst("meta[charset]");
        assertNotNull(meta);
        assertEquals("ISO-8859-1", meta.attr("charset"));
    }

    @Test
    public void testCloneCopiesOutputSettings() {
        Document doc = Document.createShell("http://example.com/");
        doc.outputSettings().prettyPrint(false);
        Document copy = doc.clone();
        assertFalse(copy.outputSettings().prettyPrint());

        // modify original – clone must stay unchanged
        doc.outputSettings().prettyPrint(true);
        assertFalse(copy.outputSettings().prettyPrint());
    }

    @Test
    public void testOutputSettingsGetterSetter() {
        Document doc = new Document("http://example.com/");
        Document.OutputSettings os = new Document.OutputSettings().prettyPrint(false);
        doc.outputSettings(os);
        assertSame(os, doc.outputSettings());
    }

    @Test
    public void testQuirksModeGetterSetter() {
        Document doc = new Document("http://example.com/");
        doc.quirksMode(Document.QuirksMode.quirks);
        assertEquals(Document.QuirksMode.quirks, doc.quirksMode());
    }

    /* -------------------------------------------------
       2. Edge‑case / exception handling tests
       ------------------------------------------------- */

    // 2.1 createShell(null) → IllegalArgumentException
    @Test(expected = IllegalArgumentException.class)
    public void testCreateShellNullBaseUri() {
        Document.createShell(null);
    }

    // 2.2 title(null) → NullPointerException
    @Test(expected = NullPointerException.class)
    public void testTitleNullArgument() {
        Document doc = Document.createShell("http://example.com/");
        doc.title(null);
    }

    // 2.3 indentAmount negative → IllegalArgumentException
    @Test(expected = IllegalArgumentException.class)
    public void testIndentAmountNegative() {
        Document.OutputSettings os = new Document.OutputSettings();
        os.indentAmount(-5);
    }

    // 2.4 charset(null) with meta‑update enabled → NullPointerException
    @Test(expected = NullPointerException.class)
    public void testCharsetNullWhenMetaUpdateEnabled() {
        Document doc = Document.createShell("http://example.com/");
        doc.updateMetaCharsetElement(true);
        doc.charset((Charset) null);   // NPE inside ensureMetaCharsetElement()
    }

    // 2.5 normalise merges duplicate <head> and <body>
    @Test
    public void testNormaliseMergesDuplicateHeadAndBody() {
        Document doc = Document.createShell("http://example.com/");
        // introduce duplicate head & body
        doc.appendElement("head").appendElement("title").text("Extra");
        doc.appendElement("body").appendElement("p").text("Extra body");
        doc.normalise();

        Elements heads = doc.getElementsByTag("head");
        Elements bodies = doc.getElementsByTag("body");
        assertEquals(1, heads.size());
        assertEquals(1, bodies.size());

        // content from the duplicates must have been merged into the originals
        assertTrue(heads.first().text().contains("Extra"));
        assertTrue(bodies.first().text().contains("Extra body"));
    }

    // 2.6 charset with XML syntax creates proper XML declaration
    @Test
    public void testCharsetWithXmlSyntaxAddsXmlDeclaration() {
        Document doc = Document.createShell("http://example.com/");
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        doc.updateMetaCharsetElement(true);
        Charset utf16 = Charset.forName("UTF-16");
        doc.charset(utf16);

        Node first = doc.childNode(0);
        assertTrue(first instanceof XmlDeclaration);
        XmlDeclaration decl = (XmlDeclaration) first;
        assertEquals("xml", decl.name());
        assertEquals("UTF-16", decl.attr("encoding"));
        assertEquals("1.0", decl.attr("version"));
    }

    // 2.7 charset without meta‑update must NOT create <meta charset>
    @Test
    public void testCharsetWithoutMetaUpdateDoesNotAddMeta() {
        Document doc = Document.createShell("http://example.com/");
        doc.updateMetaCharsetElement(false);   // explicitly disable
        doc.charset(Charset.forName("ISO-8859-2"));
        assertNull(doc.selectFirst("meta[charset]"));
    }

    // 2.8 clone produces independent OutputSettings instance
    @Test
    public void testCloneIndependence() {
        Document original = Document.createShell("http://example.com/");
        original.outputSettings().prettyPrint(false);
        Document copy = original.clone();

        // modify original after cloning
        original.outputSettings().prettyPrint(true);
        // copy must retain its original setting
        assertFalse(copy.outputSettings().prettyPrint());
    }
}
###Test END##
```