**Step 1 – Public API of `UnsupportedMimeTypeException`**

| Modifier | Return type | Method name | Parameters | Description |
|----------|-------------|-------------|------------|-------------|
| `public` | `UnsupportedMimeTypeException` | **Constructor** | `String message, String mimeType, String url` | Creates the exception with a detail message, the unsupported MIME type and the URL that caused the problem. |
| `public` | `String` | **getMimeType** | *none* | Returns the MIME type that was not supported. |
| `public` | `String` | **getUrl** | *none* | Returns the URL that produced the unsupported MIME type. |
| `public` | `String` | **toString** | *overrides* `java.lang.Object.toString()` | Returns a string that contains the standard `IOException` message plus the MIME type and URL. |

---

**Step 2 – One basic test case per method (first round of ideas)**  

| Method | Proposed test name | Core idea |
|--------|-------------------|-----------|
| Constructor | `testConstructorInitialisesFields` | Build the exception and assert that `getMessage()`, `getMimeType()` and `getUrl()` return exactly the values supplied to the ctor. |
| `getMimeType` | `testGetMimeTypeReturnsProvidedValue` | Use the constructor to set a known MIME type and verify the getter returns it. |
| `getUrl` | `testGetUrlReturnsProvidedValue` | Same as above, but for the URL. |
| `toString` | `testToStringContainsAllParts` | Verify that the string returned by `toString()` contains the original message, the MIME type and the URL (in the exact format defined in the source). |

---

**Step 3 – Edge‑case & exception‑handling analysis**

1. **`null` arguments** – The constructor does **not** guard against `null`. Passing `null` for any of the three parameters should be allowed and the getters should return `null`. `toString()` must therefore handle `null` values gracefully (it uses string concatenation, which yields the literal `"null"`).  
2. **Empty strings** – An empty message, MIME type or URL is a valid input; the getters should return the empty string and `toString()` should embed it correctly.  
3. **Whitespace / special characters** – Ensure that unusual but legal characters (spaces, tabs, line‑breaks, Unicode symbols) survive the round‑trip through the getters and `toString()`.  
4. **Very long strings** – Build a long (e.g., 10 000‑character) MIME type and URL to confirm no hidden size limits cause truncation or performance issues.  
5. **Inheritance contract** – Although not a separate method, the exception must still behave like an `IOException` (e.g., `getMessage()` returns the message supplied to the super‑ctor).  

These edge cases give us additional test methods.

---

**Step 4 – Test cases for the identified edge cases**

| Edge case | Test name | What it asserts |
|-----------|-----------|-----------------|
| `null` message, mime, URL | `testConstructorAcceptsNullValues` | All three getters return `null`; `toString()` contains the literal `"null"` for mime and URL. |
| Empty strings | `testConstructorAcceptsEmptyStrings` | Getters return empty strings; `toString()` contains `Mimetype=` (empty) and `URL=` (empty). |
| Whitespace & Unicode | `testConstructorWithWhitespaceAndUnicode` | Getters return the exact whitespace/unicode strings; `toString()` includes them unchanged. |
| Very long strings | `testConstructorWithVeryLongStrings` | Getters return the long strings unchanged; `toString()` contains the full long values (no truncation). |
| Inherited `getMessage` | `testGetMessageFromIOException` | `getMessage()` returns the message passed to the constructor. |

---

**Step 5 – Merged, complete JUnit 4 test file**

```java
###Test START##
package org.jsoup;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link UnsupportedMimeTypeException}.
 *
 * Covers:
 *   - normal usage of all public members,
 *   - edge‑case handling of null / empty / whitespace / Unicode / very long values,
 *   - inheritance contract (IOException#getMessage()).
 */
public class UnsupportedMimeTypeExceptionTest {

    // -----------------------------------------------------------------
    // 1️⃣  Basic / typical use‑case tests
    // -----------------------------------------------------------------

    @Test
    public void testConstructorInitialisesFields() {
        String message = "Unsupported MIME type";
        String mime = "application/unknown";
        String url = "http://example.com/resource";

        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(message, mime, url);

        assertEquals(message, ex.getMessage());
        assertEquals(mime, ex.getMimeType());
        assertEquals(url, ex.getUrl());
    }

    @Test
    public void testGetMimeTypeReturnsProvidedValue() {
        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException("msg", "image/xyz", "http://a.b");
        assertEquals("image/xyz", ex.getMimeType());
    }

    @Test
    public void testGetUrlReturnsProvidedValue() {
        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException("msg", "text/plain", "https://host/path");
        assertEquals("https://host/path", ex.getUrl());
    }

    @Test
    public void testToStringContainsAllParts() {
        String message = "Bad mime";
        String mime = "video/xyz";
        String url = "http://site.com/video";

        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(message, mime, url);
        String str = ex.toString();

        // The toString implementation concatenates super.toString() with ". Mimetype=" + mime + ", URL=" + url
        assertTrue(str.contains(message));
        assertTrue(str.contains(". Mimetype=" + mime + ", URL=" + url));
    }

    // -----------------------------------------------------------------
    // 2️⃣  Edge‑case tests
    // -----------------------------------------------------------------

    @Test
    public void testConstructorAcceptsNullValues() {
        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(null, null, null);

        assertNull(ex.getMessage());          // super ctor receives null → getMessage() returns null
        assertNull(ex.getMimeType());
        assertNull(ex.getUrl());

        String str = ex.toString();
        // When concatenating null, Java produces the string "null"
        assertTrue(str.contains("Mimetype=null"));
        assertTrue(str.contains("URL=null"));
    }

    @Test
    public void testConstructorAcceptsEmptyStrings() {
        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException("", "", "");

        assertEquals("", ex.getMessage());
        assertEquals("", ex.getMimeType());
        assertEquals("", ex.getUrl());

        String str = ex.toString();
        // Should contain the literal empty parts after the '=' signs
        assertTrue(str.contains("Mimetype="));
        assertTrue(str.contains(", URL="));
        // Ensure there is no stray "null"
        assertFalse(str.contains("null"));
    }

    @Test
    public void testConstructorWithWhitespaceAndUnicode() {
        String message = "   leading and trailing   ";
        String mime = "\tapplication/😀\n";
        String url = "http://例子.测试/路径";

        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(message, mime, url);

        assertEquals(message, ex.getMessage());
        assertEquals(mime, ex.getMimeType());
        assertEquals(url, ex.getUrl());

        String str = ex.toString();
        assertTrue(str.contains(message));
        assertTrue(str.contains("Mimetype=" + mime + ", URL=" + url));
    }

    @Test
    public void testConstructorWithVeryLongStrings() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10_000; i++) {
            sb.append('a' + (i % 26));
        }
        String longMime = sb.toString();
        String longUrl = "http://" + sb.toString() + ".com";

        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException("big", longMime, longUrl);

        assertEquals(longMime, ex.getMimeType());
        assertEquals(longUrl, ex.getUrl());

        String str = ex.toString();
        assertTrue(str.contains(longMime));
        assertTrue(str.contains(longUrl));
    }

    @Test
    public void testGetMessageFromIOException() {
        String message = "Underlying IO problem";
        UnsupportedMimeTypeException ex = new UnsupportedMimeTypeException(message, "type", "url");
        assertEquals(message, ex.getMessage());
    }
}
###Test END##
```