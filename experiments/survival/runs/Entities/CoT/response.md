###Test START##
```java
package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.parser.Parser;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

/**
 * JUnit‑4 test suite for {@link Entities}.
 *
 * The tests are organised in three sections:
 *
 * 1. **Public method signatures** – a quick reference of every public static method
 *    that must be covered.
 *
 * 2. **Basic functionality tests** – one straightforward test per method that checks
 *    the normal/expected behaviour.
 *
 * 3. **Edge‑case / exception tests** – additional tests that exercise boundary
 *    conditions, null handling, unknown entity names and charset‑related encoding
 *    paths.
 *
 * All tests run with the real Jsoup data files (the {@code EntitiesData} resources
 * that ship with Jsoup) so the expected entity names such as “lt”, “amp”, “nbsp”,
 * and a known multi‑code‑point entity (“NotNestedLessLess”) are available.
 */
public class EntitiesTest {

    /* --------------------------------------------------------------------- */
    /* 1. Public method signatures (reference)                               */
    /* --------------------------------------------------------------------- */
    // public static boolean isNamedEntity(String name)
    // public static boolean isBaseNamedEntity(String name)
    // public static Character getCharacterByName(String name)
    // public static String getByName(String name)
    // public static int codepointsForName(String name, int[] codepoints)
    // public static String escape(String string, Document.OutputSettings out)
    // public static String escape(String string)
    // public static String unescape(String string)

    private Document.OutputSettings defaultSettings;
    private Document.OutputSettings asciiSettings;

    @Before
    public void setUp() {
        // Default settings – UTF‑8 charset, base escape mode (the Jsoup default)
        defaultSettings = new Document.OutputSettings();

        // ASCII settings – forces the fast‑path encoder to be used and will cause
        // non‑ASCII characters to be emitted as numeric entities.
        asciiSettings = new Document.OutputSettings()
                .charset(Charset.forName("US-ASCII"))
                .escapeMode(Entities.EscapeMode.base);
    }

    /* --------------------------------------------------------------------- */
    /* 2. Basic functionality tests                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsNamedEntity_KnownAndUnknown() {
        assertTrue("lt is a known entity", Entities.isNamedEntity("lt"));
        assertTrue("amp is a known entity", Entities.isNamedEntity("amp"));
        assertFalse("foobar is not a known entity", Entities.isNamedEntity("foobar"));
        assertFalse("empty string is not a known entity", Entities.isNamedEntity(""));
    }

    @Test
    public void testIsBaseNamedEntity_KnownAndUnknown() {
        // “lt” is part of the base set, “NotNestedLessLess” is only in the extended set
        assertTrue("lt belongs to the base set", Entities.isBaseNamedEntity("lt"));
        assertFalse("NotNestedLessLess belongs only to extended set", Entities.isBaseNamedEntity("NotNestedLessLess"));
        assertFalse("unknown name is not in base set", Entities.isBaseNamedEntity("foobar"));
    }

    @Test
    public void testGetCharacterByName_KnownAndUnknown() {
        // Known entity
        Character lt = Entities.getCharacterByName("lt");
        assertEquals("Character for 'lt' should be '<'", Character.valueOf('<'), lt);

        // Unknown entity – the method returns (char)-1 which is '\uFFFF'
        Character unknown = Entities.getCharacterByName("foobar");
        assertEquals("Unknown entity should return (char)-1", (char) -1, unknown.charValue());
    }

    @Test
    public void testGetByName_KnownAndUnknown() {
        // Single‑code‑point entity
        assertEquals("lt should map to \"<\"", "<", Entities.getByName("lt"));
        // Multi‑code‑point entity – “NotNestedLessLess” expands to two characters
        String multi = Entities.getByName("NotNestedLessLess");
        assertNotNull("Multi‑code‑point entity should not be null", multi);
        assertEquals("Length of multi‑code‑point entity should be 2", 2, multi.length());

        // Unknown entity
        assertEquals("Unknown entity should return empty string", "", Entities.getByName("foobar"));
    }

    @Test
    public void testCodepointsForName_SingleAndMulti() {
        int[] buffer = new int[2];

        // Single code point
        int countSingle = Entities.codepointsForName("lt", buffer);
        assertEquals("lt should produce exactly one code point", 1, countSingle);
        assertEquals("First code point should be '<'", (int) '<', buffer[0]);

        // Multi‑code‑point entity
        int countMulti = Entities.codepointsForName("NotNestedLessLess", buffer);
        assertEquals("NotNestedLessLess should produce two code points", 2, countMulti);
        // Expected code points are 10913 and 824 (see the comment in load())
        assertEquals(10913, buffer[0]);
        assertEquals(824, buffer[1]);

        // Unknown entity
        int countUnknown = Entities.codepointsForName("foobar", buffer);
        assertEquals("Unknown entity should produce zero code points", 0, countUnknown);
    }

    @Test
    public void testEscape_WithDefaultSettings() {
        String raw = "a & b < c > d \"e\" \u00A0 f"; // contains &, <, >, ", non‑breaking space
        String escaped = Entities.escape(raw, defaultSettings);
        // Non‑breaking space should become &nbsp; (base mode)
        String expected = "a &amp; b &lt; c &gt; d \"e\" &nbsp; f";
        assertEquals("Escaped string should match expected output", expected, escaped);
    }

    @Test
    public void testEscape_NullAndEmptyInput() {
        assertEquals("null input should return empty string", "", Entities.escape(null, defaultSettings));
        assertEquals("empty string should return empty string", "", Entities.escape("", defaultSettings));
    }

    @Test
    public void testEscape_AsciiCharset_NonEncodableChar() {
        // The character “€” (U+20AC) cannot be encoded in US‑ASCII.
        String raw = "price: €";
        String escaped = Entities.escape(raw, asciiSettings);
        // Expected to be emitted as a numeric entity because the charset cannot encode it.
        assertEquals("price: &#x20ac;", escaped);
    }

    @Test
    public void testEscape_ShortcutMethod() {
        // Shortcut should delegate to the method that uses the default settings.
        String raw = "a<b";
        assertEquals(Entities.escape(raw, defaultSettings), Entities.escape(raw));
    }

    @Test
    public void testUnescape_BasicEntities() {
        String escaped = "a &amp; b &lt; c &gt; d &quot; e &#x20ac;";
        String unescaped = Entities.unescape(escaped);
        // The numeric entity &#x20ac; should become the euro sign.
        assertEquals("a & b < c > d \" e €", unescaped);
    }

    @Test
    public void testUnescape_MissingSemicolon_StrictFalse() {
        // When strict is false (the public API), a missing semicolon is tolerated.
        String escaped = "a &amp b &lt c";
        String unescaped = Entities.unescape(escaped);
        assertEquals("a & b < c", unescaped);
    }

    /* --------------------------------------------------------------------- */
    /* 3. Edge‑case / exception tests                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEscape_AttributeContext_NotExposedDirectly() {
        // The public escape method always calls the internal escape with inAttribute = false.
        // Verify that characters that would be escaped only in attribute context are NOT escaped.
        String raw = "\"quoted\"";
        String escaped = Entities.escape(raw, defaultSettings);
        // In non‑attribute context the double quotes are left unchanged.
        assertEquals("\"quoted\"", escaped);
    }

    @Test
    public void testEscape_XHTMLMode_NonBreakingSpace() {
        Document.OutputSettings xhtmlSettings = new Document.OutputSettings()
                .escapeMode(Entities.EscapeMode.xhtml);
        String raw = "\u00A0"; // non‑breaking space
        String escaped = Entities.escape(raw, xhtmlSettings);
        // In XHTML mode, NBSP is emitted as a numeric entity.
        assertEquals("&#xa0;", escaped);
    }

    @Test(expected = SerializationException.class)
    public void testEscape_IOExceptionWrapped() {
        // Force an IOException by passing a faulty Appendable implementation.
        Appendable broken = new Appendable() {
            @Override public Appendable append(CharSequence csq) throws IOException {
                throw new IOException("forced");
            }
            @Override public Appendable append(CharSequence csq, int start, int end) throws IOException {
                throw new IOException("forced");
            }
            @Override public Appendable append(char c) throws IOException {
                throw new IOException("forced");
            }
        };
        try {
            // Directly invoke the package‑private escape method via reflection.
            java.lang.reflect.Method m = Entities.class.getDeclaredMethod(
                    "escape", Appendable.class, String.class,
                    Document.OutputSettings.class, boolean.class, boolean.class, boolean.class);
            m.setAccessible(true);
            m.invoke(null, broken, "test", defaultSettings, false, false, false);
            fail("Expected SerializationException to be thrown");
        } catch (java.lang.reflect.InvocationTargetException e) {
            // unwrap the cause – the method should re‑throw as SerializationException
            throw (RuntimeException) e.getCause();
        } catch (Exception e) {
            fail("Unexpected reflection exception: " + e);
        }
    }

    @Test
    public void testCanEncode_FastPathAscii() throws Exception {
        // Verify that the fast‑path logic in canEncode works for ASCII charset.
        // The method is private; we access it via reflection.
        java.lang.reflect.Method canEncode = Entities.class.getDeclaredMethod(
                "canEncode", Entities.CoreCharset.class, char.class, CharsetEncoder.class);
        canEncode.setAccessible(true);
        CharsetEncoder fallback = Charset.forName("UTF-8").newEncoder();

        // ASCII charset – only chars < 0x80 should return true.
        Entities.CoreCharset ascii = Entities.CoreCharset.ascii;
        assertTrue((Boolean) canEncode.invoke(null, ascii, 'A', fallback));
        assertFalse((Boolean) canEncode.invoke(null, ascii, '\u00A0', fallback));
    }

    @Test
    public void testCoreCharsetByName() {
        assertEquals(Entities.CoreCharset.ascii, Entities.CoreCharset.byName("US-ASCII"));
        assertEquals(Entities.CoreCharset.utf, Entities.CoreCharset.byName("UTF-8"));
        assertEquals(Entities.CoreCharset.utf, Entities.CoreCharset.byName("UTF-16"));
        assertEquals(Entities.CoreCharset.fallback, Entities.CoreCharset.byName("ISO-8859-1"));
    }

    @Test
    public void testLoad_ValidatesEntityCount() {
        // The load method validates that the number of parsed entities matches the expected size.
        // We cannot call it directly (it is private) but we can trigger the static initialisation
        // of each EscapeMode and verify that no ValidationException was thrown.
        // If the count mismatched, the static initialiser would have thrown an IllegalArgumentException.
        // So the mere fact that we can reference the enum constants means the validation passed.
        for (Entities.EscapeMode mode : Entities.EscapeMode.values()) {
            assertNotNull("EscapeMode should be initialised: " + mode, mode);
        }
    }
}
```
###Test END##