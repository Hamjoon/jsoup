**Explanation of the approach (summary of the three‑expert discussion)**  

1. **Extracted public API** – the class only exposes four public constructors:  
   * `SerializationException()`  
   * `SerializationException(String message)`  
   * `SerializationException(Throwable cause)`  
   * `SerializationException(String message, Throwable cause)`

2. **Basic functional tests** – each constructor is exercised to verify that the inherited `RuntimeException` state (`message` and `cause`) is set according to the JDK contract.

3. **Edge‑case & exception‑handling tests** – we considered `null` arguments, empty strings, and unusual `Throwable` subclasses (checked exception, custom subclass). For the *cause‑only* constructor we also assert that the automatically generated message equals `cause.toString()` when the cause is non‑null.

4. **Combined test file** – all test methods are placed in a single JUnit 4 test class named `SerializationExceptionTest`. The file is delimited with the required markers.

---  

### ###Test START##
```java
package org.jsoup;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link SerializationException}.
 *
 * The tests cover:
 *  • Basic construction of each public constructor.
 *  • Edge cases such as {@code null} messages / causes, empty messages,
 *    and custom {@link Throwable} subclasses.
 *  • Verification of the contract defined by {@link RuntimeException}
 *    concerning {@code getMessage()} and {@code getCause()}.
 */
public class SerializationExceptionTest {

    /* -------------------------------------------------
     *  1. No‑arg constructor
     * ------------------------------------------------- */

    @Test
    public void testNoArgConstructor() {
        SerializationException ex = new SerializationException();
        assertNull("Message should be null for no‑arg constructor", ex.getMessage());
        assertNull("Cause should be null for no‑arg constructor", ex.getCause());
    }

    /* -------------------------------------------------
     *  2. Constructor with message only
     * ------------------------------------------------- */

    @Test
    public void testMessageConstructor_NullMessage() {
        SerializationException ex = new SerializationException((String) null);
        assertNull("Message should be null when null is passed", ex.getMessage());
        assertNull("Cause should be null when only message is supplied", ex.getCause());
    }

    @Test
    public void testMessageConstructor_EmptyMessage() {
        SerializationException ex = new SerializationException("");
        assertEquals("Empty string message should be preserved", "", ex.getMessage());
        assertNull("Cause should be null when only message is supplied", ex.getCause());
    }

    @Test
    public void testMessageConstructor_NonEmptyMessage() {
        String msg = "serialization failed";
        SerializationException ex = new SerializationException(msg);
        assertEquals("Message should be stored unchanged", msg, ex.getMessage());
        assertNull("Cause should be null when only message is supplied", ex.getCause());
    }

    /* -------------------------------------------------
     *  3. Constructor with cause only
     * ------------------------------------------------- */

    @Test
    public void testCauseConstructor_NullCause() {
        SerializationException ex = new SerializationException((Throwable) null);
        assertNull("Message should be null when cause is null", ex.getMessage());
        assertNull("Cause should be null when null is passed", ex.getCause());
    }

    @Test
    public void testCauseConstructor_WithIOException() {
        IOException io = new IOException("stream closed");
        SerializationException ex = new SerializationException(io);
        // RuntimeException(Throwable) uses cause.toString() as the message
        assertEquals("Message should be cause.toString()", io.toString(), ex.getMessage());
        assertSame("Cause should be the supplied throwable", io, ex.getCause());
    }

    @Test
    public void testCauseConstructor_WithCustomThrowable() {
        Throwable custom = new Throwable("custom cause");
        SerializationException ex = new SerializationException(custom);
        assertEquals("Message should be cause.toString()", custom.toString(), ex.getMessage());
        assertSame("Cause should be the custom throwable", custom, ex.getCause());
    }

    /* -------------------------------------------------
     *  4. Constructor with both message and cause
     * ------------------------------------------------- */

    @Test
    public void testMessageAndCauseConstructor_BothNull() {
        SerializationException ex = new SerializationException(null, null);
        assertNull("Message should be null when both args are null", ex.getMessage());
        assertNull("Cause should be null when both args are null", ex.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor_NullMessage_NonNullCause() {
        IOException io = new IOException("IO error");
        SerializationException ex = new SerializationException(null, io);
        assertNull("Message should be null when explicitly passed null", ex.getMessage());
        assertSame("Cause should be the supplied throwable", io, ex.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor_NonNullMessage_NullCause() {
        String msg = "serialization problem";
        SerializationException ex = new SerializationException(msg, null);
        assertEquals("Message should be the supplied non‑null string", msg, ex.getMessage());
        assertNull("Cause should be null when explicitly passed null", ex.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor_BothNonNull() {
        String msg = "failed to serialize";
        IllegalStateException cause = new IllegalStateException("illegal state");
        SerializationException ex = new SerializationException(msg, cause);
        assertEquals("Message should be the supplied string", msg, ex.getMessage());
        assertSame("Cause should be the supplied throwable", cause, ex.getCause());
    }

    @Test
    public void testMessageAndCauseConstructor_EmptyMessage_NonNullCause() {
        Throwable cause = new RuntimeException("runtime problem");
        SerializationException ex = new SerializationException("", cause);
        assertEquals("Empty message should be preserved", "", ex.getMessage());
        assertSame("Cause should be the supplied throwable", cause, ex.getCause());
    }

}
```
### ###Test END##