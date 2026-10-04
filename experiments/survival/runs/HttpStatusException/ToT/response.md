###Test START##
```java
package org.jsoup;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link HttpStatusException}.
 *
 * The suite is written as if three independent experts each contributed
 * a single test case for every public member of the class.  The final
 * result combines all of those contributions into a comprehensive set
 * of tests covering typical usage, edge‑cases and error scenarios.
 *
 * @author  Test‑Generator
 */
public class HttpStatusExceptionTest {

    /* -----------------------------------------------------------------
     *  Expert 1 – Constructor & basic inheritance checks
     * ----------------------------------------------------------------- */
    @Test
    public void constructor_initialisesAllFields_andIsInstanceOfIOException() {
        String message = "Not Found";
        int status = 404;
        String url = "http://example.com/missing";

        HttpStatusException ex = new HttpStatusException(message, status, url);

        // Verify inheritance
        assertTrue("HttpStatusException must be an IOException", ex instanceof IOException);

        // Verify message handling (Throwable#getMessage)
        assertEquals("Message must be stored correctly", message, ex.getMessage());

        // Verify fields via getters
        assertEquals("Status code must be stored correctly", status, ex.getStatusCode());
        assertEquals("URL must be stored correctly", url, ex.getUrl());
    }

    @Test
    public void constructor_allowsNullMessageAndUrl() {
        // Message may be null – Throwable permits this.
        HttpStatusException ex = new HttpStatusException(null, 500, null);

        assertNull("Message should be null when passed as null", ex.getMessage());
        assertEquals("Status code should be stored even with null message/url", 500, ex.getStatusCode());
        assertNull("URL should be null when passed as null", ex.getUrl());
    }

    @Test
    public void constructor_acceptsBoundaryStatusCodes() {
        // Zero is technically a valid int, though not a real HTTP status.
        HttpStatusException zero = new HttpStatusException("Zero", 0, "http://zero");
        assertEquals(0, zero.getStatusCode());

        // Negative numbers – not a real HTTP code but should be stored unchanged.
        HttpStatusException negative = new HttpStatusException("Negative", -1, "http://neg");
        assertEquals(-1, negative.getStatusCode());

        // Very large int (beyond HTTP spec) – ensure no overflow/truncation.
        int huge = Integer.MAX_VALUE;
        HttpStatusException big = new HttpStatusException("Huge", huge, "http://big");
        assertEquals(huge, big.getStatusCode());
    }

    /* -----------------------------------------------------------------
     *  Expert 2 – Getter methods
     * ----------------------------------------------------------------- */
    @Test
    public void getStatusCode_returnsExactValueProvidedToConstructor() {
        int[] testCodes = {200, 301, 403, 500, 999};
        for (int code : testCodes) {
            HttpStatusException ex = new HttpStatusException("msg", code, "http://test");
            assertEquals("getStatusCode() should return the code supplied to the constructor",
                         code, ex.getStatusCode());
        }
    }

    @Test
    public void getUrl_returnsExactStringProvidedToConstructor() {
        String[] urls = {
                "http://example.com",
                "https://sub.domain.org/path?query=param#fragment",
                "",
                "ftp://legacy-protocol",
                "http://localhost:8080/test"
        };
        for (String u : urls) {
            HttpStatusException ex = new HttpStatusException("msg", 200, u);
            assertEquals("getUrl() should return the URL supplied to the constructor",
                         u, ex.getUrl());
        }
    }

    /* -----------------------------------------------------------------
     *  Expert 3 – toString() formatting
     * ----------------------------------------------------------------- */
    @Test
    public void toString_includesSuperMessage_andStatusAndUrl() {
        String message = "Forbidden";
        int status = 403;
        String url = "https://secure.example.com/secret";

        HttpStatusException ex = new HttpStatusException(message, status, url);
        String result = ex.toString();

        // The super implementation prints: className: message
        String expectedPrefix = HttpStatusException.class.getName() + ": " + message;
        assertTrue("toString should start with the super's toString()", result.startsWith(expectedPrefix));

        // Verify the appended part
        String expectedSuffix = ". Status=" + status + ", URL=" + url;
        assertTrue("toString should end with status and URL info", result.endsWith(expectedSuffix));
        assertTrue("toString should contain the whole suffix", result.contains(expectedSuffix));
    }

    @Test
    public void toString_handlesNullMessageAndUrlGracefully() {
        HttpStatusException ex = new HttpStatusException(null, 418, null);
        String result = ex.toString();

        // When message is null, super.toString() ends with the class name only.
        String expectedPrefix = HttpStatusException.class.getName();
        assertTrue("When message is null, prefix should be just the class name",
                   result.startsWith(expectedPrefix));

        // Check that the custom suffix is still correctly appended.
        String expectedSuffix = ". Status=418, URL=null";
        assertTrue("Suffix must be present even when message/url are null", result.endsWith(expectedSuffix));
    }

    @Test
    public void toString_isConsistentAcrossMultipleCalls() {
        HttpStatusException ex = new HttpStatusException("Loop test", 200, "http://repeat");
        String first = ex.toString();
        String second = ex.toString();
        assertEquals("toString should be deterministic and return the same value each call", first, second);
    }
}
```
###Test END##