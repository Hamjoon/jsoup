**Step 1 – List of all public members**

| Visibility | Return type | Method / Constructor | Parameters |
|------------|-------------|----------------------|------------|
| `public` | `DocumentType` | `DocumentType(String name, String publicId, String systemId)` | `String name, String publicId, String systemId` |
| `public` | `DocumentType` | `DocumentType(String name, String publicId, String systemId, String baseUri)` *(deprecated)* | `String name, String publicId, String systemId, String baseUri` |
| `public` | `DocumentType` | `DocumentType(String name, String pubSysKey, String publicId, String systemId, String baseUri)` *(deprecated)* | `String name, String pubSysKey, String publicId, String systemId, String baseUri` |
| `public` | `void` | `setPubSysKey(String value)` | `String value` |
| `public` | `String` | `nodeName()` | – |
| `public` | *(inherited)* | `attr(String key)` – used in the tests to read stored attributes | – |

*Package‑private* methods (`outerHtmlHead`, `outerHtmlTail`, `has`) are also testable because the test class lives in the same package (`org.jsoup.nodes`).

---

**Step 2 – Basic test idea for each public member**

| Member | Basic test description |
|--------|------------------------|
| `DocumentType(String, String, String)` | Verify that the three attributes (`name`, `publicId`, `systemId`) are stored and that `pubSysKey` is set to `"PUBLIC"` when a non‑blank publicId is supplied. |
| Deprecated constructors | They do **not** call `Validate.notNull`. Check that they accept `null` values without throwing. |
| `setPubSysKey(String)` | *null* → no change; non‑null → attribute is (re)written. |
| `nodeName()` | Returns the constant `"#doctype"`. |
| `outerHtmlHead` | Render the doctype in several configurations (HTML5, full DOCTYPE, blank IDs, HTML vs XML syntax). |
| `outerHtmlTail` | Should be a no‑op (does not modify the Appendable). |

---

**Step 3 – Edge‑case & exception analysis**

| Edge case / Exception | Reason it needs testing |
|-----------------------|--------------------------|
| **`null` arguments** to the primary constructor | `Validate.notNull` should throw an `IllegalArgumentException`. |
| **Empty strings** for `publicId` / `systemId` | `has()` treats blank strings as “missing”; the generated HTML must omit the corresponding parts. |
| **`setPubSysKey(null)`** | Must not remove an existing key. |
| **HTML5 doctype with `Syntax.html`** | Should be rendered as `<!doctype>` (lower‑case). |
| **HTML5 doctype with `Syntax.xml`** | Must stay upper‑case (`<!DOCTYPE>`). |
| **Deprecated constructors with `null` values** | No validation → attributes become empty strings, not `null`. |
| **`outerHtmlTail`** | Should leave the supplied `Appendable` untouched. |

---

**Step 4 – JUnit 4 test cases for the identified edge cases**

The code below combines the basic tests and the edge‑case tests into a single, self‑contained JUnit 4 test class.  
All tests are placed in the same package (`org.jsoup.nodes`) so that package‑private methods can be invoked directly.

```java
###Test START##
package org.jsoup.nodes;

import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Document.OutputSettings.Syntax;
import org.junit.Test;
import java.io.IOException;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link DocumentType}.
 *
 * Covers:
 *  • all public constructors
 *  • setPubSysKey
 *  • nodeName
 *  • outerHtmlHead (different syntax / attribute combinations)
 *  • outerHtmlTail (no‑op)
 *  • edge cases and expected exceptions
 */
public class DocumentTypeTest {

    /* --------------------------------------------------------------------- *
     *  Helper utilities
     * --------------------------------------------------------------------- */

    /** Render the head part of a DocumentType using the supplied syntax. */
    private String renderHead(DocumentType dt, Syntax syntax) throws IOException {
        StringBuilder sb = new StringBuilder();
        OutputSettings settings = new Document.OutputSettings();
        settings.syntax(syntax);
        dt.outerHtmlHead(sb, 0, settings);
        return sb.toString();
    }

    /* --------------------------------------------------------------------- *
     *  Constructor tests – normal usage
     * --------------------------------------------------------------------- */

    @Test
    public void constructorValidCreatesAttributes() {
        DocumentType dt = new DocumentType("html",
                "-//W3C//DTD HTML 4.01//EN",
                "http://www.w3.org/TR/html4/strict.dtd");

        assertEquals("html", dt.attr("name"));
        assertEquals("-//W3C//DTD HTML 4.01//EN", dt.attr("publicId"));
        assertEquals("http://www.w3.org/TR/html4/strict.dtd", dt.attr("systemId"));
        // because a non‑blank publicId is present, pubSysKey must be "PUBLIC"
        assertEquals("PUBLIC", dt.attr("pubSysKey"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorNullNameThrows() {
        new DocumentType(null, "pub", "sys");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorNullPublicIdThrows() {
        new DocumentType("html", null, "sys");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorNullSystemIdThrows() {
        new DocumentType("html", "pub", null);
    }

    /* --------------------------------------------------------------------- *
     *  Deprecated constructors – they do *not* validate nulls
     * --------------------------------------------------------------------- */

    @Test
    public void deprecatedConstructorAllowsNulls() {
        DocumentType dt = new DocumentType(null, null, null, null);
        // The underlying attr() stores an empty string when the value is null.
        assertEquals("", dt.attr("name"));
        assertEquals("", dt.attr("publicId"));
        assertEquals("", dt.attr("systemId"));
    }

    @Test
    public void deprecatedConstructorWithPubSysKey() {
        DocumentType dt = new DocumentType("html", "SYSTEM", "pub", "sys", null);
        assertEquals("SYSTEM", dt.attr("pubSysKey"));
        assertEquals("html", dt.attr("name"));
        assertEquals("pub", dt.attr("publicId"));
        assertEquals("sys", dt.attr("systemId"));
    }

    /* --------------------------------------------------------------------- *
     *  setPubSysKey
     * --------------------------------------------------------------------- */

    @Test
    public void setPubSysKeyNullDoesNotOverwriteExistingValue() {
        DocumentType dt = new DocumentType("html", "pub", "sys");
        dt.setPubSysKey(null);
        assertEquals("PUBLIC", dt.attr("pubSysKey"));
    }

    @Test
    public void setPubSysKeyUpdatesValue() {
        DocumentType dt = new DocumentType("html", "pub", "sys");
        dt.setPubSysKey("SYSTEM");
        assertEquals("SYSTEM", dt.attr("pubSysKey"));
    }

    /* --------------------------------------------------------------------- *
     *  nodeName()
     * --------------------------------------------------------------------- */

    @Test
    public void nodeNameIsDoctype() {
        DocumentType dt = new DocumentType("html", "pub", "sys");
        assertEquals("#doctype", dt.nodeName());
    }

    /* --------------------------------------------------------------------- *
     *  outerHtmlHead – various rendering scenarios
     * --------------------------------------------------------------------- */

    @Test
    public void outerHtmlHeadHtml5LowercaseWhenNoIdsAndHtmlSyntax() throws IOException {
        DocumentType dt = new DocumentType("html", "", "");
        String out = renderHead(dt, Syntax.html);
        assertEquals("<!doctype>", out);
    }

    @Test
    public void outerHtmlHeadHtml5UppercaseWhenXmlSyntax() throws IOException {
        DocumentType dt = new DocumentType("html", "", "");
        String out = renderHead(dt, Syntax.xml);
        assertEquals("<!DOCTYPE>", out);
    }

    @Test
    public void outerHtmlHeadFullDoctypeRendering() throws IOException {
        DocumentType dt = new DocumentType(
                "html",
                "-//W3C//DTD XHTML 1.0 Transitional//EN",
                "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd");
        String out = renderHead(dt, Syntax.html);
        String expected = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";
        assertEquals(expected, out);
    }

    @Test
    public void outerHtmlHeadOmitsPublicWhenPublicIdBlank() throws IOException {
        DocumentType dt = new DocumentType("html", "", "sys");
        String out = renderHead(dt, Syntax.html);
        // No PUBLIC keyword should appear
        assertFalse(out.contains("PUBLIC"));
        // System ID must still be rendered
        assertTrue(out.contains(" \"sys\""));
    }

    @Test
    public void outerHtmlHeadOmitsSystemWhenSystemIdBlank() throws IOException {
        DocumentType dt = new DocumentType("html", "pub", "");
        String out = renderHead(dt, Syntax.html);
        // No system‑id part should be rendered
        assertFalse(out.contains("\"\"")); // empty quoted string would be a bug
        // Public ID must be rendered with PUBLIC keyword
        assertTrue(out.contains("PUBLIC \"pub\""));
    }

    @Test
    public void outerHtmlHeadWithExplicitPubSysKey() throws IOException {
        DocumentType dt = new DocumentType("html", "SYSTEM", "pub", "sys", null);
        String out = renderHead(dt, Syntax.html);
        // Should contain the explicit key "SYSTEM"
        assertTrue(out.contains("SYSTEM"));
    }

    /* --------------------------------------------------------------------- *
     *  outerHtmlTail – should be a no‑op
     * --------------------------------------------------------------------- */

    @Test
    public void outerHtmlTailDoesNotModifyAppendable() {
        DocumentType dt = new DocumentType("html", "pub", "sys");
        StringBuilder sb = new StringBuilder("prefix");
        dt.outerHtmlTail(sb, 0, new Document.OutputSettings());
        assertEquals("prefix", sb.toString());
    }
}
###Test END##
```