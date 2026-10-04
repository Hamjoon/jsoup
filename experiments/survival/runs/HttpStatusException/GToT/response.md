**Step 1 – Public API of `HttpStatusException`**

| Visibility | Return type | Method name | Parameters | Description |
|------------|-------------|-------------|------------|-------------|
| `public`   | `int`       | `getStatusCode()` | – | Returns the HTTP status code that caused the exception. |
| `public`   | `String`    | `getUrl()`        | – | Returns the URL that was being fetched when the exception occurred. |
| `public`   | `String`    | `toString()`      | – | Overrides `Object.toString()` – returns the default `IOException` message plus “`. Status=`*code*`, URL=`*url*`”. |
| `public`   | –           | `HttpStatusException(String message, int statusCode, String url)` | `String message`, `int statusCode`, `String url` | Constructor – forwards *message* to `IOException`, stores *statusCode* and *url*. |

---

**Step 2 – Basic JUnit 4 test ideas (one per method)**  

| Method | Basic test idea |
|--------|-----------------|
| Constructor | Verify that the supplied *message*, *statusCode* and *url* are stored correctly (via getters and `getMessage()`). |
| `getStatusCode()` | Create an instance with a known status code and assert that the getter returns it. |
| `getUrl()` | Create an instance with a known URL and assert that the getter returns it. |
| `toString()` | Check that the string contains the original message, the status code and the URL. |

---

**Step 3 – Edge‑case & exception‑handling scenarios**

1. **`null` message** – `IOException` permits a `null` message; the class should keep it and `toString()` must not throw NPE.  
2. **`null` URL** – The URL field may be `null`; getters should return `null` and `toString()` must render “`URL=null`”.  
3. **Negative or out‑of‑range status codes** – The constructor does not validate the code, so we should verify that any `int` value is accepted and reflected in the getters/`toString()`.  
4. **Empty strings** – Ensure that empty message or URL are handled identically to non‑empty values.  

---

**Step 4 – Full JUnit 4 test suite (covers normal, edge and error cases)**  

```java
###Test START##
package org.jsoup;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link HttpStatusException}.
 *
 * Covers:
 *   • Normal construction and getters
 *   • toString() formatting
 *   • Edge cases (null message, null URL, negative status code, empty strings)
 */
public class HttpStatusExceptionTest {

    /* ---------------------------  Normal cases  --------------------------- */

    @Test
    public void constructorSetsAllFieldsAndMessage() {
        String message = "Not Found";
        int status = 404;
        String url = "http://example.com";

        HttpStatusException ex = new HttpStatusException(message, status, url);

        // Verify inheritance of the message
        assertEquals(message, ex.getMessage());
        // Verify our own fields
        assertEquals(status, ex.getStatusCode());
        assertEquals(url, ex.getUrl());
    }

    @Test
    public void gettersReturnSuppliedValues() {
        HttpStatusException ex = new HttpStatusException("Error", 500, "https://site.org");
        assertEquals(500, ex.getStatusCode());
        assertEquals("https://site.org", ex.getUrl());
    }

    @Test
    public void toStringContainsMessageStatusAndUrl() {
        String message = "Forbidden";
        int status = 403;
        String url = "http://blocked.com";

        HttpStatusException ex = new HttpStatusException(message, status, url);
        String s = ex.toString();

        // The default IOException toString starts with the class name
        assertTrue(s.startsWith("java.io.IOException"));
        assertTrue(s.contains(message));
        assertTrue(s.contains("Status=" + status));
        assertTrue(s.contains("URL=" + url));
    }

    /* ---------------------------  Edge cases  --------------------------- */

    @Test
    public void constructorAllowsNullMessage() {
        HttpStatusException ex = new HttpStatusException(null, 200, "http://ok.com");
        assertNull(ex.getMessage());
        assertEquals(200, ex.getStatusCode());
        assertEquals("http://ok.com", ex.getUrl());

        // toString should still be well‑formed
        String s = ex.toString();
        assertTrue(s.startsWith("java.io.IOException"));
        assertTrue(s.contains("Status=200"));
        assertTrue(s.contains("URL=http://ok.com"));
    }

    @Test
    public void constructorAllowsNullUrl() {
        HttpStatusException ex = new HttpStatusException("No URL", 301, null);
        assertEquals("No URL", ex.getMessage());
        assertEquals(301, ex.getStatusCode());
        assertNull(ex.getUrl());

        // URL part of toString must be the literal "null"
        assertTrue(ex.toString().contains("URL=null"));
    }

    @Test
    public void constructorHandlesNegativeStatusCode() {
        HttpStatusException ex = new HttpStatusException("Negative", -1, "http://neg.com");
        assertEquals(-1, ex.getStatusCode());
        assertTrue(ex.toString().contains("Status=-1"));
    }

    @Test
    public void constructorHandlesEmptyMessageAndUrl() {
        HttpStatusException ex = new HttpStatusException("", 202, "");
        assertEquals("", ex.getMessage());
        assertEquals(202, ex.getStatusCode());
        assertEquals("", ex.getUrl());

        String s = ex.toString();
        // Empty message should appear as just the class name (no extra text)
        assertTrue(s.startsWith("java.io.IOException"));
        assertTrue(s.contains("Status=202"));
        assertTrue(s.contains("URL="));
    }

    @Test
    public void toStringHandlesNullMessageGracefully() {
        HttpStatusException ex = new HttpStatusException(null, 404, "http://nullmsg.com");
        String s = ex.toString();

        // Should not throw NPE and must contain status and URL
        assertTrue(s.startsWith("java.io.IOException"));
        assertTrue(s.contains("Status=404"));
        assertTrue(s.contains("URL=http://nullmsg.com"));
    }
}
###Test END##
```

The file above:

* Lists **all** public members of `HttpStatusException`.  
* Provides a **basic** test for each method/constructor.  
* Adds **edge‑case** tests for `null` arguments, negative status codes, and empty strings.  
* Uses only JUnit 4 (`@Test`, `assertEquals`, `assertTrue`, `assertNull`).  

Running this suite against the supplied class will give full confidence that the class behaves correctly under normal and boundary conditions.