**Step 1 – Public API extraction**

| Visibility | Return type | Method | Parameters | Description |
|------------|------------|--------|------------|-------------|
| `public` | `String` | `getErrorMessage()` | – | Returns the stored error message. |
| `public` | `int` | `getPosition()` | – | Returns the error position (offset). |
| `public` | `String` | `toString()` | – | Returns a *“position: message”* representation. |

*Note*: The class also has two **package‑private** constructors that are exercised from the test class (the test class lives in the same package `org.jsoup.parser`).

---

**Step 2 – Basic JUnit 4 test ideas (one per method)**  

| Expert | Method | Basic test case (description) |
|--------|--------|------------------------------|
| **Alice** | `getErrorMessage()` | Create a `ParseError` with a plain message and assert the getter returns the exact same string. |
| **Bob** | `getPosition()` | Create a `ParseError` with a known position (e.g., `42`) and assert the getter returns `42`. |
| **Carol** | `toString()` | Create a `ParseError` with position `7` and message `"boom"` and assert `toString()` returns `"7: boom"`. |

---

**Step 3 – Edge‑case & exception analysis**

| Situation | Why it matters | Expected behaviour |
|-----------|----------------|--------------------|
| **`null` error message** (first constructor) | The class does not guard against `null`. The getter should simply return `null`. |
| **`null` format string** (second constructor) | `String.format(null, …)` throws `NullPointerException`. The constructor should propagate it. |
| **Invalid format string** (e.g., `"%d"` with a non‑numeric argument) | `String.format` throws `IllegalFormatException`. The constructor should propagate it. |
| **Negative position** | No validation, so negative values are allowed – test that they are stored and reflected in `toString()`. |
| **Very large position** (e.g., `Integer.MAX_VALUE`) | Ensure no overflow/formatting issues. |
| **Message containing line‑breaks or special characters** | Verify that `toString()` concatenates correctly and does not truncate. |
| **Zero‑argument var‑args constructor** | Passing a format without placeholders should work (e.g., `"fixed"` with no args). |
| **Empty message** | Empty string should be returned unchanged. |

---

**Step 4 – Expanded JUnit 4 test cases for the edge scenarios**

*All tests are placed in the same package (`org.jsoup.parser`) so the package‑private constructors are accessible.*

```java
package org.jsoup.parser;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.IllegalFormatException;

/**
 * JUnit‑4 test suite for {@link ParseError}.
 *
 * The suite covers:
 *   • normal usage of every public method,
 *   • edge‑case values (null, negative, large numbers, special chars),
 *   • exception propagation from the format‑based constructor.
 *
 * The file is wrapped with the required markers.
 */
public class ParseErrorTest {

    /*=== Basic functionality tests =================================================*/

    @Test
    public void testGetErrorMessage_simple() {
        ParseError err = new ParseError(10, "simple message");
        assertEquals("simple message", err.getErrorMessage());
    }

    @Test
    public void testGetErrorMessage_fromFormat() {
        ParseError err = new ParseError(5, "value %d is %s", 7, "odd");
        assertEquals("value 7 is odd", err.getErrorMessage());
    }

    @Test
    public void testGetPosition() {
        ParseError err = new ParseError(42, "any");
        assertEquals(42, err.getPosition());
    }

    @Test
    public void testToString_normal() {
        ParseError err = new ParseError(7, "boom");
        assertEquals("7: boom", err.toString());
    }

    /*=== Edge‑case tests ===========================================================*/

    @Test
    public void testErrorMessage_null() {
        ParseError err = new ParseError(3, (String) null);
        assertNull(err.getErrorMessage());
        // toString should handle the null gracefully (Java will convert null to "null")
        assertEquals("3: null", err.toString());
    }

    @Test
    public void testConstructor_nullFormatString_throwsNPE() {
        try {
            new ParseError(0, null, new Object[] {}); // var‑args version
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // pass
        }
    }

    @Test
    public void testConstructor_invalidFormat_throwsIllegalFormatException() {
        try {
            new ParseError(1, "%d", "not a number");
            fail("Expected IllegalFormatException");
        } catch (IllegalFormatException expected) {
            // pass
        }
    }

    @Test
    public void testNegativePosition() {
        ParseError err = new ParseError(-15, "negative pos");
        assertEquals(-15, err.getPosition());
        assertEquals("-15: negative pos", err.toString());
    }

    @Test
    public void testMaxIntPosition() {
        ParseError err = new ParseError(Integer.MAX_VALUE, "max int");
        assertEquals(Integer.MAX_VALUE, err.getPosition());
        assertEquals(Integer.MAX_VALUE + ": max int", err.toString());
    }

    @Test
    public void testMessageWithSpecialChars() {
        String special = "line1\nline2\r\t\"quotes\" and \\backslashes\\";
        ParseError err = new ParseError(9, special);
        assertEquals(special, err.getErrorMessage());
        assertEquals("9: " + special, err.toString());
    }

    @Test
    public void testEmptyMessage() {
        ParseError err = new ParseError(0, "");
        assertEquals("", err.getErrorMessage());
        assertEquals("0: ", err.toString());
    }

    @Test
    public void testFormatWithoutPlaceholders_andNoArgs() {
        ParseError err = new ParseError(2, "fixed message");
        assertEquals("fixed message", err.getErrorMessage());
        assertEquals("2: fixed message", err.toString());
    }

    @Test
    public void testFormatWithPlaceholdersAndEmptyArgsArray() {
        try {
            new ParseError(4, "needs %s", new Object[0]);
            fail("Expected IllegalFormatException due to missing argument");
        } catch (IllegalFormatException expected) {
            // pass
        }
    }
}
```

---

### Complete test file (with required markers)

```java
###Test START##
package org.jsoup.parser;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.IllegalFormatException;

/**
 * JUnit‑4 test suite for {@link ParseError}.
 *
 * Covers normal usage, edge cases and exception propagation.
 */
public class ParseErrorTest {

    /*=== Basic functionality =====================================================*/

    @Test
    public void testGetErrorMessage_simple() {
        ParseError err = new ParseError(10, "simple message");
        assertEquals("simple message", err.getErrorMessage());
    }

    @Test
    public void testGetErrorMessage_fromFormat() {
        ParseError err = new ParseError(5, "value %d is %s", 7, "odd");
        assertEquals("value 7 is odd", err.getErrorMessage());
    }

    @Test
    public void testGetPosition() {
        ParseError err = new ParseError(42, "any");
        assertEquals(42, err.getPosition());
    }

    @Test
    public void testToString_normal() {
        ParseError err = new ParseError(7, "boom");
        assertEquals("7: boom", err.toString());
    }

    /*=== Edge‑case & exception tests ===========================================*/

    @Test
    public void testErrorMessage_null() {
        ParseError err = new ParseError(3, (String) null);
        assertNull(err.getErrorMessage());
        assertEquals("3: null", err.toString());
    }

    @Test
    public void testConstructor_nullFormatString_throwsNPE() {
        try {
            new ParseError(0, null, new Object[] {});
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testConstructor_invalidFormat_throwsIllegalFormatException() {
        try {
            new ParseError(1, "%d", "not a number");
            fail("Expected IllegalFormatException");
        } catch (IllegalFormatException expected) {
            // expected
        }
    }

    @Test
    public void testNegativePosition() {
        ParseError err = new ParseError(-15, "negative pos");
        assertEquals(-15, err.getPosition());
        assertEquals("-15: negative pos", err.toString());
    }

    @Test
    public void testMaxIntPosition() {
        ParseError err = new ParseError(Integer.MAX_VALUE, "max int");
        assertEquals(Integer.MAX_VALUE, err.getPosition());
        assertEquals(Integer.MAX_VALUE + ": max int", err.toString());
    }

    @Test
    public void testMessageWithSpecialChars() {
        String special = "line1\nline2\r\t\"quotes\" and \\backslashes\\";
        ParseError err = new ParseError(9, special);
        assertEquals(special, err.getErrorMessage());
        assertEquals("9: " + special, err.toString());
    }

    @Test
    public void testEmptyMessage() {
        ParseError err = new ParseError(0, "");
        assertEquals("", err.getErrorMessage());
        assertEquals("0: ", err.toString());
    }

    @Test
    public void testFormatWithoutPlaceholders_andNoArgs() {
        ParseError err = new ParseError(2, "fixed message");
        assertEquals("fixed message", err.getErrorMessage());
        assertEquals("2: fixed message", err.toString());
    }

    @Test
    public void testFormatWithPlaceholdersAndEmptyArgsArray() {
        try {
            new ParseError(4, "needs %s", new Object[0]);
            fail("Expected IllegalFormatException due to missing argument");
        } catch (IllegalFormatException expected) {
            // expected
        }
    }
}
###Test END##
```